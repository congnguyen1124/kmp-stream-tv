# Player specification

## Common behavior

- Selecting playable Home content starts its stream immediately.
- Show the content title and a close action in expanded playback.
- Pause when playback leaves the foreground and release native resources with the player owner.
- VOD exposes play/pause/replay, buffered progress, absolute scrubbing and ±10-second seek.
- Live exposes a LIVE label and play/pause, without seek or quality/speed affordances.
- Playback errors map to user-facing copy; retry is shown only for retryable engine errors.

## Android behavior

- Android playback uses `android_stream_player`; XML `PlayerView` only renders state and dispatches
  manager commands.
- `PlayerFragment` overlays `MainActivity`. Playback must not launch a separate Activity.
- Portrait playback is a VOD detail overlay: top close bar, 16:9 player, metadata/actions, provider
  and full-width related-content rows. It hides bottom navigation but keeps system bars visible.
- Landscape hides detail and system/app chrome, then exposes the full player title and secondary
  controller row.
- Minimize creates an in-app player row above bottom navigation without stopping playback.
- A downward drag from the video collapses portrait detail to mini after a distance/velocity
  threshold and returns to detail when cancelled.
- Tapping the mini video/title expands it; mini play/pause and close remain directly actionable.
- Switching main destinations must not hide or recreate the mini-player.
- Back closes settings, exits landscape, minimizes expanded portrait playback, then closes mini
  playback in that order.
- Selecting another Home item while mini reuses the fragment and replaces the active media.
- Selecting a related card replaces media through the same path and returns the detail list to top.
- System PiP uses the same Fragment/surface at 16:9, strips all app overlays, keeps playback alive,
  and restores the previous presentation when the pinned window is reopened.
- Android 12 PiP supplies auto-enter, source-rect and seamless-resize hints; mini/close disables
  stale auto-enter state.

Episode and outro fragments are deferred until their shared data contracts exist. The series
episode affordance remains visible as a placeholder integration point. Related cards currently use
a deterministic Android fixture and must move to shared state when the related-content API lands.

## Platform ownership

- Android uses native XML/Views plus `android_stream_player`.
- iOS uses native SwiftUI/UIKit plus AVPlayer.

Runtime player state is intentionally absent from the common ViewModel. Shared code supplies
selected media metadata; each platform owns decoder, surface and lifecycle state.
