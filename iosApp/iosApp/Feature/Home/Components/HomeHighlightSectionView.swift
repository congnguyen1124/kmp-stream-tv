import Shared
import SwiftUI

struct HighlightWideSectionView: View {
    let section: HomeSectionUiModel
    let onSelect: (HomeContentUiModel) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            LazyHStack(spacing: 12) {
                ForEach(section.items, id: \.id) { item in
                    Button {
                        onSelect(item)
                    } label: {
                        HighlightWideCard(item: item)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(item.title)
                }
            }
            .scrollTargetLayout()
            .padding(.horizontal, 40)
        }
        .scrollTargetBehavior(.viewAligned)
    }
}

private struct HighlightWideCard: View {
    let item: HomeContentUiModel

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            RemoteArtwork(url: item.thumbnailUrl)
            LinearGradient(
                colors: [.black.opacity(0.88), .black.opacity(0.12), .clear],
                startPoint: .leading,
                endPoint: .trailing
            )
            LinearGradient(colors: [.clear, .black.opacity(0.58)], startPoint: .center, endPoint: .bottom)

            VStack(alignment: .leading, spacing: 7) {
                if let age = item.ageRestriction {
                    StreamBadge(text: age)
                }
                Text(item.title)
                    .font(.title3.bold())
                    .lineLimit(2)
                Text(item.description_)
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.82))
                    .lineLimit(2)
                Label("Watch now", systemImage: "play.fill")
                    .font(.caption.bold())
            }
            .frame(maxWidth: 230, alignment: .leading)
            .padding(16)
        }
        .foregroundStyle(.white)
        .frame(width: 340, height: 191)
        .clipShape(RoundedRectangle(cornerRadius: StreamMetrics.cornerRadius, style: .continuous))
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
        ZStack {
            if let backgroundUrl = activeItem?.thumbnailUrl ?? section.backgroundUrl {
                RemoteArtwork(url: backgroundUrl)
                    .blur(radius: 30)
                    .overlay(Color.black.opacity(0.58))
                    .clipped()
            }

            VStack(alignment: .leading, spacing: 14) {
                Text(section.title)
                    .font(.title3.bold())
                    .padding(.horizontal, StreamMetrics.contentInset)

                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(spacing: 14) {
                        ForEach(section.items, id: \.id) { item in
                            RemoteArtwork(url: item.thumbnailUrl)
                                .frame(width: 236, height: 354)
                                .clipShape(RoundedRectangle(cornerRadius: StreamMetrics.cornerRadius, style: .continuous))
                                .overlay {
                                    RoundedRectangle(cornerRadius: StreamMetrics.cornerRadius, style: .continuous)
                                        .stroke(item.id == activeItem?.id ? Color.white : .clear, lineWidth: 2)
                                }
                                .scaleEffect(item.id == activeItem?.id ? 1 : 0.92)
                                .animation(.easeOut(duration: 0.2), value: activeItemID)
                                .onTapGesture { activeItemID = item.id }
                                .accessibilityLabel(item.title)
                        }
                    }
                    .padding(.horizontal, 40)
                }

                if let activeItem {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(activeItem.title)
                            .font(.headline)
                        Text(activeItem.description_)
                            .font(.caption)
                            .foregroundStyle(.white.opacity(0.76))
                            .lineLimit(2)

                        HStack(spacing: 10) {
                            Button {
                                onSelect(activeItem)
                            } label: {
                                Label("Watch now", systemImage: "play.fill")
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(.white)
                            .foregroundStyle(.black)

                            Button {
                                feedbackMessage = "Added to Watch later"
                            } label: {
                                Label("Watch later", systemImage: "plus")
                            }
                            .buttonStyle(.bordered)

                            Button {
                                feedbackMessage = activeItem.description_
                            } label: {
                                Image(systemName: "info.circle")
                            }
                            .buttonStyle(.bordered)
                            .accessibilityLabel("Information")
                        }
                        .font(.caption.weight(.semibold))
                    }
                    .padding(.horizontal, StreamMetrics.contentInset)
                }
            }
            .padding(.vertical, 18)
        }
        .frame(maxWidth: .infinity)
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

    private var feedbackPresented: Binding<Bool> {
        Binding(
            get: { feedbackMessage != nil },
            set: { if !$0 { feedbackMessage = nil } }
        )
    }
}
