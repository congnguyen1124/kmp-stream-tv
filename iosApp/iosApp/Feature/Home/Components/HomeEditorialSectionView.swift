import Shared
import SwiftUI

/// `layout_view.xml` with the Story chrome applied by `LayoutStory.bindStoryChrome`.
struct StorySectionView: View {
    let section: HomeSectionUiModel
    let onSelect: (HomeContentUiModel) -> Void

    /// Title block, `margin_tiny` gap, story rail, then the layout and root bottom padding.
    private static let height = StreamCardSize.storyTitleBlock.height
        + 4
        + StreamCardSize.story.height
        + 2 * StreamMetrics.sectionVerticalPadding

    var body: some View {
        ZStack(alignment: .top) {
            Image("bg_story_layout")
                .resizable()
                .opacity(0.8)
                .padding(.top, 4)
                .padding(.bottom, StreamMetrics.sectionVerticalPadding)

            VStack(alignment: .leading, spacing: 0) {
                storyTitle

                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(spacing: StreamMetrics.cardSpacing) {
                        ForEach(section.items, id: \.id) { item in
                            Button {
                                onSelect(item)
                            } label: {
                                HomeContentCard(item: item, style: .story)
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(item.title)
                        }
                    }
                    .padding(.horizontal, StreamMetrics.contentInset)
                }
                .frame(height: StreamCardSize.story.height)
                .padding(.top, 4)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.bottom, 2 * StreamMetrics.sectionVerticalPadding)
        }
        .frame(height: Self.height)
        .overlay(alignment: .top) {
            LinearGradient.streamStoryDividerTop
                .frame(height: 2)
                .padding(.top, 2)
        }
        .overlay(alignment: .bottom) {
            LinearGradient.streamStoryDividerBottom
                .frame(height: 2)
                .padding(.bottom, StreamMetrics.sectionVerticalPadding)
        }
    }

    private var storyTitle: some View {
        HStack(spacing: 4) {
            Image("ic_fire")
                .resizable()
                .frame(width: 16, height: 16)
            Text(section.title)
                .font(.streamSemiBold(16))
                .foregroundStyle(.white)
                .lineLimit(1)
        }
        .padding(.leading, StreamMetrics.contentInset)
        .padding(.trailing, 24)
        .frame(
            minWidth: StreamCardSize.storyTitleBlock.width,
            minHeight: StreamCardSize.storyTitleBlock.height,
            maxHeight: StreamCardSize.storyTitleBlock.height,
            alignment: .leading
        )
        .fixedSize(horizontal: true, vertical: false)
        .background { Image("bg_story_block").resizable() }
    }
}

/// `layout_background_view.xml` bound by `LayoutBackgroundView`.
struct TopTenSectionView: View {
    let section: HomeSectionUiModel
    let onSelect: (HomeContentUiModel) -> Void

    @State private var railOffset: CGFloat = 0

    /// `maxWidthTitle` in `LayoutBackgroundView`: the title column plus one poster.
    private static let titleFadeDistance = StreamCardSize.topTenTitleWidth + StreamCardSize.topTen.width
    private static let height: CGFloat = 241

    var body: some View {
        ZStack(alignment: .leading) {
            if let backgroundUrl = section.backgroundUrl, !backgroundUrl.isEmpty {
                RemoteArtwork(url: backgroundUrl)
            }

            titleBlock
                .scaleEffect(titleProgress)
                .opacity(titleProgress)

            rail
        }
        .frame(maxWidth: .infinity)
        .frame(height: Self.height)
        .clipped()
    }

    /// The source rail shrinks and fades the title column as the posters scroll over it.
    private var titleProgress: CGFloat {
        min(max(1 - railOffset / Self.titleFadeDistance, 0), 1)
    }

    private var titleBlock: some View {
        VStack(alignment: .leading, spacing: 0) {
            Image("ic_fire")
                .resizable()
                .frame(width: StreamMetrics.iconButtonSize, height: StreamMetrics.iconButtonSize)

            Color.clear.frame(height: 10)

            Text(section.title)
                .font(.streamBold(18))
                .foregroundStyle(.white)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.leading, 24)
        .padding(.trailing, 12)
        .frame(width: StreamCardSize.topTenTitleWidth, height: Self.height, alignment: .leading)
    }

    private var rail: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            LazyHStack(spacing: 0) {
                ForEach(Array(section.items.enumerated()), id: \.element.id) { index, item in
                    Button {
                        onSelect(item)
                    } label: {
                        HomeContentCard(item: item, style: .topTen, rank: index + 1)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Number \(index + 1), \(item.title)")
                }
            }
            .padding(.leading, StreamCardSize.topTenTitleWidth)
            .padding(.trailing, StreamMetrics.contentInset)
        }
        .onScrollGeometryChange(for: CGFloat.self) { geometry in
            geometry.contentOffset.x
        } action: { _, offset in
            railOffset = offset
        }
        .padding(.vertical, StreamMetrics.sectionVerticalPadding)
    }
}

/// `layout_mini_app_view.xml` with the `bg_mini_app_content` panel.
struct MiniAppsSectionView: View {
    let section: HomeSectionUiModel
    let onSelect: (HomeContentUiModel) -> Void

    var body: some View {
        HStack(spacing: 0) {
            ForEach(section.items.prefix(5), id: \.id) { item in
                Button {
                    onSelect(item)
                } label: {
                    VStack(spacing: 8) {
                        RemoteArtwork(url: item.thumbnailUrl)
                            .frame(
                                width: StreamCardSize.miniAppIcon,
                                height: StreamCardSize.miniAppIcon
                            )
                            .clipShape(
                                RoundedRectangle(
                                    cornerRadius: StreamMetrics.logoAppCorner,
                                    style: .continuous
                                )
                            )
                        Text(item.title)
                            .font(.streamMedium(12))
                            .foregroundStyle(Color.streamSecondaryText)
                            .lineLimit(2)
                            .multilineTextAlignment(.center)
                            .frame(maxWidth: StreamCardSize.miniAppMaxWidth)
                    }
                    .padding(4)
                    .frame(maxWidth: .infinity, alignment: .top)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(item.title)
            }
        }
        .padding(8)
        .frame(minHeight: StreamCardSize.miniAppMinHeight)
        .background(Color.streamMiniAppFill)
        .clipShape(
            RoundedRectangle(cornerRadius: StreamMetrics.radiusNormalExtra, style: .continuous)
        )
        .overlay {
            RoundedRectangle(cornerRadius: StreamMetrics.radiusNormalExtra, style: .continuous)
                .stroke(LinearGradient.streamMiniAppBorder, lineWidth: 1)
        }
        .padding(StreamMetrics.sectionVerticalPadding)
    }
}
