package com.example.a48lawsofpower.ui.annotations

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import com.example.a48lawsofpower.data.HighlightEntity
import com.example.a48lawsofpower.data.clampedTo

/**
 * One highlight after overlap resolution, expressed against [text].
 *
 * @param truncated true when the range was pushed back because an earlier highlight already
 *   occupied those characters. Kept so the caller can offer to drop or re-home the hidden part
 *   rather than silently pretending it is visible.
 */
data class ResolvedHighlight(
    val highlight: HighlightEntity,
    val start: Int,
    val end: Int,
    val truncated: Boolean,
)

/**
 * Reconciles saved highlights against the text they point into.
 *
 * Offsets are recorded against the text as it was when the reader made them, so a content edit
 * can leave a range that overruns or falls entirely outside the block. Everything here is
 * clamping, never throwing: a stale highlight degrades to a shorter one and a wholly stale one
 * disappears.
 *
 * Overlaps are resolved rather than allowed to stack. Two translucent backgrounds composited on
 * the same glyphs produce a third colour that is not in the palette, so the reader picks one and
 * the loser is reported as [truncated]. Containment is settled longest-first, so a long selection
 * always wins over a short one swallowed by it, regardless of which was saved first.
 */
fun resolveHighlights(
    text: String,
    highlights: List<HighlightEntity>,
): List<ResolvedHighlight> {
    if (text.isEmpty()) return emptyList()

    val usable = highlights.mapNotNull { h ->
        h.clampedTo(text)?.let { it to false } ?: (h to true)
    }.filter { (_, stale) -> !stale }

    val ordered = usable.sortedWith(
        compareByDescending<Pair<HighlightEntity, Boolean>> { (_, stale) -> stale }
            .thenByDescending { it.first.end - it.first.start }
            .thenBy { it.first.start },
    )

    val resolved = mutableListOf<ResolvedHighlight>()
    var cursor = 0
    for ((highlight, _) in ordered) {
        val start = maxOf(highlight.start, cursor)
        val end = maxOf(highlight.end, start)
        if (end > start) {
            resolved += ResolvedHighlight(
                highlight = highlight,
                start = start,
                end = end,
                truncated = start != highlight.start,
            )
            cursor = end
        }
    }
    return resolved.sortedBy { it.start }
}

/**
 * Paints [resolved] onto [text].
 *
 * A highlight carrying a note also gets an underline, so a note is visible in the prose without
 * having to open the notes list to remember which passages were annotated.
 */
fun buildHighlightedText(
    text: String,
    resolved: List<ResolvedHighlight>,
): AnnotatedString = buildAnnotatedString {
    append(text)
    resolved.forEach { r ->
        val fill = colorFor(r.highlight.color)
        val span = if (r.highlight.note.isBlank()) {
            SpanStyle(background = fill)
        } else {
            SpanStyle(
                background = fill,
                textDecoration = TextDecoration.Underline,
                fontStyle = FontStyle.Normal,
            )
        }
        addStyle(span, r.start, r.end)
    }
}

/** The exact characters a highlight covers, for previews and quote export. */
fun highlightedQuote(text: String, highlight: HighlightEntity): String =
    text.substring(
        highlight.start.coerceIn(0, text.length),
        highlight.end.coerceIn(0, text.length),
    )