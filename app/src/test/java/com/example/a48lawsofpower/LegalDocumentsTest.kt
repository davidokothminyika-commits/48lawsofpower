package com.example.a48lawsofpower

import com.example.a48lawsofpower.legal.LegalDocument
import com.example.a48lawsofpower.legal.allLegalDocuments
import com.example.a48lawsofpower.legal.hasPlaceholderText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Invariants over the shipped legal text.
 *
 * The red banner is the only thing standing between a half-edited draft and the store, so these
 * tests hold the banner to its own rule: a document with an unresolved note to the author must be
 * flagged, and the two flags have to agree. A note reintroduced in a later edit then fails the
 * build instead of shipping.
 */
class LegalDocumentsTest {

    private fun bodyOf(document: LegalDocument): String =
        document.sections.joinToString("\n") { section ->
            (section.paragraphs + section.bullets).joinToString("\n")
        }

    private fun carriesANote(document: LegalDocument): Boolean =
        DraftingNote.containsMatchIn(bodyOf(document))

    @Test
    fun `no document carries an unresolved note to the author`() {
        val offenders = allLegalDocuments.filter { carriesANote(it) }
        assertEquals(
            "Drafting notes left in shipped text: ${offenders.map { it.kind }}",
            emptyList<LegalDocument>(),
            offenders,
        )
    }

    @Test
    fun `the placeholder flag and the drafting notes always agree`() {
        val flagged = allLegalDocuments.filter { it.isPlaceholder }
        val withNotes = allLegalDocuments.filter { carriesANote(it) }

        // Notes present while the banner is down is how placeholder text ships. The banner up with
        // no notes left is how a finished document gets ignored.
        assertEquals(
            "isPlaceholder and the presence of drafting notes disagree",
            withNotes,
            flagged,
        )
        assertEquals(
            "hasPlaceholderText disagrees with the documents themselves",
            withNotes.isNotEmpty(),
            hasPlaceholderText,
        )
        assertTrue(
            "Banner is up somewhere - the text it warns about is still a draft",
            hasPlaceholderText || flagged.isEmpty(),
        )
    }

    @Test
    fun `every document is dated`() {
        allLegalDocuments.forEach { document ->
            assertFalse(
                "${document.kind} has no date",
                document.lastUpdated.isBlank() || document.lastUpdated == "Not yet dated",
            )
        }
    }

    private companion object {
        /** An open bracket, prose, a close bracket - the shape every author note has had so far. */
        val DraftingNote = Regex("""\[[A-Z][^\]]{15,}\]""")
    }
}
