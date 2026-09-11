package com.congnguyencn.kmpstreamtv.feature.home

import android.view.LayoutInflater
import android.view.ViewGroup
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.core.ui.recyclerview.BindableViewHolder
import com.congnguyencn.kmpstreamtv.databinding.ItemLayoutBackgroundBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemLayoutBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemLayoutHighlightTallBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemLayoutHighlightWideBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemLayoutMiniAppBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemLayoutStoryBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemLayoutWatchingBinding
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeContentUiModel
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeSectionUiModel
import com.congnguyencn.kmpstreamtv.feature.home.widget.layout.HomeLayoutItemClickable

abstract class LayoutViewHolder(
    parent: ViewGroup,
    layoutId: Int,
    onContentClick: (HomeContentUiModel) -> Unit,
) : BindableViewHolder<HomeSectionUiModel>(
        LayoutInflater.from(parent.context).inflate(layoutId, parent, false),
    ) {
    init {
        (itemView as? HomeLayoutItemClickable)?.setOnContentClickListener(onContentClick)
    }
}

class LayoutGeneralViewHolder(
    parent: ViewGroup,
    onContentClick: (HomeContentUiModel) -> Unit,
) : LayoutViewHolder(parent, R.layout.item_layout, onContentClick) {
    private val binding = ItemLayoutBinding.bind(itemView)

    override fun bind(item: HomeSectionUiModel) = binding.root.bindLayout(item)
}

class LayoutStoryViewHolder(
    parent: ViewGroup,
    onContentClick: (HomeContentUiModel) -> Unit,
) : LayoutViewHolder(parent, R.layout.item_layout_story, onContentClick) {
    private val binding = ItemLayoutStoryBinding.bind(itemView)

    override fun bind(item: HomeSectionUiModel) = binding.root.bindLayout(item)
}

class LayoutWatchingViewHolder(
    parent: ViewGroup,
    onContentClick: (HomeContentUiModel) -> Unit,
) : LayoutViewHolder(parent, R.layout.item_layout_watching, onContentClick) {
    private val binding = ItemLayoutWatchingBinding.bind(itemView)

    override fun bind(item: HomeSectionUiModel) = binding.root.bindLayout(item)
}

class LayoutBackgroundViewHolder(
    parent: ViewGroup,
    onContentClick: (HomeContentUiModel) -> Unit,
) : LayoutViewHolder(parent, R.layout.item_layout_background, onContentClick) {
    private val binding = ItemLayoutBackgroundBinding.bind(itemView)

    override fun bind(item: HomeSectionUiModel) = binding.root.bindLayout(item)
}

class LayoutHighlightWideViewHolder(
    parent: ViewGroup,
    onContentClick: (HomeContentUiModel) -> Unit,
) : LayoutViewHolder(parent, R.layout.item_layout_highlight_wide, onContentClick) {
    private val binding = ItemLayoutHighlightWideBinding.bind(itemView)

    override fun bind(item: HomeSectionUiModel) = binding.root.bindLayout(item)
}

class LayoutHighlightTallViewHolder(
    parent: ViewGroup,
    onContentClick: (HomeContentUiModel) -> Unit,
) : LayoutViewHolder(parent, R.layout.item_layout_highlight_tall, onContentClick) {
    private val binding = ItemLayoutHighlightTallBinding.bind(itemView)

    override fun bind(item: HomeSectionUiModel) = binding.root.bindLayout(item)

    override fun bind(
        item: HomeSectionUiModel,
        position: Int,
    ) {
        binding.root.bindLayout(item, position == 0)
    }
}

class LayoutMiniAppViewHolder(
    parent: ViewGroup,
    onContentClick: (HomeContentUiModel) -> Unit,
) : LayoutViewHolder(parent, R.layout.item_layout_mini_app, onContentClick) {
    private val binding = ItemLayoutMiniAppBinding.bind(itemView)

    override fun bind(item: HomeSectionUiModel) = binding.root.bindLayout(item)
}
