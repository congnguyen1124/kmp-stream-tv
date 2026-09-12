import Foundation

enum HomeCategory: String, CaseIterable, Identifiable {
    case home
    case movies
    case series
    case live
    case more

    var id: String { rawValue }

    var title: String {
        switch self {
        case .home: "Home"
        case .movies: "Movies"
        case .series: "Series"
        case .live: "Live"
        case .more: "More"
        }
    }

    var placeholderMessage: String {
        "The \(title) category is wired into Home navigation and ready for its screen."
    }

    /// `HomeTabFragment.MORE_CATEGORY_ID` opens the picker and is never committed as content.
    static var selectable: [HomeCategory] {
        allCases.filter { $0 != .more }
    }
}
