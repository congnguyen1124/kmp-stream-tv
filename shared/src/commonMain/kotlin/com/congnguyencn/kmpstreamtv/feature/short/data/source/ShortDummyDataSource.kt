package com.congnguyencn.kmpstreamtv.feature.short.data.source

import com.congnguyencn.kmpstreamtv.feature.short.domain.model.ShortMedia
import com.congnguyencn.kmpstreamtv.feature.short.domain.model.ShortPage
import com.congnguyencn.kmpstreamtv.feature.short.domain.model.StoryGroup
import kotlinx.coroutines.delay

/** Deterministic short-form catalogue shared by the Android and iOS native renderers. */
internal class ShortDummyDataSource {
    suspend fun getShorts(
        page: Int,
        pageSize: Int,
    ): ShortPage {
        require(page >= 0) { "Page must not be negative" }
        require(pageSize > 0) { "Page size must be positive" }
        delay(DUMMY_LATENCY_MILLIS)
        val start = page * pageSize
        val items = shorts.drop(start).take(pageSize)
        return ShortPage(items = items, hasNextPage = start + items.size < shorts.size)
    }

    suspend fun getStoryGroup(initialShortId: String): StoryGroup {
        delay(DUMMY_LATENCY_MILLIS)
        val initial =
            shorts.firstOrNull { it.id == initialShortId }
                ?: error("Story $initialShortId was not found")
        val stories = shorts.filter { it.providerId == initial.providerId }
        return StoryGroup(
            id = "story-${initial.providerId}",
            providerId = initial.providerId,
            items = stories,
        )
    }

    private companion object {
        const val DUMMY_LATENCY_MILLIS = 120L

        object StreamUrls {
            const val APPLE_TS =
                "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_ts/master.m3u8"
            const val APPLE_FMP4 =
                "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8"
            const val BIG_BUCK_BUNNY = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
            const val SHAKA_ANGEL = "https://storage.googleapis.com/shaka-demo-assets/angel-one-hls/hls.m3u8"
            const val SINTEL = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8"
            const val JW_BUNNY = "https://cdn.jwplayer.com/manifests/pZxWPRg4.m3u8"
            const val MUX_TEST = "https://test-streams.mux.dev/test_001/stream.m3u8"
        }

        object Images {
            const val BASKETBALL =
                "https://images.pexels.com/photos/9839903/pexels-photo-9839903.jpeg?auto=compress&cs=tinysrgb&w=1200"
            const val FOOTBALL =
                "https://images.pexels.com/photos/36958062/pexels-photo-36958062.jpeg?auto=compress&cs=tinysrgb&w=1200"
            const val CRICKET =
                "https://images.pexels.com/photos/11023865/pexels-photo-11023865.jpeg?auto=compress&cs=tinysrgb&w=1200"
            const val TIGER =
                "https://images.pexels.com/photos/12167844/pexels-photo-12167844.jpeg?auto=compress&cs=tinysrgb&w=1200"
            const val FESTIVAL =
                "https://images.pexels.com/photos/30765119/pexels-photo-30765119/free-photo-of-vibrant-traditional-chinese-cultural-festival.jpeg?auto=compress&cs=tinysrgb&w=1200"
            const val NEW_YEAR =
                "https://images.pexels.com/photos/36603900/pexels-photo-36603900.jpeg?auto=compress&cs=tinysrgb&w=1200"
            const val TOKYO =
                "https://images.pexels.com/photos/12343886/pexels-photo-12343886.jpeg?auto=compress&cs=tinysrgb&w=1200"
            const val CEREMONY =
                "https://images.pexels.com/photos/31370378/pexels-photo-31370378.jpeg?auto=compress&cs=tinysrgb&w=1200"
        }

        val shorts =
            listOf(
                short(
                    id = "short-cricket",
                    videoUrl = StreamUrls.JW_BUNNY,
                    imageUrl = Images.CRICKET,
                    title = "Before the strike #cricket",
                    description = "A player finds complete focus before the decisive delivery.",
                    providerId = "streamtv-sport",
                    providerName = "StreamTV Sport",
                    publishedLabel = "12 min ago",
                ),
                short(
                    id = "short-new-year",
                    videoUrl = StreamUrls.APPLE_TS,
                    imageUrl = Images.NEW_YEAR,
                    title = "A spring in red and gold #festival",
                    description = "Lunar New Year comes alive among lanterns and family traditions.",
                    providerId = "streamtv-culture",
                    providerName = "StreamTV Culture",
                    publishedLabel = "18 min ago",
                ),
                short(
                    id = "short-ceremony",
                    videoUrl = StreamUrls.MUX_TEST,
                    imageUrl = Images.CEREMONY,
                    title = "A Japanese ceremony #heritage",
                    description = "Timeless gestures and patient craft shape a quiet ceremony.",
                    providerId = "streamtv-culture",
                    providerName = "StreamTV Culture",
                    publishedLabel = "24 min ago",
                ),
                short(
                    id = "short-tiger",
                    videoUrl = StreamUrls.BIG_BUCK_BUNNY,
                    imageUrl = Images.TIGER,
                    title = "The wild gaze #nature",
                    description = "A Bengal tiger pauses and reveals its quiet power.",
                    providerId = "streamtv-nature",
                    providerName = "StreamTV Nature",
                    publishedLabel = "31 min ago",
                ),
                short(
                    id = "short-football",
                    videoUrl = StreamUrls.SHAKA_ANGEL,
                    imageUrl = Images.FOOTBALL,
                    title = "Motion on the pitch #football",
                    description = "One decisive touch at full speed changes the match.",
                    providerId = "streamtv-sport",
                    providerName = "StreamTV Sport",
                    publishedLabel = "42 min ago",
                ),
                short(
                    id = "short-basketball",
                    videoUrl = StreamUrls.SINTEL,
                    imageUrl = Images.BASKETBALL,
                    title = "Above the rim #basketball",
                    description = "A split-second contest unfolds above the basket.",
                    providerId = "streamtv-sport",
                    providerName = "StreamTV Sport",
                    publishedLabel = "1 hr ago",
                ),
                short(
                    id = "short-tokyo",
                    videoUrl = StreamUrls.APPLE_FMP4,
                    imageUrl = Images.TOKYO,
                    title = "A minute in old Tokyo #travel",
                    description = "Every corner of Asakusa holds another story.",
                    providerId = "streamtv-culture",
                    providerName = "StreamTV Culture",
                    publishedLabel = "2 hr ago",
                ),
                short(
                    id = "short-festival",
                    videoUrl = StreamUrls.BIG_BUCK_BUNNY,
                    imageUrl = Images.FESTIVAL,
                    title = "Festival colors #culture",
                    description = "Traditional costumes, music and movement fill the frame.",
                    providerId = "streamtv-culture",
                    providerName = "StreamTV Culture",
                    publishedLabel = "3 hr ago",
                ),
            )

        fun short(
            id: String,
            videoUrl: String,
            imageUrl: String,
            title: String,
            description: String,
            providerId: String,
            providerName: String,
            publishedLabel: String,
        ): ShortMedia {
            val seed = id.fold(0) { total, character -> total + character.code }
            return ShortMedia(
                id = id,
                videoUrl = videoUrl,
                thumbnailUrl = imageUrl,
                title = title,
                description = description,
                providerId = providerId,
                providerName = providerName,
                providerAvatarUrl = imageUrl,
                publishedLabel = publishedLabel,
                likeCount = 1_200 + seed * 7,
                commentCount = 80 + seed % 700,
                shareCount = 20 + seed % 180,
            )
        }
    }
}
