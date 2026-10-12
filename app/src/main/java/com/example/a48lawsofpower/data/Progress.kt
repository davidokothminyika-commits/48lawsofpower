package com.example.a48lawsofpower.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

/**
 * One row per calendar day the reader opened the app.
 *
 * Keyed by [day] (an epoch-day, i.e. days since 1970-01-01 in the device's local time) rather than
 * a millisecond timestamp. That makes "which days are consecutive" a simple arithmetic comparison
 * and makes streaks immune to timezone drift across a trip: the day boundary is resolved in
 * whatever zone the device is in when the row is written.
 *
 * One row per day is also what makes the whole gamification layer cheap - streaks, daily goals,
 * and milestone counts are all folds over this single table.
 */
@Entity(tableName = "reading_days")
data class ReadingDayEntity(
    /** Days since the epoch, local date. See the class docs for why not a timestamp. */
    @PrimaryKey
    val day: Long,

    /** Distinct laws marked read that day, not the number of times they were re-opened. */
    @ColumnInfo(name = "laws_read", defaultValue = "0")
    val lawsRead: Int = 0,

    /** Wall-clock seconds spent with a law open, accumulated across sessions. */
    @ColumnInfo(name = "seconds_read", defaultValue = "0")
    val secondsRead: Long = 0,
)

/** Current reading totals, computed by a single fold over [reading_days]. */
data class ReadingStats(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalDays: Int = 0,
    val totalLawsRead: Int = 0,
    val secondsToday: Long = 0,
    val lawsReadToday: Int = 0,
) {
    val totalMinutes: Long get() = secondsToday / 60
}

/** Today's epoch-day, in the device's current zone. */
fun todayEpochDay(zone: ZoneId = ZoneId.systemDefault()): Long =
    LocalDate.now(zone).toEpochDay()

/**
 * Folds the day rows into streak and goal numbers.
 *
 * Written as an explicit scan rather than a SQL window function so the streak rules are in one
 * readable place and identical on every supported SQLite version (minSdk 24 ships versions with no
 * window functions at all).
 *
 * Rules, in the order they matter:
 *  - A streak may end *yesterday* and still count. Today not being logged yet is the normal state
 *    at breakfast, and breaking a streak before the day is over punishes someone for waking up.
 *  - Duplicated or out-of-order rows cannot happen: [day] is the primary key.
 */
fun computeStats(
    days: List<Long>,
    today: Long,
    lawsByDay: Map<Long, Int> = emptyMap(),
    secondsByDay: Map<Long, Long> = emptyMap(),
): ReadingStats {
    if (days.isEmpty()) return ReadingStats()

    val sorted = days.sorted()
    var longest = 1
    var run = 1
    for (i in 1 until sorted.size) {
        run = if (sorted[i] == sorted[i - 1] + 1) run + 1 else 1
        if (run > longest) longest = run
    }

    // Anchor on today if logged, else yesterday, so a streak is not reported as broken before
    // the day has actually been missed.
    val anchor = when {
        sorted.contains(today) -> today
        sorted.contains(today - 1) -> today - 1
        else -> return ReadingStats(
            longestStreak = longest,
            totalDays = sorted.size,
            totalLawsRead = lawsByDay.values.sum(),
        )
    }

    var current = 0
    var cursor = anchor
    while (sorted.contains(cursor)) {
        current++
        cursor--
    }

    return ReadingStats(
        currentStreak = current,
        longestStreak = longest,
        totalDays = sorted.size,
        totalLawsRead = lawsByDay.values.sum(),
        secondsToday = secondsByDay[today] ?: 0L,
        lawsReadToday = lawsByDay[today] ?: 0,
    )
}

@Dao
interface ProgressDao {

    @Query("SELECT day FROM reading_days")
    fun observeDays(): Flow<List<Long>>

    @Query("SELECT * FROM reading_days ORDER BY day DESC")
    fun observeAllDays(): Flow<List<ReadingDayEntity>>

    /** Records activity for [day], keeping the high-water mark rather than overwriting it. */
    @Query(
        """
        INSERT INTO reading_days (day, laws_read, seconds_read) VALUES (:day, :laws, :seconds)
        ON CONFLICT(day) DO UPDATE SET
            laws_read = MAX(laws_read, excluded.laws_read),
            seconds_read = seconds_read + excluded.seconds_read
        """,
    )
    suspend fun recordActivity(day: Long, laws: Int, seconds: Long)

    @Query("SELECT * FROM reading_days WHERE day = :day LIMIT 1")
    suspend fun getDay(day: Long): ReadingDayEntity?

    @Query("DELETE FROM reading_days")
    suspend fun clear()
}