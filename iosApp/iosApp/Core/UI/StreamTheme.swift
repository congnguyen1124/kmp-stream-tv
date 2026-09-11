import SwiftUI

/// Android `dimens.xml` values reused as iOS points, as required by the native visual parity spec.
enum StreamMetrics {
    static let contentInset: CGFloat = 16
    static let cardSpacing: CGFloat = 8
    static let listTitleBottomPadding: CGFloat = 12
    static let sectionVerticalPadding: CGFloat = 16
    static let shortSectionVerticalPadding: CGFloat = 24
    static let homeContentPaddingTop: CGFloat = 84
    static let homeContentPaddingBottom: CGFloat = 32
    static let verticalListDivider: CGFloat = 24
    static let carouselMinSidePadding: CGFloat = 40
    static let highlightTopbarOffset: CGFloat = 250
    static let thumbnailCorner: CGFloat = 8
    static let storyCorner: CGFloat = 12
    static let logoAppCorner: CGFloat = 12
    static let radiusNormal: CGFloat = 12
    static let radiusNormalExtra: CGFloat = 16
    static let buttonRadius: CGFloat = 8
    static let buttonHeight: CGFloat = 48
    static let iconButtonSize: CGFloat = 24
    static let largeIconButtonSize: CGFloat = 56
}

/// Card geometry taken from the Android item layouts so both trees size artwork identically.
enum StreamCardSize {
    static let highlightWide = CGSize(width: 340, height: 191)
    static let highlightTall = CGSize(width: 294, height: 441)
    static let ephemeralWide = CGSize(width: 172, height: 97)
    static let ephemeralTall = CGSize(width: 106, height: 185)
    static let story = CGSize(width: 162, height: 288)
    static let short = CGSize(width: 162, height: 288)
    static let topTen = CGSize(width: 134, height: 201)
    static let topTenTitleWidth: CGFloat = 144
    static let topTenPositionHeight: CGFloat = 61
    static let circleIcon: CGFloat = 64
    static let circleLabel: CGFloat = 80
    static let providerLogo: CGFloat = 32
    static let miniAppIcon: CGFloat = 40
    static let miniAppMaxWidth: CGFloat = 128
    static let miniAppMinHeight: CGFloat = 90
    static let storyTitleBlock = CGSize(width: 137, height: 32)
    static let topbarBlur = CGSize(width: 414, height: 140)
}

extension Color {
    static let streamBackground = Color(hex: 0x111111)
    static let streamBackgroundSoft = Color(hex: 0x111111, alpha: 0.8)
    static let streamSurface = Color(hex: 0x2C2C2E)
    static let streamSecondaryText = Color(hex: 0xADADAD)
    static let streamBottomNavText = Color(hex: 0xAEAEB2)
    static let streamAccent = Color(hex: 0xE8301C)
    static let streamAccentBright = Color(hex: 0xFF7A18)
    static let streamLive = Color(hex: 0xDC1F26)
    static let streamTrack = Color(hex: 0xFFFFFF, alpha: 0.4)
    static let streamMiniAppFill = Color(hex: 0x282828)
    static let streamMiniAppStroke = Color(hex: 0x434343)

    init(hex: UInt32, alpha: Double = 1) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: alpha
        )
    }
}

/// One-for-one ports of the Android gradient drawables used by Home.
extension LinearGradient {
    /// `bg_button_primary`
    static let streamPrimaryButton = LinearGradient(
        colors: [.streamAccentBright, .streamAccent],
        startPoint: .leading,
        endPoint: .trailing
    )

    /// `bg_carousel_top`
    static let streamCarouselTop = LinearGradient(
        colors: [.streamBackground, .clear],
        startPoint: .top,
        endPoint: .bottom
    )

    /// `bg_carousel`
    static let streamCarouselBottom = LinearGradient(
        colors: [.clear, .streamBackground],
        startPoint: .top,
        endPoint: .bottom
    )

    /// `bg_topbar`
    static let streamTopbar = LinearGradient(
        colors: [.streamBackground, .clear],
        startPoint: .top,
        endPoint: .bottom
    )

    /// `bg_topbar_behind`
    static let streamTopbarBehind = LinearGradient(
        colors: [.streamBackground, .streamBackgroundSoft],
        startPoint: .top,
        endPoint: .bottom
    )

    /// `bg_watching_shadow`
    static let streamWatchingShadow = LinearGradient(
        colors: [.clear, .streamBackground],
        startPoint: .top,
        endPoint: .bottom
    )

    /// `divider_story_top`
    static let streamStoryDividerTop = LinearGradient(
        stops: [
            .init(color: Color(hex: 0xE8301C, alpha: 0.1), location: 0),
            .init(color: .streamAccent, location: 0.8),
            .init(color: Color(hex: 0xE8301C, alpha: 0), location: 1),
        ],
        startPoint: .leading,
        endPoint: .trailing
    )

    /// `divider_story_bottom`
    static let streamStoryDividerBottom = LinearGradient(
        stops: [
            .init(color: .black, location: 0),
            .init(color: .streamAccentBright, location: 0.3),
            .init(color: .black, location: 1),
        ],
        startPoint: .leading,
        endPoint: .trailing
    )

    /// `bg_mini_app_content` border
    static let streamMiniAppBorder = LinearGradient(
        colors: [.streamMiniAppStroke, .streamMiniAppFill],
        startPoint: .leading,
        endPoint: .trailing
    )
}

extension Font {
    static func streamRegular(_ size: CGFloat) -> Font {
        .custom("SVN-Gilroy", size: size)
    }

    static func streamMedium(_ size: CGFloat) -> Font {
        .custom("SVN-GilroyMedium", size: size)
    }

    static func streamSemiBold(_ size: CGFloat) -> Font {
        .custom("SVN-GilroySemiBold", size: size)
    }

    static func streamBold(_ size: CGFloat) -> Font {
        .custom("SVN-GilroyBold", size: size)
    }
}

/// `bg_thumbnail_live` plus the `Text.LabelText` appearance.
struct StreamBadge: View {
    let text: String
    var color: Color = .black.opacity(0.7)

    var body: some View {
        Text(text)
            .font(.streamBold(10))
            .foregroundStyle(.white)
            .padding(.horizontal, 6)
            .padding(.bottom, 2)
            .frame(minHeight: 18)
            .background(
                color,
                in: RoundedRectangle(cornerRadius: StreamMetrics.radiusNormal, style: .continuous)
            )
    }
}

/// `SaymeeLabel` over the `bg_tag_saymee` artwork, shown for the deterministic exclusive fixture flag.
struct StreamExclusiveBadge: View {
    var body: some View {
        Text("StreamTV".uppercased())
            .font(.streamBold(8))
            .tracking(-0.08)
            .foregroundStyle(.white)
            .padding(.horizontal, 4)
            .padding(.vertical, 2)
            .background { Image("bg_tag_saymee").resizable() }
    }
}
