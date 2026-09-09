package com.congnguyencn.kmpstreamtv.feature.home.data.repository

import com.congnguyencn.kmpstreamtv.feature.home.data.source.HomeDummyDataSource
import com.congnguyencn.kmpstreamtv.feature.home.domain.model.HomeSection
import com.congnguyencn.kmpstreamtv.feature.home.domain.repository.HomeRepository

internal class DummyHomeRepository(
    private val dataSource: HomeDummyDataSource,
) : HomeRepository {
    override suspend fun getHomeSections(): List<HomeSection> = dataSource.getHomeSections()
}
