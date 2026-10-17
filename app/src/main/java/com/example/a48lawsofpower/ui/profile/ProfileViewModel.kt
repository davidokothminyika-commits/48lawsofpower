package com.example.a48lawsofpower.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a48lawsofpower.data.ProgressDao
import com.example.a48lawsofpower.data.ReadingDayEntity
import com.example.a48lawsofpower.data.ReadingStats
import com.example.a48lawsofpower.data.STREAK_MIN_SECONDS
import com.example.a48lawsofpower.data.computeStats
import com.example.a48lawsofpower.data.todayEpochDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/**
 * One day in the Profile's recent-reading strip.
 *
 * [qualifies] is the same rule the streak uses, not "a row exists". A day with twenty seconds of
 * reading has a row and does not count, and a strip that showed it as a met day would contradict
 * the streak number sitting directly above it.
 *
 * [initial] is resolved here rather than in the composable. The label looks like free work - a letter
 * out of a seven-entry table - but getting it needs `LocalDate`, whose first use on a process
 * initialises a good deal of the date-time machinery, and composition runs on the main thread. That
 * one-time cost landed squarely on the frame the reader tapped Profile, which was consistently the
 * slowest tab; deriving it here moves it off the critical path entirely.
 */
data class RecentDay(
    val day: Long,
    val qualifies: Boolean,
    val initial: String,
)

/**
 * Single-letter weekday, indexed by [java.time.DayOfWeek.getValue] (Mon = 1).
 *
 * A lookup table rather than `DayOfWeek.getDisplayName(TextStyle.NARROW, Locale)`, which is the
 * obvious spelling and pulls in locale and CLDR data the first time it is touched. English-only by
 * construction: a localised label needs [java.time.format.DateTimeFormatter] with a locale, and that
 * is only worth paying once the app has a language setting to honour.
 */
private val WEEKDAY_INITIALS = arrayOf("", "M", "T", "W", "T", "F", "S", "S")

/**
 * Reading totals for the Profile screen.
 *
 * [computeStats] is a pure fold over `reading_days`, so nothing here is a new query - the existing
 * one-row-per-day table answers every figure the screen shows.
 *
 * The threshold is passed rather than defaulted, and that is the whole difference between a streak
 * and a launch counter: without [STREAK_MIN_SECONDS] here, opening the app for two seconds would
 * bank a day. `secondsToday` is read from the unfiltered row set, so "minutes read today" stays
 * honest even on a day too short to count - the goal readout needs the true number, not the
 * banked one.
 */
class ProfileViewModel(progressDao: ProgressDao) : ViewModel() {

    /** Everything the screen needs, from one read of one table. */
    private data class Snapshot(val stats: ReadingStats, val week: List<RecentDay>)

    /**
     * One subscription to `reading_days`, shared by both figures below.
     *
     * `stats` and `week` used to each collect [ProgressDao.observeAllDays] independently, so opening
     * the Profile ran the same query and the same row mapping twice. They now hang off one read.
     *
     * `flowOn(Dispatchers.Default)` is the other half of this. Room delivers on its own executor, but
     * a `map` runs in the *collector's* context - and the collector is `viewModelScope`, which is
     * `Dispatchers.Main.immediate`. So the fold, and the `LocalDate.now(ZoneId.systemDefault())` it
     * calls, were all running on the thread that has to draw the frame.
     */
    private val snapshot: StateFlow<Snapshot> =
        progressDao.observeAllDays()
            .map { days ->
                val today = todayEpochDay()
                Snapshot(
                    stats = computeStats(
                        days = days.map { it.day },
                        today = today,
                        lawsByDay = days.associate { it.day to it.lawsRead },
                        secondsByDay = days.associate { it.day to it.secondsRead },
                        minSecondsPerDay = STREAK_MIN_SECONDS,
                    ),
                    week = (0 until WEEK_LENGTH).map { back ->
                        val day = today - (WEEK_LENGTH - 1 - back)
                        RecentDay(
                            day = day,
                            qualifies = days.any { it.day == day && it.secondsRead >= STREAK_MIN_SECONDS },
                            initial = WEEKDAY_INITIALS[LocalDate.ofEpochDay(day).dayOfWeek.value],
                        )
                    },
                )
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Snapshot(ReadingStats(), emptyList()))

    val stats: StateFlow<ReadingStats> =
        snapshot.map { it.stats }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingStats())

    /**
     * The last seven days, oldest first, always exactly [WEEK_LENGTH] entries.
     *
     * Padded rather than filtered, so the strip is a fixed seven cells whether or not the reader has
     * any history. A strip that grows from two cells to seven as the week fills in shifts every
     * label sideways, and the whole point of the row is that the reader can see at a glance which
     * day is missing.
     */
    val week: StateFlow<List<RecentDay>> =
        snapshot.map { it.week }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private companion object {
        /** Seven cells. A fortnight is more than anyone reads at a glance on a phone. */
        const val WEEK_LENGTH = 7
    }
}