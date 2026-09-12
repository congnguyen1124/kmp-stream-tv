package com.congnguyencn.kmpstreamtv.feature.selection

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import kotlin.math.abs
import kotlin.math.roundToInt

/** Centers the closest selection row while preserving the first and last padded endpoints. */
class CustomLinearSnapHelper : SnapHelper() {
    private var verticalHelper: OrientationHelper? = null

    override fun calculateDistanceToFinalSnap(
        layoutManager: RecyclerView.LayoutManager,
        targetView: View,
    ): IntArray =
        intArrayOf(
            0,
            if (layoutManager.canScrollVertically()) {
                distanceToCenter(layoutManager, targetView, verticalHelper(layoutManager))
            } else {
                0
            },
        )

    override fun findSnapView(layoutManager: RecyclerView.LayoutManager): View? {
        if (!layoutManager.canScrollVertically()) return null
        val linearLayoutManager = layoutManager as? LinearLayoutManager
        if (linearLayoutManager?.findFirstCompletelyVisibleItemPosition() == 0) {
            return layoutManager.getChildAt(0)
        }
        if (
            linearLayoutManager?.findLastCompletelyVisibleItemPosition() ==
            layoutManager.itemCount - 1
        ) {
            return layoutManager.getChildAt(layoutManager.childCount - 1)
        }
        return closestToCenter(layoutManager, verticalHelper(layoutManager))
    }

    override fun findTargetSnapPosition(
        layoutManager: RecyclerView.LayoutManager,
        velocityX: Int,
        velocityY: Int,
    ): Int {
        if (layoutManager !is RecyclerView.SmoothScroller.ScrollVectorProvider) {
            return RecyclerView.NO_POSITION
        }
        val itemCount = layoutManager.itemCount
        if (itemCount == 0) return RecyclerView.NO_POSITION
        val currentView = findSnapView(layoutManager) ?: return RecyclerView.NO_POSITION
        val currentPosition = layoutManager.getPosition(currentView)
        if (currentPosition == RecyclerView.NO_POSITION) return RecyclerView.NO_POSITION
        val vector =
            layoutManager.computeScrollVectorForPosition(itemCount - 1)
                ?: return RecyclerView.NO_POSITION
        var delta = estimateNextPositionDiffForFling(layoutManager, velocityY)
        if (vector.y < 0) delta = -delta
        if (delta == 0) return RecyclerView.NO_POSITION
        return (currentPosition + delta).coerceIn(0, itemCount - 1)
    }

    private fun distanceToCenter(
        layoutManager: RecyclerView.LayoutManager,
        targetView: View,
        helper: OrientationHelper,
    ): Int {
        val childCenter =
            helper.getDecoratedStart(targetView) + helper.getDecoratedMeasurement(targetView) / 2
        val containerCenter =
            if (layoutManager.clipToPadding) {
                helper.startAfterPadding + helper.totalSpace / 2
            } else {
                helper.end / 2
            }
        return childCenter - containerCenter
    }

    private fun closestToCenter(
        layoutManager: RecyclerView.LayoutManager,
        helper: OrientationHelper,
    ): View? {
        val center =
            if (layoutManager.clipToPadding) {
                helper.startAfterPadding + helper.totalSpace / 2
            } else {
                helper.end / 2
            }
        return (0 until layoutManager.childCount)
            .mapNotNull(layoutManager::getChildAt)
            .minByOrNull { child ->
                abs(
                    helper.getDecoratedStart(child) +
                        helper.getDecoratedMeasurement(child) / 2 -
                        center,
                )
            }
    }

    private fun estimateNextPositionDiffForFling(
        layoutManager: RecyclerView.LayoutManager,
        velocityY: Int,
    ): Int {
        val distancePerChild = computeDistancePerChild(layoutManager, verticalHelper(layoutManager))
        if (distancePerChild <= 0f) return 0
        return (calculateScrollDistance(0, velocityY)[1] / distancePerChild).roundToInt()
    }

    private fun computeDistancePerChild(
        layoutManager: RecyclerView.LayoutManager,
        helper: OrientationHelper,
    ): Float {
        val positionedChildren =
            (0 until layoutManager.childCount)
                .mapNotNull(layoutManager::getChildAt)
                .mapNotNull { child ->
                    layoutManager
                        .getPosition(child)
                        .takeUnless { it == RecyclerView.NO_POSITION }
                        ?.let { position -> position to child }
                }
        val first = positionedChildren.minByOrNull { it.first } ?: return INVALID_DISTANCE
        val last = positionedChildren.maxByOrNull { it.first } ?: return INVALID_DISTANCE
        val start = minOf(helper.getDecoratedStart(first.second), helper.getDecoratedStart(last.second))
        val end = maxOf(helper.getDecoratedEnd(first.second), helper.getDecoratedEnd(last.second))
        val distance = end - start
        return if (distance == 0) INVALID_DISTANCE else distance.toFloat() / (last.first - first.first + 1)
    }

    private fun verticalHelper(layoutManager: RecyclerView.LayoutManager): OrientationHelper {
        val current = verticalHelper
        if (current == null || current.layoutManager !== layoutManager) {
            verticalHelper = OrientationHelper.createVerticalHelper(layoutManager)
        }
        return requireNotNull(verticalHelper)
    }

    private companion object {
        const val INVALID_DISTANCE = 1f
    }
}
