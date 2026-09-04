package com.ismael.daybyday.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.Stats
import com.ismael.daybyday.dayByDayApp
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onOpenYear: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val repository = LocalContext.current.dayByDayApp.repository
    val today = LocalDate.now()

    val gridStart = remember(month) {
        val first = month.atDay(1)
        first.minusDays((first.dayOfWeek.value - 1).toLong())
    }
    val weekCount = remember(month) {
        val offset = month.atDay(1).dayOfWeek.value - 1
        ((offset + month.lengthOfMonth() + 6) / 7)
    }
    val gridEnd = remember(month) { gridStart.plusDays((weekCount * 7 - 1).toLong()) }

    val entries by remember(month) { repository.observeDaysBetween(gridStart, gridEnd) }
        .collectAsStateWithLifecycle(emptyMap())
    val mediaCounts by remember(month) { repository.observeMediaCounts(gridStart, gridEnd) }
        .collectAsStateWithLifecycle(emptyMap())

    val monthEntries = entries.values.filter {
        YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) == month
    }
    val monthSummary = Stats.summarize(
        Dates.monthTitle(month),
        monthEntries,
        month.lengthOfMonth(),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DayByDay") },
                actions = {
                    IconButton(onClick = onOpenYear) {
                        Icon(Icons.Default.DateRange, contentDescription = "Vue année")
                    }
                    IconButton(onClick = onOpenStats) {
                        Icon(Icons.Default.Info, contentDescription = "Statistiques")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Réglages")
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
                .padding(horizontal = 12.dp),
        ) {
            MonthHeader(
                month = month,
                onPrevious = { onMonthChange(month.minusMonths(1)) },
                onNext = { onMonthChange(month.plusMonths(1)) },
                onTitleClick = onOpenYear,
            )

            WeekDayHeader()

            repeat(weekCount) { weekIndex ->
                val weekStart = gridStart.plusDays((weekIndex * 7).toLong())
                WeekRow(
                    weekStart = weekStart,
                    month = month,
                    today = today,
                    entries = entries,
                    mediaCounts = mediaCounts,
                    onDayClick = onDayClick,
                )
            }

            Spacer(Modifier.height(16.dp))

            SummaryCard(title = "Bilan du mois", summary = monthSummary)

            Spacer(Modifier.height(12.dp))

            Legend()

            if (month != YearMonth.from(today)) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { onMonthChange(YearMonth.from(today)) }) {
                    Text("Revenir à aujourd'hui")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTitleClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mois précédent")
        }
        Text(
            text = Dates.monthTitle(month),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onTitleClick)
                .padding(vertical = 6.dp),
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mois suivant")
        }
    }
}

@Composable
private fun WeekDayHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Dates.weekDayInitials.forEach { initial ->
            Text(
                text = initial,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(modifier = Modifier.width(WEEK_COLUMN_WIDTH)) {
            Text(
                text = "sem.",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val WEEK_COLUMN_WIDTH = 34.dp

@Composable
private fun WeekRow(
    weekStart: LocalDate,
    month: YearMonth,
    today: LocalDate,
    entries: Map<Long, DayEntry>,
    mediaCounts: Map<Long, Int>,
    onDayClick: (LocalDate) -> Unit,
) {
    val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }
    val weekEntries = weekDays.mapNotNull { entries[it.toEpochDay()] }
    val weekSummary = Stats.summarize("Semaine", weekEntries, 7)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        weekDays.forEach { date ->
            DayCell(
                date = date,
                entry = entries[date.toEpochDay()],
                mediaCount = mediaCounts[date.toEpochDay()] ?: 0,
                inMonth = YearMonth.from(date) == month,
                isToday = date == today,
                isFuture = date.isAfter(today),
                onClick = { onDayClick(date) },
                modifier = Modifier.weight(1f),
            )
        }
        WeekScore(
            weekNumber = Stats.weekNumber(weekStart),
            average = weekSummary.average,
        )
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    entry: DayEntry?,
    mediaCount: Int,
    inMonth: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dayColor = entry?.color
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val background = when {
        dayColor != null -> dayColor.color
        else -> emptyColor
    }
    val alpha = when {
        !inMonth -> 0.25f
        isFuture -> 0.55f
        else -> 1f
    }
    val textColor = if (dayColor != null) readableOn(background) else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .testTag("day-${date.toEpochDay()}"),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .background(background.copy(alpha = alpha))
                .then(
                    if (isToday) {
                        Modifier.border(
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                            RoundedCornerShape(10.dp),
                        )
                    } else {
                        Modifier
                    }
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = textColor.copy(alpha = alpha),
            )

            val hasText = entry != null && (entry.title.isNotBlank() || entry.note.isNotBlank())
            if (hasText || mediaCount > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (hasText) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(textColor.copy(alpha = 0.85f * alpha)),
                        )
                    }
                    if (mediaCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(textColor.copy(alpha = 0.85f * alpha)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekScore(weekNumber: Int, average: Double?) {
    val background = average?.let { DayColor.fromAverage(it) }
        ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val textColor = if (average == null) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    } else {
        readableOn(background)
    }
    Box(
        modifier = Modifier
            .width(WEEK_COLUMN_WIDTH)
            .padding(vertical = 2.dp, horizontal = 4.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = weekNumber.toString(),
            fontSize = 11.sp,
            color = textColor,
        )
    }
}

@Composable
private fun Legend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DayColor.entries.forEach { color ->
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color.color)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), CircleShape),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = shortLabel(color),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun shortLabel(color: DayColor): String = when (color) {
    DayColor.GREEN -> "Bonne"
    DayColor.ORANGE -> "Mitigée"
    DayColor.RED -> "Difficile"
    DayColor.BLACK -> "Très noire"
}
