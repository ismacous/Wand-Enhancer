package com.ismael.daybyday

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismael.daybyday.data.AppDatabase
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.DayDao
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.MediaItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DayDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: DayDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = database.dayDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun ecritureEtRelectureDUneJournee() = runBlocking {
        val date = LocalDate.of(2026, 5, 12)
        dao.upsertDay(
            DayEntry(
                epochDay = date.toEpochDay(),
                colorKey = DayColor.ORANGE.key,
                title = "Journée mitigée",
                note = "Beaucoup de fatigue mais une bonne nouvelle.",
            )
        )

        val stored = dao.dayOnce(date.toEpochDay())

        assertEquals(DayColor.ORANGE, stored?.color)
        assertEquals("Journée mitigée", stored?.title)
    }

    @Test
    fun miseAJourDUneJourneeExistante() = runBlocking {
        val epochDay = LocalDate.of(2026, 5, 13).toEpochDay()
        dao.upsertDay(DayEntry(epochDay = epochDay, colorKey = DayColor.RED.key))
        dao.upsertDay(DayEntry(epochDay = epochDay, colorKey = DayColor.GREEN.key, title = "Mieux"))

        val days = dao.allDays()

        assertEquals(1, days.size)
        assertEquals(DayColor.GREEN, days.first().color)
        assertEquals("Mieux", days.first().title)
    }

    @Test
    fun lectureParPlageDeDates() = runBlocking {
        val start = LocalDate.of(2026, 6, 1)
        (0 until 5).forEach { offset ->
            dao.upsertDay(
                DayEntry(
                    epochDay = start.plusDays(offset.toLong()).toEpochDay(),
                    colorKey = DayColor.GREEN.key,
                )
            )
        }

        val range = dao.observeRange(
            start.plusDays(1).toEpochDay(),
            start.plusDays(3).toEpochDay(),
        ).first()

        assertEquals(3, range.size)
    }

    @Test
    fun comptageDesMediasParJour() = runBlocking {
        val epochDay = LocalDate.of(2026, 7, 4).toEpochDay()
        dao.upsertDay(DayEntry(epochDay = epochDay))
        dao.insertMedia(MediaItem(epochDay = epochDay, relativePath = "2026/07/a.jpg", kindKey = 0))
        dao.insertMedia(MediaItem(epochDay = epochDay, relativePath = "2026/07/b.mp4", kindKey = 1))

        val counts = dao.observeMediaCounts(epochDay - 5, epochDay + 5).first()

        assertEquals(1, counts.size)
        assertEquals(2, counts.first().count)
        assertEquals(2, dao.mediaForDay(epochDay).size)
    }

    @Test
    fun suppressionDUneJournee() = runBlocking {
        val epochDay = LocalDate.of(2026, 8, 9).toEpochDay()
        dao.upsertDay(DayEntry(epochDay = epochDay, colorKey = DayColor.BLACK.key))
        dao.deleteDay(epochDay)

        assertNull(dao.dayOnce(epochDay))
    }
}
