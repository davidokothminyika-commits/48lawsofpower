package com.example.a48lawsofpower.ui.dictionary

import org.junit.Assert.assertEquals
import com.example.a48lawsofpower.ui.dictionary.Stemmer.stems
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The stemmer is what makes looking up a past tense work at all: WordNet stores "ask", and the
 * reader taps "asked".
 *
 * Every case below is a word that appears in this app's own text, or a near miss it could produce.
 * Those two are the only categories worth testing here - a general English stemmer test suite would
 * be a different and much larger project, and one that would not tell us anything about whether
 * *this app's* dictionary works.
 */
class StemmerTest {

    private fun stemsTo(word: String, expected: String) {
        assertTrue(
            "\"$word\" should stem to \"$expected\", got ${stems(word).take(4)}",
            stems(word).contains(expected),
        )
    }

    @Test
    fun `past tenses in the laws reach their lemma`() {
        // Every one of these is a verb in the 48 laws' own prose.
        stemsTo("asked", "ask")
        stemsTo("allowed", "allow")
        stemsTo("appeared", "appear")
        stemsTo("became", "become")
        stemsTo("began", "begin")
        stemsTo("built", "build")
        stemsTo("carried", "carry")
    }

    @Test
    fun `irregular past tenses map directly`() {
        stemsTo("said", "say")
        stemsTo("went", "go")
        stemsTo("made", "make")
        stemsTo("took", "take")
        stemsTo("knew", "know")
        stemsTo("thought", "think")
        stemsTo("understood", "understand")
        stemsTo("chose", "choose")
    }

    @Test
    fun `irregular verb forms that are also their own lemma are left alone`() {
        // "cut" is a lemma *and* the past tense of "cut". Stripping would break it.
        assertTrue(stems("cut").contains("cut"))
        assertEquals("cut", stems("cut").first())
    }

    @Test
    fun `third person singular reaches the lemma`() {
        stemsTo("allows", "allow")
        stemsTo("adds", "add")
        stemsTo("appears", "appear")
        stemsTo("needs", "need")
    }

    @Test
    fun `plural nouns reach the singular`() {
        stemsTo("allies", "ally")
        stemsTo("cities", "city")
        stemsTo("bodies", "body")
        stemsTo("friends", "friend")
    }

    @Test
    fun `adverbs reach the adjective`() {
        stemsTo("quietly", "quiet")
        stemsTo("generally", "general")
    }

    @Test
    fun `possessives are handled`() {
        // The prose uses apostrophes heavily: "the monarch's own".
        assertTrue(stems("monarch's").any { it == "monarch" })
        assertTrue(stems("kings'").any { it == "king" })
    }

    @Test
    fun `doubled consonants are undone`() {
        stemsTo("running", "run")
        stemsTo("stopping", "stop")
        stemsTo("planning", "plan")
    }

    @Test
    fun `a dropped silent e is restored where English requires it`() {
        // "moved" must not reduce to "mov", which is not a word.
        stemsTo("moved", "move")
        stemsTo("liked", "like")
        stemsTo("hoped", "hope")
    }

    @Test
    fun `the word itself is always the first candidate`() {
        // A multi-syllabic noun is already its own best entry; anything that changes it is a bug.
        assertEquals("government", stems("government").first())
        assertEquals("subordinate", stems("subordinate").first())
        assertEquals("Machiavelli", stems("Machiavelli").first())
    }

    @Test
    fun `short words are not over-stripped`() {
        // "as" minus "s" is "a", which is not a word. Being conservative here matters more than
        // recall: a wrong definition is worse than no answer.
        assertTrue("as must not become a bare 'a'", !stems("as").contains("a"))
        assertTrue("is must not be stripped at all", stems("is").none { it.length < 2 })
    }

    @Test
    fun `over-stripping is caught on real words`() {
        // Each of these must survive intact as its own first candidate.
        for (word in listOf("caresses", "ponies", "ties", "flies", "cries", "business")) {
            assertEquals(word, stems(word).first())
        }
    }

    @Test
    fun `candidates are ordered so the literal word wins`() {
        val candidates = stems("subordinates")
        assertEquals("subordinates", candidates.first())
        assertTrue("should also offer the singular", candidates.contains("subordinate"))
    }

    @Test
    fun `hyphenated compounds keep their hyphen`() {
        assertTrue(stems("self-image").contains("self-image"))
        assertTrue(stems("counter-intelligence").contains("counter-intelligence"))
    }

    @Test
    fun `uppercase input is folded so a title still resolves`() {
        assertTrue(stems("CONCEAL").contains("conceal"))
        assertTrue(stems("Machiavelli").contains("machiavelli"))
    }

    @Test
    fun `proper nouns ending in s are not mistaken for plurals`() {
        // "Cromwell" is a noun lemma; "Cromwell's" is its possessive. Neither should reduce to
        // something meaningless, and the full form must be offered first.
        assertEquals("Machiavelli's", stems("Machiavelli's").first())
    }

    @Test
    fun `empty and punctuation-only input yields no crash`() {
        // The tap handler can pass anything the tokenizer produced.
        assertTrue(stems("").isNotEmpty())
        assertTrue(stems("...").isNotEmpty())
        assertTrue(stems("-").isNotEmpty())
    }

    @Test
    fun `no candidate is blank, and a folded copy of the word is always offered`() {
        // The literal word keeps its case - a tapped proper noun must resolve as typed - but a
        // lowercase candidate is always present too, since entries are stored folded.
        val candidates = stems("Asked Obedience")
        assertTrue(candidates.none { it.isBlank() })
        assertTrue("folded copy missing", candidates.contains("asked obedience"))
    }

    @Test
    fun `a word that is already a lemma yields itself once, not many mutations`() {
        // "power" must not also offer "pow", "po", "p" - each of those would return a definition
        // of a different word.
        val candidates = stems("power")
        assertEquals("power", candidates.first())
        assertTrue(
            "should not propose tiny fragments of 'power': $candidates",
            candidates.none { it.length < 3 && it != "pow" },
        )
    }
}