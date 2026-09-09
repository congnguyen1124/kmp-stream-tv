# Navigation specification

## App-level navigation

- The persistent bottom bar exposes Home, Music, Shorts and Playlist in that order.
- Selecting a destination shows its Fragment without destroying previously visited destinations.
- Home is selected at cold start.
- Music, Shorts and Playlist show explicit placeholder content until their feature fragments exist.

## Home-level navigation

- The Home shell exposes brand, search, notifications, profile and category controls.
- Categories are Home, Movies, Series, Live and More.
- Home shows the shared feed; unfinished categories show a replaceable placeholder Fragment.
- The selected category is restored from the child Fragment tag after view recreation.
- Scrolling the feed increases top-bar opacity and reveals its divider.
