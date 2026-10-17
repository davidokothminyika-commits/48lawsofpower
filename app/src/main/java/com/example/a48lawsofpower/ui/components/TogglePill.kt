package com.example.a48lawsofpower.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.a48lawsofpower.ui.theme.CourtGray
import com.example.a48lawsofpower.ui.theme.HairlineBorder
import com.example.a48lawsofpower.ui.theme.Inter
import com.example.a48lawsofpower.ui.theme.LawTheme
import com.example.a48lawsofpower.ui.theme.Metrics
import com.example.a48lawsofpower.ui.theme.ObsidianBlack
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.SovereignGold
import com.example.a48lawsofpower.ui.theme.Spacing

/**
 * The one soft shape in the system: strictly for category filters. 38dp tall so it clears the
 * minimum comfortable tap region.
 *
 * [count] is optional and renders as a separate, dimmer figure rather than being concatenated into
 * the label. A filter that can return nothing needs to say so *before* it is tapped - "Saved" and
 * "Saved 0" invite very different actions, and only one of them is worth taking.
 */
@Composable
fun TogglePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    val shape = RoundedCornerShape(Metrics.PillRadius)
    val onPill = if (selected) ObsidianBlack else ShadowGray
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = LawTheme.typography.actionLabel,
                color = onPill,
                maxLines = 1,
            )
            if (count != null) {
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = count.toString(),
                    style = LawTheme.typography.actionLabel.copy(fontFamily = Inter),
                    // 0.7 rather than something fainter: on the *selected* pill this is dark text on
                    // crimson, the one place the count is hardest to read - and the count is the
                    // reason to look at the pill at all.
                    color = onPill.copy(alpha = 0.7f),
                    maxLines = 1,
                )
            }
        }
    }
}
