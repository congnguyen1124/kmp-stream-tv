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
            ThumbnailContentCard(item: item, width: 172, height: 97)
        case .portrait:
            ThumbnailContentCard(item: item, width: 106, height: 185)
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

private struct ThumbnailContentCard: View {
    let item: HomeContentUiModel
    let width: CGFloat
    let height: CGFloat

    var body: some View {
        RemoteArtwork(url: item.thumbnailUrl)
            .frame(width: width, height: height)
            .background(Color.streamSurface)
            .clipShape(RoundedRectangle(cornerRadius: StreamMetrics.thumbnailCorner, style: .continuous))
            .overlay(alignment: .bottomLeading) {
                if item.isLive {
                    StreamBadge(text: "LIVE", color: .streamLive)
                        .padding(4)
                }
            }
    }
}

private struct CircleContentCard: View {
    let item: HomeContentUiModel

    var body: some View {
        VStack(spacing: 0) {
            RemoteArtwork(url: item.thumbnailUrl)
                .frame(width: 64, height: 64)
                .background(Color.streamSurface)
                .clipShape(Circle())
                .padding(.top, 8)

            Text(item.title)
                .font(.streamRegular(12))
                .foregroundStyle(.white)
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .frame(width: 80)
                .padding(.top, 4)
        }
        .frame(width: 80)
    }
}

private struct ShortContentCard: View {
    let item: HomeContentUiModel

    var body: some View {
        RemoteArtwork(url: item.thumbnailUrl)
            .frame(width: 162, height: 288)
            .background(Color.streamSurface)
            .clipShape(RoundedRectangle(cornerRadius: StreamMetrics.thumbnailCorner, style: .continuous))
    }
}

private struct StoryContentCard: View {
    let item: HomeContentUiModel

    var body: some View {
        RemoteArtwork(url: item.thumbnailUrl)
            .frame(width: 162, height: 288)
            .background(Color.streamSurface)
            .clipShape(RoundedRectangle(cornerRadius: StreamMetrics.storyCorner, style: .continuous))
            .overlay(alignment: .topLeading) {
                RemoteArtwork(url: item.providerAvatarUrl)
                    .frame(width: 30, height: 30)
                    .clipShape(Circle())
                    .padding(1)
                    .background(Color.black, in: Circle())
                    .padding(8)
            }
    }
}

private struct ContinueWatchingCard: View {
    let item: HomeContentUiModel

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            RemoteArtwork(url: item.thumbnailUrl)
                .frame(width: 172, height: 97)
                .background(Color.streamSurface)
                .clipShape(RoundedRectangle(cornerRadius: StreamMetrics.thumbnailCorner, style: .continuous))
                .overlay(alignment: .bottom) {
                    LinearGradient(
                        colors: [.clear, .streamBackground],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .frame(height: 32)
                }
                .overlay(alignment: .bottom) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(item.subtitle)
                            .font(.streamRegular(12))
                            .foregroundStyle(.white)
                            .lineLimit(1)
                            .frame(maxWidth: .infinity, alignment: .leading)

                        GeometryReader { geometry in
                            ZStack(alignment: .leading) {
                                Capsule().fill(Color.white.opacity(0.4))
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

            HStack(spacing: 0) {
                Text(item.title)
                    .font(.streamMedium(14))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Image("ic_dots_vertical")
                    .resizable()
                    .frame(width: 16, height: 16)
                    .frame(width: 24, height: 24)
            }
            .frame(width: 172, height: 24)
            .padding(.top, 8)
        }
        .frame(width: 172, alignment: .leading)
    }
}

private struct TopTenContentCard: View {
    let item: HomeContentUiModel
    let rank: Int

    var body: some View {
        ZStack(alignment: .topLeading) {
            RemoteArtwork(url: item.thumbnailUrl)
                .frame(width: 134, height: 201)
                .background(Color.streamSurface)
                .clipShape(RoundedRectangle(cornerRadius: StreamMetrics.thumbnailCorner, style: .continuous))
                .offset(x: 22, y: 8)

            Image("number_\(min(max(rank, 1), 10))")
                .resizable()
                .scaledToFit()
                .frame(width: rankWidth, height: 61, alignment: .leading)
        }
        .frame(width: 166, height: 209, alignment: .topLeading)
    }

    private var rankWidth: CGFloat {
        let widths: [CGFloat] = [77.56, 74.94, 77.56, 75.81, 76.69, 76.69, 76.69, 74.94, 76.69, 77.56]
        return widths[min(max(rank - 1, 0), widths.count - 1)]
    }
}
