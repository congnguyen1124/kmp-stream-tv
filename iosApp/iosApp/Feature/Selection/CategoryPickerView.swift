import SwiftUI

/// Geometry and motion constants ported from `SliderLayoutManager` and the selection dimens.
enum SelectionMetrics {
    /// `selected_category_size`
    static let rowHeight: CGFloat = 50
    /// `selection_top_bar_height`
    static let topBarHeight: CGFloat = 56
    /// `selection_bottom_bar_height`
    static let bottomBarHeight: CGFloat = 96

    // `SliderLayoutManager.transformChildren`
    static let maxScale: CGFloat = 1.2
    static let minScale: CGFloat = 0.55
    static let scaleFalloff: CGFloat = 0.8
    static let minAlpha: CGFloat = 0.2
    static let alphaFalloff: CGFloat = 0.8
}

/// `selection_highlight`
private extension Color {
    static let selectionHighlight = Color(hex: 0xFFFFFF, alpha: 0.1)
}

/// One row of the wheel. `SelectionItem` on Android carries an optional image; the Home categories
/// only ever supply a title, so the image branch of `item_selection.xml` has no iOS caller yet.
struct SelectionItem: Identifiable, Equatable {
    let id: String
    let title: String
}

/// Full-screen category wheel, the port of `ItemSelectionDialogFragment`.
///
/// `SliderLayoutManager` scales and fades every row by its distance from the vertical centre and
/// `CustomLinearSnapHelper` settles the nearest row there. SwiftUI gets the same two behaviours
/// from `.scrollTargetBehavior(.viewAligned)` over content margins that leave exactly one row's
/// height inside the scroll target region, plus a per-row transform driven by the live frame.
struct CategoryPickerView: View {
    let items: [SelectionItem]
    let selectedId: String?
    let onSelect: (SelectionItem) -> Void

    @Environment(\.dismiss) private var dismiss
    /// The row the wheel has settled on — `SliderLayoutManager.selectedPosition`.
    @State private var centeredId: String?

    private static let scrollSpace = "category-picker"

    var body: some View {
        GeometryReader { geometry in
            let verticalInset = max((geometry.size.height - SelectionMetrics.rowHeight) / 2, 0)

            ZStack {
                Color.streamBackground.ignoresSafeArea()

                // `selectedCategoryBackground`: the band the centred row settles into.
                Color.selectionHighlight
                    .frame(height: SelectionMetrics.rowHeight)
                    .frame(maxHeight: .infinity, alignment: .center)
                    .allowsHitTesting(false)

                wheel(host: geometry.size, verticalInset: verticalInset)

                topBar
                bottomBar
            }
        }
        .preferredColorScheme(.dark)
    }

    // MARK: - Wheel

    private func wheel(host: CGSize, verticalInset: CGFloat) -> some View {
        ScrollView(.vertical) {
            LazyVStack(spacing: 0) {
                ForEach(items) { item in
                    row(item, host: host)
                }
            }
            .scrollTargetLayout()
        }
        .coordinateSpace(name: Self.scrollSpace)
        .contentMargins(.vertical, verticalInset, for: .scrollContent)
        .scrollIndicators(.hidden)
        .scrollTargetBehavior(.viewAligned)
        .scrollPosition(id: $centeredId, anchor: .center)
        .onAppear {
            // `scrollToPosition(selectedIndex)` on the first layout pass.
            centeredId = selectedId ?? items.first?.id
        }
    }

    private func row(_ item: SelectionItem, host: CGSize) -> some View {
        GeometryReader { proxy in
            let transform = transform(for: proxy, host: host)

            Button {
                tap(item)
            } label: {
                Text(item.title)
                    .font(.streamBold(18))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                    .padding(.horizontal, StreamMetrics.contentInset)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .scaleEffect(transform.scale)
            .opacity(transform.opacity)
            .accessibilityAddTraits(item.id == centeredId ? .isSelected : [])
        }
        .frame(height: SelectionMetrics.rowHeight)
        .id(item.id)
    }

    /// `SliderLayoutManager.transformChildren`, including its division by the list *width*.
    private func transform(for proxy: GeometryProxy, host: CGSize) -> (scale: CGFloat, opacity: Double) {
        guard host.width > 0 else { return (1, 1) }
        let frame = proxy.frame(in: .named(Self.scrollSpace))
        let factor = sqrt(abs(host.height / 2 - frame.midY) / host.width)
        let scale = min(
            max(SelectionMetrics.maxScale - factor * SelectionMetrics.scaleFalloff, SelectionMetrics.minScale),
            SelectionMetrics.maxScale
        )
        let opacity = min(max(1 - factor * SelectionMetrics.alphaFalloff, SelectionMetrics.minAlpha), 1)
        return (scale, Double(opacity))
    }

    // MARK: - Chrome

    /// `topBar`: Back and the picker title, both of which dismiss without changing the category.
    private var topBar: some View {
        HStack(spacing: StreamMetrics.cardSpacing) {
            Button(action: dismiss.callAsFunction) {
                Image("ic_arrow_back")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .padding(4)
                    .frame(width: 44, height: 44)
                    .foregroundStyle(.white)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Back")

            Button(action: dismiss.callAsFunction) {
                Text("Browse categories")
                    .font(.streamBold(18))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, StreamMetrics.cardSpacing)
        .frame(height: SelectionMetrics.topBarHeight)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(alignment: .top) {
            Color.streamBackground
                .frame(height: SelectionMetrics.topBarHeight)
                .ignoresSafeArea(edges: .top)
        }
    }

    /// `bottomBackground` plus `continueAction`, which commits whatever row is centred.
    private var bottomBar: some View {
        Button {
            guard let item = items.first(where: { $0.id == centeredId }) else { return }
            commit(item)
        } label: {
            Text("Continue")
                .font(.streamSemiBold(16))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: StreamMetrics.buttonHeight)
                .background(
                    LinearGradient.streamPrimaryButton,
                    in: RoundedRectangle(cornerRadius: StreamMetrics.buttonRadius, style: .continuous)
                )
        }
        .buttonStyle(.plain)
        .padding(.horizontal, 24)
        .padding(.bottom, 24)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
        .background(alignment: .bottom) {
            Color.streamBackground
                .frame(height: SelectionMetrics.bottomBarHeight)
                .ignoresSafeArea(edges: .bottom)
        }
    }

    // MARK: - Selection

    /// The two-step tap: an off-centre row travels to the centre, and only the centred row commits.
    private func tap(_ item: SelectionItem) {
        if item.id == centeredId {
            commit(item)
        } else {
            withAnimation { centeredId = item.id }
        }
    }

    private func commit(_ item: SelectionItem) {
        dismiss()
        onSelect(item)
    }
}
