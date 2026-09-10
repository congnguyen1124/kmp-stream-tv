import SwiftUI

/// The Android thumbnails sit on one of two placeholder drawables while artwork resolves.
enum ArtworkPlaceholder {
    /// `bg_placeholder` / `image_placeholder` / `image_placeholder_provider`
    case plain
    /// `image_placeholder_rounded` / `image_placeholder_vertical_large`
    case branded
}

/// `centerCrop` artwork. The image is drawn inside a layout-neutral base so a fill-scaled photo can
/// never widen its container, which the Home carousels rely on to size themselves from the viewport.
struct RemoteArtwork: View {
    let url: String
    var placeholder: ArtworkPlaceholder = .plain

    var body: some View {
        Color.clear
            .overlay {
                AsyncImage(url: URL(string: url)) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                    default:
                        placeholderView
                    }
                }
            }
            .clipped()
    }

    @ViewBuilder
    private var placeholderView: some View {
        switch placeholder {
        case .plain:
            Color.streamSurface
        case .branded:
            Color.streamSurface.overlay {
                Image("image_logo_place_holder")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 44, height: 44)
            }
        }
    }
}
