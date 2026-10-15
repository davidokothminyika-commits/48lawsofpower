package com.example.a48lawsofpower.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a48lawsofpower.data.ProgressDao
import com.example.a48lawsofpower.data.ReadingStats
import com.example.a48lawsofpower.data.computeStats
import com.example.a48lawsofpower.data.todayEpochDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Reading totals for the Profile screen.
 *
 * [computeStats] is a pure fold over `reading_days`, so nothing here is a new query - the existing
 * one-row-per-day table answers every figure the screen shows. `ProgressDao` had no consumer until
 * now; this is it.
 */
class ProfileViewModel(progressDao: ProgressDao) : ViewModel() {

    val stats: StateFlow<ReadingStats> =
        combine(progressDao.observeAllDays(), progressDao.observeDays()) { rows, days ->
            computeStats(
                days = days,
                today = todayEpochDay(),
                lawsByDay = rows.associate { it.day to it.lawsRead },
                secondsByDay = rows.associate { it.day to it.secondsRead },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingStats())
}