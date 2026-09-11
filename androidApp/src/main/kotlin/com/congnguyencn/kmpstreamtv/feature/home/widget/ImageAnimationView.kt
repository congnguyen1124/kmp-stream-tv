package com.congnguyencn.kmpstreamtv.feature.home.widget

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.imageview.ShapeableImageView

/** Keeps the source short-card view contract; the KMP fixture currently provides one poster frame. */
class ImageAnimationView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : ShapeableImageView(context, attrs, defStyleAttr)
