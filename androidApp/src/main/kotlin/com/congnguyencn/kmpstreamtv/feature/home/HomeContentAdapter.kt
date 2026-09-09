package com.congnguyencn.kmpstreamtv.feature.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import coil3.load
import coil3.request.crossfade
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.core.ui.dp
import com.congnguyencn.kmpstreamtv.core.ui.recyclerview.BaseListAdapter
import com.congnguyencn.kmpstreamtv.core.ui.recyclerview.BindableViewHolder
import com.congnguyencn.kmpstreamtv.databinding.ItemHomeBannerBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemHomeCardCircleBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemHomeCardHighlightTallBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemHomeCardLandscapeBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemHomeCardMiniAppBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemHomeCardPortraitBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemHomeCardShortBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemHomeCardStoryBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemHomeCardTopTenBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemHomeCardWatchingBinding
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeContentUiModel
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeSectionUiModel

internal class HomeContentAdapter(
    private val style: HomeContentStyle,
    private val onContentClick: (HomeContentUiModel) -> Unit,
) : BaseListAdapter<HomeContentUiModel>(HomeContentDiffCallback) {
    private var showsRanking = false

    fun submitSection(section: HomeSectionUiModel) {
        val rankingChanged = showsRanking != section.showsRanking
        showsRanking = section.showsRanking
        submitList(section.items) {
            if (rankingChanged && itemCount > 0) notifyItemRangeChanged(0, itemCount)
        }
    }

    override fun getItemViewType(position: Int): Int = style.ordinal

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BindableViewHolder<HomeContentUiModel> {
        val inflater = LayoutInflater.from(parent.context)
        return when (HomeContentStyle.entries[viewType]) {
            HomeContentStyle.HighlightWide -> BannerViewHolder(
                ItemHomeBannerBinding.inflate(inflater, parent, false),
                onContentClick,
            )
            HomeContentStyle.HighlightTall -> HighlightTallViewHolder(
                ItemHomeCardHighlightTallBinding.inflate(inflater, parent, false),
                onContentClick,
            )
            HomeContentStyle.Landscape -> LandscapeViewHolder(
                ItemHomeCardLandscapeBinding.inflate(inflater, parent, false),
                onContentClick,
                ::rankingFor,
            )
            HomeContentStyle.Portrait -> PortraitViewHolder(
                ItemHomeCardPortraitBinding.inflate(inflater, parent, false),
                onContentClick,
                ::rankingFor,
            )
            HomeContentStyle.TopTen -> TopTenViewHolder(
                ItemHomeCardTopTenBinding.inflate(inflater, parent, false),
                onContentClick,
                ::rankingFor,
            )
            HomeContentStyle.Story -> StoryViewHolder(
                ItemHomeCardStoryBinding.inflate(inflater, parent, false),
                onContentClick,
            )
            HomeContentStyle.Short -> ShortViewHolder(
                ItemHomeCardShortBinding.inflate(inflater, parent, false),
                onContentClick,
            )
            HomeContentStyle.Circle -> CircleViewHolder(
                ItemHomeCardCircleBinding.inflate(inflater, parent, false),
                onContentClick,
            )
            HomeContentStyle.ContinueWatching -> WatchingViewHolder(
                ItemHomeCardWatchingBinding.inflate(inflater, parent, false),
                onContentClick,
            )
            HomeContentStyle.MiniApp -> MiniAppViewHolder(
                ItemHomeCardMiniAppBinding.inflate(inflater, parent, false),
                onContentClick,
            )
        }
    }

    private fun rankingFor(item: HomeContentUiModel): String? =
        if (showsRanking) (currentList.indexOf(item) + 1).toString() else null
}

internal enum class HomeContentStyle {
    HighlightWide,
    HighlightTall,
    Landscape,
    Portrait,
    TopTen,
    Story,
    Short,
    Circle,
    ContinueWatching,
    MiniApp,
}

private abstract class ClickableContentViewHolder(
    root: View,
    onClick: (HomeContentUiModel) -> Unit,
) : BindableViewHolder<HomeContentUiModel>(root) {
    protected var boundItem: HomeContentUiModel? = null

    init {
        root.setOnClickListener { boundItem?.let(onClick) }
    }

    protected fun bindClick(item: HomeContentUiModel) {
        boundItem = item
        itemView.contentDescription = item.title
    }
}

private class BannerViewHolder(
    private val binding: ItemHomeBannerBinding,
    onClick: (HomeContentUiModel) -> Unit,
) : ClickableContentViewHolder(binding.root, onClick) {
    init {
        val availableWidth = binding.root.resources.displayMetrics.widthPixels - 40.dp
        val bannerWidth = availableWidth.coerceAtMost(340.dp)
        binding.root.updateLayoutParams {
            width = bannerWidth
            height = bannerWidth * 9 / 16
        }
    }

    override fun bind(item: HomeContentUiModel) = with(binding) {
        bindClick(item)
        artwork.load(item.thumbnailUrl) { crossfade(true) }
        title.text = item.title
        description.text = item.description
        age.text = item.ageRestriction
        age.isVisible = !item.ageRestriction.isNullOrBlank()
    }
}

private class HighlightTallViewHolder(
    private val binding: ItemHomeCardHighlightTallBinding,
    onClick: (HomeContentUiModel) -> Unit,
) : ClickableContentViewHolder(binding.root, onClick) {
    override fun bind(item: HomeContentUiModel) = with(binding) {
        bindClick(item)
        artwork.load(item.thumbnailUrl) { crossfade(true) }
        title.text = item.title
        duration.text = item.durationLabel
    }
}

private class LandscapeViewHolder(
    private val binding: ItemHomeCardLandscapeBinding,
    onClick: (HomeContentUiModel) -> Unit,
    private val ranking: (HomeContentUiModel) -> String?,
) : ClickableContentViewHolder(binding.root, onClick) {
    override fun bind(item: HomeContentUiModel) = with(binding) {
        bindClick(item)
        artwork.load(item.thumbnailUrl) { crossfade(true) }
        title.text = item.title
        liveBadge.isVisible = item.isLive
        episodeBadge.isVisible = item.episodeCount > 0
        episodeBadge.text = root.resources.getQuantityString(
            R.plurals.episode_count,
            item.episodeCount,
            item.episodeCount,
        )
        rank.text = ranking(item)
        rank.isVisible = !rank.text.isNullOrBlank()
    }
}

private class PortraitViewHolder(
    private val binding: ItemHomeCardPortraitBinding,
    onClick: (HomeContentUiModel) -> Unit,
    private val ranking: (HomeContentUiModel) -> String?,
) : ClickableContentViewHolder(binding.root, onClick) {
    override fun bind(item: HomeContentUiModel) = with(binding) {
        bindClick(item)
        artwork.load(item.thumbnailUrl) { crossfade(true) }
        title.text = item.title
        rank.text = ranking(item)
        rank.isVisible = !rank.text.isNullOrBlank()
    }
}

private class TopTenViewHolder(
    private val binding: ItemHomeCardTopTenBinding,
    onClick: (HomeContentUiModel) -> Unit,
    private val ranking: (HomeContentUiModel) -> String?,
) : ClickableContentViewHolder(binding.root, onClick) {
    override fun bind(item: HomeContentUiModel) = with(binding) {
        bindClick(item)
        artwork.load(item.thumbnailUrl) { crossfade(true) }
        rank.text = ranking(item)
    }
}

private class StoryViewHolder(
    private val binding: ItemHomeCardStoryBinding,
    onClick: (HomeContentUiModel) -> Unit,
) : ClickableContentViewHolder(binding.root, onClick) {
    override fun bind(item: HomeContentUiModel) = with(binding) {
        bindClick(item)
        artwork.load(item.thumbnailUrl) { crossfade(true) }
        provider.load(item.providerAvatarUrl) { crossfade(true) }
        Unit
    }
}

private class ShortViewHolder(
    private val binding: ItemHomeCardShortBinding,
    onClick: (HomeContentUiModel) -> Unit,
) : ClickableContentViewHolder(binding.root, onClick) {
    override fun bind(item: HomeContentUiModel) = with(binding) {
        bindClick(item)
        artwork.load(item.thumbnailUrl) { crossfade(true) }
        views.text = item.viewCountLabel
    }
}

private class CircleViewHolder(
    private val binding: ItemHomeCardCircleBinding,
    onClick: (HomeContentUiModel) -> Unit,
) : ClickableContentViewHolder(binding.root, onClick) {
    override fun bind(item: HomeContentUiModel) = with(binding) {
        bindClick(item)
        artwork.load(item.thumbnailUrl) { crossfade(true) }
        title.text = item.title
    }
}

private class WatchingViewHolder(
    private val binding: ItemHomeCardWatchingBinding,
    onClick: (HomeContentUiModel) -> Unit,
) : ClickableContentViewHolder(binding.root, onClick) {
    override fun bind(item: HomeContentUiModel) = with(binding) {
        bindClick(item)
        artwork.load(item.thumbnailUrl) { crossfade(true) }
        title.text = item.title
        subtitle.text = item.subtitle
        progress.progress = item.progressPercent
    }
}

private class MiniAppViewHolder(
    private val binding: ItemHomeCardMiniAppBinding,
    onClick: (HomeContentUiModel) -> Unit,
) : ClickableContentViewHolder(binding.root, onClick) {
    override fun bind(item: HomeContentUiModel) = with(binding) {
        bindClick(item)
        artwork.load(item.thumbnailUrl) { crossfade(true) }
        title.text = item.title
    }
}
