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
 * Exercises the DAO against a real SQLite on a real device.
 *
 * This is the layer the unit tests cannot reach: Room's generated code, the SQLite dialect of a
 * particular Android version, and the `ON CONFLICT ... DO UPDATE` clause in `recordActivity`,
 * which is the kind of SQL that is accepted by a desktop SQLite and rejected on older Android
 * ones. A build that compiles proves none of that.
 *
 * Uses an in-memory database rather than the production one so the test cannot disturb real
 * highlights, and so it starts from the current schema directly instead of depending on a
 * migration. Migration correctness is covered separately by tools/verify_migrations.py and by
 * upgrading the installed app in place.
 *
 * Rewritten against the v5+ [AnnotationDao]. This file previously held the v4 [HighlightDao] suite
 * and had not compiled since the schema changed - `assembleDebug` and `testDebugUnitTest` both stay
 * green without ever touching the androidTest source set, so nothing noticed. Its tests are kept
 * here deliberately; they were sound, just written against an API that no longer exists.
 */
@RunWith(AndroidJUnit4::class)
class AnnotationDaoInstrumentedTest {

    private lateinit var db: LawDatabase
    private lateinit var annotations: AnnotationDao
    private lateinit var progress: ProgressDao
    private var lawId: Int = 0

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, LawDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        annotations = db.annotationDao()
        progress = db.progressDao()
        lawId = seedLaw()
    }

    @After
    fun tearDown() {
        db.close()
    }

    /**
     * `annotations.law_id` is a foreign key with ON DELETE CASCADE, so a parent row has to exist.
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
                (law_number, category, title, summary, core, observance, transgression, reversal,
                 key_takeaways, bookmarked, read)
            VALUES (1, 'Strategic action', 'Never Outshine the Master', 'A test law.',
                    '', '', '', '', '', 0, 0)
            """.trimIndent(),
        )
        sql.query("SELECT id FROM laws WHERE law_number = 1").use { c ->
            c.moveToFirst()
            return c.getInt(0)
        }
    }

    /** A minimal mark. `anchor_text` is NOT NULL with no default, so it is always supplied. */
    private fun mark(
        section: String = SectionKey.CORE,
        start: Int = 0,
        end: Int = 3,
        color: String = "gold",
        note: String = "",
        type: String = AnnotationType.Highlight.wire,
        createdAt: Long = 0,
        anchor: String = "anchor text for the mark",
    ) = AnnotationEntity(
        lawId = lawId,
        section = section,
        start = start,
        end = end,
        anchorText = anchor,
        type = type,
        color = color,
        note = note,
        createdAt = createdAt,
    )

    @Test
    fun annotationRoundTrips() = runBlocking {
        annotations.insert(
            mark(
                start = 10,
                end = 42,
                color = "crimson",
                note = "check this against Ptolemy",
                createdAt = 1_700_000_000_000,
            ),
        )
        val found = annotations.observeForLaw(lawId).first()
        assertEquals(1, found.size)
        assertEquals(10, found[0].start)
        assertEquals(42, found[0].end)
        assertEquals("crimson", found[0].color)
        assertEquals("check this against Ptolemy", found[0].note)
    }

    @Test
    fun noteAndColourAreEditable() = runBlocking {
        val id = annotations.insert(mark(section = SectionKey.REVERSAL, end = 5))
        annotations.setNote(id, "revised", 1L)
        annotations.setColor(id, "violet", 1L)

        val found = annotations.observeForLaw(lawId).first().single()
        assertEquals("revised", found.note)
        assertEquals("violet", found.color)
    }

    @Test
    fun reselectingACoveringRangeFindsIt() = runBlocking {
        annotations.insert(mark(start = 10, end = 50))
        val covering = annotations.findCoveringHighlight(lawId, SectionKey.CORE, 20, 30)
        assertNotNull("a range fully inside the highlight should match", covering)
        assertEquals(10, covering!!.start)
        assertEquals(50, covering.end)
    }

    @Test
    fun aRangeNotCoveredIsNotMatched() = runBlocking {
        annotations.insert(mark(start = 10, end = 50))
        // Straddles the boundary, so containment must not match it.
        assertNull(annotations.findCoveringHighlight(lawId, SectionKey.CORE, 5, 20))
        // Different section entirely.
        assertNull(annotations.findCoveringHighlight(lawId, SectionKey.REVERSAL, 10, 50))
    }

    @Test
    fun highlightsAreListedAcrossLawsForTheNotesScreen() = runBlocking {
        annotations.insert(mark(start = 0, end = 3, createdAt = 10))
        val joined = annotations.observeFiltered(null, null).first()
        assertEquals(1, joined.size)
        assertNotNull(joined[0].lawTitle)
    }

    /**
     * Regression: a NULL filter means "no filter".
     *
     * `AnnotationsViewModel.review` passes `(null, null)` for its default, unfiltered state via
     * `takeIf`. The colour clause used to read `:color = ''`, and with a NULL parameter that
     * evaluates to NULL rather than true - so the entire WHERE was unknown and the review screen
     * returned **nothing** for a reader who had not touched the filters. Every default visit to
     * My Notes was empty, which is why the screen needed seeding before the bug was visible.
     *
     * These two assertions are the whole fix: one per NULL parameter, both of which used to yield
     * an empty list.
     */
    @Test
    fun aNullFilterMeansNoFilterNotNoResults() = runBlocking {
        annotations.insert(mark(createdAt = 1))
        annotations.insert(
            mark(type = AnnotationType.Note.wire, note = "a real note", createdAt = 2),
        )

        assertEquals(
            "both marks, when neither filter is set",
            2,
            annotations.observeFiltered(null, null).first().size,
        )
        assertEquals(
            "a NULL type filter alone must not hide anything",
            2,
            annotations.observeFiltered(null, "gold").first().size,
        )
    }

    @Test
    fun theTypeFilterNarrowsWithoutHidingTheRest() = runBlocking {
        annotations.insert(mark(createdAt = 1))
        annotations.insert(mark(type = AnnotationType.Note.wire, note = "n", createdAt = 2))

        assertEquals(1, annotations.observeFiltered("highlight", null).first().size)
        assertEquals(1, annotations.observeFiltered("note", null).first().size)
        // 'mixed' is the stored-never sentinel for "everything".
        assertEquals(2, annotations.observeFiltered("mixed", null).first().size)
    }

    @Test
    fun theColourFilterNarrowsTheList() = runBlocking {
        annotations.insert(mark(color = "gold", createdAt = 1))
        annotations.insert(mark(color = "crimson", createdAt = 2))

        assertEquals(1, annotations.observeFiltered(null, "gold").first().size)
        assertEquals(2, annotations.observeFiltered(null, null).first().size)
    }

    @Test
    fun deleteRemovesOnlyThatMark() = runBlocking {
        val keep = annotations.insert(mark(section = SectionKey.CORE, start = 0, end = 3))
        val drop = annotations.insert(mark(section = SectionKey.SUMMARY, start = 5, end = 9))
        annotations.delete(drop)
        assertEquals(listOf(keep), annotations.observeForLaw(lawId).first().map { it.id })
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
        assertEquals(3, stats.totalDays)
    }

    @Test
    fun theUniqueDayKeyRejectsADuplicateInsert() = runBlocking {
        progress.recordActivity(day = 20_000L, laws = 1, seconds = 10)
        // A plain INSERT would violate the primary key; the upsert must absorb it instead.
        progress.recordActivity(day = 20_000L, laws = 1, seconds = 10)
        assertEquals(1, progress.observeAllDays().first().size)
    }

    @Test
    fun clearingProgressLeavesAnnotationsAlone() = runBlocking {
        annotations.insert(mark(section = SectionKey.CORE, start = 0, end = 3))
        progress.recordActivity(day = 20_000L, laws = 1, seconds = 10)
        progress.clear()

        assertTrue(progress.observeAllDays().first().isEmpty())
        assertEquals(1, annotations.observeForLaw(lawId).first().size)
    }

    // ---- bookmarks -------------------------------------------------------------------
    //
    // The bookmark path had no coverage at all: the Bookmarks filter rendered in the UI with
    // nothing able to create one. These cover the query the UI toggle actually depends on.

    @Test
    fun aBookmarkIsFoundByLawAndSection() = runBlocking {
        annotations.insert(
            mark(type = AnnotationType.Bookmark.wire, start = 0, end = 0, anchor = "", color = ""),
        )
        val found = annotations.findBookmark(lawId, SectionKey.CORE)
        assertNotNull("bookmark should be retrievable for its own section", found)
        assertEquals(AnnotationType.Bookmark.wire, found!!.type)
    }

    @Test
    fun aBookmarkIsNotFoundInAnotherSection() = runBlocking {
        annotations.insert(
            mark(type = AnnotationType.Bookmark.wire, start = 0, end = 0, anchor = "", color = ""),
        )
        // A bookmark belongs to the section it was made in, so a toggle in a different section
        // must not silently un-bookmark the first one.
        assertNull(annotations.findBookmark(lawId, SectionKey.REVERSAL))
    }

    @Test
    fun aHighlightIsNeverMistakenForABookmark() = runBlocking {
        annotations.insert(mark(section = SectionKey.CORE))
        assertNull(annotations.findBookmark(lawId, SectionKey.CORE))
    }

    @Test
    fun theBookmarksFilterReturnsOnlyBookmarks() = runBlocking {
        annotations.insert(mark(section = SectionKey.CORE))
        annotations.insert(mark(type = AnnotationType.Note.wire, note = "why?", section = SectionKey.CORE))
        annotations.insert(
            mark(type = AnnotationType.Bookmark.wire, start = 0, end = 0, anchor = "", color = ""),
        )

        val onlyBookmarks = annotations
            .observeFiltered(AnnotationType.Bookmark.wire, null)
            .first()
        assertEquals(1, onlyBookmarks.size)
        assertEquals(AnnotationType.Bookmark.wire, onlyBookmarks[0].type)
    }
}
