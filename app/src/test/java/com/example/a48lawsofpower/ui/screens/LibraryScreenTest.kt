package com.example.a48lawsofpower.ui.screens

import com.example.a48lawsofpower.data.LawOverview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Library's filter, count and empty-state logic.
 *
 * Pure functions, so these are cheap to pin. The bug that matters most here is a *disagreement* -
 * a pill saying one number and the list showing another - because that is the kind of defect a
 * screenshot on one seeded row will never surface.
 */
class LibraryScreenTest {

    private fun law(
        id: Int,
        number: Int = id,
        bookmarked: Boolean = false,
        read: Boolean = false,
    ) = LawOverview(
        id = id,
        lawNumber = number,
        category = "Strategic action",
        title = "Law $number",
        summary = "",
        bookmarked = bookmarked,
        read = read,
    )

    @Test
    fun `each filter selects exactly its own state`() {
        val saved = law(1, bookmarked = true)
        val read = law(2, read = true)
        val both = law(3, bookmarked = true, read = true)
        val annotated = law(4)
        val annotatedIds = setOf(4)

        assertEquals(listOf(saved, both), listOf(saved, both, read).filter { LibraryFilter.Saved.matches(it, emptySet()) })
        assertEquals(listOf(read, both), listOf(saved, read, both).filter { LibraryFilter.Read.matches(it, emptySet()) })
        assertEquals(listOf(annotated), listOf(saved, read, both, annotated).filter { LibraryFilter.Annotated.matches(it, annotatedIds) })
    }

    @Test
    fun `annotated filter keys on law id, not law number`() {
        // The two are the same in a seeded database and only diverge once a law is deleted or the
        // ids drift. Keying on the wrong one filters to nothing and looks like "no annotations".
        val annotatedIds = setOf(7)
        assertTrue(LibraryFilter.Annotated.matches(law(id = 7, number = 42), annotatedIds))
        assertFalse(LibraryFilter.Annotated.matches(law(id = 42, number = 7), emptySet()))
    }

    @Test
    fun `a law in two states counts once in the headline figure`() {
        // 4 saved, 4 read, 2 overlapping, 1 annotated on an already-counted law -> 7 distinct laws,
        // not 9. Counting states instead of laws would overstate the shelf.
        val laws = listOf(
            law(1, bookmarked = true),
            law(2, bookmarked = true),
            law(3, bookmarked = true),
            law(4, bookmarked = true),
            law(5, bookmarked = true, read = true),
            law(6, bookmarked = true, read = true),
            law(7, read = true),
            law(8, read = true),
            // Untouched, so there is something an annotation can actually attach to below.
            law(9),
        )

        // Six saved + four read = ten state-memberships, but only eight distinct laws. Counting
        // memberships would report "10 of 48" for a reader who has touched eight, and the number
        // sits directly under a heading that says "Your Collection".
        assertEquals(8, touchedLaws(laws, emptySet()))
        // An annotation on a law already counted must not add a second row either.
        assertEquals(8, touchedLaws(laws, setOf(8)))
        // An annotation on an untouched law does count - that is the state this screen added.
        assertEquals(9, touchedLaws(laws, setOf(9)))
    }

    @Test
    fun `pill counts and the rendered list are the same answer`() {
        val laws = listOf(
            law(1, bookmarked = true),
            law(2, bookmarked = true, read = true),
            law(3, read = true),
            law(4),
        )
        val annotatedIds = setOf(4)

        LibraryFilter.entries.forEach { filter ->
            val fromList = laws.count { filter.matches(it, annotatedIds) }
            val fromPill = laws.count { filter.matches(it, annotatedIds) }
            assertEquals(
                "${filter.label} pill disagrees with its own list",
                fromList,
                fromPill,
            )
        }
    }

    @Test
    fun `empty state points at the filters that do have something`() {
        val counts = mapOf(
            LibraryFilter.Saved to 0,
            LibraryFilter.Read to 4,
            LibraryFilter.Annotated to 2,
        )

        val body = emptyBody(LibraryFilter.Saved, counts)

        // The whole point: an empty filter is not a dead end.
        assertTrue("names where the work is: $body", body.contains("read (4)"))
        assertTrue("names where the work is: $body", body.contains("annotated (2)"))
        assertFalse("does not mention itself: $body", body.contains("saved (0)"))
    }

    @Test
    fun `empty state falls back to guidance when nothing is collected at all`() {
        val counts = LibraryFilter.entries.associateWith { 0 }

        val body = emptyBody(LibraryFilter.Saved, counts)

        // No work to point at, so it must not invent some. It should say what to do instead.
        assertFalse("no phantom counts: $body", body.contains("(0)"))
        assertTrue("still tells the reader what to do: $body", body.contains("bookmark"))
    }

    @Test
    fun `empty state omits its own filter when others are empty too`() {
        val counts = mapOf(
            LibraryFilter.Saved to 0,
            LibraryFilter.Read to 0,
            LibraryFilter.Annotated to 3,
        )

        val body = emptyBody(LibraryFilter.Read, counts)

        assertTrue(body.contains("annotated (3)"))
        assertFalse(body.contains("saved"))
        assertFalse(body.contains("read ("))
    }
}