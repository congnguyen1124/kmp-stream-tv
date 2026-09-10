import SwiftUI

enum HomeChromeMetrics {
    static let brandHeight: CGFloat = 48
    static let categoryHeight: CGFloat = 40
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
        .background {
            ZStack {
                LinearGradient(
                    colors: [.black.opacity(0.94), .black.opacity(0.58), .clear],
                    startPoint: .top,
                    endPoint: .bottom
                )
                Color.black.opacity(scrimOpacity)
            }
            .ignoresSafeArea(edges: .top)
        }
    }

    private var brandBar: some View {
        HStack(spacing: 10) {
            Button {
                selectedCategory = .home
            } label: {
                Image("StreamTvLogo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 112, height: 32, alignment: .leading)
                    .accessibilityLabel("StreamTV")
            }
            .buttonStyle(.plain)

            Spacer(minLength: 8)

            actionButton(.search, systemImage: "magnifyingglass")
            actionButton(.notifications, systemImage: "bell")
            actionButton(.profile, systemImage: "person.crop.circle")
        }
        .padding(.horizontal, StreamMetrics.contentInset)
        .frame(height: HomeChromeMetrics.brandHeight)
    }

    private var categoryBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 22) {
                ForEach(HomeCategory.allCases) { category in
                    Button {
                        selectedCategory = category
                    } label: {
                        VStack(spacing: 5) {
                            HStack(spacing: 4) {
                                Text(category.title)
                                if category == .more {
                                    Image(systemName: "chevron.down")
                                        .font(.caption2.bold())
                                }
                            }
                            .font(.subheadline.weight(.semibold))

                            Capsule()
                                .fill(category == selectedCategory ? .white : .clear)
                                .frame(height: 2)
                        }
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(category == selectedCategory ? .white : .streamSecondaryText)
                }
            }
            .padding(.horizontal, StreamMetrics.contentInset)
        }
        .frame(height: HomeChromeMetrics.categoryHeight)
    }

    private var categoryMenu: some View {
        HStack {
            Menu {
                ForEach(HomeCategory.allCases) { category in
                    Button(category.title) {
                        selectedCategory = category
                    }
                }
            } label: {
                HStack(spacing: 6) {
                    Text(selectedCategory.title)
                        .font(.headline)
                    Image(systemName: "chevron.down")
                        .font(.caption.bold())
                }
                .foregroundStyle(.white)
            }

            Spacer()
        }
        .padding(.horizontal, StreamMetrics.contentInset)
        .frame(height: HomeChromeMetrics.categoryHeight)
    }

    private func actionButton(_ action: HomeChromeAction, systemImage: String) -> some View {
        Button {
            onAction(action)
        } label: {
            Image(systemName: systemImage)
                .font(.system(size: 18, weight: .semibold))
                .frame(width: 32, height: 32)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .foregroundStyle(.white)
        .accessibilityLabel(action.title)
    }
}
