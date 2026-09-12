import Shared
import Foundation
import SwiftUI
import UIKit

/// `item_short_media.xml` action-rail geometry and the `ShortMediaAction` style.
private enum ShortRailMetrics {
    static let avatarSize: CGFloat = 48
    /// `ic_short_follow_state` at its intrinsic size plus `margin_tiny` padding.
    static let followIconSize: CGFloat = 24
    static let followPadding: CGFloat = 4
    /// The follow badge paints at 32 pt; its hit area is widened to the 44 pt minimum.
    static let followHitSize: CGFloat = 44
    /// `iconSize`
    static let actionIconSize: CGFloat = 32
    /// `iconPadding`
    static let actionIconSpacing: CGFloat = 4
    static let actionPaddingTop: CGFloat = 4
    static let actionPaddingBottom: CGFloat = 8
    static let actionPaddingHorizontal: CGFloat = 8
    /// `FOLLOW_INVISIBLE_DELAY_MILLIS`
    static let followVisibleSeconds: TimeInterval = 1
    /// `scale_down_in`
    static let pauseIndicatorSeconds: TimeInterval = 0.1
    static let pauseIndicatorFromScale: CGFloat = 1.5
    static let pauseIndicatorFromOpacity: Double = 0.5
    /// `fade_out_short`
    static let resumeIndicatorSeconds: TimeInterval = 0.15
}

/// One presented sheet at a time, the way the Fragment dismisses an open sheet before showing the
/// next. The item is addressed by id so the sheet always renders the shared state's live copy.
private enum ShortSheet: Identifiable {
    case comments(String)
    case more(String)
    case provider(String)
    case search

    var id: String {
        switch self {
        case .comments(let id): "comments-\(id)"
        case .more(let id): "more-\(id)"
        case .provider(let id): "provider-\(id)"
        case .search: "search"
        }
    }
}

struct ShortMediaView: View {
    @StateObject private var store: ShortStore
    @State private var visibleId: String?
    @State private var toast: String?
    @State private var sheet: ShortSheet?
    @State private var reportTarget: ShortItemUiModel?
    /// Run after the More sheet closes, because Report raises an alert of its own.
    @State private var pendingMore: (action: ShortMoreAction, item: ShortItemUiModel)?
    @State private var isProfilePresented = false

    private let showsCloseButton: Bool
    private let isDestinationActive: Bool
    @Environment(\.dismiss) private var dismiss
    @Environment(\.scenePhase) private var scenePhase

    init(
        initialId: String? = nil,
        showsCloseButton: Bool = false,
        isDestinationActive: Bool = true
    ) {
        _store = StateObject(wrappedValue: ShortStore(initialId: initialId))
        _visibleId = State(initialValue: initialId)
        self.showsCloseButton = showsCloseButton
        self.isDestinationActive = isDestinationActive
    }

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                Color.black.ignoresSafeArea()
                content(pageSize: geometry.size)
                topBar

                if let toast {
                    Text(toast)
                        .font(.streamRegular(14))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 16)
                        .frame(minHeight: 48)
                        .background(Color.black.opacity(0.88))
                        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                        .padding(.horizontal, 24)
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
                        .padding(.bottom, 120)
                        .transition(.opacity)
                        .allowsHitTesting(false)
                }
            }
        }
        .sheet(item: $sheet, onDismiss: runPendingMore, content: sheetContent)
        .fullScreenCover(isPresented: $isProfilePresented) { UserProfileView() }
        .alert("Report this short?", isPresented: reportPresented, presenting: reportTarget) { _ in
            Button("Cancel", role: .cancel) { reportTarget = nil }
            Button("Report") {
                reportTarget = nil
                showToast("Report sent. Thank you.")
            }
        } message: { _ in
            Text("This demo records the report locally. Connect the report API before production.")
        }
        .preferredColorScheme(.dark)
    }

    // MARK: - Feed

    @ViewBuilder
    private func content(pageSize: CGSize) -> some View {
        if store.state.items.isEmpty && store.state.isLoading {
            ProgressView("Loading shorts")
                .tint(.streamAccentBright)
                .foregroundStyle(.white)
        } else if store.state.items.isEmpty, let error = store.state.errorMessage {
            VStack(spacing: 16) {
                Text(error).foregroundStyle(.white).multilineTextAlignment(.center)
                Button("Try again", action: store.retry)
                    .buttonStyle(.borderedProminent)
                    .tint(.streamAccent)
            }
            .padding(24)
        } else {
            ScrollView(.vertical) {
                LazyVStack(spacing: 0) {
                    ForEach(store.state.items, id: \.id) { item in
                        ShortPageView(
                            item: item,
                            isActive: item.id == visibleId && isDestinationActive && scenePhase == .active,
                            onProfile: { sheet = .provider(item.id) },
                            onFollow: { store.toggleFollow(item) },
                            onLike: { store.toggleLike(item) },
                            onComment: { sheet = .comments(item.id) },
                            onMore: { sheet = .more(item.id) }
                        )
                        .frame(width: pageSize.width, height: pageSize.height)
                        .id(item.id)
                    }
                }
                .scrollTargetLayout()
            }
            .scrollIndicators(.hidden)
            .scrollTargetBehavior(.paging)
            .scrollPosition(id: $visibleId)
            .onChange(of: visibleId) { _, id in
                guard let id else { return }
                store.select(id: id)
            }
            .onChange(of: store.state.activeIndex) { _, _ in
                positionAtSharedSelection()
            }
            .onChange(of: store.state.items.count) { _, _ in
                positionAtSharedSelection(ifUnsetOnly: true)
            }
            .overlay(alignment: .top) {
                if store.state.isLoadingMore {
                    ProgressView().tint(.streamAccentBright).padding(.top, 54)
                }
            }
        }
    }

    private var topBar: some View {
        HStack(spacing: 12) {
            if showsCloseButton {
                Button(action: dismiss.callAsFunction) {
                    Image(systemName: "xmark")
                        .font(.system(size: 16, weight: .bold))
                        .frame(width: 44, height: 44)
                }
                .accessibilityLabel("Close shorts")
            }

            Text("Short")
                .font(.streamBold(20))
                .frame(maxWidth: .infinity, alignment: .leading)

            Button { sheet = .search } label: {
                Image("ic_search")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .padding(10)
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .accessibilityLabel("Search")

            Button { isProfilePresented = true } label: {
                Image("ic_user_circle")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .padding(10)
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .accessibilityLabel("Profile")
        }
        .buttonStyle(.plain)
        .foregroundStyle(.white)
        .padding(.horizontal, 16)
        .frame(maxHeight: .infinity, alignment: .top)
        .padding(.top, 8)
        .background(alignment: .top) {
            LinearGradient.streamTopbar.frame(height: 96).ignoresSafeArea(edges: .top)
        }
    }

    // MARK: - Sheets

    @ViewBuilder
    private func sheetContent(_ sheet: ShortSheet) -> some View {
        switch sheet {
        case .comments(let id):
            if let item = item(id) {
                ShortCommentSheet(
                    item: item,
                    comments: store.comments(for: item),
                    onSubmit: { store.addComment($0, to: item) }
                )
            }
        case .more(let id):
            if let item = item(id) {
                ShortMoreSheet { pendingMore = ($0, item) }
            }
        case .provider(let id):
            if let item = item(id) {
                ShortProviderSheet(
                    item: item,
                    shorts: store.shorts(byProviderOf: item),
                    onSelect: { scroll(to: $0.id) },
                    onToggleFollow: { store.toggleFollow(item) }
                )
            }
        case .search:
            ShortSearchSheet(onSearch: search)
        }
    }

    private func item(_ id: String) -> ShortItemUiModel? {
        store.state.items.first { $0.id == id }
    }

    private var reportPresented: Binding<Bool> {
        Binding(
            get: { reportTarget != nil },
            set: { if !$0 { reportTarget = nil } }
        )
    }

    // MARK: - Actions

    private func runPendingMore() {
        guard let pending = pendingMore else { return }
        pendingMore = nil
        handleMore(pending.action, on: pending.item)
    }

    private func handleMore(_ action: ShortMoreAction, on item: ShortItemUiModel) {
        switch action {
        case .copyLink:
            UIPasteboard.general.string = item.videoUrl
            showToast("Link copied")

        case .notInterested:
            if let next = store.itemAfter(item) {
                scroll(to: next.id)
            }
            showToast("We’ll show fewer videos like this.")

        case .report:
            reportTarget = item
        }
    }

    private func search(_ query: String) {
        guard let match = store.firstMatch(query) else {
            showToast("No matching short video was found.")
            return
        }
        scroll(to: match.id)
    }

    private func scroll(to id: String) {
        store.selectById(id)
        withAnimation { visibleId = id }
    }

    private func showToast(_ message: String) {
        withAnimation { toast = message }
        Task { @MainActor in
            try? await Task.sleep(for: .seconds(2))
            guard toast == message else { return }
            withAnimation { toast = nil }
        }
    }

    private func positionAtSharedSelection(ifUnsetOnly: Bool = false) {
        guard (!ifUnsetOnly || visibleId == nil), !store.state.items.isEmpty else { return }
        let index = min(max(Int(store.state.activeIndex), 0), store.state.items.count - 1)
        visibleId = store.state.items[index].id
    }
}

// MARK: - Page

private struct ShortPageView: View {
    let item: ShortItemUiModel
    let isActive: Bool
    let onProfile: () -> Void
    let onFollow: () -> Void
    let onLike: () -> Void
    let onComment: () -> Void
    let onMore: () -> Void

    @StateObject private var streamPlayer = StreamPlayer()
    /// `pendingFollowProviderId`: the one second a freshly followed provider keeps its check.
    @State private var pendingFollowProviderId: String?
    @State private var followTask: Task<Void, Never>?
    @State private var indicatorScale: CGFloat = 1
    @State private var indicatorOpacity: Double = 0
    @State private var indicatorTask: Task<Void, Never>?

    var body: some View {
        ZStack {
            RemoteArtwork(url: item.thumbnailUrl, placeholder: .branded)
            NativePlayerView(player: streamPlayer.player, videoGravity: .resizeAspectFill)
                .opacity(isActive ? 1 : 0)

            LinearGradient(
                colors: [.clear, .black.opacity(0.9)],
                startPoint: .center,
                endPoint: .bottom
            )

            Color.clear
                .contentShape(Rectangle())
                .onTapGesture { togglePlayback() }

            playIndicator

            chrome
        }
        .clipped()
        .onAppear { updatePlayback(active: isActive) }
        .onChange(of: isActive) { _, active in updatePlayback(active: active) }
        .onChange(of: streamPlayer.completionCount) { _, _ in
            if isActive { streamPlayer.replay() }
        }
        .onDisappear {
            followTask?.cancel()
            indicatorTask?.cancel()
            streamPlayer.stop()
        }
    }

    /// `playIndicator` with `animatePlaybackToggle`: pausing scales the icon into view over 100 ms
    /// and holds it; resuming fades it out over 150 ms.
    private var playIndicator: some View {
        Image("ic_player_play")
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .padding(18)
            .frame(width: 72, height: 72)
            .foregroundStyle(.white)
            .scaleEffect(indicatorScale)
            .opacity(indicatorOpacity)
            .allowsHitTesting(false)
            .accessibilityHidden(true)
    }

    // MARK: - Action rail

    private var chrome: some View {
        HStack(alignment: .bottom, spacing: 8) {
            VStack(alignment: .leading, spacing: 8) {
                Button(action: onProfile) {
                    Text(item.providerName)
                        .font(.streamSemiBold(16))
                        .foregroundStyle(.white)
                        .lineLimit(1)
                        .frame(minHeight: 44, alignment: .bottom)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(item.providerName)

                // `short_description_format`
                Text("\(item.title)\n\(item.description_)")
                    .font(.streamRegular(14))
                    .foregroundStyle(.white)
                    .lineLimit(4)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            actions
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
        .padding(.leading, 16)
        .padding(.trailing, 8)
        .padding(.bottom, 16)
    }

    /// The `actions` column: avatar with its overlaid follow control, then Like, Comment, Share and
    /// More as text-top buttons.
    private var actions: some View {
        VStack(spacing: 0) {
            avatarBlock
                .padding(.bottom, 4)

            ShortRailButton(
                icon: item.isLiked ? "ic_player_heart_fill" : "ic_player_heart",
                label: item.likeCountLabel,
                accessibilityLabel: "\(item.isLiked ? "Unlike" : "Like"), \(item.likeCountLabel)",
                action: onLike
            )

            ShortRailButton(
                icon: "ic_player_comment",
                label: item.commentCountLabel,
                accessibilityLabel: "Comment, \(item.commentCountLabel)",
                action: onComment
            )

            ShareLink(item: shareURL, subject: Text(item.title), message: Text(item.title)) {
                ShortRailLabel(icon: "ic_player_share", label: "Share")
            }
            .accessibilityLabel("Share")

            ShortRailButton(
                icon: "ic_dots_vertical",
                label: nil,
                accessibilityLabel: "More options",
                action: onMore
            )
        }
    }

    /// `profileAvatar` with `follow` pinned across its bottom edge. The badge disappears once the
    /// provider is followed, one second after the tap that followed them.
    private var avatarBlock: some View {
        Button(action: onProfile) {
            RemoteArtwork(url: item.providerAvatarUrl)
                .frame(width: ShortRailMetrics.avatarSize, height: ShortRailMetrics.avatarSize)
                .clipShape(Circle())
                .overlay(Circle().stroke(Color.white, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(item.providerName) profile")
        .padding(.bottom, ShortRailMetrics.followHitSize / 2)
        .overlay(alignment: .bottom) {
            if showsFollow {
                Button(action: toggleFollow) {
                    Image(item.isFollowingProvider ? "ic_check_circle_solid" : "ic_plus_circle_solid")
                        .resizable()
                        .scaledToFit()
                        .frame(
                            width: ShortRailMetrics.followIconSize,
                            height: ShortRailMetrics.followIconSize
                        )
                        .padding(ShortRailMetrics.followPadding)
                        .frame(
                            width: ShortRailMetrics.followHitSize,
                            height: ShortRailMetrics.followHitSize
                        )
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(item.isFollowingProvider ? "Following" : "Follow")
            }
        }
    }

    /// `follow.isInvisible = isFollowingProvider && pendingFollowProviderId != providerId`
    private var showsFollow: Bool {
        !item.isFollowingProvider || pendingFollowProviderId == item.providerId
    }

    private var shareURL: URL {
        URL(string: item.videoUrl) ?? URL(string: "https://streamtv.example")!
    }

    // MARK: - Behaviour

    /// `toggleFollow`: the check shows immediately and the control hides a second later, while the
    /// follow itself applies provider-wide through the shared state.
    private func toggleFollow() {
        followTask?.cancel()
        let providerId = item.providerId
        if !item.isFollowingProvider {
            pendingFollowProviderId = providerId
            followTask = Task { @MainActor in
                try? await Task.sleep(for: .seconds(ShortRailMetrics.followVisibleSeconds))
                guard !Task.isCancelled else { return }
                pendingFollowProviderId = nil
            }
        } else {
            pendingFollowProviderId = nil
        }
        onFollow()
    }

    private func togglePlayback() {
        animatePlaybackToggle(wasPlaying: streamPlayer.isPlaying)
        streamPlayer.togglePlayback()
    }

    private func animatePlaybackToggle(wasPlaying: Bool) {
        indicatorTask?.cancel()
        if wasPlaying {
            // `scale_down_in`: the paused icon settles in and stays.
            indicatorScale = ShortRailMetrics.pauseIndicatorFromScale
            indicatorOpacity = ShortRailMetrics.pauseIndicatorFromOpacity
            withAnimation(.linear(duration: ShortRailMetrics.pauseIndicatorSeconds)) {
                indicatorScale = 1
                indicatorOpacity = 1
            }
        } else {
            // `fade_out_short`, on an accelerating curve.
            indicatorScale = 1
            indicatorOpacity = 1
            withAnimation(.easeIn(duration: ShortRailMetrics.resumeIndicatorSeconds)) {
                indicatorOpacity = 0
            }
        }
    }

    /// `hidePlayIndicator` runs whenever the adapter (re)starts a page.
    private func updatePlayback(active: Bool) {
        indicatorTask?.cancel()
        indicatorOpacity = 0
        indicatorScale = 1
        if active {
            if streamPlayer.player.currentItem == nil, let url = URL(string: item.videoUrl) {
                streamPlayer.load(url: url)
            } else {
                streamPlayer.play()
            }
        } else {
            streamPlayer.stop()
        }
    }
}

// MARK: - Rail primitives

/// `ShortMediaAction`: a 32 pt icon over a caption, with the icon keeping its own colours.
private struct ShortRailLabel: View {
    let icon: String
    let label: String?

    var body: some View {
        VStack(spacing: ShortRailMetrics.actionIconSpacing) {
            Image(icon)
                .renderingMode(.original)
                .resizable()
                .scaledToFit()
                .frame(
                    width: ShortRailMetrics.actionIconSize,
                    height: ShortRailMetrics.actionIconSize
                )

            if let label {
                Text(label)
                    .font(.streamSemiBold(12))
                    .foregroundStyle(.white)
            }
        }
        .padding(.top, ShortRailMetrics.actionPaddingTop)
        .padding(.bottom, ShortRailMetrics.actionPaddingBottom)
        .padding(.horizontal, ShortRailMetrics.actionPaddingHorizontal)
        .contentShape(Rectangle())
    }
}

private struct ShortRailButton: View {
    let icon: String
    let label: String?
    let accessibilityLabel: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ShortRailLabel(icon: icon, label: label)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(accessibilityLabel)
    }
}
