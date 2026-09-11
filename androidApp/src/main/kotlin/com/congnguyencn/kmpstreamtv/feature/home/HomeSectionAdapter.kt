package com.congnguyencn.kmpstreamtv.feature.home

import android.view.ViewGroup
import com.congnguyencn.kmpstreamtv.core.ui.recyclerview.BaseListAdapter
import com.congnguyencn.kmpstreamtv.core.ui.recyclerview.BindableViewHolder
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeContentUiModel
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeSectionPresentation
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeSectionUiModel

/** Mirrors HomePageAdapter: every section id receives a unique view type and its exact outer holder. */
internal class HomeSectionAdapter(
    private val onContentClick: (HomeSectionUiModel, HomeContentUiModel) -> Unit,
) : BaseListAdapter<HomeSectionUiModel>(HomeSectionDiffCallback) {
    private val sectionTypeIds = mutableListOf<String>()

    override fun getItemViewType(position: Int): Int {
        val sectionId = getItem(position).id
        return sectionTypeIds.indexOf(sectionId).takeIf { it >= 0 } ?: sectionTypeIds.size.also {
            sectionTypeIds += sectionId
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BindableViewHolder<HomeSectionUiModel> {
        val section = sectionTypeIds.getOrNull(viewType)?.let { id -> currentList.firstOrNull { it.id == id } }
        val sectionClick: (HomeContentUiModel) -> Unit = { content ->
            section?.let { onContentClick(it, content) }
        }
        return when (section?.presentation) {
            HomeSectionPresentation.TopTen -> LayoutBackgroundViewHolder(parent, sectionClick)
            HomeSectionPresentation.HighlightTall -> LayoutHighlightTallViewHolder(parent, sectionClick)
            HomeSectionPresentation.HighlightWide -> LayoutHighlightWideViewHolder(parent, sectionClick)
            HomeSectionPresentation.Story -> LayoutStoryViewHolder(parent, sectionClick)
            HomeSectionPresentation.ContinueWatching -> LayoutWatchingViewHolder(parent, sectionClick)
            HomeSectionPresentation.MiniApps -> LayoutMiniAppViewHolder(parent, sectionClick)
            else -> LayoutGeneralViewHolder(parent, sectionClick)
        }
    }
}
