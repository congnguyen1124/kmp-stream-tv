package com.congnguyencn.kmpstreamtv.feature.player.miniplayer

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Geometry of the shrink from a host-filling player to a floating mini player, ported from
 * `st.ottclouds.template.feature.playermanager.ui.miniplayer.MinimizableViewState`.
 *
 * Every resting position, travel range and scale ceiling below is the arithmetic of that class.
 * Two things are different, and only because the host is a `ViewGroup` rather than a composable:
 *
 * - **No coroutine scope.** The Compose version queues each update on `AndroidUiDispatcher` and its
 *   comments explain the FIFO ordering hazards that follow (a settle issued in the same pointer
 *   event as the gesture that ended it has to be issued *ahead* of that gesture's own work).
 *   [MinimizableView] calls straight into this class from the touch handler on the main thread, so
 *   there is no queue and no ordering to get wrong.
 * - **[hostHeightPx] is the overlay's height, not the window's.** The Compose version starts from
 *   the raw window and subtracts the top and bottom system-bar insets itself, because its player
 *   `Column` is only `statusBarsPadding()`-ed. `MainActivity` already pads its root by both insets,
 *   so the overlay this state measures *is* `window - topSystemBar - bottomSystemBar`. Subtracting
 *   the insets again here would double-count them.
 *
 * All dimensions are pixels. [onStateChanged] fires on every value change, animated frames
 * included, and is where the host re-applies the layout.
 */
internal class MinimizableViewState(
    val miniViewWidthRatio: Float,
    private val borderPaddingPx: Float,
    private val footerHeightPx: Float,
    private val hostWidthPx: Float,
    private val miniViewWidthPx: Float,
    private val miniViewHeightPx: Float,
    private val animationDurationMillis: Long,
    hostHeightPx: Float,
    appBottomBarHeightPx: Float,
    private val onStateChanged: () -> Unit,
) {
    var isMinimized: Boolean = false
        private set

    private val zoomScaleAnimatable = FloatAnimatable(MIN_SCALE) { onStateChanged() }
    private val offsetY = FloatAnimatable(0f) { onStateChanged() }
    private val offsetX = FloatAnimatable(0f) { onStateChanged() }

    /**
     * Bounded because [widthFraction] feeds the card's width and [edgePaddingPx] feeds its padding,
     * and neither accepts a value outside its range.
     */
    private val minimizeProgressAnimatable =
        FloatAnimatable(FULL_SIZE_PROGRESS) { onStateChanged() }
            .apply { updateBounds(lowerBound = FULL_SIZE_PROGRESS, upperBound = MINI_SIZE_PROGRESS) }

    /** Animated, so a settle can move the drawn scale and the snapped position together. */
    val zoomScale: Float get() = zoomScaleAnimatable.value

    //region Size calculation
    private val availableHeightForDraggingPx =
        (
            hostHeightPx -
                appBottomBarHeightPx -
                miniViewHeightPx -
                footerHeightPx -
                borderPaddingPx * 2
        ).coerceAtLeast(0f)

    /**
     * How far the shrink from host-filling player to mini player has come: [FULL_SIZE_PROGRESS] is
     * the host-filling player, [MINI_SIZE_PROGRESS] is the mini player.
     *
     * Its own animatable rather than `offsetY / availableHeightForDraggingPx`, because those two
     * only agree while the finger is down. [isMinimized] flips the moment the finger lifts, at
     * whatever depth the drag reached, so the shrink still has to finish afterwards — and once
     * minimized the player travels the whole host height between corners at a size that must not
     * change. Sized off [offsetY], the player jumps at both of those moments.
     */
    private val minimizeProgress get() = minimizeProgressAnimatable.value

    val isMiniSizeReached get() = minimizeProgress >= MINI_SIZE_PROGRESS - EPSILON
    val isFullSizeReached get() = minimizeProgress <= EPSILON

    val widthFraction get() = lerp(start = 1f, stop = miniViewWidthRatio, fraction = minimizeProgress)

    /**
     * Opens with the shrink, and divided by [zoomScale] so the painted gap then holds steady: the
     * padding sits on the card, outside the scaled content, so what the scale grows is the content
     * inside it. `(miniViewWidthPx - 2 * borderPadding / zoomScale) * zoomScale` leaves exactly
     * `borderPaddingPx` a side, where an undivided padding would leave `borderPaddingPx * zoomScale`.
     */
    val edgePaddingPx get() = borderPaddingPx * minimizeProgress / zoomScale

    val footerHeightWithScalePx get() = footerHeightPx / zoomScale
    //endregion

    //region Scale properties
    private val maxScale = 1f / miniViewWidthRatio
    private val isBelowScaleMidpoint get() = zoomScale < (maxScale + MIN_SCALE) / 2
    //endregion

    //region UI related properties
    val bottomContentAlpha: Float
        get() =
            if (isMinimized) {
                0f
            } else {
                // ALPHA_BOOST makes the content behind disappear faster than the drag itself
                (1f - ALPHA_BOOST * minimizeProgress).coerceAtLeast(0f)
            }

    val offsetXPx: Int get() = offsetX.value.roundToInt()

    val offsetYPx: Int get() = offsetY.value.roundToInt()
    //endregion

    //region State update methods

    /**
     * Updates minimize progress by the distance actually dragged.
     *
     * Do not derive progress directly from [offsetY] here. During a grow animation, offsetY can
     * stay at 0 while the progress is still changing, so deriving progress from offsetY would cause
     * the player to jump.
     *
     * Read offsetY once so both offsetY and the progress are updated from the same starting value.
     */
    fun onVerticalDragging(dragAmount: Float) {
        // A host too short to fit the mini player leaves no travel to divide by. The Compose
        // version cannot reach this — it is never composed at that size — but a View is laid out
        // once at zero height before its first measure pass, and a NaN progress would corrupt
        // every dimension derived from it.
        if (availableHeightForDraggingPx <= 0f) return

        val currentOffsetY = offsetY.value
        val newOffsetY = (currentOffsetY + dragAmount).coerceIn(0f, availableHeightForDraggingPx)
        val travelled = (newOffsetY - currentOffsetY) / availableHeightForDraggingPx

        offsetY.snapTo(newOffsetY)
        minimizeProgressAnimatable.snapTo(minimizeProgress + travelled)
    }

    fun snapToCorner() {
        val newScale =
            if (isBelowScaleMidpoint) {
                MIN_SCALE
            } else {
                maxScale
            }

        settleTo(newScale = newScale)
    }

    fun onDoubleTap() {
        val newScale =
            if (isBelowScaleMidpoint) {
                maxScale
            } else {
                MIN_SCALE
            }

        settleTo(newScale = newScale)
    }

    /**
     * @param panX horizontal finger travel, in the scaled card's own coordinates — see the
     * conversion at the call site in [MinimizableView].
     * @param panY vertical finger travel, in the same coordinates as [panX].
     */
    fun onGestureZoom(
        newZoomScale: Float,
        panX: Float,
        panY: Float,
    ) {
        val newScale = (zoomScale * newZoomScale).coerceIn(MIN_SCALE, maxScale)

        offsetX.snapTo(offsetX.value + panX * newScale)
        offsetY.snapTo(offsetY.value + panY * newScale)
        zoomScaleAnimatable.snapTo(newScale)
    }

    fun onToggleMinimized() {
        isMinimized = !isMinimized
        // The shrink is animated, not snapped: isMinimized flips at whatever depth the drag
        // reached, so this is what carries the player the rest of the way to its new size.
        zoomScaleAnimatable.snapTo(MIN_SCALE)
        offsetX.animateTo(targetValue = 0f, durationMillis = animationDurationMillis)
        if (isMinimized) {
            minimizeProgressAnimatable.animateTo(
                targetValue = MINI_SIZE_PROGRESS,
                durationMillis = animationDurationMillis,
            )
            offsetY.animateTo(
                targetValue = availableHeightForDraggingPx,
                durationMillis = animationDurationMillis,
            )
        } else {
            minimizeProgressAnimatable.animateTo(
                targetValue = FULL_SIZE_PROGRESS,
                durationMillis = animationDurationMillis,
            )
            offsetY.animateTo(targetValue = 0f, durationMillis = animationDurationMillis)
        }
        onStateChanged()
    }

    /** Drops every animation in flight, for a host that is being torn down or re-measured. */
    fun cancelAnimations() {
        zoomScaleAnimatable.cancel()
        offsetX.cancel()
        offsetY.cancel()
        minimizeProgressAnimatable.cancel()
    }

    /**
     * Restores [isMinimized] across a configuration change or a host re-measure, with no animation
     * and no travel: the new instance is built from the new dimensions, so every offset has to be
     * recomputed from them rather than carried over.
     */
    fun restoreMinimized(minimized: Boolean) {
        if (isMinimized == minimized) return
        isMinimized = minimized
        zoomScaleAnimatable.snapTo(MIN_SCALE)
        offsetX.snapTo(0f)
        if (minimized) {
            minimizeProgressAnimatable.snapTo(MINI_SIZE_PROGRESS)
            offsetY.snapTo(availableHeightForDraggingPx)
        } else {
            minimizeProgressAnimatable.snapTo(FULL_SIZE_PROGRESS)
            offsetY.snapTo(0f)
        }
        onStateChanged()
    }
    //endregion

    /**
     * Moves scale and offsets to the corner [newScale] implies, together. Settling the offsets while
     * leaving [zoomScale] wherever the pinch ended would draw the view at one scale and position it
     * for another, and would route the next gesture through the wrong branch of
     * [isBelowScaleMidpoint].
     */
    private fun settleTo(newScale: Float) {
        // MinimizableView lays this card out top-end, so offsetX is right-edge relative and never
        // positive — which is why the host's own centre below is a negative number.
        val leftMostOffsetX = miniViewWidthPx - hostWidthPx
        val hostCenterXFromRightEdge = -hostWidthPx / 2
        val miniPlayerCenterXFromRightEdge = offsetX.value - miniViewWidthPx / 2

        // Derived from the travel range instead of recomputed from the insets, so the two can never
        // disagree — a mismatch between them is exactly what skews the vertical split off 50%.
        val effectiveHeightPx = availableHeightForDraggingPx + miniViewHeightPx + footerHeightPx
        val yCenter = effectiveHeightPx / 2
        val miniPlayerCenterY = offsetY.value + (miniViewHeightPx + footerHeightPx) / 2

        val offsetXToSnap: Float
        val offsetYToSnap: Float

        if (abs(newScale - maxScale) <= EPSILON) {
            // At max scale the layout box still measures miniViewWidthPx but paints a full host
            // wide, so centring is its only on-screen placement.
            offsetXToSnap = leftMostOffsetX / 2f

            // LAYOUT vs PAINTED. scaleX/scaleY only repaint — the layout box keeps its size, so
            // offsetY moves a box while the eye follows the painted edges. Unprimed = layout,
            // primed = painted. s = newScale, and the pivot is the scaled box's own centre.
            //
            // WHICH box is scaled matters: the padding is on the card, OUTSIDE the scaled content,
            // so offsetY positions the padded box while only its content is scaled. Call that
            // content S, height H = miniViewHeightPx + the footer. S starts one padding below
            // offsetY.
            //
            // Scale: p -> c + (p - c)*s
            // Box S: [a, b] with a = offsetY + borderPaddingPx, b = a + H, centre c = a + H/2.
            // Therefore:
            // a' = c + s * (-H/2) = a + H/2 - s*H/2 = a + (1 - s)*H/2
            // b' = c + s * (H/2)  = a + H/2 + s*H/2 = a + (1 + s)*H/2
            // b' - a' = 2s * H/2 = sH
            // delta = sH - H = H(s - 1)
            // Regrouped as the branches below use it: a' = a - delta/2, b' = a + H + delta/2.
            // Note b' hangs off b, not off a — that missing H is the easy mistake here.
            //
            // H is divided by newScale, the scale this settle is heading to rather than the one the
            // gesture left behind. The layout divides by zoomScale, which settleTo animates to
            // newScale, so the geometry below and what gets painted converge as the settle lands.
            val scaledBoxHeightPx = miniViewHeightPx + footerHeightPx / newScale
            val restingBoxHeightPx = miniViewHeightPx + footerHeightPx
            val deltaHeightPx = scaledBoxHeightPx * (newScale - 1f)

            // Both ends aim at the painted edge the s = 1 branch below already produces, so pinching
            // to max and back does not shift where the mini player sits. Targeting the usable area
            // directly would drift ~9px, because availableHeightForDraggingPx measures the video
            // across the unpadded width while S measures it across the padded one.
            offsetYToSnap =
                if (miniPlayerCenterY < yCenter) {
                    // At s = 1 the branch below parks offsetY at 0, so a' lands on borderPaddingPx.
                    // a' = borderPaddingPx
                    // <-> (offsetY + borderPaddingPx) - deltaHeightPx/2 = borderPaddingPx
                    // <-> offsetY = deltaHeightPx/2
                    deltaHeightPx / 2
                } else {
                    // At s = 1 the branch below parks offsetY at availableHeightForDraggingPx, so
                    // b' lands on availableHeightForDraggingPx + borderPaddingPx +
                    // restingBoxHeightPx.
                    // <-> offsetY = availableHeightForDraggingPx + restingBoxHeightPx
                    //                 - scaledBoxHeightPx - deltaHeightPx/2
                    availableHeightForDraggingPx + restingBoxHeightPx - scaledBoxHeightPx -
                        deltaHeightPx / 2
                }
        } else {
            // newScale is MIN_SCALE here, so delta = 0 and the painted edges are the layout edges —
            // which is why neither axis needs the correction above.
            offsetXToSnap =
                if (miniPlayerCenterXFromRightEdge < hostCenterXFromRightEdge) {
                    leftMostOffsetX
                } else {
                    0f
                }

            offsetYToSnap =
                if (miniPlayerCenterY < yCenter) {
                    0f
                } else {
                    availableHeightForDraggingPx
                }
        }

        zoomScaleAnimatable.animateTo(targetValue = newScale, durationMillis = animationDurationMillis)
        offsetX.animateTo(targetValue = offsetXToSnap, durationMillis = animationDurationMillis)
        offsetY.animateTo(targetValue = offsetYToSnap, durationMillis = animationDurationMillis)
    }

    private fun lerp(
        start: Float,
        stop: Float,
        fraction: Float,
    ): Float = start + (stop - start) * fraction

    companion object {
        const val MIN_SCALE = 1f
        private const val FULL_SIZE_PROGRESS = 0f
        private const val MINI_SIZE_PROGRESS = 1f
        private const val EPSILON = 1e-2f
        private const val ALPHA_BOOST = 3f
    }
}
