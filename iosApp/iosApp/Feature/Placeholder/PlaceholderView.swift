import SwiftUI

struct PlaceholderView: View {
    let title: String
    let message: String

    var body: some View {
        ZStack {
            Color.streamBackground.ignoresSafeArea()

            VStack(spacing: 0) {
                Text("S")
                    .font(.streamBold(26))
                    .foregroundStyle(Color.streamAccent)
                    .frame(width: 64, height: 64)
                    .background(Color.streamSurface)
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                    .overlay {
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .stroke(Color.streamAccent.opacity(0.2), lineWidth: 1)
                    }

                Text(title)
                    .font(.streamBold(24))
                    .padding(.top, 20)

                Text(message)
                    .font(.streamRegular(15))
                    .foregroundStyle(Color.streamSecondaryText)
                    .multilineTextAlignment(.center)
                    .padding(.top, 8)
                    .frame(maxWidth: .infinity)
            }
            .padding(32)
        }
    }
}
