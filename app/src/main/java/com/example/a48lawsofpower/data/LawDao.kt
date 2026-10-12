package com.example.a48lawsofpower.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

private const val OVERVIEW_COLUMNS =
    "id, law_number, category, title, summary, bookmarked, read"

@Dao
interface LawDao {

    @Query("SELECT $OVERVIEW_COLUMNS FROM laws ORDER BY law_number ASC")
    fun observeAllLaws(): Flow<List<LawOverview>>

    @Query("SELECT $OVERVIEW_COLUMNS FROM laws WHERE bookmarked = 1 ORDER BY law_number ASC")
    fun observeBookmarkedLaws(): Flow<List<LawOverview>>

    @Query("SELECT * FROM laws WHERE law_number = :lawNumber LIMIT 1")
    suspend fun getLawByNumber(lawNumber: Int): LawEntity?

    @Query("SELECT * FROM laws WHERE law_number = :lawNumber LIMIT 1")
    fun observeLawByNumber(lawNumber: Int): Flow<LawEntity?>

    @Query("SELECT * FROM laws WHERE law_number = :lawNumber LIMIT 1")
    suspend fun getDailyLaw(lawNumber: Int): LawEntity?

    @Query("UPDATE laws SET read = :isRead WHERE law_number = :lawNumber")
    suspend fun markLawAsRead(lawNumber: Int, isRead: Boolean)

    @Query("UPDATE laws SET bookmarked = :bookmarked WHERE law_number = :lawNumber")
    suspend fun setBookmarked(lawNumber: Int, bookmarked: Boolean)
}
