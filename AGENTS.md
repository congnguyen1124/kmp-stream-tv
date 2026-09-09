# StreamTV KMP engineering notes

- User-facing copy and dummy catalogue content are English.
- This is a mobile project. `on-tv-android` is only a RecyclerView/XML organization reference.
- Keep the sharing boundary at ViewModel state. Android UI remains XML/Views; iOS UI remains SwiftUI.
- New network code goes through `StreamTvApiClient`; platform engines stay in platform source sets.
- Android RecyclerView support belongs under `core/ui/recyclerview`, not inside a feature adapter.
- Do not add Compose UI or `FocusRequester` behavior.
- Android playback must use the sibling `android_stream_player`; iOS playback must use AVPlayer.
- Update `spec/` and `docs/` with material behavior or architecture changes.
- Run `:shared:testAndroidHostTest` and `:androidApp:assembleDebug` after relevant changes.
