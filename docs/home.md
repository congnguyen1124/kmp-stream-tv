# Home implementation

Home uses the media URLs and artwork fixture derived from `android_stream_tv`, then presents it with
the mobile XML/RecyclerView layout families used by `on-tv-android`.

| Shared presentation | Android view | Dummy content |
|---|---|---|
| HighlightWide | 340 × 191 artwork carousel rail | Featured videos |
| GeneralWide | Compact 16:9 thumbnail rail | Recommended videos |
| TopTen | Background artwork, fixed title block and numbered posters | Popular videos |
| GeneralTall | Compact portrait cards | Documentary series |
| Circle | Circular artwork and two-line label | Live channels |
| HighlightTall | Blurred artwork field, large portraits and three actions | Editor spotlight shorts |
| ContinueWatching | 16:9 cards with subtitle and progress | Partially watched videos |
| Short | Large portrait cards with play/view metadata | Fresh shorts |
| Story | Editorial gradient block, story cards and provider avatar | Story shorts |
| MiniApps | Rounded utility panel with compact icon tiles | Dummy StreamTV destinations |

## Shared state

`HomeViewModel` emits loading immediately, maps repository content, then emits either sections or an
English error message. Calling `loadHome()` cancels an in-flight load before retrying.

`HomeSection` rejects empty or type-incompatible content. `HomeUiMapper` adds native-ready metadata
such as provider, subtitle, duration, view count, progress and section presentation without leaking
Android resource concepts into shared code.

## Android

`HomeFragment` collects state only while its View lifecycle is started. A vertical RecyclerView owns
section holders; every rail owns a horizontal RecyclerView and shares one recycled view pool. The
pool and adapter are released in `onDestroyView`, preventing old Activity Views from surviving a
configuration change. Pull-to-refresh, centered first-load progress and retry error states are
mutually exclusive.

`HomeSectionAdapter` selects the outer general, highlight-tall, background/top-ten or mini-app
layout. `HomeContentAdapter` then selects the exact card family. Standard mobile press/ripple
behavior is used; there is no focus requester or TV remote focus restoration.

## iOS

`HomeStore` observes the same expanded state. The existing SwiftUI feed continues to render all
sections and opens the native AVPlayer sheet. Android-specific Fragment/navigation ownership does
not cross the KMP boundary.
