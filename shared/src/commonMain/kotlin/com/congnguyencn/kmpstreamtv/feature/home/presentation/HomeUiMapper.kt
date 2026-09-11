package com.congnguyencn.kmpstreamtv.feature.home.presentation

import com.congnguyencn.kmpstreamtv.feature.home.domain.model.Channel
import com.congnguyencn.kmpstreamtv.feature.home.domain.model.Content
import com.congnguyencn.kmpstreamtv.feature.home.domain.model.HomeSection
import com.congnguyencn.kmpstreamtv.feature.home.domain.model.HomeSectionViewType
import com.congnguyencn.kmpstreamtv.feature.home.domain.model.Series
import com.congnguyencn.kmpstreamtv.feature.home.domain.model.ShortVideo
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeContentUiModel
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeSectionPresentation
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeSectionUiModel

internal class HomeUiMapper {
    fun map(sections: List<HomeSection>): List<HomeSectionUiModel> =
        sections.map { section ->
            HomeSectionUiModel(
                id = section.id,
                title = section.title,
                items = section.items.map(Content::toUiModel),
                presentation = section.viewType.toPresentation(),
                backgroundUrl = section.backgroundUrl,
                isBanner = section.viewType == HomeSectionViewType.Banner,
                usesPortraitCards = section.viewType in portraitSectionTypes,
                showsRanking = section.viewType in rankedSectionTypes,
            )
        }

    private companion object {
        val portraitSectionTypes =
            setOf(
                HomeSectionViewType.VerticalBanner,
                HomeSectionViewType.Shorts,
                HomeSectionViewType.ShortsPopular,
            )
        val rankedSectionTypes =
            setOf(
                HomeSectionViewType.VideosPopular,
                HomeSectionViewType.ShortsPopular,
            )
    }
}

private fun HomeSectionViewType.toPresentation(): HomeSectionPresentation =
    when (this) {
        HomeSectionViewType.Banner -> HomeSectionPresentation.HighlightWide
        HomeSectionViewType.VerticalBanner -> HomeSectionPresentation.HighlightTall
        HomeSectionViewType.Videos -> HomeSectionPresentation.GeneralWide
        HomeSectionViewType.VideosPopular -> HomeSectionPresentation.TopTen
        HomeSectionViewType.Series -> HomeSectionPresentation.GeneralTall
        HomeSectionViewType.Channels -> HomeSectionPresentation.Circle
        HomeSectionViewType.Shorts -> HomeSectionPresentation.Short
        HomeSectionViewType.ShortsPopular -> HomeSectionPresentation.Story
        HomeSectionViewType.ContinueWatching -> HomeSectionPresentation.ContinueWatching
        HomeSectionViewType.MiniApps -> HomeSectionPresentation.MiniApps
    }

private fun Content.toUiModel() =
    HomeContentUiModel(
        id = id,
        videoUrl = videoUrl,
        trailerUrl = trailerUrl,
        thumbnailUrl = thumbnailUrl,
        title = title,
        description = description,
        ageRestriction = ageRestriction,
        isLive = this is Channel,
        isShort = this is ShortVideo,
        episodeCount = (this as? Series)?.episodes?.size ?: 0,
        providerName =
            when (this) {
                is Channel -> title
                is ShortVideo -> "StreamTV Stories"
                is Series -> "StreamTV Series"
                else -> "StreamTV Originals"
            },
        providerAvatarUrl = thumbnailUrl,
        subtitle =
            when (this) {
                is Channel -> "Live now"
                is Series -> "${episodes.size} episodes"
                is ShortVideo -> "Short video"
                else -> "Episode ${1 + seed() % 8}"
            },
        durationLabel =
            when (this) {
                is Channel -> "LIVE"
                is ShortVideo -> "0:${durationSeconds()}"
                else -> "${durationMinutes()}:${durationSeconds()}"
            },
        viewCountLabel = "${viewCount()}K views",
        progressPercent = progress(),
    )

private fun Content.seed(): Int = id.fold(0) { total, character -> total + character.code }

private fun Content.durationMinutes(): Int = 18 + seed() % 35

private fun Content.durationSeconds(): String = (10 + seed() % 50).toString().padStart(2, '0')

private fun Content.viewCount(): Int = 12 + seed() % 880

private fun Content.progress(): Int = 18 + seed() % 68
