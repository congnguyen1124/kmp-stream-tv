import SwiftUI

struct HomeTabView: View {
    @StateObject private var store = HomeStore()
    @State private var selectedCategory = HomeCategory.home
    @State private var activeAction: HomeChromeAction?
    @State private var feedOffset: CGFloat = 0

    var body: some View {
        ZStack(alignment: .top) {
            Color.streamBackground.ignoresSafeArea()

            HomeView(
                store: store,
                contentTopInset: HomeChromeMetrics.totalHeight + 12,
                onScrollOffsetChanged: { feedOffset = $0 }
            )
            .opacity(selectedCategory == .home ? 1 : 0)
            .allowsHitTesting(selectedCategory == .home)
            .accessibilityHidden(selectedCategory != .home)

            if selectedCategory != .home {
                PlaceholderView(
                    title: selectedCategory.title,
                    message: selectedCategory.placeholderMessage,
                    systemImage: selectedCategory.systemImage
                )
                .padding(.top, HomeChromeMetrics.totalHeight)
            }

            HomeChromeView(
                selectedCategory: $selectedCategory,
                scrimOpacity: selectedCategory == .home ? toolbarScrimOpacity : 0.82,
                onAction: { activeAction = $0 }
            )
        }
        .alert(item: $activeAction) { action in
            Alert(
                title: Text(action.title),
                message: Text(action.message),
                dismissButton: .default(Text("OK"))
            )
        }
    }

    private var toolbarScrimOpacity: Double {
        min(max(Double(feedOffset / 250), 0), 0.82)
    }
}
