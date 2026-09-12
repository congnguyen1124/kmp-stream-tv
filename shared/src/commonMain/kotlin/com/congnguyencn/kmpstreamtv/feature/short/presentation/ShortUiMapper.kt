package com.congnguyencn.kmpstreamtv.feature.short.presentation

import com.congnguyencn.kmpstreamtv.feature.short.domain.model.ShortMedia

internal class ShortUiMapper {
    fun map(item: ShortMedia): ShortItemUiModel =
        ShortItemUiModel(
            id = item.id,
            videoUrl = item.videoUrl,
            thumbnailUrl = item.thumbnailUrl,
            title = item.title,
            description = item.description,
            providerId = item.providerId,
            providerName = item.providerName,
            providerAvatarUrl = item.providerAvatarUrl,
            publishedLabel = item.publishedLabel,
            likeCount = item.likeCount,
            likeCountLabel = item.likeCount.compactCount(),
            commentCount = item.commentCount,
            commentCountLabel = item.commentCount.compactCount(),
            shareCountLabel = item.shareCount.compactCount(),
        )
}

internal fun Int.compactCount(): String =
    when {
        this >= 1_000_000 -> "${formatSingleDecimal(this / 1_000_000.0)}M"
        this >= 1_000 -> "${formatSingleDecimal(this / 1_000.0)}K"
        else -> toString()
    }

private fun formatSingleDecimal(value: Double): String {
    val tenths = (value * 10).toInt()
    return if (tenths % 10 == 0) {
        (tenths / 10).toString()
    } else {
        "${tenths / 10}.${tenths % 10}"
    }
}
