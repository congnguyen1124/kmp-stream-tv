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
            default:
                Color.streamSurface
            }
        }
        .clipped()
    }
}
