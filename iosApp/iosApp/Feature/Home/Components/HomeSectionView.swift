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
    let onSelect: (HomeContentUiModel) -> Void

    var body: some View {
        switch HomeSectionStyle(section.presentation) {
        case .highlightWide:
            HighlightWideSectionView(section: section, onSelect: onSelect)
        case .highlightTall:
            HighlightTallSectionView(section: section, onSelect: onSelect)
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

private struct HomeRailSectionView: View {
    let section: HomeSectionUiModel
    let cardStyle: HomeContentCardStyle
    let onSelect: (HomeContentUiModel) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(section.title)
                .font(.title3.bold())
                .padding(.horizontal, StreamMetrics.contentInset)

            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(alignment: .top, spacing: StreamMetrics.cardSpacing) {
                    ForEach(Array(section.items.enumerated()), id: \.element.id) { index, item in
                        Button {
                            onSelect(item)
                        } label: {
                            HomeContentCard(item: item, style: cardStyle, rank: index + 1)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(item.title)
                    }
                }
                .padding(.horizontal, StreamMetrics.contentInset)
            }
        }
    }
}
