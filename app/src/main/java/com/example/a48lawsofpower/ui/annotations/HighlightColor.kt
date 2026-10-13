package com.example.a48lawsofpower.ui.annotations

import androidx.compose.ui.graphics.Color
import com.example.a48lawsofpower.ui.theme.ObservanceGreen
import com.example.a48lawsofpower.ui.theme.SovereignGold
import com.example.a48lawsofpower.ui.theme.TransgressionRed

/**
 * The highlight colours offered in the reader.
 *
 * Stored by [id] as a string, never as a packed ARGB value, so a re-tuned palette does not
 * invalidate anyone's saved highlights. Removing an id here orphans that colour and the renderer
 * falls back to [DEFAULT] rather than failing - see [colorFor].
 */
enum class HighlightColor(val id: String, val label: String) {
    Gold("gold", "Key point"),
    Crimson("crimson", "Danger"),
    Verdant("verdant", "Insight"),
    Azure("azure", "Reference"),
    Violet("violet", "Question"),
    ;

    companion object {
        val DEFAULT = Gold

        fun fromId(id: String?): HighlightColor =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/**
 * Fill used behind highlighted text.
 *
 * Deliberately translucent. The reader's surface is near-black and its body text is
 * [com.example.a48lawsofpower.ui.theme.ParchmentWhite]; an opaque fill would fight the text for
 * contrast. These sit at roughly 30-38% so the glyphs stay clearly legible on top, which is the
 * whole point of a highlight.
 */
val HighlightFills: Map<HighlightColor, Color> = mapOf(
    HighlightColor.Gold to SovereignGold.copy(alpha = 0.34f),
    HighlightColor.Crimson to TransgressionRed.copy(alpha = 0.32f),
    HighlightColor.Verdant to ObservanceGreen.copy(alpha = 0.30f),
    HighlightColor.Azure to Color(0xFF3B82F6).copy(alpha = 0.32f),
    HighlightColor.Violet to Color(0xFF8B5CF6).copy(alpha = 0.34f),
)

/** Opaque version, for the swatch chips and for the exported note marker. */
val HighlightStrokes: Map<HighlightColor, Color> = mapOf(
    HighlightColor.Gold to SovereignGold,
    HighlightColor.Crimson to TransgressionRed,
    HighlightColor.Verdant to ObservanceGreen,
    HighlightColor.Azure to Color(0xFF60A5FA),
    HighlightColor.Violet to Color(0xFFA78BFA),
)

/** Resolves a stored colour id, degrading unknown ids instead of throwing. */
fun colorFor(id: String?): Color =
    HighlightFills[HighlightColor.fromId(id)] ?: SovereignGold.copy(alpha = 0.34f)