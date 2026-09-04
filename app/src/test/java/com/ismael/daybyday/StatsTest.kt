package com.ismael.daybyday

import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
