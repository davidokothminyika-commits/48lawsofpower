package com.example.a48lawsofpower.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.a48lawsofpower.ui.theme.HairlineBorder
import com.example.a48lawsofpower.ui.theme.LawTheme
import com.example.a48lawsofpower.ui.theme.Metrics
import com.example.a48lawsofpower.ui.theme.ObservanceGreen
import com.example.a48lawsofpower.ui.theme.ParchmentWhite
import com.example.a48lawsofpower.ui.theme.ShadowGray
import com.example.a48lawsofpower.ui.theme.SovereignGold
import com.example.a48lawsofpower.ui.theme.Spacing
import com.example.a48lawsofpower.ui.theme.TransgressionRed

/** Which side of the law a passage sits on. */
enum class Stance { Observance, Transgression, Neutral }

/**
 * A historical breakdown or analysis module. The left border tag colour-codes which side of the
 * law the passage sits on, so a reader can scan a page of history without reading it.
 *
 * The tag is painted with `drawBehind` rather than a sibling `Box`, so it stretches to the height
 * of the text without an `IntrinsicSize` measurement pass on every frame.
 */
@Composable
fun AnalysisBlock(
    eyebrow: String,
    body: String,
    stance: Stance,
    modifier: Modifier = Modifier,
) {
    val accent = when (stance) {
        Stance.Observance -> ObservanceGreen
        Stance.Transgression -> TransgressionRed
        Stance.Neutral -> SovereignGold
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(color = accent, size = Size(4.dp.toPx(), size.height))
            }
            .padding(start = Spacing.md),
    ) {
        Text(
            text = eyebrow.uppercase(),
            style = LawTheme.typography.eyebrow,
            color = accent,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = body,
            style = LawTheme.typography.bodyPrimary,
            color = ParchmentWhite,
        )
    }
}

/** A quiet metadata line used for categories and dates. */
@Composable
fun MetadataRow(text: String, modifier: Modifier = Modifier, color: Color = ShadowGray) {
    Text(
        text = text.uppercase(),
        style = LawTheme.typography.eyebrow,
        color = color,
        modifier = modifier,
    )
}

/** Hairline rule between modules. */
@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = HairlineBorder) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Metrics.HairlineWidth)
            .background(color),
    )
}
