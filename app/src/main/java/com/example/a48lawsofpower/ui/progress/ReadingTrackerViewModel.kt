package com.example.a48lawsofpower.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a48lawsofpower.data.ProgressDao

/**
 * Holds the [ReadingTracker] for the Activity's lifetime.
 *
 * Activity-scoped rather than composable-scoped on purpose: the tracker has to outlive rotation,
 * or every rotation would close the counting window and write a fragmentary number of seconds that
 * never adds up to a minute.
 *
 * The foreground flag is driven from `Activity.onStart`/`onStop` rather than from a lifecycle
 * observer in composition, because the flush on `onStop` is the one write that must happen even if
 * the composition has already been torn down.
 */
class ReadingTrackerViewModel(dao: ProgressDao) : ViewModel() {

    private val tracker = ReadingTracker(dao, viewModelScope)

    fun setForeground(foreground: Boolean) = tracker.setForeground(foreground)

    fun setReaderVisible(visible: Boolean) = tracker.setReaderVisible(visible)
}