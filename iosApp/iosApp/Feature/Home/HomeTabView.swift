import SwiftUI

struct HomeTabView: View {
    @StateObject private var store = HomeStore()
    @State private var selectedCategory = HomeCategory.home
    @State private var toastMessage: String?
    @State private var isCategoryPickerPresented = false
    @State private var isProfilePresented = false
    @State private var feedOffset: CGFloat = 0

    var body: some View {
        ZStack(alignment: .top) {
            Color.streamBackground.ignoresSafeArea()

            HomeView(
                store: store,
                contentTopInset: HomeChromeMetrics.totalHeight,
                onScrollOffsetChanged: { feedOffset = $0 }
            )
            .opacity(selectedCategory == .home ? 1 : 0)
            .allowsHitTesting(selectedCategory == .home)
            .accessibilityHidden(selectedCategory != .home)

            if selectedCategory != .home {
                PlaceholderView(
                    title: selectedCategory.title,
                    message: selectedCategory.placeholderMessage
                )
                .padding(.top, HomeChromeMetrics.totalHeight)
            }

            HomeChromeView(
                selectedCategory: $selectedCategory,
                scrimOpacity: toolbarScrimOpacity,
                onAction: handle,
                onCategoryPicker: { isCategoryPickerPresented = true }
            )

            if let toastMessage {
                Text(toastMessage)
                    .font(.streamRegular(14))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 16)
                    .frame(minHeight: 48)
                    .background(Color.black.opacity(0.88))
                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                    .padding(.horizontal, 24)
                    .frame(maxHeight: .infinity, alignment: .bottom)
                    .padding(.bottom, 32)
                    .transition(.opacity)
            }
        }
        .environment(\.homeToast, showToast)
        // Both covers keep this view — and so the feed, its scroll offset and the selected
        // category — alive underneath, the way the Android destinations stay retained behind the
        // dialog and the profile Activity.
        .fullScreenCover(isPresented: $isCategoryPickerPresented) {
            CategoryPickerView(
                items: HomeCategory.selectable.map { SelectionItem(id: $0.id, title: $0.title) },
                selectedId: selectedCategory.id,
                onSelect: { item in
                    guard let category = HomeCategory(rawValue: item.id) else { return }
                    selectedCategory = category
                }
            )
        }
        .fullScreenCover(isPresented: $isProfilePresented) {
            UserProfileView()
        }
    }

    /// `ivTopBarBehind.alpha` tracks the feed offset over `highlight_topbar_offset`.
    private var toolbarScrimOpacity: Double {
        min(max(Double(feedOffset / StreamMetrics.highlightTopbarOffset), 0), 1)
    }

    /// `btnProfile` opens the personal-profile screen; search and notifications have no destination
    /// in the shared contract yet.
    private func handle(_ action: HomeChromeAction) {
        switch action {
        case .profile:
            isProfilePresented = true
        case .search, .notifications:
            showToast(action.message)
        }
    }

    private func showToast(_ message: String) {
        withAnimation { toastMessage = message }
        Task {
            try? await Task.sleep(nanoseconds: 2_000_000_000)
            guard toastMessage == message else { return }
            withAnimation { toastMessage = nil }
        }
    }
}
