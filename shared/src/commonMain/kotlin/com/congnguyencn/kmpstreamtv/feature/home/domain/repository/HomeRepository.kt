package com.congnguyencn.kmpstreamtv.feature.home.domain.repository

import com.congnguyencn.kmpstreamtv.feature.home.domain.model.HomeSection

interface HomeRepository {
    suspend fun getHomeSections(): List<HomeSection>
}
