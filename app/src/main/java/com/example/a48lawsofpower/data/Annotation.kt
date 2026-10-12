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
 * A passage the reader marked, optionally with a margin note.
 *
 * Offsets are character indices into ONE section's text, never into a concatenation of several.
 * That matters: the reader lays each block out separately (`law.core`, `law.observance`, and so
 * on), so a single [start]/[end] pair is only meaningful against the [section] it names. Storing a
 * flattened offset would silently mis-highlight the moment the blocks were reordered.
 *
 * Offsets are recorded against the text as it is *today*. The law prose is shipped in the
 * pre-packaged database and is not edited in place, so drift only happens if that content is
 * rewritten in a future release. [clampedTo] is applied at read time so a stale offset degrades to
 * a shortened highlight instead of throwing.
 */
@Entity(
    tableName = "highlights",
    foreignKeys = [
        ForeignKey(
            entity = LawEntity::class,
            parentColumns = ["id"],
            childColumns = ["law_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("law_id")],
)
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "law_id")
    val lawId: Int,

    /** Which block of the law this lives in. See [SectionKey]. */
    val section: String,

    val start: Int,

    val end: Int,

    /**
     * A stable colour *name*, not a packed ARGB value. Storing the name keeps highlights correct
     * if the palette is ever re-tuned, and keeps this layer free of Compose types.
     */
    val color: String,

    @ColumnInfo(defaultValue = "''")
    val note: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = 0,
)

/**
 * The text blocks a highlight can point into.
 *
 * Deliberately a string in the database rather than an ordinal: the reader's section layout is UI
 * and is free to change, whereas stored highlights are not. Names survive reordering.
 */
object SectionKey {
    const val CORE = "core"
    const val SUMMARY = "summary"
    const val OBSERVANCE = "observance"
    const val TRANSGRESSION = "transgression"
    const val REVERSAL = "reversal"

    /** The text a highlight with this key refers to, for the given law. */
    fun textOf(law: LawEntity, section: String): String = when (section) {
        CORE -> law.core
        SUMMARY -> law.summary
        OBSERVANCE -> law.observance
        TRANSGRESSION -> law.transgression
        REVERSAL -> law.reversal
        else -> ""
    }
}

/** Returns a copy whose offsets are guaranteed to sit inside [text], or `null` if fully stale. */
fun HighlightEntity.clampedTo(text: String): HighlightEntity? {
    val s = start.coerceIn(0, text.length)
    val e = end.coerceIn(s, text.length)
    if (e <= s) return null
    return if (s == start && e == end) this else copy(start = s, end = e)
}

@Dao
interface HighlightDao {

    @Query("SELECT * FROM highlights WHERE law_id = :lawId ORDER BY section, start")
    fun observeForLaw(lawId: Int): Flow<List<HighlightEntity>>

    /** Every highlight with its law's title, for the notes/quote-export screen. */
    @Query(
        """
        SELECT h.*, laws.law_number AS law_number, laws.title AS law_title
        FROM highlights h JOIN laws ON laws.id = h.law_id
        ORDER BY h.created_at DESC
        """,
    )
    fun observeAllWithLaw(): Flow<List<HighlightWithLaw>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(highlight: HighlightEntity): Long

    @Query("UPDATE highlights SET note = :note WHERE id = :id")
    suspend fun setNote(id: Long, note: String)

    @Query("UPDATE highlights SET color = :color WHERE id = :id")
    suspend fun setColor(id: Long, color: String)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM highlights WHERE law_id = :lawId")
    suspend fun deleteForLaw(lawId: Int)
}

/** A highlight joined with just enough of its law to label it in a list. */
data class HighlightWithLaw(
    val id: Long,
    @ColumnInfo(name = "law_id") val lawId: Int,
    val section: String,
    val start: Int,
    val end: Int,
    val color: String,
    val note: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "law_number") val lawNumber: Int,
    @ColumnInfo(name = "law_title") val lawTitle: String,
)