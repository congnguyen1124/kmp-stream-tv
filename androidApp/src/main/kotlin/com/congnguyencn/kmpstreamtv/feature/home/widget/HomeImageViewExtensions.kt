package com.congnguyencn.kmpstreamtv.feature.home.widget

import android.webkit.URLUtil
import android.widget.ImageView
import coil3.load
import coil3.request.crossfade
import coil3.request.transformations

/** Matches the active-poster treatment used by the mobile reference Home carousel. */
fun ImageView.displayBlurImage(url: String?) {
    if (!URLUtil.isValidUrl(url)) {
        load(null)
        return
    }
    load(url) {
        crossfade(true)
        transformations(SourceBlurTransformation(context, radius = 5, sampling = 25))
    }
}
