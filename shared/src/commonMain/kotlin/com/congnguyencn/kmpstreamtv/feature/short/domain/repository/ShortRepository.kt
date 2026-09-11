package com.congnguyencn.kmpstreamtv.feature.short.domain.repository

import com.congnguyencn.kmpstreamtv.feature.short.domain.model.ShortPage
import com.congnguyencn.kmpstreamtv.feature.short.domain.model.StoryGroup

interface ShortRepository {
    suspend fun getShorts(
        page: Int,
        pageSize: Int,
    ): ShortPage

    suspend fun getStoryGroup(initialShortId: String): StoryGroup
}
