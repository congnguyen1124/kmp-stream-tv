package com.congnguyencn.kmpstreamtv.feature.player.miniplayer

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs
import kotlin.math.hypot

/**
 * [MotionEvent] port of `customDetectTransformGestures`, the detector the minimized player is
 * driven by in `ottclouds-android`.
 *
 * The classification it has to reproduce, because [MinimizableViewState] routes four different
 * outcomes off it:
 *
 * | Gesture | Reported as |
 * | --- | --- |
 * | pan or pinch past touch slop | [onGesture] per event, then [onGestureEnd] on release |
 * | one finger lifted from a pinch | [onGestureEnd] immediately, gesture over |
 * | single tap | [onTap], only once the double-tap window has passed with no second touch |
 * | double tap | [onDoubleTap] |
 * | long press, or a stream cancelled by an ancestor | nothing |
 *
 * [onGestureEnd] is what settles the card into a corner, so a cancelled stream must not reach it:
 * a child that owns the gesture (the close button, a footer control) would otherwise have its own
 * behaviour overridden by a settle.
 *
 * Pan and zoom are measured from the centroid of every pointer down, the way Compose's
 * `calculatePan` and `calculateZoom` do — pan is the centroid's own travel, zoom the ratio of the
 * mean pointer distance from it.
 */
internal class TransformGestureDetector(
    private val view: View,
    private val onGesture: (panX: Float, panY: Float, zoom: Float) -> Unit,
    private val onGestureEnd: () -> Unit,
    private val onTap: () -> Unit,
    private val onDoubleTap: () -> Unit,
) {
    private val touchSlop = ViewConfiguration.get(view.context).scaledTouchSlop.toFloat()
    private val doubleTapSlop = ViewConfiguration.get(view.context).scaledDoubleTapSlop.toFloat()
    private val longPressTimeoutMillis = ViewConfiguration.getLongPressTimeout().toLong()
    private val doubleTapTimeoutMillis = ViewConfiguration.getDoubleTapTimeout().toLong()

    private var previousCentroidX = 0f
    private var previousCentroidY = 0f
    private var previousCentroidSize = 0f
    private var accumulatedPanX = 0f
    private var accumulatedPanY = 0f
    private var isPastTouchSlop = false
    private var hasMoved = false
    private var isMultiTouch = false
    private var isGestureFinished = false
    private var downTimeMillis = 0L

    private var firstTapUpTimeMillis = 0L
    private var firstTapX = 0f
    private var firstTapY = 0f
    private var isAwaitingSecondTap = false
    private val emitPendingTap =
        Runnable {
            isAwaitingSecondTap = false
            onTap()
        }

    /** True while a pan or pinch is under way, i.e. while this detector owns the stream. */
    val isTransforming: Boolean get() = isPastTouchSlop && !isGestureFinished

    fun onTouchEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // A second touch inside the double-tap window is the only thing that can still
                // turn the previous tap into a double tap, so hold the single tap back until here.
                view.removeCallbacks(emitPendingTap)
                beginGesture(event)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (isGestureFinished) return
                // Two or more fingers put the gesture past touch slop at once, matching Compose.
                isMultiTouch = true
                isPastTouchSlop = true
                resetCentroid(event = event, skippedPointerIndex = NO_POINTER)
            }

            MotionEvent.ACTION_MOVE -> {
                onMove(event)
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (isGestureFinished) return
                if (isMultiTouch && event.pointerCount - 1 == 1) {
                    // Dropping back to a single finger ends the pinch where it stands rather than
                    // letting the remaining finger keep panning.
                    finishGesture(isCanceled = false)
                    return
                }
                resetCentroid(event = event, skippedPointerIndex = event.actionIndex)
            }

            MotionEvent.ACTION_UP -> {
                onUp(event)
            }

            MotionEvent.ACTION_CANCEL -> {
                finishGesture(isCanceled = true)
            }
        }
    }

    fun cancel() {
        view.removeCallbacks(emitPendingTap)
        isAwaitingSecondTap = false
        finishGesture(isCanceled = true)
    }

    private fun beginGesture(event: MotionEvent) {
        downTimeMillis = event.eventTime
        accumulatedPanX = 0f
        accumulatedPanY = 0f
        isPastTouchSlop = false
        hasMoved = false
        isMultiTouch = false
        isGestureFinished = false
        resetCentroid(event = event, skippedPointerIndex = NO_POINTER)
    }

    private fun onMove(event: MotionEvent) {
        if (isGestureFinished) return

        val centroidX = centroidX(event = event, skippedPointerIndex = NO_POINTER)
        val centroidY = centroidY(event = event, skippedPointerIndex = NO_POINTER)
        val centroidSize = centroidSize(event = event, centroidX = centroidX, centroidY = centroidY)
        val panX = centroidX - previousCentroidX
        val panY = centroidY - previousCentroidY

        if (!isPastTouchSlop) {
            accumulatedPanX += panX
            accumulatedPanY += panY
            if (hypot(accumulatedPanX, accumulatedPanY) > touchSlop) {
                isPastTouchSlop = true
            }
        }

        if (isPastTouchSlop) {
            val zoom =
                if (previousCentroidSize == 0f) {
                    NO_ZOOM_CHANGE
                } else {
                    centroidSize / previousCentroidSize
                }
            if (zoom != NO_ZOOM_CHANGE || panX != 0f || panY != 0f) {
                onGesture(panX, panY, zoom)
            }
            hasMoved = true
        }

        previousCentroidX = centroidX
        previousCentroidY = centroidY
        previousCentroidSize = centroidSize
    }

    private fun onUp(event: MotionEvent) {
        if (isGestureFinished) return

        val isLongPressed = event.eventTime - downTimeMillis > longPressTimeoutMillis
        val isTap = !hasMoved && !isMultiTouch && !isLongPressed
        if (!isTap) {
            finishGesture(isCanceled = false)
            return
        }

        isGestureFinished = true
        val isSecondTap =
            isAwaitingSecondTap &&
                event.eventTime - firstTapUpTimeMillis in DOUBLE_TAP_MIN_TIME_MILLIS..doubleTapTimeoutMillis &&
                abs(event.x - firstTapX) <= doubleTapSlop &&
                abs(event.y - firstTapY) <= doubleTapSlop
        if (isSecondTap) {
            isAwaitingSecondTap = false
            onDoubleTap()
            return
        }

        isAwaitingSecondTap = true
        firstTapUpTimeMillis = event.eventTime
        firstTapX = event.x
        firstTapY = event.y
        view.postDelayed(emitPendingTap, doubleTapTimeoutMillis)
    }

    private fun finishGesture(isCanceled: Boolean) {
        if (isGestureFinished) return
        isGestureFinished = true
        if (!isCanceled) {
            onGestureEnd()
        }
    }

    private fun resetCentroid(
        event: MotionEvent,
        skippedPointerIndex: Int,
    ) {
        previousCentroidX = centroidX(event = event, skippedPointerIndex = skippedPointerIndex)
        previousCentroidY = centroidY(event = event, skippedPointerIndex = skippedPointerIndex)
        previousCentroidSize =
            centroidSize(
                event = event,
                centroidX = previousCentroidX,
                centroidY = previousCentroidY,
            )
    }

    private fun centroidX(
        event: MotionEvent,
        skippedPointerIndex: Int,
    ): Float = average(event = event, skippedPointerIndex = skippedPointerIndex, select = event::getX)

    private fun centroidY(
        event: MotionEvent,
        skippedPointerIndex: Int,
    ): Float = average(event = event, skippedPointerIndex = skippedPointerIndex, select = event::getY)

    /** Mean distance of the pointers from the centroid — the "size" Compose divides to get zoom. */
    private fun centroidSize(
        event: MotionEvent,
        centroidX: Float,
        centroidY: Float,
    ): Float {
        var total = 0f
        var count = 0
        for (index in 0 until event.pointerCount) {
            total += hypot(event.getX(index) - centroidX, event.getY(index) - centroidY)
            count++
        }
        return if (count == 0) 0f else total / count
    }

    private inline fun average(
        event: MotionEvent,
        skippedPointerIndex: Int,
        select: (index: Int) -> Float,
    ): Float {
        var total = 0f
        var count = 0
        for (index in 0 until event.pointerCount) {
            if (index == skippedPointerIndex) continue
            total += select(index)
            count++
        }
        return if (count == 0) 0f else total / count
    }

    private companion object {
        const val NO_POINTER = -1
        const val NO_ZOOM_CHANGE = 1f

        /** Compose's `doubleTapMinTimeMillis`: a second tap sooner than this does not count. */
        const val DOUBLE_TAP_MIN_TIME_MILLIS = 40L
    }
}
