package com.example.a48lawsofpower.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.a48lawsofpower.R

/**
 * Playfair Display for display and headings, Inter for everything read at length.
 *
 * Bundled rather than fetched: the app is offline-only, and a downloadable font would stall the
 * first render on a network round trip. Both are SIL OFL 1.1 - see `ATTRIBUTIONS.md`.
 *
 * The files are Latin-subset static instances; see the tooling note there. Compose synthesises bold
 * from a medium face when a component asks for a weight that is missing, which reads acceptably on
 * Inter but visibly fakes weight on Playfair, so both display weights are shipped.
 */
internal val Playfair = FontFamily(
    Font(R.font.playfair_display_regular, FontWeight.Normal),
    Font(R.font.playfair_display_medium, FontWeight.Medium),
    Font(R.font.playfair_display_semibold, FontWeight.SemiBold),
    Font(R.font.playfair_display_bold, FontWeight.Bold),
)

internal val Inter = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

/**
 * The editorial type hierarchy: a classic serif for law headings, a high-legibility geometric
 * sans for commentary.
 *
 * Every size is declared in `sp` so the system font scale (Dynamic Type) is honoured end to end,
 * including on the long analytical passages of the reader.
 */
@Immutable
data class LawTypography(
    /** 40/46 - large backdrop or leading numbers. */
    val lawNumber: TextStyle = TextStyle(
        fontFamily = Playfair,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = 2.sp,
    ),
    /** 22/28 - core statements of the 48 laws. */
    val lawTitle: TextStyle = TextStyle(
        fontFamily = Playfair,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.2.sp,
    ),
    /** 16/24 - deep-dive interpretations, stories and keys. */
    val bodyPrimary: TextStyle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp,
    ),
    /** 14/20 - historical examples, transgressions and footnotes. */
    val bodySecondary: TextStyle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Light,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    ),
    /** 15/18 - bottom tab bar, interactive pill tags, actions. */
    val actionLabel: TextStyle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.4.sp,
    ),
    /** Supporting role for the small serif index printed beside a card title. */
    val lawIndex: TextStyle = TextStyle(
        fontFamily = Playfair,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 2.sp,
    ),
    /** Section eyebrows above each paged block. */
    val eyebrow: TextStyle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 2.sp,
    ),
)

/**
 * Material 3 mirror of [LawTypography] so stock components (TopAppBar, BottomBar slots, ...)
 * inherit the same editorial voice instead of the default Material scale.
 */
val MaterialTypography = Typography(
    displayLarge = LawTypography().lawNumber,
    headlineMedium = LawTypography().lawTitle,
    titleMedium = LawTypography().lawTitle,
    bodyLarge = LawTypography().bodyPrimary,
    bodyMedium = LawTypography().bodyPrimary,
    bodySmall = LawTypography().bodySecondary,
    labelLarge = LawTypography().actionLabel,
    labelMedium = LawTypography().actionLabel,
    labelSmall = LawTypography().bodySecondary,
)
