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
    ///
    /// `addMenu` gives the More chip the picker and the chevron instead of a category selection, so
    /// it stays a launcher and never becomes the selected content category.
    private var categoryBar: some View {
        HStack(spacing: 0) {
            ForEach(HomeCategory.allCases) { category in
                Button {
                    if category == .more {
                        onCategoryPicker()
                    } else {
                        selectedCategory = category
                    }
                } label: {
                    HStack(spacing: 2) {
                        Text(category.title)
                            .font(.streamSemiBold(16))
                        if category == .more {
                            Image("ic_arrow_down")
                                .renderingMode(.template)
                                .resizable()
                                .frame(width: 16, height: 16)
                        }
                    }
                    .foregroundStyle(.white)
                    .padding(.horizontal, 12)
                    .frame(height: HomeChromeMetrics.categoryHeight)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(category == selectedCategory ? .isSelected : [])
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
    ///
    /// `fragment_home_tab.xml` pins all three to the top of the window, so the stack has to bleed
    /// through the top safe area here as well. Clipping it to the bar height instead leaves the
    /// status bar / Dynamic Island strip as a flat uncoloured band above the header, and lets the
    /// feed scroll past it unscrimmed.
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
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .clipped()
        .ignoresSafeArea(edges: .top)
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
