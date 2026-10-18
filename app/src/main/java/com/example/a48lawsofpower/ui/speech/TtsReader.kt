package com.example.a48lawsofpower.ui.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Reads a block of the reader aloud and reports which sentence is being spoken.
 *
 * ## Why one utterance per sentence
 *
 * `UtteranceProgressListener` reports an utterance id, not a character position - Android has no
 * API for "which character am I at". So read-along highlighting has to be built by speaking one
 * utterance per sentence and keeping the sentence-to-character-range mapping ourselves. That is
 * [Sentence].
 *
 * Splitting on sentence boundaries also means an interruption lands on a boundary, so stopping
 * resumes somewhere sane instead of mid-word.
 *
 * ## Offline
 *
 * `TextToSpeech` uses whatever engine the device ships with, which reads local audio - no network
 * request, and the app holds no INTERNET permission to make one with. A missing engine or missing
 * voice data surfaces as [State.Unavailable] rather than a crash: a phone without TTS is uncommon
 * but not impossible, and this is a reading app, not an audio one.
 */
class TtsReader(context: Context) {

    enum class State { Idle, Speaking, Unavailable }

    private var engine: TextToSpeech? = null
    private var ready = false

    /** Set by [play]; invoked when the last sentence finishes, so the caller clears its highlight. */
    var onFinished: () -> Unit = {}

    /**
     * Compose state, not a plain field.
     *
     * The engine drives these from an [UtteranceProgressListener] callback on a binder thread,
     * so a plain var would update the value without invalidating anything and the reader would
     * sit on a stale highlight until the next recomposition for some unrelated reason. Holding
     * them as snapshot state is what makes read-along follow the voice.
     */
    var state: State by mutableStateOf(State.Idle)
        private set

    /** Index into the spoken [sentences]; -1 when nothing is being read. */
    var currentSentence: Int by mutableIntStateOf(-1)
        private set

    private var sentences: List<Sentence> = emptyList()

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status != TextToSpeech.SUCCESS) {
                state = State.Unavailable
                return@TextToSpeech
            }
            val locale = Locale.getDefault()
            val availability = engine?.isLanguageAvailable(locale)
            if (availability == TextToSpeech.LANG_MISSING_DATA ||
                availability == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                // Present engine, no voice for this locale. Playback would be silent, so the UI
                // should hide the control rather than offer a button that does nothing.
                state = State.Unavailable
                return@TextToSpeech
            }
            ready = true
        }

        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                currentSentence = utteranceId?.toIntOrNull() ?: -1
            }

            override fun onDone(utteranceId: String?) {
                // The queue is sequential, so a done callback for anything but the last sentence
                // means more is still to come.
                val index = utteranceId?.toIntOrNull() ?: return
                if (index >= sentences.lastIndex) {
                    currentSentence = -1
                    state = State.Idle
                    onFinished()
                }
            }

            @Deprecated("Required by the interface; unused on modern APIs")
            override fun onError(utteranceId: String?) {
                currentSentence = -1
                state = State.Idle
            }
        })
    }

    /** Starts reading [text]. Replaces anything already queued. */
    fun play(text: String) {
        if (!ready || state == State.Unavailable || text.isBlank()) return

        engine?.stop()

        val split = splitSentences(text)
        if (split.isEmpty()) return
        sentences = split
        currentSentence = 0
        state = State.Speaking

        val params = android.os.Bundle()
        split.forEach { sentence ->
            engine?.speak(sentence.text, TextToSpeech.QUEUE_ADD, params, sentence.index.toString())
        }
    }

    fun stop() {
        engine?.stop()
        currentSentence = -1
        sentences = emptyList()
        state = State.Idle
    }

    /** Character range of the sentence being read, or `null` when idle. */
    fun currentRange(): IntRange? {
        val index = currentSentence
        if (index !in sentences.indices) return null
        return sentences[index].start until sentences[index].end
    }

    fun release() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        ready = false
        state = State.Idle
    }
}

/** One sentence: where it starts and ends in the source text, and the text itself. */
data class Sentence(val index: Int, val start: Int, val end: Int, val text: String)

/**
 * Splits [text] into sentences, keeping each one's character range.
 *
 * Hand-rolled rather than `BreakIterator`, which is locale-sensitive and mis-handles the
 * abbreviations and initials this prose is full of. A terminator rule is predictable for English
 * editorial text and costs nothing.
 *
 * A range runs from the sentence's first character to the start of the next sentence, so
 * highlighting one does not leave a ragged gap before the following one.
 */
fun splitSentences(text: String): List<Sentence> {
    if (text.isBlank()) return emptyList()

    val bounds = ArrayList<Int>(text.length / 2 + 1)
    var i = 0
    while (i < text.length) {
        val ch = text[i]
        if (ch == '.' || ch == '!' || ch == '?') {
            var runStart = i
            var j = i + 1
            // A run of three or more terminators is an ellipsis, not three sentence ends.
            while (j < text.length && (text[j] == '.' || text[j] == '!' || text[j] == '?')) j++
            val terminatorRun = j - runStart
            while (j < text.length && (text[j] == '"' || text[j] == '\'' || text[j] == ')' || text[j] == ']')) j++

            // The token immediately before the terminator. A lone capital letter is an initial -
            // "Ptolemy I. Soter", "J. F. Kennedy" - and the case of the following word cannot
            // reveal that, because it is also capitalised.
            val before = runStart - 1
            val isInitial = before >= 0 &&
                (before == 0 || !text[before - 1].isLetter()) &&
                text[before].isUpperCase()

            // Look past whitespace to the next real character before judging the boundary.
            var k = j
            while (k < text.length && text[k].isWhitespace()) k++
            val next = text.getOrNull(k)

            val isEllipsis = terminatorRun >= 3
            val endsSentence = !isEllipsis && !isInitial && (next == null || next.isUpperCase())

            if (endsSentence) {
                bounds += j
                i = j
                continue
            }
        }
        i++
    }
    if (bounds.lastOrNull() != text.length) bounds += text.length

    return bounds.mapIndexedNotNull { index, end ->
        val start = if (index == 0) 0 else bounds[index - 1]
        if (end <= start) return@mapIndexedNotNull null
        val slice = text.substring(start, end)
        if (slice.isBlank()) return@mapIndexedNotNull null
        Sentence(index, start, end, slice.trim())
    }
}