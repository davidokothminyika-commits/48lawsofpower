package com.example.a48lawsofpower.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
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
 * A law as it appears in a feed: a formal, sharp-cornered card that hints at hidden depth instead
 * of trying to cram the whole law onto it.
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
    val shape = RoundedCornerShape(Metrics.CornerRadius)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Metrics.LawCardMinHeight)
            .clip(shape)
            .background(if (pressed) CourtGrayElevated else CourtGray)
            .border(
                width = Metrics.HairlineWidth,
                color = if (pressed) SovereignGold else HairlineBorder,
                shape = shape,
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onLongClick = onLongClick,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.md, top = Spacing.sm, end = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = Spacing.sm),
            ) {
                StatusDot(mastered = law.read)
                Text(
                    text = "LAW ${law.lawNumber.toString().padStart(2, '0')}",
                    style = LawTheme.typography.lawIndex,
                    color = ShadowGray,
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }

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
        }

        Text(
            text = law.title,
            style = LawTheme.typography.lawTitle,
            color = ParchmentWhite,
            modifier = Modifier.padding(horizontal = Spacing.md),
        )

        Spacer(Modifier.height(Spacing.sm))

        Text(
            text = law.summary,
            style = LawTheme.typography.bodySecondary,
            color = ShadowGray,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
        )

        // Gradient line-fade signals hidden depth instead of hard-clipping the text.
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(Spacing.lg)
                .background(
                    Brush.verticalGradient(
                        listOf(CourtGray.copy(alpha = 0f), CourtGray),
                    ),
                ),
        )
    }
}

/** Gold marks a law still to be mastered, green marks one already observed. */
@Composable
private fun StatusDot(mastered: Boolean) {
    Box(
        modifier = Modifier
            .width(Spacing.sm)
            .height(Spacing.sm)
            .clip(RoundedCornerShape(Spacing.xs))
            .background(if (mastered) ObservanceGreen else SovereignGold),
    )
}
