import Shared
import SwiftUI

/// Side padding and card geometry resolved exactly like the Android `CarouseView` constructor:
/// the source thumbnail size is kept while it fits, then shrinks proportionally so the side
/// padding never drops below `carousel_padding_horizontal_min`.
struct HomeCarouselMetrics: Sendable {
    let viewportWidth: CGFloat
    let thumbWidth: CGFloat
    let thumbHeight: CGFloat
    let sidePadding: CGFloat

    init(available: CGFloat, thumb: CGSize) {
        viewportWidth = available
        let naturalPadding = (available - thumb.width) / 2
        guard naturalPadding < StreamMetrics.carouselMinSidePadding else {
            sidePadding = naturalPadding
            thumbWidth = thumb.width
            thumbHeight = thumb.height
            return
        }
        let width = max(0, available - 2 * StreamMetrics.carouselMinSidePadding)
        sidePadding = StreamMetrics.carouselMinSidePadding
        thumbWidth = width
        thumbHeight = thumb.width > 0 ? width * thumb.height / thumb.width : 0
    }

    /// The Android page transformer scale for a card whose centre sits `midX` from the viewport edge.
    func scale(forCardCentre midX: CGFloat) -> CGFloat {
        guard thumbWidth > 0 else { return HomeCarousel.maxScale }
        let position = min(abs(midX - viewportWidth / 2) / thumbWidth, 1)
        return HomeCarousel.minScale + (1 - position) * (HomeCarousel.maxScale - HomeCarousel.minScale)
    }
}

/// SwiftUI port of `CarouseView`: looping pages, unclipped neighbours, zero page pitch beyond the
/// card width and the same 85 %–100 % scale ramp used by the Android page transformer.
struct HomeCarousel: View {
    let items: [HomeContentUiModel]
    let thumbSize: CGSize
    let onSelect: (HomeContentUiModel) -> Void
    var onActiveIndexChanged: ((Int) -> Void)?

    @State private var activePage: Int?

    static let minScale: CGFloat = 0.85
    static let maxScale: CGFloat = 1
    fileprivate static let viewportSpace = "home-carousel-viewport"
    private static let pageLoop = 1000

    var body: some View {
        GeometryReader { geometry in
            let metrics = HomeCarouselMetrics(available: geometry.size.width, thumb: thumbSize)

            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: 0) {
                    ForEach(0..<pageCount, id: \.self) { page in
                        card(at: page, metrics: metrics)
                    }
                }
                .scrollTargetLayout()
            }
            .contentMargins(.horizontal, metrics.sidePadding, for: .scrollContent)
            .scrollTargetBehavior(.viewAligned(limitBehavior: .always))
            .scrollPosition(id: $activePage)
            .scrollClipDisabled()
            .coordinateSpace(.named(Self.viewportSpace))
        }
        .onAppear(perform: positionAtLoopCentre)
        .onChange(of: activePage) { _, page in
            guard let page, !items.isEmpty else { return }
            onActiveIndexChanged?(page % items.count)
        }
    }

    private var pageCount: Int { items.isEmpty ? 0 : items.count * Self.pageLoop }

    private func card(at page: Int, metrics: HomeCarouselMetrics) -> some View {
        let item = items[page % items.count]
        return Button {
            onSelect(item)
        } label: {
            RemoteArtwork(url: item.thumbnailUrl, placeholder: .branded)
                .frame(width: metrics.thumbWidth, height: metrics.thumbHeight)
                .clipShape(
                    RoundedRectangle(cornerRadius: StreamMetrics.thumbnailCorner, style: .continuous)
                )
                .overlay(alignment: .topLeading) {
                    if item.isDummyExclusive {
                        StreamExclusiveBadge().padding(6)
                    }
                }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(item.title)
        .visualEffect { content, proxy in
            content.scaleEffect(
                metrics.scale(forCardCentre: proxy.frame(in: .named(Self.viewportSpace)).midX)
            )
        }
    }

    private func positionAtLoopCentre() {
        guard activePage == nil, !items.isEmpty else { return }
        activePage = items.count * Self.pageLoop / 2
    }
}
