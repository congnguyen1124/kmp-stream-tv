package com.congnguyencn.kmpstreamtv.feature.story

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.forEachIndexed
import com.congnguyencn.kmpstreamtv.R
import com.google.android.material.progressindicator.LinearProgressIndicator

class StoryGroupProgressView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0,
    ) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {
        private var playingIndex = 0

        fun setup(
            count: Int,
            activeIndex: Int,
        ) {
            if (childCount != count) {
                removeAllViews()
                val margin = resources.getDimensionPixelSize(R.dimen.margin_tiny)
                repeat(count) {
                    addView(
                        createProgressItem(),
                        LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                            marginStart = margin
                            marginEnd = margin
                        },
                    )
                }
            }
            playingIndex = activeIndex.coerceIn(0, (count - 1).coerceAtLeast(0))
            forEachIndexed { index, view ->
                (view as? LinearProgressIndicator)?.progress = if (index < playingIndex) PROGRESS_MAX else 0
            }
        }

        fun updateProgress(fraction: Float) {
            (getChildAt(playingIndex) as? LinearProgressIndicator)?.progress =
                (fraction.coerceIn(0f, 1f) * PROGRESS_MAX).toInt()
        }

        private fun createProgressItem() =
            LinearProgressIndicator(context).apply {
                max = PROGRESS_MAX
                trackThickness = resources.getDimensionPixelSize(R.dimen.linear_progress_height)
                trackCornerRadius = resources.getDimensionPixelSize(R.dimen.radius_small)
                trackColor = context.getColor(R.color.white_30)
                setIndicatorColor(context.getColor(R.color.white))
            }

        private companion object {
            const val PROGRESS_MAX = 10_000
        }
    }
