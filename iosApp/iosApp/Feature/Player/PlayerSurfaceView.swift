import AVFoundation
import AVKit
import SwiftUI

/// The video surface, backed by an `AVPlayerLayer` rather than `AVPlayerViewController`.
///
/// `AVPlayerViewController` has no public API to *start* Picture in Picture, and the Android
/// overlay drives PiP from its own button and from `onUserLeaveHint`. A raw layer plus
/// `AVPictureInPictureController` is what gives the same control.
struct PlayerSurfaceView: UIViewRepresentable {
    let player: AVPlayer
    let coordinator: PictureInPictureCoordinator

    func makeUIView(context: Context) -> PlayerLayerView {
        let view = PlayerLayerView()
        view.playerLayer.player = player
        view.playerLayer.videoGravity = .resizeAspect
        view.backgroundColor = .black
        coordinator.attach(layer: view.playerLayer)
        return view
    }

    func updateUIView(_ view: PlayerLayerView, context: Context) {
        if view.playerLayer.player !== player {
            view.playerLayer.player = player
            coordinator.attach(layer: view.playerLayer)
        }
    }
}

/// A `UIView` whose backing layer *is* the player layer, so it resizes with the view for free.
final class PlayerLayerView: UIView {
    override static var layerClass: AnyClass { AVPlayerLayer.self }

    // swiftlint:disable:next force_cast
    var playerLayer: AVPlayerLayer { layer as! AVPlayerLayer }
}

/// Owns the `AVPictureInPictureController` and reports its transitions.
///
/// Counterpart of the PiP half of `MainActivity`: `start()` is `enterPlayerPictureInPicture`,
/// `setAutomaticStart` is `setAutoEnterEnabled`, and `onChange` is
/// `onPictureInPictureModeChanged`.
@MainActor
final class PictureInPictureCoordinator: NSObject, ObservableObject {
    @Published private(set) var isActive = false
    @Published private(set) var isPossible = false

    /// Fires on every enter/leave, so the overlay can restore its presentation.
    var onChange: ((Bool) -> Void)?

    private var controller: AVPictureInPictureController?
    private var possibleObservation: NSKeyValueObservation?
    private var automaticStart = false

    var isSupported: Bool { AVPictureInPictureController.isPictureInPictureSupported() }

    func attach(layer: AVPlayerLayer) {
        guard isSupported else { return }
        guard let controller = AVPictureInPictureController(playerLayer: layer) else { return }
        controller.delegate = self
        controller.canStartPictureInPictureAutomaticallyFromInline = automaticStart
        possibleObservation = controller.observe(\.isPictureInPicturePossible, options: [.initial, .new]) {
            [weak self] controller, _ in
            MainActor.assumeIsolated { self?.isPossible = controller.isPictureInPicturePossible }
        }
        self.controller = controller
    }

    func start() {
        guard let controller, controller.isPictureInPicturePossible, !controller.isPictureInPictureActive else {
            return
        }
        controller.startPictureInPicture()
    }

    func stop() {
        guard let controller, controller.isPictureInPictureActive else { return }
        controller.stopPictureInPicture()
    }

    /// `PlayerFragment.shouldAutoEnterPictureInPicture` decides this on Android at the moment the
    /// user leaves; on iOS the same decision has to be armed in advance, because the system starts
    /// PiP itself when the app is backgrounded.
    func setAutomaticStart(_ enabled: Bool) {
        guard automaticStart != enabled else { return }
        automaticStart = enabled
        controller?.canStartPictureInPictureAutomaticallyFromInline = enabled
    }
}

extension PictureInPictureCoordinator: AVPictureInPictureControllerDelegate {
    nonisolated func pictureInPictureControllerDidStartPictureInPicture(
        _ controller: AVPictureInPictureController
    ) {
        Task { @MainActor in
            isActive = true
            onChange?(true)
        }
    }

    nonisolated func pictureInPictureControllerDidStopPictureInPicture(
        _ controller: AVPictureInPictureController
    ) {
        Task { @MainActor in
            isActive = false
            onChange?(false)
        }
    }

    nonisolated func pictureInPictureController(
        _ controller: AVPictureInPictureController,
        failedToStartPictureInPictureWithError error: any Error
    ) {
        Task { @MainActor in
            isActive = false
            onChange?(false)
        }
    }
}
