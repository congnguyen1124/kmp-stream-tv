# Player specification

## Common behavior

- Start the selected stream when playback opens.
- Show title and a close/back action.
- Pause when playback leaves the foreground.
- Release native player resources when the owner is destroyed.
- VOD exposes play/pause, progress, rewind and forward.
- Live exposes a LIVE label and play/pause, with no seek affordance.

## Platform ownership

- Android uses `android_stream_player` and renders its state in native XML Views.
- iOS uses AVPlayer in Swift and renders app-owned SwiftUI controls.

Player runtime state is intentionally not placed in the common ViewModel. Shared code supplies the
selected media metadata; each native engine owns decoder and surface lifecycle.
