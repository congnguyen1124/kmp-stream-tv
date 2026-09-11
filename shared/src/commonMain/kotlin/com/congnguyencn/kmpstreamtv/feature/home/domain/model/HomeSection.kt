package com.congnguyencn.kmpstreamtv.feature.home.domain.model

data class HomeSection(
    val id: String,
    val title: String,
    val viewType: HomeSectionViewType,
    val items: List<Content>,
    val backgroundUrl: String? = null,
) {
    init {
        require(items.isNotEmpty()) { "Home section $id must not be empty" }
        require(items.all(viewType::accepts)) {
            "Home section $id contains content incompatible with $viewType"
        }
    }
}

enum class HomeSectionViewType {
    Banner,
    VerticalBanner,
    Videos,
    VideosPopular,
    Series,
    Channels,
    Shorts,
    ShortsPopular,
    ContinueWatching,
    MiniApps,
    ;

    fun accepts(content: Content): Boolean =
        when (this) {
            Banner, Videos, VideosPopular, ContinueWatching, MiniApps -> content is Video
            VerticalBanner, Shorts, ShortsPopular -> content is ShortVideo
            Series -> content is com.congnguyencn.kmpstreamtv.feature.home.domain.model.Series
            Channels -> content is Channel
        }
}
