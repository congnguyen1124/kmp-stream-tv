package com.congnguyencn.kmpstreamtv.feature.player.miniplayer

import android.animation.ValueAnimator
import android.view.animation.PathInterpolator

/**
 * The slice of the `androidx.compose.animation.core.Animatable<Float>` contract that
 * [MinimizableViewState] is written against, implemented on a [ValueAnimator].
 *
 * Three behaviours have to hold, because the ported geometry relies on them:
 * 1. [snapTo] and [animateTo] interrupt whatever is already running on this value. Compose routes
 *    both through `MutatorMutex`, which cancels at equal priority, and the settle logic depends on
 *    it — a `snapTo` issued by a gesture is expected to kill an in-flight settle.
 * 2. [updateBounds] clamps, so a caller cannot push the value outside the range its consumers
 *    accept.
 * 3. [value] is readable at any time, mid-animation included.
 *
 * Compose animates with a spring; a fixed standard-easing curve is the closest equivalent without
 * pulling in `androidx.dynamicanimation`. Only the curve differs — every resting value the state
 * computes is unchanged.
 */
internal class FloatAnimatable(
    initialValue: Float,
    private val onValueChanged: () -> Unit,
) {
    var value: Float = initialValue
        private set

    private var lowerBound = Float.NEGATIVE_INFINITY
    private var upperBound = Float.POSITIVE_INFINITY
    private var animator: ValueAnimator? = null

    fun updateBounds(
        lowerBound: Float,
        upperBound: Float,
    ) {
        this.lowerBound = lowerBound
        this.upperBound = upperBound
        set(value)
    }

    fun snapTo(targetValue: Float) {
        cancel()
        set(targetValue)
    }

    fun animateTo(
        targetValue: Float,
        durationMillis: Long,
    ) {
        cancel()
        val target = targetValue.coerceIn(lowerBound, upperBound)
        if (target == value) return
        animator =
            ValueAnimator.ofFloat(value, target).apply {
                duration = durationMillis
                interpolator = STANDARD_EASING
                addUpdateListener { set(it.animatedValue as Float) }
                start()
            }
    }

    fun cancel() {
        animator?.cancel()
        animator = null
    }

    private fun set(newValue: Float) {
        val clamped = newValue.coerceIn(lowerBound, upperBound)
        if (clamped == value) return
        value = clamped
        onValueChanged()
    }

    private companion object {
        val STANDARD_EASING = PathInterpolator(0.4f, 0f, 0.2f, 1f)
    }
}
