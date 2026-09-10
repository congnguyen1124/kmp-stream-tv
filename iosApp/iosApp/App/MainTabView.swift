import SwiftUI

enum AppDestination: Hashable {
    case home
    case music
    case shorts
    case playlist
}

struct MainTabView: View {
    @State private var selection = AppDestination.home

    var body: some View {
        TabView(selection: $selection) {
            HomeTabView()
                .tag(AppDestination.home)
                .tabItem {
                    Label("Home", systemImage: selection == .home ? "house.fill" : "house")
                }

            PlaceholderView(
                title: "Music",
                message: "The Music screen is ready for its feature implementation.",
                systemImage: "music.note"
            )
            .tag(AppDestination.music)
            .tabItem {
                Label("Music", systemImage: selection == .music ? "music.note.list" : "music.note")
            }

            PlaceholderView(
                title: "Short",
                message: "The Shorts screen is ready for its vertical feed.",
                systemImage: "play.rectangle"
            )
            .tag(AppDestination.shorts)
            .tabItem {
                Label("Short", systemImage: selection == .shorts ? "play.rectangle.fill" : "play.rectangle")
            }

            PlaceholderView(
                title: "Playlist",
                message: "The Playlist screen is ready for saved content.",
                systemImage: "text.badge.plus"
            )
            .tag(AppDestination.playlist)
            .tabItem {
                Label("Playlist", systemImage: selection == .playlist ? "list.bullet.rectangle.fill" : "list.bullet.rectangle")
            }
        }
        .tint(.white)
        .toolbarBackground(.black, for: .tabBar)
        .toolbarBackgroundVisibility(.visible, for: .tabBar)
        .preferredColorScheme(.dark)
    }
}
