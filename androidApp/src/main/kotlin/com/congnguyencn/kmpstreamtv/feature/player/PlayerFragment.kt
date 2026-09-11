package com.congnguyencn.kmpstreamtv.feature.player

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.congnguyencn.kmpstreamtv.MainActivity
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.databinding.FragmentPlayerBinding
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeContentUiModel
import com.congnguyencn.streamplayer.StreamTvPlayerManager
import com.congnguyencn.streamplayer.config.StreamTvPlayerConfig
import com.congnguyencn.streamplayer.loadAndPlay
import com.congnguyencn.streamplayer.model.StreamTvPlayerState
import com.congnguyencn.streamplayer.pause
import com.congnguyencn.streamplayer.play
import com.congnguyencn.streamplayer.replay
import com.congnguyencn.streamplayer.seekBack
import com.congnguyencn.streamplayer.seekForward
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal enum class PlayerPresentation { DETAIL, FULLSCREEN, MINI }

/**
 * VOD detail/player overlay hosted by [MainActivity].
 *
 * The overlay itself is `MinimizableView`, ported from `NewMinimizableView` in
 * `ottclouds-android`: portrait shrinks the player into a floating card that can be dragged,
 * pinched and parked in any corner, landscape becomes a controller-only fullscreen player, and
 * system Picture-in-Picture bypasses both. This fragment owns lifecycle, media and the presentation
 * the overlay is asked for; every dimension of the shrink lives in `MinimizableViewState`.
 */
class PlayerFragment : Fragment(R.layout.fragment_player) {
    private var bindingRef: FragmentPlayerBinding? = null
    private val binding get() = requireNotNull(bindingRef)
    private var manager: StreamTvPlayerManager? = null
    private var managerUsesFeedConfig: Boolean? = null
    private var currentMedia: PlayerMedia? = null
    private var loadedMediaKey: String? = null
    private var latestState = StreamTvPlayerState.Initial
    private var stateJob: Job? = null
    private var resumeWhenStarted = false
    private var presentation = PlayerPresentation.DETAIL
    private var isSystemPictureInPicture = false
    private lateinit var detailAdapter: PlayerDetailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentMedia = savedInstanceState?.getBundle(STATE_MEDIA)?.let(PlayerMedia::from)
            ?: arguments?.let(PlayerMedia::from)
        presentation = savedInstanceState
            ?.getString(STATE_PRESENTATION)
            ?.let { runCatching { PlayerPresentation.valueOf(it) }.getOrNull() }
            ?: PlayerPresentation.DETAIL
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        bindingRef = FragmentPlayerBinding.bind(view)
        detailAdapter = PlayerDetailAdapter(::handleDetailAction, ::play)
        binding.detailList.adapter = detailAdapter
        binding.playerView.apply {
            onClose = ::close
            onMinimize = ::minimize
            onPictureInPicture = { (activity as? MainActivity)?.enterPlayerPictureInPicture() }
            onFullscreenToggle = ::toggleFullscreen
            onRetry = ::retry
            onEpisodes = {
                Toast.makeText(requireContext(), R.string.player_episodes_coming_soon, Toast.LENGTH_SHORT).show()
            }
        }
        binding.miniPlaybackController.apply {
            onToggle = binding.playerView::togglePlayback
            onReplay = { manager?.replay() }
            onRewind = { manager?.seekBack() }
            onForward = { manager?.seekForward() }
        }
        binding.minimizableView.apply {
            // Measured rather than assumed, so the resting mini player keeps its border gap above
            // the real bottom navigation instead of the token's estimate of it.
            (activity as? MainActivity)?.bottomBarHeight()?.takeIf { it > 0 }?.let {
                appBottomBarHeightPx = it.toFloat()
            }
            onMinimizedChanged = ::onMinimizedChanged
            onZoomScaleChanged = binding.miniPlaybackController::applyZoomScale
        }
        currentMedia?.let(::startMedia) ?: close()
        setPresentation(presentationForCurrentOrientation(presentation))
        registerBackHandler()
    }

    override fun onStart() {
        super.onStart()
        if (resumeWhenStarted) {
            resumeWhenStarted = false
            manager?.play()
        }
    }

    override fun onStop() {
        if (!isSystemPictureInPicture) {
            resumeWhenStarted = latestState.isPlaying
            manager?.pause()
        }
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        currentMedia?.let { outState.putBundle(STATE_MEDIA, it.toBundle()) }
        outState.putString(STATE_PRESENTATION, presentation.name)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        stateJob?.cancel()
        stateJob = null
        binding.detailList.adapter = null
        binding.playerView.detach()
        bindingRef = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        manager?.close()
        manager = null
        super.onDestroy()
    }

    fun play(content: HomeContentUiModel) {
        play(PlayerMedia.from(content))
    }

    internal fun expand() {
        val minimizable = bindingRef?.minimizableView
        if (minimizable != null && minimizable.isMinimized) {
            // The grow animation reports back through onMinimizedChanged, which is what switches
            // the presentation — driving both from here would flip it a frame early.
            minimizable.maximize()
            return
        }
        setPresentation(presentationForCurrentOrientation(PlayerPresentation.DETAIL))
    }

    internal fun onHostConfigurationChanged() {
        if (presentation != PlayerPresentation.MINI && !isSystemPictureInPicture) {
            presentation = presentationForCurrentOrientation(PlayerPresentation.DETAIL)
        }
        bindingRef?.let {
            it.playerView.setPresentation(presentation)
            // The new window size reaches MinimizableView as a size change, which rebuilds the
            // geometry around it and restores the minimized state on the new dimensions.
            it.minimizableView.setPresentation(presentation)
            (activity as? MainActivity)?.presentPlayer(
                if (isSystemPictureInPicture) PlayerPresentation.FULLSCREEN else presentation,
            )
        }
    }

    internal fun prepareForSystemPictureInPicture(): Rational? {
        currentMedia ?: return null
        if (presentation == PlayerPresentation.MINI || isSystemPictureInPicture) return null
        isSystemPictureInPicture = true
        bindingRef?.let {
            it.playerView.setSystemPictureInPicture(true)
            it.minimizableView.setSystemPictureInPicture(true)
        }
        return Rational(16, 9)
    }

    internal fun pictureInPictureSourceRect(): android.graphics.Rect? {
        val playerView = bindingRef?.playerView ?: return null
        return android.graphics.Rect().takeIf(playerView::getGlobalVisibleRect)
    }

    internal fun onSystemPictureInPictureModeChanged(enabled: Boolean) {
        isSystemPictureInPicture = enabled
        if (!enabled) {
            presentation = presentationForCurrentOrientation(presentation)
        }
        bindingRef?.let {
            it.playerView.setSystemPictureInPicture(enabled)
            it.minimizableView.setSystemPictureInPicture(enabled)
            if (!enabled) {
                it.minimizableView.setPresentation(presentation)
            }
            (activity as? MainActivity)?.presentPlayer(
                if (enabled) PlayerPresentation.FULLSCREEN else presentation,
            )
        }
    }

    internal fun shouldAutoEnterPictureInPicture(): Boolean =
        presentation != PlayerPresentation.MINI && latestState.isPlaying && !isSystemPictureInPicture

    private fun play(media: PlayerMedia) {
        currentMedia = media
        if (bindingRef != null) startMedia(media)
        expand()
    }

    private fun startMedia(media: PlayerMedia) {
        val player = ensureManager(media.isShort)
        binding.playerView.attach(player)
        binding.playerView.bindMedia(media)
        detailAdapter.submit(media, PlayerDemoCatalog.recommendationsFor(media))
        binding.detailList.scrollToPosition(0)
        val mediaKey = "${media.id}|${media.url}"
        if (loadedMediaKey != mediaKey) {
            loadedMediaKey = mediaKey
            latestState = StreamTvPlayerState.Initial
            player.loadAndPlay(media.url.toUri())
        }
        render(latestState)
    }

    private fun ensureManager(useFeedConfig: Boolean): StreamTvPlayerManager {
        val existing = manager
        if (existing != null && managerUsesFeedConfig == useFeedConfig) {
            if (stateJob == null) collectState(existing)
            return existing
        }

        stateJob?.cancel()
        if (bindingRef != null) binding.playerView.detach()
        existing?.close()
        loadedMediaKey = null
        val config = if (useFeedConfig) StreamTvPlayerConfig.Feed else StreamTvPlayerConfig.Tv
        return StreamTvPlayerManager.create(requireContext().applicationContext, config).also { newManager ->
            manager = newManager
            managerUsesFeedConfig = useFeedConfig
            collectState(newManager)
        }
    }

    private fun collectState(player: StreamTvPlayerManager) {
        stateJob =
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    player.playerState.collect { state ->
                        latestState = state
                        render(state)
                    }
                }
            }
    }

    private fun render(state: StreamTvPlayerState) {
        bindingRef?.let {
            it.playerView.render(state)
            it.miniPlaybackController.render(state)
            it.miniPlaybackController.setActionsEnabled(state.playbackError == null)
        }
    }

    private fun retry() {
        currentMedia?.let { manager?.loadAndPlay(it.url.toUri()) }
    }

    private fun minimize() {
        requestPortrait()
        val minimizable = bindingRef?.minimizableView
        if (minimizable != null && !minimizable.isMinimized) {
            minimizable.minimize()
            return
        }
        setPresentation(PlayerPresentation.MINI)
    }

    /** The single seam the overlay reports its own shrink/grow through. */
    private fun onMinimizedChanged(isMinimized: Boolean) {
        setPresentation(
            if (isMinimized) {
                PlayerPresentation.MINI
            } else {
                presentationForCurrentOrientation(PlayerPresentation.DETAIL)
            },
        )
    }

    private fun setPresentation(value: PlayerPresentation) {
        presentation = value
        if (bindingRef == null) return
        binding.playerView.setPresentation(value)
        binding.minimizableView.setPresentation(value)
        (activity as? MainActivity)?.presentPlayer(value)
    }

    private fun presentationForCurrentOrientation(fallback: PlayerPresentation): PlayerPresentation =
        when {
            fallback == PlayerPresentation.MINI -> {
                PlayerPresentation.MINI
            }

            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE -> {
                PlayerPresentation.FULLSCREEN
            }

            else -> {
                PlayerPresentation.DETAIL
            }
        }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun toggleFullscreen() {
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        requireActivity().requestedOrientation =
            if (landscape) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
    }

    private fun handleDetailAction(action: PlayerDetailAction) {
        val message =
            when (action) {
                PlayerDetailAction.WATCH_LATER -> R.string.player_action_watch_later
                PlayerDetailAction.PRODUCTS -> R.string.player_action_products
                PlayerDetailAction.LIKE -> R.string.player_action_like
                PlayerDetailAction.COMMENT -> R.string.player_action_comment
                PlayerDetailAction.SHARE -> R.string.player_action_share
            }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun close() {
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        (activity as? MainActivity)?.closePlayer(this)
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun requestPortrait() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun registerBackHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.playerView.dismissSettings()) return
                    when {
                        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE -> {
                            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                        }

                        presentation == PlayerPresentation.DETAIL -> {
                            minimize()
                        }

                        else -> {
                            close()
                        }
                    }
                }
            },
        )
    }

    companion object {
        internal const val TAG = "player_fragment"
        private const val STATE_MEDIA = "player_media"
        private const val STATE_PRESENTATION = "player_presentation"

        fun newInstance(content: HomeContentUiModel) =
            PlayerFragment().apply {
                arguments = PlayerMedia.from(content).toBundle()
            }
    }
}
