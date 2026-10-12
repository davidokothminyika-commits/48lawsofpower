package com.example.a48lawsofpower.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Strict 8-point grid. Large, intentional gaps establish the pacing the design calls for.
 */
object Spacing {
    val xs: Dp = 4.dp    // Law status dots, micro-gaps
    val sm: Dp = 8.dp    // Subtitle to title spacing
    val md: Dp = 16.dp   // Standard phone edge margin, card structural padding
    val lg: Dp = 24.dp   // Between historical case blocks and analysis modules
    val xl: Dp = 40.dp   // Hero presentation gaps, top of view margins
}

/** Component metrics from the design specification. */
object Metrics {
    /** Formal 8px corners - sharp, not bubbly. */
    val CornerRadius: Dp = 8.dp
    /** Pills are the one exception: soft, 20px. */
    val PillRadius: Dp = 20.dp
    val HairlineWidth: Dp = 1.dp

    val PillHeight: Dp = 38.dp
    /** 44dp is the absolute minimum touch target. */
    val IconButtonSize: Dp = 44.dp

    val LawCardMinHeight: Dp = 120.dp
}
