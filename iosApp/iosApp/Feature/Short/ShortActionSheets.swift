import Shared
import SwiftUI

/// `bg_short_bottom_sheet` is the app background; the sheets inherit it here.
private extension View {
    func shortSheetSurface() -> some View {
        presentationBackground(Color.streamBackground)
            .preferredColorScheme(.dark)
    }
}

// MARK: - Comments

/// `fragment_short_comments.xml`: a titled full sheet over the locally submitted comments, with a
/// send row that rejects blank text.
struct ShortCommentSheet: View {
    let item: ShortItemUiModel
    let comments: [String]
    let onSubmit: (String) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var draft = ""
    @FocusState private var isInputFocused: Bool

    var body: some View {
        VStack(spacing: 0) {
            header

            Divider().overlay(Color.white.opacity(0.2))

            ScrollView {
                VStack(alignment: .leading, spacing: 8) {
                    if comments.isEmpty {
                        Text("No comments are loaded yet. Add one locally.")
                            .font(.streamRegular(14))
                            .foregroundStyle(Color.streamSecondaryText)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 24)
                    } else {
                        ForEach(Array(comments.enumerated()), id: \.offset) { _, comment in
                            Text(comment)
                                .font(.streamRegular(14))
                                .foregroundStyle(.white)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                    }
                }
                .padding(16)
            }
            .scrollIndicators(.hidden)
            .frame(maxHeight: .infinity)

            Divider().overlay(Color.white.opacity(0.2))

            inputRow
        }
        .background(Color.streamBackground)
        .shortSheetSurface()
    }

    private var header: some View {
        ZStack {
            Text("Comments (\(item.commentCount))")
                .font(.streamSemiBold(16))
                .foregroundStyle(.white)

            Button(action: dismiss.callAsFunction) {
                Image("ic_player_close")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .padding(12)
                    .frame(width: 48, height: 48)
                    .foregroundStyle(.white)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .frame(maxWidth: .infinity, alignment: .trailing)
            .accessibilityLabel("Close comments")
        }
        .frame(height: 56)
    }

    private var inputRow: some View {
        HStack(spacing: 8) {
            TextField("", text: $draft, prompt: commentPrompt, axis: .vertical)
                .font(.streamRegular(14))
                .foregroundStyle(.white)
                .lineLimit(1...3)
                .focused($isInputFocused)
                .submitLabel(.send)
                .onSubmit(submit)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Color.streamSurface, in: Capsule())

            Button(action: submit) {
                Text("Send")
                    .font(.streamSemiBold(16))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 16)
                    .frame(height: 40)
                    .background(
                        LinearGradient.streamPrimaryButton,
                        in: RoundedRectangle(cornerRadius: StreamMetrics.buttonRadius, style: .continuous)
                    )
            }
            .buttonStyle(.plain)
            .disabled(isDraftBlank)
            .opacity(isDraftBlank ? 0.38 : 1)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }

    private var commentPrompt: Text {
        Text("Write a comment").foregroundStyle(Color.streamSecondaryText)
    }

    private var isDraftBlank: Bool {
        draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private func submit() {
        guard !isDraftBlank else { return }
        onSubmit(draft)
        draft = ""
        dismiss()
    }
}

// MARK: - More

/// `fragment_short_actions.xml`
enum ShortMoreAction: String, Identifiable, CaseIterable {
    case copyLink
    case notInterested
    case report

    var id: String { rawValue }

    var title: String {
        switch self {
        case .copyLink: "Copy link"
        case .notInterested: "Not interested"
        case .report: "Report"
        }
    }

    var icon: String {
        switch self {
        case .copyLink: "ic_link"
        case .notInterested: "ic_heart_broken"
        case .report: "ic_annotation_info"
        }
    }
}

/// The chosen action is reported and then run once the sheet has finished dismissing, so a Report
/// confirmation is never raised while this sheet is still on screen.
struct ShortMoreSheet: View {
    let onAction: (ShortMoreAction) -> Void

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 0) {
            ForEach(ShortMoreAction.allCases) { action in
                Button {
                    onAction(action)
                    dismiss()
                } label: {
                    HStack(spacing: 16) {
                        Image(action.icon)
                            .renderingMode(.template)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 24, height: 24)

                        Text(action.title)
                            .font(.streamSemiBold(16))

                        Spacer(minLength: 0)
                    }
                    .foregroundStyle(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 16)
                    .frame(minHeight: 44)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.top, 8)
        .padding(.bottom, 24)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.streamBackground)
        .presentationDetents([.height(220)])
        .shortSheetSurface()
    }
}

// MARK: - Provider

/// The provider dialog: every loaded short by this provider plus a follow action.
struct ShortProviderSheet: View {
    let item: ShortItemUiModel
    let shorts: [ShortItemUiModel]
    let onSelect: (ShortItemUiModel) -> Void
    let onToggleFollow: () -> Void

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                RemoteArtwork(url: item.providerAvatarUrl)
                    .frame(width: 48, height: 48)
                    .clipShape(Circle())

                Text(item.providerName)
                    .font(.streamBold(18))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Button {
                    dismiss()
                    onToggleFollow()
                } label: {
                    Text(item.isFollowingProvider ? "Following" : "Follow")
                        .font(.streamSemiBold(16))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 16)
                        .frame(height: 40)
                        .background(
                            item.isFollowingProvider
                                ? AnyShapeStyle(Color.streamSurface)
                                : AnyShapeStyle(LinearGradient.streamPrimaryButton),
                            in: RoundedRectangle(cornerRadius: StreamMetrics.buttonRadius, style: .continuous)
                        )
                }
                .buttonStyle(.plain)
            }
            .padding(16)

            Divider().overlay(Color.white.opacity(0.2))

            ScrollView {
                VStack(spacing: 0) {
                    ForEach(shorts, id: \.id) { short in
                        Button {
                            dismiss()
                            onSelect(short)
                        } label: {
                            Text(short.title)
                                .font(.streamRegular(14))
                                .foregroundStyle(.white)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(.horizontal, 16)
                                .frame(minHeight: 44)
                                .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.vertical, 8)
            }
            .scrollIndicators(.hidden)
        }
        .background(Color.streamBackground)
        .presentationDetents([.medium])
        .shortSheetSurface()
    }
}

// MARK: - Search

/// `showShortSearch`: a titled prompt whose result scrolls to the first loaded match.
struct ShortSearchSheet: View {
    let onSearch: (String) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var query = ""
    @FocusState private var isFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Search short videos")
                .font(.streamBold(18))
                .foregroundStyle(.white)

            TextField("", text: $query, prompt: prompt)
                .font(.streamRegular(14))
                .foregroundStyle(.white)
                .focused($isFocused)
                .submitLabel(.search)
                .onSubmit(submit)
                .padding(.horizontal, 12)
                .frame(height: 44)
                .background(Color.streamSurface, in: Capsule())

            HStack(spacing: 12) {
                Button("Cancel", action: dismiss.callAsFunction)
                    .font(.streamSemiBold(16))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: StreamMetrics.buttonHeight)

                Button(action: submit) {
                    Text("Search")
                        .font(.streamSemiBold(16))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: StreamMetrics.buttonHeight)
                        .background(
                            LinearGradient.streamPrimaryButton,
                            in: RoundedRectangle(cornerRadius: StreamMetrics.buttonRadius, style: .continuous)
                        )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(Color.streamBackground)
        .presentationDetents([.height(220)])
        .shortSheetSurface()
        .onAppear { isFocused = true }
    }

    private var prompt: Text {
        Text("Title or provider").foregroundStyle(Color.streamSecondaryText)
    }

    private func submit() {
        dismiss()
        onSearch(query)
    }
}
