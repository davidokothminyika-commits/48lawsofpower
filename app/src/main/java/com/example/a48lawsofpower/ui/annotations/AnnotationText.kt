package com.example.a48lawsofpower.ui.annotations

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.example.a48lawsofpower.data.AnnotationEntity
import com.example.a48lawsofpower.data.AnnotationType
import com.example.a48lawsofpower.data.ResolvedAnnotation
import com.example.a48lawsofpower.data.resolve

/**
 * One paintable mark, as a span range.
 *
 * @property truncated true when a previous mark already occupied the start of this one and the
 *   range was pushed back. Surfaced rather than silently absorbed, so a caller can offer to
 *   re-home the hidden part instead of pretending it is visible.
 */
data class PaintableSpan(
    val annotation: AnnotationEntity,
    val start: Int,
    val end: Int,
    val truncated: Boolean,
)

/**
 * Reconciles stored marks against the text they point into and decides which can be painted.
 *
 * Two passes, in this order and for two different reasons:
 *
 *  1. [AnnotationEntity.resolve] clamps and re-finds each mark. That is the reflow story - it is
 *     why an anchor survives a re-encode - and it runs per mark.
 *  2. Overlaps are settled here. Two translucent backgrounds composited on the same glyphs
 *     produce a colour that is not in the palette, so exactly one mark owns any character.
 *     Containment is settled longest-first, so a long selection always beats a short one swallowed
 *     by it no matter which was saved first.
 *
 * Bookmarks are not paintable: they carry no range and would paint a zero-width artefact.
 */
fun resolvePaintableSpans(
    text: String,
    annotations: List<AnnotationEntity>,
): List<PaintableSpan> {
    if (text.isEmpty()) return emptyList()

    val paintable = annotations
        .filter { it.type != AnnotationType.Bookmark.wire }
        .map { it.resolve(text) }
        .filter { it.isPaintable }
        .sortedWith(
            // Longest first so a container beats its contents; type name breaks ties so the
            // result does not depend on insertion order.
            compareByDescending<ResolvedAnnotation> { it.end - it.start }
                .thenBy { it.annotation.type }
                .thenBy { it.annotation.id },
        )

    val resolved = mutableListOf<PaintableSpan>()
    var cursor = 0
    for (r in paintable) {
        val start = maxOf(r.start, cursor)
        val end = maxOf(r.end, start)
        if (end > start) {
            resolved += PaintableSpan(
                annotation = r.annotation,
                start = start,
                end = end,
                truncated = start != r.start,
            )
            cursor = end
        }
    }
    return resolved.sortedBy { it.start }
}

/**
 * Paints [spans] onto [text].
 *
 * A mark carrying a note also gets an underline. That is the same signal the guidelines ask for
 * on annotated entries in a contents list: visible in the prose without opening the review screen
 * to remember which passages were annotated.
 *
 * Highlight colour is ignored for note-type marks, which reuse the default fill so a note is
 * identifiable by shape and underline rather than by a second colour meaning.
 */
fun buildAnnotatedReaderText(
    text: String,
    spans: List<PaintableSpan>,
): AnnotatedString = buildAnnotatedString {
    append(text)
    spans.forEach { span ->
        val isHighlight = span.annotation.type == AnnotationType.Highlight.wire
        val fill = if (isHighlight) colorFor(span.annotation.color) else noteFill
        val style = if (span.annotation.note.isBlank()) {
            SpanStyle(background = fill)
        } else {
            SpanStyle(background = fill, textDecoration = TextDecoration.Underline)
        }
        addStyle(style, span.start, span.end)
    }
}

/** Notes get a fixed fill so they never read as a coloured highlight. */
private val noteFill = androidx.compose.ui.graphics.Color(0xFF8E8E93).copy(alpha = 0.26f)

/** The exact characters a mark covers, for the review screen and for export. */
fun quotedText(text: String, annotation: AnnotationEntity): String =
    annotation.resolve(text).quoted