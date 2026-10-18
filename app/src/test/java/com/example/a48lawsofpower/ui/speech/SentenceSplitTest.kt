package com.example.a48lawsofpower.ui.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The splitter is the whole of the read-along logic: the offsets it produces are what gets
 * highlighted, and Android gives no way to correct them once speech has started.
 */
class SentenceSplitTest {

    @Test
    fun `a simple sentence yields one entry`() {
        val s = splitSentences("The law asserts that power is granted.")
        assertEquals(1, s.size)
        assertEquals("The law asserts that power is granted.", s[0].text)
    }

    @Test
    fun `splits on question and exclamation marks`() {
        assertEquals(3, splitSentences("Is it so? Yes! Perhaps.").size)
    }

    @Test
    fun `ranges are contiguous so highlighting leaves no gap`() {
        val text = "First one here. Second one follows. Third closes it."
        val s = splitSentences(text)
        assertTrue("should split into three", s.size == 3)
        assertEquals(0, s.first().start)
        assertEquals(text.length, s.last().end)
        // Each sentence starts exactly where the previous ended.
        for (i in 1 until s.size) {
            assertEquals("sentence $i should abut the previous", s[i - 1].end, s[i].start)
        }
    }

    @Test
    fun `ranges index back into the source text exactly`() {
        val text = "Alpha beta gamma. Delta epsilon zeta."
        for (sentence in splitSentences(text)) {
            val slice = text.substring(sentence.start, sentence.end)
            // Trailing whitespace is trimmed off the spoken text, so compare on the trimmed form.
            assertEquals(sentence.text, slice.trim())
        }
    }

    @Test
    fun `an initial is not treated as the end of a sentence`() {
        // "I." is an initial, and the lowercase after it is the tell.
        val s = splitSentences("Ptolemy I. Soter offers the cleanest case. The rest follows.")
        assertEquals(2, s.size)
        assertTrue(s[0].text.startsWith("Ptolemy I."))
    }

    @Test
    fun `a closing quote after a terminator is absorbed`() {
        val s = splitSentences("He said \"never mind.\" Then he left the room.")
        assertEquals(2, s.size)
        assertTrue("first sentence should carry the closing quote", s[0].text.contains('"'))
    }

    @Test
    fun `an ellipsis is not three sentence ends`() {
        val s = splitSentences("She waited... and then she spoke. Nothing more.")
        assertEquals(2, s.size)
    }

    @Test
    fun `text without a terminator is one sentence`() {
        assertEquals(1, splitSentences("no terminator here").size)
    }

    @Test
    fun `blank and empty input yield nothing rather than throwing`() {
        assertTrue(splitSentences("").isEmpty())
        assertTrue(splitSentences("   ").isEmpty())
        assertTrue(splitSentences("\n\n").isEmpty())
    }

    @Test
    fun `indices are sequential from zero`() {
        val s = splitSentences("One. Two. Three. Four.")
        assertEquals(s.indices.toList(), s.map { it.index })
    }

    @Test
    fun `reassembling the ranges reproduces the whole text`() {
        val text = "Every character must survive the split. Otherwise highlighting loses prose."
        val s = splitSentences(text)
        val rebuilt = s.joinToString("") { text.substring(it.start, it.end) }
        assertEquals(text, rebuilt)
    }

    @Test
    fun `a terminator at the very end still closes the sentence`() {
        val s = splitSentences("Final words, and nothing after.")
        assertEquals(1, s.size)
        assertEquals("Final words, and nothing after.", s[0].text)
    }

    @Test
    fun `a decimal number is not a sentence boundary`() {
        // "1. " inside a version string would otherwise split mid-prose.
        val s = splitSentences("Published in 1998. Version 1.2 follows the first edition.")
        assertEquals(2, s.size)
    }
}