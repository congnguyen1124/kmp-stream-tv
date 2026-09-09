package com.congnguyencn.kmpstreamtv.core.ui.recyclerview

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class HorizontalSpacingDecoration(
    private val spacing: Int,
    private val edgeSpacing: Int,
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val position = parent.getChildAdapterPosition(view)
        val lastPosition = state.itemCount - 1
        outRect.left = if (position == 0) edgeSpacing else spacing / 2
        outRect.right = if (position == lastPosition) edgeSpacing else spacing / 2
    }
}
