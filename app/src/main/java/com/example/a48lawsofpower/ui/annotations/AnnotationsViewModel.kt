package com.example.a48lawsofpower.ui.annotations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a48lawsofpower.data.HighlightDao
import com.example.a48lawsofpower.data.HighlightEntity
import com.example.a48lawsofpower.data.HighlightWithLaw
import com.example.a48lawsofpower.data.LawDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Annotations for the reader: highlighted passages and their margin notes.
 *
 * Kept out of [com.example.a48lawsofpower.ui.LawsViewModel] on purpose. That one already owns
 * the law list and is constructed for every tab; annotation state is only needed inside the
 * reader, and giving it its own lifecycle keeps the highlight query off the list screens'
 * recomposition path.
 */
class AnnotationsViewModel(
    private val highlightDao: HighlightDao,
    lawDao: LawDao,
) : ViewModel() {

    private val _activeLawId = MutableStateFlow<Int?>(null)

    /** Highlights for the law currently open in the reader; empty when none is. */
    val highlightsForActiveLaw: StateFlow<List<HighlightEntity>> =
        _activeLawId
            .flatMapLatest { id ->
                if (id == null) flowOf(emptyList()) else highlightDao.observeForLaw(id)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Every highlight in the app, labelled with its law. Backs the notes screen and export. */
    val allHighlights: StateFlow<List<HighlightWithLaw>> = highlightDao.observeAllWithLaw()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Point the reader-scoped query at [lawId]. */
    fun openLaw(lawId: Int?) {
        if (_activeLawId.value == lawId) return
        _activeLawId.value = lawId
    }

    /**
     * Highlights the selected range.
     *
     * A selection that lands entirely inside an existing highlight is treated as a request to
     * remove that highlight instead. Without that, re-selecting the same passage to change its
     * colour would stack an invisible second range on top of the first, and the reader would have
     * no way to get back to the original.
     */
    fun highlight(
        lawId: Int,
        section: String,
        start: Int,
        end: Int,
        color: HighlightColor,
        note: String = "",
    ) {
        if (end <= start) return
        viewModelScope.launch {
            // One-shot query rather than collecting the Flow: the decision has to resolve before
            // this coroutine finishes, or the toggle would insert a row and then delete a
            // different one on the next emission.
            val inside = highlightDao.findCovering(lawId, section, start, end)
            if (inside != null) {
                highlightDao.delete(inside.id)
                return@launch
            }
            highlightDao.insert(
                HighlightEntity(
                    lawId = lawId,
                    section = section,
                    start = start,
                    end = end,
                    color = color.id,
                    note = note,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    /** Creates a highlight and attaches a margin note in one step. */
    fun highlightWithNote(
        lawId: Int,
        section: String,
        start: Int,
        end: Int,
        color: HighlightColor,
        note: String,
    ) {
        if (end <= start) return
        viewModelScope.launch {
            highlightDao.insert(
                HighlightEntity(
                    lawId = lawId,
                    section = section,
                    start = start,
                    end = end,
                    color = color.id,
                    note = note.trim(),
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun setNote(id: Long, note: String) {
        viewModelScope.launch { highlightDao.setNote(id, note.trim()) }
    }

    fun setColor(id: Long, color: HighlightColor) {
        viewModelScope.launch { highlightDao.setColor(id, color.id) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { highlightDao.delete(id) }
    }
}