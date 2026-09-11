import SwiftUI

/// Geometry of the shrink from a host-filling player to a floating mini player.
///
/// Direct port of `MinimizableViewState.kt`, which is itself a port of the `ottclouds-android`
/// composable. Every resting position, travel range and scale ceiling below is the arithmetic of
/// that class; the comments that explain *why* a term is shaped the way it is are kept, because
/// the reasoning is what makes the corner snapping land on the same pixel on both platforms.
///
/// Two things differ from the Kotlin, and only because the host is SwiftUI rather than a
/// `ViewGroup`:
///
/// - **No `FloatAnimatable`.** Every stored value is `@Published` and every animated transition is
///   wrapped in `withAnimation`, so SwiftUI interpolates the view modifiers that read them. The
///   stored value itself reaches its target immediately — which is what the Kotlin's `isMinimized`
///   already did, and the only place the difference is observable is `isMiniSizeReached`, read
///   solely to decide whether the card wears its border.
/// - **Dimensions are points, not pixels.**
@MainActor
final class MinimizableState: ObservableObject {
    static let minScale: CGFloat = 1
    /// `player_transition_duration`.
    static let animationDuration: TimeInterval = 0.2

    private static let fullSizeProgress: CGFloat = 0
    private static let miniSizeProgress: CGFloat = 1
    private static let epsilon: CGFloat = 1e-2
    private static let alphaBoost: CGFloat = 3

    @Published private(set) var isMinimized = false

    /// Animated, so a settle can move the drawn scale and the snapped position together.
    @Published private(set) var zoomScale: CGFloat = MinimizableState.minScale
    @Published private(set) var offsetX: CGFloat = 0
    @Published private(set) var offsetY: CGFloat = 0

    /// How far the shrink from host-filling player to mini player has come: `fullSizeProgress` is
    /// the host-filling player, `miniSizeProgress` is the mini player.
    ///
    /// Its own value rather than `offsetY / availableHeightForDragging`, because those two only
    /// agree while the finger is down. `isMinimized` flips the moment the finger lifts, at whatever
    /// depth the drag reached, so the shrink still has to finish afterwards — and once minimized
    /// the player travels the whole host height between corners at a size that must not change.
    /// Sized off `offsetY`, the player jumps at both of those moments.
    @Published private(set) var minimizeProgress: CGFloat = MinimizableState.fullSizeProgress

    private(set) var miniViewWidthRatio: CGFloat = 1
    private var borderPadding: CGFloat = 0
    private var footerHeight: CGFloat = 0
    private var hostWidth: CGFloat = 0
    private var miniViewWidth: CGFloat = 0
    private var miniViewHeight: CGFloat = 0
    private var availableHeightForDragging: CGFloat = 0

    /// `true` once the host has been measured at a usable size.
    private(set) var isConfigured = false

    // MARK: - Configuration

    /// Port of `MinimizableView.rebuildState`: the whole geometry is derived from the new host
    /// size, so nothing can be carried over except whether the player was minimized.
    func configure(
        hostSize: CGSize,
        borderPadding: CGFloat,
        footerHeight: CGFloat,
        miniPlayerMaxWidth: CGFloat,
        appBottomBarHeight: CGFloat,
        playerRatio: CGFloat
    ) {
        guard hostSize.width > 0, hostSize.height > 0 else { return }

        // Capping the ratio rather than the rendered width keeps every consumer correct at once:
        // the drag interpolation, the pinch-zoom ceiling and the 16:9 height all derive from it,
        // and the expanded player still reaches a full-width fraction of 1.
        let ratio = min(Self.miniPlayerWidthRatio, miniPlayerMaxWidth / hostSize.width)
        let miniWidth = hostSize.width * ratio
        // Across the PADDED width: the video box is a child of the card's padding, so its 16:9 is
        // measured after the border is taken off. Sizing it from `miniWidth` overstates the mini
        // player by `2 * borderPadding / playerRatio`, which would dock it that much clear of the
        // bottom bar on top of the border gap it is meant to keep.
        let miniHeight = (miniWidth - borderPadding * 2) / playerRatio
        let travel = max(
            hostSize.height - appBottomBarHeight - miniHeight - footerHeight - borderPadding * 2,
            0
        )

        // `configure` runs on every layout pass, so bail out unless something it derives changed —
        // rewriting the offsets otherwise would cancel an in-flight settle on an idle frame.
        let unchanged = isConfigured &&
            hostWidth == hostSize.width &&
            self.borderPadding == borderPadding &&
            self.footerHeight == footerHeight &&
            miniViewWidth == miniWidth &&
            availableHeightForDragging == travel
        if unchanged { return }

        miniViewWidthRatio = ratio
        self.borderPadding = borderPadding
        self.footerHeight = footerHeight
        hostWidth = hostSize.width
        miniViewWidth = miniWidth
        miniViewHeight = miniHeight
        availableHeightForDragging = travel
        isConfigured = true

        applyMinimized(isMinimized)
    }

    // MARK: - Derived geometry

    var isMiniSizeReached: Bool { minimizeProgress >= Self.miniSizeProgress - Self.epsilon }

    var widthFraction: CGFloat {
        Self.lerp(start: 1, stop: miniViewWidthRatio, fraction: minimizeProgress)
    }

    /// Opens with the shrink, and divided by `zoomScale` so the painted gap then holds steady: the
    /// padding sits on the card, outside the scaled content, so what the scale grows is the content
    /// inside it.
    var edgePadding: CGFloat { borderPadding * minimizeProgress / zoomScale }

    var footerHeightWithScale: CGFloat { footerHeight / zoomScale }

    var bottomContentAlpha: CGFloat {
        guard !isMinimized else { return 0 }
        // `alphaBoost` makes the content behind disappear faster than the drag itself.
        return max(1 - Self.alphaBoost * minimizeProgress, 0)
    }

    private var maxScale: CGFloat { 1 / miniViewWidthRatio }

    private var isBelowScaleMidpoint: Bool { zoomScale < (maxScale + Self.minScale) / 2 }

    // MARK: - Gestures

    /// Updates minimize progress by the distance actually dragged.
    ///
    /// Do not derive progress directly from `offsetY` here: during a grow animation `offsetY` can
    /// stay at 0 while the progress is still changing, so deriving progress from it would make the
    /// player jump.
    func onVerticalDragging(_ dragAmount: CGFloat) {
        guard availableHeightForDragging > 0 else { return }

        let currentOffsetY = offsetY
        let newOffsetY = min(max(currentOffsetY + dragAmount, 0), availableHeightForDragging)
        let travelled = (newOffsetY - currentOffsetY) / availableHeightForDragging

        offsetY = newOffsetY
        minimizeProgress = min(max(minimizeProgress + travelled, Self.fullSizeProgress), Self.miniSizeProgress)
    }

    func snapToCorner() {
        settle(to: isBelowScaleMidpoint ? Self.minScale : maxScale)
    }

    func onDoubleTap() {
        settle(to: isBelowScaleMidpoint ? maxScale : Self.minScale)
    }

    /// - Parameters:
    ///   - panX: horizontal finger travel, in the scaled card's own coordinates.
    ///   - panY: vertical finger travel, in the same coordinates as `panX`.
    func onGestureZoom(zoom: CGFloat, panX: CGFloat, panY: CGFloat) {
        let newScale = min(max(zoomScale * zoom, Self.minScale), maxScale)
        offsetX += panX * newScale
        offsetY += panY * newScale
        zoomScale = newScale
    }

    func toggleMinimized() {
        isMinimized.toggle()
        // The shrink is animated, not snapped: `isMinimized` flips at whatever depth the drag
        // reached, so this is what carries the player the rest of the way to its new size.
        zoomScale = Self.minScale
        withAnimation(.easeOut(duration: Self.animationDuration)) {
            offsetX = 0
            if isMinimized {
                minimizeProgress = Self.miniSizeProgress
                offsetY = availableHeightForDragging
            } else {
                minimizeProgress = Self.fullSizeProgress
                offsetY = 0
            }
        }
    }

    /// Restores `isMinimized` across a rotation or a host re-measure, with no animation and no
    /// travel: every offset has to be recomputed from the new dimensions rather than carried over.
    func restore(minimized: Bool) {
        guard isMinimized != minimized else { return }
        applyMinimized(minimized)
    }

    private func applyMinimized(_ minimized: Bool) {
        isMinimized = minimized
        zoomScale = Self.minScale
        offsetX = 0
        minimizeProgress = minimized ? Self.miniSizeProgress : Self.fullSizeProgress
        offsetY = minimized ? availableHeightForDragging : 0
    }

    // MARK: - Corner snapping

    /// Moves scale and offsets to the corner `newScale` implies, together. Settling the offsets
    /// while leaving `zoomScale` wherever the pinch ended would draw the view at one scale and
    /// position it for another, and would route the next gesture through the wrong branch of
    /// `isBelowScaleMidpoint`.
    private func settle(to newScale: CGFloat) {
        // The card is laid out top-trailing, so `offsetX` is right-edge relative and never
        // positive — which is why the host's own centre below is a negative number.
        let leftMostOffsetX = miniViewWidth - hostWidth
        let hostCenterXFromRightEdge = -hostWidth / 2
        let miniPlayerCenterXFromRightEdge = offsetX - miniViewWidth / 2

        // Derived from the travel range instead of recomputed from the insets, so the two can never
        // disagree — a mismatch between them is exactly what skews the vertical split off 50%.
        let effectiveHeight = availableHeightForDragging + miniViewHeight + footerHeight
        let yCenter = effectiveHeight / 2
        let miniPlayerCenterY = offsetY + (miniViewHeight + footerHeight) / 2

        let offsetXToSnap: CGFloat
        let offsetYToSnap: CGFloat

        if abs(newScale - maxScale) <= Self.epsilon {
            // At max scale the layout box still measures `miniViewWidth` but paints a full host
            // wide, so centring is its only on-screen placement.
            offsetXToSnap = leftMostOffsetX / 2

            // LAYOUT vs PAINTED. `scaleEffect` only repaints — the layout box keeps its size, so
            // `offsetY` moves a box while the eye follows the painted edges. The padding is on the
            // card, OUTSIDE the scaled content, so `offsetY` positions the padded box while only
            // its content is scaled. Call that content S, height H = miniViewHeight + footer,
            // starting one padding below `offsetY`, scaled about its own centre:
            //
            //   a' = a + (1 - s) * H/2,  b' = a + (1 + s) * H/2,  delta = H(s - 1)
            //
            // H is divided by `newScale`, the scale this settle is heading to rather than the one
            // the gesture left behind, so the geometry and what gets painted converge as it lands.
            let scaledBoxHeight = miniViewHeight + footerHeight / newScale
            let restingBoxHeight = miniViewHeight + footerHeight
            let deltaHeight = scaledBoxHeight * (newScale - 1)

            // Both ends aim at the painted edge the `s = 1` branch below already produces, so
            // pinching to max and back does not shift where the mini player sits.
            offsetYToSnap = miniPlayerCenterY < yCenter
                ? deltaHeight / 2
                : availableHeightForDragging + restingBoxHeight - scaledBoxHeight - deltaHeight / 2
        } else {
            // `newScale` is `minScale` here, so delta = 0 and the painted edges are the layout
            // edges — which is why neither axis needs the correction above.
            offsetXToSnap = miniPlayerCenterXFromRightEdge < hostCenterXFromRightEdge ? leftMostOffsetX : 0
            offsetYToSnap = miniPlayerCenterY < yCenter ? 0 : availableHeightForDragging
        }

        withAnimation(.easeOut(duration: Self.animationDuration)) {
            zoomScale = newScale
            offsetX = offsetXToSnap
            offsetY = offsetYToSnap
        }
    }

    private static func lerp(start: CGFloat, stop: CGFloat, fraction: CGFloat) -> CGFloat {
        start + (stop - start) * fraction
    }

    private static let miniPlayerWidthRatio: CGFloat = 0.7
}
