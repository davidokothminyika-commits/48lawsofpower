package com.example.a48lawsofpower.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Haptic mapping from the design spec.
 *
 *  - [selection]: tab changes and settling onto a new law.
 *  - [impactHeavy]: long-pressing a law to add it to My Strategies.
 */
class Haptics(private val delegate: HapticFeedback) {

    /** Scrolling into a new law card, and swiping between tabs. */
    fun selection() = delegate.performHapticFeedback(HapticFeedbackType.TextHandleMove)

    /** Long-pressing a law to archive or bookmark it. */
    fun impactHeavy() = delegate.performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
fun rememberHaptics(): Haptics {
    val haptics = LocalHapticFeedback.current
    return remember(haptics) { Haptics(haptics) }
}
