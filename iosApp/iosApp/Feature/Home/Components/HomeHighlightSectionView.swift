import Shared
import SwiftUI

/// `layout_highlight_wide_view.xml`
struct HighlightWideSectionView: View {
    let section: HomeSectionUiModel
    let onSelect: (HomeContentUiModel) -> Void

    var body: some View {
        ZStack {
            if let backgroundUrl = section.backgroundUrl, !backgroundUrl.isEmpty {
                RemoteArtwork(url: backgroundUrl)
            }

            HomeCarousel(
                items: section.items,
                thumbSize: StreamCardSize.highlightWide,
                onSelect: onSelect
            )
            .frame(height: StreamCardSize.highlightWide.height)
            .padding(.vertical, StreamMetrics.verticalListDivider)
        }
        .frame(height: StreamCardSize.highlightWide.height + 2 * StreamMetrics.verticalListDivider)
    }
}

/// `layout_highlight_tall_view.xml` bound by `LayoutHighlightTallView`.
struct HighlightTallSectionView: View {
    let section: HomeSectionUiModel
    /// `LayoutHighlightTallViewHolder` widens the carousel top margin for the first feed row.
    let isTopSection: Bool
    let onSelect: (HomeContentUiModel) -> Void

    @Environment(\.homeToast) private var homeToast
    @State private var activeIndex = 0
    @State private var followedItemIDs: Set<String> = []

    /// `margin_4x` between the carousel and the actions, `margin_x` below them.
    private static let actionBarTopMargin: CGFloat = 32
    private static let bottomSpacing: CGFloat = 20

    var body: some View {
        ZStack(alignment: .top) {
            blurredArtwork
            carouselGradients
            content
        }
        .frame(height: carouselTopMargin + StreamCardSize.highlightTall.height + trailingHeight)
        .clipped()
    }

    private var carouselTopMargin: CGFloat {
        isTopSection
            ? StreamMetrics.verticalListDivider + StreamMetrics.homeContentPaddingTop
            : StreamMetrics.verticalListDivider
    }

    private var trailingHeight: CGFloat {
        Self.actionBarTopMargin + StreamMetrics.largeIconButtonSize + Self.bottomSpacing
    }

    private var activeItem: HomeContentUiModel? {
        section.items.indices.contains(activeIndex) ? section.items[activeIndex] : section.items.first
    }

    private var isFollowingActiveItem: Bool {
        guard let activeItem else { return false }
        return followedItemIDs.contains(activeItem.id)
    }

    /// `ivCarouselBlur` with the reference `BlurTransformation(5, 25)` treatment.
    @ViewBuilder
    private var blurredArtwork: some View {
        if let thumbnailUrl = activeItem?.thumbnailUrl ?? section.backgroundUrl, !thumbnailUrl.isEmpty {
            RemoteArtwork(url: thumbnailUrl)
                .blur(radius: 40, opaque: true)
                .opacity(0.8)
        }
    }

    /// `bg_carousel_top` above the carousel and `bg_carousel` below it.
    private var carouselGradients: some View {
        VStack(spacing: 0) {
            LinearGradient.streamCarouselTop.frame(height: carouselTopMargin)
            Spacer(minLength: 0)
            LinearGradient.streamCarouselBottom.frame(height: trailingHeight)
        }
    }

    private var content: some View {
        VStack(spacing: 0) {
            HomeCarousel(
                items: section.items,
                thumbSize: StreamCardSize.highlightTall,
                onSelect: onSelect,
                onActiveIndexChanged: { activeIndex = $0 }
            )
            .frame(height: StreamCardSize.highlightTall.height)
            .padding(.top, carouselTopMargin)

            actionBar
                .padding(.top, Self.actionBarTopMargin)

            Color.clear.frame(height: Self.bottomSpacing)
        }
    }

    private var actionBar: some View {
        HStack(spacing: 0) {
            iconAction(
                title: "Watch later",
                icon: isFollowingActiveItem ? "ic_playlist_check" : "ic_playlist_plus",
                action: toggleWatchLater
            )

            watchNowButton
                .padding(.horizontal, 4)

            iconAction(title: "Information", icon: "ic_info_circle") {
                if let activeItem {
                    onSelect(activeItem)
                }
            }
        }
        .frame(height: StreamMetrics.largeIconButtonSize)
    }

    private var watchNowButton: some View {
        Button {
            if let activeItem {
                onSelect(activeItem)
            }
        } label: {
            HStack(spacing: 4) {
                Image("ic_play_round")
                    .resizable()
                    .frame(width: StreamMetrics.iconButtonSize, height: StreamMetrics.iconButtonSize)
                Text("Watch now")
                    .font(.streamSemiBold(16))
            }
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .frame(height: StreamMetrics.buttonHeight)
            .background(LinearGradient.streamPrimaryButton)
            .clipShape(RoundedRectangle(cornerRadius: StreamMetrics.buttonRadius, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private func iconAction(
        title: String,
        icon: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(icon)
                    .resizable()
                    .frame(width: StreamMetrics.iconButtonSize, height: StreamMetrics.iconButtonSize)
                Text(title)
                    .font(.streamSemiBold(12))
                    .foregroundStyle(Color.streamSecondaryText)
                    .lineLimit(1)
            }
            .padding(.horizontal, 8)
            .frame(
                minWidth: StreamMetrics.largeIconButtonSize,
                minHeight: StreamMetrics.largeIconButtonSize
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func toggleWatchLater() {
        guard let activeItem else { return }
        if followedItemIDs.contains(activeItem.id) {
            followedItemIDs.remove(activeItem.id)
            homeToast("Removed from Watch later")
        } else {
            followedItemIDs.insert(activeItem.id)
            homeToast("Added to Watch later")
        }
    }
}
