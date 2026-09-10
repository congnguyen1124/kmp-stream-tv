import Shared
import SwiftUI

struct PlayerView: View {
    let content: HomeContentUiModel

    @Environment(\.dismiss) private var dismiss
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var streamPlayer = StreamPlayer()

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            NativePlayerView(player: streamPlayer.player)
                .ignoresSafeArea()

            VStack {
                playerHeader
                Spacer()
                playbackControls
            }
        }
        .onAppear {
            if let url = URL(string: content.videoUrl) {
                streamPlayer.load(url: url)
            }
        }
        .onChange(of: scenePhase) { _, phase in
            if phase != .active {
                streamPlayer.pause()
            }
        }
        .onDisappear { streamPlayer.stop() }
    }

    private var playerHeader: some View {
        HStack(spacing: 12) {
            Button(action: dismiss.callAsFunction) {
                Image(systemName: "xmark")
                    .font(.headline)
                    .frame(width: 44, height: 44)
                    .background(.black.opacity(0.64), in: Circle())
            }
            .accessibilityLabel("Close")

            if content.isLive {
                StreamBadge(text: "LIVE", color: .streamLive)
            }

            Text(content.title)
                .font(.headline)
                .lineLimit(1)
            Spacer()
        }
        .foregroundStyle(.white)
        .padding()
        .background(
            LinearGradient(colors: [.black.opacity(0.75), .clear], startPoint: .top, endPoint: .bottom)
        )
    }

    private var playbackControls: some View {
        VStack(spacing: 14) {
            if !content.isLive {
                ProgressView(
                    value: streamPlayer.currentTime,
                    total: max(streamPlayer.duration, 1)
                )
                .tint(.streamAccentBright)
            }

            HStack(spacing: 26) {
                if !content.isLive {
                    control("gobackward.10", label: "Rewind 10 seconds") {
                        streamPlayer.seek(by: -10)
                    }
                }

                control(
                    streamPlayer.isPlaying ? "pause.fill" : "play.fill",
                    label: streamPlayer.isPlaying ? "Pause" : "Play"
                ) {
                    streamPlayer.togglePlayback()
                }

                if !content.isLive {
                    control("goforward.10", label: "Forward 10 seconds") {
                        streamPlayer.seek(by: 10)
                    }
                }
            }
        }
        .padding(22)
        .background(.black.opacity(0.62))
    }

    private func control(
        _ systemName: String,
        label: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.title2)
                .frame(width: 52, height: 52)
                .background(.white.opacity(0.16), in: Circle())
        }
        .foregroundStyle(.white)
        .accessibilityLabel(label)
    }
}
