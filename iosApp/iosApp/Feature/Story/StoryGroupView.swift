import Shared
import Foundation
import SwiftUI

/// `fragment_story_group.xml` reaction geometry.
private enum StoryMetrics {
    /// `reactions` LinearLayout height, which is also each button's height.
    static let reactionRowHeight: CGFloat = 56
    /// `StoryReaction.textSize`
    static let reactionTextSize: CGFloat = 26
    static let shareButtonSize: CGFloat = 48
}

/// Every emoji the reaction row offers. The burst pool drops the last one, exactly as
/// `playInitialReactionBurst` drops the last child of `reactions`.
private let storyReactions = ["👍", "❤️", "😂", "😮", "😢", "😡"]
private var storyBurstPool: [String] { Array(storyReactions.dropLast()) }

struct StoryGroupView: View {
    let initialId: String

    @StateObject private var store: StoryGroupStore
    @StateObject private var streamPlayer = StreamPlayer()
    @StateObject private var reactions = StoryReactionStore()
    @State private var loadedId: String?
    @Environment(\.dismiss) private var dismiss
    @Environment(\.scenePhase) private var scenePhase

    init(initialId: String) {
        self.initialId = initialId
        _store = StateObject(wrappedValue: StoryGroupStore(initialId: initialId))
    }

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            if let item = activeItem {
                RemoteArtwork(url: item.thumbnailUrl, placeholder: .branded).ignoresSafeArea()
                NativePlayerView(player: streamPlayer.player, videoGravity: .resizeAspectFill)
                    .ignoresSafeArea()
                storyGradient
                touchZones
                chrome(item)
            } else if store.state.isLoading {
                ProgressView("Loading story")
                    .tint(.streamAccentBright)
                    .foregroundStyle(.white)
            } else {
                errorView
            }
        }
        .onChange(of: activeItem?.id) { _, _ in loadActiveStory() }
        .onChange(of: streamPlayer.completionCount) { _, _ in moveNext() }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                if loadedId != nil { streamPlayer.play() }
            } else {
                // `onPause` cancels the burst and force-hides every animation still running.
                reactions.cancelAll()
                streamPlayer.pause()
            }
        }
        .onAppear { loadActiveStory() }
        .onDisappear {
            reactions.cancelAll()
            streamPlayer.stop()
        }
        .preferredColorScheme(.dark)
    }

    private var activeItem: ShortItemUiModel? {
        guard !store.state.items.isEmpty else { return nil }
        let index = min(max(Int(store.state.activeIndex), 0), store.state.items.count - 1)
        return store.state.items[index]
    }

    private var storyGradient: some View {
        VStack(spacing: 0) {
            LinearGradient(
                colors: [.black.opacity(0.8), .clear],
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(height: 180)
            Spacer()
            LinearGradient(
                colors: [.clear, .black.opacity(0.72)],
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(height: 140)
        }
        .ignoresSafeArea()
    }

    private var touchZones: some View {
        HStack(spacing: 0) {
            StoryTouchZone(onTap: movePrevious, onHold: streamPlayer.pause, onRelease: streamPlayer.play)
            StoryTouchZone(onTap: moveNext, onHold: streamPlayer.pause, onRelease: streamPlayer.play)
        }
        .ignoresSafeArea()
    }

    private func chrome(_ item: ShortItemUiModel) -> some View {
        VStack(spacing: 12) {
            StoryProgressView(
                itemCount: store.state.items.count,
                activeIndex: Int(store.state.activeIndex),
                activeProgress: playbackProgress
            )

            HStack(spacing: 10) {
                RemoteArtwork(url: item.providerAvatarUrl)
                    .frame(width: 40, height: 40)
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: 2) {
                    Text(item.providerName).font(.streamSemiBold(16))
                    Text(item.publishedLabel)
                        .font(.streamRegular(12))
                        .foregroundStyle(Color.streamSecondaryText)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Button(action: dismiss.callAsFunction) {
                    Image(systemName: "xmark")
                        .font(.system(size: 18, weight: .bold))
                        .frame(width: 44, height: 44)
                }
                .accessibilityLabel("Close story")
            }

            Spacer()

            HStack(spacing: 0) {
                reactionRow

                ShareLink(item: "\(item.title) \(item.videoUrl)") {
                    Image("ic_player_share")
                        .renderingMode(.template)
                        .resizable()
                        .scaledToFit()
                        .padding(12)
                        .frame(width: StoryMetrics.shareButtonSize, height: StoryMetrics.shareButtonSize)
                }
                .accessibilityLabel("Share")
            }
            .foregroundStyle(.white)
        }
        .padding(.horizontal, 12)
        .padding(.top, 8)
        .padding(.bottom, 12)
    }

    /// `reactions`: equal-weight emoji buttons, each of which clones itself into the animation
    /// layer rather than only scaling in place.
    private var reactionRow: some View {
        HStack(spacing: 0) {
            ForEach(Array(storyReactions.enumerated()), id: \.element) { column, emoji in
                Button {
                    reactions.emit(emoji: emoji, column: column)
                } label: {
                    Text(emoji)
                        .font(.system(size: StoryMetrics.reactionTextSize))
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("React with \(emoji)")
            }
        }
        .frame(height: StoryMetrics.reactionRowHeight)
        .overlay { reactionAnimationLayer }
    }

    /// `reactionAnimationLayer`: the non-interactive layer the cloned emoji travel through.
    ///
    /// Anchored to the reaction row rather than to the whole story, because each clone starts
    /// exactly where its source button sits. An overlay does not clip, so the emoji stay visible
    /// all the way up their five-button travel.
    private var reactionAnimationLayer: some View {
        GeometryReader { proxy in
            let columnWidth = proxy.size.width / CGFloat(storyReactions.count)

            ZStack(alignment: .topLeading) {
                ForEach(reactions.reactions) { reaction in
                    FloatingReactionView(
                        reaction: reaction,
                        size: CGSize(width: columnWidth, height: proxy.size.height)
                    )
                    .offset(x: columnWidth * CGFloat(reaction.column))
                }
            }
            .frame(width: proxy.size.width, height: proxy.size.height, alignment: .topLeading)
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }

    private var playbackProgress: CGFloat {
        guard streamPlayer.duration > 0 else { return 0 }
        return CGFloat(min(max(streamPlayer.currentTime / streamPlayer.duration, 0), 1))
    }

    private var errorView: some View {
        VStack(spacing: 16) {
            Text(store.state.errorMessage ?? "Unable to load this story")
                .foregroundStyle(.white)
                .multilineTextAlignment(.center)
            Button("Try again") { store.retry(initialId: initialId) }
                .buttonStyle(.borderedProminent)
                .tint(.streamAccent)
            Button("Close", action: dismiss.callAsFunction)
                .foregroundStyle(.white)
        }
        .padding(24)
    }

    private func loadActiveStory() {
        guard let item = activeItem, item.id != loadedId, let url = URL(string: item.videoUrl) else { return }
        loadedId = item.id
        streamPlayer.load(url: url)
        reactions.playInitialBurst(storyId: item.id, initialId: initialId, pool: storyBurstPool)
    }

    private func movePrevious() {
        if !store.moveToPrevious() {
            streamPlayer.replay()
        }
    }

    private func moveNext() {
        if !store.moveToNext() {
            dismiss()
        }
    }

}

/// `emotion_animation`: translate up five button heights while fading to zero, over 800 ms.
private struct FloatingReactionView: View {
    let reaction: FloatingReaction
    let size: CGSize

    @State private var isTravelling = false

    var body: some View {
        Text(reaction.emoji)
            .font(.system(size: StoryMetrics.reactionTextSize))
            .frame(width: size.width, height: size.height)
            .offset(y: isTravelling ? -size.height * StoryReactionStore.travelHeightMultiple : 0)
            .opacity(isTravelling ? 0 : 1)
            .onAppear {
                withAnimation(.linear(duration: StoryReactionStore.travelDuration)) {
                    isTravelling = true
                }
            }
    }
}

private struct StoryTouchZone: View {
    let onTap: () -> Void
    let onHold: () -> Void
    let onRelease: () -> Void

    var body: some View {
        Color.clear
            .contentShape(Rectangle())
            .onTapGesture(perform: onTap)
            .onLongPressGesture(
                minimumDuration: 0.6,
                maximumDistance: 50,
                pressing: { pressing in
                    if pressing { onHold() } else { onRelease() }
                },
                perform: {}
            )
    }
}

private struct StoryProgressView: View {
    let itemCount: Int
    let activeIndex: Int
    let activeProgress: CGFloat

    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<itemCount, id: \.self) { index in
                GeometryReader { geometry in
                    ZStack(alignment: .leading) {
                        Capsule().fill(Color.white.opacity(0.3))
                        Capsule()
                            .fill(Color.white)
                            .frame(width: geometry.size.width * fraction(for: index))
                    }
                }
                .frame(height: 2)
            }
        }
    }

    private func fraction(for index: Int) -> CGFloat {
        if index < activeIndex { return 1 }
        if index > activeIndex { return 0 }
        return activeProgress
    }
}
