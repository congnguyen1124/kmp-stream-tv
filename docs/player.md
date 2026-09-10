# Player integration

Playback is platform-specific by design. Only selected content metadata crosses from shared Home.

## Android: android_stream_player

The root build includes `../android_stream_player` as a composite build and consumes
`com.congnguyencn:stream-player:0.1.0`.

`PlayerActivity`:

1. Creates `StreamTvPlayerManager` with the feed preset for shorts and its long-form preset for other content.
2. Binds `manager.exoPlayer()` to the XML Media3 `PlayerView`.
3. Calls `loadAndPlay` for the selected URL.
4. Collects `playerState` while the Activity is started.
5. Uses command helpers for play/pause and ±10-second seek.
6. Pauses in `onStop`, detaches the surface and closes the manager in `onDestroy`.

Live channels hide seek controls and resume at the default/live position. VOD and shorts expose
progress and seeking.

## iOS: AVPlayer

`StreamPlayer` is a native Swift owner around `AVPlayer`. It replaces media items, toggles playback,
seeks, samples time twice per second and removes its observer during teardown.

`PlayerView` renders an `AVPlayerViewController` with app-owned controls. Live channels omit seek and
progress UI. It pauses when the scene leaves the foreground, then stops and clears its item when the
full-screen player disappears. `NativePlayerView` is kept in its own bridge file so UIKit surface
ownership stays separate from SwiftUI controls.

## Non-goal

The player engine is not shared through `expect`/`actual`. Media3 and AVFoundation have different
state/lifecycle surfaces; keeping their wrappers native avoids a leaky lowest-common-denominator API.
