import SwiftUI

enum HomeChromeMetrics {
    static let brandHeight: CGFloat = 48
    static let categoryHeight: CGFloat = 36
    static let totalHeight = brandHeight + categoryHeight
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
        .background(topbarBackground.ignoresSafeArea(edges: .top))
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
                actionButton(.profile, image: "ic_user_circle")
            }
        }
        .padding(.horizontal, StreamMetrics.contentInset)
        .frame(height: HomeChromeMetrics.brandHeight)
    }

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

    private var topbarBackground: some View {
        ZStack {
            LinearGradient(
                colors: [.streamBackground, Color.streamBackground.opacity(0.8)],
                startPoint: .top,
                endPoint: .bottom
            )
            .opacity(scrimOpacity)

            Image("bg_topbar_blur")
                .resizable()
                .scaledToFill()

            LinearGradient(
                colors: [.streamBackground, .clear],
                startPoint: .top,
                endPoint: .bottom
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
                .frame(width: 24, height: 24)
                .frame(width: 32, height: 32)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .foregroundStyle(.white)
        .accessibilityLabel(action.title)
    }
}
