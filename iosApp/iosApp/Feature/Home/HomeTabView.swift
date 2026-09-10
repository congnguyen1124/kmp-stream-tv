import SwiftUI

struct HomeTabView: View {
    @StateObject private var store = HomeStore()
    @State private var selectedCategory = HomeCategory.home
    @State private var toastMessage: String?
    @State private var isCategoryPickerPresented = false
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
                onAction: { showToast($0.message) },
                onCategoryPicker: { isCategoryPickerPresented = true }
            )

            if isCategoryPickerPresented {
                categoryPicker
            }

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
    }

    /// `ivTopBarBehind.alpha` tracks the feed offset over `highlight_topbar_offset`.
    private var toolbarScrimOpacity: Double {
        min(max(Double(feedOffset / StreamMetrics.highlightTopbarOffset), 0), 1)
    }

    private var categoryPicker: some View {
        ZStack {
            Color.black.opacity(0.55)
                .ignoresSafeArea()
                .onTapGesture { isCategoryPickerPresented = false }

            VStack(alignment: .leading, spacing: 0) {
                Text("Browse categories")
                    .font(.streamBold(20))
                    .foregroundStyle(.white)
                    .padding(24)

                ForEach(HomeCategory.allCases) { category in
                    Button {
                        selectedCategory = category
                        isCategoryPickerPresented = false
                    } label: {
                        Text(category.title)
                            .font(.streamRegular(16))
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .frame(height: 48)
                            .padding(.horizontal, 24)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.bottom, 8)
            .frame(maxWidth: 320)
            .background(Color.streamSurface)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .padding(32)
        }
        .transition(.opacity)
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
