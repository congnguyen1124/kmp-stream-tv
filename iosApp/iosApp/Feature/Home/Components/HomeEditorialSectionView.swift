import Shared
import SwiftUI

struct StorySectionView: View {
    let section: HomeSectionUiModel
    let onSelect: (HomeContentUiModel) -> Void

    var body: some View {
        ZStack {
            Image("bg_story_layout")
                .resizable()
                .frame(maxWidth: .infinity)
                .frame(height: 356)
                .opacity(0.8)

            VStack(alignment: .leading, spacing: 0) {
                storyTitle

                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(spacing: 8) {
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
                .padding(.top, 4)
                .padding(.bottom, 32)
            }
        }
        .frame(height: 356)
        .overlay(alignment: .top) {
            LinearGradient(
                stops: [
                    .init(color: Color(hex: 0x6419DA, alpha: 0.1), location: 0),
                    .init(color: .streamAccent, location: 0.8),
                    .init(color: Color(hex: 0x6419DA, alpha: 0), location: 1),
                ],
                startPoint: .leading,
                endPoint: .trailing
            )
            .frame(height: 2)
            .padding(.vertical, 2)
        }
        .overlay(alignment: .bottom) {
            LinearGradient(
                stops: [
                    .init(color: .black, location: 0),
                    .init(color: .streamAccentBright, location: 0.3),
                    .init(color: .black, location: 1),
                ],
                startPoint: .leading,
                endPoint: .trailing
            )
            .frame(height: 2)
        }
    }

    private var storyTitle: some View {
        HStack(spacing: 4) {
            Image("ic_fire")
                .resizable()
                .frame(width: 16, height: 16)
            Text(section.title)
                .font(.streamSemiBold(16))
                .foregroundStyle(.white)
                .lineLimit(1)
        }
        .padding(.leading, 16)
        .padding(.trailing, 24)
        .frame(minWidth: 137, minHeight: 32, maxHeight: 32, alignment: .leading)
        .fixedSize(horizontal: true, vertical: false)
        .background {
            Image("bg_story_block")
                .resizable()
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
                    .opacity(0.35)
            }

            VStack(alignment: .leading, spacing: 6) {
                Image("ic_fire")
                    .resizable()
                    .frame(width: 24, height: 24)
                Text(section.title)
                    .font(.streamBold(18))
                    .foregroundStyle(.white)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(.leading, 24)
            .padding(.trailing, 12)
            .frame(width: 144, height: 241, alignment: .leading)

            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: 0) {
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
                .padding(.leading, 144)
                .padding(.trailing, 16)
            }
            .padding(.vertical, 16)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 241)
        .clipped()
    }
}

struct MiniAppsSectionView: View {
    let section: HomeSectionUiModel
    let onSelect: (HomeContentUiModel) -> Void

    var body: some View {
        HStack(spacing: 0) {
            ForEach(section.items.prefix(5), id: \.id) { item in
                Button {
                    onSelect(item)
                } label: {
                    VStack(spacing: 8) {
                        RemoteArtwork(url: item.thumbnailUrl)
                            .frame(width: 40, height: 40)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        Text(item.title)
                            .font(.streamMedium(12))
                            .foregroundStyle(Color.streamSecondaryText)
                            .lineLimit(2)
                            .multilineTextAlignment(.center)
                    }
                    .padding(4)
                    .frame(maxWidth: .infinity, alignment: .top)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(item.title)
            }
        }
        .padding(8)
        .frame(minHeight: 90)
        .background(Color(hex: 0x282828))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(
                    LinearGradient(
                        colors: [Color(hex: 0x434343), Color(hex: 0x282828)],
                        startPoint: .leading,
                        endPoint: .trailing
                    ),
                    lineWidth: 1
                )
        }
        .padding(16)
    }
}
