import SwiftUI

/// `activity_user_profile.xml` dimensions, reused as points.
private enum ProfileMetrics {
    static let topBarHeight: CGFloat = 56
    static let horizontalPadding: CGFloat = 20
    static let avatarSize: CGFloat = 64
    static let gapSize: CGFloat = 12
    static let actionMinHeight: CGFloat = 64
    static let actionIconSize: CGFloat = 32
    static let chevronSize: CGFloat = 24
}

/// `profile_divider`
private extension Color {
    static let profileDivider = Color(hex: 0xFFFFFF, alpha: 0.2)
}

/// `bg_profile_sign_in`
private extension LinearGradient {
    static let profileSignIn = LinearGradient(
        colors: [Color(hex: 0x0A13FE, alpha: 0.6), Color(hex: 0x483CDC, alpha: 0.6)],
        startPoint: .leading,
        endPoint: .trailing
    )
}

/// Signed-out personal profile, the port of `UserProfileActivity`.
///
/// Authentication, settings, legal and feedback destinations are not in the shared contract yet, so
/// every row stays visibly actionable and answers with explicit unavailable feedback rather than
/// doing nothing.
struct UserProfileView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var toast: String?

    private let items = ProfileCatalog.signedOut

    var body: some View {
        ZStack(alignment: .top) {
            Color.streamBackground.ignoresSafeArea()

            VStack(spacing: 0) {
                topBar

                ScrollView {
                    LazyVStack(spacing: 0) {
                        ForEach(items) { item in
                            row(item)
                        }
                    }
                    .padding(.top, StreamMetrics.cardSpacing)
                    .padding(.bottom, 24)
                }
                .scrollIndicators(.hidden)
            }

            if let toast {
                Text(toast)
                    .font(.streamRegular(14))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 16)
                    .frame(minHeight: 48)
                    .background(Color.black.opacity(0.88))
                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                    .padding(.horizontal, 24)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
                    .padding(.bottom, 32)
                    .transition(.opacity)
                    .allowsHitTesting(false)
            }
        }
        .preferredColorScheme(.dark)
    }

    // MARK: - Chrome

    private var topBar: some View {
        HStack(spacing: StreamMetrics.cardSpacing) {
            Button(action: dismiss.callAsFunction) {
                Image("ic_arrow_back")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .padding(4)
                    .frame(width: 44, height: 44)
                    .foregroundStyle(.white)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Back")

            Text("Personal profile")
                .font(.streamBold(18))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, StreamMetrics.cardSpacing)
        .frame(height: ProfileMetrics.topBarHeight)
    }

    // MARK: - Rows

    @ViewBuilder
    private func row(_ item: ProfileItem) -> some View {
        switch item {
        case .signIn:
            signInCard
        case .gap:
            Color.clear.frame(height: ProfileMetrics.gapSize)
        case .section(_, let actions):
            section(actions)
        }
    }

    /// `item_profile_signin.xml`
    private var signInCard: some View {
        VStack(spacing: 0) {
            HStack(alignment: .top, spacing: StreamMetrics.contentInset) {
                Image("ic_profile_placeholder")
                    .resizable()
                    .scaledToFill()
                    .frame(width: ProfileMetrics.avatarSize, height: ProfileMetrics.avatarSize)
                    .clipShape(Circle())
                    .accessibilityHidden(true)

                VStack(alignment: .leading, spacing: 10) {
                    Text("Hello!")
                        .font(.streamBold(20))
                    Text("Log in to sync your profile, activity and preferences across devices.")
                        .font(.streamMedium(14))
                        .fixedSize(horizontal: false, vertical: true)
                }
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity, alignment: .leading)
            }

            Button {
                showToast(ProfileCatalog.unavailable(ProfileCatalog.logIn))
            } label: {
                Text(ProfileCatalog.logIn)
                    .font(.streamSemiBold(16))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 24)
                    .frame(height: StreamMetrics.buttonHeight)
                    .background(
                        LinearGradient.streamPrimaryButton,
                        in: RoundedRectangle(cornerRadius: StreamMetrics.buttonRadius, style: .continuous)
                    )
            }
            .buttonStyle(.plain)
            .padding(.top, 24)
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 20)
        .background(
            LinearGradient.profileSignIn,
            in: RoundedRectangle(cornerRadius: StreamMetrics.radiusNormal, style: .continuous)
        )
        .padding(.horizontal, ProfileMetrics.horizontalPadding)
    }

    /// `item_profile_section.xml` with one `item_profile_action.xml` per action, whose
    /// inset stroke reads as a divider between the rows of a section.
    private func section(_ actions: [ProfileAction]) -> some View {
        VStack(spacing: 0) {
            ForEach(Array(actions.enumerated()), id: \.element.id) { index, action in
                if index > 0 {
                    Rectangle()
                        .fill(Color.profileDivider)
                        .frame(height: 1)
                }
                actionRow(action)
            }
        }
        .padding(.horizontal, StreamMetrics.contentInset)
        .overlay {
            RoundedRectangle(cornerRadius: StreamMetrics.radiusNormal, style: .continuous)
                .strokeBorder(Color.profileDivider, lineWidth: 1)
        }
        .padding(.horizontal, ProfileMetrics.horizontalPadding)
    }

    private func actionRow(_ action: ProfileAction) -> some View {
        Button {
            showToast(ProfileCatalog.unavailable(action.title))
        } label: {
            HStack(spacing: StreamMetrics.contentInset) {
                Image(action.icon)
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .frame(width: ProfileMetrics.actionIconSize, height: ProfileMetrics.actionIconSize)

                Text(action.title)
                    .font(.streamSemiBold(16))
                    .frame(maxWidth: .infinity, alignment: .leading)

                Image("ic_arrow_next")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .frame(width: ProfileMetrics.chevronSize, height: ProfileMetrics.chevronSize)
            }
            .foregroundStyle(.white)
            .frame(minHeight: ProfileMetrics.actionMinHeight)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(action.title)
    }

    private func showToast(_ message: String) {
        withAnimation { toast = message }
        Task { @MainActor in
            try? await Task.sleep(for: .seconds(2))
            guard toast == message else { return }
            withAnimation { toast = nil }
        }
    }
}
