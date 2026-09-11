package com.congnguyencn.kmpstreamtv.di

import com.congnguyencn.kmpstreamtv.core.network.StreamTvApiClient
import com.congnguyencn.kmpstreamtv.core.network.createStreamTvHttpClient
import com.congnguyencn.kmpstreamtv.feature.home.data.repository.DummyHomeRepository
import com.congnguyencn.kmpstreamtv.feature.home.data.source.HomeDummyDataSource
import com.congnguyencn.kmpstreamtv.feature.home.domain.repository.HomeRepository
import com.congnguyencn.kmpstreamtv.feature.home.presentation.HomeUiMapper
import com.congnguyencn.kmpstreamtv.feature.home.presentation.HomeViewModel
import com.congnguyencn.kmpstreamtv.feature.short.data.repository.DummyShortRepository
import com.congnguyencn.kmpstreamtv.feature.short.data.source.ShortDummyDataSource
import com.congnguyencn.kmpstreamtv.feature.short.domain.repository.ShortRepository
import com.congnguyencn.kmpstreamtv.feature.short.presentation.ShortUiMapper
import com.congnguyencn.kmpstreamtv.feature.short.presentation.ShortViewModel
import com.congnguyencn.kmpstreamtv.feature.short.presentation.StoryGroupViewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

internal val sharedModule =
    module {
        single { createStreamTvHttpClient() }
        single { StreamTvApiClient(get()) }
        single { HomeDummyDataSource() }
        single<HomeRepository> { DummyHomeRepository(get()) }
        single { HomeUiMapper() }
        factory { HomeViewModel(get(), get()) }
        single { ShortDummyDataSource() }
        single<ShortRepository> { DummyShortRepository(get()) }
        single { ShortUiMapper() }
        factory { ShortViewModel(get(), get()) }
        factory { StoryGroupViewModel(get(), get()) }
    }

private var sharedKoinApplication: KoinApplication? = null

internal fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication =
    sharedKoinApplication ?: startKoin {
        appDeclaration()
        modules(sharedModule)
    }.also { sharedKoinApplication = it }

/** Stable facade used from Swift without exposing Koin lookup syntax to the native UI. */
class SharedDependencies {
    fun homeViewModel(): HomeViewModel = KoinPlatform.getKoin().get()

    fun shortViewModel(): ShortViewModel = KoinPlatform.getKoin().get()

    fun storyGroupViewModel(): StoryGroupViewModel = KoinPlatform.getKoin().get()
}
