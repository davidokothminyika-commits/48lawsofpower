package com.example.a48lawsofpower.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

/**
 * Motion tokens.
 *
 * Durations come from the app animation guidelines, section 2, restricted to the four tokens
 * that actually have a use here. The guidance was written for an audio player, so its playback
 * timings - progress bar, play/pause morph, mini-player expansion - have no equivalent in a
 * reading app and are deliberately absent rather than translated into something meaningless.
 *
 * The rule that did survive unchanged is section 1's: animations must never gate an action.
 * Nothing in this app waits on an animation to finish before doing the work the reader asked for.
 */
object Motion {

    /** 120-160ms. Icon taps, toggles, pressed state. */
    const val FAST = 140

    /** 180-240ms. Tabs, menus, list changes, screen push/pop. */
    const val STANDARD = 200

    /** 260-320ms. Full-screen reveals - the splash, a reader entering. */
    const val EMPHASIS = 280

    /** 350-450ms. First-run transitions only. */
    const val LONG = 400

    val Fast: Easing = EaseOut
    val Standard: Easing = EaseInOut

    /**
     * A decelerating curve rather than linear or a spring.
     *
     * The guidelines ask for spring or bounce "very lightly" at most. This is a mild
     * ease-out-back, used only on the splash piece where a little settle reads as weight. Every
     * other transition uses [Fast] or [Standard].
     */
    val Settle: Easing = CubicBezierEasing(0.2f, 1.1f, 0.3f, 1f)
}

/**
 * True when the reader has asked the system to reduce or disable animation.
 *
 * Honoured per section 12: under reduced motion, large movement and scaling are dropped in
 * favour of a brief fade. Read from the global animator duration scale, which is the setting
 * Android exposes for exactly this and which Accessibility's "Remove animations" writes to.
 *
 * Not remembered as a val across recompositions - the reader can change it from the system
 * settings while the app is backgrounded, and the next composition should see the new value.
 */
@Composable
@ReadOnlyComposable
fun reducedMotion(): Boolean {
    val context = LocalContext.current
    val scale: Float = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )
    return scale == 0f
}

/**
 * A duration for [token] that collapses to zero under reduced motion.
 *
 * An animation with a zero duration still runs its keyframes, so state changes land immediately
 * instead of being queued behind a transition the reader did not ask for. That matches the
 * guideline's "immediate state change" rather than substituting a fade everywhere - a fade is
 * still movement, and some readers turn animation off precisely to avoid it.
 */
@Composable
@ReadOnlyComposable
fun motionDuration(token: Int): Int = if (reducedMotion()) 0 else token