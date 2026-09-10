package com.congnguyencn.kmpstreamtv.feature.home.domain.model

sealed interface Content {
    val id: String
    val videoUrl: String
    val trailerUrl: String
    val thumbnailUrl: String
    val title: String
    val description: String
    val ageRestriction: String?
}

data class Video(
    override val id: String,
    override val videoUrl: String,
    override val trailerUrl: String,
    override val thumbnailUrl: String,
    override val title: String,
    override val description: String,
    override val ageRestriction: String?,
) : Content

data class Series(
    override val id: String,
    override val videoUrl: String,
    override val trailerUrl: String,
    override val thumbnailUrl: String,
    override val title: String,
    override val description: String,
    override val ageRestriction: String?,
    val episodes: List<Video>,
) : Content {
    init {
        require(episodes.isNotEmpty()) { "Series $id must contain at least one episode" }
    }
}

data class Channel(
    override val id: String,
    override val videoUrl: String,
    override val trailerUrl: String,
    override val thumbnailUrl: String,
    override val title: String,
    override val description: String,
    override val ageRestriction: String?,
) : Content

data class ShortVideo(
    override val id: String,
    override val videoUrl: String,
    override val trailerUrl: String,
    override val thumbnailUrl: String,
    override val title: String,
    override val description: String,
    override val ageRestriction: String?,
) : Content
