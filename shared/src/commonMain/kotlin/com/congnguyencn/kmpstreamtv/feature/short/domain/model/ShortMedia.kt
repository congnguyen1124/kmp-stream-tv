package com.congnguyencn.kmpstreamtv.feature.short.domain.model

data class ShortMedia(
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
    val commentCount: Int,
    val shareCount: Int,
)

data class ShortPage(
    val items: List<ShortMedia>,
    val hasNextPage: Boolean,
)

data class StoryGroup(
    val id: String,
    val providerId: String,
    val items: List<ShortMedia>,
) {
    init {
        require(items.isNotEmpty()) { "Story group $id must not be empty" }
        require(items.all { it.providerId == providerId }) {
            "Story group $id contains stories from another provider"
        }
    }
}
