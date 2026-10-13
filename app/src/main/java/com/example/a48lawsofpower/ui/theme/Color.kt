package com.example.a48lawsofpower.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The app's colour names, bound to the palette in [DesignTokens].
 *
 * Every screen in the app imports from here by name rather than naming a hex value, which is why
 * the retheme was a one-file change: the names below were re-pointed at the crimson palette and
 * every screen followed.
 *
 * Do not add raw colours here. If a screen needs a colour that is not in the design, add it to
 * [DesignTokens] and alias it below, so the palette stays the single source of truth.
 */

/**
 * Obsidian Black scale - the dark canvas. Rebound from #0A0A0B to the design's #111111 family.
 */
val ObsidianBlack = Ink
val CourtGray = InkElevated
val CourtGrayElevated = Color(0xFF26262A)
val HairlineBorder = InkBorder

/**
 * Parchment / Shadow type colours.
 */
val ParchmentWhite = InkText
val ShadowGray = InkTextMuted

/**
 * Authority accents. This is the visible change: gold is gone, crimson carries the accent.
 */
val SovereignGold = Crimson
val BurnishedBrass = CrimsonBright

/**
 * Semantic feedback states. Unchanged - these mean success and failure, not brand.
 */
val TransgressionRed = Color(0xFFE53E3E)
val ObservanceGreen = Color(0xFF38A169)