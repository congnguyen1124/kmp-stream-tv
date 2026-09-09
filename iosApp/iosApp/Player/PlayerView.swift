import AVKit
import SwiftUI
import Shared

struct PlayerView: View {
    let content: HomeContentUiModel

    @Environment(\.dismiss) private var dismiss
    @StateObject private var streamPlayer = StreamPlayer()

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            NativePlayerView(player: streamPlayer.player)
                .ignoresSafeArea()

            VStack {
                HStack(spacing: 12) {
                    Button(action: dismiss.callAsFunction) {
                        Image(systemName: "xmark")
                            .font(.headline)
                            .padding(12)
                            .background(.black.opacity(0.6), in: Circle())
                    }
                    if content.isLive {
                        Text("LIVE")
                            .font(.caption.bold())
                            .padding(.horizontal, 8)
                            .padding(.vertical, 5)
                            .background(.red, in: RoundedRectangle(cornerRadius: 5))
                    }
                    Text(content.title).font(.headline).lineLimit(1)
                    Spacer()
                }
                .foregroundStyle(.white)
                .padding()

                Spacer()

                VStack(spacing: 12) {
                    if !content.isLive {
                        ProgressView(
                            value: streamPlayer.currentTime,
                            total: max(streamPlayer.duration, 1)
                        )
                        .tint(.orange)
                    }
                    HStack(spacing: 26) {
                        if !content.isLive {
                            control("gobackward.10") { streamPlayer.seek(by: -10) }
                        }
                        control(streamPlayer.isPlaying ? "pause.fill" : "play.fill") {
                            streamPlayer.togglePlayback()
                        }
                        if !content.isLive {
                            control("goforward.10") { streamPlayer.seek(by: 10) }
                        }
                    }
                }
                .padding(22)
                .background(.black.opacity(0.58))
            }
        }
        .onAppear {
            if let url = URL(string: content.videoUrl) {
                streamPlayer.load(url: url)
            }
        }
        .onDisappear { streamPlayer.stop() }
    }

    private func control(_ systemName: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.title2)
                .frame(width: 52, height: 52)
                .background(.white.opacity(0.16), in: Circle())
        }
        .foregroundStyle(.white)
    }
}

private struct NativePlayerView: UIViewControllerRepresentable {
    let player: AVPlayer

    func makeUIViewController(context: Context) -> AVPlayerViewController {
        let controller = AVPlayerViewController()
        controller.player = player
        controller.showsPlaybackControls = false
        controller.videoGravity = .resizeAspect
        return controller
    }

    func updateUIViewController(_ controller: AVPlayerViewController, context: Context) {
        controller.player = player
    }
}
