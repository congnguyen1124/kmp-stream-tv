package com.congnguyencn.kmpstreamtv.feature.home.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.congnguyencn.kmpstreamtv.R
import com.congnguyencn.kmpstreamtv.databinding.ViewLayoutErrorBinding

class LayoutErrorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val binding = ViewLayoutErrorBinding.inflate(LayoutInflater.from(context), this)
    private var retryCallback: () -> Unit = {}

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        binding.tvErrorTitle.setText(R.string.layout_error_title)
        binding.tvErrorMessage.setOnClickListener { retryCallback() }
    }

    fun setOnActionButtonClicked(callback: () -> Unit) {
        retryCallback = callback
    }
}
