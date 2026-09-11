import AVFoundation
import SwiftUI

/// App-owned player UI, ported from `PlayerView.kt`.
///
/// Like the Android widget it owns no playback state: every command goes to [StreamPlayer] and
/// every value it paints comes from the immutable snapshot. What it does own is controller
/// visibility, the settings sheet and which chrome each presentation shows.
struct PlayerControllerView: View {
    let media: PlayerMedia
    @ObservedObject var player: StreamPlayer
    let coordinator: PictureInPictureCoordinator
    let presentation: PlayerPresentation
    let isSystemPictureInPicture: Bool
    let isLandscape: Bool

    let onClose: () -> Void
    let onMinimize: () -> Void
    let onPictureInPicture: () -> Void
    let onFullscreenToggle: () -> Void
    let onEpisodes: () -> Void

    @Binding var settingTypes: [PlayerSettingType]

    @State private var controlsVisible = true
    @State private var hideTask: Task<Void, Never>?
    @State private var isScrubbing = false
    @State private var scrubPosition: TimeInterval = 0
    @State private var resumeAfterSettings = false

    private var state: StreamPlayerState { player.state }
    private var isMini: Bool { presentation == .mini }
    private var isEnded: Bool { state.playbackState == .ended }
    private var chromeHidden: Bool { isMini || isSystemPictureInPicture }
    private var showsSettings: Bool { !settingTypes.isEmpty }

    var body: some View {
        ZStack {
            surface

            if !chromeHidden {
                topController
                    .frame(maxHeight: .infinity, alignment: .top)
                centerController
                bottomController
                    .frame(maxHeight: .infinity, alignment: .bottom)
            }

            if state.playbackError != nil, !chromeHidden {
                errorView
            }

            if isMini, !isSystemPictureInPicture {
                miniCloseOverlay
            }

            if showsSettings, !chromeHidden {
                PlayerSettingsSheet(
                    types: settingTypes,
                    state: state,
                    speed: player.speed,
                    onSelect: applySetting,
                    onDismiss: dismissSettings
                )
            }
        }
        .background(Color.black)
        .onChange(of: state.isPlaying) { _, _ in scheduleControllerHide() }
        .onChange(of: presentation) { _, _ in applyPresentation() }
        .onAppear { applyPresentation() }
        .onDisappear { hideTask?.cancel() }
    }

    // MARK: - Surface

    private var surface: some View {
        ZStack {
            Color.black

            PlayerSurfaceView(player: player.player, coordinator: coordinator)

            // `mediaArtwork` covers the surface until the first frame is decoded.
            if state.playbackState == .idle || state.playbackState == .buffering {
                RemoteArtwork(url: media.thumbnailUrl)
                    .allowsHitTesting(false)
            }

            // `viewMask`: tap toggles the controller, and is inert on a minimized card so the
            // pan/pinch/tap of the floating player reaches `MinimizablePlayerContainer`.
            if !chromeHidden {
                Color.clear
                    .contentShape(Rectangle())
                    .onTapGesture { toggleController() }
            }

            if state.playbackError == nil,
               state.playbackState == .idle || state.playbackState == .buffering {
                ProgressView()
                    .tint(.streamAccentBright)
                    .frame(width: 48, height: 48)
                    .allowsHitTesting(false)
            }
        }
    }

    // MARK: - Top controller

    private var topController: some View {
        HStack(spacing: 0) {
            Text(media.title)
                .font(.streamMedium(14))
                .foregroundStyle(.white)
                .lineLimit(1)
                .frame(minHeight: PlayerMetrics.titleMinHeight, alignment: .leading)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 8)
                // `playerTitle.isInvisible = !isLandscape`: it keeps its space in portrait.
                .opacity(isLandscape ? 1 : 0)

            if presentation == .detail {
                iconButton("ic_player_collapse", label: "Minimize player", action: onMinimize)
            }
            if coordinator.isSupported {
                iconButton("ic_player_minimize", label: "Picture in picture", action: onPictureInPicture)
            }
            iconButton("ic_player_close", label: "Close player", action: onClose)
        }
        .padding(PlayerMetrics.controllerPaddingMedium)
        .background(
            LinearGradient(
                colors: [.black.opacity(0.5), .clear],
                startPoint: .top,
                endPoint: .bottom
            )
        )
        .opacity(controlsVisible ? 1 : 0)
        .allowsHitTesting(controlsVisible)
        .animation(.easeInOut(duration: PlayerMetrics.transitionDuration), value: controlsVisible)
    }

    // MARK: - Center controller

    private var centerController: some View {
        HStack(spacing: PlayerMetrics.centerActionSpacing) {
            if !media.isLive, !isEnded {
                seekButton("ic_player_rewind", caption: "10", label: "Rewind") {
                    player.seekBack()
                    revealController()
                }
            }

            Button {
                togglePlayback()
            } label: {
                Image(playPauseIcon)
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .frame(width: PlayerMetrics.centerButtonSize, height: PlayerMetrics.centerButtonSize)
                    .foregroundStyle(.white)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(isEnded ? "Replay" : (state.isPlaying ? "Pause" : "Play"))

            if !media.isLive, !isEnded {
                seekButton("ic_player_forward", caption: "10", label: "Forward") {
                    player.seekForward()
                    revealController()
                }
            }
        }
        .opacity(controlsVisible ? 1 : 0)
        .allowsHitTesting(controlsVisible)
        .animation(.easeInOut(duration: PlayerMetrics.transitionDuration), value: controlsVisible)
    }

    private var playPauseIcon: String {
        if isEnded { return "ic_player_replay" }
        return state.isPlaying ? "ic_player_pause" : "ic_player_play"
    }

    // MARK: - Bottom controller

    private var bottomController: some View {
        VStack(spacing: 0) {
            progressRow

            if !media.isLive {
                PlayerSeekBar(
                    position: isScrubbing ? scrubPosition : state.position,
                    buffered: state.bufferedPosition,
                    duration: state.duration,
                    onScrubChanged: { value in
                        isScrubbing = true
                        scrubPosition = value
                        hideTask?.cancel()
                    },
                    onScrubEnded: { value in
                        isScrubbing = false
                        player.seek(to: value)
                        revealController()
                    }
                )
                .padding(.top, PlayerMetrics.bottomControllerPadding)
                .padding(.horizontal, PlayerMetrics.controllerPadding)
            }

            // `bottomActions` is landscape-only and never shown for a live stream.
            if isLandscape, !media.isLive {
                bottomActions
            }
        }
        .padding(.bottom, PlayerMetrics.controllerPaddingVertical)
        .background(
            LinearGradient(
                colors: [.clear, .black.opacity(0.5)],
                startPoint: .top,
                endPoint: .bottom
            )
        )
        .opacity(controlsVisible ? 1 : 0)
        .allowsHitTesting(controlsVisible)
        .animation(.easeInOut(duration: PlayerMetrics.transitionDuration), value: controlsVisible)
    }

    private var progressRow: some View {
        HStack(spacing: 0) {
            if media.isLive {
                Text("LIVE")
                    .font(.streamBold(10))
                    .foregroundStyle(.black)
                    .padding(.horizontal, PlayerMetrics.livePaddingHorizontal)
                    .padding(.vertical, 3)
                    .background(
                        Color.white,
                        in: RoundedRectangle(cornerRadius: PlayerMetrics.liveCorner, style: .continuous)
                    )
            } else {
                Text(elapsedLabel)
                    .font(.streamRegular(PlayerMetrics.durationTextSize))
                    .foregroundStyle(.white)
                    .monospacedDigit()
            }

            Spacer(minLength: 0)

            if !media.isLive, state.videoTracks.count > 1 {
                actionIconButton("ic_player_video_settings", label: "Video quality") {
                    showSettings([.video])
                }
            }

            actionIconButton(player.isMuted ? "ic_player_audio_off" : "ic_player_audio_on",
                             label: player.isMuted ? "Unmute" : "Mute") {
                player.toggleMuted()
                revealController()
            }

            actionIconButton(isLandscape ? "ic_player_exit_fullscreen" : "ic_player_fullscreen",
                             label: isLandscape ? "Exit fullscreen" : "Fullscreen",
                             action: onFullscreenToggle)
        }
        .padding(.horizontal, PlayerMetrics.controllerPadding)
        .padding(.vertical, PlayerMetrics.controllerPaddingVertical)
    }

    private var bottomActions: some View {
        HStack(spacing: 16) {
            textAction("ic_speed", title: "Speed \(player.speed.speedLabel)") {
                showSettings([.speed])
            }

            if state.audioTracks.count > 1 || !state.textTracks.isEmpty {
                textAction("ic_sound_and_subtitle", title: "Sound & subtitle") {
                    var types: [PlayerSettingType] = []
                    if state.audioTracks.count > 1 { types.append(.audio) }
                    if !state.textTracks.isEmpty { types.append(.subtitle) }
                    showSettings(types)
                }
            }

            if media.episodeCount > 1 {
                textAction("ic_playlist_outline", title: "Episodes", action: onEpisodes)
            }
        }
        .frame(height: PlayerMetrics.bottomActionMenuHeight)
        .padding(.horizontal, PlayerMetrics.controllerPadding)
        .padding(.top, 12)
    }

    // MARK: - Error and mini chrome

    private var errorView: some View {
        ZStack(alignment: .topTrailing) {
            Color.black

            VStack(spacing: 0) {
                Image("ic_player_error")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 40, height: 40)

                Text(state.playbackError?.message ?? "")
                    .font(.streamRegular(14))
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.center)
                    .padding(.top, 16)

                if state.playbackError?.isRetryable == true {
                    Button("Try again") { player.retry() }
                        .font(.streamSemiBold(16))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 24)
                        .frame(height: 44)
                        .background(
                            LinearGradient.streamPrimaryButton,
                            in: RoundedRectangle(cornerRadius: 8, style: .continuous)
                        )
                        .padding(.top, 20)
                }
            }
            .padding(32)
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            iconButton("ic_player_close", label: "Close player", action: onClose)
                .padding(8)
        }
    }

    private var miniCloseOverlay: some View {
        Button(action: onClose) {
            Image("ic_player_close")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .padding(PlayerMetrics.actionPadding)
                .frame(width: PlayerMetrics.miniCloseSize, height: PlayerMetrics.miniCloseSize)
                .foregroundStyle(.white)
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Close player")
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
    }

    // MARK: - Buttons

    private func iconButton(_ name: String, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(name)
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .padding(12)
                .frame(width: 48, height: 48)
                .foregroundStyle(.white)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }

    private func actionIconButton(
        _ name: String,
        label: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(name)
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .padding(PlayerMetrics.actionPadding)
                .frame(width: 40, height: 40)
                .foregroundStyle(.white)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }

    /// The seek buttons draw their step count inside the arrow artwork, as the Android
    /// `TextView` with a rewind/forward background does.
    private func seekButton(
        _ name: String,
        caption: String,
        label: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            ZStack {
                Image(name)
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .foregroundStyle(.white)

                Text(caption)
                    .font(.streamRegular(12))
                    .foregroundStyle(.white)
                    .padding(.top, 4)
            }
            .frame(width: PlayerMetrics.centerButtonSize, height: PlayerMetrics.centerButtonSize)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }

    private func textAction(_ icon: String, title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(icon)
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .frame(
                        width: PlayerMetrics.buttonIconSize,
                        height: PlayerMetrics.buttonIconSize
                    )
                Text(title)
                    .font(.streamSemiBold(12))
            }
            .foregroundStyle(.white)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    // MARK: - Behaviour

    private func togglePlayback() {
        if isEnded {
            player.replay()
        } else if media.isLive {
            player.togglePlaybackAtDefaultPosition()
        } else {
            player.togglePlayback()
        }
        revealController()
    }

    private func showSettings(_ types: [PlayerSettingType]) {
        guard !types.isEmpty else { return }
        resumeAfterSettings = state.isPlaying
        if resumeAfterSettings { player.pause() }
        hideTask?.cancel()
        withAnimation(.easeInOut(duration: PlayerMetrics.transitionDuration)) {
            settingTypes = types
        }
    }

    private func applySetting(_ type: PlayerSettingType, _ value: String) {
        switch type {
        case .speed: player.setSpeed(Float(value) ?? 1)
        case .video: player.selectVideoTrack(id: value)
        case .audio: player.selectAudioTrack(id: value)
        case .subtitle: player.selectTextTrack(id: value)
        }
    }

    private func dismissSettings() {
        withAnimation(.easeInOut(duration: PlayerMetrics.transitionDuration)) {
            settingTypes = []
        }
        if resumeAfterSettings { player.play() }
        resumeAfterSettings = false
        revealController()
    }

    private func applyPresentation() {
        hideTask?.cancel()
        if chromeHidden {
            controlsVisible = false
        } else {
            revealController()
        }
    }

    private func toggleController() {
        controlsVisible.toggle()
        if controlsVisible { scheduleControllerHide() }
    }

    private func revealController() {
        guard !chromeHidden else { return }
        controlsVisible = true
        scheduleControllerHide()
    }

    private func scheduleControllerHide() {
        hideTask?.cancel()
        guard state.isPlaying, !showsSettings, state.playbackError == nil, !chromeHidden else { return }
        hideTask = Task {
            try? await Task.sleep(for: .seconds(PlayerMetrics.controllerDisplaySeconds))
            guard !Task.isCancelled else { return }
            controlsVisible = false
        }
    }

    private var elapsedLabel: String {
        let position = isScrubbing ? scrubPosition : state.position
        return "\(position.playerTimestamp) / \(state.duration.playerTimestamp)"
    }
}

extension TimeInterval {
    /// `PlayerView.asTimestamp`: `mm:ss`, or `h:mm:ss` past an hour.
    var playerTimestamp: String {
        let total = Int(max(self, 0))
        let hours = total / 3_600
        let minutes = (total % 3_600) / 60
        let seconds = total % 60
        return hours > 0
            ? String(format: "%d:%02d:%02d", hours, minutes, seconds)
            : String(format: "%02d:%02d", minutes, seconds)
    }
}
