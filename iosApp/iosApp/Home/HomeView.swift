import SwiftUI
import Shared

struct HomeView: View {
    @StateObject private var store = HomeStore()
    @State private var selection: PlayerSelection?

    var body: some View {
        NavigationStack {
            ZStack {
                Color.streamBackground.ignoresSafeArea()
                content
            }
            .toolbarBackground(Color.streamBackground, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Image("StreamTvLogo")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 128, height: 32, alignment: .leading)
                        .accessibilityLabel("StreamTV")
                }
            }
        }
        .sheet(item: $selection) { selection in
            PlayerView(content: selection.content)
        }
        .preferredColorScheme(.dark)
    }

    @ViewBuilder
    private var content: some View {
        if store.state.isLoading {
            ProgressView("Loading your StreamTV home…")
                .tint(.orange)
                .foregroundStyle(.white)
        } else if let message = store.state.errorMessage {
            VStack(spacing: 16) {
                Text(message)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.white)
                Button("Try again", action: store.retry)
                    .buttonStyle(.borderedProminent)
                    .tint(.orange)
            }
            .padding(24)
        } else {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 30) {
                    ForEach(store.state.sections, id: \.id) { section in
                        HomeSectionView(section: section) { content in
                            selection = PlayerSelection(content: content)
                        }
                    }
                }
                .padding(.bottom, 36)
            }
        }
    }
}

private struct HomeSectionView: View {
    let section: HomeSectionUiModel
    let onSelect: (HomeContentUiModel) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if !section.isBanner {
                Text(section.title)
                    .font(.title2.bold())
                    .padding(.horizontal, 20)
            }

            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: 14) {
                    ForEach(Array(section.items.enumerated()), id: \.element.id) { index, item in
                        Button { onSelect(item) } label: {
                            if section.isBanner {
                                BannerCard(item: item)
                            } else {
                                ContentCard(
                                    item: item,
                                    isPortrait: section.usesPortraitCards,
                                    rank: section.showsRanking ? index + 1 : nil
                                )
                            }
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(item.title)
                    }
                }
                .padding(.horizontal, 20)
            }
        }
    }
}

private struct BannerCard: View {
    let item: HomeContentUiModel

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            RemoteArtwork(url: item.thumbnailUrl)
            LinearGradient(
                colors: [.black.opacity(0.85), .black.opacity(0.15), .clear],
                startPoint: .leading,
                endPoint: .trailing
            )
            VStack(alignment: .leading, spacing: 10) {
                if let age = item.ageRestriction {
                    Text(age).font(.caption.bold()).badgeStyle()
                }
                Text(item.title)
                    .font(.largeTitle.bold())
                    .lineLimit(2)
                Text(item.description)
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.78))
                    .lineLimit(2)
                Label("Watch now", systemImage: "play.fill")
                    .font(.headline)
                    .badgeStyle()
            }
            .frame(maxWidth: 390, alignment: .leading)
            .padding(24)
        }
        .frame(width: min(UIScreen.main.bounds.width - 40, 720), height: 360)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

private struct ContentCard: View {
    let item: HomeContentUiModel
    let isPortrait: Bool
    let rank: Int?

    var body: some View {
        VStack(alignment: .leading, spacing: 9) {
            ZStack(alignment: .bottomLeading) {
                RemoteArtwork(url: item.thumbnailUrl)
                if let rank {
                    Text("\(rank)")
                        .font(.system(size: 46, weight: .black))
                        .shadow(color: .black, radius: 5, x: 2, y: 2)
                        .padding(8)
                }
                badge
            }
            .frame(width: isPortrait ? 154 : 250, height: isPortrait ? 231 : 141)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

            Text(item.title)
                .font(.subheadline.bold())
                .lineLimit(1)
                .frame(width: isPortrait ? 154 : 250, alignment: .leading)
        }
    }

    @ViewBuilder
    private var badge: some View {
        if item.isLive {
            Text("LIVE")
                .font(.caption2.bold())
                .padding(7)
                .background(.red, in: RoundedRectangle(cornerRadius: 5))
                .padding(8)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        } else if item.episodeCount > 0 {
            Text("\(item.episodeCount) episodes")
                .font(.caption2.bold())
                .badgeStyle()
                .padding(8)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomTrailing)
        }
    }
}

private struct RemoteArtwork: View {
    let url: String

    var body: some View {
        AsyncImage(url: URL(string: url)) { phase in
            switch phase {
            case .success(let image):
                image.resizable().scaledToFill()
            case .failure:
                Color.gray.opacity(0.25).overlay(Image(systemName: "photo"))
            default:
                Color.gray.opacity(0.18).overlay(ProgressView())
            }
        }
        .clipped()
    }
}

private struct PlayerSelection: Identifiable {
    let content: HomeContentUiModel
    var id: String { content.id }
}

private extension View {
    func badgeStyle() -> some View {
        padding(.horizontal, 10)
            .padding(.vertical, 6)
            .foregroundStyle(.white)
            .background(.black.opacity(0.64), in: RoundedRectangle(cornerRadius: 6))
    }
}

private extension Color {
    static let streamBackground = Color(red: 0.035, green: 0.043, blue: 0.063)
}
