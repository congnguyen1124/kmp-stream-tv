package com.congnguyencn.kmpstreamtv.feature.story

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.net.toUri
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.ui.AspectRatioFrameLayout
import coil3.load
import coil3.request.crossfade
import com.congnguyencn.kmpstreamtv.MainActivity
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.databinding.FragmentStoryGroupBinding
import com.congnguyencn.kmpstreamtv.feature.short.presentation.ShortItemUiModel
import com.congnguyencn.kmpstreamtv.feature.short.presentation.StoryGroupUiState
import com.congnguyencn.kmpstreamtv.feature.short.presentation.StoryGroupViewModel
import com.congnguyencn.streamplayer.StreamTvPlayerManager
import com.congnguyencn.streamplayer.config.StreamTvPlayerConfig
import com.congnguyencn.streamplayer.exoPlayer
import com.congnguyencn.streamplayer.loadAndPlay
import com.congnguyencn.streamplayer.model.StreamTvPlaybackState
import com.congnguyencn.streamplayer.pause
import com.congnguyencn.streamplayer.play
import kotlinx.coroutines.launch
import org.koin.android.ext.android.getKoin

class StoryGroupFragment : Fragment(R.layout.fragment_story_group) {
    private var bindingRef: FragmentStoryGroupBinding? = null
    private val binding get() = requireNotNull(bindingRef)
    private var manager: StreamTvPlayerManager? = null
    private var loadedItemId: String? = null
    private var handledEndedItemId: String? = null
    private var resumeAfterHold = false

    private val viewModel: StoryGroupViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = getKoin().get<StoryGroupViewModel>() as T
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        bindingRef = FragmentStoryGroupBinding.bind(view)
        (activity as? MainActivity)?.presentStory(true)
        val player =
            StreamTvPlayerManager.create(
                context = requireContext().applicationContext,
                config = StreamTvPlayerConfig.Feed,
            )
        manager = player
        binding.mediaSurface.apply {
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            setShutterBackgroundColor(context.getColor(R.color.black))
            this.player = player.exoPlayer()
        }
        binding.close.setOnClickListener { close() }
        binding.retry.setOnClickListener {
            arguments?.getString(ARG_INITIAL_ID)?.let(viewModel::load)
        }
        binding.leftTouchTarget.apply {
            setOnClickListener { movePrevious() }
            setOnHoldListener(::pauseForHold)
            setOnHoldReleaseListener(::resumeAfterHold)
        }
        binding.rightTouchTarget.apply {
            setOnClickListener { moveNext() }
            setOnHoldListener(::pauseForHold)
            setOnHoldReleaseListener(::resumeAfterHold)
        }
        binding.reactions.children.filterIsInstance<android.widget.TextView>().forEach { reaction ->
            reaction.setOnClickListener {
                reaction
                    .animate()
                    .scaleX(1.35f)
                    .scaleY(1.35f)
                    .setDuration(120L)
                    .withEndAction {
                        reaction
                            .animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(120L)
                            .start()
                    }.start()
                showMessage(R.string.story_reaction_message)
            }
        }
        binding.share.setOnClickListener { showMessage(R.string.short_share_message) }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = close()
            },
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch {
                    player.playerState.collect { state ->
                        binding.loadingPlayer.isVisible = state.playbackState is StreamTvPlaybackState.Buffering
                        binding.playerErrorGroup.isVisible = state.error != null
                        val duration = state.duration.inWholeMilliseconds
                        val fraction =
                            if (duration > 0) state.position.inWholeMilliseconds.toFloat() / duration else 0f
                        binding.progress.updateProgress(fraction)
                        if (state.playbackState is StreamTvPlaybackState.Ended) {
                            val itemId = loadedItemId
                            if (itemId != null && handledEndedItemId != itemId) {
                                handledEndedItemId = itemId
                                moveNext()
                            }
                        }
                    }
                }
            }
        }
        arguments?.getString(ARG_INITIAL_ID)?.let(viewModel::load) ?: close()
    }

    override fun onResume() {
        super.onResume()
        if (loadedItemId != null) manager?.play()
    }

    override fun onPause() {
        manager?.pause()
        super.onPause()
    }

    override fun onDestroyView() {
        binding.mediaSurface.player = null
        manager?.close()
        manager = null
        bindingRef = null
        super.onDestroyView()
    }

    private fun render(state: StoryGroupUiState) =
        with(binding) {
            val hasContent = state.items.isNotEmpty()
            loading.isVisible = state.isLoading && !hasContent
            errorGroup.isVisible = !state.isLoading && state.errorMessage != null && !hasContent
            errorMessage.text = state.errorMessage
            storyChrome.isVisible = hasContent
            if (!hasContent || state.activeIndex !in state.items.indices) return@with
            progress.setup(state.items.size, state.activeIndex)
            bindStory(state.items[state.activeIndex])
        }

    private fun bindStory(item: ShortItemUiModel) =
        with(binding) {
            profileAvatar.load(item.providerAvatarUrl) { crossfade(true) }
            providerName.text = item.providerName
            publishedTime.text = item.publishedLabel
            artwork.load(item.thumbnailUrl) { crossfade(true) }
            if (loadedItemId != item.id) {
                loadedItemId = item.id
                handledEndedItemId = null
                playerErrorGroup.isVisible = false
                manager?.loadAndPlay(item.videoUrl.toUri())
            }
        }

    private fun movePrevious() {
        if (!viewModel.moveToPrevious()) {
            manager?.let { player ->
                loadedItemId?.let { currentId ->
                    handledEndedItemId = null
                    viewModel.currentState.items
                        .firstOrNull { it.id == currentId }
                        ?.let { player.loadAndPlay(it.videoUrl.toUri()) }
                }
            }
        }
    }

    private fun moveNext() {
        if (!viewModel.moveToNext()) close()
    }

    private fun pauseForHold() {
        resumeAfterHold = manager?.playerState?.value?.isPlaying == true
        manager?.pause()
    }

    private fun resumeAfterHold() {
        if (resumeAfterHold) manager?.play()
        resumeAfterHold = false
    }

    private fun close() {
        (activity as? MainActivity)?.closeStory(this)
    }

    private fun showMessage(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val TAG = "story_group_fragment"
        private const val ARG_INITIAL_ID = "story_initial_id"

        fun newInstance(initialId: String) =
            StoryGroupFragment().apply {
                arguments = Bundle().apply { putString(ARG_INITIAL_ID, initialId) }
            }
    }
}
