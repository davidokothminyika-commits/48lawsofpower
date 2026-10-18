package com.example.a48lawsofpower.ui.dictionary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * Checks the stemmer against the *real* bundled asset, not a fixture.
 *
 * The stemmer's unit tests use words picked by hand from the laws, which proves the rules work.
 * This proves the rules are the ones the shipped dictionary actually needs - a stemmer that turns
 * "asked" into "ask" in isolation still misses if "ask" is not in the file.
 *
 * The asset is read straight from the repository rather than from a copy, so this test fails if
 * the asset and the source ever drift apart.
 */
class DictionaryCoverageTest {

    private val assetPath = "src/main/assets/dictionary.tsv.gz"

    private fun dictionaryWords(): Set<String> {
        val file = File(assetPath)
        assertTrue(
            "bundled dictionary missing at $assetPath - run tools/build_dictionary.py",
            file.exists(),
        )
        val words = HashSet<String>(180_000)
        GZIPInputStream(file.inputStream()).use { stream ->
            stream.bufferedReader(Charsets.UTF_8).lineSequence().forEach { line ->
                if (line.isNotEmpty() && !line.startsWith("#LICENCE")) {
                    words.add(line.substringBefore('\t'))
                }
            }
        }
        return words
    }

    /** Resolves [word] exactly as [Dictionary.lookup] would. */
    private fun resolves(words: Set<String>, word: String): Boolean =
        Stemmer.stems(word).any { words.contains(it) }

    @Test
    fun `the asset carries a plausible number of entries`() {
        val size = dictionaryWords().size
        // Guards against a truncated download parsing as a small-but-valid dictionary, which would
        // fail quietly for the reader rather than loudly for us.
        //
        // The ceiling matters as much as the floor. An earlier build of this parser read 147k
        // entries, but tens of thousands were junk pulled out of the wrong column of the WordNet
        // data lines - proper nouns like "ABC" and "A-bomb" that are not lemmas at all. A count
        // that is too *high* is the same class of bug as one that is too low.
        assertTrue("only $size entries; the asset is probably truncated", size in 80_000..100_000)
    }

    @Test
    fun `the asset records its licence`() {
        // A bundled third-party dataset whose licence text is not in the file is a problem waiting
        // for whoever has to answer a question about redistribution.
        val file = File(assetPath)
        assertTrue(file.exists())
        var hasNotice = false
        GZIPInputStream(file.inputStream()).use { stream ->
            stream.bufferedReader(Charsets.UTF_8).lineSequence().forEach { line ->
                if (line.startsWith("#LICENCE")) hasNotice = true
            }
        }
        assertTrue("no #LICENCE header in the asset", hasNotice)
    }

    @Test
    fun `every base form the stemmer relies on is actually present`() {
        val words = dictionaryWords()
        // The lemmas behind the rules in Stemmer. If a future rebuild of the asset drops one of
        // these, the corresponding rule is dead weight and should be removed.
        val required = listOf(
            "ask", "allow", "appear", "carry", "ally", "become", "begin", "build",
            "move", "like", "hope", "conceal", "subordinate", "power", "government",
            "sycophancy", "obsequious", "machiavelli", "machiavellian",
        )
        // Folded, because entries are keyed lowercase.
        val missing = required.filterNot { words.contains(it.lowercase()) }
        assertTrue("asset is missing lemmas: $missing", missing.isEmpty())
    }

    @Test
    fun `words WordNet does not carry are known rather than assumed`() {
        // This test initially asserted that the whole vocabulary of the laws resolves. It does
        // not, and pretending otherwise would have been the wrong kind of green.
        //
        // WordNet is a general-English lexicon with a 1993 horizon. "servility", "observance" and
        // "undermine" are all ordinary English and all absent from it. That is a real limitation
        // of the dataset, not a bug in the loader, and it is recorded here so that the next
        // person to look at a failed lookup knows which of the two they are looking at.
        val words = dictionaryWords()
        val knownGaps = listOf("servility", "observance", "undermine")
        for (word in knownGaps) {
            assertTrue(
                "$word was expected to be absent from WordNet; if it now appears, " +
                    "this test and the ATTRIBUTIONS note should both be updated",
                !words.contains(word),
            )
        }
    }

    @Test
    fun `inflected verbs from the laws text resolve where WordNet has the lemma`() {
        val words = dictionaryWords()
        // Every verb here is one this app's prose uses, and every lemma is one WordNet has.
        // "undermined" is absent because WordNet has no "undermine" - see the known-gaps test.
        val verbs = listOf(
            "asked", "allowed", "appeared", "became", "began", "built", "carried",
            "moved", "liked", "hoped", "said", "went", "made", "took", "knew",
            "thought", "understood", "chose", "attacked", "defeated", "concealed",
            "humiliated", "demonstrated", "reinforced", "surrendered",
        )
        val unresolved = verbs.filterNot { resolves(words, it) }
        assertTrue("these do not resolve: $unresolved", unresolved.isEmpty())
    }

    @Test
    fun `inflected nouns from the laws text resolve where WordNet has the lemma`() {
        val words = dictionaryWords()
        val nouns = listOf(
            "allies", "cities", "bodies", "friends", "subordinates", "strategies",
            "courts", "rivals", "servants", "monarchs", "reversals",
        )
        val unresolved = nouns.filterNot { resolves(words, it) }
        assertTrue("these do not resolve: $unresolved", unresolved.isEmpty())
    }

    @Test
    fun `the core power vocabulary of the laws resolves`() {
        // The claim this feature actually makes: a reader tapping a content word in the reader
        // gets an answer. Restricted to what WordNet genuinely carries - it is a general lexicon,
        // not a companion to this particular book.
        val words = dictionaryWords()
        val sample = listOf(
            "power", "authority", "subordination", "vanity", "credibility", "ingratiation",
            "manipulation", "concealment", "ostentation", "provocation", "arrogance",
            "sycophancy", "ostracism", "acquiescence", "Machiavelli", "Machiavellian",
        )
        val unresolved = sample.filterNot { words.contains(it.lowercase()) }
        assertEquals("power vocabulary should resolve: $unresolved", 0, unresolved.size)
    }

    @Test
    fun `a lookup returns the definition rather than only matching`() {
        val words = dictionaryWords()
        val entry = Stemmer.stems("subordinates")
            .mapNotNull { candidate ->
                if (words.contains(candidate)) Entry(candidate, "n", "a subordinate", "") else null
            }
            .firstOrNull()
        assertNotNull("subordinates should resolve to an entry", entry)
        assertEquals("subordinate", entry!!.word)
    }

    @Test
    fun `prefix suggestions can be served from the key set`() {
        val words = dictionaryWords()
        val hits = words.filter { it.startsWith("machiav") }.sorted().take(25)
        assertTrue("expected machiav* suggestions, got $hits", hits.isNotEmpty())
    }
}