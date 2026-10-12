package com.example.a48lawsofpower

import com.example.a48lawsofpower.legal.ConsentRecord
import com.example.a48lawsofpower.legal.ConsentStatus
import com.example.a48lawsofpower.legal.consentStatusFor
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The version gate is the part of consent that can be silently wrong. If it ever returns [ConsentStatus.Granted]
 * for a reader who did not agree, every other check in the feature is moot.
 *
 * `internal` is visible here because the Kotlin Android plugin puts unit tests in the main module's
 * friend path.
 */
class ConsentStatusTest {

    private fun statusFor(record: ConsentRecord, currentVersion: Int = 2) =
        consentStatusFor(record, currentVersion)

    @Test
    fun `a first launch has nothing stored and must be prompted`() {
        assertEquals(ConsentStatus.Required, statusFor(ConsentRecord()))
    }

    @Test
    fun `an acceptance at the current version is honoured`() {
        val record = ConsentRecord(accepted = true, version = 2)
        assertEquals(ConsentStatus.Granted, statusFor(record))
    }

    @Test
    fun `an acceptance from an older version of the terms is re-prompted`() {
        // The whole point of storing the version: shipping revised terms has to reach readers who
        // already agreed to the old ones.
        val record = ConsentRecord(accepted = true, version = 1)
        assertEquals(ConsentStatus.Required, statusFor(record))
    }

    @Test
    fun `an unaccepted record is never granted, whatever version it claims`() {
        // A version written without the accepted flag - a partially written record, or a hand-edited
        // one - must still fail closed.
        val record = ConsentRecord(accepted = false, version = 99)
        assertEquals(ConsentStatus.Required, statusFor(record))
    }

    @Test
    fun `an acceptance newer than this build still passes`() {
        // Guards the >= against being tightened to ==, which would trap a user who accepted on a
        // newer build and then downgraded.
        val record = ConsentRecord(accepted = true, version = 7)
        assertEquals(ConsentStatus.Granted, statusFor(record, currentVersion = 2))
    }

    @Test
    fun `revoking leaves a record that reads as not consented`() {
        // ConsentStore.revoke() removes all three keys, so a revoked reader lands on the empty
        // default and is prompted again.
        val revoked = ConsentRecord()
        assertEquals(ConsentStatus.Required, statusFor(revoked))
    }
}