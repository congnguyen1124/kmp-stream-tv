package com.congnguyencn.kmpstreamtv.feature.home

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.databinding.ViewMenuItemBinding

class HomeCategoriesBar
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : LinearLayout(context, attrs) {
        private var categories: List<HomeCategory> = emptyList()
        private var onSelected: (HomeCategory) -> Unit = {}

        init {
            orientation = HORIZONTAL
        }

        fun submit(
            items: List<HomeCategory>,
            selected: String = "home",
            onSelected: (HomeCategory) -> Unit,
        ) {
            categories = items
            this.onSelected = onSelected
            post { render(selected) }
        }

        private fun render(selected: String) {
            removeAllViews()
            if (categories.isEmpty()) return

            val probe = ViewMenuItemBinding.inflate(LayoutInflater.from(context), this, false)
            val widths =
                categories.map { category ->
                    probe.root.text = category.title
                    probe.root.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
                    probe.root.measuredWidth
                }
            val parentWidth = width - paddingLeft - paddingRight
            if (widths.sum() < parentWidth) {
                categories.forEach { addMenu(it, selected) }
                gravity = Gravity.START
                return
            }

            probe.root.setText(R.string.category_more)
            probe.root.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_down, 0)
            probe.root.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
            probe.root.setOnClickListener { onSelected(categories.last()) }

            var usedWidth = probe.root.measuredWidth
            for ((index, category) in categories.dropLast(1).withIndex()) {
                if (usedWidth + widths[index] >= parentWidth) break
                addMenu(category, selected)
                addView(View(context), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
                usedWidth += widths[index]
            }
            addView(probe.root)
        }

        private fun addMenu(
            category: HomeCategory,
            selected: String,
        ) {
            ViewMenuItemBinding.inflate(LayoutInflater.from(context), this, true).root.apply {
                text = category.title
                isSelected = category.id == selected
                setOnClickListener { onSelected(category) }
            }
        }
    }

data class HomeCategory(
    val id: String,
    val title: String,
)
