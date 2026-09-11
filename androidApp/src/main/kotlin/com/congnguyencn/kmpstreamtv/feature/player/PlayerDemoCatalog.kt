package com.congnguyencn.kmpstreamtv.feature.player

/** Extra local entries used by the native detail screen while the related-content API is pending. */
internal object PlayerDemoCatalog {
    private val items =
        listOf(
            item(
                "recommend-basketball",
                "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_ts/master.m3u8",
                "https://images.pexels.com/photos/9839903/pexels-photo-9839903.jpeg?auto=compress&cs=tinysrgb&w=1600",
                "Pulse of the court: the final possession",
                "Two athletes chase one decisive moment through speed, focus, and emotion.",
                "18 minutes ago",
            ),
            item(
                "recommend-tiger",
                "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                "https://images.pexels.com/photos/25785873/pexels-photo-25785873.jpeg?auto=compress&cs=tinysrgb&w=1600",
                "Realm of the Bengal tiger",
                "A quiet journey through the hidden world of one of Asia's great predators.",
                "42 minutes ago",
            ),
            item(
                "recommend-tokyo",
                "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                "https://images.pexels.com/photos/12343886/pexels-photo-12343886.jpeg?auto=compress&cs=tinysrgb&w=1600",
                "Tokyo: Tradition in motion",
                "Explore Asakusa, where ancient temples and modern city life meet.",
                "1 hour ago",
            ),
            item(
                "recommend-festival",
                "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
                "https://images.pexels.com/photos/30765119/pexels-photo-30765119/free-photo-of-vibrant-traditional-chinese-cultural-festival.jpeg?auto=compress&cs=tinysrgb&w=1600",
                "Colors of a Chinese festival",
                "Costume, music, and community rituals bring a celebration to life.",
                "Yesterday",
            ),
            item(
                "recommend-football",
                "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
                "https://images.pexels.com/photos/36958062/pexels-photo-36958062.jpeg?auto=compress&cs=tinysrgb&w=1600",
                "The decisive touch",
                "A football match turns on one perfectly timed run and a fearless finish.",
                "Yesterday",
            ),
        )

    fun recommendationsFor(media: PlayerMedia): List<PlayerMedia> =
        (items + media).filterNot { it.id == media.id }.take(5)

    private fun item(
        id: String,
        url: String,
        image: String,
        title: String,
        description: String,
        subtitle: String,
    ) = PlayerMedia(
        id = id,
        url = url,
        title = title,
        thumbnailUrl = image,
        description = description,
        ageRestriction = "P",
        isLive = false,
        isShort = false,
        episodeCount = 0,
        providerName = "StreamTV Originals",
        providerAvatarUrl = image,
        subtitle = subtitle,
        durationLabel = "24:10",
        viewCountLabel = "128K views",
    )
}
