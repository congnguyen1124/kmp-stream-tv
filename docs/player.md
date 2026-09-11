# Player integration

Playback remains platform-owned. Shared Home supplies immutable content metadata; decoder state,
surface ownership, controls, rotation and teardown stay native.

Short-form playback follows the same ownership rule but uses the low-latency feed configuration.
Android lends at most three engines from `StreamTvPlayerPool`; iOS keeps AVPlayer inside each active
native short page and unloads it as the page becomes inactive. See [shorts and stories](shorts.md).

## Android implementation

Android combines two sibling reference projects with separate responsibilities:

- `../onmediaplayer-android/phone` is the visual and interaction reference. Its
  `io.teragroup.player.MediaPlayerView`, top/center/bottom controllers, error view, setting columns,
  icons, gradients, spacing and portrait/landscape rules were ported into this app.
- `../android_stream_player` remains the only playback engine. The app consumes
  `com.congnguyencn:stream-player:0.1.0` through the composite build and does not copy the old
  onmediaplayer ExoPlayer implementation.
- `../ottclouds-android` is the mini-player reference. Its
  `feature/player-manager/mobile/.../ui/miniplayer/` (`NewMinimizableView`, `MinimizableViewState`,
  `customDetectTransformGestures`) and the `HorizontalVideoDetailScreen` that hosts them were
  ported to XML/Views here — see [Floating mini player](#floating-mini-player).

The former standalone `PlayerActivity` has been removed. `MainActivity` now owns a dedicated
`playerFragmentContainer` above its destination fragments and bottom navigation.

### File ownership

```text
feature/player/
├── PlayerFragment.kt                 # lifecycle, media, detail/fullscreen/mini/PiP presentation
├── PlayerMedia.kt                    # primitive Home-to-player/detail argument model
├── PlayerDetailAdapter.kt            # VOD metadata, provider and recommendation rows
├── PlayerDemoCatalog.kt              # related-content fixture until its API exists
├── miniplayer/
│   ├── MinimizableView.kt            # the overlay: sizes, offsets, decorates and drives the card
│   ├── MinimizableViewState.kt       # all shrink/settle geometry, ported one-to-one
│   ├── TransformGestureDetector.kt   # pan/pinch/tap/double-tap classification
│   ├── FloatAnimatable.kt            # the Compose Animatable contract the state needs
│   └── AspectRatioFrameLayout.kt     # Modifier.aspectRatio for the video box
└── widget/
    ├── PlayerView.kt                 # surface and top/center/bottom controller UI
    ├── MiniPlaybackControllerView.kt # transport strip under the minimized player
    └── PlayerSettingsView.kt         # quality, speed, audio and subtitle columns

res/layout/
├── fragment_player.xml               # the overlay: detail content plus the player card
├── item_player_detail.xml
├── item_player_provider.xml
├── item_player_recommendation*.xml
├── view_mini_playback_controller.xml # time bar, rewind, play/pause/replay, forward
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

1. a 16:9 native `PlayerView` anchored to the top of the overlay;
2. a vertical `RecyclerView` containing the two-line title, view/like summary and horizontally
   scrollable Watch later, Products, Like, Comment and Share actions;
3. the StreamTV provider row with follow state;
4. a “Recommended for you” header and full-width 16:9 related cards.

The former 64 dp top bar is gone. The mini player travels from the top of the overlay, so the
player card has to start at y = 0 and would cover any bar above it; close, minimize and PiP moved
into the player controller, which is where `HorizontalVideoDetailScreen` keeps them too. The detail
`RecyclerView` reserves the player's strip with a `Space` constrained to `H,16:9` rather than being
laid out below the player, because the player is a sibling that floats over it and travels away.

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
| Mini | Floating card, 70% of the window wide (max 380 dp), parked in a corner | Transport strip below the video; close over it | Bottom/system bars visible |
| System PiP | 16:9 pinned Activity surface | Android-provided PiP chrome; app overlays hidden | App is backgrounded |

`values-land` retains the source player's 64 dp center spacing and 62 dp secondary-action height.
Portrait uses 24 dp spacing and preserves the original invisible 20 dp action row. `MainActivity`
handles configuration changes so the same fragment, manager and decoder remain alive while
`PlayerView.applyOrientation()` reapplies resource-dependent visibility and spacing.

Dragging downward begins only from an unoccupied part of the video surface, so seeking and detail
list scrolling remain independent. The card follows the finger while the detail behind it fades,
and the shrink completes on release at whatever depth the drag reached — there is no distance or
velocity threshold, matching the reference. Tapping the minimized card restores the detail screen.

Back behavior is layered:

1. close the settings view;
2. leave landscape fullscreen for portrait;
3. minimize a portrait detail player;
4. close an already minimized player.

Internal mini-player and system Picture-in-Picture are deliberately separate:

- Drag/down action keeps `MainActivity` foregrounded and exposes bottom navigation. The overlay
  container stays window-sized while minimized, because the card travels the whole window; it paints
  no background and nothing outside the card is clickable, so touches fall through to the
  destination behind it.
- The PiP controller action calls `MainActivity.enterPictureInPictureMode()` with a 16:9 ratio,
  source-rect hint and Android 12 seamless/auto-enter parameters.
- Leaving the app while expanded and playing also enters PiP from `onUserLeaveHint()`.
- In PiP, the Fragment keeps the same manager/surface alive but hides detail, settings, errors and
  all app controllers. Returning restores portrait detail or landscape fullscreen without reload.
- Mini/close disables Android 12 auto-enter so an inactive player cannot reopen PiP later.

### Floating mini player

The shrink, the corner snapping and the pinch-zoom are a one-to-one port of
`ottclouds-android`'s `feature/player-manager/mobile/.../ui/miniplayer/`. `MinimizableViewState`
carries the same arithmetic and the same reasoning comments; the numbers it produces were checked
against a running build (a 720 × 1280 window at density 2.0: card 504 px wide, 266 px video, 88 px
strip, 16 px border gap, resting offset 783 px, and at max pinch scale 1.4286 with an 11 px padding,
271 px video and 62 px strip, painting 16 px clear of both window edges — the same gap the resting
card keeps).

| Composable | View |
| --- | --- |
| `Box(fillMaxSize)` | `MinimizableView` |
| `bottomContent` | `R.id.detailContent`, faded by `bottomContentAlpha` |
| the player `Column` | `R.id.playerCard` — width fraction, edge padding, offset, top-end gravity |
| `graphicsLayer { scale }` | `R.id.playerCardContent` — scale, rounded clip, border, elevation |
| `Modifier.aspectRatio(PlayerRatio)` | `AspectRatioFrameLayout` |
| `footerContent` | `MiniPlaybackControllerView` |
| `customDetectTransformGestures` | `TransformGestureDetector` |
| `Animatable<Float>` | `FloatAnimatable` over `ValueAnimator` |
| `onRegisterMaximize` / `onRegisterMinimize` | `MinimizableView.maximize()` / `.minimize()` |

Five things differ, and only because the host is a `ViewGroup`:

1. **The padded card and the scaled content are two views.** Padding lives inside a view's own
   bounds, so scaling one view would scale its border gap with it. Compose gets this for free from
   modifier order (`.padding()` outside `.graphicsLayer{}`), and the `edgePaddingPx` and `settleTo`
   arithmetic is written against that split.
2. **No coroutine scope.** The composable queues every update on `AndroidUiDispatcher` and its
   comments explain the FIFO ordering hazards that follow. `MinimizableView` calls the state
   directly from the touch handler on the main thread, so there is no queue and no ordering to get
   wrong.
3. **`hostHeightPx` is the overlay's height, not the window's.** `MainActivity` already pads its
   root by both system-bar insets, so the overlay is `window - topSystemBar - bottomSystemBar`;
   subtracting the insets again, as the composable does, would double-count them. `appBottomBarHeightPx`
   is the measured bottom-navigation height, cached in `MainActivity` because that bar is `GONE`
   while the player is expanded and a `GONE` view measures 0.
4. **Pan is converted before it reaches `onGestureZoom`.** That function multiplies the pan by the
   scale it is heading to, because the Compose detector sits *inside* the scaled layer and reports
   travel in the card's own coordinates. `MinimizableView` sits above the scale, so its pan is
   already in host pixels and is divided back first.
5. **Touches are hit-tested against the card.** The composable's `pointerInput` sits on the player
   `Column` alone; this view spans the whole overlay, so without `isGestureOnCard` the detail list
   could not be scrolled and a tap beside a minimized player would expand it instead of reaching the
   destination behind. `clipChildren`/`clipToPadding` are off down to `playerCardContent`, because a
   pinched-open card paints past the box it measures.

One deliberate departure: the drag starts on downward movement only, where
`detectVerticalDragGestures` starts on vertical slop in either direction and therefore minimizes on
an upward drag too. An upward drag has nowhere to travel — `onVerticalDragging` clamps the offset at
0 — so that path only ever discarded the gesture's intent.

Known limitation, shared with the reference: the Media3 surface is a `SurfaceView`, which
`clipToOutline` does not round. The card's corners are rounded, the video frame inside it is not.
Setting `app:surface_type="texture_view"` on `mediaSurface` in `view_player.xml` fixes it at the
cost of DRM-secure playback; it is left at Media3's default here because the reference does, and
because it could not be verified on the available emulator (its system trust store rejects the
demo CDN certificates, so no stream plays).

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
