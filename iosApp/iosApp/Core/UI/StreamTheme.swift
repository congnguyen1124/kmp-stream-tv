import SwiftUI

enum StreamMetrics {
    static let contentInset: CGFloat = 16
    static let cardSpacing: CGFloat = 8
    static let thumbnailCorner: CGFloat = 8
    static let storyCorner: CGFloat = 12
}

extension Color {
    static let streamBackground = Color(hex: 0x111111)
    static let streamSurface = Color(hex: 0x2C2C2E)
    static let streamSecondaryText = Color(hex: 0xADADAD)
    static let streamBottomNavText = Color(hex: 0xAEAEB2)
    static let streamAccent = Color(hex: 0x6419DA)
    static let streamAccentBright = Color(hex: 0xB446F0)
    static let streamLive = Color(hex: 0xDC1F26)

    init(hex: UInt32, alpha: Double = 1) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: alpha
        )
    }
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
            .background(color, in: RoundedRectangle(cornerRadius: 4, style: .continuous))
    }
}
