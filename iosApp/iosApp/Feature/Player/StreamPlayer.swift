import AVFoundation
import Combine
import Foundation

/// Port of `StreamTvPlaybackState`.
enum StreamPlaybackState: Equatable {
    case idle
    case buffering
    case ready
    case ended
}

/// Port of `StreamTvPlaybackError`, including the `isRetryable` split the error view keys off.
enum StreamPlaybackError: Equatable {
    case noNetwork
    case notFound
    case notEntitled
    case unsupportedFormat
    case unknown

    var isRetryable: Bool {
        switch self {
        case .noNetwork, .unknown: true
        case .notFound, .notEntitled, .unsupportedFormat: false
        }
    }

    /// Mirrors the `player_error_*` strings.
    var message: String {
        switch self {
        case .noNetwork: "No network connection. Check your connection and try again."
        case .notFound: "This content is no longer available."
        case .notEntitled: "You do not have access to this content."
        case .unsupportedFormat: "This format is not supported on your device."
        case .unknown: "Something went wrong while playing this content."
        }
    }

    /// Maps an `AVFoundation` failure onto the shared taxonomy.
    static func from(_ error: Error?) -> StreamPlaybackError {
        guard let error = error as NSError? else { return .unknown }
        if error.domain == NSURLErrorDomain {
            switch error.code {
            case NSURLErrorNotConnectedToInternet,
                 NSURLErrorNetworkConnectionLost,
                 NSURLErrorTimedOut,
                 NSURLErrorCannotConnectToHost:
                return .noNetwork
            case NSURLErrorFileDoesNotExist, NSURLErrorBadURL, NSURLErrorUnsupportedURL:
                return .notFound
            default:
                return .unknown
            }
        }
        switch error.code {
        case -1_100, -12_939, -12_660: return .notFound
        case -11_800, -12_746, -12_318: return .unsupportedFormat
        case -16_840, -16_845: return .notEntitled
        default: return .unknown
        }
    }
}

/// An audio or subtitle option. Port of `StreamTvAudioTrack` / `StreamTvTextTrack`.
struct StreamMediaTrack: Identifiable, Equatable {
    static let offId = "off"

    let id: String
    let label: String
    let isSelected: Bool
}

/// A video variant. Port of `StreamTvVideoTrack`.
struct StreamVideoTrack: Identifiable, Equatable {
    static let autoId = "auto"

    let id: String
    let height: Int
    /// Peak bits per second, as `AVAssetVariant` reports it.
    let bitrate: Int
    let isSelected: Bool
}

/// Immutable snapshot the whole player UI renders from. Port of `StreamTvPlayerState`.
struct StreamPlayerState: Equatable {
    var playbackState: StreamPlaybackState = .idle
    var isPlaying = false
    var position: TimeInterval = 0
    var duration: TimeInterval = 0
    var bufferedPosition: TimeInterval = 0
    var playbackError: StreamPlaybackError?
    var videoTracks: [StreamVideoTrack] = []
    var audioTracks: [StreamMediaTrack] = []
    var textTracks: [StreamMediaTrack] = []

    static let initial = StreamPlayerState()
}

/// iOS counterpart of `StreamTvPlayerManager`: owns the `AVPlayer`, publishes immutable snapshots
/// and takes commands. Nothing in the UI layer reads the `AVPlayer` directly, exactly as the
/// Android widgets only ever see `StreamTvPlayerState`.
@MainActor
final class StreamPlayer: ObservableObject {
    /// Both platforms step by ten seconds.
    static let seekStep: TimeInterval = 10

    let player = AVPlayer()

    @Published private(set) var state = StreamPlayerState.initial
    @Published private(set) var isMuted = false
    @Published private(set) var speed: Float = 1

    /// Bumped once per play-to-end. The Short feed advances to the next item on a change here, and
    /// the Story group advances to the next story, so it has to count rather than latch: replaying
    /// the same item must be distinguishable from never having finished.
    @Published private(set) var completionCount = 0

    /// Flat readings over `state`, kept because the Short and Story feeds only need the three
    /// values and never the full snapshot. Computed rather than stored so there is one source of
    /// truth; `state` is `@Published`, so a view observing this object still redraws on a change.
    var isPlaying: Bool { state.isPlaying }
    var currentTime: TimeInterval { state.position }
    var duration: TimeInterval { state.duration }

    private var timeObserver: Any?
    private var cancellables = Set<AnyCancellable>()
    private var itemCancellables = Set<AnyCancellable>()
    private var trackLoadTask: Task<Void, Never>?
    private var loadedURL: URL?
    /// `true` between a play command and the first frame, so a paused-by-buffering player still
    /// paints its pause icon rather than flipping back to play.
    private var wantsPlayback = false

    init() {
        configureAudioSession()
        observePlayer()
    }

    deinit {
        if let timeObserver {
            player.removeTimeObserver(timeObserver)
        }
    }

    // MARK: - Commands

    /// Loads and starts playback. Port of `StreamTvPlayerManager.loadAndPlay`.
    func load(url: URL) {
        trackLoadTask?.cancel()
        itemCancellables.removeAll()
        loadedURL = url
        wantsPlayback = true

        let item = AVPlayerItem(url: url)
        state = StreamPlayerState(playbackState: .buffering)
        completionCount = 0
        player.replaceCurrentItem(with: item)
        observeItem(item)
        player.rate = speed
        loadTracks(for: item)
    }

    /// Re-runs the last load. Port of `PlayerFragment.retry`.
    func retry() {
        guard let loadedURL else { return }
        load(url: loadedURL)
    }

    func play() {
        guard player.currentItem != nil else { return }
        wantsPlayback = true
        player.rate = speed
        publish()
    }

    func pause() {
        wantsPlayback = false
        player.pause()
        publish()
    }

    func togglePlayback() {
        if state.isPlaying { pause() } else { play() }
    }

    /// Live streams resume at the edge rather than where they were paused.
    /// Port of `togglePlayPauseAtDefaultPosition`.
    func togglePlaybackAtDefaultPosition() {
        if state.isPlaying {
            pause()
        } else {
            if let seekable = player.currentItem?.seekableTimeRanges.last?.timeRangeValue {
                player.seek(to: CMTimeRangeGetEnd(seekable), toleranceBefore: .zero, toleranceAfter: .zero)
            }
            play()
        }
    }

    func replay() {
        player.seek(to: .zero, toleranceBefore: .zero, toleranceAfter: .zero) { [weak self] _ in
            Task { @MainActor [weak self] in self?.play() }
        }
    }

    func seekBack() { seek(by: -Self.seekStep) }

    func seekForward() { seek(by: Self.seekStep) }

    func seek(by seconds: TimeInterval) {
        seek(to: state.position + seconds)
    }

    func seek(to seconds: TimeInterval) {
        guard state.duration > 0 else { return }
        let target = min(max(seconds, 0), state.duration)
        player.seek(
            to: CMTime(seconds: target, preferredTimescale: 600),
            toleranceBefore: .zero,
            toleranceAfter: .zero
        )
        state.position = target
    }

    func setSpeed(_ value: Float) {
        speed = value
        if state.isPlaying { player.rate = value }
    }

    func toggleMuted() {
        isMuted.toggle()
        player.isMuted = isMuted
    }

    func stop() {
        trackLoadTask?.cancel()
        player.pause()
        player.replaceCurrentItem(with: nil)
        itemCancellables.removeAll()
        loadedURL = nil
        wantsPlayback = false
        state = .initial
        completionCount = 0
    }

    // MARK: - Track selection

    /// `AVPlayer` has no discrete variant picker, so a quality choice becomes a peak-bitrate
    /// ceiling — the closest equivalent of pinning an ExoPlayer video track.
    func selectVideoTrack(id: String) {
        guard let item = player.currentItem else { return }
        if id.isEmpty || id == StreamVideoTrack.autoId {
            item.preferredPeakBitRate = 0
            state.videoTracks = state.videoTracks.map {
                StreamVideoTrack(id: $0.id, height: $0.height, bitrate: $0.bitrate, isSelected: false)
            }
            return
        }
        guard let track = state.videoTracks.first(where: { $0.id == id }) else { return }
        item.preferredPeakBitRate = Double(track.bitrate)
        state.videoTracks = state.videoTracks.map {
            StreamVideoTrack(id: $0.id, height: $0.height, bitrate: $0.bitrate, isSelected: $0.id == id)
        }
    }

    func selectAudioTrack(id: String) {
        select(id: id, characteristic: .audible) { [weak self] tracks in
            self?.state.audioTracks = tracks
        }
    }

    func selectTextTrack(id: String) {
        select(id: id, characteristic: .legible) { [weak self] tracks in
            self?.state.textTracks = tracks
        }
    }

    private func select(
        id: String,
        characteristic: AVMediaCharacteristic,
        store: @escaping ([StreamMediaTrack]) -> Void
    ) {
        guard let item = player.currentItem else { return }
        let asset = item.asset
        trackLoadTask = Task { [weak self] in
            guard let group = try? await asset.loadMediaSelectionGroup(for: characteristic) else { return }
            guard !Task.isCancelled else { return }
            await MainActor.run {
                let option = group.options.first { Self.trackId(for: $0) == id }
                item.select(option, in: group)
                store(Self.tracks(in: group, selected: option))
                self?.publish()
            }
        }
    }

    // MARK: - Observation

    private func configureAudioSession() {
        // `.playback` is what lets audio survive the silent switch and keeps Picture in Picture
        // alive once the app is backgrounded.
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .moviePlayback)
        try? AVAudioSession.sharedInstance().setActive(true)
    }

    private func observePlayer() {
        timeObserver = player.addPeriodicTimeObserver(
            forInterval: CMTime(seconds: 0.25, preferredTimescale: 600),
            queue: .main
        ) { [weak self] _ in
            MainActor.assumeIsolated { self?.publish() }
        }

        player.publisher(for: \.timeControlStatus)
            .sink { [weak self] _ in self?.publish() }
            .store(in: &cancellables)
    }

    private func observeItem(_ item: AVPlayerItem) {
        item.publisher(for: \.status)
            .sink { [weak self] status in
                guard let self else { return }
                if status == .failed {
                    state.playbackError = .from(item.error)
                    state.playbackState = .idle
                    wantsPlayback = false
                }
                publish()
            }
            .store(in: &itemCancellables)

        item.publisher(for: \.isPlaybackBufferEmpty)
            .sink { [weak self] _ in self?.publish() }
            .store(in: &itemCancellables)

        item.publisher(for: \.isPlaybackLikelyToKeepUp)
            .sink { [weak self] _ in self?.publish() }
            .store(in: &itemCancellables)

        NotificationCenter.default
            .publisher(for: .AVPlayerItemDidPlayToEndTime, object: item)
            .sink { [weak self] _ in
                guard let self else { return }
                wantsPlayback = false
                state.playbackState = .ended
                state.isPlaying = false
                completionCount += 1
            }
            .store(in: &itemCancellables)

        NotificationCenter.default
            .publisher(for: .AVPlayerItemFailedToPlayToEndTime, object: item)
            .sink { [weak self] note in
                guard let self else { return }
                let error = note.userInfo?[AVPlayerItemFailedToPlayToEndTimeErrorKey] as? Error
                state.playbackError = .from(error)
                wantsPlayback = false
                publish()
            }
            .store(in: &itemCancellables)
    }

    private func loadTracks(for item: AVPlayerItem) {
        let asset = item.asset
        trackLoadTask = Task { [weak self] in
            // Variants are an `AVURLAsset` property — an HLS master playlist's rendition list.
            var loadedVariants: [AVAssetVariant] = []
            if let urlAsset = asset as? AVURLAsset {
                loadedVariants = (try? await urlAsset.load(.variants)) ?? []
            }
            let audioGroup = try? await asset.loadMediaSelectionGroup(for: .audible)
            let textGroup = try? await asset.loadMediaSelectionGroup(for: .legible)
            guard !Task.isCancelled else { return }

            await MainActor.run {
                guard let self, self.player.currentItem === item else { return }
                self.state.videoTracks = Self.videoTracks(from: loadedVariants)
                if let audioGroup {
                    self.state.audioTracks = Self.tracks(
                        in: audioGroup,
                        selected: item.currentMediaSelection.selectedMediaOption(in: audioGroup)
                    )
                }
                if let textGroup {
                    self.state.textTracks = Self.tracks(
                        in: textGroup,
                        selected: item.currentMediaSelection.selectedMediaOption(in: textGroup)
                    )
                }
            }
        }
    }

    /// Folds the live `AVPlayer` readings into the snapshot the UI renders.
    private func publish() {
        guard let item = player.currentItem else { return }

        let duration = item.duration.seconds
        state.duration = duration.isFinite && duration > 0 ? duration : 0
        let position = player.currentTime().seconds
        state.position = position.isFinite ? max(position, 0) : 0
        state.bufferedPosition = item.loadedTimeRanges
            .map { CMTimeRangeGetEnd($0.timeRangeValue).seconds }
            .filter { $0.isFinite }
            .max() ?? 0

        if state.playbackState != .ended {
            let isStalled = item.isPlaybackBufferEmpty || !item.isPlaybackLikelyToKeepUp
            state.playbackState = switch item.status {
            case .failed: .idle
            case .readyToPlay: isStalled && player.timeControlStatus != .playing ? .buffering : .ready
            default: .buffering
            }
        }
        state.isPlaying = player.timeControlStatus == .playing ||
            (wantsPlayback && player.timeControlStatus == .waitingToPlayAtSpecifiedRate)

        if state.playbackError != nil, item.status != .failed, player.timeControlStatus == .playing {
            state.playbackError = nil
        }
    }

    // MARK: - Track mapping

    private static func videoTracks(from variants: [AVAssetVariant]) -> [StreamVideoTrack] {
        variants
            .compactMap { variant -> StreamVideoTrack? in
                guard let bitrate = variant.peakBitRate ?? variant.averageBitRate else { return nil }
                let height = variant.videoAttributes?.presentationSize.height ?? 0
                return StreamVideoTrack(
                    id: String(Int(bitrate)),
                    height: Int(height),
                    bitrate: Int(bitrate),
                    isSelected: false
                )
            }
            .sorted { $0.bitrate > $1.bitrate }
    }

    private static func tracks(
        in group: AVMediaSelectionGroup,
        selected: AVMediaSelectionOption?
    ) -> [StreamMediaTrack] {
        group.options.map { option in
            StreamMediaTrack(
                id: trackId(for: option),
                label: label(for: option),
                isSelected: option == selected
            )
        }
    }

    private static func trackId(for option: AVMediaSelectionOption) -> String {
        [option.mediaType.rawValue, option.extendedLanguageTag ?? "", option.displayName]
            .joined(separator: "|")
    }

    private static func label(for option: AVMediaSelectionOption) -> String {
        if let tag = option.extendedLanguageTag,
           let language = Locale.current.localizedString(forLanguageCode: tag),
           !language.isEmpty {
            return language
        }
        return option.displayName
    }
}
