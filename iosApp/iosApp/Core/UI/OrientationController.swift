import SwiftUI
import UIKit

/// Drives interface orientation the way `PlayerFragment` drives `requestedOrientation`.
///
/// `MainActivity` is declared with no fixed `screenOrientation`, so the app follows the sensor
/// until the player asks otherwise: `toggleFullscreen` requests sensor landscape or portrait,
/// minimizing requests portrait, and closing hands control back with
/// `SCREEN_ORIENTATION_UNSPECIFIED`. iOS needs two halves for the same effect — a mask the app
/// delegate reports, and a geometry request that performs the rotation.
@MainActor
enum OrientationController {
    /// What `application(_:supportedInterfaceOrientationsFor:)` reports. `.allButUpsideDown` is
    /// the `UNSPECIFIED` equivalent.
    private(set) static var mask: UIInterfaceOrientationMask = .allButUpsideDown

    static var isLandscape: Bool {
        scene?.interfaceOrientation.isLandscape ?? false
    }

    /// `SCREEN_ORIENTATION_SENSOR_LANDSCAPE`.
    static func requestLandscape() { apply(.landscape) }

    /// `SCREEN_ORIENTATION_SENSOR_PORTRAIT`.
    static func requestPortrait() { apply(.portrait) }

    /// `SCREEN_ORIENTATION_UNSPECIFIED`: stop forcing, follow the device again.
    static func release() {
        mask = .allButUpsideDown
        rootController?.setNeedsUpdateOfSupportedInterfaceOrientations()
    }

    private static func apply(_ newMask: UIInterfaceOrientationMask) {
        mask = newMask
        rootController?.setNeedsUpdateOfSupportedInterfaceOrientations()
        scene?.requestGeometryUpdate(.iOS(interfaceOrientations: newMask))
    }

    private static var scene: UIWindowScene? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
            ?? UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first
    }

    private static var rootController: UIViewController? {
        scene?.keyWindow?.rootViewController
    }
}

/// Reports [OrientationController]'s mask to UIKit, and is also where the scene-level lifecycle
/// hooks the player needs would go.
final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        supportedInterfaceOrientationsFor window: UIWindow?
    ) -> UIInterfaceOrientationMask {
        MainActor.assumeIsolated { OrientationController.mask }
    }
}
