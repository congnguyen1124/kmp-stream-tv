# Short feed and grouped stories

## Shared state

`ShortDummyDataSource` supplies deterministic short-form fixtures through `ShortRepository`.
`ShortViewModel` exposes four-item paging, selected index, retry, optimistic like state and
provider-wide follow state. `selectById` continues paging until a Home-selected item is available.
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
follow, like, comment, share and more actions, loading/error states, tap-to-pause and looping at end.
API-dependent comment/share/profile destinations currently give explicit feedback.

`StoryGroupFragment` is a full-window overlay opened from the Home story rail. It has segmented
progress, provider metadata, reactions and share, tap-left/tap-right navigation, hold-to-pause and
automatic advance on playback end. It closes after the last story.

## iOS

`ShortMediaView` renders the same state in a paging vertical SwiftUI scroll view. Only the active
page loads and plays its AVPlayer item; inactive pages unload theirs. `StoryGroupView` owns one
AVPlayer and mirrors segmented progress, navigation, hold behavior, reactions and automatic advance.
Home short cards can open a positioned full-screen feed, while the persistent Short tab retains its
own store.

## Deferred integrations

The repository is deterministic until the short/content endpoint is connected through
`StreamTvApiClient`. Authentication, comments, profiles, analytics and remote interaction writes
are explicit integration points; local optimistic state keeps the completed UI behavior testable.
