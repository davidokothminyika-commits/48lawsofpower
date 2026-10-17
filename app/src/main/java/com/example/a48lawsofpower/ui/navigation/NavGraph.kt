package com.example.a48lawsofpower.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.a48lawsofpower.legal.LegalDocumentKind
import com.example.a48lawsofpower.ui.LawsViewModel
import com.example.a48lawsofpower.ui.RootDestination
import com.example.a48lawsofpower.ui.components.rememberHaptics
import com.example.a48lawsofpower.ui.annotations.AnnotationsViewModel
import com.example.a48lawsofpower.ui.legal.LegalViewModel
import com.example.a48lawsofpower.ui.settings.SettingsViewModel
import com.example.a48lawsofpower.ui.screens.AnnotationReviewScreen
import com.example.a48lawsofpower.data.AnnotationWithLaw
import com.example.a48lawsofpower.ui.components.NoteEditorDialog
import com.example.a48lawsofpower.ui.screens.CodexScreen
import com.example.a48lawsofpower.ui.screens.DossierScreen
import com.example.a48lawsofpower.ui.screens.LawDetailScreen
import com.example.a48lawsofpower.ui.screens.ProfileScreen
import com.example.a48lawsofpower.ui.screens.LibraryFilter
import com.example.a48lawsofpower.ui.screens.LibraryScreen
import com.example.a48lawsofpower.ui.profile.ProfileViewModel
import com.example.a48lawsofpower.ui.progress.ReadingTrackerViewModel
import com.example.a48lawsofpower.ui.progress.StreakCelebration
import com.example.a48lawsofpower.ui.components.StreakCelebrationOverlay
import com.example.a48lawsofpower.ui.screens.LegalDocumentScreen
import com.example.a48lawsofpower.ui.screens.LegalScreen
import com.example.a48lawsofpower.ui.screens.SplashScreen
import com.example.a48lawsofpower.ui.screens.StrategiesScreen
import com.example.a48lawsofpower.ui.theme.Spacing
import kotlinx.serialization.Serializable

// Pure type-safe routes

@Serializable object SplashRoute

@Serializable object DossierRoute

@Serializable object StrategiesRoute

@Serializable object CodexRoute

@Serializable object NotesRoute

@Serializable object LibraryRoute

@Serializable object ProfileRoute

@Serializable object LegalRoute

@Serializable data class DetailRoute(val lawNumber: Int)

/** The document is keyed by name rather than by enum ordinal, so reordering the enum cannot break a link. */
@Serializable data class LegalDocumentRoute(val kind: String)

@Composable
fun AppNavGraph(
    viewModel: LawsViewModel,
    legalViewModel: LegalViewModel,
    annotationsViewModel: AnnotationsViewModel,
    settingsViewModel: SettingsViewModel,
    profileViewModel: ProfileViewModel,
    readingTrackerViewModel: ReadingTrackerViewModel,
    startAtDossier: Boolean,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()

    val laws by viewModel.filteredLaws.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val bookmarked by viewModel.bookmarkedLaws.collectAsStateWithLifecycle()
    val selectedLaw by viewModel.selectedLaw.collectAsStateWithLifecycle()
    val dailyLaw by viewModel.dailyLaw.collectAsStateWithLifecycle()
    // The Daily Codex law, promoted to the Home screen's featured card. Resolved against the
    // *filtered* list so a search or category pill that excludes it also drops the card, rather
    // than leaving a featured law the reader cannot otherwise see.
    val featured = dailyLaw?.let { daily ->
        laws.firstOrNull { it.lawNumber == daily.lawNumber }
    }
    val totalLaws by viewModel.lawCount.collectAsStateWithLifecycle()
    val masteredLaws by viewModel.masteredCount.collectAsStateWithLifecycle()
    val consentRecord by legalViewModel.record.collectAsStateWithLifecycle()
    val activeLawId = selectedLaw?.id
    LaunchedEffect(activeLawId) { annotationsViewModel.openLaw(activeLawId) }
    val lawAnnotations by annotationsViewModel.annotationsForActiveLaw.collectAsStateWithLifecycle()
    val allAnnotations by annotationsViewModel.review.collectAsStateWithLifecycle()
    val annotationFilter by annotationsViewModel.filter.collectAsStateWithLifecycle()
    val lastDeleted by annotationsViewModel.lastDeleted.collectAsStateWithLifecycle()

    // Library filter and reading stats are screen-local: neither is a navigation-level concern, and
    // holding them here keeps a new destination from needing another parameter.
    var libraryFilter by rememberSaveable { mutableStateOf(LibraryFilter.All) }
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val readingStats by profileViewModel.stats.collectAsStateWithLifecycle()
    // Annotated-ness is not on LawOverview, so the ids are lifted out of the review list once and
    // shared by the Library rows and the Profile counts.
    val annotatedLawIds = remember(allAnnotations) { allAnnotations.map { it.lawId }.toSet() }

    val activeTab = backStackEntry?.destination.rootTab()
    // An explicit flag rather than `backStackEntry.destination.hasRoute<SplashRoute>()`. That
    // version still let the bar over the splash: during the first frames after a cold start
    // currentBackStackEntryAsState() can report an entry restored from saved state while the NavHost
    // is already drawing the splash, so both the "is it the splash" check and the tab lookup read
    // the same stale entry and disagree with what is actually on screen. Tracking that the reader
    // has left the splash cannot go stale that way.
    // Whether the entry screen has been dismissed - or, for a returning reader, was never shown at
    // all. Seeded from [startAtDossier]: when the splash is skipped nothing will ever set this to
    // true, so it has to start true, or the bottom bar is suppressed on every launch after the first.
    // When the splash *is* shown it starts false and is set by Get Started.
    var splashDismissed by rememberSaveable(startAtDossier) { mutableStateOf(startAtDossier) }
    val haptics = rememberHaptics()

    // Reading time accrues only while a law is the destination in front of the reader. The other
    // half of that gate - the app being foregrounded - is the Activity's, so the two signals live in
    // different places on purpose and neither can be wrong while the other still says yes.
    val isReaderVisible = backStackEntry?.destination?.hasRoute<DetailRoute>() == true
    LaunchedEffect(isReaderVisible) { readingTrackerViewModel.setReaderVisible(isReaderVisible) }

    var celebration by remember { mutableStateOf<StreakCelebration?>(null) }
    LaunchedEffect(Unit) {
        readingTrackerViewModel.celebrations.collect { celebration = it }
    }
    // Leaving the reader clears it. A reward for reading that outlives the reader would sit on top
    // of the Dossier, congratulating someone for something they can no longer see themselves doing.
    LaunchedEffect(isReaderVisible) {
        if (!isReaderVisible) celebration = null
    }

    Scaffold(
        bottomBar = {
            if (splashDismissed && activeTab != null) {
                CodexBottomBar(
                    current = activeTab,
                    onSelect = { destination ->
                        haptics.selection()
                        navController.switchTab(destination)
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                // The entry screen is a first-run screen, not a launch screen. Returning readers go
                // straight to the Dossier; `MainActivity` holds the graph back until the persisted
                // flag has actually been read, so this never has to guess and change its mind.
                startDestination = if (startAtDossier) DossierRoute else SplashRoute,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable<SplashRoute>(
                    // No transition. The splash is dismissed by a deliberate tap, and cross-fading
                    // it meant it stayed visible underneath the Dossier while the tab bar popped in
                    // over it - two things on screen at once, neither finished. It now leaves on the
                    // frame of the tap and the skeleton takes over immediately, which is also what
                    // makes the skeletons readable rather than a flash.
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popExitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                ) {
                    // Popped, not pushed onto: the Dossier must not sit behind the splash, or a
                    // back gesture from it would return to a dead entry screen. popUpTo the
                    // splash itself, which removes it from the back stack entirely.
                    SplashScreen(
                        onGetStarted = {
                            splashDismissed = true
                            // Persisted, not just remembered: this is the difference between the
                            // entry screen appearing on first launch and on every launch.
                            settingsViewModel.markIntroSeen()
                            navController.navigate(DossierRoute) {
                                popUpTo(SplashRoute) { inclusive = true }
                            }
                        },
                    )
                }

                composable<DossierRoute> {
                    DossierScreen(
                        laws = laws,
                        isLoading = isLoading,
                        featured = featured,
                        categories = categories,
                        selectedCategory = selectedCategory,
                        query = query,
                        totalLaws = totalLaws,
                        masteredLaws = masteredLaws,
                        onQueryChange = viewModel::setQuery,
                        onCategorySelected = viewModel::selectCategory,
                        onLawClick = { number -> navController.openLaw(number, viewModel) },
                        onToggleBookmark = viewModel::toggleBookmark,
                        contentPadding = innerPadding,
                    )
                }

                composable<StrategiesRoute> {
                    StrategiesScreen(
                        laws = bookmarked,
                        onLawClick = { number -> navController.openLaw(number, viewModel) },
                        onToggleBookmark = viewModel::toggleBookmark,
                        contentPadding = innerPadding,
                    )
                }

                composable<CodexRoute> {
                    CodexScreen(
                        law = dailyLaw,
                        onLawClick = { number -> navController.openLaw(number, viewModel) },
                        contentPadding = innerPadding,
                    )
                }

                composable<NotesRoute> {
        var editingNote by remember { mutableStateOf<AnnotationWithLaw?>(null) }
                    AnnotationReviewScreen(
                        annotations = allAnnotations,
                        filter = annotationFilter,
                        canUndo = lastDeleted != null,
                        onFilterChange = annotationsViewModel::setFilter,
                        onOpenLaw = { number -> navController.openLaw(number, viewModel) },
                        onEditNote = { row -> editingNote = row },
                        onDelete = annotationsViewModel::delete,
                        onUndo = annotationsViewModel::undoDelete,
                        contentPadding = innerPadding,
                    )

                    // The review screen's edit affordance opens the real editor, pre-filled with
                    // the mark's note and its quoted passage. It used to navigate to the law
                    // instead, which meant the editor was unreachable from the only screen where
                    // notes are listed - the row tap already opens the law, so the pencil had
                    // nothing to add by doing it again.
                    editingNote?.let { row ->
                        NoteEditorDialog(
                            quoted = row.anchorText,
                            lawTitle = row.lawTitle,
                            initialNote = row.note,
                            onSave = { text ->
                                annotationsViewModel.editNote(row.id, text)
                                editingNote = null
                            },
                            onDismiss = { editingNote = null },
                        )
                    }
                }

                composable<LibraryRoute> {
                    LibraryScreen(
                        laws = laws,
                        filter = libraryFilter,
                        onFilterChange = { libraryFilter = it },
                        annotatedLawIds = annotatedLawIds,
                        onLawClick = { number -> navController.openLaw(number, viewModel) },
                        onToggleBookmark = viewModel::toggleBookmark,
                        contentPadding = innerPadding,
                    )
                }

                composable<ProfileRoute> {
                    ProfileScreen(
                        stats = readingStats,
                        totalLaws = totalLaws,
                        masteredLaws = masteredLaws,
                        savedCount = bookmarked.size,
                        annotatedCount = allAnnotations.size,
                        annotatedLaws = annotatedLawIds.size,
                        themeMode = themeMode,
                        onThemeModeChange = settingsViewModel::setThemeMode,
                        onOpenStrategies = {
                            navController.navigate(StrategiesRoute) { launchSingleTop = true }
                        },
                        onOpenNotes = { navController.navigate(NotesRoute) { launchSingleTop = true } },
                        onOpenLegal = { navController.navigate(LegalRoute) { launchSingleTop = true } },
                        contentPadding = innerPadding,
                    )
                }

                composable<LegalRoute> {
                    LegalScreen(
                        record = consentRecord,
                        onOpenDocument = { kind ->
                            navController.navigate(LegalDocumentRoute(kind = kind.name))
                        },
                        // Clearing the record flips the gate in MainActivity, which tears this whole
                        // graph down. No back stack to unwind and nothing to pop back to.
                        onRevokeConsent = legalViewModel::revoke,
                        contentPadding = innerPadding,
                    )
                }

                composable<LegalDocumentRoute> { entry ->
                    val kind = entry.toRoute<LegalDocumentRoute>().kind
                    val resolved = runCatching { LegalDocumentKind.valueOf(kind) }.getOrNull()
                    if (resolved == null) {
                        // A stale or hand-edited route: go back rather than render a blank reader.
                        navController.popBackStack()
                    } else {
                        LegalDocumentScreen(
                            kind = resolved,
                            onBackClick = { navController.popBackStack() },
                        )
                    }
                }

                composable<DetailRoute> { entry ->
                    val route: DetailRoute = entry.toRoute()

                    // Re-fetch detail context if the view is recreated/restored. This has to happen in
                    // a side effect: calling it from composition re-queried the database on every
                    // recomposition of this destination.
                    LaunchedEffect(route.lawNumber) {
                        viewModel.loadLawDetail(route.lawNumber)
                    }

                    LawDetailScreen(
                        law = selectedLaw,
                        annotations = lawAnnotations,
                        onHighlight = { section, start, end, text, color ->
                            val id = selectedLaw?.id
                            if (id != null) {
                                annotationsViewModel.highlight(id, section, start, end, text, color)
                            }
                        },
                        onAddNote = { section, start, end, text, note ->
                            val id = selectedLaw?.id
                            if (id != null) {
                                annotationsViewModel.addNote(id, section, start, end, text, note)
                            }
                        },
                        onBackClick = {
                            viewModel.clearSelectedLaw()
                            navController.popBackStack()
                        },
                        onToggleBookmark = { viewModel.toggleBookmark(route.lawNumber) },
                        onToggleRead = {
                            // Optimistic read is mirrored here from the loaded law, so the toggle is
                            // reversible rather than a one-way stamp.
                            selectedLaw?.let { law -> viewModel.setRead(law.lawNumber, !law.read) }
                        },
                    )
                }
            }

            StatusBarScrim(Modifier.align(Alignment.TopCenter))

            // Last in the Box, so it draws above both the screen and the status bar scrim. It
            // carries its own status-bar padding and so clears the scrim rather than sitting on it.
            StreakCelebrationOverlay(
                celebration = celebration,
                onDismiss = { celebration = null },
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

/**
 * Every screen here draws edge to edge, so list content slides under the system clock and the
 * battery icons while scrolling and becomes unreadable. This is one opaque strip behind the
 * status bar, shared by all destinations so the fix lives in a single place.
 */
@Composable
private fun StatusBarScrim(modifier: Modifier = Modifier) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(statusBarHeight + Spacing.sm)
            .background(MaterialTheme.colorScheme.background),
    )
}

/** Loads the law into the shared ViewModel, then opens the reader. */
private fun NavHostController.openLaw(lawNumber: Int, viewModel: LawsViewModel) {
    viewModel.loadLawDetail(lawNumber)
    navigate(DetailRoute(lawNumber = lawNumber)) { launchSingleTop = true }
}

/** The reader and the legal documents run full-bleed, so the bar only exists on the four root tabs. */
private fun NavDestination?.rootTab(): RootDestination? = when {
    this?.hasRoute<DossierRoute>() == true -> RootDestination.Dossier
    this?.hasRoute<CodexRoute>() == true -> RootDestination.Codex
    this?.hasRoute<LibraryRoute>() == true -> RootDestination.Library
    this?.hasRoute<ProfileRoute>() == true -> RootDestination.Profile
    else -> null
}

private fun NavHostController.switchTab(destination: RootDestination) {
    val route: Any = when (destination) {
        RootDestination.Dossier -> DossierRoute
        RootDestination.Codex -> CodexRoute
        RootDestination.Library -> LibraryRoute
        RootDestination.Profile -> ProfileRoute
    }
    navigate(route) {
        // Pop to the Dossier explicitly, NOT to graph.startDestinationId. The start destination
        // is now the splash, which is popped off the stack on entry; popping to it here would
        // either fail to find it or unwind the user's tab state back to the first tab on every
        // switch.
        popUpTo(DossierRoute) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
