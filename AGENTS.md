# StreamTV KMP engineering notes

- User-facing copy and dummy catalogue content are English.
- This is a mobile project. `on-tv-android` is only a RecyclerView/XML organization reference.
- Keep the sharing boundary at ViewModel state. Android UI remains XML/Views; iOS UI remains SwiftUI.
- New network code goes through `StreamTvApiClient`; platform engines stay in platform source sets.
- Android RecyclerView support belongs under `core/ui/recyclerview`, not inside a feature adapter.
- Do not add Compose UI or `FocusRequester` behavior.
- Android playback must use the sibling `android_stream_player`; iOS playback must use AVPlayer.
- Update `spec/` and `docs/` with material behavior or architecture changes.
- Run `ktlintCheck`, `:shared:testAndroidHostTest`, and `:androidApp:assembleDebug` after relevant
  changes. Use `ktlintFormat` before the check when Kotlin or Gradle Kotlin DSL files change.

## Android-to-iOS update handoff

- Before merging any Android change, update `docs/updates/YYYY-MM-DD.md` with every observable UI,
  behavior, navigation, state-contract and asset change that iOS must review. If a change is
  intentionally Android-only, record it with the reason instead of omitting it.
- Keep one pending handoff file per update date and append later changes made on that date. Include
  the Android source/reference, exact behavior, shared-model impact, iOS implementation checklist,
  acceptance checks and verification commands. Put device screenshots in
  `docs/updates/images/YYYY-MM-DD/` and link them from the handoff when a device is available.
- A file named `YYYY-MM-DD.md` means iOS parity is still pending. Only after the iOS implementation
  and its acceptance checks pass may the iOS implementer rename it to `YYYY-MM-DD_done.md`.
- Do not merge an observable Android feature/fix until its handoff file is complete and current.
