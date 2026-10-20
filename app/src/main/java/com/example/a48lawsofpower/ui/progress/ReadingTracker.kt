package com.example.a48lawsofpower.ui.progress

import android.os.SystemClock
import com.example.a48lawsofpower.data.ProgressDao
import com.example.a48lawsofpower.data.todayEpochDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

        dao.recordActivity(day = today(), laws = 0, seconds = seconds)
        pendingMillis -= seconds * 1_000L

        // Deliberately no "did today just cross the minimum" test here. The reward used to be
        // emitted from this exact spot, and it meant the celebration existed only in memory: leave
        // the law before it lands, or die mid-animation, and the day was banked with nothing ever
        // shown. `pendingStreakAchievement` re-derives it from the rows this write just changed,
        // and `streak_celebrated_day` is what keeps the ones that *were* shown from showing again.
    }

    private companion object {
        /**
         * Long enough that a flush costs nothing on a reader's battery, short enough that a crash
         * or a force-stop cannot cost more than a few seconds of real reading.
         */
        const val flushIntervalMillis = 15_000L
    }
}