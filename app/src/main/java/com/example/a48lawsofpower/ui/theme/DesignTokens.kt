package com.example.a48lawsofpower.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette from the "Reading App UI/UX" design.
 *
 * Two families, because the design is two-tone by intent and collapsing it to one would lose the
 * contrast the reference relies on:
 *
 *  - [Ink] for the reading surfaces. The reader is where the words are, and it stays dark so long
 *    sessions do not sit on white.
 *  - [Paper] for the browsing surfaces - library, notes, profile. Lists read better on light.
 *
 * [Crimson] is the single accent. In the reference it marks the primary action, the active tab,
 * and the law number badge - carrying all three at once, so it has to hold up against both a
 * near-black and a near-white field.
 */

/** Crimson, the primary. #991C1C as specified. */
val Crimson = Color(0xFF991C1C)

/** Lighter crimson for pressed/hover states on the dark family. */
val CrimsonBright = Color(0xFFB32626)

/** Dark surface scale. #111111 base, lifted by two steps for cards and pressed rows. */
val Ink = Color(0xFF111111)
val InkElevated = Color(0xFF1C1C1E)
val InkBorder = Color(0xFF2A2A2E)

/** Light surface scale. #F8FAFC base, with #E5E7EB for cards and dividers. */
val Paper = Color(0xFFF8FAFC)
val PaperCard = Color(0xFFFFFFFF)
val PaperBorder = Color(0xFFE5E7EB)

/** Body text on the light family. #374151 as specified. */
val PaperText = Color(0xFF374151)

/** Muted text on either family. Sits below body contrast, so never used for prose. */
val InkTextMuted = Color(0xFF8E8E93)
val PaperTextMuted = Color(0xFF6B7280)

/** On-dark text. #F5F5F7, unchanged from the original theme. */
val InkText = Color(0xFFF5F5F7)

/**
 * Deeper crimson for the light family.
 *
 * The accent has to hold up against a near-black *and* a near-white field, which is the whole reason
 * it is the only accent. On paper it goes a step darker rather than lighter: #B32626 on #F8FAFC is
 * about 4.0:1 - fine for the large display numerals, short of the 4.5:1 body minimum for the small
 * labels that use it. So the light family runs #991C1C for text and reserves the brighter crimson
 * for fills behind white type.
 */
val CrimsonDeep = Color(0xFF7A1616)

/**
 * Semantic feedback colours, per family.
 *
 * These are *text* colours. On dark, #E53E3E and #38A169 clear the background easily; on paper they
 * do not - #38A169 on #F8FAFC is roughly 2.8:1, which is not legible. Hence a second set rather than
 * one shared pair.
 */
val TransgressionRedDark = Color(0xFFE53E3E)
val TransgressionRedLight = Color(0xFFB3261E)
val ObservanceGreenDark = Color(0xFF38A169)
val ObservanceGreenLight = Color(0xFF1B6B43)

/*
 * The original palette - ObsidianBlack, SovereignGold, ParchmentWhite, ShadowGray, CourtGray,
 * CourtGrayElevated, HairlineBorder, BurnishedBrass, ObservanceGreen, TransgressionRed - still
 * lives in Color.kt. Every screen imports it, so nothing is redeclared here. Migrating a screen
 * means swapping its imports to the tokens above; once the last one is converted, Color.kt goes
 * and the crimson family becomes the only palette.
 */