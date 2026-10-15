package com.example.a48lawsofpower.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the v5 additions against a real SQLite on a real device.
 *
 * This is the layer the unit tests cannot reach: Room's generated code, the SQLite dialect of a
 * particular Android version, and the `ON CONFLICT ... DO UPDATE` clause in `recordActivity`,
 * which is the kind of SQL that is accepted by a desktop SQLite and rejected on older Android
 * ones. A build that compiles proves none of that.
 *
 * Uses an in-memory database rather than the production one so the test cannot disturb real
 * highlights, and so it starts from the v5 schema directly instead of depending on a migration.
 * Migration correctness is covered separately by tools/verify_migrations.py and by upgrading the
 * installed app in place.
 */
@RunWith(AndroidJUnit4::class)
class AnnotationDaoInstrumentedTest {

    private lateinit var db: LawDatabase
    private lateinit var highlights: HighlightDao
    private lateinit var progress: ProgressDao
    private var lawId: Int = 0

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, LawDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        highlights = db.highlightDao()
        progress = db.progressDao()
        lawId = seedLaw()
    }

    @After
    fun tearDown() {
        db.close()
    }

    /**
     * `highlights.law_id` is a foreign key with ON DELETE CASCADE, so a parent row has to exist.
     *
     * In production the laws come from the pre-packaged asset, which an in-memory database does
     * not have. LawDao exposes no insert - the reader never creates laws - so the single row is
     * written with raw SQL. Deliberately the minimum a row can be: the tests only need a valid
     * parent and one title for the notes-screen join.
     */
    private fun seedLaw(): Int {
        val sql = db.openHelper.writableDatabase
        sql.execSQL(
            """
            INSERT INTO laws
                (law_number, category, title, summary, core, observance, transgression, reversal, bookmarked, read)
            VALUES (1, 'Strategic action', 'Never Outshine the Master', 'A test law.', '', '', '', '', 0, 0)
            """.trimIndent(),
        )
        sql.query("SELECT id FROM laws WHERE law_number = 1").use { c ->
            c.moveToFirst()
            return c.getInt(0)
        }
    }

    @Test
    fun highlightRoundTrips() = runBlocking {
        highlights.insert(
            HighlightEntity(
                lawId = lawId,
                section = SectionKey.CORE,
                start = 10,
                end = 42,
                color = "crimson",
                note = "check this against Ptolemy",
                createdAt = 1_700_000_000_000,
            ),
        )
        val found = highlights.observeForLaw(lawId).first()
        assertEquals(1, found.size)
        assertEquals(10, found[0].start)
        assertEquals(42, found[0].end)
        assertEquals("crimson", found[0].color)
        assertEquals("check this against Ptolemy", found[0].note)
    }

    @Test
    fun noteAndColourAreEditable() = runBlocking {
        val id = highlights.insert(
            HighlightEntity(lawId = lawId, section = SectionKey.REVERSAL, start = 0, end = 5, color = "gold"),
        )
        highlights.setNote(id, "revised")
        highlights.setColor(id, "violet")

        val found = highlights.observeForLaw(lawId).first().single()
        assertEquals("revised", found.note)
        assertEquals("violet", found.color)
    }

    @Test
    fun reselectingACoveringRangeFindsIt() = runBlocking {
        highlights.insert(
            HighlightEntity(lawId = lawId, section = SectionKey.CORE, start = 10, end = 50, color = "gold"),
        )
        val covering = highlights.findCovering(lawId, SectionKey.CORE, 20, 30)
        assertNotNull("a range fully inside the highlight should match", covering)
        assertEquals(10, covering!!.start)
        assertEquals(50, covering.end)
    }

    @Test
    fun aRangeNotCoveredIsNotMatched() = runBlocking {
        highlights.insert(
            HighlightEntity(lawId = lawId, section = SectionKey.CORE, start = 10, end = 50, color = "gold"),
        )
        // Straddles the boundary, so containment must not match it.
        assertNull(highlights.findCovering(lawId, SectionKey.CORE, 5, 20))
        // Different section entirely.
        assertNull(highlights.findCovering(lawId, SectionKey.REVERSAL, 10, 50))
    }

    @Test
    fun highlightsAreListedAcrossLawsForTheNotesScreen() = runBlocking {
        highlights.insert(
            HighlightEntity(lawId = lawId, section = SectionKey.CORE, start = 0, end = 3, color = "gold", createdAt = 10),
        )
        val joined = highlights.observeAllWithLaw().first()
        assertEquals(1, joined.size)
        assertNotNull(joined[0].lawTitle)
    }

    @Test
    fun deleteRemovesOnlyThatHighlight() = runBlocking {
        val keep = highlights.insert(
            HighlightEntity(lawId = lawId, section = SectionKey.CORE, start = 0, end = 3, color = "gold"),
        )
        val drop = highlights.insert(
            HighlightEntity(lawId = lawId, section = SectionKey.SUMMARY, start = 5, end = 9, color = "gold"),
        )
        highlights.delete(drop)
        val left = highlights.observeForLaw(lawId).first().map { it.id }
        assertEquals(listOf(keep), left)
    }

    @Test
    fun readingDayUpsertKeepsTheHighWaterMarkAndAddsSeconds() = runBlocking {
        val day = 20_000L
        progress.recordActivity(day = day, laws = 2, seconds = 60)
        // Re-recording fewer laws must not lower the count...
        progress.recordActivity(day = day, laws = 1, seconds = 30)
        // ...but seconds accumulate across sessions.
        progress.recordActivity(day = day, laws = 5, seconds = 45)

        val row = progress.getDay(day)
        assertNotNull(row)
        assertEquals(5, row!!.lawsRead)
        assertEquals(135L, row.secondsRead)
    }

    @Test
    fun statsFoldAcrossRealStoredDays() = runBlocking {
        progress.recordActivity(day = 19_998L, laws = 1, seconds = 10)
        progress.recordActivity(day = 19_999L, laws = 1, seconds = 10)
        progress.recordActivity(day = 20_000L, laws = 2, seconds = 90)

        val rows = progress.observeAllDays().first()
        val stats = computeStats(
            days = rows.map { it.day },
            today = 20_000L,
            lawsByDay = rows.associate { it.day to it.lawsRead },
            secondsByDay = rows.associate { it.day to it.secondsRead },
        )
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.longestStreak)
        assertEquals(2, stats.lawsReadToday)
        assertEquals(1, stats.totalMinutes)
    }

    @Test
    fun theUniqueDayKeyRejectsADuplicateInsert() = runBlocking {
        progress.recordActivity(day = 20_000L, laws = 1, seconds = 10)
        // A plain INSERT would violate the primary key; the upsert must absorb it instead.
        progress.recordActivity(day = 20_000L, laws = 1, seconds = 10)
        assertEquals(1, progress.observeAllDays().first().size)
    }

    @Test
    fun clearingProgressLeavesHighlightsAlone() = runBlocking {
        highlights.insert(
            HighlightEntity(lawId = lawId, section = SectionKey.CORE, start = 0, end = 3, color = "gold"),
        )
        progress.recordActivity(day = 20_000L, laws = 1, seconds = 10)
        progress.clear()

        assertTrue(progress.observeAllDays().first().isEmpty())
        assertEquals(1, highlights.observeForLaw(lawId).first().size)
    }
}