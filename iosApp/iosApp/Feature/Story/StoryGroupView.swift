import Shared
import Foundation
import SwiftUI

struct StoryGroupView: View {
    let initialId: String

    @StateObject private var store: StoryGroupStore
    @StateObject private var streamPlayer = StreamPlayer()
    @State private var loadedId: String?
    @State private var toast: String?
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

            if let toast {
                Text(toast)
                    .font(.streamSemiBold(14))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 16)
                    .frame(height: 40)
                    .background(.black.opacity(0.75), in: Capsule())
                    .frame(maxHeight: .infinity, alignment: .bottom)
                    .padding(.bottom, 86)
                    .transition(.opacity)
            }
        }
        .onChange(of: activeItem?.id) { _, _ in loadActiveStory() }
        .onChange(of: streamPlayer.completionCount) { _, _ in moveNext() }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                if loadedId != nil { streamPlayer.play() }
            } else {
                streamPlayer.pause()
            }
        }
        .onAppear { loadActiveStory() }
        .onDisappear { streamPlayer.stop() }
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

            HStack(spacing: 12) {
                ForEach(["👍", "❤️", "😂", "😮", "😢", "😡"], id: \.self) { reaction in
                    Button(reaction) { showToast("Reaction sent") }
                        .font(.system(size: 24))
                        .buttonStyle(.plain)
                        .frame(maxWidth: .infinity)
                }

                ShareLink(item: "\(item.title) \(item.videoUrl)") {
                    Image(systemName: "square.and.arrow.up")
                        .font(.system(size: 22, weight: .semibold))
                        .frame(width: 44, height: 44)
                }
            }
            .foregroundStyle(.white)
        }
        .padding(.horizontal, 12)
        .padding(.top, 8)
        .padding(.bottom, 12)
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

    private func showToast(_ value: String) {
        withAnimation { toast = value }
        Task { @MainActor in
            try? await Task.sleep(for: .seconds(1.2))
            withAnimation { toast = nil }
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
