package com.example.a48lawsofpower.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a48lawsofpower.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Persisted reader preferences. Separate from the consent store on purpose: withdrawing consent must
 * clear the legal record and nothing else, and the two have different lifetimes.
 */
class SettingsStore(private val context: Context) {

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { preferences ->
            // An unrecognised value means a downgrade or a hand-edited file. Falling back to System
            // is better than refusing to launch, and better than pinning a stale family.
            preferences[KeyThemeMode]?.let { stored ->
                ThemeMode.entries.firstOrNull { it.name == stored }
            } ?: ThemeMode.System
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[KeyThemeMode] = mode.name }
    }

    /**
     * Whether the reader has dismissed the entry screen.
     *
     * Nullable, and that is the whole point of the type. The start destination of the nav graph has
     * to be chosen before the graph composes, so "not read yet" and "read, and the answer is false"
     * cannot be the same value - defaulting to false would flash the entry screen at everyone on
     * every cold start, and defaulting to true would skip it for a genuine first launch.
     */
    val hasSeenIntro: Flow<Boolean> = context.settingsDataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { it[KeySeenIntro] ?: false }

    suspend fun markIntroSeen() {
        context.settingsDataStore.edit { it[KeySeenIntro] = true }
    }

    /**
     * The most recent calendar day whose streak achievement the reader has actually been shown.
     *
     * Null until that has ever happened on this device. This is what turns the celebration from
     * something that has to be caught live into something that can be *acknowledged*: the number is
     * re-derived from `reading_days` on every launch, so an achievement that was interrupted
     * mid-animation, that arrived with restored data, or that pre-dates this key is still pending
     * next time - and is celebrated exactly once, when it is finally acknowledged.
     *
     * An epoch-day rather than a timestamp, for the reason `reading_days` uses one: it is only ever
     * compared against rows already keyed that way, so a timezone change cannot leave the
     * acknowledgment and the table disagreeing about which day is which.
     */
    val lastCelebratedDay: Flow<Long?> = context.settingsDataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { it[KeyCelebratedDay] }

    suspend fun markStreakCelebrated(day: Long) {
        context.settingsDataStore.edit { it[KeyCelebratedDay] = day }
    }

    private companion object {
        val KeyThemeMode = stringPreferencesKey("theme_mode")
        val KeySeenIntro = booleanPreferencesKey("seen_intro")
        val KeyCelebratedDay = longPreferencesKey("streak_celebrated_day")
    }
}

class SettingsViewModel(private val store: SettingsStore) : ViewModel() {

    /**
     * Eagerly shared, for the same reason consent is: the family has to be settled before the first
     * frame. Starting cold would paint the default family and then repaint the whole app a frame
     * later, which on a reading surface is a visible flash.
     */
    val themeMode: StateFlow<ThemeMode> = store.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.System)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { store.setThemeMode(mode) }
    }

    /**
     * Null until the first read lands. See [SettingsStore.hasSeenIntro] for why that is not a
     * boolean with a default.
     */
    val hasSeenIntro: StateFlow<Boolean?> = store.hasSeenIntro
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun markIntroSeen() {
        viewModelScope.launch { store.markIntroSeen() }
    }
}