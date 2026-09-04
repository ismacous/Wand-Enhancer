package com.ismael.daybyday.data

import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

/** Resume chiffre d'une periode (semaine, mois, annee). */
data class PeriodSummary(
    val label: String,
    val average: Double?,
    val filledDays: Int,
    val totalDays: Int,
    val counts: Map<DayColor, Int>,
) {
    val hasData: Boolean get() = filledDays > 0

    fun countOf(color: DayColor): Int = counts[color] ?: 0

    /** Pourcentage de la periode reellement notee. */
    val coverage: Float get() = if (totalDays == 0) 0f else filledDays.toFloat() / totalDays
}

object Stats {

    val WEEK_FIELDS: WeekFields = WeekFields.of(Locale.FRANCE)

    fun summarize(label: String, entries: Collection<DayEntry>, totalDays: Int): PeriodSummary {
        val colored = entries.mapNotNull { it.color }
        val counts = DayColor.entries.associateWith { color -> colored.count { it == color } }
        val average = if (colored.isEmpty()) null else colored.sumOf { it.score }.toDouble() / colored.size
        return PeriodSummary(
            label = label,
            average = average,
            filledDays = colored.size,
            totalDays = totalDays,
            counts = counts,
        )
    }

    /** Numero de semaine ISO-like (lundi -> dimanche) pour l'affichage. */
    fun weekNumber(date: LocalDate): Int = date.get(WEEK_FIELDS.weekOfWeekBasedYear())

    /** Plus longue serie de jours consecutifs notes, en terminant par la serie en cours. */
    fun streaks(entries: List<DayEntry>, today: LocalDate): Pair<Int, Int> {
        val days = entries.filter { it.colorKey != null }.map { it.epochDay }.toSortedSet()
        if (days.isEmpty()) return 0 to 0

        var longest = 0
        var running = 0
        var previous: Long? = null
        for (day in days) {
            running = if (previous != null && day == previous + 1) running + 1 else 1
            if (running > longest) longest = running
            previous = day
        }

        var current = 0
        var cursor = today.toEpochDay()
        if (!days.contains(cursor)) cursor -= 1
        while (days.contains(cursor)) {
            current += 1
            cursor -= 1
        }
        return current to longest
    }
}
