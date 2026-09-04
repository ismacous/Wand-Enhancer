package com.ismael.daybyday.ui

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.PeriodSummary
import com.ismael.daybyday.data.Stats
import com.ismael.daybyday.dayByDayApp
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val repository = LocalContext.current.dayByDayApp.repository
    val today = LocalDate.now()
    var year by rememberSaveable { mutableIntStateOf(today.year) }

    val allDays by remember { repository.observeAllDays() }
        .collectAsStateWithLifecycle(emptyList())

    val yearDays = allDays.filter { LocalDate.ofEpochDay(it.epochDay).year == year }
    val yearLength = if (LocalDate.of(year, 1, 1).isLeapYear) 366 else 365
    val yearSummary = Stats.summarize("Année $year", yearDays, yearLength)
    val allTimeTotalDays = allDays.minOfOrNull { it.epochDay }
        ?.let { (today.toEpochDay() - it + 1).toInt() } ?: 0
    val allTimeSummary = Stats.summarize("Depuis le début", allDays, allTimeTotalDays)
    val (currentStreak, longestStreak) = Stats.streaks(allDays, today)

    val weekSummaries = remember(yearDays) { weeklySummaries(yearDays) }
    val bestWeek = weekSummaries.filter { it.second.filledDays >= 3 }
        .maxByOrNull { it.second.average ?: -1.0 }
    val hardestWeek = weekSummaries.filter { it.second.filledDays >= 3 }
        .minByOrNull { it.second.average ?: 99.0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistiques") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
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
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Année précédente")
                }
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                IconButton(onClick = { year += 1 }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Année suivante")
                }
            }

            SummaryCard(title = "Bilan de l'année", summary = yearSummary)

            Spacer(Modifier.height(16.dp))
            Text("Mois par mois", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

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

            Spacer(Modifier.height(16.dp))
            Text("Semaines marquantes", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            InfoCard {
                if (bestWeek == null && hardestWeek == null) {
                    Text(
                        "Pas encore assez de jours notés cette année.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    bestWeek?.let { (weekStart, summary) ->
                        WeekLine("Meilleure semaine", weekStart, summary)
                    }
                    hardestWeek?.let { (weekStart, summary) ->
                        Spacer(Modifier.height(8.dp))
                        WeekLine("Semaine la plus dure", weekStart, summary)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Régularité", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            InfoCard {
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
private fun InfoCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
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
