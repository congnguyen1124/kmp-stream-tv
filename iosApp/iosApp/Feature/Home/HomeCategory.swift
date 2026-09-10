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

    var systemImage: String {
        switch self {
        case .home: "house"
        case .movies: "film"
        case .series: "rectangle.stack"
        case .live: "dot.radiowaves.left.and.right"
        case .more: "ellipsis"
        }
    }
}
