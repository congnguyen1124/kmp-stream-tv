import AVFoundation
import Combine

@MainActor
final class StreamPlayer: ObservableObject {
    let player = AVPlayer()

    @Published private(set) var isPlaying = false
    @Published private(set) var currentTime: Double = 0
    @Published private(set) var duration: Double = 0
    @Published private(set) var completionCount = 0

    private var timeObserver: Any?
    private var endObserver: NSObjectProtocol?

    init() {
        timeObserver = player.addPeriodicTimeObserver(
            forInterval: CMTime(seconds: 0.5, preferredTimescale: 600),
            queue: .main
        ) { [weak self] time in
            Task { @MainActor [weak self] in
                guard let self else { return }
                currentTime = time.seconds.isFinite ? time.seconds : 0
                let itemDuration = player.currentItem?.duration.seconds ?? 0
                duration = itemDuration.isFinite ? itemDuration : 0
                isPlaying = player.rate != 0
            }
        }
        endObserver = NotificationCenter.default.addObserver(
            forName: AVPlayerItem.didPlayToEndTimeNotification,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            Task { @MainActor [weak self] in
                guard let self, notification.object as? AVPlayerItem === player.currentItem else { return }
                isPlaying = false
                completionCount += 1
            }
        }
    }

    func load(url: URL) {
        currentTime = 0
        duration = 0
        player.replaceCurrentItem(with: AVPlayerItem(url: url))
        player.play()
        isPlaying = true
    }

    func togglePlayback() {
        if player.rate == 0 {
            player.play()
            isPlaying = true
        } else {
            pause()
        }
    }

    func play() {
        player.play()
        isPlaying = true
    }

    func replay() {
        player.seek(to: .zero) { [weak self] _ in
            Task { @MainActor [weak self] in self?.play() }
        }
    }

    func pause() {
        player.pause()
        isPlaying = false
    }

    func seek(by seconds: Double) {
        let target = max(0, min(currentTime + seconds, duration))
        player.seek(to: CMTime(seconds: target, preferredTimescale: 600))
    }

    func stop() {
        pause()
        player.replaceCurrentItem(with: nil)
        currentTime = 0
        duration = 0
    }

    deinit {
        if let timeObserver {
            player.removeTimeObserver(timeObserver)
        }
        if let endObserver {
            NotificationCenter.default.removeObserver(endObserver)
        }
    }
}
