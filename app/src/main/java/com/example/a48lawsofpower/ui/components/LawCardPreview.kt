package com.example.a48lawsofpower.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a48lawsofpower.data.LawOverview
import com.example.a48lawsofpower.ui.theme.CourtGray
import com.example.a48lawsofpower.ui.theme.CourtGrayElevated
import com.example.a48lawsofpower.ui.theme.HairlineBorder
import com.example.a48lawsofpower.ui.theme.LawTheme
import com.example.a48lawsofpower.ui.theme.Metrics
import com.example.a48lawsofpower.ui.theme.ObservanceGreen
import com.example.a48lawsofpower.ui.theme.ParchmentWhite
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.SovereignGold
import com.example.a48lawsofpower.ui.theme.Spacing

/**
 * A law as it appears in a feed.
 *
 * Restyled to the reference design: a crimson disc carrying the law number, a serif title, a
 * single clamped line of summary, and a chevron. The previous version was a tall bordered card
 * with a "LAW 07" eyebrow, a status dot, a bookmark button, and a gradient fade standing in for
 * depth - roughly three times the height for the same information, which meant five laws per
 * screen instead of eight.
 *
 * The bookmark control moved onto the chevron's end of the row rather than disappearing: the
 * strategies list still needs a way to remove a law, and a long-press is not discoverable. It is
 * a smaller hit target than before, so it keeps a 44dp touch area via padding.
 */
@Composable
fun LawCardPreview(
    law: LawOverview,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp)
            .clip(RoundedCornerShape(Metrics.CornerRadius))
            .background(if (pressed) CourtGrayElevated else CourtGray)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LawNumberBadge(number = law.lawNumber, mastered = law.read)

        Spacer(Modifier.width(Spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = law.title,
                style = LawTheme.typography.lawTitle,
                color = ParchmentWhite,
                fontFamily = FontFamily.Serif,
                fontSize = 18.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = law.summary,
                style = LawTheme.typography.bodySecondary,
                color = ShadowGray,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                // Two lines, not one. Several law titles wrap to a second line, and a single-line
                // summary under a two-line title left the shorter titles looking half-empty.
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(Spacing.sm))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ActionIconButton(
                icon = if (law.bookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (law.bookmarked) {
                    "Remove ${law.title} from My Strategies"
                } else {
                    "Add ${law.title} to My Strategies"
                },
                active = law.bookmarked,
                onClick = onToggleBookmark,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = ShadowGray,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * The crimson disc carrying the law number.
 *
 * Crimson for a law still to be read, green once mastered - the same signal the old status dot
 * gave, so mastery is still legible at a glance, just without a second row of chrome.
 */
@Composable
private fun LawNumberBadge(number: Int, mastered: Boolean) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (mastered) ObservanceGreen else SovereignGold),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            color = ParchmentWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}