package com.congnguyencn.kmpstreamtv.feature.home

import androidx.recyclerview.widget.DiffUtil
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeContentUiModel
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeSectionUiModel

internal object HomeSectionDiffCallback : DiffUtil.ItemCallback<HomeSectionUiModel>() {
    override fun areItemsTheSame(
        oldItem: HomeSectionUiModel,
        newItem: HomeSectionUiModel,
    ) = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: HomeSectionUiModel,
        newItem: HomeSectionUiModel,
    ) = oldItem == newItem
}

internal object HomeContentDiffCallback : DiffUtil.ItemCallback<HomeContentUiModel>() {
    override fun areItemsTheSame(
        oldItem: HomeContentUiModel,
        newItem: HomeContentUiModel,
    ) = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: HomeContentUiModel,
        newItem: HomeContentUiModel,
    ) = oldItem == newItem
}
