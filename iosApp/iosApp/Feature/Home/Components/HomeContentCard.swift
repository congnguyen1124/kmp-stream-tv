import Shared
import SwiftUI

enum HomeContentCardStyle {
    case landscape
    case portrait
    case circle
    case short
    case story
    case continueWatching
    case topTen
}

/// Mirrors `HomeContentAdapter.isDummyExclusive`, which drives the Android exclusive tag.
extension HomeContentUiModel {
    var isDummyExclusive: Bool {
        id.utf16.reduce(0) { total, unit in total + Int(unit) } % 5 == 0
    }
}

struct HomeContentCard: View {
    let item: HomeContentUiModel
    let style: HomeContentCardStyle
    var rank: Int = 0

    @ViewBuilder
    var body: some View {
        switch style {
        case .landscape:
            ThumbnailContentCard(item: item, size: StreamCardSize.ephemeralWide)
        case .portrait:
            ThumbnailContentCard(item: item, size: StreamCardSize.ephemeralTall)
        case .circle:
            CircleContentCard(item: item)
        case .short:
            ShortContentCard(item: item)
        case .story:
            StoryContentCard(item: item)
        case .continueWatching:
            ContinueWatchingCard(item: item)
        case .topTen:
            TopTenContentCard(item: item, rank: rank)
        }
    }
}

/// `item_thumbnail.xml`
private struct ThumbnailContentCard: View {
    let item: HomeContentUiModel
    let size: CGSize

    var body: some View {
        RemoteArtwork(url: item.thumbnailUrl, placeholder: .branded)
            .frame(width: size.width, height: size.height)
            .clipShape(RoundedRectangle(cornerRadius: StreamMetrics.thumbnailCorner, style: .continuous))
            .overlay(alignment: .topLeading) {
                if item.isDummyExclusive {
                    StreamExclusiveBadge().padding(6)
                }
            }
            .overlay(alignment: .bottomLeading) {
                if item.isLive {
                    StreamBadge(text: "LIVE", color: .streamLive)
                        .padding(4)
                }
            }
    }
}

/// `item_circle.xml`
private struct CircleContentCard: View {
    let item: HomeContentUiModel

    var body: some View {
        VStack(spacing: 0) {
            RemoteArtwork(url: item.thumbnailUrl)
                .frame(width: StreamCardSize.circleIcon, height: StreamCardSize.circleIcon)
                .clipShape(Circle())
                .padding(.top, 8)

            Text(item.title)
                .font(.streamRegular(14))
                .foregroundStyle(.white)
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .frame(width: StreamCardSize.circleLabel)
                .padding(.top, 4)
        }
        .frame(width: StreamCardSize.circleLabel)
    }
}

/// `item_thumb_short.xml`; the source view-count label stays hidden in the reference layout.
private struct ShortContentCard: View {
    let item: HomeContentUiModel

    var body: some View {
        RemoteArtwork(url: item.thumbnailUrl, placeholder: .branded)
            .frame(width: StreamCardSize.short.width, height: StreamCardSize.short.height)
            .clipShape(RoundedRectangle(cornerRadius: StreamMetrics.thumbnailCorner, style: .continuous))
    }
}

/// `item_story.xml`
private struct StoryContentCard: View {
    let item: HomeContentUiModel

    var body: some View {
        RemoteArtwork(url: item.thumbnailUrl, placeholder: .branded)
            .frame(width: StreamCardSize.story.width, height: StreamCardSize.story.height)
            .clipShape(RoundedRectangle(cornerRadius: StreamMetrics.storyCorner, style: .continuous))
            .overlay(alignment: .topLeading) {
                if !item.providerAvatarUrl.isEmpty {
                    RemoteArtwork(url: item.providerAvatarUrl)
                        .clipShape(Circle())
                        .padding(1)
                        .background(Color.black, in: Circle())
                        .frame(
                            width: StreamCardSize.providerLogo,
                            height: StreamCardSize.providerLogo
                        )
                        .padding(8)
                }
            }
    }
}

/// `item_watching.xml`
private struct ContinueWatchingCard: View {
    let item: HomeContentUiModel

    @Environment(\.homeToast) private var homeToast

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            RemoteArtwork(url: item.thumbnailUrl)
                .frame(
                    width: StreamCardSize.ephemeralWide.width,
                    height: StreamCardSize.ephemeralWide.height
                )
                .clipShape(
                    RoundedRectangle(cornerRadius: StreamMetrics.thumbnailCorner, style: .continuous)
                )
                .overlay(alignment: .bottom) {
                    LinearGradient.streamWatchingShadow.frame(height: 32)
                }
                .overlay(alignment: .bottom) { progressOverlay }
                .overlay(alignment: .topLeading) {
                    if item.isDummyExclusive {
                        StreamExclusiveBadge().padding(6)
                    }
                }

            titleRow
        }
        .frame(width: StreamCardSize.ephemeralWide.width, alignment: .leading)
    }

    private var progressOverlay: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(item.subtitle)
                .font(.streamRegular(12))
                .foregroundStyle(.white)
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)

            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    Capsule().fill(Color.streamTrack)
                    Capsule()
                        .fill(Color.white)
                        .frame(width: geometry.size.width * CGFloat(item.progressPercent) / 100)
                }
            }
            .frame(height: 2)
        }
        .padding(.horizontal, 8)
        .padding(.bottom, 8)
    }

    private var titleRow: some View {
        HStack(spacing: 0) {
            Text(item.title)
                .font(.streamMedium(14))
                .foregroundStyle(.white)
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.trailing, 8)

            Button {
                homeToast(item.description_)
            } label: {
                Image("ic_dots_vertical")
                    .resizable()
                    .frame(width: 16, height: 16)
                    .frame(width: 24, height: 24)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("More options")
        }
        .frame(width: StreamCardSize.ephemeralWide.width, height: 24)
        .padding(.top, 8)
    }
}

/// `item_top_ten.xml`
private struct TopTenContentCard: View {
    let item: HomeContentUiModel
    let rank: Int

    var body: some View {
        ZStack(alignment: .topLeading) {
            RemoteArtwork(url: item.thumbnailUrl, placeholder: .branded)
                .frame(width: StreamCardSize.topTen.width, height: StreamCardSize.topTen.height)
                .clipShape(
                    RoundedRectangle(cornerRadius: StreamMetrics.thumbnailCorner, style: .continuous)
                )
                .overlay(alignment: .bottomTrailing) {
                    if item.isDummyExclusive {
                        StreamExclusiveBadge().padding(6)
                    }
                }
                .offset(x: 22, y: 8)

            Image("number_\(min(max(rank, 1), 10))")
                .resizable()
                .scaledToFit()
                .frame(width: rankWidth, height: StreamCardSize.topTenPositionHeight, alignment: .leading)
        }
        .frame(width: 166, height: 209, alignment: .topLeading)
    }

    private var rankWidth: CGFloat {
        let widths: [CGFloat] = [77.56, 74.94, 77.56, 75.81, 76.69, 76.69, 76.69, 74.94, 76.69, 77.56]
        return widths[min(max(rank - 1, 0), widths.count - 1)]
    }
}
