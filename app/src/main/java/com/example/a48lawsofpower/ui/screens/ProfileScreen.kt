package com.example.a48lawsofpower.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a48lawsofpower.data.ReadingStats
import com.example.a48lawsofpower.ui.components.Hairline
import com.example.a48lawsofpower.ui.components.TogglePill
import com.example.a48lawsofpower.ui.components.rememberHaptics
import com.example.a48lawsofpower.ui.profile.RecentDay
import com.example.a48lawsofpower.ui.theme.CourtGray
import com.example.a48lawsofpower.ui.theme.CourtGrayElevated
import com.example.a48lawsofpower.ui.theme.Inter
import com.example.a48lawsofpower.ui.theme.LawTheme
import com.example.a48lawsofpower.ui.theme.Metrics
import com.example.a48lawsofpower.ui.theme.Motion
import com.example.a48lawsofpower.ui.theme.ObservanceGreen
import com.example.a48lawsofpower.ui.theme.ParchmentWhite
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.SovereignGold
import com.example.a48lawsofpower.ui.theme.Spacing
import com.example.a48lawsofpower.ui.theme.ThemeMode
import com.example.a48lawsofpower.ui.theme.reducedMotion

/**
 * Profile: the reader's standing, the things they have kept, and the app's settings.
 *
 * Strategies and Notes live in here rather than on the tab bar. The bar could not carry six
 * destinations without crowding, and these two are not places a reader browses - they are somewhere
 * they *return* to. Reaching them from the profile is also what makes the relationship honest: they
 * are contents of the reader's own record, alongside the stats that describe it.
 *
 * Legal moved here too, for the same reason.
 */
@Composable
fun ProfileScreen(
    stats: ReadingStats,
    week: List<RecentDay>,
    totalLaws: Int,
    masteredLaws: Int,
    savedCount: Int,
    annotatedCount: Int,
    annotatedLaws: Int,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOpenStrategies: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenLegal: () -> Unit,
    /** Reopens the achievement card for the current streak. See [StatRow]. */
    onStreakClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val haptics = rememberHaptics()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = contentPadding,
    ) {
        item(key = "header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = Spacing.xl,
                        start = Spacing.md,
                        end = Spacing.md,
                        bottom = Spacing.md,
                    ),
            ) {
                Text("PROFILE", style = LawTheme.typography.eyebrow, color = ShadowGray)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "Your Progress",
                    style = LawTheme.typography.lawNumber.copy(fontSize = 28.sp, lineHeight = 34.sp),
                    color = ParchmentWhite,
                )
            }
        }

        item(key = "stats") {
            StatRow(
                stats = stats,
                week = week,
                totalLaws = totalLaws,
                masteredLaws = masteredLaws,
                onStreakClick = onStreakClick,
                modifier = Modifier.padding(horizontal = Spacing.md),
            )
        }

        item(key = "your-eyebrow") {
            SectionEyebrow("YOURS", Modifier.padding(top = Spacing.lg))
        }

        item(key = "strategies") {
            SettingsRow(
                icon = Icons.Outlined.BookmarkBorder,
                title = "Strategies",
                subtitle = "$savedCount ${if (savedCount == 1) "law" else "laws"} saved",
                onClick = onOpenStrategies,
                modifier = Modifier.padding(horizontal = Spacing.md),
            )
        }

        item(key = "notes") {
            SettingsRow(
                icon = Icons.Outlined.EditNote,
                title = "Notes",
                subtitle = "$annotatedCount ${if (annotatedCount == 1) "annotation" else "annotations"} across $annotatedLaws ${if (annotatedLaws == 1) "law" else "laws"}",
                onClick = onOpenNotes,
                modifier = Modifier.padding(horizontal = Spacing.md),
            )
        }

        item(key = "appearance-eyebrow") {
            SectionEyebrow("APPEARANCE", Modifier.padding(top = Spacing.lg))
        }

        item(key = "theme") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                ThemeMode.entries.forEach { mode ->
                    TogglePill(
                        label = mode.label(),
                        selected = mode == themeMode,
                        onClick = {
                            haptics.selection()
                            onThemeModeChange(mode)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item(key = "legal-eyebrow") {
            SectionEyebrow("LEGAL", Modifier.padding(top = Spacing.lg))
        }

        item(key = "legal") {
            SettingsRow(
                icon = Icons.Outlined.Gavel,
                title = "Terms, Privacy & Cookies",
                subtitle = "Your consent record and every legal document",
                onClick = onOpenLegal,
                modifier = Modifier.padding(horizontal = Spacing.md),
            )
        }

        item(key = "tail") {
            Spacer(Modifier.height(Spacing.xl * 2))
        }
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.System -> "System"
    ThemeMode.Light -> "Light"
    ThemeMode.Dark -> "Dark"
}

@Composable
private fun SectionEyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = LawTheme.typography.eyebrow,
        color = ShadowGray,
        modifier = modifier.padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.sm),
    )
}

/**
 * The reader's standing, in three tiers rather than one flat grid of six numbers.
 *
 * The previous version was a 2x3 of equal-weight figures, which meant mastery, streak, and days read
 * all looked equally like the thing the reader cared about. None of them did:
 *  - **Mastery** is the arc of the whole project, so it gets a bar and a fraction - "12/48" as bare
 *    text gives no sense of how far along that is.
 *  - **The week** makes the streak concrete. A streak of "4" is an abstraction; four filled cells
 *    with three gaps in them is a picture of an actual week, and it is the only place the reader can
 *    see that today is still unfilled.
 *  - **The rest** is reference and sits at the bottom in ordinary type.
 *
 * The streak figure also doubles as the way back into the achievement card. That celebration is
 * ephemeral by design - it plays once, for a few seconds, and acknowledging it is what stops it
 * coming back - so a reader who wants to look at it again has no other route. A streak of zero has
 * nothing to celebrate, so the tap is only offered when there is one.
 *
 * "Annotations" was dropped from here entirely: the Notes row below already says the same number
 * with more context, and two identical figures on one screen reads as an error.
 */
@Composable
private fun StatRow(
    stats: ReadingStats,
    week: List<RecentDay>,
    totalLaws: Int,
    masteredLaws: Int,
    onStreakClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Metrics.CornerRadius))
            .background(CourtGray)
            .padding(Spacing.md),
    ) {
        MasteryBar(mastered = masteredLaws, total = totalLaws)
        Spacer(Modifier.height(Spacing.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Stat(
                label = "Streak",
                value = "${stats.currentStreak}d",
                tint = ObservanceGreen,
                modifier = Modifier.weight(1f),
                onClick = if (stats.currentStreak > 0) onStreakClick else null,
            )
            Stat("Best", "${stats.longestStreak}d", ShadowGray, Modifier.weight(1f))
            Stat("Days read", "${stats.totalDays}", ParchmentWhite, Modifier.weight(1f))
        }
        Spacer(Modifier.height(Spacing.md))
        Hairline()
        Spacer(Modifier.height(Spacing.md))
        WeekStrip(week = week, todayQualified = stats.todayQualifies)
    }
}

/**
 * Mastery as a proportion of the whole corpus.
 *
 * Animates its fill, because a bar that snaps to 48/48 gives no sense of the journey and this is
 * the one figure on the screen that only ever moves one way. Under reduced motion it renders at its
 * final width immediately - the app's rule is that reduced motion means no movement rather than
 * movement that is merely faster.
 */
@Composable
private fun MasteryBar(mastered: Int, total: Int, modifier: Modifier = Modifier) {
    val fraction = if (total <= 0) 0f else mastered.toFloat() / total
    val target by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(if (reducedMotion()) 0 else Motion.LONG, easing = Motion.Settle),
        label = "mastery",
    )

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "$mastered of $total",
                // Inter, not Playfair: Playfair's zero is a near-circle and reads as a lowercase
                // "o", so "12 of 48" printed in it looks wrong at exactly the size the eye lands on.
                style = LawTheme.typography.lawTitle.copy(
                    fontFamily = Inter,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    letterSpacing = 0.sp,
                ),
                color = ParchmentWhite,
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = "laws mastered",
                style = LawTheme.typography.bodySecondary,
                color = ShadowGray,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(CourtGrayElevated),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(target)
                    .fillMaxHeight()
                    .background(SovereignGold),
            )
        }
    }
}

/**
 * The last seven days, one cell each, oldest at the left.
 *
 * A day that cleared the streak minimum is filled; a day that did not is not. Cells are always
 * seven wide so the row does not reflow as the week fills in.
 *
 * Today is outlined rather than filled when it has not been read yet - "nothing here" and "you have
 * not got to it yet" are different states, and only the second is recoverable.
 */
@Composable
private fun WeekStrip(week: List<RecentDay>, todayQualified: Boolean, modifier: Modifier = Modifier) {
    if (week.isEmpty()) return

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            week.forEach { day ->
                val isToday = day == week.last()
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    day.qualifies -> ObservanceGreen
                                    isToday -> CourtGrayElevated
                                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                            ),
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = day.initial,
                        style = LawTheme.typography.eyebrow,
                        color = if (isToday) ParchmentWhite else ShadowGray,
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = if (todayQualified) {
                "Today is banked. The streak holds."
            } else {
                "Read for a minute to bank today."
            },
            style = LawTheme.typography.bodySecondary,
            color = if (todayQualified) ObservanceGreen else ShadowGray,
        )
    }
}

/**
 * Single-letter weekday, precomputed by the ViewModel.
 *
 * Kept off this screen deliberately: resolving it here means `LocalDate` is first touched during
 * composition on the main thread, and that one-time initialisation was measurably the slowest thing
 * on the tab. See [RecentDay.initial].
 */

/**
 * One figure: the value, then its label.
 *
 * [onClick] is null for the two figures that are reference only. Conditional rather than a no-op
 * lambda because a clickable with an empty action still announces itself to a screen reader as
 * something that does something, and "Best: 5d, double tap to activate" with nothing behind it is
 * worse than no affordance at all.
 */
@Composable
private fun Stat(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = if (onClick != null) {
            modifier.clickable(onClick = onClick)
        } else {
            modifier
        },
    ) {
        Text(
            text = value,
            style = LawTheme.typography.lawTitle.copy(
                fontFamily = Inter,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                letterSpacing = 0.sp,
            ),
            color = tint,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(text = label, style = LawTheme.typography.bodySecondary, color = ShadowGray)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Metrics.CornerRadius))
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.md, horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(Metrics.IconButtonSize)
                .clip(RoundedCornerShape(Metrics.CornerRadius))
                .background(SovereignGold.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = SovereignGold, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(text = title, style = LawTheme.typography.lawTitle.copy(fontSize = 17.sp, lineHeight = 22.sp), color = ParchmentWhite)
            Spacer(Modifier.height(Spacing.xs))
            Text(text = subtitle, style = LawTheme.typography.bodySecondary, color = ShadowGray)
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = ShadowGray,
            modifier = Modifier.size(20.dp),
        )
    }
}