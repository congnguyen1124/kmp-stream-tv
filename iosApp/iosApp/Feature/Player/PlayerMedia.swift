import Shared
import SwiftUI

/// Which of the three layouts the player overlay is currently asked for.
///
/// Port of `PlayerPresentation` in `PlayerFragment.kt`.
enum PlayerPresentation {
    /// Portrait: player card anchored to the top of the overlay, detail list underneath.
    case detail
    /// Landscape: controller-only player filling the host.
    case fullscreen
    /// The floating card that can be dragged, pinched and parked in any corner.
    case mini
}

/// Primitive-only hand-off from shared Home state to the native iOS player.
///
/// Port of `PlayerMedia.kt`. The Android version carries a `Bundle` round trip for process death;
/// the iOS overlay lives in the scene's view tree for as long as the process does, so the value
/// type is all that is needed here.
struct PlayerMedia: Identifiable, Equatable {
    let id: String
    let url: String
    let title: String
    let thumbnailUrl: String
    let summary: String
    let ageRestriction: String?
    let isLive: Bool
    let isShort: Bool
    let episodeCount: Int
    let providerName: String
    let providerAvatarUrl: String
    let subtitle: String
    let durationLabel: String
    let viewCountLabel: String

    /// Identity of the loaded stream, so re-binding the same media does not restart playback.
    var mediaKey: String { "\(id)|\(url)" }

    init(
        id: String,
        url: String,
        title: String,
        thumbnailUrl: String,
        summary: String,
        ageRestriction: String?,
        isLive: Bool,
        isShort: Bool,
        episodeCount: Int,
        providerName: String,
        providerAvatarUrl: String,
        subtitle: String,
        durationLabel: String,
        viewCountLabel: String
    ) {
        self.id = id
        self.url = url
        self.title = title
        self.thumbnailUrl = thumbnailUrl
        self.summary = summary
        self.ageRestriction = ageRestriction
        self.isLive = isLive
        self.isShort = isShort
        self.episodeCount = episodeCount
        self.providerName = providerName
        self.providerAvatarUrl = providerAvatarUrl
        self.subtitle = subtitle
        self.durationLabel = durationLabel
        self.viewCountLabel = viewCountLabel
    }

    init(content: HomeContentUiModel) {
        self.init(
            id: content.id,
            url: content.videoUrl,
            title: content.title,
            thumbnailUrl: content.thumbnailUrl,
            summary: content.description_,
            ageRestriction: content.ageRestriction,
            isLive: content.isLive,
            isShort: content.isShort,
            episodeCount: Int(content.episodeCount),
            providerName: content.providerName,
            providerAvatarUrl: content.providerAvatarUrl,
            subtitle: content.subtitle,
            durationLabel: content.durationLabel,
            viewCountLabel: content.viewCountLabel
        )
    }
}
