package com.example.a48lawsofpower.ui.screens

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/** What a key press means on the reader. */
enum class ReaderShortcut {
    PreviousSection,
    NextSection,
    ToggleSpeech,
    ToggleRead,
    ToggleBookmark,
}

/**
 * Maps a key press to a reader action.
 *
 * ## Why the mapping takes a [Key] and not a [KeyEvent]
 *
 * So it can be tested without a device. `KeyEvent` wraps `android.view.KeyEvent`, which is a
 * stubbed platform class in JVM unit tests - every method on it throws unless you bring in
 * Robolectric. [Key] and [KeyEventType] are ordinary Compose value types, so the whole mapping is
 * testable in milliseconds.
 *
 * Keyboard handling is exactly the kind of thing that silently rots: the composable that wires it
 * up looks fine, and what quietly breaks is a chord being swallowed by a parent, or a key arriving
 * as both KeyDown and KeyAction and toggling twice. Those are decisions about the mapping, and
 * they belong where a test can see them.
 *
 * ## Why these keys
 *
 * Single letters that map to the one verb per key: S speaks, R reads, B bookmarks. Arrows page,
 * because that is what arrows do everywhere else.
 *
 * Nothing uses a modifier. A shortcut that needs Ctrl held down is a shortcut most people will
 * never find, and on a phone with no keyboard these are inert anyway.
 */
fun shortcutFor(key: androidx.compose.ui.input.key.Key, type: androidx.compose.ui.input.key.KeyEventType): ReaderShortcut? {
    // KeyDown only, because KeyEventType has just the two and acting on both would double every
    // action. Note this means a held key repeats: Android sends repeated KeyDown, so holding an
    // arrow pages through sections and holding S re-toggles. The repeat rate is fast enough that
    // S would chatter, but debouncing it needs timing a KeyEvent does not carry, and a mapping
    // function has nowhere to keep state. Left as is - holding S is a mistake with an obvious
    // visible result, and the alternative is a shortcut that ignores a deliberate second press.
    if (type != androidx.compose.ui.input.key.KeyEventType.KeyDown) return null

    return when (key) {
        androidx.compose.ui.input.key.Key.DirectionLeft -> ReaderShortcut.PreviousSection
        androidx.compose.ui.input.key.Key.DirectionRight -> ReaderShortcut.NextSection
        androidx.compose.ui.input.key.Key.S -> ReaderShortcut.ToggleSpeech
        androidx.compose.ui.input.key.Key.R -> ReaderShortcut.ToggleRead
        androidx.compose.ui.input.key.Key.B -> ReaderShortcut.ToggleBookmark
        else -> null
    }
}

/**
 * Text shown once on opening a reader with a keyboard attached.
 *
 * Returns null when there is no keyboard, so a phone user is never told about keys they have no
 * way to press.
 */
fun shortcutHint(hasKeyboard: Boolean): String? =
    if (hasKeyboard) "← → page · S read aloud · R mark read · B bookmark" else null