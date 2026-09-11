import Shared
import SwiftUI

/// VOD detail/player overlay state, ported from `PlayerFragment.kt`.
///
/// The overlay itself is [MinimizablePlayerContainer]: portrait shrinks the player into a floating
/// card that can be dragged, pinched and parked in any corner, landscape becomes a controller-only
/// fullscreen player, and system Picture in Picture bypasses both. This object owns lifecycle,
/// media and the presentation the overlay is asked for; every dimension of the shrink lives in
/// [MinimizableState].
@MainActor
final class PlayerOverlayStore: ObservableObject {
    @Published private(set) var media: PlayerMedia?
    @Published private(set) var recommendations: [PlayerMedia] = []
    @Published private(set) var presentation: PlayerPresentation = .detail
    @Published private(set) var isSystemPictureInPicture = false
    @Published var settingTypes: [PlayerSettingType] = []
    @Published private(set) var toast: String?

    let player = StreamPlayer()
    let minimizable = MinimizableState()
    let pictureInPicture = PictureInPictureCoordinator()

    var isPresented: Bool { media != nil }

    /// The bottom navigation is only visible behind a minimized player, matching
    /// `MainActivity.presentPlayer`.
    var showsBottomNavigation: Bool { !isPresented || presentation == .mini }

    private var loadedMediaKey: String?
    private var resumeWhenActive = false
    private var toastTask: Task<Void, Never>?

    init() {
        pictureInPicture.onChange = { [weak self] enabled in
            self?.onSystemPictureInPictureChanged(enabled)
        }
    }

    // MARK: - Opening and closing

    /// `MainActivity.openPlayer`: an overlay that is already up swaps media and expands.
    func open(content: HomeContentUiModel) {
        play(PlayerMedia(content: content))
    }

    func play(_ media: PlayerMedia) {
        self.media = media
        recommendations = PlayerDemoCatalog.recommendations(for: media)
        startMedia(media)
        expand()
    }

    private func startMedia(_ media: PlayerMedia) {
        guard loadedMediaKey != media.mediaKey else { return }
        loadedMediaKey = media.mediaKey
        guard let url = URL(string: media.url) else { return }
        player.load(url: url)
    }

    func close() {
        settingTypes = []
        pictureInPicture.stop()
        pictureInPicture.setAutomaticStart(false)
        player.stop()
        media = nil
        recommendations = []
        loadedMediaKey = nil
        isSystemPictureInPicture = false
        presentation = .detail
        minimizable.restore(minimized: false)
        UIApplication.shared.isIdleTimerDisabled = false
        OrientationController.release()
    }

    // MARK: - Presentation

    /// `PlayerFragment.expand`. The grow animation reports back through
    /// [onMinimizedChanged], which is what switches the presentation — driving both from here
    /// would flip it a frame early.
    func expand() {
        if minimizable.isMinimized {
            minimizable.toggleMinimized()
            onMinimizedChanged(minimizable.isMinimized)
            return
        }
        setPresentation(presentationForCurrentOrientation(fallback: .detail))
    }

    func minimize() {
        requestPortrait()
        if !minimizable.isMinimized {
            minimizable.toggleMinimized()
            onMinimizedChanged(minimizable.isMinimized)
            return
        }
        setPresentation(.mini)
    }

    /// The single seam the overlay reports its own shrink/grow through.
    func onMinimizedChanged(_ isMinimized: Bool) {
        setPresentation(
            isMinimized ? .mini : presentationForCurrentOrientation(fallback: .detail)
        )
    }

    func setPresentation(_ value: PlayerPresentation) {
        presentation = value
        minimizable.restore(minimized: value == .mini)
        // A minimized player is the one presentation that must never slide into system PiP behind
        // the user's back — `MainActivity.presentPlayer` disables auto-enter for exactly that case.
        pictureInPicture.setAutomaticStart(value != .mini && player.state.isPlaying)
    }

    /// `PlayerFragment.onHostConfigurationChanged`.
    func onOrientationChanged() {
        if presentation != .mini, !isSystemPictureInPicture {
            setPresentation(presentationForCurrentOrientation(fallback: .detail))
        }
    }

    private func presentationForCurrentOrientation(
        fallback: PlayerPresentation
    ) -> PlayerPresentation {
        if fallback == .mini { return .mini }
        return OrientationController.isLandscape ? .fullscreen : .detail
    }

    func toggleFullscreen() {
        if OrientationController.isLandscape {
            OrientationController.requestPortrait()
        } else {
            OrientationController.requestLandscape()
        }
    }

    private func requestPortrait() {
        guard OrientationController.isLandscape else { return }
        OrientationController.requestPortrait()
    }

    // MARK: - Picture in Picture

    func enterPictureInPicture() {
        guard media != nil, presentation != .mini, !isSystemPictureInPicture else { return }
        pictureInPicture.start()
    }

    private func onSystemPictureInPictureChanged(_ enabled: Bool) {
        isSystemPictureInPicture = enabled
        if enabled {
            settingTypes = []
        } else {
            setPresentation(presentationForCurrentOrientation(fallback: presentation))
        }
    }

    // MARK: - Lifecycle

    /// `onStop` pauses unless system PiP owns playback, and `onStart` resumes what it paused.
    func onScenePhaseChanged(_ phase: ScenePhase) {
        switch phase {
        case .active:
            if resumeWhenActive {
                resumeWhenActive = false
                player.play()
            }
        case .background, .inactive:
            guard !isSystemPictureInPicture, isPresented else { return }
            resumeWhenActive = player.state.isPlaying
            player.pause()
        @unknown default:
            break
        }
    }

    /// `keepScreenOn` on the Android media surface.
    func onPlaybackChanged() {
        UIApplication.shared.isIdleTimerDisabled = isPresented && player.state.isPlaying
        pictureInPicture.setAutomaticStart(presentation != .mini && player.state.isPlaying)
    }

    // MARK: - Detail actions

    func handle(_ action: PlayerDetailAction) {
        showToast(action.message)
    }

    func showEpisodesComingSoon() {
        showToast("The episode list will be added as a separate screen.")
    }

    private func showToast(_ message: String) {
        toastTask?.cancel()
        withAnimation { toast = message }
        toastTask = Task {
            try? await Task.sleep(for: .seconds(2))
            guard !Task.isCancelled else { return }
            withAnimation { toast = nil }
        }
    }
}
