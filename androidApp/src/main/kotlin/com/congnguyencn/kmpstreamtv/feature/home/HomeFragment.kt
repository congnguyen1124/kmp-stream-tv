package com.congnguyencn.kmpstreamtv.feature.home

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.congnguyencn.kmpstreamtv.MainActivity
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.databinding.FragmentHomeBinding
import com.congnguyencn.kmpstreamtv.feature.home.presentation.HomeUiState
import com.congnguyencn.kmpstreamtv.feature.home.presentation.HomeViewModel
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeSectionPresentation
import kotlinx.coroutines.launch
import org.koin.android.ext.android.getKoin

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var bindingRef: FragmentHomeBinding? = null
    private val binding get() = requireNotNull(bindingRef)
    private var homeAdapter: HomeSectionAdapter? = null
    private val viewModel: HomeViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = getKoin().get<HomeViewModel>() as T
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        bindingRef = FragmentHomeBinding.bind(view)
        val sectionAdapter =
            HomeSectionAdapter { section, content ->
                val activity = activity as? MainActivity ?: return@HomeSectionAdapter
                when {
                    section.presentation == HomeSectionPresentation.Story -> activity.openStory(content.id)
                    content.isShort -> activity.openShort(content.id)
                    else -> activity.openPlayer(content)
                }
            }
        homeAdapter = sectionAdapter

        binding.rcvHomepage.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sectionAdapter
            setHasFixedSize(false)
            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(
                        recyclerView: RecyclerView,
                        dx: Int,
                        dy: Int,
                    ) {
                        (parentFragment as? HomeTabFragment)
                            ?.updateToolbarForScroll(recyclerView.computeVerticalScrollOffset())
                    }
                },
            )
        }
        binding.swipeRefresh.apply {
            setColorSchemeResources(R.color.heliotrope)
            setProgressBackgroundColorSchemeResource(R.color.shark)
            val topOffset = resources.getDimensionPixelSize(R.dimen.home_content_padding_top)
            val endOffset = topOffset + resources.getDimensionPixelSize(R.dimen.margin_4x)
            setProgressViewOffset(false, topOffset, endOffset)
            setOnRefreshListener(viewModel::loadHome)
        }
        binding.retry.setOnClickListener { viewModel.loadHome() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: HomeUiState) =
        with(binding) {
            val hasContent = state.sections.isNotEmpty()
            swipeRefresh.isRefreshing = state.isLoading && hasContent
            loading.isVisible = state.isLoading && !hasContent
            rcvHomepage.isVisible = hasContent && state.errorMessage == null
            errorGroup.isVisible = !state.isLoading && state.errorMessage != null
            errorMessage.text = state.errorMessage
            homeAdapter?.submitList(state.sections)
        }

    override fun onDestroyView() {
        binding.rcvHomepage.adapter = null
        homeAdapter = null
        bindingRef = null
        super.onDestroyView()
    }
}
