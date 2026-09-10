# Home specification

## Purpose

Home is an ordered vertical mobile feed of editorial layout families. Android and iOS render the
same shared sections and playback identity while remaining free to use native platform composition.

## States

- First load shows centered progress below the persistent Home chrome.
- Pull-to-refresh keeps existing content visible and shows the refresh indicator.
- Loaded shows every section in repository order.
- Error shows the repository message, or “Unable to load Home content”, plus retry.
- Retry starts a fresh load and removes the old error.

## Content

Supported content types are Video, Series, Channel and Short. Every item has stable playback,
trailer, thumbnail, title, description and age data. Presentation also exposes deterministic dummy
provider, duration, view-count and progress values required by the cloned mobile card families.

Section compatibility:

- Banner, Videos, Popular Videos, Continue Watching and Mini Apps accept Video.
- Series accepts Series.
- Channels accepts Channel.
- Vertical Banner, Shorts and Popular Shorts accept Short.

All ten section types must occur exactly once in the deterministic fixture, and Story must be the
first returned section. Selecting a media item opens playback. Home contains no explicit focus
requester or focus restoration contract.

## Android rendering contract

- Section holders must inflate the matching `item_layout*` custom-view wrapper used by the mobile
  reference instead of flattening the outer hierarchy into the page adapter.
- Ordinary layouts use nested horizontal RecyclerViews; both highlight layouts use `ViewPager2`
  through the cloned carousel behavior with the reference size, scale, translation and looping.
- Tall Highlight must retain every source child: active blurred artwork, the two source carousel
  background drawables, carousel, Watch later, Watch now, Information, click proxy, bottom spacer,
  loading overlay and error overlay.
- Loading/error views remain part of every source layout family even when dummy content resolves
  synchronously. Do not add display-only scrims or gradients that are absent from the reference XML.
