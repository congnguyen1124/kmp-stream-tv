import Shared
import SwiftUI

struct HomeView: View {
    @ObservedObject var store: HomeStore
    let contentTopInset: CGFloat
    let onScrollOffsetChanged: (CGFloat) -> Void

    @State private var playerSelection: PlayerSelection?
    @State private var positionedInitialFeed = false

    var body: some View {
        ZStack {
            Color.streamBackground.ignoresSafeArea()
            content
        }
        .fullScreenCover(item: $playerSelection) { selection in
            PlayerView(content: selection.content)
        }
    }

    @ViewBuilder
    private var content: some View {
        if store.state.sections.isEmpty && store.state.isLoading {
            ProgressView("Loading your StreamTV home…")
                .tint(.streamAccentBright)
                .foregroundStyle(.white)
                .padding(.top, contentTopInset)
        } else if let message = store.state.errorMessage {
            VStack(spacing: 16) {
                Text(message)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.white)
                Button("Try again", action: store.retry)
                    .buttonStyle(.borderedProminent)
                    .tint(.streamAccent)
            }
            .padding(24)
            .padding(.top, contentTopInset)
        } else {
            feed
        }
    }

    private var feed: some View {
        ScrollViewReader { proxy in
            ScrollView {
                GeometryReader { geometry in
                    Color.clear.preference(
                        key: HomeFeedOffsetPreferenceKey.self,
                        value: -geometry.frame(in: .named("home-feed")).minY
                    )
                }
                .frame(height: 0)
                .id(Self.feedTopID)

                LazyVStack(alignment: .leading, spacing: StreamMetrics.sectionSpacing) {
                    ForEach(store.state.sections, id: \.id) { section in
                        HomeSectionView(section: section) { content in
                            playerSelection = PlayerSelection(content: content)
                        }
                    }
                }
                .padding(.top, contentTopInset)
                .padding(.bottom, 90)
            }
            .coordinateSpace(name: "home-feed")
            .onPreferenceChange(HomeFeedOffsetPreferenceKey.self, perform: onScrollOffsetChanged)
            .refreshable {
                await store.refresh()
            }
            .onAppear {
                guard !positionedInitialFeed else { return }
                positionedInitialFeed = true
                proxy.scrollTo(Self.feedTopID, anchor: .top)
            }
        }
    }

    private static let feedTopID = "home-feed-top"
}

private struct PlayerSelection: Identifiable {
    let content: HomeContentUiModel
    var id: String { content.id }
}

private struct HomeFeedOffsetPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = 0

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}
