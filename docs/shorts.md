# Short feed and grouped stories

## Shared state

`ShortDummyDataSource` supplies deterministic short-form fixtures through `ShortRepository`.
`ShortViewModel` exposes four-item paging, selected index, retry, optimistic like state,
provider-wide follow state and local comments/counts until the interaction API is connected.
`selectById` continues paging until a Home-selected item is available.
`StoryGroupViewModel` resolves the selected item to every story from the same provider and owns only
the previous/next index.

`ShortItemUiModel` is deliberately flat so Android Views and SwiftUI render the same metadata and
interaction state without importing the domain model. Runtime playback state remains native.

## Android

`ShortMediaFragment` replaces the Short placeholder. A vertical `RecyclerView` plus
`PagerSnapHelper` produces one full-height page at a time. `ShortMediaAdapter` uses a three-player
`StreamTvPlayerPool` with `StreamTvPlayerConfig.Feed`; changing pages pauses and rewinds every other
slot, while re-entering a warm item reuses its buffer. Hiding the persistent destination pauses its
active player and showing it resumes playback.

Each page mirrors the reference short UI: full-bleed 9:16 video, provider and description chrome,
the over-avatar follow control, vertically stacked like/comment/share/more `MaterialButton`s,
loading/error states, animated tap-to-pause feedback and looping at end. Like and follow update
optimistically; a followed provider hides the plus affordance like the reference. Comment opens a
bottom sheet and adds local comments; the follow check remains visible for one second before hiding.
Share invokes Android's native share sheet, and More provides
Copy link, Not interested (advance) and Report. Provider opens the currently loaded videos for that
profile, toolbar Search selects a title/provider match, and toolbar Profile opens the personal
profile Activity. Remote comment, report and recommendation writes remain API integration points.

`StoryGroupFragment` is a full-window overlay opened from the Home story rail. It has segmented
progress, provider metadata, reactions and share, tap-left/tap-right navigation, hold-to-pause and
automatic advance on playback end. A reaction launches a pooled copy of that emoji from its button,
then translates it upward by five button heights while fading over 800 ms. The initial story also
plays the reference 3–6 second randomized reaction burst; pooled views and its coroutine are cleared
when the Fragment pauses or its view is destroyed. It closes after the last story.

## iOS

`ShortMediaView` renders the same state in a paging vertical SwiftUI scroll view. Only the active
page loads and plays its AVPlayer item; inactive pages unload theirs. Home short cards can open a
positioned full-screen feed, while the persistent Short tab retains its own store.

Its action rail carries the reference order and geometry: a 48-point provider avatar with the
plus/check follow control overlaid on its bottom edge, then Like, Comment, Share and More as
32-point icon-over-caption buttons. Like shows the activated heart and the optimistic compact
count, Share carries the **Share** label and opens `ShareLink` with the title and URL, and the
follow check hides one second after following while the follow itself applies provider-wide. Every
control keeps a VoiceOver label and a 44-point hit target; the follow badge paints at 32 points
inside that larger target. Tapping the video scales the centre indicator in over 100 ms when pausing
and fades it out over 150 ms when resuming.

`ShortActionSheets` supplies the comment, More, provider and search presentations. Comments list
`ShortViewModel.commentsFor` and submit through `addComment`, which rejects blank text and raises
the shared count. More writes the URL with `UIPasteboard`, advances to the next loaded item for Not
interested, and confirms Report before its local completion message. The provider sheet lists that
provider's loaded titles and toggles follow; search selects the first loaded title or provider
match; the toolbar profile affordance presents the personal-profile screen.

`StoryGroupView` owns one AVPlayer and mirrors segmented progress, navigation, hold behavior and
automatic advance. `StoryReactionStore` replaces the pooled `TextView`s with in-flight reaction
models, bounded at the same 50, and the animation layer is an overlay on the reaction row so each
clone starts in its source column and is not clipped as it travels. A reaction rises five button
heights while fading over 800 ms; the initial story plays the 3–6 second randomized burst once, and
both the burst and everything in flight are cancelled when the viewer backgrounds or disappears.

## Deferred integrations

The repository is deterministic until the short/content endpoint is connected through
`StreamTvApiClient`. Authentication, comments, profiles, analytics and remote interaction writes
are explicit integration points; local optimistic state keeps the completed UI behavior testable.
