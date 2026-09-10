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

struct HomeContentCard: View {
    let item: HomeContentUiModel
    let style: HomeContentCardStyle
    var rank: Int = 0

    @ViewBuilder
    var body: some View {
        switch style {
        case .landscape:
            StandardContentCard(item: item, width: 172, artworkHeight: 97)
        case .portrait:
            StandardContentCard(item: item, width: 122, artworkHeight: 190)
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

private struct StandardContentCard: View {
    let item: HomeContentUiModel
    let width: CGFloat
    let artworkHeight: CGFloat

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            RemoteArtwork(url: item.thumbnailUrl)
                .frame(width: width, height: artworkHeight)
                .overlay(alignment: .topLeading) {
                    if item.isLive {
                        StreamBadge(text: "LIVE", color: .streamLive)
                            .padding(8)
                    }
                }
                .overlay(alignment: .bottomTrailing) {
                    if item.episodeCount > 0 {
                        StreamBadge(text: "\(item.episodeCount) episodes")
                            .padding(8)
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))

            Text(item.title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.white)
                .lineLimit(2)
                .frame(width: width, alignment: .leading)
        }
    }
}

private struct CircleContentCard: View {
    let item: HomeContentUiModel

    var body: some View {
        VStack(spacing: 8) {
            RemoteArtwork(url: item.thumbnailUrl)
                .frame(width: 80, height: 80)
                .overlay {
                    Circle().stroke(.white.opacity(0.18), lineWidth: 1)
                }
                .clipShape(Circle())
                .overlay(alignment: .topLeading) {
                    StreamBadge(text: "LIVE", color: .streamLive)
                        .scaleEffect(0.82, anchor: .topLeading)
                }

            Text(item.title)
                .font(.caption.weight(.semibold))
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .frame(width: 92)
        }
        .foregroundStyle(.white)
    }
}

private struct ShortContentCard: View {
    let item: HomeContentUiModel

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            RemoteArtwork(url: item.thumbnailUrl)
            LinearGradient(colors: [.clear, .black.opacity(0.82)], startPoint: .center, endPoint: .bottom)

            VStack(alignment: .leading, spacing: 6) {
                Image(systemName: "play.circle.fill")
                    .font(.title2)
                Text(item.title)
                    .font(.subheadline.bold())
                    .lineLimit(2)
                Label(item.viewCountLabel, systemImage: "eye.fill")
                    .font(.caption2)
                    .foregroundStyle(.white.opacity(0.82))
            }
            .frame(width: 138, alignment: .leading)
            .padding(12)
        }
        .foregroundStyle(.white)
        .frame(width: 162, height: 288)
        .clipShape(RoundedRectangle(cornerRadius: StreamMetrics.cornerRadius, style: .continuous))
    }
}

private struct StoryContentCard: View {
    let item: HomeContentUiModel

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            RemoteArtwork(url: item.thumbnailUrl)
            LinearGradient(colors: [.clear, .black.opacity(0.9)], startPoint: .center, endPoint: .bottom)

            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 7) {
                    RemoteArtwork(url: item.providerAvatarUrl)
                        .frame(width: 28, height: 28)
                        .clipShape(Circle())
                    Text(item.providerName)
                        .font(.caption.bold())
                        .lineLimit(1)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                Text(item.title)
                    .font(.subheadline.bold())
                    .lineLimit(2)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .frame(width: 138, alignment: .leading)
            .padding(12)
        }
        .foregroundStyle(.white)
        .frame(width: 162, height: 288)
        .clipShape(RoundedRectangle(cornerRadius: StreamMetrics.cornerRadius, style: .continuous))
    }
}

private struct ContinueWatchingCard: View {
    let item: HomeContentUiModel

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            ZStack(alignment: .bottom) {
                RemoteArtwork(url: item.thumbnailUrl)
                ProgressView(value: Double(item.progressPercent), total: 100)
                    .tint(.streamAccentBright)
                    .background(.white.opacity(0.25))
            }
            .frame(width: 220, height: 124)
            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))

            Text(item.title)
                .font(.subheadline.bold())
                .lineLimit(1)
            Text(item.subtitle)
                .font(.caption)
                .foregroundStyle(Color.streamSecondaryText)
                .lineLimit(1)
        }
        .frame(width: 220, alignment: .leading)
        .foregroundStyle(.white)
    }
}

private struct TopTenContentCard: View {
    let item: HomeContentUiModel
    let rank: Int

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            RemoteArtwork(url: item.thumbnailUrl)
            LinearGradient(colors: [.clear, .black.opacity(0.68)], startPoint: .center, endPoint: .bottom)
            Text("\(rank)")
                .font(.system(size: 58, weight: .black, design: .rounded))
                .foregroundStyle(.white)
                .shadow(color: .black, radius: 5, x: 2, y: 2)
                .padding(8)
        }
        .frame(width: 134, height: 201)
        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
    }
}
