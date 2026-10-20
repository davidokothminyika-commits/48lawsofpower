package com.example.a48lawsofpower.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a48lawsofpower.ui.progress.StreakAchievement
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
private const val ACHIEVEMENT_VISIBLE_MILLIS = 3_800L

/**
 * Stars shown as a tally, capped rather than equal to the streak.
 *
 * Five is where a row stops reading as a count and starts reading as decoration: past it the number
 * already does the talking, and ten gold stars under "10" is a scoreboard, not a reward.
 */
private const val MAX_STARS = 5

/** Stagger between each star's entrance, so the row arrives as a sweep rather than a block. */
private const val POP_STAGGER_MILLIS = 90L

/** The number counts up rather than snapping - the last beat of the award, and it names the goal. */
private const val COUNT_UP_MILLIS = 600

/**
 * The streak achievement: animated stars, the number of days, and "STREAK UNLOCKED" the first time.
 *
 * What makes it appear is not this composable's concern, and that is deliberate: the caller hands it
 * a [StreakAchievement] or null, and the rule that decides - a banked day newer than the last one
 * the reader was congratulated for - lives with the data that answers it (`pendingStreakAchievement`).
 * One rule covers the day being earned in the reader, a launch that never got to show it, and data
 * restored onto a new device, and none of those cases can drift apart from each other because none
 * of them are written here.
 *
 * What *is* here is the dismissal contract: the card removes itself by tap or after
 * [ACHIEVEMENT_VISIBLE_MILLIS], and reports upward rather than hiding itself, so the overlay's
 * content and the acknowledgment the caller writes when it leaves cannot fall out of step. An
 * animation must never gate anything - three and a half seconds is a default, not a sentence the
 * reader has to sit through - which is also why nothing underneath is blocked while this is up.
 */
@Composable
fun StreakAchievementOverlay(
    achievement: StreakAchievement?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = achievement != null,
        enter = fadeIn(tween(if (reducedMotion()) 0 else Motion.STANDARD)),
        exit = fadeOut(tween(if (reducedMotion()) 0 else Motion.STANDARD)),
        modifier = modifier,
    ) {
        val streak = achievement?.streak ?: 0
        val firstEver = achievement?.isFirstEver == true
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
                        // Only the very first achievement unlocks anything - after that the reader
                        // is extending something they already hold, and repeating "unlocked" for a
                        // seventh day would make the word stop meaning anything.
                        text = if (firstEver) "STREAK UNLOCKED" else "DAY STREAK",
                        style = LawTheme.typography.eyebrow,
                        color = SovereignGold,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    StreakCount(streak)
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            StarRow(streak.coerceAtMost(MAX_STARS))
            Spacer(Modifier.height(Spacing.sm))
            Hairline()
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = encouragementFor(streak),
                style = LawTheme.typography.bodySecondary,
                color = ShadowGray,
            )
        }
    }

    // Keyed on the achievement itself, so a new one restarts the clock instead of inheriting the
    // previous one's remaining time. One card at a time by construction - there is no list to grow.
    LaunchedEffect(achievement) {
        if (achievement == null) return@LaunchedEffect
        delay(ACHIEVEMENT_VISIBLE_MILLIS)
        onDismiss()
    }
}

/**
 * The day count, counting up from zero to [streak] once the card is on screen.
 *
 * `animateIntAsState` does not animate on first composition - it starts at the target - so the
 * count needs something to flip the target after the card actually lands, which is the same trick
 * the star uses.
 *
 * The *accessibility* value is the final number, not whatever the count is passing through: a
 * screen reader that focused mid-animation would otherwise announce "three" for a five day streak
 * and never correct itself, and no one re-reads a card they can see.
 */
@Composable
private fun StreakCount(streak: Int, modifier: Modifier = Modifier) {
    val reduced = reducedMotion()
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val count by animateIntAsState(
        targetValue = if (entered) streak else 0,
        animationSpec = tween(if (reduced) 0 else COUNT_UP_MILLIS, easing = Motion.Settle),
        label = "streak-count",
    )

    Text(
        // Inter, not Playfair, for the same reason the Profile figures use it: this is a number the
        // reader is meant to read instantly, and Playfair's round zero reads as a lowercase "o".
        text = "$count",
        style = LawTheme.typography.lawTitle.copy(
            fontFamily = Inter,
            fontSize = 34.sp,
            lineHeight = 38.sp,
            letterSpacing = 0.sp,
        ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.semantics { contentDescription = "$streak" },
    )
}

/**
 * The tally: [count] small stars popping in one after another, then breathing together.
 *
 * The stagger and the breathe are separate animations on purpose - the stagger is a one-shot that
 * happens exactly once per card, the breathe is a loop that runs while it is on screen, and folding
 * them into a single spring gave the row a settle that never quite finished, which reads as
 * jitters rather than as life.
 *
 * Under reduced motion the stagger is skipped and the breathe holds at 1, per the app's rule that
 * reduced motion means "no movement" rather than "movement, but faster".
 */
@Composable
private fun StarRow(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    val reduced = reducedMotion()

    val loop = rememberInfiniteTransition(label = "achievement-stars")
    val breathe by loop.animateFloat(
        initialValue = 1f,
        targetValue = if (reduced) 1f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        // No key: the count only ever changes when a different card is composed, and the default
        // value of each remember slot still starts that star at zero, which is the entrance.
        repeat(count) { index ->
            var popped by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                if (!reduced) delay(index * POP_STAGGER_MILLIS)
                popped = true
            }
            val pop by animateFloatAsState(
                targetValue = if (popped) 1f else 0f,
                animationSpec = tween(
                    durationMillis = if (reduced) 0 else Motion.LONG,
                    easing = if (reduced) LinearEasing else Motion.Settle,
                ),
                label = "star-$index",
            )
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = SovereignGold,
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer {
                        scaleX = pop * breathe
                        scaleY = pop * breathe
                        alpha = pop
                        // A few degrees of tilt, settled to level. Level at rest matters: a star
                        // that ends crooked reads as a mistake rather than a flourish.
                        rotationZ = (1f - pop) * 20f
                    },
            )
        }
    }
}

/**
 * The large star.
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
