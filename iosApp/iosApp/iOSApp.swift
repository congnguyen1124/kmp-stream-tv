import SwiftUI
import Shared

@main
struct iOSApp: App {
    /// Reports `OrientationController`'s mask, so the player can force landscape and hand control
    /// back the way `PlayerFragment` does with `requestedOrientation`.
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    init() {
        BootstrapKt.startKoinForIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
