package com.example.a48lawsofpower.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * What a mark *is*.
 *
 * One table rather than three. A bookmark is not a highlight with an empty colour; it is a point
 * of interest, and the review screen needs "everything I marked in this law, newest first" as a
 * single query. Splitting by type means three unions in SQL and three places to get the filters
 * right; a discriminator column keeps one query and one filter vocabulary.
 *
 * The order matters at insert time and nowhere else. [MIXED] is never stored.
 */
enum class AnnotationType(val wire: String) {
    Highlight("highlight"),
    Note("note"),
    Bookmark("bookmark"),
    ;

    companion object {
        val MIXED = "mixed"

        fun fromWire(value: String?): AnnotationType? =
            entries.firstOrNull { it.wire == value }
    }
}

/**
 * A reader's mark on one law.
 *
 * ## How a mark is re-found
 *
 * Nothing here is a screen coordinate. A coordinate dies the moment the reader changes font size,
 * rotates, or opens the app on a different phone, so it is never stored.
 *
 * Identity is the triple [section] + [start]/[end] into that section's text, and that is
 * reinforced by [anchorText] - the exact characters the mark covered, copied at creation time.
 * Offsets alone are enough to paint a highlight but not enough to *explain* it: a stored range of
 * 412-489 is meaningless in a notes list, and it silently points at the wrong words if the prose
 * is ever edited. [anchorText] is what gets shown in the review screen and what gets exported.
 *
 * If the underlying text changes, [resolve] degrades rather than lying: see there for the rules.
 */
@Entity(
    tableName = "annotations",
    foreignKeys = [
        ForeignKey(
            entity = LawEntity::class,
            parentColumns = ["id"],
            childColumns = ["law_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("law_id"), Index("type"), Index("created_at")],
)
data class AnnotationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "law_id")
    val lawId: Int,

    /** Which block of the law this sits in. See [SectionKey]. */
    val section: String,

    /** Character offsets into [SectionKey.textOf] for this law. Never screen coordinates. */
    val start: Int,

    val end: Int,

    /**
     * The exact text covered, stored verbatim at creation.
     *
     * Doubles as the reflow safety net and as the human-readable label in the review list.
     */
    @ColumnInfo(name = "anchor_text")
    val anchorText: String,

    @ColumnInfo(name = "type")
    val type: String = AnnotationType.Highlight.wire,

    /** Stable colour name, or empty for a bookmark. See the ui.annotations palette. */
    val color: String = "",

    @ColumnInfo(defaultValue = "''")
    val note: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = 0,

    @ColumnInfo(name = "updated_at", defaultValue = "0")
    val updatedAt: Long = 0,
)

/**
 * The text blocks a mark can point into.
 *
 * A string rather than an ordinal: the reader's section layout is UI and may be reordered, while
 * stored marks are not. Names survive that.
 */
object SectionKey {
    const val CORE = "core"
    const val SUMMARY = "summary"
    const val OBSERVANCE = "observance"
    const val TRANSGRESSION = "transgression"
    const val REVERSAL = "reversal"

    fun textOf(law: LawEntity, section: String): String = when (section) {
        CORE -> law.core
        SUMMARY -> law.summary
        OBSERVANCE -> law.observance
        TRANSGRESSION -> law.transgression
        REVERSAL -> law.reversal
        else -> ""
    }
}

/** Where an [AnnotationEntity] ended up after being re-fitted to the current text. */
enum class AnchorFit {
    /** Offsets still address the same characters. */
    Exact,

    /** Offsets were clamped or nudged, but still land inside the section. */
    Adjusted,

    /** No usable overlap left. The mark is kept but cannot be painted. */
    Orphaned,
}

/**
 * A mark resolved against the text as it is *today*.
 *
 * @property quoted the characters actually covered now, which is what should be shown and
 *   exported rather than the stored [AnnotationEntity.anchorText].
 */
data class ResolvedAnnotation(
    val annotation: AnnotationEntity,
    val start: Int,
    val end: Int,
    val quoted: String,
    val fit: AnchorFit,
) {
    val isPaintable: Boolean get() = fit != AnchorFit.Orphaned
}

/**
 * Re-fits a stored mark to the current text.
 *
 * Offsets are the primary anchor and the copied [AnnotationEntity.anchorText] is the check, which
 * is what makes this survive a re-encode: if the prose is unchanged this is [AnchorFit.Exact]; if
 * the prose grew or shrank around the mark, the range is clamped to something real and reported
 * as [AnchorFit.Adjusted]; only a mark with no remaining overlap becomes [AnchorFit.Orphaned],
 * and it is kept rather than deleted so a reader's work is never silently discarded.
 */
fun AnnotationEntity.resolve(text: String): ResolvedAnnotation {
    if (text.isEmpty()) {
        return ResolvedAnnotation(this, 0, 0, "", AnchorFit.Orphaned)
    }
    val start = start.coerceIn(0, text.length)
    val end = end.coerceIn(start, text.length)
    if (end <= start) {
        return ResolvedAnnotation(this, start, start, "", AnchorFit.Orphaned)
    }

    // The cheap case: the stored text still sits exactly where it was recorded.
    if (start + anchorText.length == end &&
        anchorText.isNotEmpty() &&
        text.regionMatches(start, anchorText, 0, anchorText.length)
    ) {
        return ResolvedAnnotation(this, start, end, anchorText, AnchorFit.Exact)
    }

    // The prose moved under the mark. Look for the anchor text nearby before giving up, so a
    // re-encode that shifted everything by a paragraph does not orphan every highlight in the app.
    val found = anchorText.takeIf { it.length >= MIN_ANCHOR_SEARCH }
        ?.let { needle -> text.indexOf(needle) }
        ?.takeIf { it >= 0 && it + anchorText.length <= text.length }
    if (found != null) {
        return ResolvedAnnotation(this, found, found + anchorText.length, anchorText, AnchorFit.Adjusted)
    }

    return ResolvedAnnotation(this, start, end, text.substring(start, end), AnchorFit.Adjusted)
}

/** Below this length an anchor string is too common in prose to be worth searching for. */
private const val MIN_ANCHOR_SEARCH = 24

/** An annotation joined with just enough of its law to label it in a review list. */
data class AnnotationWithLaw(
    val id: Long,
    @ColumnInfo(name = "law_id") val lawId: Int,
    val section: String,
    val start: Int,
    val end: Int,
    @ColumnInfo(name = "anchor_text") val anchorText: String,
    val type: String,
    val color: String,
    val note: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "law_number") val lawNumber: Int,
    @ColumnInfo(name = "law_title") val lawTitle: String,
)

/** Filter state for the consolidated review screen. */
data class AnnotationFilter(
    val type: String = AnnotationType.MIXED,
    val color: String = "",
) {
    val isUnfiltered: Boolean get() = type == AnnotationType.MIXED && color.isBlank()
}

@Dao
interface AnnotationDao {

    @Query("SELECT * FROM annotations WHERE law_id = :lawId ORDER BY section, start")
    fun observeForLaw(lawId: Int): Flow<List<AnnotationEntity>>

    /**
     * Every mark, newest first, labelled with its law.
     *
     * Filtering is done in SQL rather than in Kotlin. A reader with a few hundred marks would fit
     * in memory either way, but the query is the only version that stays correct when the list
     * grows, and it keeps the composable from holding a full table it immediately discards.
     *
     * Both filters are nullable and both use NULL to mean "no filter" - `AnnotationsViewModel.review`
     * passes `takeIf`, so the default unfiltered state arrives as `(null, null)`. The colour clause
     * must therefore be `:color IS NULL` and not `:color = ''`: with a NULL parameter `NULL = ''` is
     * NULL rather than true, which makes the whole WHERE unknown and returns **no rows at all**. That
     * left the review screen empty for every reader on the default filter, and nothing had been
     * seeded to make it visible. The type clause already got this right; the two were inconsistent.
     */
    @Query(
        """
        SELECT a.*, l.law_number AS law_number, l.title AS law_title
        FROM annotations a JOIN laws l ON l.id = a.law_id
        WHERE (:type IS NULL OR :type = 'mixed' OR a.type = :type)
          AND (:color IS NULL OR a.color = :color)
        ORDER BY a.created_at DESC, a.id DESC
        """,
    )
    fun observeFiltered(type: String?, color: String?): Flow<List<AnnotationWithLaw>>

    @Query("SELECT * FROM annotations WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AnnotationEntity?

    /**
     * An existing highlight in this section that fully contains `[start, end)`.
     *
     * Containment, not overlap. This backs "select the same passage again to remove it", and a
     * partial overlap is ambiguous - resolving it here would silently delete a mark the reader
     * only grazed.
     */
    @Query(
        """
        SELECT * FROM annotations
        WHERE law_id = :lawId AND section = :section AND type = 'highlight'
          AND start <= :start AND `end` >= :end
        ORDER BY (`end` - start) ASC LIMIT 1
        """,
    )
    suspend fun findCoveringHighlight(lawId: Int, section: String, start: Int, end: Int): AnnotationEntity?

    /** A bookmark is a point, so it duplicates rather than overlapping. */
    @Query(
        "SELECT * FROM annotations WHERE law_id = :lawId AND section = :section AND type = 'bookmark' LIMIT 1",
    )
    suspend fun findBookmark(lawId: Int, section: String): AnnotationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(annotation: AnnotationEntity): Long

    @Query("UPDATE annotations SET note = :note, updated_at = :now WHERE id = :id")
    suspend fun setNote(id: Long, note: String, now: Long)

    @Query("UPDATE annotations SET color = :color, updated_at = :now WHERE id = :id")
    suspend fun setColor(id: Long, color: String, now: Long)

    @Query("DELETE FROM annotations WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM annotations WHERE law_id = :lawId")
    suspend fun deleteForLaw(lawId: Int)

    /**
     * Fills a missing anchor from the text the range points into.
     *
     * Rows created by `MIGRATION_5_6` arrive with an empty anchor because SQL could not compute
     * it, and a repair pass on read also fixes any anchor lost another way. Only rows with an
     * empty anchor are written, so this never rewrites a mark that is already anchored - the
     * anchor is evidence of where the reader actually marked, not something to regenerate.
     */
    @Query("UPDATE annotations SET anchor_text = :anchor WHERE id = :id AND anchor_text = ''")
    suspend fun backfillAnchorText(id: Long, anchor: String)

    /** One-shot read for the anchor repair pass, which cannot collect the Flow. */
    @Query("SELECT * FROM annotations WHERE law_id = :lawId AND section = :section ORDER BY start")
    suspend fun observeForLawOnce(lawId: Int, section: String): List<AnnotationEntity>

    /** True when a law carries any mark, for the subtle table-of-contents indicator. */
    @Query("SELECT COUNT(*) FROM annotations WHERE law_id = :lawId AND type != 'bookmark'")
    suspend fun countMarks(lawId: Int): Int
}