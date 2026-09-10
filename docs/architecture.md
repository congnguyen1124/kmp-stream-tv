# Architecture

## Boundary

The sharing boundary ends at `HomeViewModel`. Everything above it is native UI; everything below it
is common Kotlin. This avoids a shared rendering framework while still sharing loading, retry,
mapping, validation and state transitions.

```text
Native UI
  Android: MainActivity → HomeTabFragment → HomeFragment → section/content adapters → XML ViewHolder
  iOS:     MainTabView → HomeTabView → HomeView → section/content SwiftUI views
                             │
                             ▼
Shared presentation: HomeViewModel → HomeUiState
                             │
                             ▼
Shared domain/data: HomeRepository → HomeDummyDataSource
                             │
                             └── future remote source → StreamTvApiClient (Ktor)
```

## Shared layers

- `domain/model` defines typed content and validates section/content compatibility.
- `domain/repository` is the stable data contract.
- `data/source` owns deterministic fixtures and simulated latency.
- `data/repository` hides the active source from presentation.
- `presentation` maps domain objects into flat Swift-friendly immutable UI models and exposes one
  `StateFlow<HomeUiState>`.
- `di` owns the Koin graph and platform bootstrap entry points.
- `core/network` owns Ktor and selects OkHttp or Darwin with `expect`/`actual`.

No platform UI imports a repository or Ktor type. No shared code imports an Activity, SwiftUI,
Media3 or AVFoundation type.

## Android presentation

Android follows the reusable RecyclerView pattern from `on-tv-android`:

- `BaseListAdapter` centralizes normal item binding.
- `BindableViewHolder` gives holders one typed binding contract.
- stable-id `DiffUtil.ItemCallback`s compare identity separately from content.
- `HorizontalSpacingDecoration` owns rail edge/inter-item spacing.
- a vertical section adapter owns horizontal content adapters.
- section presentation is exhaustive: each shared semantic family resolves to a dedicated XML holder.

The Activity owns app-level bottom navigation. The Home tab owns its top bar and category navigation;
the actual feed is a child Fragment. This mirrors the reference ownership boundaries and lets future
category fragments replace placeholders without coupling their state to MainActivity.

There is no focus coordinator and no `FocusRequester`. Cards use standard mobile click/pressed
behavior, while Android's normal View focus remains available for accessibility and keyboards.

## iOS presentation

`HomeStore` owns the shared ViewModel and converts its `StateFlow` to `@Published` state through the
shared `Observation` handle. It cancels observation and disposes the ViewModel with the Swift owner.
The SwiftUI tree contains no repository or fixture logic.

The native source tree mirrors Android's ownership boundaries:

- `App` owns persistent app-level destinations.
- `Feature/Home` owns the store, category shell, feed and section/card renderers.
- `Feature/Placeholder` owns reusable unfinished destinations.
- `Feature/Player` owns AVPlayer state, UIKit bridging and playback controls.
- `Core/UI` owns platform-wide visual tokens and remote artwork.

The domain class for short-form media is named `ShortVideo`; `Short` is reserved by Kotlin's numeric
type and would export a duplicate `SharedShort` Objective-C symbol when the static iOS framework is
linked.

## Dependency injection lifecycle

Each process starts Koin once. Home ViewModels are factories because each native screen owns its
lifecycle. Android places the instance in `ViewModelStore`; iOS owns it in `HomeStore`.

## Networking lifecycle

The Ktor client is a Koin singleton. The current dummy repository does not issue requests. A remote
implementation should depend on `StreamTvApiClient`, map transport DTOs to domain objects, and be
substituted only at the Koin binding.
