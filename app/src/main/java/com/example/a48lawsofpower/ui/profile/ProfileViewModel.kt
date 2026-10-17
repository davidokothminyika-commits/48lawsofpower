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
}