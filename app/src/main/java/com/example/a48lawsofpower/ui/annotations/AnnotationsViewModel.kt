package com.example.a48lawsofpower.ui.annotations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a48lawsofpower.data.AnnotationDao
import com.example.a48lawsofpower.data.AnnotationFilter
import com.example.a48lawsofpower.data.AnnotationType
import com.example.a48lawsofpower.data.AnnotationWithLaw
import com.example.a48lawsofpower.data.AnnotationEntity
import com.example.a48lawsofpower.data.LawDao
import com.example.a48lawsofpower.data.SectionKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The reader's marks.
 *
 * Kept out of [com.example.a48lawsofpower.ui.LawsViewModel] on purpose: that one is
 * constructed for every tab, and annotation state is only needed inside the reader and the
 * review screen.
 *
 * **Undo is one level deep, and deliberately so.** Deleting a note is the only destructive action
 * a reader can take here, and it is reversible by holding the deletion rather than hunting for
 * it. A full undo stack would need somewhere to put the history; an in-memory list of the last
 * few deleted rows is enough for the one gesture that needs it, and it costs a field.
 */
class AnnotationsViewModel(
    private val annotationDao: AnnotationDao,
    private val lawDao: LawDao,
) : ViewModel() {

    private val _activeLawId = MutableStateFlow<Int?>(null)

    /** Marks on the law currently open in the reader. */
    val annotationsForActiveLaw: StateFlow<List<AnnotationEntity>> =
        _activeLawId
            .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else annotationDao.observeForLaw(id) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _filter = MutableStateFlow(AnnotationFilter())
    val filter: StateFlow<AnnotationFilter> = _filter.asStateFlow()

    /** Every mark, filtered, newest first. Backs the review screen and export. */
    val review: StateFlow<List<AnnotationWithLaw>> =
        _filter
            .flatMapLatest { f ->
                annotationDao.observeFiltered(
                    type = f.type.takeIf { it != AnnotationType.MIXED },
                    color = f.color.takeIf { it.isNotBlank() },
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The most recent deletion, so the UI can offer a single-step undo. */
    private val _lastDeleted = MutableStateFlow<AnnotationEntity?>(null)
    val lastDeleted: StateFlow<AnnotationEntity?> = _lastDeleted.asStateFlow()

    fun openLaw(lawId: Int?) {
        if (_activeLawId.value == lawId) return
        _activeLawId.value = lawId
    }

    fun setFilter(filter: AnnotationFilter) {
        _filter.value = filter
    }

    /**
     * Creates a highlight over `[start, end)`.
     *
     * Re-selecting a passage that is *already* a highlight removes it, because the alternative is
     * an invisible second range stacked on the first with no way back to the original. Only a
     * covering highlight is considered; a partial overlap is ambiguous and is left alone.
     *
     * The covered text is captured here rather than derived later. It is the only thing that can
     * both explain the mark in a list and re-find it after a re-encode.
     */
    fun highlight(
        lawId: Int,
        section: String,
        start: Int,
        end: Int,
        text: String,
        color: HighlightColor,
    ) {
        if (end <= start) return
        viewModelScope.launch {
            val covering = annotationDao.findCoveringHighlight(lawId, section, start, end)
            if (covering != null) {
                softDelete(covering)
                return@launch
            }
            annotationDao.insert(
                AnnotationEntity(
                    lawId = lawId,
                    section = section,
                    start = start,
                    end = end,
                    anchorText = text.substring(start.coerceIn(0, text.length), end.coerceIn(0, text.length)),
                    type = AnnotationType.Highlight.wire,
                    color = color.id,
                    note = "",
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    /**
     * Attaches a note to an existing highlight.
     *
     * A note with no highlight still needs to exist somewhere, so one is created covering the same
     * range first. That keeps "delete the highlight, keep the note" a decision the reader can make
     * rather than a data-loss surprise.
     */
    fun addNote(lawId: Int, section: String, start: Int, end: Int, text: String, note: String) {
        viewModelScope.launch {
            val existing = annotationDao.findCoveringHighlight(lawId, section, start, end)
            if (existing != null) {
                annotationDao.setNote(
                    existing.id,
                    existing.note.ifBlank { note.trim() },
                    System.currentTimeMillis(),
                )
                return@launch
            }
            annotationDao.insert(
                AnnotationEntity(
                    lawId = lawId,
                    section = section,
                    start = start,
                    end = end,
                    anchorText = text.substring(start.coerceIn(0, text.length), end.coerceIn(0, text.length)),
                    type = AnnotationType.Note.wire,
                    color = "",
                    note = note.trim(),
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    /**
     * Toggles a bookmark on a law.
     *
     * One bookmark per law rather than per section: the law is the thing the reader is
     * navigating, and a list of "three bookmarks in law 12" is noise.
     */
    fun toggleBookmark(lawId: Int, section: String = SectionKey.CORE) {
        viewModelScope.launch {
            val existing = annotationDao.findBookmark(lawId, section)
            if (existing != null) {
                softDelete(existing)
            } else {
                annotationDao.insert(
                    AnnotationEntity(
                        lawId = lawId,
                        section = section,
                        start = 0,
                        end = 0,
                        anchorText = "",
                        type = AnnotationType.Bookmark.wire,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
    }

    /**
     * Deletes a mark, keeping it recoverable for one step.
     *
     * A highlight that carries a note is *not* cascaded away silently - the note is what the
     * reader typed, and losing it to a mis-tap on the highlight is the worst outcome this screen
     * can produce. It is rewritten as a note-only mark at the same range first.
     */
    private suspend fun softDelete(annotation: AnnotationEntity) {
        if (annotation.note.isNotBlank() && annotation.type == AnnotationType.Highlight.wire) {
            annotationDao.insert(
                annotation.copy(
                    id = 0,
                    type = AnnotationType.Note.wire,
                    color = "",
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
        annotationDao.delete(annotation.id)
        _lastDeleted.value = annotation
    }

    /** Restores the most recent deletion. No-op when there is nothing to restore. */
    fun undoDelete() {
        val deleted = _lastDeleted.value ?: return
        viewModelScope.launch {
            annotationDao.insert(deleted)
            _lastDeleted.value = null
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            annotationDao.getById(id)?.let { softDelete(it) }
        }
    }

    fun setColor(id: Long, color: HighlightColor) {
        viewModelScope.launch { annotationDao.setColor(id, color.id, System.currentTimeMillis()) }
    }

    fun editNote(id: Long, note: String) {
        viewModelScope.launch { annotationDao.setNote(id, note, System.currentTimeMillis()) }
    }

    /**
     * Repairs marks whose anchor text was never recorded.
     *
     * Rows carried over from schema 5 have offsets but no anchor, and SQL could not derive one
     * during the migration. Deriving it here - where the law's prose is in hand - fixes those
     * rows and any anchor lost another way, and only ever writes where the anchor is empty so a
     * real anchor is never overwritten with a guess.
     */
    fun backfillAnchors(lawId: Int, section: String, text: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val rows = annotationDao.observeForLawOnce(lawId, section)
                for (row in rows) {
                    if (row.anchorText.isNotEmpty()) continue
                    val start = row.start.coerceIn(0, text.length)
                    val end = row.end.coerceIn(start, text.length)
                    if (end > start) annotationDao.backfillAnchorText(row.id, text.substring(start, end))
                }
            }
        }
    }
}