package com.example.a48lawsofpower.ui.progress

import android.os.SystemClock
import com.example.a48lawsofpower.data.ProgressDao
import com.example.a48lawsofpower.data.STREAK_MIN_SECONDS
import com.example.a48lawsofpower.data.computeStats
import com.example.a48lawsofpower.data.todayEpochDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * A streak that has just been earned, and what to say about it.
 *
 * @property streak the streak *including* today, so a reader who has been away a week and comes
 *   back sees the number they have just earned rather than the one they left with.
 */
data class StreakCelebration(
    val streak: Int,
    val day: Long,
)

/**
 * What to say about a streak of this length.
 *
 * The voice is the app's own - court, discipline, compounding - and it deliberately never implies
 * the reader has fallen behind. There is no "don't break it!" copy here. Guilt is a bad way to
 * keep someone reading, and a shame-driven streak is exactly the mechanic that makes people quit
 * the app entirely.
 */
fun encouragementFor(streak: Int): String = when {
    streak >= 100 -> "A hundred unbroken days. The court does not forget this."
    streak >= 60 -> "Two months without a single day missed."
    streak >= 30 -> "A full month. Discipline, compounded."
    streak >= 21 -> "Three weeks. This is no longer an effort."
    streak >= 14 -> "A fortnight, unbroken."
    streak >= 7 -> "A full week of study."
    streak >= 3 -> "It is becoming a habit."
    streak >= 2 -> "Two in a row. The pattern holds."
    else -> "The streak begins."
}

/**
 * Accrues reading time and writes it to `reading_days`.
 *
 * ## Why this is not `LaunchedEffect(law) { while (true) { delay(1000) } }`
 *
 * Because almost every naive version of that is wrong in a way no test catches on the happy path:
 *
 *  - **Time must stop when the app does.** `delay` keeps running while the app is backgrounded, so
 *    a reader who leaves the phone face-down for an hour gets an hour of "reading". Both signals
 *    below have to be true - the app foregrounded *and* a law actually open - and only then does
 *    the clock run.
 *  - **Un-flushed time is lost time.** Process death silently drops anything sitting in memory, so
 *    the window is flushed on a short interval rather than only on exit, which bounds the loss to
 *    [flushIntervalMillis] instead of to however long the session ran.
 *  - **Elapsed time is derived, not counted.** The window is anchored at `elapsedRealtime` and the
 *    delta is measured when it closes. Ticking a counter instead would credit the device's sleep
 *    as reading, because ticks are delayed over sleep and a counter does not notice.
 *  - **Sub-second remainders are carried, not discarded.** Each flush writes whole seconds and
 *    keeps the remainder in [pendingMillis], so a reader who skims for 59 seconds across six pauses
 *    is not quietly short of a minute by five accumulated rounding errors.
 *
 * ## What is not handled
 *
 * A session open across midnight is attributed to the day it ends, which can be off by up to
 * [flushIntervalMillis] on the day before. Sessions are short and the error is one interval wide,
 * so it was not worth the complexity of splitting a window across a boundary that usually falls
 * mid-sentence.
 */
class ReadingTracker(
    private val dao: ProgressDao,
    private val scope: CoroutineScope,
    private val minSeconds: Int = STREAK_MIN_SECONDS,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
    private val today: () -> Long = ::todayEpochDay,
    private val tickMillis: Long = 1_000L,
) {

    private var foreground = false
    private var readerVisible = false
    private var counting = false

    /**
     * Start of the current counting window, or null when no window is open.
     *
     * Null rather than a timestamp, because "no window" and "a window that started at the epoch"
     * are different states and conflating them charged the reader for every second between closing
     * a law and the next flush - five idle minutes counted as five minutes of reading. `harvest`
     * now has a single reason to return early, and it cannot be the wrong one.
     */
    private var windowStart: Long? = null

    private var windowOpenedAt = 0L
    private var pendingMillis = 0L
    private var ticker: Job? = null

    private val _celebrations = MutableSharedFlow<StreakCelebration>(extraBufferCapacity = 1)

    /**
     * Emits exactly once per day, on the flush where today first crosses the threshold.
     *
     * "Once per day" falls out of the condition rather than needing a flag or a persisted
     * "have I already congratulated them" row: after the crossing is written, today's total is
     * already at or above the minimum, so the next flush - and the first flush after a cold start -
     * fails `before < min && after >= min` and stays quiet. A persisted key would have been another
     * thing to keep in sync with the database for no gain.
     */
    val celebrations: SharedFlow<StreakCelebration> = _celebrations.asSharedFlow()

    fun setForeground(value: Boolean) {
        if (foreground != value) {
            foreground = value
            reevaluate()
        }
    }

    /** True only while a law is actually the destination in front of the reader. */
    fun setReaderVisible(value: Boolean) {
        if (readerVisible != value) {
            readerVisible = value
            reevaluate()
        }
    }

    private fun reevaluate() {
        val shouldCount = foreground && readerVisible
        if (shouldCount == counting) return
        if (shouldCount) startCounting() else stopCounting()
    }

    private fun startCounting() {
        counting = true
        val now = elapsedRealtime()
        windowStart = now
        windowOpenedAt = now
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                delay(tickMillis)
                harvest()
                if (elapsedRealtime() - windowOpenedAt >= flushIntervalMillis) flush()
            }
        }
    }

    private fun stopCounting() {
        counting = false
        ticker?.cancel()
        ticker = null
        harvest()
        // Closing the window *before* the write is what stops the async flush below from charging
        // the idle time between closing a law and the flush landing.
        windowStart = null
        scope.launch { flush() }
    }

    /** Closes the current window, folding its elapsed time into [pendingMillis]. */
    private fun harvest() {
        val start = windowStart ?: return
        val now = elapsedRealtime()
        val delta = now - start
        if (delta <= 0L) return
        windowStart = now
        pendingMillis += delta
    }

    /** Seconds read today that have not reached the database yet. For display and tests. */
    fun pendingSeconds(): Long = pendingMillis / 1_000L

    /** Writes everything accrued so far. Safe to call at any time, including while counting. */
    suspend fun flush() {
        harvest()
        windowOpenedAt = elapsedRealtime()

        // Below a second there is nothing to record yet, and clearing the remainder here is how
        // short readings would be rounded away.
        val seconds = pendingMillis / 1_000L
        if (seconds <= 0L) return

        val day = today()
        val before = dao.getDay(day)?.secondsRead ?: 0L
        dao.recordActivity(day = day, laws = 0, seconds = seconds)
        pendingMillis -= seconds * 1_000L

        val after = dao.getDay(day)?.secondsRead ?: 0L
        if (before < minSeconds && after >= minSeconds) {
            _celebrations.tryEmit(StreakCelebration(streak = streakOn(day), day = day))
        }
    }

    /** The streak as it stands on disk, on the same rules the Profile screen reads. */
    private suspend fun streakOn(day: Long): Int {
        val rows = dao.observeAllDays().first()
        return computeStats(
            days = rows.map { it.day },
            today = day,
            lawsByDay = rows.associate { it.day to it.lawsRead },
            secondsByDay = rows.associate { it.day to it.secondsRead },
            minSecondsPerDay = minSeconds,
        ).currentStreak
    }

    private companion object {
        /**
         * Long enough that a flush costs nothing on a reader's battery, short enough that a crash
         * or a force-stop cannot cost more than a few seconds of real reading.
         */
        const val flushIntervalMillis = 15_000L
    }
}