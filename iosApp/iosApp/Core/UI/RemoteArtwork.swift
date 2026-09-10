import SwiftUI

struct RemoteArtwork: View {
    let url: String

    var body: some View {
        AsyncImage(url: URL(string: url)) { phase in
            switch phase {
            case .success(let image):
                image
                    .resizable()
                    .scaledToFill()
            case .failure:
                placeholder(systemImage: "photo")
            default:
                placeholder(systemImage: nil)
                    .overlay {
                        ProgressView()
                            .tint(.white.opacity(0.7))
                    }
            }
        }
        .clipped()
    }

    private func placeholder(systemImage: String?) -> some View {
        Color.streamSurface.overlay {
            if let systemImage {
                Image(systemName: systemImage)
                    .font(.title2)
                    .foregroundStyle(.white.opacity(0.7))
            }
        }
    }
}
