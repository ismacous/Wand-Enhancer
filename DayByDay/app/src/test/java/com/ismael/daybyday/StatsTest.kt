package com.ismael.daybyday

import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.DayTagCrossRef
import com.ismael.daybyday.data.FoodLevel
import com.ismael.daybyday.data.SportLevel
import com.ismael.daybyday.data.Stats
import com.ismael.daybyday.data.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StatsTest {

    private fun entry(date: LocalDate, color: DayColor?) =
        DayEntry(epochDay = date.toEpochDay(), colorKey = color?.key)

    @Test
    fun `moyenne ignore les jours sans couleur`() {
        val start = LocalDate.of(2026, 1, 1)
        val entries = listOf(
            entry(start, DayColor.GREEN),
            entry(start.plusDays(1), DayColor.RED),
            entry(start.plusDays(2), null),
        )

        val summary = Stats.summarize("Test", entries, 31)

        assertEquals(2, summary.filledDays)
        assertEquals(2.0, summary.average!!, 0.001)
        assertEquals(1, summary.countOf(DayColor.GREEN))
        assertEquals(1, summary.countOf(DayColor.RED))
        assertEquals(0, summary.countOf(DayColor.BLACK))
    }

    @Test
    fun `periode vide n a pas de moyenne`() {
        val summary = Stats.summarize("Vide", emptyList(), 30)
        assertNull(summary.average)
        assertEquals(0, summary.filledDays)
    }

    @Test
    fun `journee noire vaut zero et journee verte vaut trois`() {
        assertEquals(0, DayColor.BLACK.score)
        assertEquals(3, DayColor.GREEN.score)
    }

    @Test
    fun `series de jours consecutifs`() {
        val today = LocalDate.of(2026, 3, 10)
        val entries = listOf(
            // Serie de 4 jours qui se termine aujourd'hui.
            entry(today.minusDays(3), DayColor.GREEN),
            entry(today.minusDays(2), DayColor.ORANGE),
            entry(today.minusDays(1), DayColor.RED),
            entry(today, DayColor.GREEN),
            // Serie isolee plus ancienne.
            entry(today.minusDays(20), DayColor.GREEN),
        )

        val (current, longest) = Stats.streaks(entries, today)

        assertEquals(4, current)
        assertEquals(4, longest)
    }

    @Test
    fun `la serie en cours tolere une journee pas encore notee`() {
        val today = LocalDate.of(2026, 3, 10)
        val entries = listOf(
            entry(today.minusDays(2), DayColor.GREEN),
            entry(today.minusDays(1), DayColor.GREEN),
        )

        val (current, _) = Stats.streaks(entries, today)

        assertEquals(2, current)
    }

    @Test
    fun `le sport ressort comme facteur des bonnes journees`() {
        val start = LocalDate.of(2026, 1, 1)
        val days = buildList {
            // Six journees avec du sport, plutot bonnes.
            repeat(6) { offset ->
                add(
                    DayEntry(
                        epochDay = start.plusDays(offset.toLong()).toEpochDay(),
                        colorKey = DayColor.GREEN.key,
                        sportLevel = SportLevel.GOOD.key,
                    )
                )
            }
            // Six journees sans sport, plutot difficiles.
            repeat(6) { offset ->
                add(
                    DayEntry(
                        epochDay = start.plusDays((10 + offset).toLong()).toEpochDay(),
                        colorKey = DayColor.RED.key,
                        sportLevel = SportLevel.NONE.key,
                    )
                )
            }
        }

        val insights = Stats.insights(days, emptyList(), emptyList())

        assertEquals(1, insights.size)
        assertEquals("Les jours où tu as bougé", insights.first().label)
        assertEquals(2.0, insights.first().delta, 0.001)
        assertEquals(6, insights.first().withDays)
    }

    @Test
    fun `un facteur avec trop peu de journees est ignore`() {
        val start = LocalDate.of(2026, 2, 1)
        val days = listOf(
            DayEntry(
                epochDay = start.toEpochDay(),
                colorKey = DayColor.GREEN.key,
                foodLevel = FoodLevel.GOOD.key,
            ),
            DayEntry(
                epochDay = start.plusDays(1).toEpochDay(),
                colorKey = DayColor.RED.key,
                foodLevel = FoodLevel.HARD.key,
            ),
        )

        assertTrue(Stats.insights(days, emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun `les etiquettes sont comparees elles aussi`() {
        val start = LocalDate.of(2026, 3, 1)
        val tag = Tag(id = 7, name = "Ami·es")
        val days = mutableListOf<DayEntry>()
        val links = mutableListOf<DayTagCrossRef>()

        repeat(5) { offset ->
            val epochDay = start.plusDays(offset.toLong()).toEpochDay()
            days += DayEntry(epochDay = epochDay, colorKey = DayColor.GREEN.key)
            links += DayTagCrossRef(epochDay = epochDay, tagId = tag.id)
        }
        repeat(5) { offset ->
            days += DayEntry(
                epochDay = start.plusDays((10 + offset).toLong()).toEpochDay(),
                colorKey = DayColor.ORANGE.key,
            )
        }

        val insights = Stats.insights(days, listOf(tag), links)

        assertEquals(1, insights.size)
        assertEquals("Les jours « Ami·es »", insights.first().label)
        assertEquals(1.0, insights.first().delta, 0.001)
    }

    @Test
    fun `une journee vide reste vide meme avec les nouveaux champs`() {
        assertTrue(DayEntry(epochDay = 0).isEmpty)
        assertTrue(!DayEntry(epochDay = 0, sportLevel = SportLevel.NONE.key).isEmpty)
        assertTrue(!DayEntry(epochDay = 0, weightKg = 130.0).isEmpty)
    }
}
