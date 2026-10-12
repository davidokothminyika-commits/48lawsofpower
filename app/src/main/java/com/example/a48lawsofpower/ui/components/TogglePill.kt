package com.example.a48lawsofpower.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.a48lawsofpower.ui.theme.CourtGray
import com.example.a48lawsofpower.ui.theme.HairlineBorder
import com.example.a48lawsofpower.ui.theme.LawTheme
import com.example.a48lawsofpower.ui.theme.Metrics
import com.example.a48lawsofpower.ui.theme.ObsidianBlack
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.SovereignGold
import com.example.a48lawsofpower.ui.theme.Spacing

/**
 * The one soft shape in the system: strictly for category filters. 38dp tall so it clears the
 * minimum comfortable tap region.
 */
@Composable
fun TogglePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Metrics.PillRadius)
    Box(
        modifier = modifier
            .heightIn(Metrics.PillHeight)
            .clip(shape)
            .background(if (selected) SovereignGold else CourtGray)
            .border(
                width = Metrics.HairlineWidth,
                color = if (selected) SovereignGold else HairlineBorder,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = LawTheme.typography.actionLabel,
            color = if (selected) ObsidianBlack else ShadowGray,
            maxLines = 1,
        )
    }
}
