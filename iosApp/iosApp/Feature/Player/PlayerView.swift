import Shared
import Foundation
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

            VStack(spacing: 0) {
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
        HStack(spacing: 0) {
            Button("Back", action: dismiss.callAsFunction)
                .buttonStyle(PlayerTextButtonStyle())

            if content.isLive {
                StreamBadge(text: "LIVE", color: .streamLive)
                    .padding(.leading, 16)
            }

            Text(content.title)
                .font(.streamBold(20))
                .foregroundStyle(.white)
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.leading, 16)
        }
        .padding(20)
        .background(Color.black.opacity(0.6))
    }

    private var playbackControls: some View {
        VStack(spacing: 0) {
            if !content.isLive {
                GeometryReader { geometry in
                    ZStack(alignment: .leading) {
                        Rectangle().fill(Color.white.opacity(0.4))
                        Rectangle()
                            .fill(Color.streamAccent)
                            .frame(width: geometry.size.width * playbackFraction)
                    }
                }
                .frame(height: 4)
            }

            Text(content.isLive ? "LIVE" : timeLabel)
                .font(.streamRegular(14))
                .foregroundStyle(Color.streamSecondaryText)
                .padding(.top, 8)

            HStack(spacing: 12) {
                if !content.isLive {
                    Button("−10") { streamPlayer.seek(by: -10) }
                        .buttonStyle(PlayerTextButtonStyle())
                        .accessibilityLabel("Rewind 10 seconds")
                }

                Button(streamPlayer.isPlaying ? "Pause" : "Play") {
                    streamPlayer.togglePlayback()
                }
                .buttonStyle(PlayerTextButtonStyle())

                if !content.isLive {
                    Button("+10") { streamPlayer.seek(by: 10) }
                        .buttonStyle(PlayerTextButtonStyle())
                        .accessibilityLabel("Forward 10 seconds")
                }
            }
            .padding(.top, 10)
        }
        .padding(20)
        .background(Color.black.opacity(0.6))
    }

    private var playbackFraction: CGFloat {
        guard streamPlayer.duration > 0 else { return 0 }
        return CGFloat(min(max(streamPlayer.currentTime / streamPlayer.duration, 0), 1))
    }

    private var timeLabel: String {
        "\(timestamp(streamPlayer.currentTime)) / \(timestamp(streamPlayer.duration))"
    }

    private func timestamp(_ seconds: Double) -> String {
        let total = max(Int(seconds.rounded(.down)), 0)
        let hours = total / 3_600
        let minutes = (total % 3_600) / 60
        let remainingSeconds = total % 60
        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, remainingSeconds)
        }
        return String(format: "%02d:%02d", minutes, remainingSeconds)
    }
}

private struct PlayerTextButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.streamSemiBold(16))
            .foregroundStyle(.white)
            .padding(.horizontal, 16)
            .frame(minHeight: 40)
            .background(Color.streamAccent.opacity(configuration.isPressed ? 0.7 : 1))
            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
    }
}
