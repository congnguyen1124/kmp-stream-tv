# StreamTV KMP

StreamTV KMP shares application state through the ViewModel layer while keeping both user
interfaces native:

- Android is a mobile app using Views, XML, nested `RecyclerView`s and `android_stream_player`.
- iOS uses SwiftUI and a dedicated AVFoundation/AVPlayer implementation.
- `shared` owns Home models, repository contracts, deterministic fixtures, mapping, Koin wiring and
  the Ktor client boundary.

The current mobile product slice is the complete Main/Home shell plus playback. Its media catalogue
comes from the dummy fixture modelled after `android_stream_tv`, while Android's screen hierarchy,
navigation chrome and RecyclerView layout families follow `on-tv-android`. It deliberately does not
port TV remote focus or Compose `FocusRequester` behavior.

Android and iOS reuse the original StreamTV wordmark and launcher artwork from
`android_stream_tv`, keeping one visual identity across the native shells.

## Project map

```text
kmp-stream-tv/
├── shared/
│   └── src/
│       ├── commonMain/       # Domain, data, HomeViewModel, Koin, Ktor boundary
│       ├── androidMain/      # OkHttp Ktor engine and Android Koin bootstrap
│       └── iosMain/          # Darwin Ktor engine and iOS Koin bootstrap
├── androidApp/
│   └── src/main/
│       ├── kotlin/.../core/ui/recyclerview/  # RecyclerView support primitives
│       ├── kotlin/.../feature/home/          # Home tab/fragment, category bar, adapters and holders
│       ├── kotlin/.../feature/placeholder/   # Ready-to-replace destination fragments
│       ├── kotlin/.../feature/player/        # PlayerFragment, PlayerView and stream-player bridge
│       └── res/layout/                       # XML layouts
├── iosApp/iosApp/
│   ├── Home/                 # SwiftUI Home and shared-state bridge
│   └── Player/               # Native AVPlayer owner and surface
├── docs/                     # Architecture and integration notes
└── spec/                     # Framework-neutral behavior contracts
```

## State flow

```text
HomeDummyDataSource → HomeRepository → HomeViewModel(StateFlow<HomeUiState>)
                                            ├── Android XML / RecyclerView
                                            └── iOS SwiftUI / Observation
```

Native code never reconstructs domain rules. `HomeUiState` is the immutable boundary both UIs
render. The flat UI models are intentional: they remain ergonomic in Swift while the domain model
keeps typed `Video`, `Series`, `Channel`, and `Short` variants.

## Playback

Android resolves the sibling player project through a Gradle composite build:

```text
kmp-stream-tv/../android_stream_player
```

`PlayerFragment` overlays `MainActivity` and owns `StreamTvPlayerManager`. Its app-owned XML
`PlayerView` ports the phone controller UI from `onmediaplayer-android`. Portrait reproduces the
`on-tv-android` VOD detail hierarchy with metadata, provider actions and recommendations; landscape
is fullscreen; downward drag creates an internal mini-player above bottom navigation; Android
system PiP remains available separately. All playback commands and state continue to come from
`android_stream_player`.

iOS does not wrap the Android library. `StreamPlayer.swift` owns an `AVPlayer`, its periodic time
observer and cleanup. `PlayerView.swift` provides native controls and live/VOD differences.

See [player integration](docs/player.md) for lifecycle details.

## Android navigation shell

`MainActivity` owns the four-item bottom navigation (`Home`, `Music`, `Short`, `Playlist`) and
keeps destination fragments alive while switching tabs. `HomeTabFragment` owns the overlaid brand,
search, notification, profile and category bars. `HomeFragment` is only responsible for loading and
rendering the shared state. Non-Home destinations and non-Home categories intentionally point to
placeholder fragments so their future features can replace one class without rebuilding the shell.

See [navigation shell](docs/navigation.md) for ownership and replacement points.

## Koin and Ktor

Koin constructs the shared data source, repository, mapper, ViewModel and Ktor client. Android calls
`startKoinForAndroid` from `Application`; iOS calls `startKoinForIos` before creating `HomeStore`.

Ktor uses OkHttp on Android and Darwin/`NSURLSession` on iOS. Home remains deterministic dummy data,
so the HTTP client is isolated behind `StreamTvApiClient` and ready for a remote data source without
changing ViewModel or UI contracts.

## Build and test

Requirements: JDK 17+, Android SDK 37, and the sibling `android_stream_player` checkout.

```bash
./gradlew ktlintCheck
./gradlew :shared:testAndroidHostTest
./gradlew :androidApp:assembleDebug
```

Use `./gradlew ktlintFormat` to apply the repository's Kotlin style before running the checks. See
[Kotlin code style](docs/code-style.md) for versioning, scope, and module-specific commands.

Open `iosApp/iosApp.xcodeproj` in Xcode to build iOS. Configure `TEAM_ID` in
`iosApp/Configuration/Config.xcconfig` for a signed device build.

## Documentation

- [Architecture](docs/architecture.md)
- [Home implementation](docs/home.md)
- [Navigation shell](docs/navigation.md)
- [Player integration](docs/player.md)
- [Kotlin code style](docs/code-style.md)
- [Home specification](spec/home.md)
- [Navigation specification](spec/navigation.md)
- [Player specification](spec/player.md)
