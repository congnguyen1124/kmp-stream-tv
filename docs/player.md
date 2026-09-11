# Player integration

Playback remains platform-owned. Shared Home supplies immutable content metadata; decoder state,
surface ownership, controls, rotation and teardown stay native.

## Android implementation

Android combines two sibling reference projects with separate responsibilities:

- `../onmediaplayer-android/phone` is the visual and interaction reference. Its
  `io.teragroup.player.MediaPlayerView`, top/center/bottom controllers, error view, setting columns,
  icons, gradients, spacing and portrait/landscape rules were ported into this app.
- `../android_stream_player` remains the only playback engine. The app consumes
  `com.congnguyencn:stream-player:0.1.0` through the composite build and does not copy the old
  onmediaplayer ExoPlayer implementation.

The former standalone `PlayerActivity` has been removed. `MainActivity` now owns a dedicated
`playerFragmentContainer` above its destination fragments and bottom navigation.

### File ownership

```text
feature/player/
├── PlayerFragment.kt                 # lifecycle, detail/fullscreen/mini/PiP transitions
├── PlayerMedia.kt                    # primitive Home-to-player/detail argument model
├── PlayerDetailAdapter.kt            # VOD metadata, provider and recommendation rows
├── PlayerDemoCatalog.kt              # related-content fixture until its API exists
└── widget/
    ├── PlayerView.kt                 # surface and top/center/bottom controller UI
    └── PlayerSettingsView.kt         # quality, speed, audio and subtitle columns

res/layout/
├── fragment_player.xml               # top bar, 16:9 player and detail RecyclerView
├── item_player_detail.xml
├── item_player_provider.xml
├── item_player_recommendation*.xml
└── view_player.xml                    # reusable player surface/controller
```

`PlayerView` owns no playback state. It receives a `StreamTvPlayerManager`, binds
`manager.exoPlayer()` to the Media3 surface, renders `StreamTvPlayerState`, and sends playback
actions back through the manager API.

### Navigation flow

1. Every Home item callback calls `MainActivity.openPlayer(content)` and opens the portrait VOD
   detail presentation, not a separate Activity.
2. When no player exists, the Activity adds `PlayerFragment` to the overlay container.
3. When a mini-player already exists, the same fragment and engine are reused; selecting another
   Home item replaces the media and expands the player.
4. The overlay is excluded from the bottom-navigation fragment hide/show loop, so mini playback
   survives switching between Home, Music, Short and Playlist.
5. Recommendation cards use the same `play(media)` path, reset the list to its first row and reuse
   the manager whenever the Feed/TV preset did not change.
6. Closing removes the fragment, restores the bottom bar/system bars and releases the engine.

Only primitive metadata enters fragment arguments through `PlayerMedia`: URLs, title, description,
provider, labels, booleans and episode count. The common `HomeContentUiModel` is not made Android
`Parcelable` or `Serializable`.

### Portrait VOD detail

The portrait hierarchy follows `on-tv-android`'s `VodDetailFragment` and `DetailAdapter`:

1. a 64 dp top bar with the close action;
2. a 16:9 native `PlayerView`;
3. a vertical `RecyclerView` containing the two-line title, view/like summary and horizontally
   scrollable Watch later, Products, Like, Comment and Share actions;
4. the StreamTV provider row with follow state;
5. a “Recommended for you” header and full-width 16:9 related cards.

The compact screen matches the supplied VOD reference by keeping extended metadata collapsed.
Tapping the “See more” summary reveals/hides the dummy description. Watch later, Like and Follow
retain local UI state; API-dependent Products, Comment and Share remain explicit demo actions.
Related rows come from `PlayerDemoCatalog` until shared related-content state is introduced.

### Controller structure

The full controller follows the phone module from `onmediaplayer-android`:

- Top: landscape title, drag/minimize action, system PiP action and close action.
- Center: rewind 10 seconds, play/pause/replay and forward 10 seconds.
- Bottom: elapsed/duration text, seek and buffered progress, quality, volume, fullscreen, speed,
  audio/subtitle and episode affordances.
- Error: source-style unavailable icon, mapped app message, conditional retry and close.
- Settings: black full-player overlay with the same column model used by the original player.

Controllers fade after five seconds while playback is advancing and reappear after a surface tap or
controller action. Scrubbing pauses auto-hide and dispatches an absolute seek when released. Media3
continues to render timed text on the surface; the settings view changes the selected text track
through the engine.

The episode affordance is retained for series so the original player view is not lost. Its
destination remains a placeholder until the episode fragment/data contract is implemented.

### Engine command mapping

| UI action | `android_stream_player` operation |
| --- | --- |
| Initial load, retry or replace item | `loadAndPlay(uri)` |
| Play or pause VOD | `togglePlayPause()` |
| Play or pause live | `togglePlayPauseAtDefaultPosition()` |
| Replay ended VOD | `replay()` |
| Rewind / forward | `seekBack()` / `seekForward()` |
| Scrub | `seekTo(duration)` |
| Speed | `setSpeed(value)` |
| Video quality | `selectVideoTrack(id)`; `auto` restores adaptive selection |
| Audio | `selectAudioTrack(id)` |
| Subtitles | `selectTextTrack(id)`; `off` disables subtitles |

Volume remains a local surface concern, matching the source controller: it changes the exposed
ExoPlayer volume between `0f` and `1f`. No duplicated playback state is created for it.

### Responsive, drag and mini behavior

| State | Player container | Controls | App chrome |
| --- | --- | --- | --- |
| Portrait detail | Full destination overlay; player remains 16:9 above detail list | Compact transport and progress; title is in detail row | Bottom bar hidden; system bars visible |
| Landscape fullscreen | Full screen | Full title, wide center spacing and applicable VOD actions | Bottom/system bars hidden |
| Mini | 96 dp row above bottom navigation, 170 dp video surface | Title, play/pause/replay and close | Bottom/system bars visible |
| System PiP | 16:9 pinned Activity surface | Android-provided PiP chrome; app overlays hidden | App is backgrounded |

`values-land` retains the source player's 64 dp center spacing and 62 dp secondary-action height.
Portrait uses 24 dp spacing and preserves the original invisible 20 dp action row. `MainActivity`
handles configuration changes so the same fragment, manager and decoder remain alive while
`PlayerView.applyOrientation()` reapplies resource-dependent visibility and spacing.

Dragging downward begins only from an unoccupied part of the video surface, so seeking and detail
list scrolling remain independent. The fragment follows the finger with a small fade. Releasing
past 22% of the screen height or above 1,250 px/s completes the transition to mini; otherwise it
animates back into place. Tapping the mini video/title restores the detail screen.

Back behavior is layered:

1. close the settings view;
2. leave landscape fullscreen for portrait;
3. minimize a portrait detail player;
4. close an already minimized player.

Internal mini-player and system Picture-in-Picture are deliberately separate:

- Drag/down action keeps `MainActivity` foregrounded and exposes bottom navigation.
- The PiP controller action calls `MainActivity.enterPictureInPictureMode()` with a 16:9 ratio,
  source-rect hint and Android 12 seamless/auto-enter parameters.
- Leaving the app while expanded and playing also enters PiP from `onUserLeaveHint()`.
- In PiP, the Fragment keeps the same manager/surface alive but hides detail, settings, errors and
  all app controllers. Returning restores portrait detail or landscape fullscreen without reload.
- Mini/close disables Android 12 auto-enter so an inactive player cannot reopen PiP later.

### Lifecycle

- `PlayerFragment` creates the manager with `StreamTvPlayerConfig.Feed` for shorts and
  `StreamTvPlayerConfig.Tv` for other Home content.
- State is collected only while the fragment view lifecycle is `STARTED`.
- Playback pauses when the Activity leaves the foreground and resumes only if it was playing;
  system PiP is the exception and continues playback.
- The Media3 surface is detached in `onDestroyView`.
- The manager is closed in `onDestroy`, releasing decoder and playback resources.
- Changing between Feed and TV content recreates the manager with the correct preset; replacing
  content within the same preset reuses it.

## iOS: AVPlayer

The iOS side is unchanged by this Android UI port. `StreamPlayer` owns `AVPlayer`, item replacement,
time sampling, seek and teardown. SwiftUI `PlayerView` and `NativePlayerView` own the controls and
UIKit surface respectively. There is no shared `expect`/`actual` player abstraction.

## Verification

Run from the repository root:

```bash
./gradlew :shared:testAndroidHostTest
./gradlew :androidApp:lintDebug
./gradlew :androidApp:assembleDebug
```
