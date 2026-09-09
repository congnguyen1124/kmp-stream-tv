package com.congnguyencn.kmpstreamtv

import android.app.Application
import com.congnguyencn.kmpstreamtv.di.startKoinForAndroid

class StreamTvApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoinForAndroid(this)
    }
}
