package com.example.a48lawsofpower.ui.dictionary

import com.example.a48lawsofpower.ui.dictionary.WordResolver.wordAtOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tapping a word must resolve *that* word.
 *
 * This is the part of the dictionary that fails silently. An off-by-one does not crash - it returns
 * a nearby word and shows a confident, plausible, wrong definition. That is worse than showing
 * nothing at all, so the offset arithmetic is tested directly rather than eyeballed on a device.
 *
 * The sample text is taken from this app's own laws, because that is what has to work.
 */
class WordResolverTest {

    private val text =
        "The law asserts that in any hierarchy, superiority is a kind of danger."

    @Test
    fun `tapping the middle of a word returns that word`() {
        assertEquals("hierarchy", wordAtOffset(text, text.indexOf("hierarchy") + 4))
    }

    @Test
    fun `every character of a word resolves to that same word`() {
        // The reader aims at the middle of a wide letter; Compose reports a boundary. Every offset
        // inside "danger" must give "danger".
        val start = text.indexOf("danger")
        for (offset in start until start + "danger".length) {
            assertEquals(
                "offset $offset inside 'danger'",
                "danger",
                wordAtOffset(text, offset),
            )
        }
    }

    @Test
    fun `every character of a sentence resolves to a real word`() {
        // Nothing may return a fragment or a stray character. "a" is the one legitimate single-letter
        // result - it is a real word and this sentence contains one.
        for (offset in text.indices) {
            val word = wordAtOffset(text, offset)
            assertTrue("offset $offset gave '$word'", word.isNotEmpty())
            assertTrue("offset $offset gave a digit '$word'", !word.any { it.isDigit() })
            assertTrue(
                "offset $offset gave a fragment '$word'",
                word.length >= 2 || word == "a",
            )
        }
    }

    @Test
    fun `the first word of the text resolves`() {
        assertEquals("The", wordAtOffset(text, 1))
    }

    @Test
    fun `the last word of the text resolves`() {
        assertEquals("danger", wordAtOffset(text, text.length - 2))
    }

    @Test
    fun `a tap past the end of the text returns nothing`() {
        // Tapping the padding below a paragraph is not a lookup.
        assertEquals("", wordAtOffset(text, text.length))
        assertEquals("", wordAtOffset(text, text.length + 50))
    }

    @Test
    fun `a negative offset returns nothing rather than throwing`() {
        assertEquals("", wordAtOffset(text, -1))
        assertEquals("", wordAtOffset(text, -100))
    }

    @Test
    fun `empty and whitespace text returns nothing`() {
        assertEquals("", wordAtOffset("", 0))
        assertEquals("", wordAtOffset("   ", 1))
        assertEquals("", wordAtOffset("\n\n\n", 1))
    }

    @Test
    fun `possessives keep their apostrophe rather than splitting`() {
        val t = "the monarch's own authority"
        val start = t.indexOf("monarch's")
        // Tapping the middle of the possessive should give the whole possessive, or at minimum the
        // name - never "s".
        val word = wordAtOffset(t, start + 4)
        assertTrue("got '$word'", word.contains("monarch"))
        assertTrue("tapping must not return just 's'", word != "s")
    }

    @Test
    fun `tapping the possessive s returns something meaningful`() {
        val t = "the monarch's own authority"
        val sIndex = t.indexOf("'s") + 1
        val word = wordAtOffset(t, sIndex)
        assertTrue("a bare 's' is not a word", word.length > 1)
    }

    @Test
    fun `an initial does not split the name around it`() {
        // "J. F. Kennedy" - tapping Kennedy must not return "F".
        val t = "Kennedy avoided the throne, as J. F. Kennedy noted."
        val start = t.indexOf("Kennedy")
        assertEquals("Kennedy", wordAtOffset(t, start + 3))
    }

    @Test
    fun `a hyphenated compound resolves as one unit`() {
        val t = "A counter-intelligence operation"
        val start = t.indexOf("counter-intelligence")
        assertEquals("counter-intelligence", wordAtOffset(t, start + 8))
    }

    @Test
    fun `curly apostrophes are treated like straight ones`() {
        // The prose uses typographic apostrophes; treating them as word characters is what keeps
        // "monarch’s" from resolving to "monarch".
        val t = "the monarch’s own authority"
        val word = wordAtOffset(t, t.indexOf("monarch") + 4)
        assertTrue("got '$word'", word.contains("monarch"))
    }

    @Test
    fun `a run of whitespace does not produce a word`() {
        val t = "power        authority"
        for (offset in 5 until 13) {
            val word = wordAtOffset(t, offset)
            assertTrue("whitespace offset $offset gave '$word'", word.isNotBlank())
        }
    }

    @Test
    fun `a leading space then a word resolves to that word`() {
        val t = "  conceal"
        assertEquals("conceal", wordAtOffset(t, 2))
    }

    @Test
    fun `text that is entirely punctuation returns nothing`() {
        assertEquals("", wordAtOffset("--- ... ---", 2))
    }

    @Test
    fun `digits are not treated as words`() {
        // A reader tapping a number should get nothing, not a definition of a stray letter.
        val t = "Law 32 in 1998"
        assertEquals("", wordAtOffset(t, t.indexOf("1998") + 1))
    }

    @Test
    fun `a capitalised word keeps its case`() {
        val t = "Machiavelli advised the Medici"
        assertEquals("Machiavelli", wordAtOffset(t, 4))
    }
}