package com.congnguyencn.kmpstreamtv.feature.short.data.repository

import com.congnguyencn.kmpstreamtv.feature.short.data.source.ShortDummyDataSource
import com.congnguyencn.kmpstreamtv.feature.short.domain.model.ShortPage
import com.congnguyencn.kmpstreamtv.feature.short.domain.model.StoryGroup
import com.congnguyencn.kmpstreamtv.feature.short.domain.repository.ShortRepository

internal class DummyShortRepository(
    private val dataSource: ShortDummyDataSource,
) : ShortRepository {
    override suspend fun getShorts(
        page: Int,
        pageSize: Int,
    ): ShortPage = dataSource.getShorts(page, pageSize)

    override suspend fun getStoryGroup(initialShortId: String): StoryGroup = dataSource.getStoryGroup(initialShortId)
}
