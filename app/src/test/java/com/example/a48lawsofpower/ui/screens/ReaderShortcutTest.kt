package com.example.a48lawsofpower.ui.screens

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderShortcutTest {

    @Test
    fun `letters map to their verb`() {
        assertEquals(ReaderShortcut.ToggleSpeech, shortcutFor(Key.S, KeyEventType.KeyDown))
        assertEquals(ReaderShortcut.ToggleRead, shortcutFor(Key.R, KeyEventType.KeyDown))
        assertEquals(ReaderShortcut.ToggleBookmark, shortcutFor(Key.B, KeyEventType.KeyDown))
    }

    @Test
    fun `arrows page between sections`() {
        assertEquals(ReaderShortcut.PreviousSection, shortcutFor(Key.DirectionLeft, KeyEventType.KeyDown))
        assertEquals(ReaderShortcut.NextSection, shortcutFor(Key.DirectionRight, KeyEventType.KeyDown))
    }

    @Test
    fun `key up is ignored`() {
        // Acting on both down and up would double every action.
        assertNull(shortcutFor(Key.S, KeyEventType.KeyUp))
        assertNull(shortcutFor(Key.DirectionRight, KeyEventType.KeyUp))
    }

    @Test
    fun `every key event type other than down is ignored`() {
        // KeyEventType has only Down and Up; acting on both would double every action.
        assertNull(shortcutFor(Key.S, KeyEventType.KeyUp))
        assertNull(shortcutFor(Key.DirectionRight, KeyEventType.KeyUp))
    }

    @Test
    fun `an unmapped key is left alone`() {
        assertNull(shortcutFor(Key.A, KeyEventType.KeyDown))
        assertNull(shortcutFor(Key.Spacebar, KeyEventType.KeyDown))
        assertNull(shortcutFor(Key.Enter, KeyEventType.KeyDown))
        assertNull(shortcutFor(Key.Escape, KeyEventType.KeyDown))
    }

    @Test
    fun `no letter is bound to two actions`() {
        // A duplicate binding is silently unreachable rather than broken, so it would not show up
        // in any symptom - only here.
        val letters = listOf(Key.S, Key.R, Key.B)
        val mapped = letters.map { shortcutFor(it, KeyEventType.KeyDown) }
        assertEquals("each letter must resolve to a distinct action", mapped.size, mapped.distinct().size)
    }

    @Test
    fun `the hint is shown only when a keyboard is present`() {
        assertNull("a phone user cannot press any of these", shortcutHint(hasKeyboard = false))
        assertEquals(true, shortcutHint(hasKeyboard = true) != null)
    }

    @Test
    fun `the hint names every action the mapping supports`() {
        val hint = shortcutHint(hasKeyboard = true)!!
        for (fragment in listOf("←", "→", "S", "R", "B")) {
            assertEquals("hint should mention $fragment", true, hint.contains(fragment))
        }
    }
}