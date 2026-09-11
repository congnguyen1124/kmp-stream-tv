package com.congnguyencn.kmpstreamtv.feature.home.widget.layout.highlight.tall

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.updateLayoutParams
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.databinding.LayoutHighlightTallViewBinding
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeContentUiModel
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeSectionUiModel
import com.congnguyencn.kmpstreamtv.feature.home.widget.CarouseView
import com.congnguyencn.kmpstreamtv.feature.home.widget.displayBlurImage
import com.congnguyencn.kmpstreamtv.feature.home.widget.layout.HomeLayoutBaseView

class LayoutHighlightTallView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0,
    ) : HomeLayoutBaseView(context, attrs, defStyleAttr, defStyleRes),
        CarouseView.OnPageChangeCallback {
        private val binding =
            LayoutHighlightTallViewBinding.inflate(LayoutInflater.from(context), this, true)
        private var items: List<HomeContentUiModel> = emptyList()
        private val followedItemIds = mutableSetOf<String>()
        private var activePosition = 0

        private val activeItem: HomeContentUiModel?
            get() = items.getOrNull(activePosition)

        init {
            binding.carouselView.onPageChangeCallback = this
            binding.carouselView.onItemClickListener =
                CarouseView.OnItemClickCallback {
                    activeItem?.let { item -> onContentClick?.invoke(item) }
                }
            binding.errorView.setOnActionButtonClicked {
                binding.errorView.visibility = GONE
            }
        }

        fun bindLayout(data: HomeSectionUiModel) = bindLayout(data, false)

        fun bindLayout(
            data: HomeSectionUiModel,
            isTopLayout: Boolean,
        ) = with(binding) {
            items = data.items
            activePosition = activePosition.coerceIn(items.indices)
            carouselView.bindItems(items)
            bindFollowStatus()
            carouselView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                val listDivider = resources.getDimensionPixelSize(R.dimen.vertical_list_divider)
                topMargin =
                    if (isTopLayout) {
                        listDivider + resources.getDimensionPixelSize(R.dimen.home_content_padding_top)
                    } else {
                        listDivider
                    }
            }

            btnPlay.setOnClickListener {
                activeItem?.let { item ->
                    viewClick.setOnClickListener { onContentClick?.invoke(item) }
                    viewClick.performClick()
                }
            }
            btnWatchLater.setOnClickListener {
                val item = activeItem ?: return@setOnClickListener
                val isFollowed =
                    if (item.id in followedItemIds) {
                        followedItemIds.remove(item.id)
                        false
                    } else {
                        followedItemIds.add(item.id)
                        true
                    }
                btnWatchLater.isSelected = isFollowed
                Toast
                    .makeText(
                        context,
                        if (isFollowed) R.string.added_to_watch_later else R.string.removed_from_watch_later,
                        Toast.LENGTH_SHORT,
                    ).show()
            }
            btnInfo.setOnClickListener {
                activeItem?.let { item -> onContentClick?.invoke(item) }
            }
            pbLoading.visibility = GONE
            errorView.visibility = GONE
        }

        override fun onPageChanged(position: Int) {
            activePosition = position
            binding.ivCarouselBlur.displayBlurImage(activeItem?.thumbnailUrl)
            bindFollowStatus()
        }

        private fun bindFollowStatus() {
            binding.btnWatchLater.isSelected = activeItem?.id in followedItemIds
        }
    }
