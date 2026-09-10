import SwiftUI

enum HomeChromeMetrics {
    static let brandHeight: CGFloat = 48
    static let categoryHeight: CGFloat = 36
    static let totalHeight = brandHeight + categoryHeight
    static let iconSize: CGFloat = 32
    static let iconPadding: CGFloat = 4
}

enum HomeChromeAction: String, Identifiable {
    case search
    case notifications
    case profile

    var id: String { rawValue }

    var title: String {
        switch self {
        case .search: "Search"
        case .notifications: "Notifications"
        case .profile: "Profile"
        }
    }

    var message: String {
        "\(title) will be connected in a later screen."
    }
}

/// `fragment_home_tab.xml`: brand bar, category bar and the two stacked top-bar backgrounds.
struct HomeChromeView: View {
    @Binding var selectedCategory: HomeCategory
    let scrimOpacity: Double
    let onAction: (HomeChromeAction) -> Void
    let onCategoryPicker: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            brandBar

            if selectedCategory == .home {
                categoryBar
            } else {
                categoryMenu
            }
        }
        .frame(maxWidth: .infinity)
        .background(alignment: .top) { topbarBackground }
    }

    private var brandBar: some View {
        HStack(spacing: 0) {
            Button {
                selectedCategory = .home
            } label: {
                Image("img_logo_app")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 104, height: 32, alignment: .leading)
                    .accessibilityLabel("StreamTV")
            }
            .buttonStyle(.plain)

            Spacer(minLength: 0)

            HStack(spacing: 8) {
                actionButton(.search, image: "ic_search")
                actionButton(.notifications, image: "ic_notification_normal")
                    .padding(.horizontal, HomeChromeMetrics.iconPadding)
                actionButton(.profile, image: "ic_user_circle")
            }
        }
        .padding(.horizontal, StreamMetrics.contentInset)
        .frame(height: HomeChromeMetrics.brandHeight)
    }

    /// `HomeCategoriesBar` keeps every category left aligned while the row fits the bar width.
    private var categoryBar: some View {
        HStack(spacing: 0) {
            ForEach(HomeCategory.allCases) { category in
                Button {
                    selectedCategory = category
                } label: {
                    Text(category.title)
                        .font(.streamSemiBold(16))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 12)
                        .frame(height: HomeChromeMetrics.categoryHeight)
                }
                .buttonStyle(.plain)
            }

            Spacer(minLength: 0)
        }
        .padding(.horizontal, 6)
        .frame(height: HomeChromeMetrics.categoryHeight)
    }

    private var categoryMenu: some View {
        HStack {
            Button(action: onCategoryPicker) {
                HStack(spacing: 4) {
                    Text(selectedCategory.title)
                        .font(.streamBold(18))
                    Image("ic_chevron_down")
                        .resizable()
                        .frame(width: 16, height: 16)
                }
                .foregroundStyle(.white)
            }
            .buttonStyle(.plain)

            Spacer()
        }
        .padding(.horizontal, StreamMetrics.contentInset)
        .frame(height: HomeChromeMetrics.categoryHeight)
    }

    /// `ivTopBarBehind` fades in with the feed, then `bg_topbar` and the unscaled blur artwork.
    private var topbarBackground: some View {
        ZStack(alignment: .topLeading) {
            LinearGradient.streamTopbarBehind
                .opacity(scrimOpacity)

            LinearGradient.streamTopbar

            Image("bg_topbar_blur")
                .resizable()
                .frame(
                    width: StreamCardSize.topbarBlur.width,
                    height: StreamCardSize.topbarBlur.height
                )
        }
        .frame(height: HomeChromeMetrics.totalHeight)
        .clipped()
    }

    private func actionButton(_ action: HomeChromeAction, image: String) -> some View {
        Button {
            onAction(action)
        } label: {
            Image(image)
                .resizable()
                .frame(
                    width: HomeChromeMetrics.iconSize - 2 * HomeChromeMetrics.iconPadding,
                    height: HomeChromeMetrics.iconSize - 2 * HomeChromeMetrics.iconPadding
                )
                .frame(width: HomeChromeMetrics.iconSize, height: HomeChromeMetrics.iconSize)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .foregroundStyle(.white)
        .accessibilityLabel(action.title)
    }
}
