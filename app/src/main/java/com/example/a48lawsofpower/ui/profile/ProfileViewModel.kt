package com.example.a48lawsofpower.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a48lawsofpower.data.ProgressDao
import com.example.a48lawsofpower.data.ReadingStats
import com.example.a48lawsofpower.data.STREAK_MIN_SECONDS
import com.example.a48lawsofpower.data.computeStats
import com.example.a48lawsofpower.data.todayEpochDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * One day in the Profile's recent-reading strip.
 *
 * [qualifies] is the same rule the streak uses, not "a row exists". A day with twenty seconds of
 * reading has a row and does not count, and a strip that showed it as a met day would contradict
 * the streak number sitting directly above it.
 */
data class RecentDay(
    val day: Long,
    val qualifies: Boolean,
)

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

    val stats: StateFlow<ReadingStats> =
        progressDao.observeAllDays()
            .map { rows ->
                computeStats(
                    days = rows.map { it.day },
                    today = todayEpochDay(),
                    lawsByDay = rows.associate { it.day to it.lawsRead },
                    secondsByDay = rows.associate { it.day to it.secondsRead },
                    minSecondsPerDay = STREAK_MIN_SECONDS,
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingStats())

    /**
     * The last seven days, oldest first, always exactly [WEEK_LENGTH] entries.
     *
     * Padded rather than filtered, so the strip is a fixed seven cells whether or not the reader
     * has any history. A strip that grows from two cells to seven as the week fills in shifts every
     * label sideways, and the whole point of the row is that the reader can see at a glance which
     * day is missing.
     */
    val week: StateFlow<List<RecentDay>> =
        progressDao.observeAllDays()
            .map { rows ->
                val today = todayEpochDay()
                val met = rows
                    .filter { it.secondsRead >= STREAK_MIN_SECONDS }
                    .mapTo(mutableSetOf()) { it.day }
                (0 until WEEK_LENGTH).map { back ->
                    val day = today - (WEEK_LENGTH - 1 - back)
                    RecentDay(day = day, qualifies = day in met)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private companion object {
        /** Seven cells. A fortnight is more than anyone reads at a glance on a phone. */
        const val WEEK_LENGTH = 7
    }
}