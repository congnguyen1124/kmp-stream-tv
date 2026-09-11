package com.congnguyencn.kmpstreamtv.feature.player.miniplayer

import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.feature.player.PlayerPresentation
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Hosts the detail, fullscreen and floating mini-player layouts of one player overlay — the View
 * counterpart of `NewMinimizableView` in `ottclouds-android`.
 *
 * The layout it drives, mapped to the composable it is ported from:
 *
 * | This view | `NewMinimizableView` |
 * | --- | --- |
 * | itself | `Box(fillMaxSize)` |
 * | `R.id.detailContent` | `bottomContent`, faded by `bottomContentAlpha` |
 * | `R.id.playerCard` | the `Column`: width fraction, edge padding, offset, top-end gravity |
 * | `R.id.playerCardContent` | what `graphicsLayer { scale }` scales, clips and shadows |
 * | `R.id.playerSurface` | the 16:9 video `Box` |
 * | `R.id.miniPlaybackController` | `footerContent` |
 *
 * The padded card and the scaled content have to be two views, not one: padding lives inside a
 * view's own bounds, so scaling the card would scale its border gap with it. That split is what
 * makes the `edgePaddingPx` and `settleTo` arithmetic in [MinimizableViewState] land where it does.
 */
class MinimizableView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : FrameLayout(context, attrs, defStyleAttr) {
        private val borderPaddingPx = resources.getDimension(R.dimen.mini_player_border_padding)
        private val footerHeightPx = resources.getDimension(R.dimen.mini_player_footer_height)
        private val miniPlayerMaxWidthPx = resources.getDimension(R.dimen.mini_player_max_width)
        private val cornerRadiusPx = resources.getDimension(R.dimen.mini_player_corner_radius)
        private val elevationPx = resources.getDimension(R.dimen.mini_player_elevation)
        private val borderWidthPx = resources.getDimensionPixelSize(R.dimen.mini_player_border_width)
        private val animationDurationMillis =
            resources.getInteger(R.integer.player_transition_duration).toLong()
        private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

        private lateinit var detailContent: View
        private lateinit var playerCard: View
        private lateinit var playerCardContent: View
        private lateinit var playerSurface: AspectRatioFrameLayout
        private lateinit var footer: View

        private var state: MinimizableViewState? = null
        private var presentation = PlayerPresentation.DETAIL
        private var isSystemPictureInPicture = false
        private var isFooterShown = false
        private var paintedCornerRadiusPx = cornerRadiusPx

        /**
         * Only set while a downward drag owns the stream. The drag is stolen from whichever child
         * the finger landed on, so it cannot be detected in [onTouchEvent] alone.
         */
        private var isDraggingDown = false
        private var dragDownX = 0f
        private var dragDownY = 0f
        private var previousDragY = 0f

        /**
         * Whether the stream being dispatched started on the card.
         *
         * This view spans the whole overlay, so without it the detail list below the player could
         * not be scrolled and a tap beside a minimized player would expand it instead of reaching
         * the destination behind. It is the `pointerInput` sitting on the player `Column` alone in
         * `NewMinimizableView`, rather than on the `Box` that also holds `bottomContent`.
         */
        private var isGestureOnCard = false

        /**
         * Whether the event currently being dispatched was already handed to the gesture code from
         * [onInterceptTouchEvent].
         *
         * `ViewGroup.dispatchTouchEvent` calls `onInterceptTouchEvent` and then, when no child took
         * the event, `onTouchEvent` for that same event — so the down of an untouched card arrives
         * twice. Cleared when the gesture is stolen, because from then on only `onTouchEvent` runs.
         */
        private var isEventHandledFromIntercept = false

        private val transformDetector by lazy {
            TransformGestureDetector(
                view = this,
                onGesture = ::onTransformGesture,
                onGestureEnd = { state?.snapToCorner() },
                onTap = { toggleMinimized() },
                onDoubleTap = { state?.onDoubleTap() },
            )
        }

        private val cardOutlineProvider =
            object : ViewOutlineProvider() {
                override fun getOutline(
                    view: View,
                    outline: Outline,
                ) {
                    outline.setRoundRect(0, 0, view.width, view.height, paintedCornerRadiusPx)
                }
            }

        private val cardBorder =
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.TRANSPARENT)
                setStroke(borderWidthPx, context.getColor(R.color.white_10))
            }

        /** Fires whenever the minimized/expanded state flips, never for an intermediate frame. */
        var onMinimizedChanged: ((isMinimized: Boolean) -> Unit)? = null

        /**
         * Fires on every scale change so content inside the scaled card can divide its own
         * dimensions by the scale and keep them painting at a constant size.
         */
        var onZoomScaleChanged: ((zoomScale: Float) -> Unit)? = null

        /**
         * Height of the app chrome the mini player has to stay clear of at the bottom of the host,
         * i.e. `appBottomBarHeightPx`. Measured by the host rather than assumed, so the resting
         * mini player keeps its border gap above the real bottom navigation.
         */
        var appBottomBarHeightPx: Float = resources.getDimension(R.dimen.mini_player_bottom_margin)
            set(value) {
                if (field == value) return
                field = value
                rebuildState()
            }

        val isMinimized: Boolean get() = state?.isMinimized == true

        override fun onFinishInflate() {
            super.onFinishInflate()
            detailContent = findViewById(R.id.detailContent)
            playerCard = findViewById(R.id.playerCard)
            playerCardContent = findViewById(R.id.playerCardContent)
            playerSurface = findViewById(R.id.playerSurface)
            footer = findViewById(R.id.miniPlaybackController)
            footer.isVisible = false
            playerSurface.aspectRatio = PLAYER_RATIO
        }

        override fun onSizeChanged(
            width: Int,
            height: Int,
            oldWidth: Int,
            oldHeight: Int,
        ) {
            super.onSizeChanged(width, height, oldWidth, oldHeight)
            if (width == oldWidth && height == oldHeight) return
            rebuildState()
        }

        override fun onDetachedFromWindow() {
            transformDetector.cancel()
            state?.cancelAnimations()
            super.onDetachedFromWindow()
        }

        internal fun setPresentation(presentation: PlayerPresentation) {
            this.presentation = presentation
            val minimized = presentation == PlayerPresentation.MINI
            if (isMinimized != minimized) {
                state?.restoreMinimized(minimized)
            }
            applyState()
        }

        internal fun setSystemPictureInPicture(enabled: Boolean) {
            if (isSystemPictureInPicture == enabled) return
            isSystemPictureInPicture = enabled
            applyState()
        }

        /** The `onRegisterMinimize` lambda of the composable: collapse, or do nothing if collapsed. */
        internal fun minimize() {
            val state = state ?: return
            if (!state.isMinimized) toggleMinimized()
        }

        /** The `onRegisterMaximize` lambda of the composable: expand, or do nothing if expanded. */
        internal fun maximize() {
            val state = state ?: return
            if (state.isMinimized) toggleMinimized()
        }

        override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                isGestureOnCard = !isFillingHost() && state != null && isInsideCard(event)
            }
            if (!isGestureOnCard) return false

            // Handled from the intercept pass so the gesture code sees the whole stream, the way a
            // Compose pointerInput on this node sees every event in PointerEventPass.Main even
            // while a child is still consuming them.
            handleGesture(event)
            val shouldSteal = isDraggingDown || transformDetector.isTransforming
            isEventHandledFromIntercept = !shouldSteal
            return shouldSteal
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (!isGestureOnCard) return false
            if (isEventHandledFromIntercept) {
                isEventHandledFromIntercept = false
                return true
            }
            handleGesture(event)
            return true
        }

        /**
         * Hit-tested against the card's layout bounds, not the area it paints.
         *
         * A pinched-open card paints wider than it measures, and Compose hit-tests layout bounds
         * too — its `pointerInput` sits inside the `graphicsLayer`, which only repaints. Following
         * the painted edges here would give the port a larger touch target than the original.
         */
        private fun isInsideCard(event: MotionEvent): Boolean {
            val left = playerCard.left + playerCard.translationX
            val top = playerCard.top + playerCard.translationY
            return event.x >= left &&
                event.x <= left + playerCard.width &&
                event.y >= top &&
                event.y <= top + playerCard.height
        }

        private fun handleGesture(event: MotionEvent) {
            val state = state ?: return
            if (state.isMinimized) {
                transformDetector.onTouchEvent(event)
            } else {
                handleDragDown(event = event, state = state)
            }
        }

        /**
         * `detectVerticalDragGestures` on the expanded player: follow the finger down, and toggle on
         * release at whatever depth the drag reached.
         *
         * Downward only, unlike the composable, which starts on vertical slop in either direction
         * and so minimizes on an upward drag too — an upward drag has nowhere to travel, because
         * `onVerticalDragging` clamps the offset at 0.
         */
        private fun handleDragDown(
            event: MotionEvent,
            state: MinimizableViewState,
        ) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragDownX = event.x
                    dragDownY = event.y
                    previousDragY = event.y
                    isDraggingDown = false
                }

                MotionEvent.ACTION_MOVE -> {
                    if (!isDraggingDown) {
                        val distanceX = event.x - dragDownX
                        val distanceY = event.y - dragDownY
                        if (distanceY > touchSlop && distanceY > abs(distanceX)) {
                            isDraggingDown = true
                            previousDragY = event.y
                        }
                        return
                    }
                    state.onVerticalDragging(dragAmount = event.y - previousDragY)
                    previousDragY = event.y
                }

                MotionEvent.ACTION_UP -> {
                    if (isDraggingDown) toggleMinimized()
                    isDraggingDown = false
                }

                MotionEvent.ACTION_CANCEL -> {
                    isDraggingDown = false
                }
            }
        }

        private fun onTransformGesture(
            panX: Float,
            panY: Float,
            zoom: Float,
        ) {
            val state = state ?: return
            // `onGestureZoom` multiplies the pan by the scale it is heading to, because the Compose
            // detector it was written for sits inside the scaled layer and therefore reports travel
            // in the scaled card's own coordinates. This view sits above the scale, so its pan is
            // already in host pixels and has to be divided back before that multiplication.
            val scale = state.zoomScale
            state.onGestureZoom(newZoomScale = zoom, panX = panX / scale, panY = panY / scale)
        }

        private fun toggleMinimized() {
            val state = state ?: return
            state.onToggleMinimized()
            onMinimizedChanged?.invoke(state.isMinimized)
        }

        private fun rebuildState() {
            val hostWidthPx = width.toFloat()
            val hostHeightPx = height.toFloat()
            if (hostWidthPx <= 0f || hostHeightPx <= 0f) return

            // Before the first measure pass there is no state to carry over, so a restored
            // fragment falls back to the presentation it was asked for — otherwise a player that
            // was minimized when the process died comes back host-filling.
            val wasMinimized = state?.isMinimized ?: (presentation == PlayerPresentation.MINI)
            state?.cancelAnimations()

            // Capping the ratio rather than the rendered width keeps every consumer correct at
            // once: the drag interpolation, the pinch-zoom ceiling and the 16:9 height all derive
            // from it, and the expanded player still reaches a full-width fraction of 1.
            val miniViewWidthRatio = minOf(MINI_PLAYER_WIDTH_RATIO, miniPlayerMaxWidthPx / hostWidthPx)
            val miniViewWidthPx = hostWidthPx * miniViewWidthRatio

            // Across the PADDED width: the video box is a child of the card's padding, so its 16:9
            // is measured after the border is taken off. Sizing it from miniViewWidthPx overstates
            // the mini player by 2 * borderPadding / PLAYER_RATIO, which would dock it that much
            // clear of the bottom bar on top of the border gap it is meant to keep.
            val miniViewHeightPx = (miniViewWidthPx - borderPaddingPx * 2) / PLAYER_RATIO

            state =
                MinimizableViewState(
                    miniViewWidthRatio = miniViewWidthRatio,
                    borderPaddingPx = borderPaddingPx,
                    footerHeightPx = footerHeightPx,
                    hostWidthPx = hostWidthPx,
                    miniViewWidthPx = miniViewWidthPx,
                    miniViewHeightPx = miniViewHeightPx,
                    animationDurationMillis = animationDurationMillis,
                    hostHeightPx = hostHeightPx,
                    appBottomBarHeightPx = appBottomBarHeightPx,
                    onStateChanged = ::applyState,
                ).apply { restoreMinimized(minimized = wasMinimized) }
            applyState()
        }

        private fun isFillingHost(): Boolean = presentation == PlayerPresentation.FULLSCREEN || isSystemPictureInPicture

        private fun applyState() {
            val state = state ?: return
            if (isFillingHost()) {
                applyFillingHostLayout()
                return
            }

            detailContent.isVisible = !state.isMinimized
            detailContent.alpha = state.bottomContentAlpha

            val cardWidth = (width * state.widthFraction).roundToInt()
            playerCard.setSize(width = cardWidth, height = ViewGroup.LayoutParams.WRAP_CONTENT)
            val edgePadding = state.edgePaddingPx.roundToInt()
            playerCard.setPadding(edgePadding, edgePadding, edgePadding, edgePadding)
            playerCard.translationX = state.offsetXPx.toFloat()
            playerCard.translationY = state.offsetYPx.toFloat()

            playerCardContent.scaleX = state.zoomScale
            playerCardContent.scaleY = state.zoomScale
            playerCardContent.setSize(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            playerSurface.aspectRatio = PLAYER_RATIO
            playerSurface.setSize(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = ViewGroup.LayoutParams.WRAP_CONTENT,
            )

            applyCardDecoration(state = state)
            applyFooter(state = state)
            onZoomScaleChanged?.invoke(state.zoomScale)
        }

        private fun applyFillingHostLayout() {
            detailContent.isVisible = false
            playerCard.setSize(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = ViewGroup.LayoutParams.MATCH_PARENT,
            )
            playerCard.setPadding(0, 0, 0, 0)
            playerCard.translationX = 0f
            playerCard.translationY = 0f
            playerCardContent.scaleX = MinimizableViewState.MIN_SCALE
            playerCardContent.scaleY = MinimizableViewState.MIN_SCALE
            playerCardContent.setSize(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = ViewGroup.LayoutParams.MATCH_PARENT,
            )
            playerSurface.aspectRatio = AspectRatioFrameLayout.NO_ASPECT_RATIO
            playerSurface.setSize(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = ViewGroup.LayoutParams.MATCH_PARENT,
            )
            clearCardDecoration()
            isFooterShown = false
            footer.animate().cancel()
            footer.isVisible = false
            onZoomScaleChanged?.invoke(MinimizableViewState.MIN_SCALE)
        }

        /**
         * Corner radius, border and shadow come in with the card, and the radius is divided by the
         * scale so the painted corner holds at one value however far the card is pinched.
         */
        private fun applyCardDecoration(state: MinimizableViewState) {
            if (!state.isMiniSizeReached && !state.isMinimized) {
                clearCardDecoration()
                return
            }
            paintedCornerRadiusPx = cornerRadiusPx / state.zoomScale
            cardBorder.cornerRadius = paintedCornerRadiusPx
            if (playerCardContent.foreground != cardBorder) {
                playerCardContent.foreground = cardBorder
                playerCardContent.outlineProvider = cardOutlineProvider
                playerCardContent.clipToOutline = true
                playerCardContent.elevation = elevationPx
            }
            playerCardContent.invalidateOutline()
        }

        private fun clearCardDecoration() {
            if (playerCardContent.foreground == null) return
            playerCardContent.foreground = null
            playerCardContent.clipToOutline = false
            playerCardContent.outlineProvider = ViewOutlineProvider.BACKGROUND
            playerCardContent.elevation = 0f
        }

        /**
         * The footer is only there at mini size, and its height is divided by the scale for the same
         * reason the corner radius is: the strip should read the same height at every pinch.
         */
        private fun applyFooter(state: MinimizableViewState) {
            val shouldShow =
                !isSystemPictureInPicture && (state.isMiniSizeReached || state.isMinimized)
            if (shouldShow) {
                footer.setSize(
                    width = ViewGroup.LayoutParams.MATCH_PARENT,
                    height = state.footerHeightWithScalePx.roundToInt(),
                )
            }
            if (shouldShow == isFooterShown) return

            isFooterShown = shouldShow
            footer.animate().cancel()
            if (shouldShow) {
                footer.alpha = 0f
                footer.isVisible = true
                footer
                    .animate()
                    .alpha(1f)
                    .setDuration(animationDurationMillis)
                    .start()
            } else {
                footer
                    .animate()
                    .alpha(0f)
                    .setDuration(animationDurationMillis)
                    .withEndAction { if (!isFooterShown) footer.isVisible = false }
                    .start()
            }
        }

        /**
         * Writes layout params only when they actually change.
         *
         * [applyState] runs on every animated frame, and `updateLayoutParams` asks for a layout
         * pass whether or not it changed anything — which, called from `onSizeChanged`, would also
         * mean a second layout pass on every size change.
         */
        private fun View.setSize(
            width: Int,
            height: Int,
        ) {
            val params = layoutParams ?: return
            if (params.width == width && params.height == height) return
            params.width = width
            params.height = height
            layoutParams = params
        }

        private companion object {
            const val MINI_PLAYER_WIDTH_RATIO = 0.7f
            const val PLAYER_RATIO = 16f / 9
        }
    }
