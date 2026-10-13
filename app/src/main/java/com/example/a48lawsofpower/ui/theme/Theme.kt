package com.example.a48lawsofpower.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The palette is dark-only.
 *
 * The design is two-tone - dark reading surfaces, light browsing ones - but a Material colour
 * scheme is global, so it can only carry one family at a time. Dark is bound here because the
 * reader, where the reader spends the most time, is the surface that matters. Screens on the
 * light family paint their own background and text explicitly rather than switching the scheme,
 * which would repaint every Material component underneath them mid-navigation.
 */
private val ObsidianColorScheme = darkColorScheme(
    primary = Crimson,
    onPrimary = Color.White,
    primaryContainer = CrimsonBright,
    onPrimaryContainer = Color.White,
    secondary = CrimsonBright,
    onSecondary = Color.White,
    tertiary = CrimsonBright,
    onTertiary = Color.White,
    background = Ink,
    onBackground = InkText,
    surface = InkElevated,
    onSurface = InkText,
    surfaceVariant = Color(0xFF26262A),
    onSurfaceVariant = InkTextMuted,
    surfaceContainer = InkElevated,
    surfaceContainerHigh = Color(0xFF26262A),
    surfaceContainerHighest = Color(0xFF2E2E33),
    surfaceContainerLow = Color(0xFF171719),
    surfaceContainerLowest = Ink,
    outline = InkBorder,
    outlineVariant = InkBorder,
    error = TransgressionRed,
    onError = Color.White,
    scrim = Color(0xFF000000),
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
