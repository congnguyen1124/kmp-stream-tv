import Foundation

/// One transient emoji in flight, the iOS stand-in for a pooled `TextView` inside
/// `reactionAnimationLayer`.
struct FloatingReaction: Identifiable, Equatable {
    let id = UUID()
    let emoji: String
    /// Index of the reaction button the emoji was cloned from, which fixes its horizontal position.
    let column: Int
}

/// Owns every transient reaction and the burst that seeds them.
///
/// `StoryGroupFragment` keeps this work in the Fragment: a `Pools.SynchronizedPool` bounded at
/// `MAX_REACTION_VIEWS`, a set of live views, and a single burst `Job` that `onPause` and
/// `onDestroyView` cancel before recycling everything. SwiftUI has no view to recycle, so the
/// bound applies to the in-flight models instead, and the same cancellation points clear them.
@MainActor
final class StoryReactionStore: ObservableObject {
    @Published private(set) var reactions: [FloatingReaction] = []

    /// `emotion_animation`: 800 ms of upward travel and fade.
    static let travelDuration: TimeInterval = 0.8
    /// `toYDelta="-500%"` — five times the reaction button's own height.
    static let travelHeightMultiple: CGFloat = 5
    /// `MAX_REACTION_VIEWS`
    private static let maxInFlight = 50
    /// `RANDOM_REACTION_INTERVAL_MILLIS`
    private static let burstInterval = Duration.milliseconds(100)
    /// `MIN_RANDOM_REACTIONS..MAX_RANDOM_REACTIONS`
    private static let burstCount = 30...60

    private var burst: Task<Void, Never>?
    private var removals: [UUID: Task<Void, Never>] = [:]
    /// `ARG_PLAYED_REACTION_BURST`: the burst belongs to the story the viewer opened on, once.
    private var hasPlayedBurst = false

    /// `startReactionAnimation`: clone the tapped emoji into the animation layer.
    func emit(emoji: String, column: Int) {
        let reaction = FloatingReaction(emoji: emoji, column: column)
        reactions.append(reaction)
        while reactions.count > Self.maxInFlight {
            remove(reactions[0].id)
        }
        removals[reaction.id] = Task { [weak self] in
            try? await Task.sleep(for: .seconds(Self.travelDuration))
            guard !Task.isCancelled else { return }
            self?.remove(reaction.id)
        }
    }

    /// `playInitialReactionBurst`: 30–60 random reactions at 100 ms intervals, and only for the
    /// story the viewer was opened on.
    func playInitialBurst(storyId: String, initialId: String, pool: [String]) {
        guard storyId == initialId, !hasPlayedBurst, !pool.isEmpty else { return }
        hasPlayedBurst = true
        burst?.cancel()
        burst = Task { [weak self] in
            for _ in 0..<Int.random(in: Self.burstCount) {
                try? await Task.sleep(for: Self.burstInterval)
                guard !Task.isCancelled else { return }
                guard let emoji = pool.indices.randomElement() else { return }
                self?.emit(emoji: pool[emoji], column: emoji)
            }
        }
    }

    /// `onPause` / `onDestroyView`: cancel the burst and force-hide everything still in flight.
    func cancelAll() {
        burst?.cancel()
        burst = nil
        removals.values.forEach { $0.cancel() }
        removals.removeAll()
        reactions.removeAll()
    }

    private func remove(_ id: UUID) {
        removals.removeValue(forKey: id)?.cancel()
        reactions.removeAll { $0.id == id }
    }

    deinit {
        burst?.cancel()
        removals.values.forEach { $0.cancel() }
    }
}
