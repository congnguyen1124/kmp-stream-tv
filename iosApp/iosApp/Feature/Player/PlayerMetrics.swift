import SwiftUI

/// Android `dimens.xml` player values reused as iOS points, matching how `StreamMetrics` already
/// mirrors the Home dimensions.
enum PlayerMetrics {
    // Controller — `player_*`.
    static let controllerPadding: CGFloat = 16
    static let controllerPaddingMedium: CGFloat = 12
    static let controllerPaddingVertical: CGFloat = 4
    static let actionPadding: CGFloat = 4
    static let centerActionSpacing: CGFloat = 24
    static let centerButtonSize: CGFloat = 40
    static let progressHeight: CGFloat = 2
    static let liveCorner: CGFloat = 16
    static let livePaddingHorizontal: CGFloat = 8
    static let durationTextSize: CGFloat = 14
    static let bottomControllerPadding: CGFloat = 16
    static let titleMinHeight: CGFloat = 24
    static let buttonIconSize: CGFloat = 24
    static let bottomActionMenuHeight: CGFloat = 20
    static let settingLayoutPadding: CGFloat = 32
    static let settingContentPadding: CGFloat = 24
    static let transitionDuration: TimeInterval = 0.2
    /// `PlayerView.CONTROLLER_DISPLAY_MILLIS`.
    static let controllerDisplaySeconds: TimeInterval = 5

    // Floating mini player — `mini_player_*`.
    static let miniBorderPadding: CGFloat = 8
    static let miniBottomMargin: CGFloat = 80
    static let miniFooterHeight: CGFloat = 44
    static let miniCornerRadius: CGFloat = 16
    static let miniElevation: CGFloat = 2
    static let miniBorderWidth: CGFloat = 1
    static let miniTimeBarHeight: CGFloat = 2
    static let miniActionPadding: CGFloat = 8
    static let miniCloseSize: CGFloat = 40
    /// Upper bound on the mini player width, so it stays a *mini* player on a large window.
    static let miniMaxWidth: CGFloat = 380

    static let playerRatio: CGFloat = 16.0 / 9.0
}
