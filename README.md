# 48 Laws of Power

An offline Android reader and reference app for the 48 Laws of Power, built
with Kotlin and Jetpack Compose. Each law is broken into a core judgement,
what happens when the law is observed and when it is ignored, and its
reversal.

## Features

- 48 laws in a Room-backed local database, no network access at runtime
- Codex, Dossier, and Strategies screens
- Reading progress and personal strategy notes, stored on-device
- In-app consent gate and Legal tab
- Material 3, light and dark

## Build

Requires JDK 17 and the Android SDK.

```sh
./gradlew assembleDebug
```

The database is generated from `content/*.json` by `tools/build_laws_db.py`.

## Attribution

"The 48 Laws of Power" and the names of its 48 laws are the work of Robert
Greene (1998). This project contains no text from the book. Everything in
`content/` is original commentary.

See [NOTICE.md](NOTICE.md).

## License

MIT for the code only. See [LICENSE](LICENSE).