package com.ismael.daybyday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.Stats
import com.ismael.daybyday.dayByDayApp
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearScreen(
    year: Int,
    onYearChange: (Int) -> Unit,
    onMonthClick: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    val repository = LocalContext.current.dayByDayApp.repository
    val today = LocalDate.now()

    val start = remember(year) { LocalDate.of(year, 1, 1) }
    val end = remember(year) { LocalDate.of(year, 12, 31) }
    val entries by remember(year) { repository.observeDaysBetween(start, end) }
        .collectAsStateWithLifecycle(emptyMap())

    val yearSummary = Stats.summarize(
        year.toString(),
        entries.values,
        if (start.isLeapYear) 366 else 365,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Année $year") },
                actions = {
                    IconButton(onClick = { onYearChange(year - 1) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Année précédente",
                        )
                    }
                    IconButton(onClick = { onYearChange(year + 1) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Année suivante",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
        ) {
            item {
                SummaryCard(title = "Bilan de l'année $year", summary = yearSummary)
                Spacer(Modifier.height(12.dp))
            }

            items((1..12).toList().chunked(2)) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    pair.forEach { monthValue ->
                        MiniMonth(
                            month = YearMonth.of(year, monthValue),
                            entries = entries,
                            today = today,
                            onHeaderClick = { onMonthClick(YearMonth.of(year, monthValue)) },
                            onDayClick = onDayClick,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun MiniMonth(
    month: YearMonth,
    entries: Map<Long, DayEntry>,
    today: LocalDate,
    onHeaderClick: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val first = month.atDay(1)
    val offset = first.dayOfWeek.value - 1
    val weekCount = (offset + month.lengthOfMonth() + 6) / 7
    val gridStart = first.minusDays(offset.toLong())

    val monthEntries = (0 until month.lengthOfMonth())
        .mapNotNull { entries[first.plusDays(it.toLong()).toEpochDay()] }
    val summary = Stats.summarize(Dates.monthTitle(month), monthEntries, month.lengthOfMonth())

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onHeaderClick)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = Dates.monthShort(month),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                AverageChip(summary.average)
            }

            Spacer(Modifier.height(6.dp))

            repeat(weekCount) { weekIndex ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(7) { dayIndex ->
                        val date = gridStart.plusDays((weekIndex * 7 + dayIndex).toLong())
                        val inMonth = YearMonth.from(date) == month
                        val entry = entries[date.toEpochDay()]
                        val color = entry?.color?.color
                            ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(1.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (inMonth) color else androidx.compose.ui.graphics.Color.Transparent
                                    )
                                    .then(
                                        if (inMonth) {
                                            Modifier.clickable { onDayClick(date) }
                                        } else {
                                            Modifier
                                        }
                                    ),
                            )
                            if (date == today) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.Center),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
