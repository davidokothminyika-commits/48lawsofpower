package com.example.a48lawsofpower.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity

/** What the reader has chosen in Settings. */
enum class ThemeMode {
    /** Follow the OS. The default for a new install. */
    System,
    Light,
    Dark,
}

/**
 * The dark family - the reading surface. Where the words are, and kept dark so long sessions do not
 * sit on white.
 */
private val InkColorScheme = darkColorScheme(
    primary = Crimson,
    onPrimary = Color.White,
    primaryContainer = CrimsonBright,
    onPrimaryContainer = Color.White,
    secondary = CrimsonBright,
    onSecondary = Color.White,
    tertiary = CrimsonBright,
    onTertiary = Color.White,
    tertiaryContainer = ObservanceGreenDark,
    onTertiaryContainer = Color.White,
    background = Ink,
    onBackground = InkText,
    surface = Ink,
    onSurface = InkText,
    surfaceVariant = Color(0xFF26262A),
    onSurfaceVariant = InkTextMuted,
    surfaceContainer = InkElevated,
    surfaceContainerHigh = Color(0xFF242427),
    surfaceContainerHighest = Color(0xFF2E2E33),
    surfaceContainerLow = Color(0xFF171719),
    surfaceContainerLowest = Ink,
    outline = InkBorder,
    outlineVariant = InkBorder,
    error = TransgressionRedDark,
    onError = Color.White,
    scrim = Color(0xFF000000),
)

/**
 * The light family - the browsing surface. Library, notes, profile, and any screen the reader is
 * scanning rather than reading.
 */
private val PaperColorScheme = lightColorScheme(
    primary = Crimson,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF7E3E3),
    onPrimaryContainer = CrimsonDeep,
    secondary = CrimsonDeep,
    onSecondary = Color.White,
    tertiary = CrimsonDeep,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD8EFE1),
    onTertiaryContainer = ObservanceGreenLight,
    background = Paper,
    onBackground = PaperText,
    surface = Paper,
    onSurface = PaperText,
    surfaceVariant = Color(0xFFEEF1F5),
    onSurfaceVariant = PaperTextMuted,
    surfaceContainer = PaperCard,
    surfaceContainerHigh = Color(0xFFF1F4F8),
    surfaceContainerHighest = Color(0xFFE5E7EB),
    surfaceContainerLow = Paper,
    surfaceContainerLowest = Color.White,
    outline = PaperBorder,
    outlineVariant = PaperBorder,
    error = TransgressionRedLight,
    onError = Color.White,
    scrim = Color(0xFF000000),
)

val LocalLawTypography = staticCompositionLocalOf { LawTypography() }

/** True when the dark family is the active one. Screens that paint their own surfaces check this. */
val LocalIsDarkTheme = staticCompositionLocalOf { true }

@Composable
fun _48LawsOfPowerTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    // The system bars are drawn by the OS and do not follow the Material scheme, so they have to be
    // told separately: white status-bar glyphs over the paper family are effectively invisible. This
    // is a SideEffect rather than a LaunchedEffect so it re-applies after every configuration change
    // without ever launching a second time for a value that is already correct.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(
        LocalLawTypography provides LawTypography(),
        LocalIsDarkTheme provides dark,
        LocalLawSemanticColors provides if (dark) DarkSemantics else LightSemantics,
    ) {
        MaterialTheme(
            colorScheme = if (dark) InkColorScheme else PaperColorScheme,
            typography = MaterialTypography,
            content = content,
        )
    }
}

/** Shorthand for `LawTheme.typography.lawTitle` and friends. */
object LawTheme {
    val typography: LawTypography
        @Composable @ReadOnlyComposable
        get() = LocalLawTypography.current

    /** True when the dark family is active. */
    val isDark: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalIsDarkTheme.current

    /** Success and failure colours for the active family. */
    val semantics: LawSemanticColors
        @Composable @ReadOnlyComposable
        get() = LocalLawSemanticColors.current
}