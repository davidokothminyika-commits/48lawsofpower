package com.example.a48lawsofpower.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The handful of colours Material 3 has no slot for.
 *
 * Everything else in the app resolves through `MaterialTheme.colorScheme`, which carries one family
 * at a time and is swapped whole by [_48LawsOfPowerTheme]. Success and failure are the exception:
 * they mean the same thing in both families, but the dark-family values sit too close to the paper
 * family's background to read as text, so each family needs its own.
 */
@Immutable
data class LawSemanticColors(
    /** Reads against body text: "observed", "mastered", a positive delta. */
    val success: Color,
    /** Reads against body text: "transgression", a destructive action. */
    val danger: Color,
    /** Success as a filled block, for a status chip. */
    val onSuccess: Color,
    /** Danger as a filled block, for a destructive button. */
    val onDanger: Color,
)

/** Built from tokens rather than from the names below, which resolve per-composition. */
internal val DarkSemantics = LawSemanticColors(
    success = ObservanceGreenDark,
    danger = TransgressionRedDark,
    onSuccess = Ink,
    onDanger = Color.White,
)

internal val LightSemantics = LawSemanticColors(
    success = ObservanceGreenLight,
    danger = TransgressionRedLight,
    onSuccess = Color.White,
    onDanger = Color.White,
)

val LocalLawSemanticColors = staticCompositionLocalOf { DarkSemantics }

/**
 * The app's colour names, bound to whichever Material family is active.
 *
 * These started as plain `val`s pointing at a fixed palette. They are composable getters instead,
 * because the app ships a light family as well as a dark one and a fixed `val` cannot answer the
 * question. Keeping the *names* means screens that already read `ParchmentWhite` or `ShadowGray`
 * adapt to the light family without being touched - the semantic role survives, only its value moves.
 *
 * Two rules keep this honest:
 *
 *  - Use a name that matches its role, not its look. A name like `textMuted` stays readable on both
 *    families; a name that describes a hue would not.
 *  - Do not add raw colours here. If a screen needs a colour that is not in the design, add it to
 *    [DesignTokens] and alias it below, so the palette stays the single source of truth.
 *
 * Everything below is @Composable. That is the cost of the indirection: a name read inside a
 * `drawBehind` block, or used as a default argument, will not compile. Hoist the value into the
 * enclosing composable and close over it there instead.
 */

/** Base canvas. */
val ObsidianBlack: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.background

/** Cards, sheets, list rows. */
val CourtGray: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceContainer

/** One step above a card: pressed states, selected pills, menus. */
val CourtGrayElevated: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceContainerHighest

/** Hairline dividers and card outlines. */
val HairlineBorder: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outline

/** Primary text. */
val ParchmentWhite: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onBackground

/** Secondary text: metadata, labels, counts. Below body contrast, so never for prose. */
val ShadowGray: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant

/** The single accent. Carries the primary action, the active tab, the law-number badge. */
val SovereignGold: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary

/** Pressed / hover accent, one step brighter on dark and deeper on light. */
val BurnishedBrass: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.tertiary

/** Success, readable as text on either family. */
val ObservanceGreen: Color
    @Composable @ReadOnlyComposable get() = LocalLawSemanticColors.current.success

/** Failure, readable as text on either family. */
val TransgressionRed: Color
    @Composable @ReadOnlyComposable get() = LocalLawSemanticColors.current.danger