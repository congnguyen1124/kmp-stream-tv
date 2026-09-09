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

All ten section types must occur exactly once in the deterministic fixture. Selecting a media item
opens playback. Home contains no explicit focus requester or focus restoration contract.
