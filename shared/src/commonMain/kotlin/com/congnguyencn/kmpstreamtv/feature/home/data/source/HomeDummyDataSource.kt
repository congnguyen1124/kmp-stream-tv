package com.congnguyencn.kmpstreamtv.feature.home.data.source

import com.congnguyencn.kmpstreamtv.feature.home.domain.model.Channel
import com.congnguyencn.kmpstreamtv.feature.home.domain.model.HomeSection
import com.congnguyencn.kmpstreamtv.feature.home.domain.model.HomeSectionViewType
import com.congnguyencn.kmpstreamtv.feature.home.domain.model.Series
import com.congnguyencn.kmpstreamtv.feature.home.domain.model.Short
import com.congnguyencn.kmpstreamtv.feature.home.domain.model.Video
import kotlinx.coroutines.delay

/** Deterministic catalogue: android_stream_tv media rendered through on-tv-android layout families. */
internal class HomeDummyDataSource {
    suspend fun getHomeSections(): List<HomeSection> {
        delay(DUMMY_LATENCY_MILLIS)
        return listOf(
            HomeSection("popular-shorts", "Stories for you", HomeSectionViewType.ShortsPopular, shorts),
            HomeSection("featured", "Featured today", HomeSectionViewType.Banner, videos),
            HomeSection("for-you", "Videos for you", HomeSectionViewType.Videos, videos.reversed()),
            HomeSection("popular-videos", "Popular videos", HomeSectionViewType.VideosPopular, videos),
            HomeSection("series", "Documentary series", HomeSectionViewType.Series, series),
            HomeSection("channels", "Live channels", HomeSectionViewType.Channels, channels),
            HomeSection("portrait", "Editor's spotlight", HomeSectionViewType.VerticalBanner, shorts),
            HomeSection("continue", "Continue watching", HomeSectionViewType.ContinueWatching, videos.drop(2) + videos.take(2)),
            HomeSection("shorts", "Fresh shorts", HomeSectionViewType.Shorts, shorts.reversed()),
            HomeSection("mini-apps", "Explore StreamTV", HomeSectionViewType.MiniApps, videos.take(5)),
        )
    }

    private companion object {
        const val DUMMY_LATENCY_MILLIS = 120L

        object StreamUrls {
            const val APPLE_TS =
                "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_ts/master.m3u8"
            const val APPLE_FMP4 =
                "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8"
            const val TEARS_OF_STEEL =
                "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
            const val BIG_BUCK_BUNNY = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
            const val SHAKA_ANGEL = "https://storage.googleapis.com/shaka-demo-assets/angel-one-hls/hls.m3u8"
            const val SINTEL = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8"
            const val JW_BUNNY = "https://cdn.jwplayer.com/manifests/pZxWPRg4.m3u8"
            const val MUX_TEST = "https://test-streams.mux.dev/test_001/stream.m3u8"
            const val AKAMAI_LIVE = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8"
            const val SHAKA_LIVE = "https://storage.googleapis.com/shaka-live-assets/player-source.m3u8"
        }

        object Images {
            const val BASKETBALL =
                "https://images.pexels.com/photos/9839903/pexels-photo-9839903.jpeg?auto=compress&cs=tinysrgb&w=1600"
            const val FOOTBALL =
                "https://images.pexels.com/photos/36958062/pexels-photo-36958062.jpeg?auto=compress&cs=tinysrgb&w=1600"
            const val CRICKET =
                "https://images.pexels.com/photos/11023865/pexels-photo-11023865.jpeg?auto=compress&cs=tinysrgb&w=1200"
            const val TIGER =
                "https://images.pexels.com/photos/25785873/pexels-photo-25785873.jpeg?auto=compress&cs=tinysrgb&w=1600"
            const val TIGER_PORTRAIT =
                "https://images.pexels.com/photos/12167844/pexels-photo-12167844.jpeg?auto=compress&cs=tinysrgb&w=1200"
            const val FESTIVAL =
                "https://images.pexels.com/photos/30765119/pexels-photo-30765119/free-photo-of-vibrant-traditional-chinese-cultural-festival.jpeg?auto=compress&cs=tinysrgb&w=1600"
            const val NEW_YEAR =
                "https://images.pexels.com/photos/36603900/pexels-photo-36603900.jpeg?auto=compress&cs=tinysrgb&w=1200"
            const val TOKYO =
                "https://images.pexels.com/photos/12343886/pexels-photo-12343886.jpeg?auto=compress&cs=tinysrgb&w=1600"
            const val CEREMONY =
                "https://images.pexels.com/photos/31370378/pexels-photo-31370378.jpeg?auto=compress&cs=tinysrgb&w=1200"
        }

        val videos = listOf(
            video("basketball", StreamUrls.APPLE_TS, StreamUrls.APPLE_FMP4, Images.BASKETBALL,
                "Pulse of the court", "Two athletes chase one decisive moment through speed, focus, and emotion."),
            video("tiger", StreamUrls.TEARS_OF_STEEL, StreamUrls.SINTEL, Images.TIGER,
                "Realm of the Bengal tiger", "A quiet journey through the hidden world of one of Asia's great predators.", "T13"),
            video("tokyo", StreamUrls.BIG_BUCK_BUNNY, StreamUrls.TEARS_OF_STEEL, Images.TOKYO,
                "Tokyo: Tradition in motion", "Explore Asakusa, where ancient temples and modern city life meet."),
            video("festival", StreamUrls.TEARS_OF_STEEL, StreamUrls.BIG_BUCK_BUNNY, Images.FESTIVAL,
                "Colors of a Chinese festival", "Costume, music, and community rituals bring a celebration to life."),
            video("football", StreamUrls.APPLE_FMP4, StreamUrls.SHAKA_ANGEL, Images.FOOTBALL,
                "The decisive touch", "A football match turns on one perfectly timed run and a fearless finish."),
            video("cricket", StreamUrls.SHAKA_ANGEL, StreamUrls.SINTEL, Images.CRICKET,
                "Under pressure at the crease", "A batter prepares for the delivery that could decide the match."),
            video("new-year", StreamUrls.SINTEL, StreamUrls.APPLE_TS, Images.NEW_YEAR,
                "Welcoming the new spring", "Red, gold, and generations of tradition fill a joyful Lunar New Year."),
            video("ceremony", StreamUrls.BIG_BUCK_BUNNY, StreamUrls.APPLE_FMP4, Images.CEREMONY,
                "Grace in every gesture", "A close look at the discipline and meaning of a Japanese ceremony."),
        )

        val shorts = listOf(
            short("cricket", StreamUrls.JW_BUNNY, Images.CRICKET, "Before the strike", "A player finds complete focus."),
            short("new-year", StreamUrls.APPLE_TS, Images.NEW_YEAR, "A spring in red and gold", "Lunar New Year among lanterns."),
            short("ceremony", StreamUrls.MUX_TEST, Images.CEREMONY, "A Japanese ceremony", "Timeless gestures shape a ceremony."),
            short("tiger", StreamUrls.BIG_BUCK_BUNNY, Images.TIGER_PORTRAIT, "The wild gaze", "A tiger's quiet power."),
            short("football", StreamUrls.SHAKA_ANGEL, Images.FOOTBALL, "Motion on the pitch", "One decisive touch at full speed."),
            short("basketball", StreamUrls.SINTEL, Images.BASKETBALL, "Above the rim", "A split-second contest above the basket."),
            short("tokyo", StreamUrls.APPLE_FMP4, Images.TOKYO, "A minute in old Tokyo", "Every corner holds a story."),
            short("festival", StreamUrls.BIG_BUCK_BUNNY, Images.FESTIVAL, "Festival colors", "Traditional costumes fill the frame."),
        )

        val series = listOf(
            series("wild-asia", Images.TIGER, "Wild Asia", "Asia's landscapes and remarkable wildlife.", videos[1], videos[0]),
            series("heritage", Images.TOKYO, "Living heritage of East Asia", "People and living traditions of China and Japan.", videos[2], videos[3]),
            series("performance", Images.BASKETBALL, "The edge of performance", "Preparation becomes instinct under pressure.", videos[0], videos[4]),
            series("rituals", Images.CEREMONY, "Rituals of Asia", "Ceremonies that connect past and present.", videos[7], videos[6], videos[3]),
        )

        val channels = listOf(
            channel("sport", Images.BASKETBALL, "StreamTV Sport", "The day's biggest sporting moments."),
            channel("nature", Images.TIGER, "StreamTV Nature", "An uninterrupted window into the wild.", StreamUrls.SHAKA_LIVE),
            channel("football", Images.FOOTBALL, "StreamTV Football", "Live matches and tactical analysis."),
            channel("cricket", Images.CRICKET, "StreamTV Cricket", "International cricket throughout the day.", StreamUrls.SHAKA_LIVE),
            channel("culture", Images.FESTIVAL, "StreamTV Culture", "Festivals, art, food, and living traditions."),
            channel("cities", Images.TOKYO, "StreamTV Cities", "The streets and rhythms of remarkable cities.", StreamUrls.SHAKA_LIVE),
        )

        fun video(
            id: String,
            url: String,
            trailer: String,
            image: String,
            title: String,
            description: String,
            age: String = "P",
        ) = Video("video-$id", url, trailer, image, title, description, age)

        fun short(id: String, url: String, image: String, title: String, description: String) = Short(
            id = "short-$id",
            videoUrl = url,
            trailerUrl = StreamUrls.APPLE_FMP4,
            thumbnailUrl = image,
            title = title,
            description = description,
            ageRestriction = "P",
        )

        fun series(id: String, image: String, title: String, description: String, vararg episodes: Video) = Series(
            id = "series-$id",
            videoUrl = episodes.first().videoUrl,
            trailerUrl = episodes.first().trailerUrl,
            thumbnailUrl = image,
            title = title,
            description = description,
            ageRestriction = episodes.first().ageRestriction,
            episodes = episodes.toList(),
        )

        fun channel(
            id: String,
            image: String,
            title: String,
            description: String,
            url: String = StreamUrls.AKAMAI_LIVE,
        ) = Channel(
            id = "channel-$id",
            videoUrl = url,
            trailerUrl = StreamUrls.APPLE_TS,
            thumbnailUrl = image,
            title = title,
            description = description,
            ageRestriction = "P",
        )
    }
}
