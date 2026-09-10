# Home implementation

Home uses the media URLs and artwork fixture derived from `android_stream_tv`, then presents it with
the mobile XML/RecyclerView layout families used by `on-tv-android`.

| Shared presentation | Android view | Dummy content |
|---|---|---|
| Story | Editorial gradient block, story cards and provider avatar | Story shorts; always first |
| HighlightWide | 340 × 191 artwork carousel rail | Featured videos |
| GeneralWide | Compact 16:9 thumbnail rail | Recommended videos |
| TopTen | Background artwork, fixed title block and numbered posters | Popular videos |
| GeneralTall | Compact portrait cards | Documentary series |
| Circle | Circular artwork and two-line label | Live channels |
| HighlightTall | Blurred artwork field, large portraits and three actions | Editor spotlight shorts |
| ContinueWatching | 16:9 cards with subtitle and progress | Partially watched videos |
| Short | Large portrait cards with play/view metadata | Fresh shorts |
| MiniApps | Rounded utility panel with compact icon tiles | Dummy StreamTV destinations |

## Shared state

`HomeViewModel` emits loading immediately, maps repository content, then emits either sections or an
English error message. Calling `loadHome()` cancels an in-flight load before retrying.

`HomeSection` rejects empty or type-incompatible content. `HomeUiMapper` adds native-ready metadata
such as provider, subtitle, duration, view count, progress and section presentation without leaking
Android resource concepts into shared code.

The fixture intentionally returns the Story section first. This mirrors the production payload
shape while keeping all ten supported layout families deterministic for UI development and tests.

## Android

`HomeFragment` collects state only while its View lifecycle is started. A vertical RecyclerView owns
section holders; every rail owns a horizontal RecyclerView and shares one recycled view pool. The
pool and adapter are released in `onDestroyView`, preventing old Activity Views from surviving a
configuration change. Pull-to-refresh, centered first-load progress and retry error states are
mutually exclusive.

`HomeSectionAdapter` selects the outer general, highlight-tall, background/top-ten or mini-app
layout. `HomeContentAdapter` then selects the exact card family. Standard mobile press/ripple
behavior is used; there is no focus requester or TV remote focus restoration.

The Android visual layer ports the relevant `on-tv-android` resource graph: SVN Gilroy font-family
weights, `#111111` surface palette, top-bar gradients/blur assets, Story background/dividers,
numbered Top 10 artwork, card ratios and the four bottom-navigation state icons. StreamTV's own
wordmark and launcher assets remain sourced from `android_stream_tv`.

Wide and tall highlights use centered, snapping RecyclerViews. They preserve the reference card
sizes on roomy displays, responsively shrink to a 40 dp minimum side peek on compact phones, and
scale neighboring pages from 85% to 100% as the centered item changes.

## iOS

`HomeStore` observes the same expanded state and remains owned by `HomeTabView` while categories
change. `HomeView` handles loading, retry, pull-to-refresh, scroll reporting and full-screen native
AVPlayer presentation. `HomeSectionView` exhaustively maps the ten shared semantic presentations to
SwiftUI section families; `HomeContentCard` owns the reusable landscape, portrait, circle, story,
short, continue-watching and ranked card families.

Story, wide/tall highlights, Top 10 and Mini Apps have dedicated section files because they own
layout behavior beyond an ordinary horizontal rail. Shared theme and remote-artwork primitives live
under `Core/UI`, while app navigation, Home, placeholders and player code live in independent
`App`/`Feature` directories. Android-specific Fragment/RecyclerView ownership does not cross the KMP
boundary.
