package com.congnguyencn.kmpstreamtv.feature.selection

import android.content.Context
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.sqrt

/** Vertical wheel layout used by the full-screen category picker. */
class SliderLayoutManager(
    context: Context,
) : LinearLayoutManager(context, VERTICAL, false) {
    private var millisecondsPerInch = INITIAL_MILLISECONDS_PER_INCH
    private var recyclerView: RecyclerView? = null

    var selectedPosition: Int = RecyclerView.NO_POSITION
        private set

    override fun onAttachedToWindow(view: RecyclerView?) {
        super.onAttachedToWindow(view)
        recyclerView = view
        view?.clipToPadding = false
        CustomLinearSnapHelper().attachToRecyclerView(view)
    }

    override fun onDetachedFromWindow(
        view: RecyclerView?,
        recycler: RecyclerView.Recycler?,
    ) {
        recyclerView = null
        super.onDetachedFromWindow(view, recycler)
    }

    override fun onLayoutChildren(
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?,
    ) {
        super.onLayoutChildren(recycler, state)
        transformChildren()
        updateSelectedPosition()
    }

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?,
    ): Int {
        val scrolled = super.scrollVerticallyBy(dy, recycler, state)
        transformChildren()
        return scrolled
    }

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            millisecondsPerInch = DEFAULT_MILLISECONDS_PER_INCH
            updateSelectedPosition()
        }
    }

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView,
        state: RecyclerView.State?,
        position: Int,
    ) {
        val smoothScroller =
            object : LinearSmoothScroller(recyclerView.context) {
                override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float =
                    millisecondsPerInch / displayMetrics.densityDpi
            }
        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)
    }

    private fun transformChildren() {
        if (width <= 0) return
        val middle = height / 2f
        for (index in 0 until childCount) {
            val child = getChildAt(index) ?: continue
            val childMiddle = (getDecoratedTop(child) + getDecoratedBottom(child)) / 2f
            val factor = sqrt(abs(middle - childMiddle) / width)
            val scale = (MAX_SCALE - factor * SCALE_FALLOFF).coerceIn(MIN_SCALE, MAX_SCALE)
            child.scaleX = scale
            child.scaleY = scale
            child.alpha = (1f - factor * ALPHA_FALLOFF).coerceIn(MIN_ALPHA, 1f)
        }
    }

    private fun updateSelectedPosition() {
        val attachedRecyclerView = recyclerView ?: return
        val centerY = attachedRecyclerView.height / 2
        selectedPosition =
            (0 until attachedRecyclerView.childCount)
                .map(attachedRecyclerView::getChildAt)
                .minByOrNull { child ->
                    abs(
                        getDecoratedTop(child) +
                            (getDecoratedBottom(child) - getDecoratedTop(child)) / 2 -
                            centerY,
                    )
                }?.let(attachedRecyclerView::getChildLayoutPosition)
                ?: RecyclerView.NO_POSITION
    }

    private companion object {
        const val INITIAL_MILLISECONDS_PER_INCH = 10f
        const val DEFAULT_MILLISECONDS_PER_INCH = 100f
        const val MAX_SCALE = 1.2f
        const val MIN_SCALE = 0.55f
        const val SCALE_FALLOFF = 0.8f
        const val MIN_ALPHA = 0.2f
        const val ALPHA_FALLOFF = 0.8f
    }
}
