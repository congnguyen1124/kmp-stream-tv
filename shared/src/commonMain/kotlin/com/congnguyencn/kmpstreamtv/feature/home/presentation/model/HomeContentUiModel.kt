package com.congnguyencn.kmpstreamtv.feature.home.presentation.model

/** Flat, immutable presentation model kept deliberately friendly to Kotlin/Swift interop. */
data class HomeContentUiModel(
    val id: String,
    val videoUrl: String,
    val trailerUrl: String,
    val thumbnailUrl: String,
    val title: String,
    val description: String,
    val ageRestriction: String?,
    val isLive: Boolean,
    val isShort: Boolean,
    val episodeCount: Int,
    val providerName: String,
    val providerAvatarUrl: String,
    val subtitle: String,
    val durationLabel: String,
    val viewCountLabel: String,
    val progressPercent: Int,
)
