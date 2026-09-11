package com.congnguyencn.kmpstreamtv.feature.home.widget.layout

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.databinding.ItemMiniAppBinding
import com.congnguyencn.kmpstreamtv.databinding.LayoutBackgroundViewBinding
import com.congnguyencn.kmpstreamtv.databinding.LayoutMiniAppViewBinding
import com.congnguyencn.kmpstreamtv.databinding.LayoutViewBinding
import com.congnguyencn.kmpstreamtv.feature.home.HomeContentAdapter
import com.congnguyencn.kmpstreamtv.feature.home.HomeContentStyle
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeContentUiModel
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeSectionPresentation
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeSectionUiModel

interface HomeLayoutItemClickable {
    fun setOnContentClickListener(listener: ((HomeContentUiModel) -> Unit)?)
}

abstract class HomeLayoutBaseView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0,
    ) : FrameLayout(context, attrs, defStyleAttr, defStyleRes),
        HomeLayoutItemClickable {
        protected var onContentClick: ((HomeContentUiModel) -> Unit)? = null

        override fun setOnContentClickListener(listener: ((HomeContentUiModel) -> Unit)?) {
            onContentClick = listener
        }

        protected fun bindLayoutBackground(
            view: ImageView,
            backgroundUrl: String?,
        ) {
            val url = backgroundUrl?.takeIf(String::isNotBlank)
            if (url == null) {
                view.visibility = GONE
                view.load(null)
                return
            }
            view.visibility = VISIBLE
            view.load(url) { crossfade(true) }
        }
    }

class LayoutGeneralView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0,
    ) : HomeLayoutBaseView(context, attrs, defStyleAttr, defStyleRes) {
        private val binding = LayoutViewBinding.inflate(LayoutInflater.from(context), this, true)
        private var adapter: HomeContentAdapter? = null
        private var style: HomeContentStyle? = null

        init {
            binding.errorView.setOnActionButtonClicked { binding.errorView.visibility = GONE }
        }

        fun bindLayout(data: HomeSectionUiModel) =
            with(binding) {
                val targetStyle = data.presentation.toContentStyle()
                if (style != targetStyle) {
                    style = targetStyle
                    adapter = HomeContentAdapter(targetStyle) { item -> onContentClick?.invoke(item) }
                    rcvItems.configureHorizontal(adapter!!)
                }
                tvLayoutTitle.text = data.title
                ivLayoutBackground.visibility = GONE
                rcvItems.visibility = VISIBLE
                pbLoading.visibility = GONE
                errorView.visibility = GONE
                adapter?.submitSection(data)

                rcvItems.minimumHeight =
                    when (data.presentation) {
                        HomeSectionPresentation.GeneralWide -> {
                            resources.getDimensionPixelSize(
                                R.dimen.ephemeral_wide_thumbnail_height,
                            )
                        }

                        HomeSectionPresentation.GeneralTall -> {
                            resources.getDimensionPixelSize(
                                R.dimen.ephemeral_tall_thumbnail_height,
                            )
                        }

                        HomeSectionPresentation.Circle -> {
                            resources.getDimensionPixelSize(R.dimen.circle_icon_size)
                        }

                        HomeSectionPresentation.Short -> {
                            resources.getDimensionPixelSize(R.dimen.short_thumbnail_height)
                        }

                        else -> {
                            0
                        }
                    }
                if (data.presentation == HomeSectionPresentation.Short) bindShort()
            }

        private fun bindShort() =
            with(binding) {
                tvLayoutTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lightning, 0, 0, 0)
                layoutContent.setPadding(
                    0,
                    resources.getDimensionPixelSize(R.dimen.margin_xx),
                    0,
                    resources.getDimensionPixelSize(R.dimen.margin_xx),
                )
            }
    }

class LayoutStory
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0,
    ) : HomeLayoutBaseView(context, attrs, defStyleAttr, defStyleRes) {
        private val binding = LayoutViewBinding.inflate(LayoutInflater.from(context), this, true)
        private val adapter = HomeContentAdapter(HomeContentStyle.Story) { item -> onContentClick?.invoke(item) }

        init {
            binding.rcvItems.configureHorizontal(adapter)
            binding.errorView.setOnActionButtonClicked { binding.errorView.visibility = GONE }
            bindStoryChrome()
        }

        fun bindLayout(data: HomeSectionUiModel) =
            with(binding) {
                tvLayoutTitle.text = data.title
                rcvItems.visibility = VISIBLE
                pbLoading.visibility = GONE
                errorView.visibility = GONE
                adapter.submitSection(data)
            }

        private fun bindStoryChrome() =
            with(binding) {
                tvLayoutTitle.updateLayoutParams<ViewGroup.MarginLayoutParams> { leftMargin = 0 }
                tvLayoutTitle.apply {
                    setBackgroundResource(R.drawable.bg_story_block)
                    setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_fire, 0, 0, 0)
                    setTextAppearance(R.style.Text_Heading2)
                    setPadding(
                        resources.getDimensionPixelSize(R.dimen.content_padding_horizontal),
                        resources.getDimensionPixelSize(R.dimen.margin_tiny_x),
                        0,
                        resources.getDimensionPixelSize(R.dimen.margin_tiny_x),
                    )
                }
                rcvItems.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = resources.getDimensionPixelSize(R.dimen.margin_tiny)
                }
                dividerTop.visibility = VISIBLE
                dividerBottom.visibility = VISIBLE
                ivLayoutBackground.apply {
                    visibility = VISIBLE
                    setImageResource(R.drawable.bg_story_layout)
                    scaleType = android.widget.ImageView.ScaleType.FIT_XY
                    alpha = 0.8f
                }
                val spacing = resources.getDimensionPixelSize(R.dimen.margin)
                layoutContent.setPadding(0, 0, 0, spacing)
                binding.root.setPadding(0, 0, 0, spacing)
                rcvItems.minimumHeight = resources.getDimensionPixelSize(R.dimen.story_thumbnail_height)
            }
    }

class LayoutWatching
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0,
    ) : HomeLayoutBaseView(context, attrs, defStyleAttr, defStyleRes) {
        private val binding = LayoutViewBinding.inflate(LayoutInflater.from(context), this, true)
        private val adapter =
            HomeContentAdapter(HomeContentStyle.ContinueWatching) { item -> onContentClick?.invoke(item) }

        init {
            binding.rcvItems.configureHorizontal(adapter)
            binding.rcvItems.minimumHeight = resources.getDimensionPixelSize(R.dimen.ephemeral_wide_thumbnail_height)
        }

        fun bindLayout(data: HomeSectionUiModel) =
            with(binding) {
                tvLayoutTitle.text = data.title
                ivLayoutBackground.visibility = GONE
                rcvItems.visibility = VISIBLE
                pbLoading.visibility = GONE
                errorView.visibility = GONE
                adapter.submitSection(data)
            }
    }

class LayoutBackgroundView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0,
    ) : HomeLayoutBaseView(context, attrs, defStyleAttr, defStyleRes) {
        private val binding = LayoutBackgroundViewBinding.inflate(LayoutInflater.from(context), this, true)
        private val adapter = HomeContentAdapter(HomeContentStyle.TopTen) { item -> onContentClick?.invoke(item) }
        private var overallXScroll = 0f
        private val maxWidthTitle =
            resources.getDimensionPixelSize(R.dimen.top_ten_title_width) +
                resources.getDimensionPixelSize(R.dimen.top_ten_thumbnail_width)

        init {
            binding.rcvItems.configureHorizontal(adapter)
            binding.rcvItems.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(
                        recyclerView: RecyclerView,
                        dx: Int,
                        dy: Int,
                    ) {
                        overallXScroll += dx
                        if (overallXScroll <= maxWidthTitle) {
                            val scale = 1f - (overallXScroll / maxWidthTitle)
                            binding.layoutTitle.scaleX = scale
                            binding.layoutTitle.scaleY = scale
                            binding.layoutTitle.alpha = scale
                        }
                    }
                },
            )
            binding.errorView.setOnActionButtonClicked { binding.errorView.visibility = GONE }
        }

        fun bindLayout(data: HomeSectionUiModel) =
            with(binding) {
                tvLayoutTitle.text = data.title
                ivLayoutIcon.setImageResource(R.drawable.ic_fire)
                ivLayoutIcon.visibility = VISIBLE
                viewDivider.visibility = VISIBLE
                bindLayoutBackground(ivLayoutBackground, data.backgroundUrl)
                rcvItems.visibility = VISIBLE
                pbLoading.visibility = GONE
                errorView.visibility = GONE
                adapter.submitSection(data)
            }
    }

class LayoutMiniApp
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0,
    ) : HomeLayoutBaseView(context, attrs, defStyleAttr, defStyleRes) {
        private val binding = LayoutMiniAppViewBinding.inflate(LayoutInflater.from(context), this)

        init {
            setPadding(resources.getDimensionPixelSize(R.dimen.margin))
            binding.errorView.setOnActionButtonClicked { binding.errorView.visibility = GONE }
        }

        fun bindLayout(data: HomeSectionUiModel) =
            with(binding) {
                llMiniApps.removeAllViews()
                pbLoading.visibility = GONE
                errorView.visibility = GONE
                data.items.take(5).forEach { item ->
                    ItemMiniAppBinding.inflate(LayoutInflater.from(context), llMiniApps, true).also { child ->
                        child.ivAppIcon.contentDescription = item.title
                        child.ivAppIcon.load(item.thumbnailUrl) { crossfade(true) }
                        child.tvAppName.text = item.title
                        child.ivAppIcon.setOnClickListener { onContentClick?.invoke(item) }
                        child.tvAppName.setOnClickListener { onContentClick?.invoke(item) }
                    }
                }
            }
    }

private fun RecyclerView.configureHorizontal(contentAdapter: HomeContentAdapter) {
    layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
    adapter = contentAdapter
    isNestedScrollingEnabled = false
    itemAnimator = null
}

internal fun HomeSectionPresentation.toContentStyle(): HomeContentStyle =
    when (this) {
        HomeSectionPresentation.GeneralWide -> HomeContentStyle.Landscape

        HomeSectionPresentation.GeneralTall -> HomeContentStyle.Portrait

        HomeSectionPresentation.Circle -> HomeContentStyle.Circle

        HomeSectionPresentation.Short -> HomeContentStyle.Short

        HomeSectionPresentation.Story -> HomeContentStyle.Story

        HomeSectionPresentation.ContinueWatching -> HomeContentStyle.ContinueWatching

        HomeSectionPresentation.TopTen -> HomeContentStyle.TopTen

        HomeSectionPresentation.HighlightWide,
        HomeSectionPresentation.HighlightTall,
        HomeSectionPresentation.MiniApps,
        -> error("$this is rendered by its dedicated layout view")
    }
