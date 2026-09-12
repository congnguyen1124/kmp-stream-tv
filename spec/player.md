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
- Portrait playback is a VOD detail overlay: a top-anchored 16:9 player, then metadata/actions,
  provider and full-width related-content rows. It hides bottom navigation but keeps system bars
  visible. Close, minimize and PiP live in the player controller, so the detail has no top bar of
  its own — the player is anchored to the top of the overlay and there is no room for one.
- Landscape hides detail and system/app chrome, then exposes the full player title and secondary
  controller row.
- Minimize shrinks the player into a floating mini card without stopping playback: 70% of the
  window wide (capped at 380dp), 16:9 with a transport strip below it, an 8dp border gap, 16dp
  corners and a close action over the video.
- A downward drag from the video follows the finger and completes the shrink on release, at
  whatever depth the drag reached. The detail content behind it fades out three times as fast as
  the drag travels, so it is gone well before the card lands.
- A minimized card can be dragged anywhere and settles into the nearest of the four corners,
  pinched open up to window width and back, and double-tapped to jump between those two sizes.
  Pinching to full width and back must not move where the card rests.
- Tapping the minimized card expands it back to detail; the transport strip stays directly
  actionable at any size. Transport actions must not bubble into the card tap-to-expand gesture,
  and their surfaces use the app background color.
- Rotating or otherwise resizing the window while minimized keeps the card minimized and re-derives
  its resting corner from the new dimensions.
- Switching main destinations must not hide or recreate the mini-player, and the window-sized
  overlay must pass touches outside the card through to the destination behind it.
- Back closes settings, exits landscape, minimizes expanded portrait playback, then closes mini
  playback in that order.
- Mini-player geometry is cloned from `ottclouds-android`'s `MinimizableViewState`; see
  `docs/player.md` for the seams that differ because the host is a `ViewGroup` and not a composable.
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
