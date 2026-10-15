package com.example.a48lawsofpower.ui.annotations

import androidx.compose.ui.graphics.Color

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
 * Deliberately translucent. An opaque fill would fight the glyphs for contrast, whatever family sits
 * underneath. These sit at roughly 30-38% so the text stays clearly legible on top, which is the
 * whole point of a highlight.
 *
 * These are **fixed** colours, not theme tokens, and that is deliberate. A highlight is a marker the
 * reader chose and named - "Key point" and "Danger" are different tools and must never render the
 * same. Binding them to the app accent did exactly that: the retheme from gold to crimson left both
 * entries painting crimson, so the distinction survived only in the label. Tying them to the colour
 * scheme would also make a saved mark depend on the active family, and a reader who highlights in
 * dark and reads in light would find their own marks changed underneath them.
 *
 * They are mid-tone so the opaque stroke version stays legible on both the ink and paper families.
 */
val HighlightFills: Map<HighlightColor, Color> = mapOf(
    HighlightColor.Gold to Color(0xFFC79A16).copy(alpha = 0.34f),
    HighlightColor.Crimson to Color(0xFFC0362C).copy(alpha = 0.32f),
    HighlightColor.Verdant to Color(0xFF2E8B57).copy(alpha = 0.30f),
    HighlightColor.Azure to Color(0xFF2E7BD6).copy(alpha = 0.32f),
    HighlightColor.Violet to Color(0xFF8250DF).copy(alpha = 0.34f),
)

/** Opaque version, for the swatch chips and for the exported note marker. */
val HighlightStrokes: Map<HighlightColor, Color> = mapOf(
    HighlightColor.Gold to Color(0xFFC79A16),
    HighlightColor.Crimson to Color(0xFFC0362C),
    HighlightColor.Verdant to Color(0xFF2E8B57),
    HighlightColor.Azure to Color(0xFF2E7BD6),
    HighlightColor.Violet to Color(0xFF8250DF),
)

/** Resolves a stored colour id, degrading unknown ids instead of throwing. */
fun colorFor(id: String?): Color =
    HighlightFills[HighlightColor.fromId(id)] ?: HighlightFills.getValue(HighlightColor.DEFAULT)