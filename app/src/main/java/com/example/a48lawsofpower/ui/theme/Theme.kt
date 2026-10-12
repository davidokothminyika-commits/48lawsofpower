package com.example.a48lawsofpower.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The palette is deliberately dark-only. The design calls for a single monolithic Obsidian Black
 * canvas; offering a light variant would dilute the editorial premise, so there is nothing to
 * switch on and nothing to animate when the system theme changes.
 */
private val ObsidianColorScheme = darkColorScheme(
    primary = SovereignGold,
    onPrimary = ObsidianBlack,
    primaryContainer = BurnishedBrass,
    onPrimaryContainer = ParchmentWhite,
    secondary = BurnishedBrass,
    onSecondary = ObsidianBlack,
    tertiary = SovereignGold,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = ParchmentWhite,
    surface = CourtGray,
    onSurface = ParchmentWhite,
    surfaceVariant = CourtGrayElevated,
    onSurfaceVariant = ShadowGray,
    surfaceContainer = CourtGray,
    surfaceContainerHigh = CourtGrayElevated,
    surfaceContainerHighest = CourtGrayElevated,
    surfaceContainerLow = CourtGray,
    surfaceContainerLowest = ObsidianBlack,
    outline = HairlineBorder,
    outlineVariant = HairlineBorder,
    error = TransgressionRed,
    onError = ParchmentWhite,
    scrim = ObsidianBlack,
)

val LocalLawTypography = staticCompositionLocalOf { LawTypography() }

@Composable
fun _48LawsOfPowerTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLawTypography provides LawTypography()) {
        MaterialTheme(
            colorScheme = ObsidianColorScheme,
            typography = MaterialTypography,
            content = content,
        )
    }
}

/** Shorthand for `LawTheme.typography.lawTitle` and friends. */
object LawTheme {
    val typography: LawTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalLawTypography.current
}
