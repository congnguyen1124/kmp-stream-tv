package com.congnguyencn.kmpstreamtv.feature.player.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.databinding.ViewMiniPlaybackControllerBinding
import com.congnguyencn.streamplayer.model.StreamTvPlaybackState
import com.congnguyencn.streamplayer.model.StreamTvPlayerState

/**
 * Transport strip under the minimized player, ported from `MiniPlaybackController` in
 * `ottclouds-android`: a time bar over rewind, play/pause/replay and forward.
 *
 * Every dimension the strip paints is divided by the card's zoom scale in [applyZoomScale], for the
 * same reason the composable divides its `2.dp` and `8.dp` by `zoomScale`: the strip lives inside
 * the scaled card, so an undivided value would grow with the pinch.
 */
class MiniPlaybackControllerView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : LinearLayout(context, attrs, defStyleAttr) {
        private val binding = ViewMiniPlaybackControllerBinding.inflate(LayoutInflater.from(context), this)
        private val timeBarHeightPx = resources.getDimensionPixelSize(R.dimen.mini_player_time_bar_height)
        private val actionPaddingPx = resources.getDimensionPixelSize(R.dimen.mini_player_action_padding)
        private val timeBarMax = resources.getInteger(R.integer.mini_player_time_bar_max)

        private var isActionEnabled = true
        private var appliedActionPaddingPx = -1

        var onToggle: (() -> Unit)? = null
        var onRewind: (() -> Unit)? = null
        var onForward: (() -> Unit)? = null
        var onReplay: (() -> Unit)? = null

        init {
            orientation = VERTICAL
            setBackgroundColor(context.getColor(R.color.player_mini_footer))
            binding.miniRewindAction.setOnClickListener { onRewind?.invoke() }
            binding.miniForwardAction.setOnClickListener { onForward?.invoke() }
            binding.miniPlayPauseAction.setOnClickListener {
                if (isEnded) onReplay?.invoke() else onToggle?.invoke()
            }
            applyZoomScale(zoomScale = 1f)
        }

        private var isEnded = false

        /** Disabled while an error or a locked overlay owns the card, matching `isEnabled` there. */
        internal fun setActionsEnabled(enabled: Boolean) {
            if (isActionEnabled == enabled) return
            isActionEnabled = enabled
            alpha = if (enabled) ENABLED_ALPHA else DISABLED_ALPHA
            listOf(
                binding.miniRewindAction,
                binding.miniPlayPauseAction,
                binding.miniForwardAction,
            ).forEach { it.isEnabled = enabled }
        }

        internal fun render(state: StreamTvPlayerState) {
            isEnded = state.playbackState == StreamTvPlaybackState.Ended
            val isBuffering =
                state.playbackState == StreamTvPlaybackState.Buffering ||
                    state.playbackState == StreamTvPlaybackState.Idle
            binding.miniBuffering.isVisible = isBuffering
            binding.miniPlayPauseAction.isVisible = !isBuffering
            binding.miniPlayPauseAction.setImageResource(
                when {
                    isEnded -> R.drawable.ic_player_replay
                    state.isPlaying -> R.drawable.ic_player_pause
                    else -> R.drawable.ic_player_play
                },
            )
            binding.miniPlayPauseAction.contentDescription =
                context.getString(
                    when {
                        isEnded -> R.string.replay
                        state.isPlaying -> R.string.pause
                        else -> R.string.play
                    },
                )

            val durationMillis = state.duration.inWholeMilliseconds
            binding.miniRewindAction.isEnabled = isActionEnabled && durationMillis > 0
            binding.miniForwardAction.isEnabled = isActionEnabled && !isEnded
            binding.miniForwardAction.alpha = if (isEnded) DISABLED_ALPHA else ENABLED_ALPHA
            binding.miniTimeBar.progress = durationMillis.toTimeBarValue(state.position.inWholeMilliseconds)
            binding.miniTimeBar.secondaryProgress =
                durationMillis.toTimeBarValue(state.bufferedPosition.inWholeMilliseconds)
        }

        /**
         * @param zoomScale the card's current scale; every painted dimension is divided by it so the
         * strip reads the same size at any pinch.
         */
        internal fun applyZoomScale(zoomScale: Float) {
            val scale = zoomScale.coerceAtLeast(MIN_ZOOM_SCALE)
            // Written only on a change: the host calls this on every animated frame, and
            // updateLayoutParams asks for a layout pass whether or not it changed anything.
            val timeBarHeight = (timeBarHeightPx / scale).toInt().coerceAtLeast(1)
            if (binding.miniTimeBar.layoutParams.height != timeBarHeight) {
                binding.miniTimeBar.updateLayoutParams<LayoutParams> { height = timeBarHeight }
            }
            val padding = (actionPaddingPx / scale).toInt()
            if (padding == appliedActionPaddingPx) return
            appliedActionPaddingPx = padding
            listOf(
                binding.miniRewindAction,
                binding.miniPlayPauseAction,
                binding.miniForwardAction,
                binding.miniBuffering,
            ).forEach { it.setPadding(padding, padding, padding, padding) }
        }

        /**
         * The time bar is a fixed-range [android.widget.ProgressBar] rather than one re-maxed per
         * item, so a duration longer than `Int.MAX_VALUE` milliseconds cannot overflow it.
         */
        private fun Long.toTimeBarValue(positionMillis: Long): Int {
            if (this <= 0L) return 0
            return (positionMillis.coerceIn(0L, this) * timeBarMax / this).toInt()
        }

        private companion object {
            const val ENABLED_ALPHA = 1f
            const val DISABLED_ALPHA = 0.38f
            const val MIN_ZOOM_SCALE = 0.01f
        }
    }
