package com.ismael.daybyday

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismael.daybyday.data.AppDatabase
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.DayDao
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.DayTagCrossRef
import com.ismael.daybyday.data.DayPart
import com.ismael.daybyday.data.MediaItem
import com.ismael.daybyday.data.MoneyEntry
import com.ismael.daybyday.data.SportLevel
import com.ismael.daybyday.data.Tag
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
    fun ecritureEtRelectureDUneJournee(): Unit = runBlocking {
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
    fun miseAJourDUneJourneeExistante(): Unit = runBlocking {
        val epochDay = LocalDate.of(2026, 5, 13).toEpochDay()
        dao.upsertDay(DayEntry(epochDay = epochDay, colorKey = DayColor.RED.key))
        dao.upsertDay(DayEntry(epochDay = epochDay, colorKey = DayColor.GREEN.key, title = "Mieux"))

        val days = dao.allDays()

        assertEquals(1, days.size)
        assertEquals(DayColor.GREEN, days.first().color)
        assertEquals("Mieux", days.first().title)
    }

    @Test
    fun lectureParPlageDeDates(): Unit = runBlocking {
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
    fun comptageDesMediasParJour(): Unit = runBlocking {
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
    fun suppressionDUneJournee(): Unit = runBlocking {
        val epochDay = LocalDate.of(2026, 8, 9).toEpochDay()
        dao.upsertDay(DayEntry(epochDay = epochDay, colorKey = DayColor.BLACK.key))
        dao.deleteDay(epochDay)

        assertNull(dao.dayOnce(epochDay))
    }

    @Test
    fun rechercheDansLesTitresEtLesNotes(): Unit = runBlocking {
        val base = LocalDate.of(2026, 9, 1).toEpochDay()
        dao.upsertDay(DayEntry(epochDay = base, title = "Retour à la maison", note = "Journée calme"))
        dao.upsertDay(DayEntry(epochDay = base + 1, title = "Boulot", note = "Longue réunion"))

        val byTitle = dao.search("maison").first()
        val byNote = dao.search("réunion").first()
        val nothing = dao.search("zzzz").first()

        assertEquals(1, byTitle.size)
        assertEquals(base, byTitle.first().epochDay)
        assertEquals(1, byNote.size)
        assertEquals(0, nothing.size)
    }

    @Test
    fun etiquettesLieesEtDelieesDUneJournee(): Unit = runBlocking {
        val epochDay = LocalDate.of(2026, 9, 2).toEpochDay()
        val tagId = dao.insertTag(Tag(name = "Marche", emoji = "🚶", sortOrder = 0))
        dao.upsertDay(DayEntry(epochDay = epochDay))

        dao.linkTag(DayTagCrossRef(epochDay = epochDay, tagId = tagId))
        assertEquals(1, dao.tagCountForDay(epochDay))
        assertEquals("Marche", dao.observeTagsForDay(epochDay).first().first().name)

        dao.unlinkTag(epochDay, tagId)
        assertEquals(0, dao.tagCountForDay(epochDay))
    }

    @Test
    fun supprimerUneEtiquetteNeCasseRien(): Unit = runBlocking {
        val epochDay = LocalDate.of(2026, 9, 3).toEpochDay()
        val tagId = dao.insertTag(Tag(name = "Fast-food", sortOrder = 1))
        dao.upsertDay(DayEntry(epochDay = epochDay))
        dao.linkTag(DayTagCrossRef(epochDay = epochDay, tagId = tagId))

        dao.deleteTagLinks(tagId)
        dao.deleteTag(tagId)

        assertEquals(0, dao.allTags().size)
        assertEquals(0, dao.allDayTags().size)
        assertEquals(epochDay, dao.dayOnce(epochDay)?.epochDay)
    }

    @Test
    fun suiviDuPoidsEtDesDetailsDeLaJournee(): Unit = runBlocking {
        val base = LocalDate.of(2026, 9, 4).toEpochDay()
        dao.upsertDay(
            DayEntry(
                epochDay = base,
                weightKg = 130.4,
                sportLevel = SportLevel.GOOD.key,
                wentOut = true,
            )
        )
        dao.upsertDay(DayEntry(epochDay = base + 1, weightKg = 129.8))
        dao.upsertDay(DayEntry(epochDay = base + 2, title = "Sans poids"))

        val weights = dao.observeWeights().first()
        val stored = dao.dayOnce(base)

        assertEquals(2, weights.size)
        assertEquals(130.4, weights.first().weightKg, 0.001)
        assertEquals(SportLevel.GOOD, stored?.sport)
        assertEquals(true, stored?.wentOut)
    }

    @Test
    fun mouvementsDArgentEtSolde(): Unit = runBlocking {
        val day = LocalDate.of(2026, 9, 10).toEpochDay()
        dao.upsertMoney(MoneyEntry(epochDay = day, amountCents = 120_000, label = "Salaire"))
        dao.upsertMoney(MoneyEntry(epochDay = day, amountCents = -45_50, label = "Courses"))
        dao.upsertMoney(MoneyEntry(epochDay = day + 40, amountCents = -1_000, label = "Hors période"))

        val balance = dao.observeMoneyBalance().first()
        val inMonth = dao.observeMoneyBetween(day - 5, day + 5).first()

        assertEquals(120_000L - 4_550L - 1_000L, balance)
        assertEquals(2, inMonth.size)

        dao.deleteMoney(inMonth.first().id)
        assertEquals(2, dao.allMoney().size)
    }

    @Test
    fun momentsDeLaJourneeEnregistres(): Unit = runBlocking {
        val epochDay = LocalDate.of(2026, 9, 11).toEpochDay()
        dao.upsertDay(
            DayEntry(epochDay = epochDay)
                .withPartColor(DayPart.MORNING, DayColor.GREEN.key)
                .withPartColor(DayPart.NIGHT, DayColor.BLACK.key)
        )

        val stored = dao.dayOnce(epochDay)

        assertEquals(DayColor.GREEN, stored?.partColor(DayPart.MORNING))
        assertEquals(DayColor.BLACK, stored?.partColor(DayPart.NIGHT))
        assertEquals(null, stored?.partColor(DayPart.EVENING))
        // Moyenne de 3 et 0 -> 1,5 arrondi vers la couleur la plus proche.
        assertEquals(2, stored?.filledParts?.size)
    }
}
