package com.example.a48lawsofpower.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.a48lawsofpower.ui.theme.CourtGray
import com.example.a48lawsofpower.ui.theme.HairlineBorder
import com.example.a48lawsofpower.ui.theme.LawTheme
import com.example.a48lawsofpower.ui.theme.Metrics
import com.example.a48lawsofpower.ui.theme.Spacing
import com.example.a48lawsofpower.ui.theme.reducedMotion

/**
 * A placeholder block that breathes rather than blinks.
 *
 * Shown while the first read of the laws table is in flight. The point is continuity, not
 * decoration: the placeholders are the same shape as the real cards, so the list does not jump when
 * the data lands. A spinner would centre a circle on screen for a fraction of a second and then
 * vanish, which reads worse than simply not having drawn anything yet.
 *
 * Under reduced motion the sweep stops and the block holds a flat fill. A shimmer is movement, and
 * a reader who has asked for less of it should get less of it.
 */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Metrics.CornerRadius,
    minHeight: Dp = 0.dp,
) {
    val calm = reducedMotion()

    // Hoisted out of the draw lambda: drawWithCache runs in DrawScope, which is not @Composable and
    // so cannot read a theme value itself.
    val baseFill = CourtGray
    val bandTint = HairlineBorder

    // -1 to 1 across the width plus its own length, so the band enters and exits cleanly at the
    // edges instead of popping.
    val sweep by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (calm) 0 else 1400),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )

    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(cornerRadius))
            .background(baseFill)
            // A purely horizontal sweep drawn as a second background rather than through a
            // DrawScope. Giving start and end the same y keeps it size-independent, so the band does
            // not need the element's width to be laid out correctly - which is what let this stay a
            // plain `background` instead of a draw lambda.
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        bandTint.copy(alpha = 0f),
                        bandTint.copy(alpha = 0.22f),
                        bandTint.copy(alpha = 0f),
                    ),
                    start = Offset(sweep * 900f - 280f, 0f),
                    end = Offset(sweep * 900f + 280f, 0f),
                ),
            ),
    )
}

/**
 * A stand-in for one law card: the number badge, two lines of title and two of body, in the same
 * proportions as [LawCardPreview].
 */
@Composable
fun SkeletonLawCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        SkeletonBlock(
            modifier = Modifier.size(40.dp),
            cornerRadius = 20.dp,
        )
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(16.dp),
            )
            Spacer(Modifier.height(Spacing.sm))
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(12.dp),
            )
            Spacer(Modifier.height(Spacing.xs))
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(12.dp),
            )
        }
    }
}

/** A short list of them. Six is roughly one screen, which is all that is ever visible at once. */
@Composable
fun SkeletonLawList(
    count: Int = 6,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Top,
    ) {
        repeat(count) { index ->
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                cornerRadius = Metrics.CornerRadius,
            )
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}