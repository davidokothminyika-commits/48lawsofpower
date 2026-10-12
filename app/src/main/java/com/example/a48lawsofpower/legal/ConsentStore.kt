package com.example.a48lawsofpower.legal

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Declared once at file scope: DataStore forbids two instances sharing a file for one Context. */
private val Context.consentDataStore: DataStore<Preferences> by preferencesDataStore(name = "consent")

/** What was actually written to disk. Raw, so the version comparison stays the caller's job. */
data class ConsentRecord(
    val accepted: Boolean = false,
    val version: Int = 0,
    val acceptedAtMillis: Long = 0L,
)

enum class ConsentStatus {
    /**
     * The stored record has not been read yet. The gate must not be shown in this state: flashing
     * it for the frame or two before DataStore answers reads as a consent prompt popping at the
     * reader unbidden, on a launch where they already agreed.
     */
    Unknown,

    /** No acceptance on record, or an acceptance against text that has since been revised. */
    Required,

    Granted,
}

/**
 * Decides whether a stored record still satisfies the current terms.
 *
 * Version-gated rather than a bare boolean, because a consent flag cannot distinguish "agreed to the
 * current terms" from "agreed to whatever was on screen in March". Raising `currentVersion` re-prompts
 * exactly the readers whose agreement went stale, and leaves everyone who already accepted the new
 * text alone.
 *
 * The comparison is `>=`, not `==`: an acceptance can only ever come from a version at or below the
 * one the app ships, and `>=` fails safe if a future build ever ships a lower number than the
 * installed record.
 */
internal fun consentStatusFor(record: ConsentRecord, currentVersion: Int): ConsentStatus =
    if (record.accepted && record.version >= currentVersion) ConsentStatus.Granted else ConsentStatus.Required

/**
 * Consent acceptance, persisted in DataStore.
 *
 * Deliberately not in Room. The database is shipped as a pre-packaged asset that Room validates
 * against an exported schema, so putting a user-preference column there would mean a schema
 * migration, an asset rebuild, and an uninstall for every reader to pick it up. Consent has to be
 * right on the very first frame of a first launch, and it has to survive a migration of the terms
 * text independently of the law content - neither of which is the Room asset's job.
 */
class ConsentStore(private val context: Context) {

    val record: Flow<ConsentRecord> = context.consentDataStore.data
        // A corrupt or unreadable file is treated as "never agreed" rather than allowed to crash the
        // app on launch. Failing closed on consent is the only safe direction.
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { preferences ->
            ConsentRecord(
                accepted = preferences[KeyAccepted] ?: false,
                version = preferences[KeyVersion] ?: 0,
                acceptedAtMillis = preferences[KeyAcceptedAt] ?: 0L,
            )
        }

    /**
     * Records acceptance together with the version accepted, in one transaction - a bare boolean
     * written without its version would read back as stale and re-prompt on the next launch.
     */
    suspend fun accept(version: Int, timestampMillis: Long) {
        context.consentDataStore.edit { preferences ->
            preferences[KeyAccepted] = true
            preferences[KeyVersion] = version
            preferences[KeyAcceptedAt] = timestampMillis
        }
    }

    /** Clears the acceptance, which drops the reader back through the gate on the next launch. */
    suspend fun revoke() {
        context.consentDataStore.edit { preferences ->
            preferences.remove(KeyAccepted)
            preferences.remove(KeyVersion)
            preferences.remove(KeyAcceptedAt)
        }
    }

    private companion object {
        val KeyAccepted = booleanPreferencesKey("consent_accepted")
        val KeyVersion = intPreferencesKey("consent_version")
        val KeyAcceptedAt = longPreferencesKey("consent_accepted_at")
    }
}