import Shared
import SwiftUI

struct StorySectionView: View {
    let section: HomeSectionUiModel
    let onSelect: (HomeContentUiModel) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(section.title, systemImage: "flame.fill")
                .font(.title3.bold())
                .padding(.horizontal, StreamMetrics.contentInset)
                .padding(.vertical, 10)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.streamAccent.opacity(0.76))

            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: StreamMetrics.cardSpacing) {
                    ForEach(section.items, id: \.id) { item in
                        Button {
                            onSelect(item)
                        } label: {
                            HomeContentCard(item: item, style: .story)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(item.title)
                    }
                }
                .padding(.horizontal, StreamMetrics.contentInset)
            }
        }
        .padding(.bottom, 16)
        .background {
            LinearGradient(
                colors: [.streamAccent.opacity(0.32), .streamBackground],
                startPoint: .top,
                endPoint: .bottom
            )
        }
    }
}

struct TopTenSectionView: View {
    let section: HomeSectionUiModel
    let onSelect: (HomeContentUiModel) -> Void

    var body: some View {
        ZStack(alignment: .leading) {
            if let backgroundUrl = section.backgroundUrl {
                RemoteArtwork(url: backgroundUrl)
                    .blur(radius: 18)
                    .overlay(Color.black.opacity(0.68))
            }

            VStack(alignment: .leading, spacing: 12) {
                Text(section.title)
                    .font(.title3.bold())
                    .padding(.horizontal, StreamMetrics.contentInset)

                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(spacing: 10) {
                        ForEach(Array(section.items.enumerated()), id: \.element.id) { index, item in
                            Button {
                                onSelect(item)
                            } label: {
                                HomeContentCard(item: item, style: .topTen, rank: index + 1)
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel("Number \(index + 1), \(item.title)")
                        }
                    }
                    .padding(.horizontal, StreamMetrics.contentInset)
                }
            }
            .padding(.vertical, 18)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 273)
        .clipped()
    }
}

struct MiniAppsSectionView: View {
    let section: HomeSectionUiModel
    let onSelect: (HomeContentUiModel) -> Void

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 8), count: 5)

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(section.title)
                .font(.title3.bold())

            LazyVGrid(columns: columns, alignment: .center, spacing: 12) {
                ForEach(section.items.prefix(5), id: \.id) { item in
                    Button {
                        onSelect(item)
                    } label: {
                        VStack(spacing: 7) {
                            RemoteArtwork(url: item.thumbnailUrl)
                                .frame(height: 48)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                            Text(item.title)
                                .font(.caption2.weight(.semibold))
                                .lineLimit(2)
                                .multilineTextAlignment(.center)
                        }
                        .foregroundStyle(.white)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(item.title)
                }
            }
        }
        .padding(16)
        .background(Color.streamSurface, in: RoundedRectangle(cornerRadius: StreamMetrics.cornerRadius, style: .continuous))
        .padding(.horizontal, StreamMetrics.contentInset)
    }
}
