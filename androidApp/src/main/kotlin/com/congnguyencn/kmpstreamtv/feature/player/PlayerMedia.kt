package com.congnguyencn.kmpstreamtv.feature.player

import android.os.Bundle
import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeContentUiModel

/** Primitive-only hand-off from shared Home state to the native Android player. */
internal data class PlayerMedia(
    val id: String,
    val url: String,
    val title: String,
    val thumbnailUrl: String,
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
) {
    fun toBundle(): Bundle = Bundle().apply {
        putString(KEY_ID, id)
        putString(KEY_URL, url)
        putString(KEY_TITLE, title)
        putString(KEY_THUMBNAIL_URL, thumbnailUrl)
        putString(KEY_DESCRIPTION, description)
        putString(KEY_AGE_RESTRICTION, ageRestriction)
        putBoolean(KEY_IS_LIVE, isLive)
        putBoolean(KEY_IS_SHORT, isShort)
        putInt(KEY_EPISODE_COUNT, episodeCount)
        putString(KEY_PROVIDER_NAME, providerName)
        putString(KEY_PROVIDER_AVATAR_URL, providerAvatarUrl)
        putString(KEY_SUBTITLE, subtitle)
        putString(KEY_DURATION_LABEL, durationLabel)
        putString(KEY_VIEW_COUNT_LABEL, viewCountLabel)
    }

    companion object {
        fun from(content: HomeContentUiModel) = PlayerMedia(
            id = content.id,
            url = content.videoUrl,
            title = content.title,
            thumbnailUrl = content.thumbnailUrl,
            description = content.description,
            ageRestriction = content.ageRestriction,
            isLive = content.isLive,
            isShort = content.isShort,
            episodeCount = content.episodeCount,
            providerName = content.providerName,
            providerAvatarUrl = content.providerAvatarUrl,
            subtitle = content.subtitle,
            durationLabel = content.durationLabel,
            viewCountLabel = content.viewCountLabel,
        )

        fun from(bundle: Bundle): PlayerMedia? {
            val url = bundle.getString(KEY_URL).orEmpty()
            if (url.isBlank()) return null
            return PlayerMedia(
                id = bundle.getString(KEY_ID).orEmpty(),
                url = url,
                title = bundle.getString(KEY_TITLE).orEmpty(),
                thumbnailUrl = bundle.getString(KEY_THUMBNAIL_URL).orEmpty(),
                description = bundle.getString(KEY_DESCRIPTION).orEmpty(),
                ageRestriction = bundle.getString(KEY_AGE_RESTRICTION),
                isLive = bundle.getBoolean(KEY_IS_LIVE),
                isShort = bundle.getBoolean(KEY_IS_SHORT),
                episodeCount = bundle.getInt(KEY_EPISODE_COUNT),
                providerName = bundle.getString(KEY_PROVIDER_NAME).orEmpty(),
                providerAvatarUrl = bundle.getString(KEY_PROVIDER_AVATAR_URL).orEmpty(),
                subtitle = bundle.getString(KEY_SUBTITLE).orEmpty(),
                durationLabel = bundle.getString(KEY_DURATION_LABEL).orEmpty(),
                viewCountLabel = bundle.getString(KEY_VIEW_COUNT_LABEL).orEmpty(),
            )
        }

        private const val KEY_ID = "player_media_id"
        private const val KEY_URL = "player_media_url"
        private const val KEY_TITLE = "player_media_title"
        private const val KEY_THUMBNAIL_URL = "player_media_thumbnail_url"
        private const val KEY_DESCRIPTION = "player_media_description"
        private const val KEY_AGE_RESTRICTION = "player_media_age_restriction"
        private const val KEY_IS_LIVE = "player_media_is_live"
        private const val KEY_IS_SHORT = "player_media_is_short"
        private const val KEY_EPISODE_COUNT = "player_media_episode_count"
        private const val KEY_PROVIDER_NAME = "player_media_provider_name"
        private const val KEY_PROVIDER_AVATAR_URL = "player_media_provider_avatar_url"
        private const val KEY_SUBTITLE = "player_media_subtitle"
        private const val KEY_DURATION_LABEL = "player_media_duration_label"
        private const val KEY_VIEW_COUNT_LABEL = "player_media_view_count_label"
    }
}
