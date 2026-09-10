import Shared
import SwiftUI

enum HomeSectionStyle {
    case highlightWide
    case highlightTall
    case generalWide
    case topTen
    case generalTall
    case circle
    case short
    case story
    case continueWatching
    case miniApps

    init(_ presentation: HomeSectionPresentation) {
        switch presentation.name {
        case "HighlightWide": self = .highlightWide
        case "HighlightTall": self = .highlightTall
        case "TopTen": self = .topTen
        case "GeneralTall": self = .generalTall
        case "Circle": self = .circle
        case "Short": self = .short
        case "Story": self = .story
        case "ContinueWatching": self = .continueWatching
        case "MiniApps": self = .miniApps
        default: self = .generalWide
        }
    }
}

struct HomeSectionView: View {
    let section: HomeSectionUiModel
    /// `LayoutHighlightTallViewHolder` widens its top margin for the first feed row.
    let index: Int
    let onSelect: (HomeContentUiModel) -> Void

    var body: some View {
        switch HomeSectionStyle(section.presentation) {
        case .highlightWide:
            HighlightWideSectionView(section: section, onSelect: onSelect)
        case .highlightTall:
            HighlightTallSectionView(section: section, isTopSection: index == 0, onSelect: onSelect)
        case .topTen:
            TopTenSectionView(section: section, onSelect: onSelect)
        case .story:
            StorySectionView(section: section, onSelect: onSelect)
        case .miniApps:
            MiniAppsSectionView(section: section, onSelect: onSelect)
        case .generalWide:
            HomeRailSectionView(section: section, cardStyle: .landscape, onSelect: onSelect)
        case .generalTall:
            HomeRailSectionView(section: section, cardStyle: .portrait, onSelect: onSelect)
        case .circle:
            HomeRailSectionView(section: section, cardStyle: .circle, onSelect: onSelect)
        case .short:
            HomeRailSectionView(section: section, cardStyle: .short, onSelect: onSelect)
        case .continueWatching:
            HomeRailSectionView(section: section, cardStyle: .continueWatching, onSelect: onSelect)
        }
    }
}

/// `layout_view.xml` bound by `LayoutGeneralView` and `LayoutWatching`.
private struct HomeRailSectionView: View {
    let section: HomeSectionUiModel
    let cardStyle: HomeContentCardStyle
    let onSelect: (HomeContentUiModel) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: StreamMetrics.listTitleBottomPadding) {
            title

            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(alignment: .top, spacing: cardStyle.railSpacing) {
                    ForEach(section.items, id: \.id) { item in
                        Button {
                            onSelect(item)
                        } label: {
                            HomeContentCard(item: item, style: cardStyle)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(item.title)
                    }
                }
                .padding(.horizontal, StreamMetrics.contentInset)
                .frame(minHeight: cardStyle.railMinHeight, alignment: .top)
            }
        }
        .padding(.vertical, cardStyle.sectionVerticalPadding)
    }

    /// `LayoutGeneralView.bindShort` swaps in the `ic_lightning` compound drawable.
    private var title: some View {
        HStack(spacing: 4) {
            if cardStyle == .short {
                Image("ic_lightning")
                    .resizable()
                    .frame(width: 24, height: 25)
            }

            Text(section.title)
                .font(.streamBold(18))
                .foregroundStyle(.white)
        }
        .padding(.horizontal, StreamMetrics.contentInset)
    }
}

private extension HomeContentCardStyle {
    /// The Android item layouts own their trailing spacing; these are the resulting card gaps.
    var railSpacing: CGFloat {
        switch self {
        case .short: 18
        default: StreamMetrics.cardSpacing
        }
    }

    /// `rcvItems.minimumHeight` assigned while binding each layout family.
    var railMinHeight: CGFloat {
        switch self {
        case .landscape, .continueWatching: StreamCardSize.ephemeralWide.height
        case .portrait: StreamCardSize.ephemeralTall.height
        case .circle: StreamCardSize.circleIcon
        case .short: StreamCardSize.short.height
        case .story: StreamCardSize.story.height
        case .topTen: StreamCardSize.topTen.height
        }
    }

    /// `layoutContent` vertical padding, widened by `bindShort`.
    var sectionVerticalPadding: CGFloat {
        self == .short ? StreamMetrics.shortSectionVerticalPadding : StreamMetrics.sectionVerticalPadding
    }
}
