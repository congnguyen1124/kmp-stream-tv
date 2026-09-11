package com.congnguyencn.kmpstreamtv.feature.player

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal enum class PlayerPresentation { DETAIL, FULLSCREEN, MINI }

/**
 * VOD detail/player overlay hosted by [MainActivity]. Portrait mirrors VodDetailFragment, landscape
 * becomes a controller-only fullscreen player, and a downward drag collapses it above bottom nav.
 */
class PlayerFragment : Fragment(R.layout.fragment_player) {
    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = requireNotNull(_binding)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlayerBinding.bind(view)
        detailAdapter = PlayerDetailAdapter(::handleDetailAction, ::play)
        binding.detailList.adapter = detailAdapter
        binding.detailClose.setOnClickListener { close() }
        binding.playerView.apply {
            onClose = ::close
            onMinimize = ::minimize
            onPictureInPicture = { (activity as? MainActivity)?.enterPlayerPictureInPicture() }
            onExpand = ::expand
            onFullscreenToggle = ::toggleFullscreen
            onRetry = ::retry
            onEpisodes = {
                Toast.makeText(requireContext(), R.string.player_episodes_coming_soon, Toast.LENGTH_SHORT).show()
            }
            onDrag = ::renderDrag
            onDragEnd = ::finishDrag
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
        _binding = null
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
        setPresentation(presentationForCurrentOrientation(PlayerPresentation.DETAIL))
    }

    internal fun onHostConfigurationChanged() {
        if (presentation != PlayerPresentation.MINI && !isSystemPictureInPicture) {
            presentation = presentationForCurrentOrientation(PlayerPresentation.DETAIL)
        }
        _binding?.let {
            applyPresentationLayout()
            it.playerView.setPresentation(presentation)
            (activity as? MainActivity)?.presentPlayer(
                if (isSystemPictureInPicture) PlayerPresentation.FULLSCREEN else presentation,
            )
        }
    }

    internal fun prepareForSystemPictureInPicture(): Rational? {
        currentMedia ?: return null
        if (presentation == PlayerPresentation.MINI || isSystemPictureInPicture) return null
        isSystemPictureInPicture = true
        _binding?.playerView?.setSystemPictureInPicture(true)
        _binding?.let { applyPresentationLayout() }
        return Rational(16, 9)
    }

    internal fun pictureInPictureSourceRect(): android.graphics.Rect? {
        val playerView = _binding?.playerView ?: return null
        return android.graphics.Rect().takeIf(playerView::getGlobalVisibleRect)
    }

    internal fun onSystemPictureInPictureModeChanged(enabled: Boolean) {
        isSystemPictureInPicture = enabled
        _binding?.playerView?.setSystemPictureInPicture(enabled)
        if (!enabled) {
            presentation = presentationForCurrentOrientation(presentation)
        }
        _binding?.let {
            applyPresentationLayout()
            (activity as? MainActivity)?.presentPlayer(
                if (enabled) PlayerPresentation.FULLSCREEN else presentation,
            )
        }
    }

    internal fun shouldAutoEnterPictureInPicture(): Boolean =
        presentation != PlayerPresentation.MINI && latestState.isPlaying && !isSystemPictureInPicture

    private fun play(media: PlayerMedia) {
        currentMedia = media
        if (_binding != null) startMedia(media)
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
        } else {
            binding.playerView.render(latestState)
        }
    }

    private fun ensureManager(useFeedConfig: Boolean): StreamTvPlayerManager {
        val existing = manager
        if (existing != null && managerUsesFeedConfig == useFeedConfig) {
            if (stateJob == null) collectState(existing)
            return existing
        }

        stateJob?.cancel()
        if (_binding != null) binding.playerView.detach()
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
        stateJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                player.playerState.collect { state ->
                    latestState = state
                    _binding?.playerView?.render(state)
                }
            }
        }
    }

    private fun retry() {
        currentMedia?.let { manager?.loadAndPlay(it.url.toUri()) }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun minimize() {
        requestPortrait()
        setPresentation(PlayerPresentation.MINI)
    }

    private fun setPresentation(value: PlayerPresentation) {
        presentation = value
        if (_binding == null) return
        resetDragImmediately()
        binding.playerView.setPresentation(value)
        applyPresentationLayout()
        (activity as? MainActivity)?.presentPlayer(value)
    }

    private fun applyPresentationLayout() = with(binding) {
        val showDetail = presentation == PlayerPresentation.DETAIL && !isSystemPictureInPicture
        val fillParent = !showDetail
        detailTopBar.isVisible = showDetail
        detailList.isVisible = showDetail
        detailShadow.isVisible = showDetail
        playerView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            dimensionRatio = if (showDetail) "H,16:9" else null
            topToBottom = if (showDetail) R.id.detailTopBar else ConstraintSet.UNSET
            topToTop = if (fillParent) ConstraintSet.PARENT_ID else ConstraintSet.UNSET
            bottomToBottom = if (fillParent) ConstraintSet.PARENT_ID else ConstraintSet.UNSET
        }
    }

    private fun presentationForCurrentOrientation(fallback: PlayerPresentation): PlayerPresentation =
        when {
            fallback == PlayerPresentation.MINI -> PlayerPresentation.MINI
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ->
                PlayerPresentation.FULLSCREEN
            else -> PlayerPresentation.DETAIL
        }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun toggleFullscreen() {
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        requireActivity().requestedOrientation = if (landscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    private fun renderDrag(distanceY: Float) {
        if (presentation != PlayerPresentation.DETAIL) return
        binding.root.animate().cancel()
        binding.root.translationY = distanceY
        val progress = (distanceY / (binding.root.height * DRAG_DISMISS_FRACTION)).coerceIn(0f, 1f)
        binding.root.alpha = 1f - progress * DRAG_MAX_FADE
    }

    private fun finishDrag(distanceY: Float, velocityY: Float) {
        val shouldMinimize = distanceY >= binding.root.height * DRAG_DISMISS_FRACTION ||
            velocityY >= DRAG_MIN_VELOCITY
        if (shouldMinimize) {
            binding.root.animate()
                .translationY(binding.root.height * DRAG_EXIT_TRANSLATION)
                .alpha(0.82f)
                .setDuration(DRAG_ANIMATION_MILLIS)
                .withEndAction {
                    resetDragImmediately()
                    minimize()
                }
                .start()
        } else {
            binding.root.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(DRAG_ANIMATION_MILLIS)
                .start()
        }
    }

    private fun resetDragImmediately() {
        _binding?.root?.animate()?.cancel()
        _binding?.root?.translationY = 0f
        _binding?.root?.alpha = 1f
    }

    private fun handleDetailAction(action: PlayerDetailAction) {
        val message = when (action) {
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
                    when {
                        binding.playerView.dismissSettings() -> Unit
                        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE -> {
                            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                        }
                        presentation == PlayerPresentation.DETAIL -> minimize()
                        else -> close()
                    }
                }
            },
        )
    }

    companion object {
        internal const val TAG = "player_fragment"
        private const val STATE_MEDIA = "player_media"
        private const val STATE_PRESENTATION = "player_presentation"
        private const val DRAG_DISMISS_FRACTION = 0.22f
        private const val DRAG_EXIT_TRANSLATION = 0.35f
        private const val DRAG_MAX_FADE = 0.18f
        private const val DRAG_MIN_VELOCITY = 1_250f
        private const val DRAG_ANIMATION_MILLIS = 180L

        fun newInstance(content: HomeContentUiModel) = PlayerFragment().apply {
            arguments = PlayerMedia.from(content).toBundle()
        }
    }
}
