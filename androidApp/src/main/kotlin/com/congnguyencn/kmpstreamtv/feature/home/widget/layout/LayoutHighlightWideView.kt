package com.congnguyencn.kmpstreamtv.feature.home.widget.layout

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.congnguyencn.kmpstreamtv.databinding.LayoutHighlightWideViewBinding
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeContentUiModel
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeSectionUiModel
import com.congnguyencn.kmpstreamtv.feature.home.widget.CarouseView

class LayoutHighlightWideView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0,
    ) : HomeLayoutBaseView(context, attrs, defStyleAttr, defStyleRes),
        CarouseView.OnPageChangeCallback {
        private val binding =
            LayoutHighlightWideViewBinding.inflate(LayoutInflater.from(context), this, true)
        private var items: List<HomeContentUiModel> = emptyList()

        init {
            binding.carouselView.onPageChangeCallback = this
            binding.carouselView.onItemClickListener =
                CarouseView.OnItemClickCallback { position ->
                    items.getOrNull(position)?.let { activeItem -> onContentClick?.invoke(activeItem) }
                }
            binding.errorView.setOnActionButtonClicked {
                binding.errorView.visibility = GONE
            }
        }

        fun bindLayout(data: HomeSectionUiModel) =
            with(binding) {
                items = data.items
                bindLayoutBackground(ivLayoutBackground, data.backgroundUrl)
                carouselView.bindItems(items)
                pbLoading.visibility = GONE
                errorView.visibility = GONE
            }

        override fun onPageChanged(position: Int) = Unit
    }
