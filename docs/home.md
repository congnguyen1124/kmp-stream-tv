# Home implementation

Home uses the media URLs and artwork fixture derived from `android_stream_tv`, then presents it with
the mobile XML/RecyclerView layout families used by `on-tv-android`.

| Shared presentation | Android view | Dummy content |
|---|---|---|
| Story | Android artwork block, 162 × 288 story cards and 32 dp provider avatar | Story shorts; always first |
| HighlightWide | 340 × 191 artwork carousel rail | Featured videos |
| GeneralWide | 172 × 97 thumbnail rail | Recommended videos |
| TopTen | Background artwork, fixed title block and numbered posters | Popular videos |
| GeneralTall | 106 × 185 portrait cards | Documentary series |
| Circle | 64 dp circular artwork and 80 dp two-line label | Live channels |
| HighlightTall | Artwork field, 294 × 441 portraits and three actions | Editor spotlight shorts |
| ContinueWatching | 16:9 cards with subtitle and progress | Partially watched videos |
| Short | 162 × 288 portrait cards | Fresh shorts |
| MiniApps | Rounded utility panel with compact icon tiles | Dummy StreamTV destinations |

## Shared state

`HomeViewModel` emits loading immediately, maps repository content, then emits either sections or an
English error message. Calling `loadHome()` cancels an in-flight load before retrying.

`HomeSection` rejects empty or type-incompatible content. `HomeUiMapper` adds native-ready metadata
such as provider, subtitle, duration, view count, progress and section presentation without leaking
Android resource concepts into shared code. Layout background artwork is explicit section data; it
is never synthesized from the first item thumbnail. The fixture supplies it only to the Top Ten
layout, so the wide highlight matches the reference's background-free payload.

The fixture intentionally returns the Story section first. This mirrors the production payload
shape while keeping all ten supported layout families deterministic for UI development and tests.

## Android

`HomeFragment` collects state only while its View lifecycle is started. A vertical RecyclerView owns
section holders; ordinary content rails own horizontal RecyclerViews. The adapter is released in
`onDestroyView`, preventing old Activity Views from surviving a configuration change.
Pull-to-refresh, centered first-load progress and retry error states are mutually exclusive.

`HomeSectionAdapter` follows the source `HomePageAdapter` contract: each section id has a unique
view type and maps to a dedicated holder in `HomeItemViewHolders.kt`. Those holders inflate the
source-shaped `item_layout*` wrappers, whose roots are custom general, Story, Watching,
background/Top Ten, wide-highlight, tall-highlight or Mini App views. Each cloned layout keeps its
original loading and error children even though the deterministic fixture binds immediately.
`HomeContentAdapter` selects the exact card family. Standard mobile press behavior is used; there
is no focus requester or TV remote focus restoration.

The Android visual layer ports the relevant `on-tv-android` resource graph: SVN Gilroy font-family
weights, `#111111` surface palette, top-bar gradients/blur assets, Story background/dividers,
numbered Top 10 artwork, card ratios and the four bottom-navigation state icons. StreamTV's own
wordmark and launcher assets remain sourced from `android_stream_tv`.

Wide and tall highlights use the source `CarouseView` algorithm around `ViewPager2`: 1,000-page
looping, three offscreen pages, unclipped side cards, the same translation formula and 85%–100%
page scale. They preserve the reference card sizes on roomy displays and shrink proportionally to
the source 40 dp minimum side padding on compact phones. Tall highlights use the reference active
poster blur parameters (radius 5, downsampling 25), source carousel top/bottom drawables, three
actions, click proxy, bottom spacer, loading overlay and error overlay; no additional scrim or
gradient layer is introduced.

## iOS

`HomeStore` observes the same expanded state and remains owned by `HomeTabView` while categories
change. `HomeView` handles loading, retry, pull-to-refresh, scroll reporting and full-screen native
AVPlayer presentation. `HomeSectionView` exhaustively maps the ten shared semantic presentations to
SwiftUI section families; `HomeContentCard` owns the reusable landscape, portrait, circle, story,
short, continue-watching and ranked card families.

Android resources are the visual source of truth on both platforms. iOS embeds the same Gilroy
Regular/Medium/Semibold/Bold files, raster logo/background/rank artwork from `drawable-xxxhdpi`, and
SVG conversions of every Android vector drawable. `scripts/sync_ios_android_assets.py` regenerates
those iOS resources from `androidApp/src/main/res`; selectors are represented by their individual
selected and unselected SVG assets. SwiftUI uses the XML dp dimensions directly as points and does
not add card labels, gradients or metadata absent from the corresponding Android item layout.

Story, wide/tall highlights, Top 10 and Mini Apps have dedicated section files because they own
layout behavior beyond an ordinary horizontal rail. Shared theme and remote-artwork primitives live
under `Core/UI`, while app navigation, Home, placeholders and player code live in independent
`App`/`Feature` directories. Android-specific Fragment/RecyclerView ownership does not cross the KMP
boundary.
