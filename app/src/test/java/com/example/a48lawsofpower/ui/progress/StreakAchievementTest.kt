package com.example.a48lawsofpower.ui.progress

import com.example.a48lawsofpower.data.ReadingDayEntity
import com.example.a48lawsofpower.data.STREAK_MIN_SECONDS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** today() fixed, so a test cannot quietly pass because the calendar moved. */
private const val TODAY = 20_000L

private val BANKED = STREAK_MIN_SECONDS.toLong()

/** A row [offset] days back that has cleared the streak minimum, or one that has not. */
private fun day(offset: Long, seconds: Long = BANKED) = ReadingDayEntity(
    day = TODAY - offset,
    secondsRead = seconds,
)

/**
 * The rule behind the achievement overlay.
 *
 * These are the cases the card's *trigger* has to get right, as opposed to its animation - the ones
 * where showing would contradict the Profile figure beneath it, or where not showing would mean an
 * achievement the reader earned is never acknowledged. The old design could only be tested by
 * driving a tracker through sixty virtual seconds; this one is a fold, so it is a table of inputs.
 */
class StreakAchievementTest {

    @Test
    fun `the first achievement ever is flagged as the first`() {
        val pending = pendingStreakAchievement(
            days = listOf(day(offset = 1)),
            today = TODAY,
            lastCelebratedDay = null,
        )

        assertEquals(1, pending?.streak)
        assertEquals("yesterday is the newest banked day", TODAY - 1, pending?.day)
        assertTrue(pending!!.isFirstEver)
    }

    @Test
    fun `an uncelebrated run behind yesterday is a full streak including its anchor`() {
        val pending = pendingStreakAchievement(
            days = listOf(day(offset = 3), day(offset = 2), day(offset = 1)),
            today = TODAY,
            lastCelebratedDay = null,
        )

        assertEquals("today not banked yet, so the run ends at yesterday", 3, pending?.streak)
        assertEquals(TODAY - 1, pending?.day)
    }

    @Test
    fun `an acknowledged day produces nothing`() {
        val pending = pendingStreakAchievement(
            days = listOf(day(offset = 3), day(offset = 2), day(offset = 1)),
            today = TODAY,
            lastCelebratedDay = TODAY - 1,
        )

        assertNull("the card was already shown for this day", pending)
    }

    @Test
    fun `today's crossing extends the run and is a new achievement`() {
        val prior = listOf(day(offset = 3), day(offset = 2), day(offset = 1))
        assertNull(
            pendingStreakAchievement(prior, TODAY, lastCelebratedDay = TODAY - 1),
        )

        val pending = pendingStreakAchievement(
            days = prior + day(offset = 0),
            today = TODAY,
            lastCelebratedDay = TODAY - 1,
        )

        assertEquals("including today", 4, pending?.streak)
        assertEquals(TODAY, pending?.day)
        assertFalse("this device has celebrated before", pending!!.isFirstEver)
    }

    @Test
    fun `an empty history produces nothing`() {
        assertNull(pendingStreakAchievement(emptyList(), TODAY, lastCelebratedDay = null))
    }

    @Test
    fun `a day under the minimum is not an achievement`() {
        val pending = pendingStreakAchievement(
            days = listOf(day(offset = 0, seconds = STREAK_MIN_SECONDS - 1L)),
            today = TODAY,
            lastCelebratedDay = null,
        )

        assertNull(pending)
    }

    @Test
    fun `an under-threshold today does not make an older banked day current again`() {
        // Yesterday banked two days ago... the row set is a broken run: the only banked day is
        // older than yesterday, so the Profile figure would read zero while the card claimed one.
        val pending = pendingStreakAchievement(
            days = listOf(day(offset = 2), day(offset = 0, seconds = 5L)),
            today = TODAY,
            lastCelebratedDay = null,
        )

        assertNull(pending)
    }

    @Test
    fun `a run that ended before yesterday is not celebrated on a later launch`() {
        val pending = pendingStreakAchievement(
            days = listOf(day(offset = 5), day(offset = 4)),
            today = TODAY,
            lastCelebratedDay = null,
        )

        assertNull("congratulating on it would contradict a streak of zero", pending)
    }

    @Test
    fun `an acknowledgment ahead of the newest row stays quiet`() {
        // The clock was set backwards after the achievement was acknowledged. Nothing new has been
        // earned, so re-announcing the same days would be the card firing twice.
        val pending = pendingStreakAchievement(
            days = listOf(day(offset = 1)),
            today = TODAY,
            lastCelebratedDay = TODAY,
        )

        assertNull(pending)
    }

    @Test
    fun `a future-dated row never announces a streak of zero`() {
        // Reachable only by a device whose clock moved back after banking. The newest day passes
        // the today-or-yesterday filter on arithmetic alone, but computeStats has no live run to
        // anchor on - and a card reading "0" would be worse than no card.
        val pending = pendingStreakAchievement(
            days = listOf(day(offset = -1)),
            today = TODAY,
            lastCelebratedDay = null,
        )

        assertNull(pending)
    }
}
