import SwiftUI

/// Port of `PlayerSettingType`.
enum PlayerSettingType: String, Identifiable, CaseIterable {
    case video
    case audio
    case subtitle
    case speed

    var id: String { rawValue }

    var title: String {
        switch self {
        case .video: "Video quality"
        case .audio: "Audio"
        case .subtitle: "Subtitle"
        case .speed: "Speed"
        }
    }
}

private struct PlayerSettingOption: Identifiable {
    let title: String
    let value: String
    let isSelected: Bool

    var id: String { value }
}

/// Full-screen settings sheet, ported from `PlayerSettingsView.kt`: one column per requested type,
/// a check mark on the active option, and a close button in the top-trailing corner.
struct PlayerSettingsSheet: View {
    let types: [PlayerSettingType]
    let state: StreamPlayerState
    let speed: Float
    let onSelect: (PlayerSettingType, String) -> Void
    let onDismiss: () -> Void

    private static let speeds: [Float] = [0.5, 1, 1.5, 2]

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color.black

            HStack(alignment: .top, spacing: 20) {
                ForEach(types) { type in
                    column(type)
                }
            }
            .padding(PlayerMetrics.settingLayoutPadding)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)

            Button(action: onDismiss) {
                Image("ic_player_close")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .frame(width: PlayerMetrics.buttonIconSize, height: PlayerMetrics.buttonIconSize)
                    .padding(PlayerMetrics.controllerPaddingMedium)
                    .foregroundStyle(.white)
            }
            .buttonStyle(.plain)
            .padding(.top, 8)
            .padding(.trailing, 8)
            .accessibilityLabel("Close settings")
        }
        .transition(.opacity)
    }

    private func column(_ type: PlayerSettingType) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(type.title)
                .font(.streamBold(18))
                .foregroundStyle(.white)

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(options(for: type)) { option in
                        Button {
                            onSelect(type, option.value)
                        } label: {
                            HStack(spacing: 8) {
                                Image("ic_player_check")
                                    .renderingMode(.template)
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 16, height: 16)
                                    .foregroundStyle(.white)
                                    .opacity(option.isSelected ? 1 : 0)

                                Text(option.title)
                                    .font(option.isSelected ? .streamSemiBold(16) : .streamMedium(14))
                                    .foregroundStyle(
                                        option.isSelected ? Color.white : Color.streamSecondaryText
                                    )

                                Spacer(minLength: 0)
                            }
                            .frame(minHeight: 48)
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(.top, PlayerMetrics.settingContentPadding)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func options(for type: PlayerSettingType) -> [PlayerSettingOption] {
        switch type {
        case .speed:
            Self.speeds.map {
                PlayerSettingOption(title: $0.speedLabel, value: String($0), isSelected: $0 == speed)
            }

        case .video:
            videoOptions

        case .audio:
            state.audioTracks.map {
                PlayerSettingOption(title: $0.label, value: $0.id, isSelected: $0.isSelected)
            }

        case .subtitle:
            [
                PlayerSettingOption(
                    title: "No subtitle",
                    value: StreamMediaTrack.offId,
                    isSelected: !state.textTracks.contains { $0.isSelected }
                ),
            ] + state.textTracks.map {
                PlayerSettingOption(title: $0.label, value: $0.id, isSelected: $0.isSelected)
            }
        }
    }

    private var videoOptions: [PlayerSettingOption] {
        let tracks = state.videoTracks
        let isAuto = tracks.filter(\.isSelected).count != 1
        return [
            PlayerSettingOption(title: "Auto", value: StreamVideoTrack.autoId, isSelected: isAuto),
        ] + tracks.map { track in
            let kbps = track.bitrate / 1_000
            var label = track.height > 0 ? "\(track.height)p" : "Unknown"
            if kbps > 0 { label += " · \(kbps) Kbps" }
            return PlayerSettingOption(
                title: label,
                value: track.id,
                isSelected: track.isSelected && !isAuto
            )
        }
    }
}

extension Float {
    /// `1.0` reads as `1x`, `1.5` as `1.5x` — the `displaySpeed` helper in `PlayerSettingsView`.
    var speedLabel: String {
        self == Float(Int(self)) ? "\(Int(self))x" : "\(self)x"
    }
}
