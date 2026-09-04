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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onOpenSearch: () -> Unit,
) {
    val app = LocalContext.current.dayByDayApp
    val repository = app.repository
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
    val todayEntry by remember { repository.observeDay(today) }
        .collectAsStateWithLifecycle(null)

    val monthEntries = entries.values.filter {
        YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) == month
    }
    val monthSummary = Stats.summarize(Dates.monthTitle(month), monthEntries, month.lengthOfMonth())

    val greeting = remember(app.prefs.firstName) {
        val name = app.prefs.firstName.trim()
        if (name.isEmpty()) "DayByDay" else "Salut $name"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(greeting) },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Rechercher")
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
            TodayCard(
                today = today,
                entry = todayEntry,
                onPickColor = { color ->
                    app.appScope.launch {
                        val base = repository.dayOnce(today) ?: DayEntry(epochDay = today.toEpochDay())
                        val next = if (base.colorKey == color.key) null else color.key
                        repository.saveDay(base.copy(colorKey = next))
                    }
                },
                onOpenToday = { onDayClick(today) },
            )

            Spacer(Modifier.height(16.dp))

            MonthHeader(
                month = month,
                onPrevious = { onMonthChange(month.minusMonths(1)) },
                onNext = { onMonthChange(month.plusMonths(1)) },
            )

            WeekDayHeader()

            repeat(weekCount) { weekIndex ->
                WeekRow(
                    weekStart = gridStart.plusDays((weekIndex * 7).toLong()),
                    month = month,
                    today = today,
                    entries = entries,
                    mediaCounts = mediaCounts,
                    onDayClick = onDayClick,
                )
            }

            Spacer(Modifier.height(16.dp))

            SummaryCard(title = "Bilan du mois", summary = monthSummary)

            if (month != YearMonth.from(today)) {
                TextButton(
                    onClick = { onMonthChange(YearMonth.from(today)) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Revenir à aujourd'hui")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TodayCard(
    today: LocalDate,
    entry: DayEntry?,
    onPickColor: (DayColor) -> Unit,
    onOpenToday: () -> Unit,
) {
    SectionCard {
        Text(
            text = "Aujourd'hui · ${Dates.dayMedium(today)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = entry?.color?.label ?: "Comment s'est passée ta journée ?",
            style = MaterialTheme.typography.titleLarge,
        )
        val todayTitle = entry?.title.orEmpty()
        if (todayTitle.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = todayTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DayColor.entries.forEach { dayColor ->
                val selected = entry?.colorKey == dayColor.key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(dayColor.color)
                        .border(
                            BorderStroke(
                                if (selected) 3.dp else 1.dp,
                                if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                },
                            ),
                            RoundedCornerShape(14.dp),
                        )
                        .clickable { onPickColor(dayColor) }
                        .testTag("today-${dayColor.name}"),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Text("✓", color = readableOn(dayColor.color), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        TextButton(onClick = onOpenToday) {
            Text(if (entry == null) "Écrire dans mon journal" else "Ouvrir ma journée")
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
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
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mois suivant")
        }
    }
}

@Composable
private fun WeekDayHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
    ) {
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
    val background = dayColor?.color ?: MaterialTheme.colorScheme.surfaceVariant
    val alpha = when {
        !inMonth -> 0.25f
        isFuture -> 0.55f
        else -> 1f
    }
    val textColor = if (dayColor != null) {
        readableOn(background)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

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
                .clip(RoundedCornerShape(12.dp))
                .background(background.copy(alpha = alpha))
                .then(
                    if (isToday) {
                        Modifier.border(
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                            RoundedCornerShape(12.dp),
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
            val hasTracking = entry != null &&
                (entry.sportLevel != null || entry.foodLevel != null || entry.wentOut != null)
            if (hasText || hasTracking || mediaCount > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (hasText) {
                        Dot(textColor.copy(alpha = 0.85f * alpha), CircleShape)
                    }
                    if (mediaCount > 0) {
                        Dot(textColor.copy(alpha = 0.85f * alpha), RoundedCornerShape(1.dp))
                    }
                    if (hasTracking) {
                        Dot(textColor.copy(alpha = 0.5f * alpha), CircleShape)
                    }
                }
            }
        }
    }
}

@Composable
private fun Dot(color: androidx.compose.ui.graphics.Color, shape: androidx.compose.ui.graphics.Shape) {
    Box(
        modifier = Modifier
            .size(4.dp)
            .clip(shape)
            .background(color),
    )
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
        Text(text = weekNumber.toString(), fontSize = 11.sp, color = textColor)
    }
}
