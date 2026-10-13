package com.example.a48lawsofpower.ui.navigation

import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.a48lawsofpower.ui.legal.LegalViewModel
import com.example.a48lawsofpower.ui.screens.CodexScreen
import com.example.a48lawsofpower.ui.screens.DossierScreen
import com.example.a48lawsofpower.ui.screens.LawDetailScreen
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

@Serializable object LegalRoute

@Serializable data class DetailRoute(val lawNumber: Int)

/** The document is keyed by name rather than by enum ordinal, so reordering the enum cannot break a link. */
@Serializable data class LegalDocumentRoute(val kind: String)

@Composable
fun AppNavGraph(viewModel: LawsViewModel, legalViewModel: LegalViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()

    val laws by viewModel.filteredLaws.collectAsStateWithLifecycle()
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

    val activeTab = backStackEntry?.destination.rootTab()
    // Explicit, rather than relying on rootTab() returning null for the splash. During the first
    // frame after a cold start currentBackStackEntryAsState() can still report the entry restored
    // from saved state while the NavHost is already drawing the splash, which put the bottom bar
    // over the Get Started button and swallowed taps meant for it.
    val onSplash = backStackEntry?.destination?.hasRoute<SplashRoute>() == true
    val haptics = rememberHaptics()

    Scaffold(
        bottomBar = {
            if (activeTab != null && !onSplash) {
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
                startDestination = SplashRoute,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable<SplashRoute> {
                    // Popped, not pushed onto: the Dossier must not sit behind the splash, or a
                    // back gesture from it would return to a dead entry screen. popUpTo the
                    // splash itself, which removes it from the back stack entirely.
                    SplashScreen(
                        onGetStarted = {
                            navController.navigate(DossierRoute) {
                                popUpTo(SplashRoute) { inclusive = true }
                            }
                        },
                    )
                }

                composable<DossierRoute> {
                    DossierScreen(
                        laws = laws,
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
                        onBackClick = {
                            viewModel.clearSelectedLaw()
                            navController.popBackStack()
                        },
                        onToggleBookmark = { viewModel.toggleBookmark(route.lawNumber) },
                    )
                }
            }

            StatusBarScrim(Modifier.align(Alignment.TopCenter))
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
    this?.hasRoute<StrategiesRoute>() == true -> RootDestination.Strategies
    this?.hasRoute<CodexRoute>() == true -> RootDestination.Codex
    this?.hasRoute<LegalRoute>() == true -> RootDestination.Legal
    else -> null
}

private fun NavHostController.switchTab(destination: RootDestination) {
    val route: Any = when (destination) {
        RootDestination.Dossier -> DossierRoute
        RootDestination.Strategies -> StrategiesRoute
        RootDestination.Codex -> CodexRoute
        RootDestination.Legal -> LegalRoute
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
