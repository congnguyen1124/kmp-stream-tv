# Kotlin code style

The project uses the official ktlint code style for Kotlin and Gradle Kotlin DSL files. Formatting
is shared by `shared`, `androidApp`, and the root Gradle scripts through
`org.jlleitschuh.gradle.ktlint`.

## Version policy

- The Gradle plugin version is declared as `ktlint-gradle` in `gradle/libs.versions.toml`.
- The ktlint rules engine is declared separately as `ktlint-cli` and pinned in the root build script.
- `.editorconfig` is the source of truth for whitespace, the 120-column limit, and trailing commas.
- Experimental rules are intentionally disabled. Adopt one only after agreeing on the rule and
  formatting the complete repository.

Keeping the plugin and rules engine versions separate is intentional: a plugin patch may change its
default ktlint dependency, while this project must produce repeatable formatting locally and in CI.

## Commands

Check all modules and Gradle Kotlin scripts without editing files:

```bash
./gradlew ktlintCheck
```

Format everything ktlint can correct, then run the check again:

```bash
./gradlew ktlintFormat
./gradlew ktlintCheck
```

Module-scoped commands are also available when iterating on a smaller change:

```bash
./gradlew :shared:ktlintCheck
./gradlew :androidApp:ktlintFormat
```

Generated sources and build outputs are excluded. Do not add a baseline for new violations; format
or fix the source instead. Reports are written under each project's `build/reports/ktlint` folder in
plain-text and Checkstyle formats.

## Development workflow

Run `ktlintFormat` before the normal tests when Kotlin or `.gradle.kts` files change. Before handing
off a change, run `ktlintCheck` together with the relevant shared tests and Android build. IDE
formatting should respect the root `.editorconfig`, so Android Studio and the command line converge
on the same layout.
