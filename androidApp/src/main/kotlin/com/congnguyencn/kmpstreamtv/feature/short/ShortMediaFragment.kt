package com.congnguyencn.kmpstreamtv.feature.short

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
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
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.databinding.FragmentShortMediaBinding
import com.congnguyencn.kmpstreamtv.feature.profile.UserProfileActivity
import com.congnguyencn.kmpstreamtv.feature.short.presentation.ShortItemUiModel
import com.congnguyencn.kmpstreamtv.feature.short.presentation.ShortUiState
import com.congnguyencn.kmpstreamtv.feature.short.presentation.ShortViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        binding.search.setOnClickListener { showShortSearch() }
        binding.profile.setOnClickListener {
            startActivity(Intent(requireContext(), UserProfileActivity::class.java))
        }
        childFragmentManager.setFragmentResultListener(
            ShortCommentBottomSheetFragment.RESULT_KEY,
            viewLifecycleOwner,
        ) { _, result ->
            val itemId =
                result.getString(ShortCommentBottomSheetFragment.RESULT_ITEM_ID) ?: return@setFragmentResultListener
            val comment =
                result.getString(ShortCommentBottomSheetFragment.RESULT_COMMENT) ?: return@setFragmentResultListener
            viewModel.addComment(itemId, comment)
        }
        childFragmentManager.setFragmentResultListener(
            ShortActionsBottomSheetFragment.RESULT_KEY,
            viewLifecycleOwner,
        ) { _, result ->
            val itemId =
                result.getString(ShortActionsBottomSheetFragment.RESULT_ITEM_ID) ?: return@setFragmentResultListener
            val action =
                result
                    .getString(ShortActionsBottomSheetFragment.RESULT_ACTION)
                    ?.let { runCatching { ShortMoreAction.valueOf(it) }.getOrNull() }
                    ?: return@setFragmentResultListener
            viewModel.currentState.items.firstOrNull { it.id == itemId }?.let { item ->
                handleMoreAction(item, action)
            }
        }

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
            ShortAction.PROFILE -> showProviderProfile(item)
            ShortAction.COMMENT -> showComments(item)
            ShortAction.SHARE -> share(item)
            ShortAction.MORE -> showMoreActions(item)
        }
    }

    private fun showShortSearch() {
        val input =
            EditText(requireContext()).apply {
                hint = getString(R.string.search_shorts_hint)
                isSingleLine = true
                setTextColor(context.getColor(R.color.white))
                setHintTextColor(context.getColor(R.color.silverChalice))
            }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.search_shorts)
            .setView(input)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.search) { _, _ ->
                val query =
                    input.text
                        ?.toString()
                        ?.trim()
                        .orEmpty()
                val match =
                    viewModel.currentState.items.firstOrNull { item ->
                        item.title.contains(query, ignoreCase = true) ||
                            item.providerName.contains(query, ignoreCase = true)
                    }
                if (query.isEmpty() || match == null) {
                    showMessage(R.string.no_short_search_result)
                } else {
                    scrollToShort(match.id)
                }
            }.show()
    }

    private fun showProviderProfile(selectedItem: ShortItemUiModel) {
        val providerShorts = viewModel.currentState.items.filter { it.providerId == selectedItem.providerId }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(selectedItem.providerName)
            .setItems(providerShorts.map { it.title }.toTypedArray()) { _, position ->
                providerShorts.getOrNull(position)?.let { scrollToShort(it.id) }
            }.setNegativeButton(R.string.close, null)
            .setPositiveButton(
                if (selectedItem.isFollowingProvider) R.string.following else R.string.follow,
            ) { _, _ ->
                viewModel.toggleFollow(selectedItem.providerId)
            }.show()
    }

    private fun showComments(item: ShortItemUiModel) {
        (
            childFragmentManager.findFragmentByTag(
                ShortCommentBottomSheetFragment.TAG,
            ) as? ShortCommentBottomSheetFragment
        )?.dismiss()
        ShortCommentBottomSheetFragment
            .newInstance(
                itemId = item.id,
                count = item.commentCount,
                comments = viewModel.commentsFor(item.id),
            ).show(childFragmentManager, ShortCommentBottomSheetFragment.TAG)
    }

    private fun share(item: ShortItemUiModel) {
        val message = "${item.title}\n${item.videoUrl}"
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, item.title)
                putExtra(Intent.EXTRA_TEXT, message)
            }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun showMoreActions(item: ShortItemUiModel) {
        (
            childFragmentManager.findFragmentByTag(
                ShortActionsBottomSheetFragment.TAG,
            ) as? ShortActionsBottomSheetFragment
        )?.dismiss()
        ShortActionsBottomSheetFragment
            .newInstance(item.id)
            .show(childFragmentManager, ShortActionsBottomSheetFragment.TAG)
    }

    private fun handleMoreAction(
        item: ShortItemUiModel,
        action: ShortMoreAction,
    ) {
        when (action) {
            ShortMoreAction.COPY_LINK -> {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(item.title, item.videoUrl))
                showMessage(R.string.link_copied)
            }

            ShortMoreAction.NOT_INTERESTED -> {
                val items = viewModel.currentState.items
                val currentIndex = items.indexOfFirst { it.id == item.id }
                val nextIndex = (currentIndex + 1).coerceAtMost(items.lastIndex)
                if (nextIndex in items.indices) {
                    binding.shortList.smoothScrollToPosition(nextIndex)
                    viewModel.select(nextIndex)
                }
                showMessage(R.string.not_interested_short_message)
            }

            ShortMoreAction.REPORT -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.report_short_title)
                    .setMessage(R.string.report_short_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.report) { _, _ -> showMessage(R.string.report_sent) }
                    .show()
            }
        }
    }

    private fun scrollToShort(itemId: String) {
        viewModel.selectById(itemId)
        val position = viewModel.currentState.items.indexOfFirst { it.id == itemId }
        if (position >= 0) binding.shortList.smoothScrollToPosition(position)
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
