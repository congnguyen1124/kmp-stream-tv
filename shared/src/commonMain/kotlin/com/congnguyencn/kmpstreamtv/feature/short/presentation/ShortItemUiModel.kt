package com.congnguyencn.kmpstreamtv.feature.short.presentation

data class ShortItemUiModel(
    val id: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val title: String,
    val description: String,
    val providerId: String,
    val providerName: String,
    val providerAvatarUrl: String,
    val publishedLabel: String,
    val likeCount: Int,
    val likeCountLabel: String,
    val commentCount: Int,
    val commentCountLabel: String,
    val shareCountLabel: String,
    val isLiked: Boolean = false,
    val isFollowingProvider: Boolean = false,
)
