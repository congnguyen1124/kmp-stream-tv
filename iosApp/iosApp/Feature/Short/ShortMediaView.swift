import Shared
import Foundation
import SwiftUI

struct ShortMediaView: View {
    @StateObject private var store: ShortStore
    @State private var visibleId: String?
    @State private var message: String?

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
            }
        }
        .alert("Short", isPresented: messagePresented) {
            Button("OK", role: .cancel) { message = nil }
        } message: {
            Text(message ?? "")
        }
        .preferredColorScheme(.dark)
    }

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
                            onProfile: { message = "The \(item.providerName) profile will open here." },
                            onFollow: { store.toggleFollow(item) },
                            onLike: { store.toggleLike(item) },
                            onComment: { message = "Comments will open in their own sheet." },
                            onMore: { message = "More short-video actions will be available here." }
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
                        .frame(width: 36, height: 36)
                }
                .accessibilityLabel("Close shorts")
            }

            Text("Short")
                .font(.streamBold(20))
                .frame(maxWidth: .infinity, alignment: .leading)

            Button { message = "Search will be connected in a later screen." } label: {
                Image(systemName: "magnifyingglass").frame(width: 36, height: 36)
            }
            .accessibilityLabel("Search")

            Button { message = "Profile will be connected in a later screen." } label: {
                Image(systemName: "person.crop.circle").frame(width: 36, height: 36)
            }
            .accessibilityLabel("Profile")
        }
        .font(.system(size: 20, weight: .semibold))
        .foregroundStyle(.white)
        .padding(.horizontal, 16)
        .frame(maxHeight: .infinity, alignment: .top)
        .padding(.top, 8)
        .background(alignment: .top) {
            LinearGradient.streamTopbar.frame(height: 96).ignoresSafeArea(edges: .top)
        }
    }

    private var messagePresented: Binding<Bool> {
        Binding(
            get: { message != nil },
            set: { if !$0 { message = nil } }
        )
    }

    private func positionAtSharedSelection(ifUnsetOnly: Bool = false) {
        guard (!ifUnsetOnly || visibleId == nil), !store.state.items.isEmpty else { return }
        let index = min(max(Int(store.state.activeIndex), 0), store.state.items.count - 1)
        visibleId = store.state.items[index].id
    }
}

private struct ShortPageView: View {
    let item: ShortItemUiModel
    let isActive: Bool
    let onProfile: () -> Void
    let onFollow: () -> Void
    let onLike: () -> Void
    let onComment: () -> Void
    let onMore: () -> Void

    @StateObject private var streamPlayer = StreamPlayer()

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
                .onTapGesture { streamPlayer.togglePlayback() }

            if isActive && !streamPlayer.isPlaying {
                Image(systemName: "play.fill")
                    .font(.system(size: 34, weight: .bold))
                    .foregroundStyle(.white)
                    .padding(22)
                    .background(.black.opacity(0.45), in: Circle())
                    .allowsHitTesting(false)
            }

            chrome
        }
        .clipped()
        .onAppear { updatePlayback(active: isActive) }
        .onChange(of: isActive) { _, active in updatePlayback(active: active) }
        .onChange(of: streamPlayer.completionCount) { _, _ in
            if isActive { streamPlayer.replay() }
        }
        .onDisappear { streamPlayer.stop() }
    }

    private var chrome: some View {
        HStack(alignment: .bottom, spacing: 8) {
            VStack(alignment: .leading, spacing: 8) {
                Button(action: onProfile) {
                    Text(item.providerName)
                        .font(.streamBold(16))
                        .foregroundStyle(.white)
                }
                .buttonStyle(.plain)

                Text("\(item.title)\n\(item.description_)")
                    .font(.streamRegular(14))
                    .foregroundStyle(.white)
                    .lineLimit(4)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            VStack(spacing: 12) {
                Button(action: onProfile) {
                    RemoteArtwork(url: item.providerAvatarUrl)
                        .frame(width: 48, height: 48)
                        .clipShape(Circle())
                        .overlay(Circle().stroke(Color.white, lineWidth: 1))
                }
                .buttonStyle(.plain)

                Button(item.isFollowingProvider ? "Following" : "Follow", action: onFollow)
                    .font(.streamSemiBold(12))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 10)
                    .frame(height: 30)
                    .background(
                        item.isFollowingProvider ? Color.streamSurface.opacity(0.8) : Color.streamAccent,
                        in: Capsule()
                    )

                ShortActionButton(
                    icon: item.isLiked ? "heart.fill" : "heart",
                    label: item.likeCountLabel,
                    action: onLike
                )
                ShortActionButton(icon: "message", label: item.commentCountLabel, action: onComment)
                ShareLink(item: "\(item.title) \(item.videoUrl)") {
                    ShortActionLabel(icon: "square.and.arrow.up", label: item.shareCountLabel)
                }
                ShortActionButton(icon: "ellipsis", label: "", action: onMore)
            }
            .frame(width: 68)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
        .padding(.leading, 16)
        .padding(.trailing, 8)
        .padding(.bottom, 16)
    }

    private func updatePlayback(active: Bool) {
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

private struct ShortActionButton: View {
    let icon: String
    let label: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ShortActionLabel(icon: icon, label: label)
        }
        .buttonStyle(.plain)
    }
}

private struct ShortActionLabel: View {
    let icon: String
    let label: String

    var body: some View {
        VStack(spacing: 2) {
            Image(systemName: icon)
                .font(.system(size: 24, weight: .semibold))
                .frame(width: 48, height: 42)
            if !label.isEmpty {
                Text(label).font(.streamSemiBold(12))
            }
        }
        .foregroundStyle(.white)
    }
}
