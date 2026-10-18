package com.example.a48lawsofpower.ui.dictionary

import android.content.Context
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

/** One dictionary entry: what a word means. */
data class Entry(
    val word: String,
    val partOfSpeech: String,
    val definition: String,
    val example: String,
)

/**
 * The bundled WordNet, held in memory as a flat map.
 *
 * ## Why the whole thing is loaded, and when
 *
 * WordNet 3.1 gzips to 4.3MB and roughly 12MB of text once expanded. A binary search over a
 * memory-mapped file would be the textbook answer, but it costs a second lookup on a background
 * thread per tap, and the asset is inside the APK so it is already paged in by the loader.
 *
 * Loading happens once, lazily, on first lookup - never at startup. Opening a law should not pay
 * for a dictionary the reader may never use, and on a low-end phone 12MB of string allocation is a
 * visible hitch. The first tap pays it instead, behind a loading state.
 *
 * ## Why a HashMap and not a trie
 *
 * A trie saves comparisons on long keys and costs a node per character. These keys average eight
 * characters, at which point the HashMap's hash-once-then-compare is cheaper and far more compact.
 */
class Dictionary private constructor(
    private val entries: Map<String, Entry>,
) {

    val size: Int get() = entries.size

    /** Words beginning with [prefix], for the search box. Capped by the caller. */
    fun suggestions(prefix: String, limit: Int = 25): List<String> {
        if (prefix.isBlank()) return emptyList()
        val folded = prefix.lowercase()
        return entries.keys
            .filter { it.startsWith(folded) }
            .sorted()
            .take(limit)
    }

    /**
     * Looks up [word], trying each stem in turn.
     *
     * The stem list is ordered by likelihood - the word itself first, then its irregular base form,
     * then suffixes stripped longest-first - so the first hit is the most likely reading. Returning
     * every candidate's entry would be worse than useless: "flies" as noun and as verb have
     * different meanings and showing both leaves the reader to guess which they tapped.
     */
    fun lookup(word: String): Entry? {
        for (candidate in Stemmer.stems(word)) {
            entries[candidate]?.let { return it }
        }
        return null
    }

    companion object {
        /**
         * Candidate asset names, in preference order.
         *
         * The Android asset merge does not preserve a `.gz` extension: it transparently
         * decompresses a gzipped asset and renames it, so a file committed as
         * `dictionary.tsv.gz` reaches the APK as `dictionary.tsv`. Verified in
         * `mergeDebugAssets` rather than assumed - the first version of this loader looked only
         * for the `.gz` name and every lookup died on FileNotFoundException.
         *
         * Both are tried so the loader works whichever way the asset is packaged.
         */
        private val ASSET_CANDIDATES = listOf("dictionary.tsv.gz", "dictionary.tsv")

        @Volatile
        private var instance: Dictionary? = null

        /** Returns the loaded dictionary, loading it on first call. */
        fun get(context: Context): Dictionary =
            instance ?: synchronized(this) {
                instance ?: load(context.applicationContext).also { instance = it }
            }

        /** True once [get] has completed, so the UI can avoid showing a spinner for a later tap. */
        fun isLoaded(): Boolean = instance != null

        private fun load(context: Context): Dictionary {
            val map = HashMap<String, Entry>(150_000)
            // gzip magic number. Read from the stream rather than trusted from the filename,
            // because the asset merge rewrites the extension.
            val opened = try {
                context.assets.open(ASSET_CANDIDATES.first())
            } catch (_: Exception) {
                try {
                    context.assets.open(ASSET_CANDIDATES[1])
                } catch (e: Exception) {
                    Log.w("Dictionary", "bundled dictionary missing from assets", e)
                    return Dictionary(emptyMap())
                }
            }

            try {
                // Sniff the gzip magic number from the stream itself rather than inferring
                // compression from the filename, which the asset merge rewrites.
                val buffered = BufferedInputStream(opened, 1 shl 16)
                buffered.mark(2)
                val magic = buffered.read()
                val second = buffered.read()
                buffered.reset()

                val decompressed =
                    if (magic == 0x1f && second == 0x8b) GZIPInputStream(buffered) else buffered

                decompressed.use { stream ->
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8), 1 shl 16).use { reader ->
                        reader.lineSequence().forEach { line ->
                            when {
                                // Licence lines are metadata, not entries. Skipping rather than
                                // failing keeps the asset self-describing without the parser knowing
                                // the licence exists.
                                line.startsWith("#LICENCE") -> Unit

                                line.isEmpty() -> Unit

                                else -> {
                                    val parts = line.split('\t')
                                    if (parts.size >= 3) {
                                        val key = parts[0]
                                        map[key] = Entry(
                                            word = key,
                                            partOfSpeech = parts[1],
                                            definition = parts[2],
                                            example = if (parts.size > 3) parts[3] else "",
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // A missing or corrupt asset must not take the reader down with it.
                //
                // This fired for real: a stale build packaged an uncompressed dictionary.tsv while
                // the loader looked for dictionary.tsv.gz, and the resulting FileNotFoundException
                // propagated out of a pointer-input callback and crashed the app. Losing the
                // dictionary is a downgrade; losing the ability to read the laws is not acceptable.
                // An empty dictionary makes every lookup report "not found", which is visible and
                // survivable.
                Log.w("Dictionary", "bundled dictionary unavailable; lookups will miss", e)
                return Dictionary(emptyMap())
            }
            return Dictionary(map)
        }
    }
}