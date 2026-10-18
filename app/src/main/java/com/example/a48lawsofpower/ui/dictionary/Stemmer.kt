package com.example.a48lawsofpower.ui.dictionary

/**
 * Reduces an inflected English word to the form WordNet actually stores.
 *
 * ## Why this has to exist
 *
 * WordNet's data files contain only lemmas: "ask", "allow", "appear". The inflected forms a reader
 * meets in prose - "asked", "allows", "appeared" - are resolved by a stemmer at query time, and
 * Princeton's `morphy` is not something to bundle. Without this, looking up any past tense in the
 * laws returns nothing, which is most of the book's verbs.
 *
 * The obvious alternative - expanding every inflection at build time - multiplies the asset by
 * roughly six for no benefit. A lookup-time stemmer costs a few string operations and keeps the
 * dictionary at one entry per lemma.
 *
 * ## Why it is a hand-rolled stripper and not Porter
 *
 * Porter's algorithm is a research artifact with published accuracy figures; this is neither, and
 * pretending otherwise would be the problem. What this needs to do is much narrower: recognise the
 * handful of suffixes that actually occur in English prose and strip the longest one that leaves a
 * plausible stem. Every rule below is here because a word in this app's text needed it, and the
 * test suite names those words.
 *
 * That narrower aim is also why it is safe to be conservative. A word that fails to stem is a miss
 * the reader retries with the base form; a word over-stripped ("caresses" -> "caress" -> "car")
 * returns a confidently wrong definition, which is worse than no answer.
 */
object Stemmer {

    /** Below this a stripped stem is not worth returning - "a" minus nothing is not a word. */
    private const val MIN_STEM_LENGTH = 3

    /**
     * Suffix -> the letters added when the base form is inflected.
     *
     * Read as: to turn the stem back into the inflected form, append [additions]. Used only for
     * irregular verbs and nouns, where a plain suffix strip cannot work.
     */
    private val IRREGULAR: Map<String, String> = mapOf(
        // The ones this text actually uses. -ed past tenses of "be" are by far the most frequent
        // irregular in ordinary prose and are individually worth listing.
        "was" to "be", "were" to "be", "been" to "be", "am" to "be", "is" to "be", "are" to "be",
        "had" to "have", "has" to "have", "having" to "have",
        "did" to "do", "does" to "do", "done" to "do", "doing" to "do",
        "said" to "say",
        "went" to "go", "gone" to "go", "goes" to "go", "going" to "go",
        "made" to "make", "making" to "make",
        "took" to "take", "taken" to "take", "takes" to "take", "taking" to "take",
        "came" to "come", "coming" to "come", "comes" to "come",
        "saw" to "see", "seen" to "see", "seeing" to "see", "sees" to "see",
        "knew" to "know", "known" to "know",
        "got" to "get", "gotten" to "get",
        "gave" to "give", "given" to "give",
        "found" to "find", "finding" to "find",
        "thought" to "think",
        "told" to "tell",
        "became" to "become",
        "left" to "leave",
        "felt" to "feel",
        "brought" to "bring",
        "began" to "begin", "begun" to "begin",
        "kept" to "keep",
        "held" to "hold",
        "wrote" to "write", "written" to "write",
        "stood" to "stand",
        "heard" to "hear",
        "meant" to "mean",
        "met" to "meet",
        "ran" to "run", "running" to "run",
        "paid" to "pay",
        "sat" to "sit",
        "spoke" to "speak", "spoken" to "speak",
        "lay" to "lie", "lain" to "lie",
        "led" to "lead",
        "grew" to "grow", "grown" to "grow",
        "lost" to "lose",
        "fell" to "fall", "fallen" to "fall",
        "sent" to "send",
        "built" to "build",
        "understood" to "understand",
        "drew" to "draw", "drawn" to "draw",
        "broke" to "break", "broken" to "break",
        "spent" to "spend",
        "cut" to "cut",
        "rose" to "rise", "risen" to "rise",
        "driven" to "drive", "drove" to "drive",
        "bought" to "buy",
        "wore" to "wear", "worn" to "wear",
        "ate" to "eat", "eaten" to "eat",
        "sang" to "sing", "sung" to "sing",
        "swam" to "swim",
        "threw" to "throw", "thrown" to "throw",
        "flew" to "fly", "flown" to "fly",
        "chose" to "choose", "chosen" to "choose",
        "shook" to "shake", "shaken" to "shake",
        "rode" to "ride", "ridden" to "ride",
        "struck" to "strike",
        "swore" to "swear", "sworn" to "swear",
        "wore" to "wear",
        "hid" to "hide", "hidden" to "hide",
        "bit" to "bite", "bitten" to "bite",
        "blew" to "blow", "blown" to "blow",
        "hung" to "hang",
        "stuck" to "stick",
        "sought" to "seek",
        "taught" to "teach",
        "fought" to "fight",
        "sold" to "sell",
        "told" to "tell",
        "tore" to "tear", "torn" to "tear",
        "bound" to "bind",
    )

    /**
     * Regular suffixes, longest first.
     *
     * Longest-first is the whole point: "ated" must be tried before "ed", or "created" reduces to
     * "creat" and returns a definition of the wrong word entirely.
     */
    private val SUFFIXES: List<String> = listOf(
        "ational", "fulness", "ousness", "iveness", "tional", "biliti", "lessli",
        "ication", "ization", "ation", "ition", "ement", "ances", "ences",
        "ances", "ogies", "ously", "ingly", "ement", "ement",
        "ically", "ability", "ibility", "ousness", "iveness",
        "ement", "ments", "ness", "ment", "tion", "sion", "ing", "ers", "est", "ies",
        "ied", "ed", "es", "er", "ly", "s",
    ).distinct().sortedByDescending { it.length }

    /** Suffixes that only strip cleanly off a verb, not a noun. */
    private val VERB_ONLY = setOf("ed", "ing")

    /**
     * Returns candidate stems for [word], most likely first. The word itself is always first.
     *
     * Returning a *list* rather than one stem is deliberate. "flies" could be the verb "fly" or the
     * noun "fly", and the dictionary knows which sense belongs to which; trying each in turn and
     * taking the first that exists gets it right without a part-of-speech tag the tap has no way to
     * supply.
     */
    fun stems(word: String): List<String> {
        // The literal word, in its original case, is always offered first. A reader who taps a
        // capitalised name must get a result that matches what they tapped - folding to lowercase
        // here loses the name they actually selected.
        val out = LinkedHashSet<String>()
        out += word

        val lower = word.lowercase()
        out += lower

        IRREGULAR[lower]?.let { out += it }

        for (suffix in SUFFIXES) {
            if (!lower.endsWith(suffix)) continue
            val stem = lower.dropLast(suffix.length)

            // Restore the dropped 'e' where English does. "moved" -> "mov" is wrong; "move" is
            // right. Only for the silent-e endings, hence the narrow check.
            val restored = when {
                suffix == "ed" && stem.endsWith("v") -> stem + "e"
                suffix == "ed" && stem.endsWith("c") -> stem + "k"
                suffix == "ing" && stem.length >= 4 && stem.last() !in "aeiou" -> stem + "e"
                else -> stem
            }

            if (restored.length >= MIN_STEM_LENGTH) out += restored
            if (stem.length >= MIN_STEM_LENGTH) out += stem

            // "-ied" is the past tense of a "-y" verb and the plural of a "-y" noun: "carried"
            // -> "carry", "allied" -> "ally". Dropping "ied" from "carried" leaves "carr", so
            // the "y" is appended to the stem as it stands - not with a character removed, which
            // would give "cary".
            if (suffix == "ied" && stem.length >= MIN_STEM_LENGTH) out += stem + "y"

            // "-ies" is the same rule for plurals and third-person: "flies" -> "fly".
            if (suffix == "ies" && stem.length >= MIN_STEM_LENGTH) out += stem + "y"

            // Silent-e restoration is *offered*, not decided.
            //
            // "liked" is "like", but "asked" is "ask", and both end "ed" with a bare
            // consonant-vowel-consonant stem. There is no rule that separates them - the
            // dictionary itself is the only authority. So both candidates are proposed and the
            // lookup keeps whichever the dictionary actually contains. Guessing here is what
            // would return a confidently wrong definition of the wrong word.
            if (suffix == "ed" && stem.length >= 2 &&
                stem[stem.length - 1] !in "aeiou" && stem[stem.length - 2] in "aeiou"
            ) {
                val withE = stem + "e"
                if (withE.length >= MIN_STEM_LENGTH) out += withE
            }

            // Doubled consonant: "running" -> "run". Only after the plain strip has been offered,
            // so "stopping" still also matches "stop".
            if (stem.length >= 3 && stem.last() == stem[stem.length - 2] && stem.last() !in "aeiouls") {
                out += stem.dropLast(1)
            }
        }

        // "ies" also strips to a bare "i"-less stem above; add the "-y" reading of any -es ending.
        if (lower.endsWith("es") && lower.length > 3) out += lower.dropLast(2)
        if (lower.endsWith("s") && !lower.endsWith("ss") && lower.length > 3) {
            out += lower.dropLast(1)
        }
        if (lower.endsWith("'s") || lower.endsWith("s'")) out += lower.dropLast(2)

        return out.toList()
    }

    /** True if [suffix] should only be stripped when the word is being read as a verb. */
    fun isVerbOnly(suffix: String): Boolean = suffix in VERB_ONLY
}