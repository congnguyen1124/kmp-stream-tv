import SwiftUI

struct PlaceholderView: View {
    let title: String
    let message: String
    let systemImage: String

    var body: some View {
        ZStack {
            Color.streamBackground.ignoresSafeArea()

            VStack(spacing: 16) {
                Image(systemName: systemImage)
                    .font(.system(size: 42, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 84, height: 84)
                    .background(Color.streamSurface, in: Circle())

                Text(title)
                    .font(.title.bold())

                Text(message)
                    .font(.body)
                    .foregroundStyle(Color.streamSecondaryText)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: 320)
            }
            .padding(32)
        }
    }
}
