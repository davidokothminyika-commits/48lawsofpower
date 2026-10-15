package com.example.a48lawsofpower.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
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

    private companion object {
        val KeyThemeMode = stringPreferencesKey("theme_mode")
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
}