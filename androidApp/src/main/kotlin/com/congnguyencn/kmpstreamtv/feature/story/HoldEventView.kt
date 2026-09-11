package com.congnguyencn.kmpstreamtv.feature.story

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class HoldEventView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0,
    ) : View(context, attrs, defStyleAttr, defStyleRes) {
        private var pressTime = 0L
        private var isHolding = false
        private var onHoldAction: () -> Unit = {}
        private var onHoldReleaseAction: () -> Unit = {}
        private val onHoldTask =
            Runnable {
                isHolding = true
                onHoldAction()
            }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    pressTime = System.currentTimeMillis()
                    removeCallbacks(onHoldTask)
                    postDelayed(onHoldTask, LONG_PRESS_THRESHOLD_MILLIS)
                }

                MotionEvent.ACTION_UP -> {
                    removeCallbacks(onHoldTask)
                    if (System.currentTimeMillis() - pressTime < LONG_PRESS_THRESHOLD_MILLIS) performClick()
                    releaseHold()
                }

                MotionEvent.ACTION_CANCEL -> {
                    removeCallbacks(onHoldTask)
                    releaseHold()
                }
            }
            return true
        }

        override fun performClick(): Boolean {
            super.performClick()
            return true
        }

        fun setOnHoldListener(listener: () -> Unit) {
            onHoldAction = listener
        }

        fun setOnHoldReleaseListener(listener: () -> Unit) {
            onHoldReleaseAction = listener
        }

        private fun releaseHold() {
            if (!isHolding) return
            isHolding = false
            onHoldReleaseAction()
        }

        private companion object {
            const val LONG_PRESS_THRESHOLD_MILLIS = 600L
        }
    }
