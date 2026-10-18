package com.example.a48lawsofpower.ui.annotations

import com.example.a48lawsofpower.data.AnnotationType
import com.example.a48lawsofpower.data.AnnotationWithLaw
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun mark(
    lawNumber: Int = 1,
    lawTitle: String = "Never Outshine the Master",
    section: String = "core",
    anchor: String = "a passage worth keeping",
    note: String = "",
    color: String = HighlightColor.Gold.id,
    type: String = AnnotationType.Highlight.wire,
    createdAt: Long = 0,
) = AnnotationWithLaw(
    id = createdAt + 1,
    lawId = lawNumber,
    section = section,
    start = 0,
    end = anchor.length,
    anchorText = anchor,
    type = type,
    color = color,
    note = note,
    createdAt = createdAt,
    updatedAt = createdAt,
    lawNumber = lawNumber,
    lawTitle = lawTitle,
)

class QuoteExportTest {

    @Test
    fun `an empty export says so rather than producing a blank file`() {
        val out = buildExport(emptyList(), stamp = "1 January 2026")
        assertTrue(out.contains("No marks yet"))
    }

    @Test
    fun `the header states what the file is and where it came from`() {
        val out = buildExport(emptyList(), stamp = "1 January 2026")
        assertTrue(out.contains("48 Laws of Power"))
        assertTrue(out.contains("1 January 2026"))
        // Attribution travels with the export, so a file found later still says what it is.
        assertTrue(out.contains("Robert Greene"))
    }

    @Test
    fun `a highlight becomes a blockquote carrying its passage`() {
        val out = buildExport(listOf(mark(anchor = "superiority is a kind of danger")), stamp = "d")
        assertTrue(out.contains("> superiority is a kind of danger"))
        assertTrue(out.contains("Highlighted"))
    }

    @Test
    fun `a note is attached beneath its passage`() {
        val out = buildExport(listOf(mark(note = "check against Ptolemy")), stamp = "d")
        assertTrue(out.contains("**Note.** check against Ptolemy"))
    }

    @Test
    fun `a bookmark exports as a checklist item, not a quote`() {
        val out = buildExport(
            listOf(mark(type = AnnotationType.Bookmark.wire, color = "", anchor = "")),
            stamp = "d",
        )
        assertTrue(out.contains("- [ ] Bookmarked"))
        assertTrue("a bookmark has no passage to quote", !out.contains("> "))
    }

    @Test
    fun `multi-line anchors do not break the blockquote`() {
        val out = buildExport(listOf(mark(anchor = "line one\nline two")), stamp = "d")
        assertTrue("newlines must be collapsed", out.contains("> line one line two"))
        // The whole passage has to land on the one quote line; a second line lacking the marker
        // would render as a stray continuation rather than part of the blockquote.
        assertEquals(
            "quote must occupy exactly one line",
            1,
            out.lines().count { it.startsWith("> line one") },
        )
    }

    @Test
    fun `marks are grouped by law in ascending order`() {
        val out = buildExport(
            listOf(
                mark(lawNumber = 7, lawTitle = "Court Attention", createdAt = 1),
                mark(lawNumber = 2, lawTitle = "Conceal Your Intentions", createdAt = 2),
            ),
            stamp = "d",
        )
        val law2 = out.indexOf("## Law 02")
        val law7 = out.indexOf("## Law 07")
        assertTrue("law 02 should come before law 07", law2 in 0 until law7)
    }

    @Test
    fun `sections within a law keep reading order not creation order`() {
        // Marked in a strange order, but the reversal is the last thing read.
        val out = buildExport(
            listOf(
                mark(section = "reversal", anchor = "the closing thought", createdAt = 1),
                mark(section = "core", anchor = "the opening thought", createdAt = 2),
            ),
            stamp = "d",
        )
        assertTrue(
            "core should precede reversal inside one law",
            out.indexOf("the opening thought") < out.indexOf("the closing thought"),
        )
    }

    @Test
    fun `the colour label is included so intent survives the export`() {
        val out = buildExport(listOf(mark(color = HighlightColor.Crimson.id)), stamp = "d")
        assertTrue(out.contains(HighlightColor.Crimson.label))
    }

    @Test
    fun `an unknown type falls back to being treated as a highlight`() {
        // A mark written by a newer version must not crash an older export.
        val out = buildExport(listOf(mark(type = "annotation-doodle")), stamp = "d")
        assertTrue(out.contains("> a passage worth keeping"))
    }

    @Test
    fun `a partial export says it is partial`() {
        // The dangerous case is a filtered export that reads as though it were everything.
        val out = buildExport(
            listOf(mark(color = HighlightColor.Crimson.id)),
            stamp = "d",
            scopeNote = "showing Crimson highlights only",
        )
        assertTrue(out.contains("Partial export"))
        assertTrue(out.contains("Crimson highlights only"))
    }

    @Test
    fun `a full export does not carry a partial caveat`() {
        val out = buildExport(listOf(mark()), stamp = "d", scopeNote = null)
        assertTrue(!out.contains("Partial export"))
    }
}