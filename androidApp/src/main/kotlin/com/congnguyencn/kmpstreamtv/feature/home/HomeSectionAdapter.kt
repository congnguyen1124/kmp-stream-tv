package com.congnguyencn.kmpstreamtv.feature.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.core.ui.recyclerview.BaseListAdapter
import com.congnguyencn.kmpstreamtv.core.ui.recyclerview.BindableViewHolder
import com.congnguyencn.kmpstreamtv.core.ui.recyclerview.HorizontalSpacingDecoration
import com.congnguyencn.kmpstreamtv.databinding.ItemMiniAppBinding
import com.congnguyencn.kmpstreamtv.databinding.LayoutBackgroundViewBinding
import com.congnguyencn.kmpstreamtv.databinding.LayoutHighlightTallViewBinding
import com.congnguyencn.kmpstreamtv.databinding.LayoutHighlightWideViewBinding
import com.congnguyencn.kmpstreamtv.databinding.LayoutMiniAppViewBinding
import com.congnguyencn.kmpstreamtv.databinding.LayoutViewBinding
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeContentUiModel
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeSectionPresentation
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeSectionUiModel
import kotlin.math.abs

internal class HomeSectionAdapter(
    private val onContentClick: (HomeContentUiModel) -> Unit,
) : BaseListAdapter<HomeSectionUiModel>(HomeSectionDiffCallback) {
    private val recycledViewPool = RecyclerView.RecycledViewPool()

    override fun getItemViewType(position: Int): Int = getItem(position).presentation.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindableViewHolder<HomeSectionUiModel> {
        val inflater = LayoutInflater.from(parent.context)
        return when (val presentation = HomeSectionPresentation.entries[viewType]) {
            HomeSectionPresentation.HighlightWide -> HighlightWideSectionViewHolder(
                LayoutHighlightWideViewBinding.inflate(inflater, parent, false), recycledViewPool, onContentClick,
            )
            HomeSectionPresentation.HighlightTall -> HighlightTallSectionViewHolder(
                LayoutHighlightTallViewBinding.inflate(inflater, parent, false), recycledViewPool, onContentClick,
            )
            HomeSectionPresentation.TopTen -> BackgroundSectionViewHolder(
                LayoutBackgroundViewBinding.inflate(inflater, parent, false), recycledViewPool, onContentClick,
            )
            HomeSectionPresentation.MiniApps -> MiniAppSectionViewHolder(
                LayoutMiniAppViewBinding.inflate(inflater, parent, false), onContentClick,
            )
            else -> GeneralSectionViewHolder(
                LayoutViewBinding.inflate(inflater, parent, false), presentation, recycledViewPool, onContentClick,
            )
        }
    }
}

private class GeneralSectionViewHolder(
    private val binding: LayoutViewBinding,
    presentation: HomeSectionPresentation,
    pool: RecyclerView.RecycledViewPool,
    onContentClick: (HomeContentUiModel) -> Unit,
) : BindableViewHolder<HomeSectionUiModel>(binding) {
    private val adapter = HomeContentAdapter(presentation.toContentStyle(), onContentClick)
    private val isStory = presentation == HomeSectionPresentation.Story
    private var boundSectionId: String? = null

    init {
        binding.rcvItems.configureHorizontal(pool, adapter)
        binding.ivLayoutBackground.isVisible = isStory
        binding.dividerTop.isVisible = isStory
        binding.dividerBottom.isVisible = isStory
        if (isStory) {
            binding.ivLayoutBackground.setImageResource(R.drawable.bg_story_layout)
            binding.ivLayoutBackground.scaleType = ImageView.ScaleType.FIT_XY
            binding.ivLayoutBackground.alpha = 0.8f
            binding.tvLayoutTitle.setBackgroundResource(R.drawable.bg_story_block)
            binding.tvLayoutTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_fire, 0, 0, 0)
            binding.tvLayoutTitle.setTextAppearance(R.style.Text_Heading2)
            binding.tvLayoutTitle.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginStart = 0
                leftMargin = 0
            }
            binding.tvLayoutTitle.setPadding(
                binding.root.resources.getDimensionPixelSize(R.dimen.content_padding_horizontal),
                binding.root.resources.getDimensionPixelSize(R.dimen.margin_tiny_x),
                0,
                binding.root.resources.getDimensionPixelSize(R.dimen.margin_tiny_x),
            )
            binding.rcvItems.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = binding.root.resources.getDimensionPixelSize(R.dimen.margin_tiny)
            }
            val storySpacing = binding.root.resources.getDimensionPixelSize(R.dimen.margin)
            binding.layoutContent.setPadding(0, 0, 0, storySpacing)
            binding.root.setPadding(0, 0, 0, storySpacing)
        }
    }

    override fun bind(item: HomeSectionUiModel) = with(binding) {
        if (boundSectionId != item.id) rcvItems.scrollToPosition(0)
        boundSectionId = item.id
        tvLayoutTitle.text = item.title
        adapter.submitSection(item)
    }
}

private class HighlightWideSectionViewHolder(
    private val binding: LayoutHighlightWideViewBinding,
    pool: RecyclerView.RecycledViewPool,
    onContentClick: (HomeContentUiModel) -> Unit,
) : BindableViewHolder<HomeSectionUiModel>(binding) {
    private val adapter = HomeContentAdapter(HomeContentStyle.HighlightWide, onContentClick)

    init {
        binding.carouselView.configureCarousel(pool, adapter, R.dimen.highlight_wide_thumbnail_width)
    }

    override fun bind(item: HomeSectionUiModel) = adapter.submitSection(item)
}

private class HighlightTallSectionViewHolder(
    private val binding: LayoutHighlightTallViewBinding,
    pool: RecyclerView.RecycledViewPool,
    private val onContentClick: (HomeContentUiModel) -> Unit,
) : BindableViewHolder<HomeSectionUiModel>(binding) {
    private val adapter = HomeContentAdapter(HomeContentStyle.HighlightTall, onContentClick)
    private var activeItem: HomeContentUiModel? = null

    init {
        binding.carouselView.configureCarousel(
            pool = pool,
            contentAdapter = adapter,
            desiredWidthRes = R.dimen.highlight_tall_thumbnail_width,
            onCenteredItemChanged = { position ->
                adapter.currentList.getOrNull(position)?.let(::showActiveItem)
            },
        )
        binding.btnPlay.setOnClickListener { activeItem?.let(onContentClick) }
        binding.btnWatchLater.setOnClickListener {
            Toast.makeText(binding.root.context, R.string.added_to_watch_later, Toast.LENGTH_SHORT).show()
        }
        binding.btnInfo.setOnClickListener {
            activeItem?.let { item -> Toast.makeText(binding.root.context, item.description, Toast.LENGTH_LONG).show() }
        }
    }

    override fun bind(item: HomeSectionUiModel) = bindSection(item, isFirstSection = false)

    override fun bind(item: HomeSectionUiModel, position: Int) = bindSection(item, isFirstSection = position == 0)

    private fun bindSection(item: HomeSectionUiModel, isFirstSection: Boolean) = with(binding) {
        carouselView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = root.resources.getDimensionPixelSize(R.dimen.vertical_list_divider) +
                if (isFirstSection) root.resources.getDimensionPixelSize(R.dimen.home_content_padding_top) else 0
        }
        activeItem = item.items.firstOrNull()
        activeItem?.let(::showActiveItem)
        adapter.submitSection(item)
    }

    private fun showActiveItem(item: HomeContentUiModel) {
        activeItem = item
        binding.ivCarouselBlur.load(item.thumbnailUrl) { crossfade(true) }
    }
}

private class BackgroundSectionViewHolder(
    private val binding: LayoutBackgroundViewBinding,
    pool: RecyclerView.RecycledViewPool,
    onContentClick: (HomeContentUiModel) -> Unit,
) : BindableViewHolder<HomeSectionUiModel>(binding) {
    private val adapter = HomeContentAdapter(HomeContentStyle.TopTen, onContentClick)

    init {
        binding.rcvItems.configureHorizontal(pool, adapter)
    }

    override fun bind(item: HomeSectionUiModel) = with(binding) {
        tvLayoutTitle.text = item.title
        ivLayoutBackground.load(item.backgroundUrl) { crossfade(true) }
        adapter.submitSection(item)
    }
}

private class MiniAppSectionViewHolder(
    private val binding: LayoutMiniAppViewBinding,
    private val onContentClick: (HomeContentUiModel) -> Unit,
) : BindableViewHolder<HomeSectionUiModel>(binding) {
    override fun bind(item: HomeSectionUiModel) {
        binding.llMiniApps.removeAllViews()
        item.items.take(5).forEach { content ->
            val child = ItemMiniAppBinding.inflate(LayoutInflater.from(binding.root.context), binding.llMiniApps, false)
            child.root.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            child.root.contentDescription = content.title
            child.root.setOnClickListener { onContentClick(content) }
            child.ivAppIcon.load(content.thumbnailUrl) { crossfade(true) }
            child.tvAppName.text = content.title
            binding.llMiniApps.addView(child.root)
        }
    }
}

private fun RecyclerView.configureHorizontal(
    pool: RecyclerView.RecycledViewPool,
    contentAdapter: HomeContentAdapter,
) {
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    adapter = contentAdapter
    setRecycledViewPool(pool)
    isNestedScrollingEnabled = false
    setBackgroundColor(Color.TRANSPARENT)
    itemAnimator = null
}

private fun RecyclerView.configureCarousel(
    pool: RecyclerView.RecycledViewPool,
    contentAdapter: HomeContentAdapter,
    desiredWidthRes: Int,
    onCenteredItemChanged: (Int) -> Unit = {},
) {
    configureHorizontal(pool, contentAdapter)
    val desiredWidth = resources.getDimensionPixelSize(desiredWidthRes)
    val minimumPadding = resources.getDimensionPixelSize(R.dimen.carousel_padding_horizontal_min)
    val horizontalPadding = maxOf(minimumPadding, (resources.displayMetrics.widthPixels - desiredWidth) / 2)
    setPadding(horizontalPadding, paddingTop, horizontalPadding, paddingBottom)
    clipChildren = false
    clipToPadding = false
    overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    addItemDecoration(
        HorizontalSpacingDecoration(
            spacing = resources.getDimensionPixelSize(R.dimen.horizontal_list_divider),
            edgeSpacing = 0,
        ),
    )
    val snapHelper = PagerSnapHelper().also { it.attachToRecyclerView(this) }

    fun updatePageScale() {
        val center = (width + paddingLeft - paddingRight) / 2f
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val childCenter = (child.left + child.right) / 2f
            val distance = (abs(center - childCenter) / child.width.coerceAtLeast(1)).coerceIn(0f, 1f)
            val scale = 1f - (0.15f * distance)
            child.scaleX = scale
            child.scaleY = scale
        }
    }
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) = updatePageScale()

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState != RecyclerView.SCROLL_STATE_IDLE) return
            val snappedView = snapHelper.findSnapView(layoutManager) ?: return
            val position = layoutManager?.getPosition(snappedView) ?: return
            onCenteredItemChanged(position)
        }
    })
    doOnLayout { updatePageScale() }
}

private fun HomeSectionPresentation.toContentStyle(): HomeContentStyle = when (this) {
    HomeSectionPresentation.GeneralWide -> HomeContentStyle.Landscape
    HomeSectionPresentation.GeneralTall -> HomeContentStyle.Portrait
    HomeSectionPresentation.Circle -> HomeContentStyle.Circle
    HomeSectionPresentation.Short -> HomeContentStyle.Short
    HomeSectionPresentation.Story -> HomeContentStyle.Story
    HomeSectionPresentation.ContinueWatching -> HomeContentStyle.ContinueWatching
    HomeSectionPresentation.HighlightWide -> HomeContentStyle.HighlightWide
    HomeSectionPresentation.HighlightTall -> HomeContentStyle.HighlightTall
    HomeSectionPresentation.TopTen -> HomeContentStyle.TopTen
    HomeSectionPresentation.MiniApps -> HomeContentStyle.MiniApp
}
