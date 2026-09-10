# Native navigation shells

## Main destinations

`MainActivity` hosts a `FragmentContainerView` above a labeled Material bottom navigation bar. The
menu contains Home, Music, Short and Playlist, matching the mobile reference structure. Switching
destinations hides and shows tagged Fragment instances so Home scroll position and loaded shared
state are retained.

The bar uses the reference black surface, 28 dp selected/unselected icon pairs, white/gray state
colors and Gilroy semibold 12 sp labels. The Material active pill is disabled to preserve the
reference appearance.

Home is implemented by `HomeTabFragment`; the other three destinations use `PlaceholderFragment`.
To implement a destination, replace its factory branch in `MainActivity.createDestination` while
keeping the menu id and tag stable.

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

`MainTabView` owns the same persistent Home, Music, Short and Playlist destinations through a native
SwiftUI `TabView`. `HomeTabView` owns Home's brand row, working search/notification/profile feedback,
the Home/Movies/Series/Live/More category controls and the toolbar scrim. The loaded `HomeStore` and
its feed remain alive while a placeholder category is shown, preserving shared state and scroll
identity in the same ownership layer used by Android's `HomeTabFragment`.

The SwiftUI hierarchy follows the Android feature split without sharing native rendering code:

```text
MainActivity / MainTabView
  ├── HomeTabFragment / HomeTabView
  │     ├── HomeFragment / HomeView
  │     └── Home chrome and categories
  └── PlaceholderFragment / PlaceholderView
```
