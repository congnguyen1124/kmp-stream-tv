package com.congnguyencn.kmpstreamtv.feature.home

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.core.ui.dp

class HomeCategoriesBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : HorizontalScrollView(context, attrs) {
    private val row = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(12.dp, 0, 12.dp, 0)
    }
    private var categories: List<HomeCategory> = emptyList()
    private var selectedId: String = "home"
    private var onSelected: (HomeCategory) -> Unit = {}

    init {
        isHorizontalScrollBarEnabled = false
        isFillViewport = false
        overScrollMode = OVER_SCROLL_NEVER
        addView(row, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
    }

    fun submit(
        items: List<HomeCategory>,
        selected: String = "home",
        onSelected: (HomeCategory) -> Unit,
    ) {
        categories = items
        selectedId = selected
        this.onSelected = onSelected
        render()
    }

    private fun render() {
        row.removeAllViews()
        categories.forEach { category ->
            row.addView(TextView(context).apply {
                text = category.title
                gravity = Gravity.CENTER
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (category.id == selectedId) R.color.stream_text else R.color.stream_text_secondary,
                    ),
                )
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
                setPadding(13.dp, 0, 13.dp, 0)
                minHeight = 40.dp
                background = ContextCompat.getDrawable(
                    context,
                    if (category.id == selectedId) {
                        R.drawable.category_selected_background
                    } else {
                        R.drawable.nav_item_background
                    },
                )
                isClickable = true
                isFocusable = false
                setOnClickListener {
                    if (selectedId != category.id) {
                        selectedId = category.id
                        render()
                    }
                    onSelected(category)
                }
            })
        }
    }
}

data class HomeCategory(val id: String, val title: String)
