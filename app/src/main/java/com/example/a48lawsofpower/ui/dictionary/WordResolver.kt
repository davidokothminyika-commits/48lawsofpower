package com.example.a48lawsofpower.ui.dictionary

import androidx.compose.ui.text.TextLayoutResult

/**
 * Finds the word under a tap.
 *
 * ## Why this is its own file
 *
 * The offset arithmetic is the part of tapping-to-lookup that can actually be wrong, and it is the
 * part a screenshot cannot check. Getting it wrong does not crash - it looks up a nearby word and
 * returns a confident, plausible, incorrect definition, which is the worst possible failure for a
 * dictionary. So the character logic lives here, separated from the Compose plumbing, and is tested
 * directly.
 *
 * `TextLayoutResult` appears only in [wordAt]; [wordAtOffset] is pure and is what the tests drive.
 */
object WordResolver {

    /**
     * Characters that can appear inside a word.
     *
     * Apostrophes are included so "monarch's" resolves whole, and hyphens so "counter-intelligence"
     * does not resolve to "intelligence" when the reader tapped the first half. Curly and straight
     * apostrophes both count: the prose uses the typographic form.
     */
    private fun isWordChar(ch: Char): Boolean =
        ch.isLetter() || ch == '\'' || ch == '\u2019' || ch == '-'

    /**
     * A one-character run is not a word.
     *
     * Tapping punctuation must not produce a lookup for the character itself. The single exception
     * is "a" and "A", which are real words and common enough in this prose that looking them up
     * should work; anything else at length one is debris from a scan that started mid-token.
     */
    private fun isRealWord(candidate: String): Boolean =
        candidate.length > 1 || candidate.equals("a", ignoreCase = true)

    /** Walks out from [offset] in both directions and returns the run of word characters. */
    private fun run(text: String, offset: Int): String {
        var start = offset.coerceIn(0, text.length)
        var end = start + 1
        while (start > 0 && isWordChar(text[start - 1])) start--
        while (end < text.length && isWordChar(text[end])) end++
        return text.substring(start, end).trim('\'', '\u2019', '-')
    }

    /**
     * Returns the word containing [offset], or an empty string if there is none.
     *
     * Empty is a legitimate and common answer: a tap on whitespace, on a digit, or on punctuation
     * resolves to no word, and showing nothing is right. Inventing a nearby word instead would put
     * a confident wrong definition under the reader's finger.
     */
    fun wordAtOffset(text: String, offset: Int): String {
        if (text.isEmpty()) return ""

        // A tap past the end of the text is a tap on the padding below the paragraph.
        if (offset < 0 || offset >= text.length) return ""

        val candidate = run(text, offset)
        if (isRealWord(candidate)) return candidate

        // The offset was on whitespace or punctuation. Prefer the word to the right, which is
        // where a reader aiming between two words almost always means - but only if there is one,
        // and only if it is a real word.
        var scan = offset + 1
        while (scan < text.length && text[scan].isWhitespace()) scan++
        if (scan < text.length) {
            val following = run(text, scan)
            if (isRealWord(following)) return following
        }

        return ""
    }

    /** Resolves a tap at [position] against a laid-out text block. */
    fun wordAt(text: String, position: androidx.compose.ui.geometry.Offset, layout: TextLayoutResult?): String {
        val layoutResult = layout ?: return ""
        return try {
            val charOffset = layoutResult.getOffsetForPosition(position)
            wordAtOffset(text, charOffset)
        } catch (_: IllegalArgumentException) {
            // getOffsetForPosition throws when the position is outside the laid-out region. A tap
            // in the padding is not an error worth propagating.
            ""
        }
    }
}
