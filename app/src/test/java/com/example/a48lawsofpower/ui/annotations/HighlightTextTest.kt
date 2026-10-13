package com.example.a48lawsofpower.ui.annotations

import com.example.a48lawsofpower.data.HighlightEntity
import com.example.a48lawsofpower.data.ReadingDayEntity
import com.example.a48lawsofpower.data.computeStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Epoch-day anchors used throughout, kept as literals so the tests read as calendar dates. */
private const val D0 = 20_000L

private fun highlight(
    start: Int,
    end: Int,
    color: String = HighlightColor.Gold.id,
    note: String = "",
    createdAt: Long = 0,
) = HighlightEntity(
    id = createdAt + 1,
    lawId = 1,
    section = "core",
    start = start,
    end = end,
    color = color,
    note = note,
    createdAt = createdAt,
)

class HighlightTextTest {

    @Test
    fun `no highlights leaves text untouched`() {
        val resolved = resolveHighlights("the law asserts", emptyList())
        assertTrue(resolved.isEmpty())
        assertEquals("the law asserts", buildHighlightedText("the law asserts", resolved).text)
    }

    @Test
    fun `a highlight paints exactly its own range`() {
        val text = "Brilliance in the presence of a superior is not a gift."
        val resolved = resolveHighlights(text, listOf(highlight(0, 10)))
        assertEquals(1, resolved.size)
        assertEquals(0, resolved[0].start)
        assertEquals(10, resolved[0].end)
        assertTrue(!resolved[0].truncated)
    }

    @Test
    fun `overlapping highlights never share a character`() {
        val text = "0123456789"
        val resolved = resolveHighlights(text, listOf(highlight(0, 6), highlight(4, 10)))

        assertEquals(2, resolved.size)
        assertEquals(0, resolved[0].start)
        assertEquals(6, resolved[0].end)
        // The second is pushed back to where the first ended, so no glyph gets two fills.
        assertEquals(6, resolved[1].start)
        assertEquals(10, resolved[1].end)
        assertTrue(resolved[1].truncated)
    }

    @Test
    fun `a fully contained highlight wins regardless of save order`() {
        val text = "0123456789"
        val short = highlight(2, 4, createdAt = 1)
        val long = highlight(0, 8, createdAt = 2)
        val resolved = resolveHighlights(text, listOf(short, long))

        assertEquals(1, resolved.size)
        assertEquals(0, resolved[0].start)
        assertEquals(8, resolved[0].end)
    }

    @Test
    fun `a wholly contained highlight is dropped rather than rendered as a sliver`() {
        val text = "0123456789"
        val outer = highlight(0, 10)
        val inner = highlight(3, 5)
        val resolved = resolveHighlights(text, listOf(outer, inner))

        assertEquals(1, resolved.size)
        assertEquals(0, resolved[0].start)
        assertEquals(10, resolved[0].end)
    }

    @Test
    fun `offsets past the end of a shortened text are clamped, not fatal`() {
        val shortened = "0123"
        val stale = highlight(0, 999)
        val resolved = resolveHighlights(shortened, listOf(stale))

        assertEquals(1, resolved.size)
        assertEquals(0, resolved[0].start)
        assertEquals(4, resolved[0].end)
    }

    @Test
    fun `a highlight with no room left after clamping disappears`() {
        assertTrue(resolveHighlights("0123", listOf(highlight(99, 120))).isEmpty())
    }

    @Test
    fun `empty text yields nothing even when highlights exist`() {
        assertTrue(resolveHighlights("", listOf(highlight(0, 5))).isEmpty())
    }

    @Test
    fun `note and no note both render, and only the noted one is underlined`() {
        val text = "0123456789"
        val plain = resolveHighlights(text, listOf(highlight(0, 5)))
        val noted = resolveHighlights(text, listOf(highlight(0, 5, note = "why?")))

        val plainSpans = buildHighlightedText(text, plain).spanStyles
        val notedSpans = buildHighlightedText(text, noted).spanStyles

        assertEquals(1, plainSpans.size)
        assertEquals(1, notedSpans.size)
        assertEquals(null, plainSpans[0].item.textDecoration)
        assertEquals(
            androidx.compose.ui.text.style.TextDecoration.Underline,
            notedSpans[0].item.textDecoration,
        )
    }

    @Test
    fun `an unknown colour id degrades instead of throwing`() {
        // A highlight saved before a palette change must not take the reader down.
        val resolved = resolveHighlights("0123", listOf(highlight(0, 4, color = "chartreuse")))
        assertEquals(1, resolved.size)
        assertEquals(HighlightColor.Gold, HighlightColor.fromId("chartreuse"))
    }

    @Test
    fun `quote extraction returns the highlighted characters`() {
        assertEquals("gift", highlightedQuote("a gift to the court", highlight(2, 6)))
    }
}

class ReadingStatsTest {

    @Test
    fun `consecutive days form a streak`() {
        val stats = computeStats(listOf(D0 - 2, D0 - 1, D0), D0)
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.longestStreak)
        assertEquals(3, stats.totalDays)
    }

    @Test
    fun `a gap breaks the current streak but not the longest`() {
        val stats = computeStats(listOf(D0 - 6, D0 - 5, D0, D0 - 1), D0)
        assertEquals(2, stats.currentStreak)
        assertEquals(2, stats.longestStreak)
    }

    @Test
    fun `a streak ending yesterday still counts today`() {
        // The important one: waking up before opening the app must not read as a broken streak.
        val stats = computeStats(listOf(D0 - 3, D0 - 2, D0 - 1), D0)
        assertEquals(3, stats.currentStreak)
    }

    @Test
    fun `a streak that ended before yesterday is broken`() {
        val stats = computeStats(listOf(D0 - 5, D0 - 4, D0 - 3), D0)
        assertEquals(0, stats.currentStreak)
        assertEquals(3, stats.longestStreak)
    }

    @Test
    fun `no activity at all yields zeroes rather than a null`() {
        val stats = computeStats(emptyList(), D0)
        assertEquals(0, stats.currentStreak)
        assertEquals(0, stats.longestStreak)
        assertEquals(0, stats.totalDays)
    }

    @Test
    fun `today's counters are read from the day maps`() {
        val stats = computeStats(
            days = listOf(D0),
            today = D0,
            lawsByDay = mapOf(D0 to 3),
            secondsByDay = mapOf(D0 to 125),
        )
        assertEquals(3, stats.lawsReadToday)
        assertEquals(125, stats.secondsToday)
        assertEquals(2, stats.totalMinutes)
    }

    @Test
    fun `unsorted input still produces the right answer`() {
        val stats = computeStats(listOf(D0, D0 - 2, D0 - 1, D0 - 5), D0)
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.longestStreak)
    }
}