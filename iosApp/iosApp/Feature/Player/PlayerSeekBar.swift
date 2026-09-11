import SwiftUI

/// The scrubber under the player, ported from the `seekProgress` `SeekBar` and its
/// `player_progress` layer-list: a track, a buffered bar, an elapsed bar and a round thumb.
///
/// Scrubbing reports continuously so the elapsed label can follow the finger (`fromUser` in
/// `onProgressChanged`) and commits once on release (`onStopTrackingTouch`).
struct PlayerSeekBar: View {
    let position: TimeInterval
    let buffered: TimeInterval
    let duration: TimeInterval
    let onScrubChanged: (TimeInterval) -> Void
    let onScrubEnded: (TimeInterval) -> Void

    private static let thumbSize: CGFloat = 12
    private static let hitHeight: CGFloat = 28

    var body: some View {
        GeometryReader { geometry in
            let width = geometry.size.width
            // The thumb travels between its own half-widths, so it never hangs off either end of
            // the track the way a raw `0…width` offset would.
            let travel = max(width - Self.thumbSize, 0)
            let progress = fraction(position)
            let bufferedFraction = fraction(buffered)

            ZStack(alignment: .leading) {
                Capsule()
                    .fill(Color(white: 0.32))
                    .frame(height: PlayerMetrics.progressHeight)

                Capsule()
                    .fill(Color.streamSecondaryText)
                    .frame(width: width * bufferedFraction, height: PlayerMetrics.progressHeight)

                Capsule()
                    .fill(Color.white)
                    .frame(width: width * progress, height: PlayerMetrics.progressHeight)

                Circle()
                    .fill(Color.white)
                    .frame(width: Self.thumbSize, height: Self.thumbSize)
                    .offset(x: travel * progress)
            }
            .frame(height: Self.hitHeight)
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        onScrubChanged(time(at: value.location.x, width: width))
                    }
                    .onEnded { value in
                        onScrubEnded(time(at: value.location.x, width: width))
                    }
            )
        }
        .frame(height: Self.hitHeight)
        .accessibilityElement()
        .accessibilityLabel("Seek")
        .accessibilityValue(position.playerTimestamp)
    }

    private func fraction(_ value: TimeInterval) -> CGFloat {
        guard duration > 0 else { return 0 }
        return min(max(value / duration, 0), 1)
    }

    private func time(at x: CGFloat, width: CGFloat) -> TimeInterval {
        guard width > 0, duration > 0 else { return 0 }
        return min(max(x / width, 0), 1) * duration
    }
}
