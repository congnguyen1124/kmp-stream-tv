# Navigation specification

## App-level navigation

- The persistent bottom bar exposes Home, Music, Short and Playlist in that order.
- Selecting a destination shows its native screen without discarding previously visited destination state.
- Home is selected at cold start.
- Music, Short and Playlist show explicit placeholder content until their feature screens exist.

## Home-level navigation

- The Home shell exposes brand, search, notifications, profile and category controls.
- Categories are Home, Movies, Series, Live and More.
- Home shows the shared feed; unfinished categories show a replaceable native placeholder screen.
- A non-Home category replaces the category row with a compact category-title submenu; its chevron
  opens the category picker, while the wordmark returns directly to Home.
- The selected category and loaded Home state survive navigation to another category or app destination.
- Scrolling the feed increases the reference top-bar background opacity over the content.
