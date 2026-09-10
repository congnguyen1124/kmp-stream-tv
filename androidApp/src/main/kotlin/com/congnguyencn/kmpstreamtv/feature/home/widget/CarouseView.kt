package com.congnguyencn.kmpstreamtv.feature.home.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.ViewPager2
import coil3.load
import coil3.request.crossfade
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.databinding.ItemThumbnailCarouselBinding
import com.congnguyencn.kmpstreamtv.databinding.ViewPagerCarouselBinding
import com.congnguyencn.kmpstreamtv.feature.home.isDummyExclusive
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeContentUiModel
import kotlin.math.abs

private const val PAGE_LOOP = 1000

/** The source Home carousel ported one-for-one, with only its item type changed to the KMP UI model. */
class CarouseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val binding = ViewPagerCarouselBinding.inflate(LayoutInflater.from(context), this)
    private val carouselAdapter: CarouselAdapter
    private val minScale = 0.85f
    private val maxScale = 1f
    private val thumbWidth: Int
    private val thumbHeight: Int
    private var realSize = 0
    private var cachedItems: List<HomeContentUiModel>? = null

    var onPageChangeCallback: OnPageChangeCallback? = null
    var onItemClickListener: OnItemClickCallback? = null

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CarouseView,
            defStyleAttr,
            defStyleRes,
        ).apply {
            try {
                thumbWidth = getDimensionPixelSize(R.styleable.CarouseView_thumbWidth, 0)
                thumbHeight = getDimensionPixelSize(R.styleable.CarouseView_thumbHeight, 0)
            } finally {
                recycle()
            }
        }

        val screenWidth = resources.displayMetrics.widthPixels
        var paddingH = (screenWidth - thumbWidth) / 2
        val paddingMin = resources.getDimensionPixelSize(R.dimen.carousel_padding_horizontal_min)
        var actualThumbWidth = thumbWidth
        var actualThumbHeight = thumbHeight
        if (paddingH < paddingMin) {
            paddingH = paddingMin
            actualThumbWidth = screenWidth - (2 * paddingH)
            actualThumbHeight = actualThumbWidth * thumbHeight / thumbWidth
        }

        carouselAdapter = CarouselAdapter(actualThumbWidth, actualThumbHeight) { position ->
            if (realSize > 0) onItemClickListener?.onClicked(position % realSize)
        }

        binding.viewPager.apply {
            clipChildren = false
            clipToPadding = false
            offscreenPageLimit = 3
            adapter = carouselAdapter
            setPageTransformer(
                CompositePageTransformer().apply {
                    addTransformer { page, position ->
                        val scaleRange = maxScale - minScale
                        val percent = 1f - abs(position)
                        val scale = minScale + (percent * scaleRange)
                        page.scaleY = scale
                        page.scaleX = scale
                        page.translationX = position * -(2 * paddingH)
                    }
                },
            )
            registerOnPageChangeCallback(
                object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        if (realSize > 0) onPageChangeCallback?.onPageChanged(position % realSize)
                    }
                },
            )
        }
    }

    fun bindItems(items: List<HomeContentUiModel>) {
        if (cachedItems == items) return
        cachedItems = items
        realSize = items.size
        carouselAdapter.setItems(items)
        if (realSize == 0) return
        binding.root.post {
            binding.viewPager.setCurrentItem(realSize * PAGE_LOOP / 2, false)
            binding.viewPager.requestTransform()
        }
    }

    fun interface OnPageChangeCallback {
        fun onPageChanged(position: Int)
    }

    fun interface OnItemClickCallback {
        fun onClicked(position: Int)
    }
}

private class CarouselAdapter(
    private val thumbWidth: Int,
    private val thumbHeight: Int,
    private val onItemClick: (Int) -> Unit,
) : RecyclerView.Adapter<CarouselItemViewHolder>() {
    private val items = arrayListOf<HomeContentUiModel>()

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(data: List<HomeContentUiModel>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        CarouselItemViewHolder(parent, thumbWidth, thumbHeight, onItemClick)

    override fun onBindViewHolder(holder: CarouselItemViewHolder, position: Int) {
        holder.bind(position, items[position % items.size])
    }

    override fun getItemCount(): Int = if (items.isEmpty()) 0 else items.size * PAGE_LOOP
}

private class CarouselItemViewHolder(
    parent: ViewGroup,
    thumbWidth: Int,
    thumbHeight: Int,
    private val onItemClick: (Int) -> Unit,
) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context).inflate(R.layout.item_thumbnail_carousel, parent, false),
) {
    private val binding = ItemThumbnailCarouselBinding.bind(itemView).also {
        it.ivThumbnailCarousel.layoutParams = it.ivThumbnailCarousel.layoutParams.apply {
            width = thumbWidth
            height = thumbHeight
        }
    }

    fun bind(position: Int, item: HomeContentUiModel) = with(binding) {
        ivThumbnailCarousel.load(item.thumbnailUrl) { crossfade(true) }
        ivThumbnailCarousel.contentDescription = item.title
        ivThumbnailCarousel.setOnClickListener { onItemClick(position) }
        tvSaymee.isVisible = item.isDummyExclusive
    }
}
