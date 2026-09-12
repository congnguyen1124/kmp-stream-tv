import Foundation

/// `ProfileAction` — one row of an outlined section.
struct ProfileAction: Identifiable, Equatable {
    let id: String
    let icon: String
    let title: String
}

/// `ProfileItem` — the signed-out hierarchy the RecyclerView renders as sign-in, gap and section
/// view types.
enum ProfileItem: Identifiable, Equatable {
    case signIn
    case gap(Int)
    case section(id: String, actions: [ProfileAction])

    var id: String {
        switch self {
        case .signIn: "sign-in"
        case .gap(let index): "gap-\(index)"
        case .section(let id, _): "section-\(id)"
        }
    }
}

/// `signedOutProfileItems`: the same order, grouping and icon family as the Android reference.
enum ProfileCatalog {
    static let signedOut: [ProfileItem] = [
        .signIn,
        .gap(0),
        .section(
            id: "settings",
            actions: [
                ProfileAction(id: "settings", icon: "ic_app_setting", title: "App settings"),
            ]
        ),
        .gap(1),
        .section(
            id: "about",
            actions: [
                ProfileAction(id: "about", icon: "ic_info_outline_medium", title: "About StreamTV"),
                ProfileAction(id: "privacy", icon: "ic_policy", title: "Privacy policy"),
                ProfileAction(id: "terms", icon: "ic_term", title: "Terms and conditions"),
                ProfileAction(id: "feedback", icon: "ic_feedback", title: "Feedback"),
            ]
        ),
        .gap(2),
    ]

    static let logIn = "Log in"

    /// `profile_action_unavailable`
    static func unavailable(_ action: String) -> String {
        "\(action) is not available yet."
    }
}
