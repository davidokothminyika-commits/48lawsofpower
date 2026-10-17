package com.example.a48lawsofpower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.a48lawsofpower.ui.theme.CourtGray
import com.example.a48lawsofpower.ui.theme.Inter
import com.example.a48lawsofpower.ui.theme.LawTheme
import com.example.a48lawsofpower.ui.theme.Metrics
import com.example.a48lawsofpower.ui.theme.ObservanceGreen
import com.example.a48lawsofpower.ui.theme.ParchmentWhite
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.SovereignGold
import com.example.a48lawsofpower.ui.theme.Spacing
import com.example.a48lawsofpower.ui.theme.ThemeMode
import com.example.a48lawsofpower.ui.theme.TransgressionRed

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = Spacing.xl,
                        start = Spacing.md,
                        end = Spacing.md,
                        bottom = Spacing.md,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The avatar. There is no account, so it carries no identity and no initials - it is
                // a mark that this screen is about the reader, not a portrait of one.
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(CourtGray),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonOutline,
                        contentDescription = null,
                        tint = SovereignGold,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Text("PROFILE", style = LawTheme.typography.eyebrow, color = ShadowGray)
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = "Your Progress",
                        style = LawTheme.typography.lawNumber.copy(fontSize = 28.sp, lineHeight = 34.sp),
                        color = ParchmentWhite,
                    )
                }
            }
        }

        item(key = "stats") {
            StatRow(
                stats = stats,
                totalLaws = totalLaws,
                masteredLaws = masteredLaws,
                annotatedCount = annotatedCount,
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

        item(key = "theme-note") {
            Text(
                text = "The reader is built for the dark family, but both are supported end to end. " +
                    "Highlighting and note colours stay fixed in both, so your marks never change.",
                style = LawTheme.typography.bodySecondary,
                color = ShadowGray,
                modifier = Modifier.padding(
                    start = Spacing.md,
                    end = Spacing.md,
                    top = Spacing.sm,
                ),
            )
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

@Composable
private fun StatRow(
    stats: ReadingStats,
    totalLaws: Int,
    masteredLaws: Int,
    annotatedCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Metrics.CornerRadius))
            .background(CourtGray)
            .padding(Spacing.md),
    ) {
        Row {
            Stat("Mastered", "$masteredLaws/$totalLaws", SovereignGold, Modifier.weight(1f))
            Stat("Streak", "${stats.currentStreak}d", ObservanceGreen, Modifier.weight(1f))
            Stat("Best", "${stats.longestStreak}d", ShadowGray, Modifier.weight(1f))
        }
        Spacer(Modifier.height(Spacing.md))
        Hairline()
        Spacer(Modifier.height(Spacing.md))
        Row {
            Stat("Days read", "${stats.totalDays}", ParchmentWhite, Modifier.weight(1f))
            Stat("Annotations", "$annotatedCount", ParchmentWhite, Modifier.weight(1f))
            // Minutes, not laws. A day now counts toward the streak on reading time rather than on
            // laws opened, so "today" has to be measured the same way or the two figures contradict
            // each other on screen.
            Stat(
                label = "Minutes today",
                value = "${stats.totalMinutes}",
                tint = if (stats.todayQualifies) ObservanceGreen else ShadowGray,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(Spacing.md))
        Hairline()
        Spacer(Modifier.height(Spacing.md))
        DailyGoal(stats)
    }
}

/**
 * How close today's reading is to banking the day.
 *
 * Sits under the figures rather than replacing any of them, because the streak is the *outcome*
 * and this is the *remaining work* - the reader should be able to see both without one hiding the
 * other. Green when the day is already safe, so the state is legible without reading the words.
 */
@Composable
private fun DailyGoal(stats: ReadingStats, modifier: Modifier = Modifier) {
    val goalMet = stats.todayQualifies
    val remaining = stats.secondsToGoal()

    Column(modifier.fillMaxWidth()) {
        Text(
            text = if (goalMet) {
                "Today is banked. The streak holds."
            } else {
                "Read for ${(remaining / 60.0f).coerceAtLeast(1.0f).toInt()} more " +
                    "${if (remaining <= 60) "minute" else "minutes"} to keep it."
            },
            style = LawTheme.typography.bodySecondary,
            color = if (goalMet) ObservanceGreen else ShadowGray,
        )
    }
}

@Composable
private fun Stat(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            // Inter, not Playfair. Playfair's zero is a near-circle and reads as a lowercase "o",
            // so "0/48" printed in it looks like "o/48" - and these are the numbers a reader scans
            // for. The serif is kept for the headings, where it belongs.
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