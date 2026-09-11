package com.congnguyencn.kmpstreamtv.feature.short

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.congnguyencn.kmpstreamtv.MainActivity
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.databinding.FragmentShortMediaBinding
import com.congnguyencn.kmpstreamtv.feature.short.presentation.ShortItemUiModel
import com.congnguyencn.kmpstreamtv.feature.short.presentation.ShortUiState
import com.congnguyencn.kmpstreamtv.feature.short.presentation.ShortViewModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.getKoin

class ShortMediaFragment : Fragment(R.layout.fragment_short_media) {
    private var bindingRef: FragmentShortMediaBinding? = null
    private val binding get() = requireNotNull(bindingRef)
    private var adapter: ShortMediaAdapter? = null
    private val snapHelper = PagerSnapHelper()
    private var pendingInitialId: String? = null
    private var hostResumed = false

    private val viewModel: ShortViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = getKoin().get<ShortViewModel>() as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingInitialId = savedInstanceState?.getString(STATE_INITIAL_ID) ?: arguments?.getString(ARG_INITIAL_ID)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        bindingRef = FragmentShortMediaBinding.bind(view)
        val shortAdapter = ShortMediaAdapter(requireContext(), ::handleAction)
        adapter = shortAdapter
        binding.shortList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = shortAdapter
            itemAnimator = null
            setHasFixedSize(true)
            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(
                        recyclerView: RecyclerView,
                        newState: Int,
                    ) {
                        if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                            shortAdapter.onScrollStarted()
                        }
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) updateActivePage()
                    }
                },
            )
        }
        snapHelper.attachToRecyclerView(binding.shortList)
        binding.retry.setOnClickListener { viewModel.reload() }
        binding.search.setOnClickListener { showMessage(R.string.search_coming_soon) }
        binding.profile.setOnClickListener { showMessage(R.string.profile_coming_soon) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
        pendingInitialId?.let(viewModel::selectById)
    }

    override fun onResume() {
        super.onResume()
        hostResumed = true
        if (!isHidden) adapter?.start()
    }

    override fun onPause() {
        adapter?.stop()
        hostResumed = false
        super.onPause()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            adapter?.stop()
        } else if (hostResumed) {
            adapter?.start()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pendingInitialId?.let { outState.putString(STATE_INITIAL_ID, it) }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        binding.shortList.adapter = null
        adapter?.release()
        adapter = null
        bindingRef = null
        super.onDestroyView()
    }

    fun show(initialId: String) {
        pendingInitialId = initialId
        if (bindingRef != null) viewModel.selectById(initialId)
    }

    private fun render(state: ShortUiState) =
        with(binding) {
            val hasItems = state.items.isNotEmpty()
            loading.isVisible = state.isLoading && !hasItems
            shortList.isVisible = hasItems
            errorGroup.isVisible = !state.isLoading && state.errorMessage != null && !hasItems
            errorMessage.text = state.errorMessage
            loadingMore.isVisible = state.isLoadingMore
            adapter?.submitList(state.items) {
                if (state.activeIndex in state.items.indices) {
                    shortList.scrollToPosition(state.activeIndex)
                    adapter?.setActivePosition(state.activeIndex)
                    pendingInitialId = null
                }
            }
        }

    private fun updateActivePage() {
        val layoutManager = binding.shortList.layoutManager ?: return
        val snap = snapHelper.findSnapView(layoutManager) ?: return
        val position = layoutManager.getPosition(snap)
        adapter?.setActivePosition(position)
        viewModel.select(position)
    }

    private fun handleAction(
        item: ShortItemUiModel,
        action: ShortAction,
    ) {
        when (action) {
            ShortAction.FOLLOW -> viewModel.toggleFollow(item.providerId)
            ShortAction.LIKE -> viewModel.toggleLike(item.id)
            ShortAction.PROFILE -> showMessage(getString(R.string.short_profile_message, item.providerName))
            ShortAction.COMMENT -> showMessage(R.string.short_comment_message)
            ShortAction.SHARE -> showMessage(R.string.short_share_message)
            ShortAction.MORE -> showMessage(R.string.short_more_message)
        }
    }

    private fun showMessage(messageRes: Int) = showMessage(getString(messageRes))

    private fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val ARG_INITIAL_ID = "short_initial_id"
        private const val STATE_INITIAL_ID = "short_pending_initial_id"

        fun newInstance(initialId: String? = null) =
            ShortMediaFragment().apply {
                arguments = Bundle().apply { initialId?.let { putString(ARG_INITIAL_ID, it) } }
            }
    }
}
