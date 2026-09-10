import Shared
import SwiftUI

struct HighlightWideSectionView: View {
    let section: HomeSectionUiModel
    let onSelect: (HomeContentUiModel) -> Void

    var body: some View {
        GeometryReader { geometry in
            let width = min(340, max(0, geometry.size.width - 80))
            let height = width * 191 / 340
            let edgePadding = max(40, (geometry.size.width - 340) / 2)

            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: 8) {
                    ForEach(section.items, id: \.id) { item in
                        Button {
                            onSelect(item)
                        } label: {
                            RemoteArtwork(url: item.thumbnailUrl)
                                .frame(width: width, height: height)
                                .background(Color.streamSurface)
                                .clipShape(
                                    RoundedRectangle(
                                        cornerRadius: StreamMetrics.thumbnailCorner,
                                        style: .continuous
                                    )
                                )
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(item.title)
                        .scrollTransition(.interactive, axis: .horizontal) { content, phase in
                            content.scaleEffect(1 - (0.15 * abs(phase.value)))
                        }
                    }
                }
                .scrollTargetLayout()
                .frame(height: 191, alignment: .top)
                .padding(.horizontal, edgePadding)
            }
            .scrollTargetBehavior(.viewAligned(limitBehavior: .always))
            .padding(.vertical, 24)
        }
        .frame(height: 239)
    }
}

struct HighlightTallSectionView: View {
    let section: HomeSectionUiModel
    let onSelect: (HomeContentUiModel) -> Void

    @State private var activeItemID: String?
    @State private var feedbackMessage: String?

    private var activeItem: HomeContentUiModel? {
        section.items.first(where: { $0.id == activeItemID }) ?? section.items.first
    }

    var body: some View {
        GeometryReader { geometry in
            let width = min(294, max(0, geometry.size.width - 80))
            let height = width * 441 / 294
            let edgePadding = max(40, (geometry.size.width - 294) / 2)

            ZStack(alignment: .top) {
                background

                VStack(spacing: 0) {
                    ScrollView(.horizontal, showsIndicators: false) {
                        LazyHStack(spacing: 8) {
                            ForEach(section.items, id: \.id) { item in
                                Button {
                                    onSelect(item)
                                } label: {
                                    RemoteArtwork(url: item.thumbnailUrl)
                                        .frame(width: width, height: height)
                                        .background(Color.streamSurface)
                                        .clipShape(
                                            RoundedRectangle(
                                                cornerRadius: StreamMetrics.thumbnailCorner,
                                                style: .continuous
                                            )
                                        )
                                }
                                .buttonStyle(.plain)
                                .id(item.id)
                                .accessibilityLabel(item.title)
                                .scrollTransition(.interactive, axis: .horizontal) { content, phase in
                                    content.scaleEffect(1 - (0.15 * abs(phase.value)))
                                }
                            }
                        }
                        .scrollTargetLayout()
                        .frame(height: 441, alignment: .top)
                        .padding(.horizontal, edgePadding)
                    }
                    .scrollPosition(id: $activeItemID, anchor: .center)
                    .scrollTargetBehavior(.viewAligned(limitBehavior: .always))
                    .padding(.top, 24)

                    actionBar
                        .padding(.top, 32)

                    Color.clear.frame(height: 20)
                }
            }
        }
        .frame(height: 573)
        .onAppear {
            if activeItemID == nil {
                activeItemID = section.items.first?.id
            }
        }
        .alert("StreamTV", isPresented: feedbackPresented) {
            Button("OK", role: .cancel) { feedbackMessage = nil }
        } message: {
            Text(feedbackMessage ?? "")
        }
    }

    private var background: some View {
        ZStack {
            if let backgroundUrl = activeItem?.thumbnailUrl ?? section.backgroundUrl {
                RemoteArtwork(url: backgroundUrl)
                    .opacity(0.8)
            }

            LinearGradient(
                stops: [
                    .init(color: .streamBackground, location: 0),
                    .init(color: .clear, location: 0.18),
                    .init(color: .clear, location: 0.68),
                    .init(color: .streamBackground, location: 1),
                ],
                startPoint: .top,
                endPoint: .bottom
            )
        }
        .clipped()
    }

    private var actionBar: some View {
        HStack(spacing: 0) {
            actionButton(title: "Watch later", icon: "ic_playlist_plus") {
                feedbackMessage = "Added to Watch later"
            }

            Button {
                if let activeItem {
                    onSelect(activeItem)
                }
            } label: {
                HStack(spacing: 8) {
                    Image("ic_play_round")
                        .resizable()
                        .frame(width: 24, height: 24)
                    Text("Watch now")
                        .font(.streamSemiBold(16))
                }
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 48)
                .background(Color.streamAccent)
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            }
            .buttonStyle(.plain)
            .padding(.horizontal, 4)

            actionButton(title: "Information", icon: "ic_info_circle") {
                feedbackMessage = activeItem?.description_ ?? ""
            }
        }
        .frame(height: 56)
    }

    private func actionButton(title: String, icon: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(icon)
                    .resizable()
                    .frame(width: 24, height: 24)
                Text(title)
                    .font(.streamSemiBold(12))
                    .foregroundStyle(Color.streamSecondaryText)
                    .lineLimit(1)
            }
            .frame(minWidth: 92, minHeight: 56)
        }
        .buttonStyle(.plain)
    }

    private var feedbackPresented: Binding<Bool> {
        Binding(
            get: { feedbackMessage != nil },
            set: { if !$0 { feedbackMessage = nil } }
        )
    }
}
