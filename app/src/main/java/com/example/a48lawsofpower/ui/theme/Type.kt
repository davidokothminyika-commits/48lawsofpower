package com.example.a48lawsofpower.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The editorial type hierarchy: classic serif display faces for law headings, high-legibility
 * geometric sans-serifs for commentary.
 *
 * Every size is declared in `sp` so the system font scale (Dynamic Type) is honoured end to end,
 * including on the long analytical passages of the reader.
 */
@Immutable
data class LawTypography(
    /** 40/46 - large backdrop or leading numbers. */
    val lawNumber: TextStyle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = 2.sp,
    ),
    /** 22/28 - core statements of the 48 laws. */
    val lawTitle: TextStyle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.2.sp,
    ),
    /** 16/24 - deep-dive interpretations, stories and keys. */
    val bodyPrimary: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp,
    ),
    /** 14/20 - historical examples, transgressions and footnotes. */
    val bodySecondary: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    ),
    /** 15/18 - bottom tab bar, interactive pill tags, actions. */
    val actionLabel: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.4.sp,
    ),
    /** Supporting role for the small serif index printed beside a card title. */
    val lawIndex: TextStyle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 2.sp,
    ),
    /** Section eyebrows above each paged block. */
    val eyebrow: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
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
