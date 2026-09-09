package com.congnguyencn.kmpstreamtv.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.android.logger.AndroidLogger
import org.koin.core.logger.Level

fun startKoinForAndroid(context: Context) {
    initKoin {
        logger(AndroidLogger(Level.ERROR))
        androidContext(context.applicationContext)
    }
}
