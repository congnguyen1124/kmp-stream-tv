import SwiftUI

enum AppDestination: String, CaseIterable, Identifiable {
    case home
    case music
    case shorts
    case playlist

    var id: String { rawValue }

    var title: String {
        switch self {
        case .home: "Home"
        case .music: "Music"
        case .shorts: "Short"
        case .playlist: "Playlist"
        }
    }

    var selectedIcon: String {
        switch self {
        case .home: "ic_home_fill"
        case .music: "ic_tv_fill"
        case .shorts: "ic_short_fill"
        case .playlist: "ic_playlist_fill"
        }
    }

    var unselectedIcon: String {
        switch self {
        case .home: "ic_home_outline"
        case .music: "ic_tv_outline"
        case .shorts: "ic_short_outline"
        case .playlist: "ic_playlist_outline"
        }
    }
}

struct MainTabView: View {
    @State private var selection = AppDestination.home

    var body: some View {
        ZStack(alignment: .bottom) {
            Color.streamBackground.ignoresSafeArea()

            ZStack {
                destination(.home) { HomeTabView() }
                destination(.music) {
                    PlaceholderView(
                        title: "Music",
                        message: "The Music fragment shell is ready for its feature implementation."
                    )
                }
                destination(.shorts) {
                    PlaceholderView(
                        title: "Short",
                        message: "The Shorts fragment shell is ready for its vertical feed."
                    )
                }
                destination(.playlist) {
                    PlaceholderView(
                        title: "Playlist",
                        message: "The Playlist fragment shell is ready for saved content."
                    )
                }
            }
            .safeAreaPadding(.bottom, BottomNavigationMetrics.totalHeight)

            BottomNavigationBar(selection: $selection)
        }
        .preferredColorScheme(.dark)
    }

    private func destination<Content: View>(
        _ destination: AppDestination,
        @ViewBuilder content: () -> Content
    ) -> some View {
        content()
            .opacity(selection == destination ? 1 : 0)
            .allowsHitTesting(selection == destination)
            .accessibilityHidden(selection != destination)
    }
}
