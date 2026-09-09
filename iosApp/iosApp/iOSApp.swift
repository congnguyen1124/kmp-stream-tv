import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        BootstrapKt.startKoinForIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
