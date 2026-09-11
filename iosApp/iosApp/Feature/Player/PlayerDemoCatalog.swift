import Foundation

/// Extra local entries used by the native detail screen while the related-content API is pending.
///
/// Port of `PlayerDemoCatalog.kt` — same five fixtures, same order, so both platforms show the
/// same recommendation rail.
enum PlayerDemoCatalog {
    static func recommendations(for media: PlayerMedia) -> [PlayerMedia] {
        (items + [media]).filter { $0.id != media.id }.prefix(5).map { $0 }
    }

    private static let items: [PlayerMedia] = [
        item(
            id: "recommend-basketball",
            url: "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_ts/master.m3u8",
            image: "https://images.pexels.com/photos/9839903/pexels-photo-9839903.jpeg?auto=compress&cs=tinysrgb&w=1600",
            title: "Pulse of the court: the final possession",
            summary: "Two athletes chase one decisive moment through speed, focus, and emotion.",
            subtitle: "18 minutes ago"
        ),
        item(
            id: "recommend-tiger",
            url: "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            image: "https://images.pexels.com/photos/25785873/pexels-photo-25785873.jpeg?auto=compress&cs=tinysrgb&w=1600",
            title: "Realm of the Bengal tiger",
            summary: "A quiet journey through the hidden world of one of Asia's great predators.",
            subtitle: "42 minutes ago"
        ),
        item(
            id: "recommend-tokyo",
            url: "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            image: "https://images.pexels.com/photos/12343886/pexels-photo-12343886.jpeg?auto=compress&cs=tinysrgb&w=1600",
            title: "Tokyo: Tradition in motion",
            summary: "Explore Asakusa, where ancient temples and modern city life meet.",
            subtitle: "1 hour ago"
        ),
        item(
            id: "recommend-festival",
            url: "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
            image: "https://images.pexels.com/photos/30765119/pexels-photo-30765119/free-photo-of-vibrant-traditional-chinese-cultural-festival.jpeg?auto=compress&cs=tinysrgb&w=1600",
            title: "Colors of a Chinese festival",
            summary: "Costume, music, and community rituals bring a celebration to life.",
            subtitle: "Yesterday"
        ),
        item(
            id: "recommend-football",
            url: "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
            image: "https://images.pexels.com/photos/36958062/pexels-photo-36958062.jpeg?auto=compress&cs=tinysrgb&w=1600",
            title: "The decisive touch",
            summary: "A football match turns on one perfectly timed run and a fearless finish.",
            subtitle: "Yesterday"
        ),
    ]

    private static func item(
        id: String,
        url: String,
        image: String,
        title: String,
        summary: String,
        subtitle: String
    ) -> PlayerMedia {
        PlayerMedia(
            id: id,
            url: url,
            title: title,
            thumbnailUrl: image,
            summary: summary,
            ageRestriction: "P",
            isLive: false,
            isShort: false,
            episodeCount: 0,
            providerName: "StreamTV Originals",
            providerAvatarUrl: image,
            subtitle: subtitle,
            durationLabel: "24:10",
            viewCountLabel: "128K views"
        )
    }
}
