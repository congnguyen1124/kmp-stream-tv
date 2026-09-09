# Android navigation shell

## Main destinations

`MainActivity` hosts a `FragmentContainerView` above a labeled Material bottom navigation bar. The
menu contains Home, Music, Shorts and Playlist, matching the mobile reference structure. Switching
destinations hides and shows tagged Fragment instances so Home scroll position and loaded shared
state are retained.

Home is implemented by `HomeTabFragment`; the other three destinations use `PlaceholderFragment`.
To implement a destination, replace its factory branch in `MainActivity.createDestination` while
keeping the menu id and tag stable.

## Home destinations

`HomeTabFragment` draws the brand row, search and notification actions, profile affordance and a
scrollable category row over its child container. Home, Movies, Series, Live and More are wired.
Only Home has product content today; other entries deliberately render reusable placeholders.

The toolbar scrim becomes opaque as the Home feed scrolls. Child Fragment state and the selected
category tag survive view recreation. Search, notification and profile buttons currently provide
explicit “coming later” feedback instead of dead click targets.
