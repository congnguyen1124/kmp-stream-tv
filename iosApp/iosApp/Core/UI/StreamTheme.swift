import SwiftUI

enum StreamMetrics {
    static let contentInset: CGFloat = 16
    static let sectionSpacing: CGFloat = 24
    static let cardSpacing: CGFloat = 8
    static let cornerRadius: CGFloat = 12
}

extension Color {
    static let streamBackground = Color(red: 0.067, green: 0.067, blue: 0.067)
    static let streamSurface = Color(red: 0.173, green: 0.173, blue: 0.18)
    static let streamSecondaryText = Color(red: 0.678, green: 0.678, blue: 0.678)
    static let streamAccent = Color(red: 0.392, green: 0.098, blue: 0.855)
    static let streamAccentBright = Color(red: 0.706, green: 0.275, blue: 0.941)
    static let streamLive = Color(red: 0.863, green: 0.122, blue: 0.149)
}

struct StreamBadge: View {
    let text: String
    var color: Color = .black.opacity(0.68)

    var body: some View {
        Text(text)
            .font(.caption2.weight(.bold))
            .foregroundStyle(.white)
            .padding(.horizontal, 8)
            .padding(.vertical, 5)
            .background(color, in: RoundedRectangle(cornerRadius: 5, style: .continuous))
    }
}
