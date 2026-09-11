package com.congnguyencn.kmpstreamtv.feature.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import coil3.load
import coil3.request.crossfade
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.core.ui.recyclerview.BaseListAdapter
import com.congnguyencn.kmpstreamtv.core.ui.recyclerview.BindableViewHolder
import com.congnguyencn.kmpstreamtv.databinding.ItemCircleBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemStoryBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemThumbShortBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemThumbnailBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemTopTenBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemWatchingBinding
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeContentUiModel
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeSectionUiModel

internal class HomeContentAdapter(
    private val style: HomeContentStyle,
    private val onContentClick: (HomeContentUiModel) -> Unit,
) : BaseListAdapter<HomeContentUiModel>(HomeContentDiffCallback) {
    fun submitSection(section: HomeSectionUiModel) = submitList(section.items)

    override fun getItemViewType(position: Int): Int = style.ordinal

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BindableViewHolder<HomeContentUiModel> {
        val inflater = LayoutInflater.from(parent.context)
        return when (val itemStyle = HomeContentStyle.entries[viewType]) {
            HomeContentStyle.Landscape,
            HomeContentStyle.Portrait,
            -> {
                ThumbnailViewHolder(ItemThumbnailBinding.inflate(inflater, parent, false), itemStyle, onContentClick)
            }

            HomeContentStyle.TopTen -> {
                TopTenViewHolder(
                    ItemTopTenBinding.inflate(inflater, parent, false),
                    onContentClick,
                    ::positionFor,
                )
            }

            HomeContentStyle.Story -> {
                StoryViewHolder(ItemStoryBinding.inflate(inflater, parent, false), onContentClick)
            }

            HomeContentStyle.Short -> {
                ShortViewHolder(ItemThumbShortBinding.inflate(inflater, parent, false), onContentClick)
            }

            HomeContentStyle.Circle -> {
                CircleViewHolder(ItemCircleBinding.inflate(inflater, parent, false), onContentClick)
            }

            HomeContentStyle.ContinueWatching -> {
                WatchingViewHolder(
                    ItemWatchingBinding.inflate(inflater, parent, false),
                    onContentClick,
                )
            }
        }
    }

    private fun positionFor(item: HomeContentUiModel): Int = currentList.indexOf(item).coerceAtLeast(0)
}

internal enum class HomeContentStyle {
    Landscape,
    Portrait,
    TopTen,
    Story,
    Short,
    Circle,
    ContinueWatching,
}

private abstract class ClickableContentViewHolder(
    root: View,
    clickableView: View,
    onClick: (HomeContentUiModel) -> Unit,
) : BindableViewHolder<HomeContentUiModel>(root) {
    protected var boundItem: HomeContentUiModel? = null

    init {
        clickableView.setOnClickListener { boundItem?.let(onClick) }
    }

    protected fun bindClick(item: HomeContentUiModel) {
        boundItem = item
        itemView.contentDescription = item.title
    }
}

private class ThumbnailViewHolder(
    private val binding: ItemThumbnailBinding,
    style: HomeContentStyle,
    onClick: (HomeContentUiModel) -> Unit,
) : ClickableContentViewHolder(binding.root, binding.ivThumbnail, onClick) {
    init {
        val dimensions =
            when (style) {
                HomeContentStyle.Landscape -> {
                    R.dimen.ephemeral_wide_thumbnail_width to
                        R.dimen.ephemeral_wide_thumbnail_height
                }

                else -> {
                    R.dimen.ephemeral_tall_thumbnail_width to R.dimen.ephemeral_tall_thumbnail_height
                }
            }
        binding.ivThumbnail.updateLayoutParams {
            width = binding.root.resources.getDimensionPixelSize(dimensions.first)
            height = binding.root.resources.getDimensionPixelSize(dimensions.second)
        }
    }

    override fun bind(item: HomeContentUiModel) =
        with(binding) {
            bindClick(item)
            ivThumbnail.load(item.thumbnailUrl) { crossfade(true) }
            tvSaymee.isVisible = item.isDummyExclusive
            tvBadgeLive.isVisible = item.isLive
        }
}

private class TopTenViewHolder(
    private val binding: ItemTopTenBinding,
    onClick: (HomeContentUiModel) -> Unit,
    private val positionFor: (HomeContentUiModel) -> Int,
) : ClickableContentViewHolder(binding.root, binding.ivTopTenThumbnail, onClick) {
    override fun bind(item: HomeContentUiModel) =
        with(binding) {
            bindClick(item)
            ivTopTenThumbnail.load(item.thumbnailUrl) { crossfade(true) }
            tvSayme.isVisible = item.isDummyExclusive
            ivTopTenPosition.setImageResource(RANK_DRAWABLES[positionFor(item).coerceIn(RANK_DRAWABLES.indices)])
        }

    private companion object {
        val RANK_DRAWABLES =
            intArrayOf(
                R.drawable.number_1,
                R.drawable.number_2,
                R.drawable.number_3,
                R.drawable.number_4,
                R.drawable.number_5,
                R.drawable.number_6,
                R.drawable.number_7,
                R.drawable.number_8,
                R.drawable.number_9,
                R.drawable.number_10,
            )
    }
}

private class StoryViewHolder(
    private val binding: ItemStoryBinding,
    onClick: (HomeContentUiModel) -> Unit,
) : ClickableContentViewHolder(binding.root, binding.ivStoryThumbnail, onClick) {
    override fun bind(item: HomeContentUiModel) {
        with(binding) {
            bindClick(item)
            ivStoryThumbnail.load(item.thumbnailUrl) { crossfade(true) }
            ivStoryProvider.load(item.providerAvatarUrl) { crossfade(true) }
            ivStoryProvider.isVisible = item.providerAvatarUrl.isNotBlank()
        }
    }
}

private class ShortViewHolder(
    private val binding: ItemThumbShortBinding,
    onClick: (HomeContentUiModel) -> Unit,
) : ClickableContentViewHolder(binding.root, binding.ivShortThumbnail, onClick) {
    override fun bind(item: HomeContentUiModel) =
        with(binding) {
            bindClick(item)
            ivShortThumbnail.load(item.thumbnailUrl) { crossfade(true) }
            tvShortNumView.text = item.viewCountLabel
        }
}

private class CircleViewHolder(
    private val binding: ItemCircleBinding,
    onClick: (HomeContentUiModel) -> Unit,
) : ClickableContentViewHolder(binding.root, binding.ivThumbnail, onClick) {
    override fun bind(item: HomeContentUiModel) =
        with(binding) {
            bindClick(item)
            ivThumbnail.load(item.thumbnailUrl) { crossfade(true) }
            tvName.text = item.title
        }
}

private class WatchingViewHolder(
    private val binding: ItemWatchingBinding,
    onClick: (HomeContentUiModel) -> Unit,
) : ClickableContentViewHolder(binding.root, binding.ivThumbnail, onClick) {
    init {
        binding.btnMore.setOnClickListener {
            boundItem?.let { item ->
                Toast.makeText(binding.root.context, item.description, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun bind(item: HomeContentUiModel) =
        with(binding) {
            bindClick(item)
            ivThumbnail.load(item.thumbnailUrl) { crossfade(true) }
            tvTitle.text = item.title
            tvSubTitle.text = item.subtitle
            tvSaymeLabel.isVisible = item.isDummyExclusive
            ivSelect.visibility = View.GONE
            progressTimeWatched.progress = item.progressPercent
        }
}

internal val HomeContentUiModel.isDummyExclusive: Boolean
    get() = id.fold(0) { total, character -> total + character.code } % 5 == 0
