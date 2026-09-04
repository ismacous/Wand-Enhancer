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

/**
 * Comparaison entre les journees ou un facteur est present et celles ou il ne
 * l'est pas. Purement descriptif : ca montre ce qui accompagne les bonnes
 * journees, pas ce qui les cause.
 */
data class FactorInsight(
    val label: String,
    val withAverage: Double,
    val withoutAverage: Double,
    val withDays: Int,
    val withoutDays: Int,
) {
    val delta: Double get() = withAverage - withoutAverage
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

    /** Numero de semaine (lundi -> dimanche) pour l'affichage. */
    fun weekNumber(date: LocalDate): Int = date.get(WEEK_FIELDS.weekOfWeekBasedYear())

    /** Serie en cours et plus longue serie de jours consecutifs notes. */
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

    private fun averageScore(entries: Collection<DayEntry>): Double? {
        val colors = entries.mapNotNull { it.color }
        return if (colors.isEmpty()) null else colors.sumOf { it.score }.toDouble() / colors.size
    }

    private fun compare(
        label: String,
        withGroup: List<DayEntry>,
        withoutGroup: List<DayEntry>,
        minDays: Int,
    ): FactorInsight? {
        if (withGroup.size < minDays || withoutGroup.size < minDays) return null
        val withAverage = averageScore(withGroup) ?: return null
        val withoutAverage = averageScore(withoutGroup) ?: return null
        return FactorInsight(
            label = label,
            withAverage = withAverage,
            withoutAverage = withoutAverage,
            withDays = withGroup.size,
            withoutDays = withoutGroup.size,
        )
    }

    /**
     * Compare l'humeur moyenne selon le sport, l'alimentation, les sorties et
     * chaque etiquette. Ne renvoie que les facteurs avec assez de journees des
     * deux cotes pour que la comparaison veuille dire quelque chose.
     */
    fun insights(
        days: List<DayEntry>,
        tags: List<Tag>,
        links: List<DayTagCrossRef>,
        minDays: Int = 4,
    ): List<FactorInsight> {
        val colored = days.filter { it.colorKey != null }
        val results = mutableListOf<FactorInsight>()

        val sportDays = colored.filter { it.sportLevel != null }
        compare(
            label = "Les jours où tu as bougé",
            withGroup = sportDays.filter { (it.sportLevel ?: 0) >= SportLevel.LIGHT.key },
            withoutGroup = sportDays.filter { it.sportLevel == SportLevel.NONE.key },
            minDays = minDays,
        )?.let(results::add)

        val foodDays = colored.filter { it.foodLevel != null }
        compare(
            label = "Les jours où tu as bien mangé",
            withGroup = foodDays.filter { it.foodLevel == FoodLevel.GOOD.key },
            withoutGroup = foodDays.filter { it.foodLevel != FoodLevel.GOOD.key },
            minDays = minDays,
        )?.let(results::add)

        val outDays = colored.filter { it.wentOut != null }
        compare(
            label = "Les jours où tu es sorti",
            withGroup = outDays.filter { it.wentOut == true },
            withoutGroup = outDays.filter { it.wentOut == false },
            minDays = minDays,
        )?.let(results::add)

        val taggedDays = links.groupBy({ it.tagId }, { it.epochDay })
        val byEpochDay = colored.associateBy { it.epochDay }
        tags.forEach { tag ->
            val withEpochDays = taggedDays[tag.id].orEmpty().toSet()
            val withGroup = withEpochDays.mapNotNull { byEpochDay[it] }
            val withoutGroup = colored.filter { it.epochDay !in withEpochDays }
            compare(
                label = "Les jours « ${tag.name} »",
                withGroup = withGroup,
                withoutGroup = withoutGroup,
                minDays = minDays,
            )?.let(results::add)
        }

        return results.sortedByDescending { kotlin.math.abs(it.delta) }
    }
}
