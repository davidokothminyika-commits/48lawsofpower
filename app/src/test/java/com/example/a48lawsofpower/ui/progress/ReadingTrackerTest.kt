package com.example.a48lawsofpower.ui.progress

import com.example.a48lawsofpower.data.ProgressDao
import com.example.a48lawsofpower.data.ReadingDayEntity
import com.example.a48lawsofpower.data.STREAK_MIN_SECONDS
import com.example.a48lawsofpower.data.computeStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * In-memory [ProgressDao] that reproduces the two behaviours [ReadingTracker] depends on: seconds
 * accumulate additively, and `laws_read` takes the high-water mark rather than incrementing.
 *
 * Deliberately not a Room test. What is being checked here is the tracker's *timing* decisions, and
 * a real database would only add the risk that a slow query on a background dispatcher hid a
 * scheduling bug in the tracker itself.
 */
private class FakeProgressDao : ProgressDao {

    val rows = MutableStateFlow<Map<Long, ReadingDayEntity>>(emptyMap())

    override fun observeDays(): Flow<List<Long>> = rows.map { it.keys.sorted() }

    override fun observeAllDays(): Flow<List<ReadingDayEntity>> =
        rows.map { it.values.sortedByDescending(ReadingDayEntity::day) }

    override suspend fun recordActivity(day: Long, laws: Int, seconds: Long) {
        rows.value = rows.value.toMutableMap().apply {
            val existing = get(day)
            this[day] = ReadingDayEntity(
                day = day,
                lawsRead = maxOf(existing?.lawsRead ?: 0, laws),
                secondsRead = (existing?.secondsRead ?: 0L) + seconds,
            )
        }
    }

    override suspend fun getDay(day: Long): ReadingDayEntity? = rows.value[day]

    override suspend fun clear() {
        rows.value = emptyMap()
    }
}

/** Copy the reader should never be shown. Deliberately does not ban the word "streak". */
private val GUILT_WORDS = listOf("don't", "dont", "break", "lose", "fail", "lazy", "behind")

/** today() fixed, so a test cannot quietly pass because the calendar moved. */
private const val TODAY = 20_000L

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingTrackerTest {

    /**
     * The tracker's clock is bound to the test scheduler's virtual time, so `advanceTimeBy` is
     * literally "61 seconds of reading" rather than a mock that has to be poked by hand in the right
     * order. This is what makes the timing tests below readable.
     */
    private fun TestScope.tracker(
        dao: ProgressDao,
        minSeconds: Int = STREAK_MIN_SECONDS,
    ) = ReadingTracker(
        dao = dao,
        // backgroundScope, not the test body: the tracker's ticker is a deliberately endless loop
        // and would otherwise be an uncompleted child job that fails every test using it. This also
        // mirrors production, where the tracker's scope is the ViewModel's and dies with it.
        scope = backgroundScope,
        minSeconds = minSeconds,
        elapsedRealtime = { testScheduler.currentTime },
        today = { TODAY },
    )

    @Test
    fun `counts seconds only while foreground and a law is open`() = runTest {
        val dao = FakeProgressDao()
        val tracker = tracker(dao)

        tracker.setForeground(true)
        tracker.setReaderVisible(true)
        advanceTimeBy(30_000)
        tracker.setReaderVisible(false)
        advanceTimeBy(60_000)
        tracker.flush()

        assertEquals(30L, dao.getDay(TODAY)?.secondsRead)
    }

    @Test
    fun `backgrounded time does not accrue`() = runTest {
        val dao = FakeProgressDao()
        val tracker = tracker(dao)

        tracker.setForeground(true)
        tracker.setReaderVisible(true)
        advanceTimeBy(10_000)
        tracker.setForeground(false)
        advanceTimeBy(300_000)
        tracker.setForeground(true)
        advanceTimeBy(5_000)
        tracker.flush()

        assertEquals(
            "an hour in the background is not reading",
            15L,
            dao.getDay(TODAY)?.secondsRead,
        )
    }

    @Test
    fun `backgrounding flushes so a later crash cannot lose the session`() = runTest {
        val dao = FakeProgressDao()
        val tracker = tracker(dao)

        tracker.setForeground(true)
        tracker.setReaderVisible(true)
        advanceTimeBy(45_000)
        tracker.setForeground(false)
        // Deliberately no explicit flush: the stop path must have written on its own.
        runCurrent()

        assertEquals(45L, dao.getDay(TODAY)?.secondsRead)
    }

    @Test
    fun `sub-second remainders are carried rather than rounded away`() = runTest {
        val dao = FakeProgressDao()
        val tracker = tracker(dao)

        tracker.setForeground(true)
        tracker.setReaderVisible(true)

        // Five sessions of 11.8s. Truncating each one to 11 would total 55 and lose a day that the
        // reader actually spent 59 seconds reading; the remainder has to survive the flush.
        repeat(5) {
            advanceTimeBy(11_800)
            tracker.flush()
            tracker.setForeground(false)
            tracker.setForeground(true)
            runCurrent()
        }

        assertEquals(59L, dao.getDay(TODAY)?.secondsRead)
    }

    @Test
    fun `celebrates once when the day first clears the minimum`() = runTest {
        val dao = FakeProgressDao()
        val tracker = tracker(dao)
        val seen = mutableListOf<StreakCelebration>()
        val collector = backgroundScope.launch {
            tracker.celebrations.collect { seen += it }
        }

        tracker.setForeground(true)
        tracker.setReaderVisible(true)
        advanceTimeBy(59_000)
        tracker.flush()
        runCurrent()
        assertTrue("a sub-minute day is not a streak", seen.isEmpty())

        advanceTimeBy(2_000)
        tracker.flush()
        runCurrent()

        assertEquals(1, seen.size)
        assertEquals(1, seen.single().streak)
        collector.cancel()
    }

    @Test
    fun `does not celebrate twice on the same day`() = runTest {
        val dao = FakeProgressDao()
        val tracker = tracker(dao)
        val seen = mutableListOf<StreakCelebration>()
        val collector = backgroundScope.launch {
            tracker.celebrations.collect { seen += it }
        }

        tracker.setForeground(true)
        tracker.setReaderVisible(true)
        advanceTimeBy(70_000)
        tracker.flush()
        runCurrent()
        assertEquals(1, seen.size)

        // Keep reading. The day is already banked, so the reader should not be congratulated again.
        advanceTimeBy(120_000)
        tracker.flush()
        tracker.setForeground(false)
        tracker.setForeground(true)
        advanceTimeBy(60_000)
        tracker.flush()
        runCurrent()

        assertEquals("one reward per day, not one per flush", 1, seen.size)
        collector.cancel()
    }

    @Test
    fun `celebration reports the streak including today`() = runTest {
        val dao = FakeProgressDao()
        // Three consecutive prior days already banked.
        for (offset in 3 downTo 1) {
            dao.recordActivity(TODAY - offset, laws = 0, seconds = STREAK_MIN_SECONDS.toLong())
        }
        val tracker = tracker(dao)
        var seen: StreakCelebration? = null
        val collector = backgroundScope.launch { tracker.celebrations.collect { seen = it } }

        tracker.setForeground(true)
        tracker.setReaderVisible(true)
        advanceTimeBy(STREAK_MIN_SECONDS * 1_000L + 1_000)
        tracker.flush()
        runCurrent()

        assertEquals(4, seen?.streak)
        collector.cancel()
    }

    @Test
    fun `a day below the minimum does not extend a streak`() {
        val stats = computeStats(
            days = listOf(TODAY - 2, TODAY - 1, TODAY),
            today = TODAY,
            secondsByDay = mapOf(TODAY - 2L to 600L, TODAY - 1L to 600L, TODAY to 5L),
            minSecondsPerDay = STREAK_MIN_SECONDS,
        )

        assertEquals("yesterday's pair still counts as a live streak", 2, stats.currentStreak)
        assertFalse(stats.todayQualifies)
        assertEquals(2, stats.totalDays)
    }

    @Test
    fun `a day at the minimum does extend a streak`() {
        val stats = computeStats(
            days = listOf(TODAY - 2, TODAY - 1, TODAY),
            today = TODAY,
            secondsByDay = mapOf(
                TODAY - 2L to 60L,
                TODAY - 1L to 60L,
                TODAY to STREAK_MIN_SECONDS.toLong(),
            ),
            minSecondsPerDay = STREAK_MIN_SECONDS,
        )

        assertEquals(3, stats.currentStreak)
        assertTrue(stats.todayQualifies)
    }

    @Test
    fun `minutes read today stays honest on a day too short to bank`() {
        val stats = computeStats(
            days = listOf(TODAY),
            today = TODAY,
            secondsByDay = mapOf(TODAY to 45L),
            minSecondsPerDay = STREAK_MIN_SECONDS,
        )

        // The goal readout needs the real number, not the banked one - a reader who has read 45
        // seconds must be told they need 15 more, not shown a zero.
        assertEquals(45L, stats.secondsToday)
        assertEquals(0, stats.currentStreak)
        assertEquals(15L, stats.secondsToGoal())
    }

    @Test
    fun `encouragement never shames a lapsed reader`() {
        // Only positive frames are asserted. The absence of guilt language is a design rule, and a
        // regression to "don't break your streak!" should fail here rather than ship.
        for (streak in 1..120) {
            val copy = encouragementFor(streak)
            assertTrue("streak $streak produced blank copy", copy.isNotBlank())
            GUILT_WORDS.forEach { banned ->
                assertFalse(
                    "streak $streak copy contains '$banned': $copy",
                    copy.lowercase().contains(banned),
                )
            }
        }
    }

    @Test
    fun `pending seconds are readable while still counting`() = runTest {
        val dao = FakeProgressDao()
        val tracker = tracker(dao)

        tracker.setForeground(true)
        tracker.setReaderVisible(true)
        advanceTimeBy(9_000)
        runCurrent()

        // Unflushed, so nothing is in the database yet - which is the whole reason the periodic
        // flush exists.
        assertEquals(9L, tracker.pendingSeconds())
        assertEquals("nothing written yet", 0L, dao.getDay(TODAY)?.secondsRead ?: 0L)

        // Closing the law drains it to the database.
        tracker.setReaderVisible(false)
        runCurrent()
        assertEquals(0L, tracker.pendingSeconds())
        assertEquals(9L, dao.getDay(TODAY)?.secondsRead)
    }

    @Test
    fun `a day with no qualifying activity reports no streak`() {
        val stats = computeStats(
            days = listOf(TODAY - 1, TODAY),
            today = TODAY,
            secondsByDay = mapOf(TODAY - 1L to 3L, TODAY to 3L),
            minSecondsPerDay = STREAK_MIN_SECONDS,
        )

        assertEquals(0, stats.currentStreak)
        assertEquals(0, stats.totalDays)
        assertEquals(0, stats.longestStreak)
    }
}

