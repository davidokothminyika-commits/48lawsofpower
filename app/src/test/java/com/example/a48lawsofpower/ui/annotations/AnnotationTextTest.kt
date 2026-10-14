package com.example.a48lawsofpower.ui.annotations

import com.example.a48lawsofpower.data.AnchorFit
import com.example.a48lawsofpower.data.AnnotationEntity
import com.example.a48lawsofpower.data.AnnotationType
import com.example.a48lawsofpower.data.ReadingDayEntity
import com.example.a48lawsofpower.data.computeStats
import com.example.a48lawsofpower.data.resolve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val D0 = 20_000L

private fun mark(
    start: Int,
    end: Int,
    anchor: String = "",
    type: String = AnnotationType.Highlight.wire,
    color: String = HighlightColor.Gold.id,
    note: String = "",
    createdAt: Long = 0,
) = AnnotationEntity(
    id = createdAt + 1,
    lawId = 1,
    section = "core",
    start = start,
    end = end,
    anchorText = anchor,
    type = type,
    color = color,
    note = note,
    createdAt = createdAt,
)

class AnnotationAnchorTest {

    @Test
    fun `an unchanged anchor resolves exactly`() {
        val text = "0123456789"
        val resolved = mark(2, 7, anchor = "23456").resolve(text)
        assertEquals(AnchorFit.Exact, resolved.fit)
        assertEquals(2, resolved.start)
        assertEquals("23456", resolved.quoted)
    }

    @Test
    fun `a re-encoded document is re-found by its anchor text`() {
        // The prose grew above the mark, so the stored offsets now point somewhere else
        // entirely. The anchor is what saves it. It has to be a realistic length - below the
        // search minimum an anchor is ambiguous, which is the next test.
        val anchor = "the first ones are the ones the speaker rehearsed"
        val moved = "PREFACE ADDED BY A LATER EDIT. $anchor, and the risk lives here."
        val original = mark(0, anchor.length, anchor = anchor).resolve(anchor)
        assertEquals(AnchorFit.Exact, original.fit)

        val resolved = mark(0, anchor.length, anchor = anchor).resolve(moved)
        assertEquals(AnchorFit.Adjusted, resolved.fit)
        assertEquals(31, resolved.start)
        assertEquals(anchor, resolved.quoted)
    }

    @Test
    fun `a mark with no anchor falls back to clamped offsets`() {
        val resolved = mark(0, 5).resolve("0123456789")
        assertEquals(AnchorFit.Adjusted, resolved.fit)
        assertEquals("01234", resolved.quoted)
    }

    @Test
    fun `offsets past the end of shortened text are clamped, not fatal`() {
        val resolved = mark(0, 999, anchor = "gone").resolve("0123")
        assertEquals(AnchorFit.Adjusted, resolved.fit)
        assertEquals(4, resolved.end)
    }

    @Test
    fun `a mark with no remaining room is orphaned but kept`() {
        val resolved = mark(99, 120).resolve("0123")
        assertEquals(AnchorFit.Orphaned, resolved.fit)
        assertEquals("", resolved.quoted)
    }

    @Test
    fun `empty text orphans rather than throwing`() {
        assertEquals(AnchorFit.Orphaned, mark(0, 5, anchor = "x").resolve("").fit)
    }

    @Test
    fun `a short anchor is not searched for, because it occurs everywhere`() {
        // "abc" will appear somewhere in this text, but trusting that would re-home the mark to
        // the wrong place. Under the minimum length we degrade to clamping instead.
        val text = "xxabcxx and abc again"
        val resolved = mark(0, 3, anchor = "abc").resolve(text)
        assertEquals(AnchorFit.Adjusted, resolved.fit)
        assertEquals("xxa", resolved.quoted)
    }
}

class PaintableSpanTest {

    @Test
    fun `no marks leaves text untouched`() {
        val spans = resolvePaintableSpans("the law asserts", emptyList())
        assertTrue(spans.isEmpty())
        assertEquals("the law asserts", buildAnnotatedReaderText("the law asserts", spans).text)
    }

    @Test
    fun `a mark paints exactly its own range`() {
        val spans = resolvePaintableSpans("0123456789", listOf(mark(0, 5, anchor = "01234")))
        assertEquals(1, spans.size)
        assertEquals(0, spans[0].start)
        assertEquals(5, spans[0].end)
        assertTrue(!spans[0].truncated)
    }

    @Test
    fun `overlapping marks never share a character`() {
        val spans = resolvePaintableSpans("0123456789", listOf(mark(0, 6), mark(4, 10)))
        assertEquals(2, spans.size)
        assertEquals(0, spans[0].start)
        assertEquals(6, spans[0].end)
        assertEquals(6, spans[1].start)
        assertEquals(10, spans[1].end)
        assertTrue(spans[1].truncated)
    }

    @Test
    fun `a contained mark wins regardless of save order`() {
        val spans = resolvePaintableSpans(
            "0123456789",
            listOf(mark(2, 4, createdAt = 1), mark(0, 8, createdAt = 2)),
        )
        assertEquals(1, spans.size)
        assertEquals(0, spans[0].start)
        assertEquals(8, spans[0].end)
    }

    @Test
    fun `a fully swallowed mark is dropped rather than rendered as a sliver`() {
        val spans = resolvePaintableSpans("0123456789", listOf(mark(0, 10), mark(3, 5)))
        assertEquals(1, spans.size)
        assertEquals(0, spans[0].start)
        assertEquals(10, spans[0].end)
    }

    @Test
    fun `bookmarks are not painted`() {
        val bookmark = mark(0, 0, type = AnnotationType.Bookmark.wire, anchor = "")
        assertTrue(resolvePaintableSpans("0123456789", listOf(bookmark)).isEmpty())
    }

    @Test
    fun `a noted mark is underlined, a plain one is not`() {
        val text = "0123456789"
        val plain = buildAnnotatedReaderText(text, resolvePaintableSpans(text, listOf(mark(0, 5))))
        val noted = buildAnnotatedReaderText(
            text,
            resolvePaintableSpans(text, listOf(mark(0, 5, note = "why?"))),
        )
        assertEquals(null, plain.spanStyles[0].item.textDecoration)
        assertEquals(
            androidx.compose.ui.text.style.TextDecoration.Underline,
            noted.spanStyles[0].item.textDecoration,
        )
    }

    @Test
    fun `an unknown colour id degrades instead of throwing`() {
        // A mark saved before a palette change must not take the reader down.
        val spans = resolvePaintableSpans("0123", listOf(mark(0, 4, color = "chartreuse")))
        assertEquals(1, spans.size)
        assertEquals(HighlightColor.Gold, HighlightColor.fromId("chartreuse"))
    }

    @Test
    fun `quoted text is what the mark actually covers`() {
        assertEquals("gift", quotedText("a gift to the court", mark(2, 6, anchor = "gift")))
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