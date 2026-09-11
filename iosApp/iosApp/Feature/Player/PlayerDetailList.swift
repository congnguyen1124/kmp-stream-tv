import SwiftUI

/// Port of `PlayerDetailAction`.
enum PlayerDetailAction: String, Identifiable {
    case watchLater
    case products
    case like
    case comment
    case share

    var id: String { rawValue }

    /// The `player_action_*` strings the Android fragment toasts.
    var message: String {
        switch self {
        case .watchLater: "Watch later updated"
        case .products: "Products will be connected to the commerce screen."
        case .like: "Like updated"
        case .comment: "Comments will open in their own screen."
        case .share: "Sharing will be connected when the content API is available."
        }
    }
}

/// What sits under the player in the detail presentation: the metadata block, the provider row and
/// the recommendation rail. Port of `PlayerDetailAdapter.kt`, including the local toggle sets it
/// keeps for watch-later, like and follow while those APIs are pending.
struct PlayerDetailList: View {
    let media: PlayerMedia
    let recommendations: [PlayerMedia]
    let onAction: (PlayerDetailAction) -> Void
    let onRecommendation: (PlayerMedia) -> Void

    @State private var savedIds: Set<String> = []
    @State private var likedIds: Set<String> = []
    @State private var followedProviders: Set<String> = []
    @State private var isSummaryExpanded = false

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                detailBlock
                providerRow

                Text("Recommended for you")
                    .font(.streamBold(18))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 16)
                    .padding(.top, 20)
                    .padding(.bottom, 10)

                ForEach(recommendations) { item in
                    recommendationRow(item)
                }
            }
            .padding(.bottom, 24)
        }
        .scrollIndicators(.hidden)
        .background(Color.streamBackground)
    }

    // MARK: - Detail

    private var detailBlock: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(media.title)
                .font(.streamBold(18))
                .foregroundStyle(.white)
                .fixedSize(horizontal: false, vertical: true)

            Button {
                guard !media.summary.isEmpty else { return }
                withAnimation(.easeInOut(duration: 0.2)) { isSummaryExpanded.toggle() }
            } label: {
                Text(userGraph)
                    .font(.streamRegular(14))
                    .foregroundStyle(Color.streamSecondaryText)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .padding(.top, 6)

            if isSummaryExpanded, !media.summary.isEmpty {
                Text(media.summary)
                    .font(.streamRegular(14))
                    .foregroundStyle(.white)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.top, 8)
            }

            HStack(spacing: 8) {
                if let age = media.ageRestriction, !age.isEmpty {
                    Text(age)
                        .font(.streamSemiBold(12))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(
                            Color.streamSurface,
                            in: RoundedRectangle(cornerRadius: 4, style: .continuous)
                        )
                }

                Text(subInfo)
                    .font(.streamRegular(12))
                    .foregroundStyle(Color.streamSecondaryText)
            }
            .padding(.top, 10)

            actionRow
                .padding(.top, 16)
        }
        .padding(.horizontal, 16)
        .padding(.top, 16)
    }

    /// `player_user_graph`: "<views> | <likes> … See more".
    private var userGraph: String {
        let secondary = [media.viewCountLabel, "12K likes"]
            .filter { !$0.isEmpty }
            .joined(separator: " | ")
        return "\(secondary) … See more"
    }

    private var subInfo: String {
        if media.isLive { return "LIVE" }
        if media.episodeCount > 0 {
            return media.episodeCount == 1 ? "1 episode" : "\(media.episodeCount) episodes"
        }
        return media.durationLabel
    }

    private var actionRow: some View {
        HStack(spacing: 0) {
            let isSaved = savedIds.contains(media.id)
            let isLiked = likedIds.contains(media.id)

            action(
                icon: isSaved ? "ic_player_check" : "ic_playlist_plus",
                title: "Watch later",
                isActive: isSaved
            ) {
                savedIds.toggle(media.id)
                onAction(.watchLater)
            }

            action(icon: "ic_player_shopping_bag", title: "Products", isActive: false) {
                onAction(.products)
            }

            action(
                icon: isLiked ? "ic_player_heart_fill" : "ic_player_heart",
                title: "Like",
                isActive: isLiked
            ) {
                likedIds.toggle(media.id)
                onAction(.like)
            }

            action(icon: "ic_player_comment", title: "Comment", isActive: false) {
                onAction(.comment)
            }

            action(icon: "ic_player_share", title: "Share", isActive: false) {
                onAction(.share)
            }
        }
    }

    private func action(
        icon: String,
        title: String,
        isActive: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Image(icon)
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 24, height: 24)
                Text(title)
                    .font(.streamSemiBold(12))
                    .lineLimit(1)
            }
            .foregroundStyle(isActive ? Color.streamAccentBright : .white)
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(title)
        .accessibilityAddTraits(isActive ? .isSelected : [])
    }

    // MARK: - Provider

    private var providerRow: some View {
        let name = media.providerName.isEmpty ? "StreamTV" : media.providerName
        let isFollowed = followedProviders.contains(media.providerName)

        return HStack(spacing: 12) {
            // Android draws `R.mipmap.ic_launcher_round` here; the iOS catalogue's app mark is a
            // wide wordmark, so it is fitted inside the circle rather than cropped to it.
            Image("StreamTvLogo")
                .resizable()
                .scaledToFit()
                .padding(6)
                .frame(width: 40, height: 40)
                .background(Color.streamSurface, in: Circle())

            Text(name)
                .font(.streamSemiBold(14))
                .foregroundStyle(.white)
                .lineLimit(1)

            Spacer(minLength: 0)

            Button {
                followedProviders.toggle(media.providerName)
            } label: {
                HStack(spacing: 6) {
                    Image(isFollowed ? "ic_player_check" : "ic_player_add")
                        .renderingMode(.template)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 16, height: 16)
                    Text(isFollowed ? "Following" : "Follow")
                        .font(.streamSemiBold(14))
                }
                .foregroundStyle(.white)
                .padding(.horizontal, 14)
                .frame(height: 36)
                .overlay(
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .stroke(
                            isFollowed ? Color.streamAccentBright : Color.white.opacity(0.3),
                            lineWidth: 1
                        )
                )
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 16)
        .padding(.top, 20)
        .padding(.bottom, 4)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(Color.streamSurface)
                .frame(height: 1)
        }
    }

    // MARK: - Recommendations

    private func recommendationRow(_ item: PlayerMedia) -> some View {
        Button {
            onRecommendation(item)
        } label: {
            HStack(spacing: 12) {
                RemoteArtwork(url: item.thumbnailUrl)
                    .frame(width: 148, height: 83)
                    .clipShape(
                        RoundedRectangle(cornerRadius: StreamMetrics.thumbnailCorner, style: .continuous)
                    )

                VStack(alignment: .leading, spacing: 6) {
                    Text(item.title)
                        .font(.streamSemiBold(14))
                        .foregroundStyle(.white)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)

                    Text(item.subtitle.isEmpty ? item.viewCountLabel : item.subtitle)
                        .font(.streamRegular(12))
                        .foregroundStyle(Color.streamSecondaryText)
                        .lineLimit(1)
                }

                Spacer(minLength: 0)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(item.title)
    }
}

private extension Set where Element == String {
    mutating func toggle(_ value: String) {
        if !insert(value).inserted { remove(value) }
    }
}
