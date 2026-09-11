import SwiftUI

/// Composes the three layers the overlay is made of, the way `fragment_player.xml` does:
/// the controller-wrapped video surface, the mini transport footer, and the detail list.
struct PlayerOverlayView: View {
    @ObservedObject var store: PlayerOverlayStore

    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        Group {
            if let media = store.media {
                content(media)
            }
        }
        .onChange(of: scenePhase) { _, phase in store.onScenePhaseChanged(phase) }
        .onChange(of: store.player.state.isPlaying) { _, _ in store.onPlaybackChanged() }
    }

    private func content(_ media: PlayerMedia) -> some View {
        GeometryReader { geometry in
            let isLandscape = geometry.size.width > geometry.size.height

            ZStack {
                MinimizablePlayerContainer(
                    state: store.minimizable,
                    presentation: store.presentation,
                    isSystemPictureInPicture: store.isSystemPictureInPicture,
                    appBottomBarHeight: BottomNavigationMetrics.totalHeight,
                    onMinimizedChanged: store.onMinimizedChanged
                ) {
                    PlayerControllerView(
                        media: media,
                        player: store.player,
                        coordinator: store.pictureInPicture,
                        presentation: store.presentation,
                        isSystemPictureInPicture: store.isSystemPictureInPicture,
                        isLandscape: isLandscape,
                        onClose: store.close,
                        onMinimize: store.minimize,
                        onPictureInPicture: store.enterPictureInPicture,
                        onFullscreenToggle: store.toggleFullscreen,
                        onEpisodes: store.showEpisodesComingSoon,
                        settingTypes: $store.settingTypes
                    )
                } footer: {
                    MiniPlaybackControllerView(
                        state: store.player.state,
                        zoomScale: store.minimizable.zoomScale,
                        isEnabled: store.player.state.playbackError == nil,
                        onToggle: { store.player.togglePlayPause() },
                        onReplay: { store.player.replay() },
                        onRewind: { store.player.seekBack() },
                        onForward: { store.player.seekForward() }
                    )
                } detail: {
                    PlayerDetailList(
                        media: media,
                        recommendations: store.recommendations,
                        onAction: store.handle,
                        onRecommendation: store.play
                    )
                }

                if let toast = store.toast {
                    Text(toast)
                        .font(.streamRegular(14))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 16)
                        .frame(minHeight: 48)
                        .background(Color.black.opacity(0.88))
                        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                        .padding(.horizontal, 24)
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
                        .padding(.bottom, 120)
                        .transition(.opacity)
                        .allowsHitTesting(false)
                }
            }
            .onChange(of: isLandscape) { _, _ in store.onOrientationChanged() }
        }
        // The overlay stays inside the safe area at every presentation. Android hides the system
        // bars and runs edge to edge in landscape, but an iPhone's landscape inset is the Dynamic
        // Island on the leading edge: bleeding under it would put the title and close button
        // behind the cut-out, and the 16:9 video is pillarboxed on a 19.5:9 screen either way, so
        // filling those strips would gain nothing.
    }
}
