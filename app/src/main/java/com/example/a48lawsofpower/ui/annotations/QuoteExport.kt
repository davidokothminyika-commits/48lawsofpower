package com.example.a48lawsofpower.ui.annotations

import com.example.a48lawsofpower.data.AnnotationFilter
import com.example.a48lawsofpower.data.AnnotationType
import com.example.a48lawsofpower.data.AnnotationWithLaw
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Renders the reader's marks as plain text they can paste anywhere.
 *
 * ## Why text, and why this shape
 *
 * Markdown is the lingua franca of note-taking: Obsidian, Notion, Bear, Logseq and most read-later
 * tools all import it, and everything degrades to readable prose if the target does not understand
 * it. The alternative - a bespoke JSON blob - is only readable by this app, which defeats the point
 * of an export.
 *
 * Blockquotes carry the passages so they survive being pasted somewhere that drops styling, and
 * the note sits under its own mark as a plain indented continuation so the pairing survives too.
 *
 * The document carries a header stating what it is and where it came from, because a file of
 * unattributed quotations is much less useful a year later when nobody remembers which app made it.
 */
fun buildExport(
    annotations: List<AnnotationWithLaw>,
    stamp: String = todayStamp(),
    scopeNote: String? = null,
): String = buildString {
    appendLine("# 48 Laws of Power — my notes")
    appendLine()
    appendLine("Exported $stamp from the 48 Laws of Power app.")
    appendLine("Commentary is original; the laws are the work of Robert Greene (1998).")
    // A partial export that does not say so is worse than no export: it looks like the whole
    // record, and the marks left out are gone as far as the reader is concerned.
    if (scopeNote != null) {
        appendLine()
        appendLine("_Partial export: $scopeNote._")
    }
    appendLine()

    if (annotations.isEmpty()) {
        appendLine("_No marks yet._")
        return@buildString
    }

    // Grouped by law, ascending, so the export reads as a reading journey rather than as an
    // undated pile. Within a law, the reading order of the sections is preserved rather than
    // sorted by id, because two marks in the same paragraph should not appear to be reordered.
    val byLaw = annotations
        .groupBy { it.lawNumber }
        .toSortedMap()
        .toList()

    for ((lawNumber, marks) in byLaw) {
        val lawTitle = marks.first().lawTitle
        appendLine("## Law ${lawNumber.toString().padStart(2, '0')} — $lawTitle")
        appendLine()

        for (mark in marks.sortedBy { SECTION_ORDER.indexOf(it.section).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE }) {
            when (AnnotationType.fromWire(mark.type)) {
                AnnotationType.Bookmark -> {
                    appendLine("- [ ] Bookmarked")
                    if (mark.note.isNotBlank()) {
                        appendLine("  - ${mark.note}")
                    }
                }

                AnnotationType.Note -> {
                    appendLine("> ${quoteFor(mark.anchorText)}")
                    if (mark.note.isNotBlank()) {
                        appendLine()
                        appendLine("**Note.** ${mark.note}")
                    }
                }

                null, AnnotationType.Highlight -> {
                    val tag = HighlightColor.fromId(mark.color).label
                    appendLine("> ${quoteFor(mark.anchorText)}")
                    appendLine()
                    appendLine("*Highlighted — $tag.*")
                    if (mark.note.isNotBlank()) {
                        appendLine()
                        appendLine("**Note.** ${mark.note}")
                    }
                }
            }
            appendLine()
        }
    }
}

/**
 * Rendering order for sections within a law.
 *
 * Arbitrary but fixed. Without it, grouping by law would leave the marks ordered by creation time
 * across the whole law, which reorders passages the reader marked in one sitting.
 */
private val SECTION_ORDER = listOf("takeaways", "core", "summary", "observance", "transgression", "reversal")

/** Collapses newlines so a multi-line anchor does not break the blockquote. */
private fun quoteFor(text: String): String =
    text.trim().replace(Regex("\\s+"), " ")

private fun todayStamp(zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofPattern("d MMMM yyyy")
        .withZone(zone)
        .format(Instant.now())
/**
 * Describes what the on-screen filter is hiding, for the export header.
 *
 * `null` when nothing is filtered, so a full export carries no caveat.
 */
fun AnnotationFilter.scopeNote(): String? {
    val parts = buildList {
        if (type != AnnotationType.MIXED) {
            add(
                when (type) {
                    AnnotationType.Highlight.wire -> "highlights only"
                    AnnotationType.Note.wire -> "notes only"
                    AnnotationType.Bookmark.wire -> "bookmarks only"
                    else -> "$type only"
                },
            )
        }
        if (color.isNotBlank()) add("${HighlightColor.fromId(color).label.lowercase()} highlights only")
    }
    return if (parts.isEmpty()) null else "showing ${parts.joinToString(" and ")}"
}
