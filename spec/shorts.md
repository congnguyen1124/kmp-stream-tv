# Short feed and story behavior

## Short feed

- The Short main destination renders a vertical, one-item-per-page feed on Android and iOS.
- The first item starts automatically when the destination is visible.
- Starting a drag pauses the current item; settling selects and plays exactly one item.
- Leaving or hiding the destination pauses playback. Re-entering resumes the selected item.
- Reaching the end loops the current short.
- Approaching the end of loaded content requests the next page once.
- A Home short selection opens or selects the feed and positions it at the requested id, loading
  additional pages when necessary.
- Like and follow update immediately. Follow applies to every loaded and subsequently loaded item
  from that provider.
- Loading, paging, empty-error and retry states are visible and do not obscure retained content.

## Grouped story viewer

- Selecting a Home story opens a full-window viewer at the selected story.
- The group contains stories from the selected provider only.
- Segmented progress shows completed, active and upcoming stories.
- A short tap on the left/right half moves backward/forward. Holding pauses and releasing resumes.
- Playback end advances to the next story; the viewer closes after the last story.
- Pressing close or Back dismisses the viewer and restores app chrome.
- Reactions give immediate feedback and share invokes the native platform affordance where
  available.

## Platform boundary

- Repository, paging, interaction state and story selection live in common Kotlin ViewModels.
- Android owns XML/RecyclerView and uses `android_stream_player` with the Feed configuration.
- iOS owns SwiftUI and AVPlayer.
- Player position, buffering, end detection and lifecycle are never stored in shared state.
