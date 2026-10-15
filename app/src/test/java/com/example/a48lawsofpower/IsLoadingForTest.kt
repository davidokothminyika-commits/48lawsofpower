package com.example.a48lawsofpower

import com.example.a48lawsofpower.ui.isLoadingFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the direction of the loading predicate.
 *
 * This exists because it shipped inverted once: `isLoading` was `map { it.isNotEmpty() }`, so the
 * flag was true exactly when the laws had arrived. The Dossier branches `if (isLoading) skeletons
 * else laws`, which meant the skeleton rendered permanently and the list never drew at all - not a
 * degraded placeholder, but content that could never appear.
 *
 * A wrong-direction boolean is invisible to review and obvious to a reader, which is the wrong way
 * round. Four assertions on a one-line function is cheap insurance.
 */
class IsLoadingForTest {

    @Test
    fun `nothing loaded yet is loading`() {
        assertTrue(isLoadingFor(0))
    }

    @Test
    fun `laws present means not loading`() {
        // The assertion that would have caught the original bug.
        assertFalse(isLoadingFor(48))
    }

    @Test
    fun `a single law is enough to stop loading`() {
        // Not `> 1`: the pre-packaged asset always carries 48, but a partially-populated read must
        // still hand over rather than keep the skeleton up.
        assertFalse(isLoadingFor(1))
    }

    @Test
    fun `loading is exactly "the count is zero", across the range`() {
        // The real invariant. Stated positively because "adjacent counts must differ" would be false
        // for every pair above zero - two loaded states are meant to look the same.
        for (count in 0..64) {
            assertEquals(
                "count=$count",
                count == 0,
                isLoadingFor(count),
            )
        }
    }

    @Test
    fun `the predicate is the inverse of the one that shipped broken`() {
        // Pins the direction against the exact expression that was wrong, rather than against a
        // single sample value that a future edit might still satisfy by coincidence.
        for (count in 0..64) {
            assertNotEquals(isLoadingFor(count), count != 0)
        }
    }
}