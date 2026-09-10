import SwiftUI

/// Several Android Home layouts raise `Toast` feedback. iOS routes them all to the single
/// `HomeTabView` toast so nested sections never own a competing presentation.
private struct HomeToastKey: EnvironmentKey {
    static let defaultValue: (String) -> Void = { _ in }
}

extension EnvironmentValues {
    var homeToast: (String) -> Void {
        get { self[HomeToastKey.self] }
        set { self[HomeToastKey.self] = newValue }
    }
}
