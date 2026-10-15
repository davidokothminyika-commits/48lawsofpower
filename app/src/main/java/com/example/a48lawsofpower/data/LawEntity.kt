package com.example.a48lawsofpower.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single law with its full editorial payload.
 *
 * The `defaultValue` declarations are load-bearing: they are what lets `Migration_3_4` add these
 * columns to already-installed databases, and Room requires a declared default before it will add a
 * NOT NULL column. The quotes around `''` are part of the value, not a typo.
 */
@Entity(tableName = "laws")
data class LawEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "law_number")
    val lawNumber: Int,

    val category: String,

    val title: String,

    val summary: String,

    /** Section I - the editorial judgement on the law. */
    @ColumnInfo(name = "core", defaultValue = "''")
    val core: String = "",

    /** Section II - what happens when the law is followed. */
    @ColumnInfo(name = "observance", defaultValue = "''")
    val observance: String = "",

    /** Section II - what happens when the law is ignored. */
    @ColumnInfo(name = "transgression", defaultValue = "''")
    val transgression: String = "",

    /** Section III - the caveat and the counter-strategy. */
    @ColumnInfo(name = "reversal", defaultValue = "''")
    val reversal: String = "",

    /**
     * The short, portable points of the law - what the reader is meant to carry out of the reader.
     *
     * Stored as one newline-delimited string rather than a list so the column stays a plain TEXT and
     * the pre-packaged asset stays inspectable. Takeaways are single-line by construction; [takeaways]
     * is the safe way to read it back and drops the empty case an upgraded install will have.
     */
    @ColumnInfo(name = "key_takeaways", defaultValue = "''")
    val keyTakeaways: String = "",

    @ColumnInfo(name = "bookmarked", defaultValue = "0")
    val bookmarked: Boolean = false,

    @ColumnInfo(name = "read")
    val read: Boolean = false,
)

/**
 * Lightweight projection for the list screens. Reading only these columns keeps the four long
 * editorial blocks off the main list query, which is what makes scrolling cheap.
 */
data class LawOverview(
    val id: Int,
    @ColumnInfo(name = "law_number")
    val lawNumber: Int,
    val category: String,
    val title: String,
    val summary: String,
    @ColumnInfo(name = "bookmarked")
    val bookmarked: Boolean,
    @ColumnInfo(name = "read")
    val read: Boolean,
)

/** Separator for [LawEntity.keyTakeaways]. A newline, because a takeaway is always one line. */
private const val TAKEAWAY_SEPARATOR = '\n'

/**
 * The takeaways as a list.
 *
 * Blank-tolerant on purpose: an install that upgraded from schema 6 has the column but no rows in
 * it, and `trim` plus `filter` means the reader simply shows no takeaways block rather than an
 * empty list with stray separators.
 */
fun LawEntity.takeaways(): List<String> =
    keyTakeaways.split(TAKEAWAY_SEPARATOR)
        .map(String::trim)
        .filter(String::isNotEmpty)

/** Inverse of [takeaways], for the content tooling. */
fun List<String>.toKeyTakeawaysColumn(): String =
    joinToString(TAKEAWAY_SEPARATOR.toString()) { it.trim() }
