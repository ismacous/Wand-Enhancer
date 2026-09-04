package com.ismael.daybyday.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.FactorInsight
import com.ismael.daybyday.data.FoodLevel
import com.ismael.daybyday.data.PeriodSummary
import com.ismael.daybyday.data.SportLevel
import com.ismael.daybyday.data.Stats
import com.ismael.daybyday.data.WeightPoint
import com.ismael.daybyday.dayByDayApp
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen() {
    val app = LocalContext.current.dayByDayApp
    val repository = app.repository
    val today = LocalDate.now()
    var year by rememberSaveable { mutableIntStateOf(today.year) }

    val allDays by remember { repository.observeAllDays() }
        .collectAsStateWithLifecycle(emptyList())
    val tags by remember { repository.observeTags() }
        .collectAsStateWithLifecycle(emptyList())
    val dayTags by remember { repository.observeAllDayTags() }
        .collectAsStateWithLifecycle(emptyList())
    val weights by remember { repository.observeWeights() }
        .collectAsStateWithLifecycle(emptyList())

    val yearDays = allDays.filter { LocalDate.ofEpochDay(it.epochDay).year == year }
    val yearLength = if (LocalDate.of(year, 1, 1).isLeapYear) 366 else 365
    val yearSummary = Stats.summarize("Année $year", yearDays, yearLength)
    val allTimeTotalDays = allDays.minOfOrNull { it.epochDay }
        ?.let { (today.toEpochDay() - it + 1).toInt() } ?: 0
    val allTimeSummary = Stats.summarize("Depuis le début", allDays, allTimeTotalDays)
    val (currentStreak, longestStreak) = Stats.streaks(allDays, today)

    val insights = remember(allDays, tags, dayTags) { Stats.insights(allDays, tags, dayTags) }

    val weekSummaries = remember(yearDays) { weeklySummaries(yearDays) }
    val bestWeek = weekSummaries.filter { it.second.filledDays >= 3 }
        .maxByOrNull { it.second.average ?: -1.0 }
    val hardestWeek = weekSummaries.filter { it.second.filledDays >= 3 }
        .minByOrNull { it.second.average ?: 99.0 }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mon bilan") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { year -= 1 }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Année précédente",
                    )
                }
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = { year += 1 }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Année suivante",
                    )
                }
            }

            SummaryCard(title = "Bilan de l'année", summary = yearSummary)

            Spacer(Modifier.height(16.dp))

            InsightsCard(insights)

            Spacer(Modifier.height(16.dp))

            HabitsCard(yearDays)

            Spacer(Modifier.height(16.dp))

            WeightCard(weights, app.prefs.bodyMassIndex(weights.lastOrNull()?.weightKg))

            Spacer(Modifier.height(16.dp))

            SectionCard(title = "Mois par mois") {
                (1..12).forEach { monthValue ->
                    val month = YearMonth.of(year, monthValue)
                    val monthDays = yearDays.filter {
                        LocalDate.ofEpochDay(it.epochDay).monthValue == monthValue
                    }
                    val summary = Stats.summarize(
                        Dates.monthShort(month),
                        monthDays,
                        month.lengthOfMonth(),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = Dates.monthShort(month),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(48.dp),
                        )
                        DistributionBar(
                            summary = summary,
                            modifier = Modifier.weight(1f),
                            height = 12,
                        )
                        Spacer(Modifier.width(10.dp))
                        AverageChip(summary.average)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            SectionCard(title = "Semaines marquantes") {
                if (bestWeek == null && hardestWeek == null) {
                    Text(
                        "Pas encore assez de jours notés cette année.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    bestWeek?.let { (weekStart, summary) ->
                        WeekLine("Ta meilleure semaine", weekStart, summary)
                    }
                    hardestWeek?.let { (weekStart, summary) ->
                        Spacer(Modifier.height(12.dp))
                        WeekLine("Ta semaine la plus dure", weekStart, summary)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            SectionCard(title = "Régularité") {
                StatLine("Jours notés en tout", allTimeSummary.filledDays.toString())
                StatLine("Série en cours", "$currentStreak jour(s)")
                StatLine("Plus longue série", "$longestStreak jour(s)")
                allTimeSummary.average?.let {
                    StatLine("Moyenne depuis le début", formatAverage(it))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InsightsCard(insights: List<FactorInsight>) {
    SectionCard(title = "Ce qui va avec tes bonnes journées") {
        if (insights.isEmpty()) {
            Text(
                "Continue à remplir le sport, les repas, les sorties et les étiquettes : " +
                    "dès que tu auras assez de journées, tu verras ici ce qui revient " +
                    "dans tes bons jours.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@SectionCard
        }
        insights.take(6).forEach { insight ->
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        insight.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = String.format(Locale.FRANCE, "%+.1f", insight.delta),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (insight.delta >= 0) {
                            DayColor.GREEN.color
                        } else {
                            DayColor.RED.color
                        },
                    )
                }
                Text(
                    text = "${formatAverage(insight.withAverage)} sur ${insight.withDays} jour(s) " +
                        "· ${formatAverage(insight.withoutAverage)} sur ${insight.withoutDays} jour(s) sans",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "C'est une observation, pas une explication : ça montre ce qui accompagne " +
                "tes bonnes journées.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HabitsCard(yearDays: List<DayEntry>) {
    val movedDays = yearDays.count { (it.sportLevel ?: -1) >= SportLevel.LIGHT.key }
    val realSessions = yearDays.count { it.sportLevel == SportLevel.GOOD.key }
    val outDays = yearDays.count { it.wentOut == true }
    val goodFood = yearDays.count { it.foodLevel == FoodLevel.GOOD.key }
    val sportFilled = yearDays.count { it.sportLevel != null }

    SectionCard(title = "Bouger, manger, sortir") {
        StatLine("Jours où tu as bougé", "$movedDays jour(s)")
        StatLine("Vraies séances de sport", "$realSessions")
        StatLine("Jours où tu es sorti", "$outDays jour(s)")
        StatLine("Jours « bien mangé »", "$goodFood jour(s)")
        if (sportFilled == 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Remplis la partie « Ta journée en détail » pour voir ces chiffres bouger.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeightCard(points: List<WeightPoint>, bmi: Double?) {
    SectionCard(title = "Poids") {
        if (points.isEmpty()) {
            Text(
                "Note ton poids quand tu veux dans une journée : la courbe apparaîtra ici.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@SectionCard
        }

        val first = points.first()
        val last = points.last()
        StatLine("Dernier poids", formatWeight(last.weightKg))
        if (points.size > 1) {
            StatLine(
                "Depuis le ${Dates.dayShort(LocalDate.ofEpochDay(first.epochDay))}",
                formatSignedKg(last.weightKg - first.weightKg),
            )
        }
        bmi?.let { StatLine("IMC", String.format(Locale.FRANCE, "%.1f", it)) }

        if (points.size >= 2) {
            Spacer(Modifier.height(12.dp))
            WeightSparkline(
                points = points.takeLast(120),
                lineColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            )
        }
    }
}

@Composable
private fun WeightSparkline(
    points: List<WeightPoint>,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    val minWeight = points.minOf { it.weightKg }
    val maxWeight = points.maxOf { it.weightKg }
    val span = (maxWeight - minWeight).takeIf { it > 0.5 } ?: 1.0
    val minDay = points.first().epochDay
    val dayRange = (points.last().epochDay - minDay).takeIf { it > 0 } ?: 1

    Canvas(modifier = modifier) {
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = ((point.epochDay - minDay).toFloat() / dayRange) * size.width
            val ratio = ((point.weightKg - minWeight) / span).toFloat()
            val y = size.height - ratio * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = lineColor, style = Stroke(width = 4f))
        points.forEach { point ->
            val x = ((point.epochDay - minDay).toFloat() / dayRange) * size.width
            val ratio = ((point.weightKg - minWeight) / span).toFloat()
            val y = size.height - ratio * size.height
            drawCircle(color = lineColor, radius = 5f, center = Offset(x, y))
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun WeekLine(label: String, weekStart: LocalDate, summary: PeriodSummary) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelLarge)
                Text(
                    "Semaine ${Stats.weekNumber(weekStart)} — du ${Dates.dayMedium(weekStart)} " +
                        "au ${Dates.dayMedium(weekStart.plusDays(6))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AverageChip(summary.average)
        }
        Spacer(Modifier.height(6.dp))
        DistributionBar(summary)
    }
}

/** Regroupe les jours d'une annee par semaine (du lundi au dimanche). */
private fun weeklySummaries(days: List<DayEntry>): List<Pair<LocalDate, PeriodSummary>> =
    days.groupBy { entry ->
        val date = LocalDate.ofEpochDay(entry.epochDay)
        date.minusDays((date.dayOfWeek.value - 1).toLong())
    }.map { (weekStart, entries) ->
        weekStart to Stats.summarize("Semaine", entries, 7)
    }.sortedBy { it.first }
