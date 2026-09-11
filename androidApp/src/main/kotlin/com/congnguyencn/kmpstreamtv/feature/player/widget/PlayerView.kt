package com.congnguyencn.kmpstreamtv.feature.player.widget

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import coil3.load
import coil3.request.crossfade
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.databinding.ViewPlayerBinding
import com.congnguyencn.kmpstreamtv.feature.player.PlayerMedia
import com.congnguyencn.kmpstreamtv.feature.player.PlayerPresentation
import com.congnguyencn.streamplayer.StreamTvPlayerManager
import com.congnguyencn.streamplayer.exoPlayer
import com.congnguyencn.streamplayer.pause
import com.congnguyencn.streamplayer.play
import com.congnguyencn.streamplayer.replay
import com.congnguyencn.streamplayer.seekBack
import com.congnguyencn.streamplayer.seekForward
import com.congnguyencn.streamplayer.seekTo
import com.congnguyencn.streamplayer.selectAudioTrack
import com.congnguyencn.streamplayer.selectTextTrack
import com.congnguyencn.streamplayer.selectVideoTrack
import com.congnguyencn.streamplayer.setSpeed
import com.congnguyencn.streamplayer.togglePlayPause
import com.congnguyencn.streamplayer.togglePlayPauseAtDefaultPosition
import com.congnguyencn.streamplayer.model.StreamTvPlaybackError
import com.congnguyencn.streamplayer.model.StreamTvPlaybackState
import com.congnguyencn.streamplayer.model.StreamTvPlayerState
import com.congnguyencn.streamplayer.model.StreamTvTextTrack
import com.congnguyencn.streamplayer.model.StreamTvVideoTrack
import kotlin.time.Duration.Companion.milliseconds
import kotlin.math.abs

/**
 * App-owned Android player UI, ported from `io.teragroup.player.MediaPlayerView`.
 *
 * This class deliberately owns no playback state: all commands and immutable snapshots come from
 * [StreamTvPlayerManager]. It only owns controller visibility, settings presentation and the
 * expanded/mini layout of the media surface.
 */
class PlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    private val binding = ViewPlayerBinding.inflate(LayoutInflater.from(context), this, true)
    private val controllerViews: List<View> by lazy {
        listOf(binding.topController, binding.centerController, binding.bottomController)
    }
    private var manager: StreamTvPlayerManager? = null
    private var media: PlayerMedia? = null
    private var latestState = StreamTvPlayerState.Initial
    private var presentation = PlayerPresentation.DETAIL
    private var controlsVisible = true
    private var isSystemPictureInPicture = false
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var isDragging = false
    private var velocityTracker: VelocityTracker? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var playbackSpeed = DEFAULT_SPEED
    private var resumeAfterSettings = false
    private var isMuted = false
    private val hideControls = Runnable { setControllerVisible(false) }

    var onClose: (() -> Unit)? = null
    var onMinimize: (() -> Unit)? = null
    var onPictureInPicture: (() -> Unit)? = null
    var onExpand: (() -> Unit)? = null
    var onFullscreenToggle: (() -> Unit)? = null
    var onRetry: (() -> Unit)? = null
    var onEpisodes: (() -> Unit)? = null
    var onDrag: ((distanceY: Float) -> Unit)? = null
    var onDragEnd: ((distanceY: Float, velocityY: Float) -> Unit)? = null

    init {
        setBackgroundColor(context.getColor(R.color.black))
        binding.viewMask.setOnClickListener {
            if (presentation == PlayerPresentation.MINI) onExpand?.invoke() else toggleController()
        }
        binding.viewMask.setOnTouchListener(::handleSurfaceTouch)
        binding.miniController.setOnClickListener { onExpand?.invoke() }
        binding.closeAction.setOnClickListener { onClose?.invoke() }
        binding.closeErrorAction.setOnClickListener { onClose?.invoke() }
        binding.miniCloseAction.setOnClickListener { onClose?.invoke() }
        binding.minimizeAction.setOnClickListener { onMinimize?.invoke() }
        binding.pipAction.setOnClickListener { onPictureInPicture?.invoke() }
        binding.fullscreenAction.setOnClickListener { onFullscreenToggle?.invoke() }
        binding.retryAction.setOnClickListener { onRetry?.invoke() }
        binding.episodesAction.setOnClickListener { onEpisodes?.invoke() }

        binding.rewindAction.setOnClickListener {
            manager?.seekBack()
            revealController()
        }
        binding.forwardAction.setOnClickListener {
            manager?.seekForward()
            revealController()
        }
        binding.playPauseAction.setOnClickListener { togglePlayback() }
        binding.miniPlayPauseAction.setOnClickListener { togglePlayback() }
        binding.volumeAction.setOnClickListener { toggleMute() }
        binding.qualityAction.setOnClickListener { showSettings(listOf(PlayerSettingType.VIDEO)) }
        binding.speedAction.setOnClickListener { showSettings(listOf(PlayerSettingType.SPEED)) }
        binding.soundSubtitleAction.setOnClickListener {
            buildList {
                if (latestState.audioTracks.size > 1) add(PlayerSettingType.AUDIO)
                if (latestState.textTracks.isNotEmpty()) add(PlayerSettingType.SUBTITLE)
            }.takeIf { it.isNotEmpty() }?.let(::showSettings)
        }
        binding.seekProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.progressText.text = context.getString(
                        R.string.elapsed_time,
                        progress.toLong().asTimestamp(),
                        seekBar.max.toLong().asTimestamp(),
                    )
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                removeCallbacks(hideControls)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                manager?.seekTo(seekBar.progress.toLong().milliseconds)
                revealController()
            }
        })

        binding.settingsView.onOptionSelected = { type, value ->
            when (type) {
                PlayerSettingType.SPEED -> {
                    playbackSpeed = value.toFloatOrNull() ?: DEFAULT_SPEED
                    manager?.setSpeed(playbackSpeed)
                    renderSpeed()
                }
                PlayerSettingType.VIDEO -> manager?.selectVideoTrack(
                    value.ifBlank { StreamTvVideoTrack.AUTO_ID },
                )
                PlayerSettingType.AUDIO -> manager?.selectAudioTrack(value)
                PlayerSettingType.SUBTITLE -> manager?.selectTextTrack(
                    value.ifBlank { StreamTvTextTrack.OFF_ID },
                )
            }
            binding.settingsView.render(latestState, playbackSpeed)
        }
        binding.settingsView.onDismiss = {
            if (resumeAfterSettings) manager?.play()
            resumeAfterSettings = false
            revealController()
        }
        renderSpeed()
    }

    internal fun attach(manager: StreamTvPlayerManager) {
        this.manager = manager
        binding.mediaSurface.player = manager.exoPlayer()
        isMuted = manager.exoPlayer()?.volume == 0f
        renderVolume()
    }

    internal fun detach() {
        removeCallbacks(hideControls)
        binding.mediaSurface.player = null
        manager = null
    }

    internal fun bindMedia(media: PlayerMedia) {
        this.media = media
        binding.playerTitle.text = media.title
        binding.miniTitle.text = media.title
        binding.mediaArtwork.load(media.thumbnailUrl) { crossfade(true) }
        binding.episodesAction.isVisible = media.episodeCount > 1
        applyOrientation()
    }

    internal fun render(state: StreamTvPlayerState) {
        latestState = state
        val error = state.playbackError
        val isLoading = error == null && (
            state.playbackState == StreamTvPlaybackState.Idle ||
                state.playbackState == StreamTvPlaybackState.Buffering
            )
        binding.loading.isVisible = isLoading
        binding.errorView.isVisible =
            error != null && presentation != PlayerPresentation.MINI && !isSystemPictureInPicture
        binding.mediaArtwork.isVisible = state.playbackState == StreamTvPlaybackState.Idle ||
            state.playbackState == StreamTvPlaybackState.Buffering
        if (error != null) {
            binding.errorMessage.setText(error.messageRes())
            binding.retryAction.isVisible = error.isRetryable
            removeCallbacks(hideControls)
        }

        val isEnded = state.playbackState == StreamTvPlaybackState.Ended
        val actionIcon = when {
            isEnded -> R.drawable.ic_player_replay
            state.isPlaying -> R.drawable.ic_player_pause
            else -> R.drawable.ic_player_play
        }
        binding.playPauseAction.setImageResource(actionIcon)
        binding.miniPlayPauseAction.setImageResource(actionIcon)
        val actionDescription = if (state.isPlaying) R.string.pause else R.string.play
        binding.playPauseAction.setContentDescription(context.getString(actionDescription))
        binding.miniPlayPauseAction.setContentDescription(context.getString(actionDescription))

        val isLive = media?.isLive == true
        binding.rewindAction.isVisible = !isLive && !isEnded
        binding.forwardAction.isVisible = !isLive && !isEnded
        binding.seekProgress.isVisible = !isLive
        binding.progressText.isVisible = !isLive
        binding.liveBadge.isVisible = isLive
        binding.qualityAction.isVisible = !isLive && state.videoTracks.size > 1
        binding.soundSubtitleAction.isVisible =
            !isLive && (state.audioTracks.size > 1 || state.textTracks.isNotEmpty())

        if (!isLive && !binding.seekProgress.isPressed) {
            val duration = state.duration.inWholeMilliseconds.coerceAtLeast(0)
            val position = state.position.inWholeMilliseconds.coerceIn(0, duration)
            val buffered = state.bufferedPosition.inWholeMilliseconds.coerceIn(0, duration)
            binding.seekProgress.max = duration.toProgressInt()
            binding.seekProgress.progress = position.toProgressInt()
            binding.seekProgress.secondaryProgress = buffered.toProgressInt()
            binding.progressText.text = context.getString(
                R.string.elapsed_time,
                position.asTimestamp(),
                duration.asTimestamp(),
            )
        }
        binding.settingsView.render(state, playbackSpeed)
        applyOrientation()
        if (state.isPlaying && controlsVisible && error == null) scheduleControllerHide()
    }

    internal fun setPresentation(presentation: PlayerPresentation) {
        this.presentation = presentation
        val isMini = presentation == PlayerPresentation.MINI
        binding.surfaceContainer.updateLayoutParams<LayoutParams> {
            width = if (isMini) resources.getDimensionPixelSize(R.dimen.player_mini_video_width) else MATCH_PARENT
            height = MATCH_PARENT
        }
        controllerViews.forEach { it.isVisible = !isMini && !isSystemPictureInPicture }
        binding.miniController.isVisible = isMini && !isSystemPictureInPicture
        binding.errorView.isVisible = !isMini && !isSystemPictureInPicture && latestState.playbackError != null
        binding.settingsView.isVisible = false
        controlsVisible = !isMini
        if (isMini) {
            removeCallbacks(hideControls)
        } else {
            revealController()
        }
        applyOrientation()
    }

    internal fun setSystemPictureInPicture(enabled: Boolean) {
        isSystemPictureInPicture = enabled
        removeCallbacks(hideControls)
        if (enabled) {
            controllerViews.forEach { it.isVisible = false }
            binding.miniController.isVisible = false
            binding.errorView.isVisible = false
            binding.settingsView.isVisible = false
        } else {
            setPresentation(presentation)
        }
    }

    internal fun applyOrientation() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val bottomActionHeight = resources.getDimensionPixelSize(R.dimen.player_bottom_action_menu_height)
        if (isSystemPictureInPicture) {
            controllerViews.forEach { it.isVisible = false }
            binding.miniController.isVisible = false
            return
        }
        binding.playerTitle.isInvisible = !isLandscape
        binding.closeAction.isVisible = presentation == PlayerPresentation.FULLSCREEN
        binding.minimizeAction.isVisible = presentation == PlayerPresentation.DETAIL
        binding.pipAction.isVisible = presentation != PlayerPresentation.MINI
        binding.bottomActions.visibility =
            if (isLandscape && media?.isLive != true) View.VISIBLE else View.INVISIBLE
        binding.bottomActions.updateLayoutParams<android.view.ViewGroup.LayoutParams> {
            height = bottomActionHeight
        }
        listOf(binding.speedAction, binding.soundSubtitleAction, binding.episodesAction).forEach { action ->
            action.updateLayoutParams<LinearLayout.LayoutParams> {
                height = bottomActionHeight
            }
        }
        binding.playPauseAction.updateLayoutParams<LinearLayout.LayoutParams> {
            marginStart = resources.getDimensionPixelSize(R.dimen.player_center_action_spacing)
        }
        binding.forwardAction.updateLayoutParams<LinearLayout.LayoutParams> {
            marginStart = resources.getDimensionPixelSize(R.dimen.player_center_action_spacing)
        }
        binding.fullscreenAction.setImageResource(
            if (isLandscape) R.drawable.ic_player_exit_fullscreen else R.drawable.ic_player_fullscreen,
        )
        binding.fullscreenAction.setContentDescription(
            context.getString(if (isLandscape) R.string.exit_fullscreen else R.string.fullscreen),
        )
    }

    internal fun dismissSettings(): Boolean = binding.settingsView.dismiss()

    private fun showSettings(types: List<PlayerSettingType>) {
        resumeAfterSettings = latestState.isPlaying
        if (resumeAfterSettings) manager?.pause()
        removeCallbacks(hideControls)
        binding.settingsView.show(types, latestState, playbackSpeed)
    }

    private fun togglePlayback() {
        val player = manager ?: return
        when {
            latestState.playbackState == StreamTvPlaybackState.Ended -> player.replay()
            media?.isLive == true -> player.togglePlayPauseAtDefaultPosition()
            else -> player.togglePlayPause()
        }
        revealController()
    }

    private fun toggleMute() {
        val exoPlayer = manager?.exoPlayer() ?: return
        isMuted = !isMuted
        exoPlayer.volume = if (isMuted) 0f else 1f
        renderVolume()
        revealController()
    }

    private fun renderVolume() {
        binding.volumeAction.setImageResource(
            if (isMuted) R.drawable.ic_player_audio_off else R.drawable.ic_player_audio_on,
        )
        binding.volumeAction.setContentDescription(
            context.getString(if (isMuted) R.string.unmute else R.string.mute),
        )
    }

    private fun renderSpeed() {
        val value = if (playbackSpeed == playbackSpeed.toInt().toFloat()) {
            playbackSpeed.toInt().toString()
        } else {
            playbackSpeed.toString()
        }
        binding.speedAction.text = context.getString(
            R.string.player_speed_value,
            context.getString(R.string.player_speed),
            value,
        )
    }

    private fun controllerAnimationMillis(): Long =
        resources.getInteger(R.integer.player_transition_duration).toLong()

    private fun toggleController() {
        setControllerVisible(!controlsVisible)
        if (controlsVisible) scheduleControllerHide()
    }

    private fun revealController() {
        if (presentation == PlayerPresentation.MINI) return
        setControllerVisible(true)
        scheduleControllerHide()
    }

    private fun setControllerVisible(visible: Boolean) {
        if (presentation == PlayerPresentation.MINI || isSystemPictureInPicture) return
        controlsVisible = visible
        removeCallbacks(hideControls)
        controllerViews.forEach { view ->
            view.animate().cancel()
            if (visible) {
                view.isVisible = true
                view.animate().alpha(1f).setDuration(controllerAnimationMillis()).start()
            } else {
                view.animate()
                    .alpha(0f)
                    .setDuration(controllerAnimationMillis())
                    .withEndAction { if (!controlsVisible) view.isVisible = false }
                    .start()
            }
        }
    }

    private fun scheduleControllerHide() {
        removeCallbacks(hideControls)
        if (latestState.isPlaying && binding.settingsView.isVisible.not()) {
            postDelayed(hideControls, CONTROLLER_DISPLAY_MILLIS)
        }
    }

    private fun handleSurfaceTouch(view: View, event: MotionEvent): Boolean {
        velocityTracker?.addMovement(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x
                touchDownY = event.y
                isDragging = false
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain().also { it.addMovement(event) }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val distanceX = event.x - touchDownX
                val distanceY = event.y - touchDownY
                if (
                    presentation == PlayerPresentation.DETAIL &&
                    !isSystemPictureInPicture &&
                    distanceY > touchSlop &&
                    distanceY > abs(distanceX)
                ) {
                    isDragging = true
                }
                if (isDragging) {
                    onDrag?.invoke(distanceY.coerceAtLeast(0f))
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    velocityTracker?.computeCurrentVelocity(1_000)
                    onDragEnd?.invoke(
                        (event.y - touchDownY).coerceAtLeast(0f),
                        velocityTracker?.yVelocity ?: 0f,
                    )
                } else if (event.actionMasked == MotionEvent.ACTION_UP) {
                    view.performClick()
                }
                velocityTracker?.recycle()
                velocityTracker = null
                isDragging = false
                return true
            }
        }
        return true
    }

    private fun StreamTvPlaybackError.messageRes(): Int = when (this) {
        is StreamTvPlaybackError.NoNetwork -> R.string.player_error_no_network
        is StreamTvPlaybackError.NotFound -> R.string.player_error_not_found
        is StreamTvPlaybackError.NotEntitled -> R.string.player_error_not_entitled
        is StreamTvPlaybackError.UnsupportedFormat -> R.string.player_error_unsupported
        is StreamTvPlaybackError.Unknown -> R.string.player_error_message
    }

    private fun Long.toProgressInt(): Int = coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    private fun Long.asTimestamp(): String {
        val totalSeconds = (coerceAtLeast(0) / 1_000)
        val hours = totalSeconds / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
        else "%02d:%02d".format(minutes, seconds)
    }

    private companion object {
        const val DEFAULT_SPEED = 1f
        const val CONTROLLER_DISPLAY_MILLIS = 5_000L
    }
}
