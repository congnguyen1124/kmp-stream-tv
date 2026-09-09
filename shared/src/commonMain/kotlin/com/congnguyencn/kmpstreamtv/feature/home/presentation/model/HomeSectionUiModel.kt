package com.congnguyencn.kmpstreamtv.feature.home.presentation.model

data class HomeSectionUiModel(
    val id: String,
    val title: String,
    val items: List<HomeContentUiModel>,
    val presentation: HomeSectionPresentation,
    val backgroundUrl: String?,
    val isBanner: Boolean,
    val usesPortraitCards: Boolean,
    val showsRanking: Boolean,
)

/** Semantic layout families rendered independently by Android Views and SwiftUI. */
enum class HomeSectionPresentation {
    HighlightWide,
    HighlightTall,
    GeneralWide,
    TopTen,
    GeneralTall,
    Circle,
    Short,
    Story,
    ContinueWatching,
    MiniApps,
}
