package com.example.a48lawsofpower.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a48lawsofpower.ui.progress.StreakCelebration
import com.example.a48lawsofpower.ui.progress.encouragementFor
import com.example.a48lawsofpower.ui.theme.CourtGray
import com.example.a48lawsofpower.ui.theme.Inter
import com.example.a48lawsofpower.ui.theme.LawTheme
import com.example.a48lawsofpower.ui.theme.Metrics
import com.example.a48lawsofpower.ui.theme.Motion
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.SovereignGold
import com.example.a48lawsofpower.ui.theme.Spacing
import com.example.a48lawsofpower.ui.theme.reducedMotion
import kotlinx.coroutines.delay

/** How long the card holds before it leaves on its own. Long enough to read, not to sit. */
private const val CELEBRATION_VISIBLE_MILLIS = 3_800L

/**
 * The streak reward: an animated star, the number, and one line of encouragement.
 *
 * Shown once a day, at the moment the day's reading first clears the streak minimum - which is why
 * it appears over the reader rather than over a list. The reader has just done the thing, and the
 * acknowledgement belongs where the doing happened.
 *
 * It is dismissible by tap as well as by timeout, per the app's rule that an animation must never
 * gate anything: three and a half seconds is a default, not a sentence the reader has to sit
 * through.
 *
 * The card never dismisses *itself* in the sense of hiding content it owns - it reports the
 * dismissal upward, so the celebration that triggered it and the flag that suppressed it cannot
 * drift apart.
 */
@Composable
fun StreakCelebrationOverlay(
    celebration: StreakCelebration?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = celebration != null,
        enter = fadeIn(tween(if (reducedMotion()) 0 else Motion.STANDARD)),
        exit = fadeOut(tween(if (reducedMotion()) 0 else Motion.STANDARD)),
        modifier = modifier,
    ) {
        val dismissSource = remember { MutableInteractionSource() }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .statusBarsPadding()
                .shadow(12.dp, RoundedCornerShape(Metrics.CornerRadius))
                .clip(RoundedCornerShape(Metrics.CornerRadius))
                .background(CourtGray)
                .clickable(
                    interactionSource = dismissSource,
                    indication = null,
                    onClick = onDismiss,
                )
                .padding(Spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // A fixed box around the star, so the row's height is set by the text rather than
                // by the star's graphicsLayer scale - a scaled icon reports its pre-scale bounds, so
                // letting it size the row left the card visibly off-centre.
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    CelebratingStar()
                }
                Spacer(Modifier.width(Spacing.sm))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (celebration?.streak == 1) "STREAK BEGUN" else "DAY STREAK",
                        style = LawTheme.typography.eyebrow,
                        color = SovereignGold,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        // Inter, not Playfair, for the same reason the Profile figures use it: this
                        // is a number the reader is meant to read instantly, and Playfair's round
                        // zero reads as a lowercase "o".
                        text = "${celebration?.streak ?: 0}",
                        style = LawTheme.typography.lawTitle.copy(
                            fontFamily = Inter,
                            fontSize = 34.sp,
                            lineHeight = 38.sp,
                            letterSpacing = 0.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            Hairline()
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = encouragementFor(celebration?.streak ?: 0),
                style = LawTheme.typography.bodySecondary,
                color = ShadowGray,
            )
        }
    }

    // Keyed on the celebration itself, so a new one restarts the clock instead of inheriting the
    // previous one's remaining time. One card at a time by construction - there is no list to grow.
    LaunchedEffect(celebration) {
        if (celebration == null) return@LaunchedEffect
        delay(CELEBRATION_VISIBLE_MILLIS)
        onDismiss()
    }
}

/**
 * The star itself.
 *
 * Two animations rather than one spring: it arrives with a settle, then breathes on a slow loop
 * while it is on screen. A single spring that keeps jiggling reads as a glitch, and an infinitely
 * repeating spring is a battery leak with a nice entrance.
 *
 * Under reduced motion the breathe is dropped and only the opacity changes, per the app's rule that
 * reduced motion means "no movement" rather than "movement, but faster".
 */
@Composable
private fun CelebratingStar(modifier: Modifier = Modifier) {
    val reduced = reducedMotion()

    // animateFloatAsState does not animate on first composition - it starts at the target - so the
    // entrance needs something to flip the target once the star is actually on screen.
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val entrance by animateFloatAsState(
        targetValue = if (entered) 1f else 0.7f,
        animationSpec = tween(
            durationMillis = if (reduced) 0 else Motion.LONG,
            easing = if (reduced) LinearEasing else Motion.Settle,
        ),
        label = "entrance",
    )

    val loop = rememberInfiniteTransition(label = "streak-star")
    val breathe by loop.animateFloat(
        initialValue = 1f,
        targetValue = if (reduced) 1f else 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    Icon(
        imageVector = Icons.Filled.Star,
        contentDescription = null,
        tint = SovereignGold,
        modifier = modifier.size(36.dp).graphicsLayer {
            scaleX = entrance * breathe
            scaleY = entrance * breathe
            // A few degrees of tilt, settled to level. Level at rest matters: a star that ends
            // crooked reads as a mistake rather than a flourish.
            rotationZ = (1f - entrance) * 24f
        },
    )
}