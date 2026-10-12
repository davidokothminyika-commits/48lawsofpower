package com.example.a48lawsofpower.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.a48lawsofpower.ui.theme.LawTheme
import com.example.a48lawsofpower.ui.theme.Metrics
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.SovereignGold

/**
 * A 44dp touch target - the absolute minimum - with thin-line glyphs only. When the thing it
 * controls is engaged, the tint moves to Sovereign Gold.
 */
@Composable
fun ActionIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(Metrics.IconButtonSize),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) SovereignGold else ShadowGray,
        )
    }
}
