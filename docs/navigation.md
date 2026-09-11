# Native navigation shells

## Main destinations

`MainActivity` hosts a `FragmentContainerView` above a labeled Material bottom navigation bar. The
menu contains Home, Music, Short and Playlist, matching the mobile reference structure. Switching
destinations hides and shows tagged Fragment instances so Home scroll position and loaded shared
state are retained.

The bar uses the reference black surface, 28 dp selected/unselected icon pairs, white/gray state
colors and Gilroy semibold 12 sp labels. The Material active pill is disabled to preserve the
reference appearance.

Home is implemented by `HomeTabFragment` and Short by `ShortMediaFragment`. Music and Playlist use
`PlaceholderFragment`. Destination fragments keep their stable menu id/tag and are hidden/shown so
the Short selection and the Home scroll position survive tab changes.

Selecting a normal short card routes to the Short destination and positions its feed at that item.
Selecting a card in the `Story` section opens `StoryGroupFragment` in a full-window overlay. The
story overlay hides bottom/system chrome until it closes. Expanded or mini long-form playback is
closed before either exclusive short-form experience starts, preventing overlapping audio.

## Home destinations

`HomeTabFragment` draws the brand row, search and notification actions, profile affordance and a
scrollable category row over its child container. Home, Movies, Series, Live and More are wired.
Only Home has product content today; other entries deliberately render reusable placeholders.

Like the reference shell, choosing a non-Home category swaps the category row for a compact submenu
row. Tapping its title opens the category picker, and tapping the StreamTV wordmark returns Home.

The toolbar scrim becomes opaque as the Home feed scrolls. Child Fragment state and the selected
category tag survive view recreation. Search, notification and profile buttons currently provide
explicit “coming later” feedback instead of dead click targets.

## iOS

`MainTabView` owns the same persistent Home, Music, Short and Playlist destinations through an
app-owned SwiftUI bottom bar. It deliberately avoids the system `TabView` chrome so the black
56-point bar, 28-point Android SVG state icons and Gilroy 12-point labels stay visually identical to
the Android Material bar. `HomeTabView` owns Home's brand row, working search/notification/profile feedback,
the Home/Movies/Series/Live/More category controls and the toolbar scrim. The loaded `HomeStore` and
its feed remain alive while a placeholder category is shown, preserving shared state and scroll
identity in the same ownership layer used by Android's `HomeTabFragment`.

The iOS brand row and category row use the Android 48/36-point heights, source logo and converted
toolbar SVGs. Non-Home categories use the compact submenu and app-owned category dialog; unavailable
toolbar actions use transient feedback rather than iOS alert chrome.

The SwiftUI hierarchy follows the Android feature split without sharing native rendering code:

```text
MainActivity / MainTabView
  ├── HomeTabFragment / HomeTabView
  │     ├── HomeFragment / HomeView
  │     └── Home chrome and categories
  ├── ShortMediaFragment / ShortMediaView
  ├── StoryGroupFragment / StoryGroupView (full-screen overlay)
  └── PlaceholderFragment / PlaceholderView
```
