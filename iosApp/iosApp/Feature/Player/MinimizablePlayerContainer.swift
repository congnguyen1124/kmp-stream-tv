import SwiftUI

/// Hosts the detail, fullscreen and floating mini-player layouts of one player overlay.
///
/// Port of `MinimizableView.kt`. The hierarchy it drives, mapped to the Android view:
///
/// | This view | `MinimizableView` |
/// | --- | --- |
/// | `detailLayer` | `detailContent`, faded by `bottomContentAlpha` |
/// | `card` | `playerCard`: width fraction, edge padding, offset, top-trailing alignment |
/// | `cardContent` | `playerCardContent`, what the scale scales, clips and shadows |
/// | `player()` | `playerSurface`, the 16:9 video box |
/// | `footer()` | `miniPlaybackController` |
///
/// The padded card and the scaled content stay two layers, not one: `padding` applied *after*
/// `scaleEffect` sits outside the scaled content, which is what makes the `edgePadding` and
/// corner-snapping arithmetic in [MinimizableState] land where it does.
struct MinimizablePlayerContainer<Player: View, Footer: View, Detail: View>: View {
    @ObservedObject var state: MinimizableState
    let presentation: PlayerPresentation
    let isSystemPictureInPicture: Bool
    /// Height of the app chrome the mini player has to stay clear of at the bottom of the host.
    let appBottomBarHeight: CGFloat
    let onMinimizedChanged: (Bool) -> Void

    @ViewBuilder let player: () -> Player
    @ViewBuilder let footer: () -> Footer
    @ViewBuilder let detail: () -> Detail

    /// Pan and pinch are reported cumulatively by SwiftUI but consumed as deltas by the state,
    /// exactly as `TransformGestureDetector` feeds `onGestureZoom` on Android.
    @State private var lastPan: CGSize = .zero
    @State private var lastMagnification: CGFloat = 1
    @State private var isDraggingDown = false

    private var isFillingHost: Bool {
        presentation == .fullscreen || isSystemPictureInPicture
    }

    private var showsFooter: Bool {
        !isSystemPictureInPicture && (state.isMiniSizeReached || state.isMinimized)
    }

    /// Corner radius, border and shadow come in with the shrink, and the radius is divided by the
    /// scale so the painted corner holds at one value however far the card is pinched.
    private var isDecorated: Bool {
        state.isMiniSizeReached || state.isMinimized
    }

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .topTrailing) {
                if isFillingHost {
                    player()
                        .frame(width: geometry.size.width, height: geometry.size.height)
                } else {
                    detailLayer(host: geometry.size)
                    card(host: geometry.size)
                }
            }
            .frame(width: geometry.size.width, height: geometry.size.height)
            .onAppear { configure(host: geometry.size) }
            .onChange(of: geometry.size) { _, size in configure(host: size) }
            .onChange(of: appBottomBarHeight) { _, _ in configure(host: geometry.size) }
        }
    }

    private func configure(host: CGSize) {
        state.configure(
            hostSize: host,
            borderPadding: PlayerMetrics.miniBorderPadding,
            footerHeight: PlayerMetrics.miniFooterHeight,
            miniPlayerMaxWidth: PlayerMetrics.miniMaxWidth,
            appBottomBarHeight: appBottomBarHeight,
            playerRatio: PlayerMetrics.playerRatio
        )
    }

    // MARK: - Detail layer

    /// `detailContent` reserves the player's 16:9 strip rather than laying the player out inside
    /// it, because the player is a sibling that floats over this content and travels away from it.
    private func detailLayer(host: CGSize) -> some View {
        VStack(spacing: 0) {
            Color.clear
                .frame(width: host.width, height: host.width / PlayerMetrics.playerRatio)

            Rectangle()
                .fill(Color.streamSurface)
                .frame(height: 1)

            detail()
        }
        .frame(width: host.width, height: host.height, alignment: .top)
        .background(Color.streamBackground)
        .opacity(state.bottomContentAlpha)
        .allowsHitTesting(!state.isMinimized && state.bottomContentAlpha > 0)
    }

    // MARK: - Card

    private func card(host: CGSize) -> some View {
        withCardGestures(cardBody(host: host))
    }

    private func cardBody(host: CGSize) -> some View {
        let cornerRadius = PlayerMetrics.miniCornerRadius / state.zoomScale

        return VStack(spacing: 0) {
            player()
                .aspectRatio(PlayerMetrics.playerRatio, contentMode: .fit)
                .background(Color.black)

            if showsFooter {
                footer()
                    .frame(height: state.footerHeightWithScale)
                    .transition(.opacity)
            }
        }
        .clipShape(
            RoundedRectangle(cornerRadius: isDecorated ? cornerRadius : 0, style: .continuous)
        )
        .overlay {
            if isDecorated {
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .strokeBorder(Color.white.opacity(0.1), lineWidth: PlayerMetrics.miniBorderWidth)
            }
        }
        .shadow(
            color: .black.opacity(isDecorated ? 0.35 : 0),
            radius: PlayerMetrics.miniElevation * 2,
            y: PlayerMetrics.miniElevation
        )
        .scaleEffect(state.zoomScale, anchor: .center)
        .padding(state.edgePadding)
        .frame(width: host.width * state.widthFraction, alignment: .top)
        .offset(x: state.offsetX, y: state.offsetY)
        .animation(.easeInOut(duration: PlayerMetrics.transitionDuration), value: showsFooter)
    }

    // MARK: - Gestures

    /// The two branches of `MinimizableView.handleGesture`: while expanded the card follows a
    /// downward drag, while minimized it pans, pinches and toggles on tap.
    ///
    /// Attached conditionally rather than as one gesture, because the expanded card must let a tap
    /// fall through to the controller's own tap-to-toggle mask inside `player()`.
    @ViewBuilder
    private func withCardGestures(_ content: some View) -> some View {
        if state.isMinimized {
            content
                .gesture(transformGesture)
                .onTapGesture(count: 2) { state.onDoubleTap() }
                .onTapGesture(count: 1) { toggleMinimized() }
        } else {
            content.gesture(dragDownGesture)
        }
    }

    /// `detectVerticalDragGestures` on the expanded player: follow the finger down, and toggle on
    /// release at whatever depth the drag reached.
    ///
    /// Downward only — an upward drag has nowhere to travel, because `onVerticalDragging` clamps
    /// the offset at 0.
    private var dragDownGesture: some Gesture {
        DragGesture(minimumDistance: 8)
            .onChanged { value in
                if !isDraggingDown {
                    let height = value.translation.height
                    guard height > 0, height > abs(value.translation.width) else { return }
                    isDraggingDown = true
                    lastPan = .zero
                }
                guard isDraggingDown else { return }
                state.onVerticalDragging(value.translation.height - lastPan.height)
                lastPan = value.translation
            }
            .onEnded { _ in
                let wasDragging = isDraggingDown
                isDraggingDown = false
                lastPan = .zero
                if wasDragging { toggleMinimized() }
            }
    }

    /// `TransformGestureDetector`: pan and pinch arrive together, and the release snaps the card
    /// into the nearest corner at the scale the pinch settled on.
    private var transformGesture: some Gesture {
        SimultaneousGesture(
            DragGesture(minimumDistance: 2),
            MagnifyGesture(minimumScaleDelta: 0)
        )
        .onChanged { value in
            let pan = value.first?.translation ?? lastPan
            let magnification = value.second?.magnification ?? lastMagnification
            let panDelta = CGSize(
                width: pan.width - lastPan.width,
                height: pan.height - lastPan.height
            )
            let zoomDelta = lastMagnification == 0 ? 1 : magnification / lastMagnification
            lastPan = pan
            lastMagnification = magnification

            // `onGestureZoom` multiplies the pan by the scale it is heading to, because the
            // detector it was written for sits inside the scaled layer. This gesture sits above
            // the scale, so its pan is already in host points and has to be divided back before
            // that multiplication.
            state.onGestureZoom(
                zoom: zoomDelta,
                panX: panDelta.width / state.zoomScale,
                panY: panDelta.height / state.zoomScale
            )
        }
        .onEnded { _ in
            lastPan = .zero
            lastMagnification = 1
            state.snapToCorner()
        }
    }

    private func toggleMinimized() {
        state.toggleMinimized()
        onMinimizedChanged(state.isMinimized)
    }
}
