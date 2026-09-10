package com.congnguyencn.kmpstreamtv.feature.home

import com.congnguyencn.kmpstreamtv.feature.home.domain.model.HomeSection
import com.congnguyencn.kmpstreamtv.feature.home.domain.model.HomeSectionViewType
import com.congnguyencn.kmpstreamtv.feature.home.domain.model.Short
import com.congnguyencn.kmpstreamtv.feature.home.data.source.HomeDummyDataSource
import com.congnguyencn.kmpstreamtv.feature.home.presentation.HomeUiMapper
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeSectionPresentation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class HomeModelTest {
    @Test
    fun bannerRejectsPortraitContent() {
        val short = Short("id", "video", "trailer", "image", "title", "description", null)

        assertFailsWith<IllegalArgumentException> {
            HomeSection("featured", "Featured", HomeSectionViewType.Banner, listOf(short))
        }
    }

    @Test
    fun dummyCatalogueContainsEveryHomeSection() = runTest {
        val sections = HomeDummyDataSource().getHomeSections()

        assertEquals(HomeSectionViewType.entries.toSet(), sections.map { it.viewType }.toSet())
        assertEquals(10, sections.size)
        assertEquals(HomeSectionViewType.ShortsPopular, sections.first().viewType)
    }

    @Test
    fun mapperProvidesEveryNativeLayoutFamilyAndCardMetadata() = runTest {
        val uiSections = HomeUiMapper().map(HomeDummyDataSource().getHomeSections())

        assertEquals(HomeSectionPresentation.entries.toSet(), uiSections.map { it.presentation }.toSet())
        assertEquals(HomeSectionPresentation.Story, uiSections.first().presentation)
        assertTrue(uiSections.flatMap { it.items }.all { it.progressPercent in 1..99 })
        assertTrue(uiSections.flatMap { it.items }.all { it.durationLabel.isNotBlank() })
        assertTrue(uiSections.flatMap { it.items }.all { it.providerName.isNotBlank() })
    }
}
