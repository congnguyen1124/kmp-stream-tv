import SwiftUI

enum BottomNavigationMetrics {
    static let iconSize: CGFloat = 28
    static let itemWidth: CGFloat = 64
    static let itemHeight: CGFloat = 48
    static let contentPadding: CGFloat = 6
    static let bottomInset: CGFloat = 10
    /// Room the destinations reserve so the floating bar never covers their last row.
    static let totalHeight = itemHeight + 2 * contentPadding + bottomInset
}

/// Icon-only floating bar. It keeps the Android icon set and selected/unselected tints while
/// adopting the Liquid Glass material, so the feed stays visible underneath.
struct BottomNavigationBar: View {
    @Binding var selection: AppDestination

    var body: some View {
        HStack(spacing: 0) {
            ForEach(AppDestination.allCases) { destination in
                item(destination)
            }
        }
        .padding(BottomNavigationMetrics.contentPadding)
        .modifier(LiquidGlassCapsule())
        .padding(.bottom, BottomNavigationMetrics.bottomInset)
    }

    private func item(_ destination: AppDestination) -> some View {
        let isSelected = selection == destination
        return Button {
            selection = destination
        } label: {
            Image(isSelected ? destination.selectedIcon : destination.unselectedIcon)
                .renderingMode(.template)
                .resizable()
                .frame(
                    width: BottomNavigationMetrics.iconSize,
                    height: BottomNavigationMetrics.iconSize
                )
                .foregroundStyle(isSelected ? Color.white : Color.streamBottomNavText)
                .frame(
                    width: BottomNavigationMetrics.itemWidth,
                    height: BottomNavigationMetrics.itemHeight
                )
                .background {
                    if isSelected {
                        Capsule().fill(Color.white.opacity(0.16))
                    }
                }
                .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(destination.title)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

/// Liquid Glass where the OS provides it, with a material capsule on earlier releases.
private struct LiquidGlassCapsule: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content.glassEffect(.regular.interactive(), in: Capsule())
        } else {
            content
                .background(.ultraThinMaterial, in: Capsule())
                .overlay {
                    Capsule().strokeBorder(Color.white.opacity(0.12), lineWidth: 1)
                }
        }
    }
}
