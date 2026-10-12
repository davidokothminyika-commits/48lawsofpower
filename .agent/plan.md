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

