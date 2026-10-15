package com.example.a48lawsofpower.ui.navigation

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.a48lawsofpower.ui.RootDestination
import com.example.a48lawsofpower.ui.components.Hairline
import com.example.a48lawsofpower.ui.theme.LawTheme
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.SovereignGold
import com.example.a48lawsofpower.ui.theme.Spacing

private data class TabSpec(
    val destination: RootDestination,
    val label: String,
    val icon: ImageVector,
)

private val tabs = listOf(
    TabSpec(RootDestination.Dossier, "Dossier", Icons.AutoMirrored.Outlined.MenuBook),
    TabSpec(RootDestination.Library, "Library", Icons.AutoMirrored.Outlined.LibraryBooks),
    TabSpec(RootDestination.Codex, "Codex", Icons.Outlined.Today),
    TabSpec(RootDestination.Profile, "Profile", Icons.Outlined.PersonOutline),
)

/**
 * The four-tab bottom bar, grounded in the thumb zone. Thin-line glyphs only.
 */
@Composable
fun CodexBottomBar(
    current: RootDestination,
    onSelect: (RootDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Hairline(color = SovereignGold.copy(alpha = 0.2f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                TabItem(
                    tab = tab,
                    selected = tab.destination == current,
                    onClick = { onSelect(tab.destination) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: TabSpec,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (selected) SovereignGold else ShadowGray
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Spacing.sm))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = tab.label,
            style = LawTheme.typography.actionLabel,
            color = tint,
            maxLines = 1,
        )
        Spacer(Modifier.height(Spacing.xs))
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(if (selected) Spacing.md else 0.dp)
                .clip(RoundedCornerShape(Spacing.xs))
                .background(if (selected) SovereignGold else Color.Transparent),
        )
    }
}
