# Project Plan



## Project Brief



## Implementation Steps

### Task_1_SetupDatabase: Add Room, Navigation, and Serialization dependencies. Create assets/database/laws.db with initial laws. Create LawEntity, LawDao, and LawDatabase using createFromAsset.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - Room, Nav Compose, Serialization dependencies added
  - Database asset created
  - LawEntity, LawDao, LawDatabase implemented
- **StartTime:** 2026-10-10 19:57:34 EAT
- **Notes:** Schema is at version 4 (`core`/`observance`/`transgression`/`reversal` columns).
  `MIGRATION_3_4` rebuilds the table because it also drops `full_text`. `tools/build_laws_db.py`
  regenerates `app/src/main/assets/database/laws.db` from `content/laws-*.json`.
  `PrepackagedDatabaseTest` fails the build if the asset drifts from the exported schema.

### Task_2_UIAndTheming: Define editorial colors (DarkCharcoal, SoftOffWhite, MutedGray, DeepCrimson) and apply them. Build HomeScreen with a list and DetailScreen for immersive reading.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - Colors and Theme are defined
  - HomeScreen and DetailScreen Composables are built

### Task_3_NavAndWiring: Create LawsViewModel for state management. Build type-safe AppNavGraph. Wire ViewModel, Navigation, and Theme into MainActivity.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - LawsViewModel implemented with StateFlow
  - Type-safe navigation integrated
  - MainActivity wires all components together

### Task_4_RunAndVerify: Run and verify application stability. Instruct critic_agent to verify alignment with user requirements and report critical UI issues.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - make sure all existing tests pass
  - build pass
  - app does not crash
  - critic_agent verifies stability and requirements
- **Notes:** Verified on device `MB28006883561102010` (720x1612, 195dpi override):
  `assembleDebug` and `testDebugUnitTest` green, no `FATAL EXCEPTION` in logcat across
  Dossier / My Strategies / Daily Codex / reader.
  Two defects found by screenshot review and fixed:
  1. Reader's `ReaderTopInset` was a hardcoded 104dp that ignored the device status bar, so the
     section eyebrow printed underneath the floating top bar. Now measured via `onSizeChanged`.
  2. Scrolling list content rendered directly beneath the system clock. A shared `StatusBarScrim`
     in `AppNavGraph` keeps an opaque strip behind the status bar for every destination.
  Also dropped a duplicated horizontal inset: the pager and its hosted `LazyColumn` both applied
  `Spacing.md`, giving the reader 32dp margins against the list screens' 16dp.

### Task_5_ConsentAndLegal: Add a first-launch consent gate and in-app access to the Terms, Privacy Policy and Cookie Notice.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - Gate shown on first launch before any law data is read; consent can be withdrawn in-app
  - Terms, Privacy Policy and Cookie Notice reachable at any time from the app
  - `assembleDebug` and `testDebugUnitTest` pass; no crashes on device
- **Notes:** Consent lives in DataStore (`ConsentStore`), deliberately *not* in Room - the database
  is a pre-packaged asset validated against an exported schema, so a preference column there would
  force a migration, an asset rebuild and an uninstall.
  Acceptance is **version-gated**: `consentStatusFor` compares the stored version against
  `CONSENT_VERSION`, so revising the terms re-prompts exactly the readers whose agreement went stale.
  A bare boolean cannot distinguish "agreed to the current terms" from "agreed to whatever was on
  screen in March".
  The gate is rendered by `MainActivity` above `AppNavGraph`, not inside it - `LawsViewModel` opens
  the database on construction, and that must not happen before consent is granted.
  `ConsentStatus.Unknown` is a distinct state so the gate does not flash on every launch while
  DataStore is still reading.
  Withdrawal needs no navigation bookkeeping: clearing the record flips the same state flow, which
  tears the nav graph down and puts the gate back on screen.
  Verified on `MB28006883561102010`: first launch gates with ACCEPT disabled, accepting enters,
  force-stop and relaunch goes straight to the Dossier, withdrawal returns to the gate.
  0 `FATAL EXCEPTION`; `ConsentStatusTest` covers the version gate (6 tests).
- **Outstanding (blocking release):** the three legal documents are **placeholder drafting skeletons**
  flagged in-app with a red `PlaceholderBanner`. They are deliberately impossible to ship unnoticed:
  set `isPlaceholder = false` per document only once real, reviewed text replaces the `[...]` notes
  in `LegalDocuments.kt`, then bump `CONSENT_VERSION`. See `NOTICE.md`.
  Also still open from this work: the manifest declares `android.permission.INTERNET` but no screen
  makes a network request - delete the permission or document it, since it contradicts the Privacy
  Policy's claim that nothing leaves the device.
  Pre-existing, unrelated: `LawsViewModel.loadDailyLaw()` uses `java.time`, which needs API 26 while
  `minSdk` is 24 and core library desugaring is not enabled - the Daily Codex will crash on
  API 24/25 devices.

### Task_6_ThemeFontsLibraryProfile: Ship the light family, bundle the real fonts, add Key Takeaways, Library and Profile.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - Light/dark/system theme toggle, honoured by every screen, status bar included
  - Playfair Display and Inter bundled and wired into the type scale
  - Key Takeaways authored for all 48 laws and visible in the reader; Mark as Read is explicit
  - Library and Profile screens reachable; Notes and Strategies folded into Profile
  - Build, tests and migrations green; no crashes on device
- **Notes:**
  - **Theme.** `Color.kt`'s names became `@Composable` getters over `MaterialTheme.colorScheme`, so
    screens that already read `ParchmentWhite` follow the family without being rewritten. The cost is
    that the names cannot be read in a `DrawScope` or a default argument - the compiler finds these,
    and the fix is to hoist the value into the enclosing composable. Success and failure are not
    Material roles, so they live in `LawSemanticColors`; the dark-family values (#38A169 on paper is
    ~2.8:1) are too weak to read on white and have separate light variants.
  - **System bars** are not Material components and do not follow the scheme; `_48LawsOfPowerTheme`
    sets `isAppearanceLightStatusBars` in a `SideEffect`. Without it the light family drew white
    status-bar glyphs on white.
  - **Fonts** are bundled, not downloadable - the app is offline-only. Static Latin subsets produced
    with `fontTools.subset`, cutting 1.8MB to 681KB with **zero** missing glyphs, verified against
    every character in `content/*.json` and the Kotlin sources. Recorded in `ATTRIBUTIONS.md` with
    both OFL texts under `third_party/fonts/`.
  - **Highlight colours were decoupled from the theme.** They were bound to the app accent, so the
    gold-to-crimson retheme left "Key point" and "Danger" painting the *same* crimson; they are now
    fixed mid-tone values, which also stops a reader's saved marks changing when they switch family.
  - **Key Takeaways** are a new `key_takeaways` TEXT column (newline-delimited, not a converted list,
    so the pre-packaged asset stays inspectable). `MIGRATION_6_7` is a plain `ALTER TABLE ADD COLUMN`
    - nothing is dropped, unlike 3->4. 192 takeaways across 48 laws, authored as original commentary:
    no text from Greene's book is reproduced, per `NOTICE.md`.
  - **Mark as Read is now explicit.** Opening a law used to mark it read, which made "mastered" mean
    "tapped once" and inflated the streak.
  - **Tab bar is four wide**: Dossier, Library, Codex, Profile. Strategies, Notes and Legal moved into
    Profile - they are places a reader *returns* to, not browses, and six tabs crowded the bar.
  - **Splash breath** is a 1.5% scale / 1.9s ease-in-out loop with alpha on the same phase. Measured
    on device: lit pixels oscillate 8265<->8514 with minima 2.0s apart, matching the reverse cycle.
    Under reduced motion it collapses to a constant - a perpetual loop is the one animation that
    cannot be shortened usefully.
  - Verified on `MB28006883561102010`: 35 unit tests pass, `verify_migrations.py` reproduces the
    exported schema exactly, asset at v7 with 48/48 laws populated, 0 `FATAL EXCEPTION`.
  - Fixed while verifying: the tab bar still appeared over the splash. The existing guard read
    `hasRoute<SplashRoute>()` off `currentBackStackEntryAsState()`, which can report a stale restored
    entry while the NavHost already draws the splash. Replaced with an explicit `leftSplash` flag.
- **Outstanding:** Library and Profile were built from assumptions - there is no mockup for either in
  the repo - and need design review. The Key Takeaways column is empty for installs that upgraded from
  schema 6 until they reinstall, since Room copies the pre-packaged asset only when the database file
  is absent; the reader degrades to an explicit empty state rather than a blank block.
  The unused `INTERNET` permission flagged in Task_5 has since been removed from the manifest.

### Task_7_SplashHandover: Kill the splash linger and add skeleton loading to the Dossier.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - Get Started removes the splash on the frame of the tap
  - No tab bar over the splash; no empty-state flash before the laws arrive
  - Build and tests green; no crashes on device
- **Notes:**
  - The splash had four transitions fighting the handover: the default cross-fade kept it on screen
    under the Dossier, the tab bar popped in over it because `leftSplash` flipped before navigation,
    and the list rendered "Nothing filed here" - a *search* empty state - while the first read was
    still in flight. All three are gone: `SplashRoute` declares `EnterTransition.None` /
    `ExitTransition.None`, and the Dossier distinguishes loading from empty.
  - `isLoading` is a separate signal from "no laws", because those are different states and the
    screen used to render the second for both.
  - The skeleton is card-shaped rather than a centred spinner, so the list does not jump when the
    laws land, and the sweep stops under reduced motion.
- **Caveat worth knowing:** the laws ship in a pre-packaged SQLite asset and `allLaws` is shared at
  `Eagerly` from the ViewModel's construction, so the first read normally completes while the reader
  is still on the splash or the consent gate. The skeleton is therefore correct but will rarely be
  seen on this device - it covers a genuinely slow first read (cold disk, slow class loading), not a
  manufactured delay. Making it routinely visible would mean deferring the load until the Dossier
  opens, which is a real option and is *not* a fake delay, but it has not been done because it
  changes when the database is touched.

### Task_8_VerifyAndRepairAnnotations: Fix the minSdk crash, then verify the three never-tested annotation features.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - No API 24/25 crash from `java.time`
  - Note editor, My Notes list and undo exercised with real data on device
  - Instrumented suite compiles and runs
  - Unit + instrumented tests green; no crashes
- **Notes:**
  - **Desugaring.** `java.time` is API 26+, minSdk is 24, and it is used in `LawsViewModel`,
    `Progress.kt` and `CodexScreen` - so the failure is at class load, not a recoverable call, and
    the Daily Codex was simply dead on API 24/25. Fixed with `isCoreLibraryDesugaringEnabled` plus
    `com.android.tools:desugar_jdk_libs` 2.1.5. Verified by finding `j$/time/LocalDate`,
    `j$/time/ZoneId` and `j$/time/DayOfWeek` in the built DEX rather than trusting the flag.
  - **My Notes was empty for everyone.** `observeFiltered` filtered colour with `:color = ''` while
    `AnnotationsViewModel.review` passes `null` for "no filter" via `takeIf`. `NULL = ''` is NULL, not
    true, so the whole WHERE was unknown and the query returned nothing. The type clause already used
    `IS NULL`; the two were simply inconsistent. This only surfaces with a populated list, which is
    exactly what "never verified" hid. Pinned by three new instrumented assertions.
  - **The edit pencil navigated instead of editing.** `onEditNote` in the nav graph opened the law,
    so `NoteEditorDialog` was unreachable from the only screen that lists notes - and the row tap
    already opens the law, so the pencil duplicated it under a misleading label. It now opens the
    editor pre-filled with the note and its quoted passage.
  - **The instrumented suite had not compiled since v5.** `AnnotationDaoInstrumentedTest` still held
    the old `HighlightDao` API. `assembleDebug` and `testDebugUnitTest` never compile this source
    set, so the rot was invisible. Rewritten against `AnnotationDao`, keeping the original tests.
  - Verified on device with four seeded marks: the list renders quotes, notes and type dots; the
    editor opens pre-filled; delete drops 3 -> 2 and offers undo; undo restores 2 -> 3 and consumes
    itself, matching the documented one-level design.
  - 40 unit + 14 instrumented tests pass, 0 crashes.
- **Note on method:** reading the app database with `cat databases/laws_database` returns a *stale*
  snapshot while a `-wal` file exists. That twice looked like a real bug - empty annotations, and a
  working undo. Device-state claims here come from screenshots; database claims need the `-wal` too.
- **Still open:** the note editor's quoted passage did not appear above the field in the captured
  frame. Unresolved - either it is genuinely clipped, or the capture caught the enter animation.
  Worth a deliberate look before release, since the quote is the editor's whole reason to exist.

### Task_9_OneTimeOnboarding: Show the entry screen once and keep the consent gate to first launch plus revocation.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - Get Started appears on first launch only
  - Terms & Privacy gate appears on first launch and on revocation only
  - A returning launch goes straight to the Dossier
  - Build and tests green
- **Notes:**
  - The entry screen was the nav graph's unconditional `startDestination`, so every cold start began
    with it. It is now chosen: `startDestination = if (startAtDossier) DossierRoute else SplashRoute`,
    driven by a persisted `seen_intro` flag in the settings store.
  - **The flag is `StateFlow<Boolean?>`, not `Boolean`,** and that is the load-bearing detail. The
    start destination must be picked before the graph composes, so "not read yet" and "read, and the
    answer is false" cannot collapse into one value: defaulting to false would flash Get Started at
    every returning reader, defaulting to true would skip it on a genuine first launch. `MainActivity`
    holds the graph on the loading state until it resolves.
  - `leftSplash` stays as in-session state and only governs the bottom bar; persistence is what makes
    the entry screen a first-run screen rather than a launch screen.
  - Verified on device from `pm clear`: gate -> accept -> Get Started -> Dossier -> force-stop ->
    relaunch goes straight to the Dossier with neither screen. Revocation returning to the gate was
    verified separately via Profile > Withdraw.
- **Environment note:** this device repeatedly loses taps - a media player and a Google assistant
  overlay take the foreground mid-run, processes freeze, and `input tap` events are sometimes
  delivered to the *next* window. Several apparent bugs during this work were that, not the app.
  Each was confirmed by checking `mCurrentFocus` or the store file before believing a screenshot.

