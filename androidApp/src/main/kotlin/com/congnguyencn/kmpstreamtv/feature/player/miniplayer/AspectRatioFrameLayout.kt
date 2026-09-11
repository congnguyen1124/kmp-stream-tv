package com.congnguyencn.kmpstreamtv.feature.player.miniplayer

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * A [FrameLayout] whose height follows its width at a fixed [aspectRatio], or measures like a plain
 * layout when the ratio is [NO_ASPECT_RATIO].
 *
 * This is what `Modifier.aspectRatio(PlayerRatio)` does for the video `Box` in
 * `NewMinimizableView`. A `ConstraintLayout` `layout_constraintDimensionRatio` expresses the same
 * thing, but not inside a card whose own height is `wrap_content`: the mini player's height is
 * exactly what the ratio has to produce, so the ratio cannot be the thing that depends on it.
 */
internal class AspectRatioFrameLayout
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : FrameLayout(context, attrs, defStyleAttr) {
        /** Width-to-height ratio, e.g. `16f / 9`. [NO_ASPECT_RATIO] measures like a plain layout. */
        var aspectRatio: Float = NO_ASPECT_RATIO
            set(value) {
                if (field == value) return
                field = value
                requestLayout()
            }

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            if (aspectRatio <= NO_ASPECT_RATIO) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                return
            }
            val width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
            val height = (width / aspectRatio).toInt()
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
            )
            setMeasuredDimension(width, height)
        }

        internal companion object {
            const val NO_ASPECT_RATIO = 0f
        }
    }
