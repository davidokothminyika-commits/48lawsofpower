# 48 Laws of Power

An offline Android reader and reference app for the 48 Laws of Power, built
with Kotlin and Jetpack Compose. Each law is broken into a core judgement,
what happens when the law is observed and when it is ignored, and its
reversal.

## Features

- 48 laws in a Room-backed local database, no network access at runtime
- Daily reading streaks, banked at one minute of actual reading, with an
  animated reward the first time a day qualifies
- Highlights, notes and bookmarks, anchored to the text so they survive a
  re-encode
- Library as the reader's own collection - saved, read, annotated
- Mastery progress and a seven-day reading strip on the Profile
- In-app consent gate and Legal tab
- Material 3, light and dark

> **Performance is only meaningful on the release build.** A debug Compose
> build measured ~30x slower on tab entry on the test device. Use
> `assembleRelease` before judging any frame timing.

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