import Shared
import SwiftUI

struct HomeView: View {
    @ObservedObject var store: HomeStore
    let contentTopInset: CGFloat
    let onScrollOffsetChanged: (CGFloat) -> Void

    @EnvironmentObject private var playerStore: PlayerOverlayStore
    @State private var positionedInitialFeed = false

    var body: some View {
        ZStack {
            Color.streamBackground.ignoresSafeArea()
            content
        }
    }

    @ViewBuilder
    private var content: some View {
        if store.state.sections.isEmpty && store.state.isLoading {
            ProgressView()
                .tint(.streamAccentBright)
                .frame(width: 44, height: 44)
                .accessibilityLabel("Loading Home content")
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

                LazyVStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(store.state.sections.enumerated()), id: \.element.id) { index, section in
                        HomeSectionView(section: section, index: index) { content in
                            playerStore.open(content: content)
                        }
                    }
                }
                .padding(.top, contentTopInset)
                .padding(.bottom, StreamMetrics.homeContentPaddingBottom)
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

private struct HomeFeedOffsetPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = 0

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}
