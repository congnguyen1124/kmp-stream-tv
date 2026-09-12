import SwiftUI

/// Transport strip under the minimized player, ported from `MiniPlaybackControllerView.kt`:
/// a time bar over rewind, play/pause/replay and forward.
///
/// Every dimension the strip paints is divided by the card's zoom scale, for the same reason the
/// Android widget divides its own: the strip lives inside the scaled card, so an undivided value
/// would grow with the pinch.
struct MiniPlaybackControllerView: View {
    let state: StreamPlayerState
    let zoomScale: CGFloat
    let isEnabled: Bool

    let onToggle: () -> Void
    let onReplay: () -> Void
    let onRewind: () -> Void
    let onForward: () -> Void

    private static let enabledAlpha: CGFloat = 1
    private static let disabledAlpha: CGFloat = 0.38
    private static let minZoomScale: CGFloat = 0.01

    private var scale: CGFloat { max(zoomScale, Self.minZoomScale) }
    private var actionPadding: CGFloat { PlayerMetrics.miniActionPadding / scale }
    private var timeBarHeight: CGFloat { max(PlayerMetrics.miniTimeBarHeight / scale, 1) }
    private var isEnded: Bool { state.playbackState == .ended }
    private var isBuffering: Bool {
        state.playbackState == .buffering || state.playbackState == .idle
    }

    var body: some View {
        VStack(spacing: 0) {
            timeBar

            HStack(spacing: 0) {
                Spacer(minLength: 0)

                action("ic_player_rewind", label: "Rewind", isEnabled: isEnabled && state.duration > 0) {
                    onRewind()
                }

                if isBuffering {
                    ProgressView()
                        .tint(.white)
                        .padding(actionPadding)
                        .frame(width: 32, height: 32)
                } else {
                    action(
                        isEnded ? "ic_player_replay" : (state.isPlaying ? "ic_player_pause" : "ic_player_play"),
                        label: isEnded ? "Replay" : (state.isPlaying ? "Pause" : "Play"),
                        isEnabled: isEnabled
                    ) {
                        if isEnded { onReplay() } else { onToggle() }
                    }
                }

                action("ic_player_forward", label: "Forward", isEnabled: isEnabled && !isEnded) {
                    onForward()
                }

                Spacer(minLength: 0)
            }
            .frame(maxHeight: .infinity)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        // `player_mini_footer` resolves to `appBackground`, same as `bg_mini_player_action`.
        .background(Color.streamBackground)
        .opacity(isEnabled ? Self.enabledAlpha : Self.disabledAlpha)
    }

    private var timeBar: some View {
        GeometryReader { geometry in
            ZStack(alignment: .leading) {
                Rectangle().fill(Color.white.opacity(0.2))
                Rectangle()
                    .fill(Color.streamSecondaryText)
                    .frame(width: geometry.size.width * fraction(state.bufferedPosition))
                Rectangle()
                    .fill(Color.white)
                    .frame(width: geometry.size.width * fraction(state.position))
            }
        }
        .frame(height: timeBarHeight)
    }

    private func action(
        _ name: String,
        label: String,
        isEnabled: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(name)
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .padding(actionPadding)
                .frame(width: 32, height: 32)
                .foregroundStyle(.white)
                // `bg_mini_player_action`: an opaque app-background hit surface, which is also
                // what keeps the tap on the transport instead of the card underneath it.
                .background(Color.streamBackground)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!isEnabled)
        .opacity(isEnabled ? Self.enabledAlpha : Self.disabledAlpha)
        .accessibilityLabel(label)
    }

    private func fraction(_ value: TimeInterval) -> CGFloat {
        guard state.duration > 0 else { return 0 }
        return min(max(value / state.duration, 0), 1)
    }
}
