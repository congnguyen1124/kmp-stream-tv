package com.congnguyencn.kmpstreamtv.feature.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.core.ui.dp
import com.congnguyencn.kmpstreamtv.core.ui.recyclerview.BaseListAdapter
import com.congnguyencn.kmpstreamtv.core.ui.recyclerview.BindableViewHolder
import com.congnguyencn.kmpstreamtv.core.ui.recyclerview.HorizontalSpacingDecoration
import com.congnguyencn.kmpstreamtv.databinding.ItemHomeHighlightTallBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemHomeMiniSectionBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemHomeSectionBackgroundBinding
import com.congnguyencn.kmpstreamtv.databinding.ItemHomeSectionBinding
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeContentUiModel
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeSectionPresentation
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeSectionUiModel

internal class HomeSectionAdapter(
    private val onContentClick: (HomeContentUiModel) -> Unit,
) : BaseListAdapter<HomeSectionUiModel>(HomeSectionDiffCallback) {
    private val recycledViewPool = RecyclerView.RecycledViewPool()

    override fun getItemViewType(position: Int): Int = getItem(position).presentation.ordinal

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BindableViewHolder<HomeSectionUiModel> {
        val inflater = LayoutInflater.from(parent.context)
        return when (val presentation = HomeSectionPresentation.entries[viewType]) {
            HomeSectionPresentation.HighlightTall -> HighlightTallSectionViewHolder(
                ItemHomeHighlightTallBinding.inflate(inflater, parent, false),
                recycledViewPool,
                onContentClick,
            )
            HomeSectionPresentation.TopTen -> BackgroundSectionViewHolder(
                ItemHomeSectionBackgroundBinding.inflate(inflater, parent, false),
                recycledViewPool,
                onContentClick,
            )
            HomeSectionPresentation.MiniApps -> MiniAppSectionViewHolder(
                ItemHomeMiniSectionBinding.inflate(inflater, parent, false),
                recycledViewPool,
                onContentClick,
            )
            else -> GeneralSectionViewHolder(
                ItemHomeSectionBinding.inflate(inflater, parent, false),
                presentation,
                recycledViewPool,
                onContentClick,
            )
        }
    }
}

private class GeneralSectionViewHolder(
    private val binding: ItemHomeSectionBinding,
    presentation: HomeSectionPresentation,
    recycledViewPool: RecyclerView.RecycledViewPool,
    onContentClick: (HomeContentUiModel) -> Unit,
) : BindableViewHolder<HomeSectionUiModel>(binding) {
    private val contentAdapter = HomeContentAdapter(presentation.toContentStyle(), onContentClick)
    private var boundSectionId: String? = null
    private val isHighlight = presentation == HomeSectionPresentation.HighlightWide
    private val isStory = presentation == HomeSectionPresentation.Story

    init {
        binding.content.configureHorizontal(recycledViewPool, contentAdapter, edgeSpacing = 20.dp)
        if (isHighlight) PagerSnapHelper().attachToRecyclerView(binding.content)
        binding.root.setBackgroundResource(
            if (isStory) R.drawable.story_section_background else android.R.color.transparent,
        )
    }

    override fun bind(item: HomeSectionUiModel) = with(binding) {
        if (boundSectionId != item.id) content.scrollToPosition(0)
        boundSectionId = item.id
        title.text = if (isStory) "🔥  ${item.title}" else item.title
        title.isVisible = !isHighlight
        contentAdapter.submitSection(item)
    }
}

private class HighlightTallSectionViewHolder(
    private val binding: ItemHomeHighlightTallBinding,
    recycledViewPool: RecyclerView.RecycledViewPool,
    private val onContentClick: (HomeContentUiModel) -> Unit,
) : BindableViewHolder<HomeSectionUiModel>(binding) {
    private val contentAdapter = HomeContentAdapter(HomeContentStyle.HighlightTall, onContentClick)
    private var firstItem: HomeContentUiModel? = null

    init {
        binding.content.configureHorizontal(recycledViewPool, contentAdapter, edgeSpacing = 0)
        PagerSnapHelper().attachToRecyclerView(binding.content)
        binding.play.setOnClickListener { firstItem?.let(onContentClick) }
        binding.watchLater.setOnClickListener {
            Toast.makeText(binding.root.context, R.string.added_to_watch_later, Toast.LENGTH_SHORT).show()
        }
        binding.info.setOnClickListener {
            firstItem?.let { item ->
                Toast.makeText(binding.root.context, item.description, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun bind(item: HomeSectionUiModel) = with(binding) {
        firstItem = item.items.firstOrNull()
        title.text = item.title
        backgroundArtwork.load(item.backgroundUrl) { crossfade(true) }
        contentAdapter.submitSection(item)
    }
}

private class BackgroundSectionViewHolder(
    private val binding: ItemHomeSectionBackgroundBinding,
    recycledViewPool: RecyclerView.RecycledViewPool,
    onContentClick: (HomeContentUiModel) -> Unit,
) : BindableViewHolder<HomeSectionUiModel>(binding) {
    private val contentAdapter = HomeContentAdapter(HomeContentStyle.TopTen, onContentClick)

    init {
        binding.content.configureHorizontal(recycledViewPool, contentAdapter, edgeSpacing = 0)
    }

    override fun bind(item: HomeSectionUiModel) = with(binding) {
        title.text = item.title
        backgroundArtwork.load(item.backgroundUrl) { crossfade(true) }
        contentAdapter.submitSection(item)
    }
}

private class MiniAppSectionViewHolder(
    private val binding: ItemHomeMiniSectionBinding,
    recycledViewPool: RecyclerView.RecycledViewPool,
    onContentClick: (HomeContentUiModel) -> Unit,
) : BindableViewHolder<HomeSectionUiModel>(binding) {
    private val contentAdapter = HomeContentAdapter(HomeContentStyle.MiniApp, onContentClick)

    init {
        binding.content.configureHorizontal(recycledViewPool, contentAdapter, edgeSpacing = 0)
    }

    override fun bind(item: HomeSectionUiModel) = with(binding) {
        title.text = item.title
        contentAdapter.submitSection(item)
    }
}

private fun RecyclerView.configureHorizontal(
    pool: RecyclerView.RecycledViewPool,
    contentAdapter: HomeContentAdapter,
    edgeSpacing: Int,
) {
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    adapter = contentAdapter
    setRecycledViewPool(pool)
    setHasFixedSize(true)
    isNestedScrollingEnabled = false
    addItemDecoration(HorizontalSpacingDecoration(12.dp, edgeSpacing))
}

private fun HomeSectionPresentation.toContentStyle(): HomeContentStyle = when (this) {
    HomeSectionPresentation.HighlightWide -> HomeContentStyle.HighlightWide
    HomeSectionPresentation.GeneralWide -> HomeContentStyle.Landscape
    HomeSectionPresentation.GeneralTall -> HomeContentStyle.Portrait
    HomeSectionPresentation.Circle -> HomeContentStyle.Circle
    HomeSectionPresentation.Short -> HomeContentStyle.Short
    HomeSectionPresentation.Story -> HomeContentStyle.Story
    HomeSectionPresentation.ContinueWatching -> HomeContentStyle.ContinueWatching
    HomeSectionPresentation.HighlightTall -> HomeContentStyle.HighlightTall
    HomeSectionPresentation.TopTen -> HomeContentStyle.TopTen
    HomeSectionPresentation.MiniApps -> HomeContentStyle.MiniApp
}
