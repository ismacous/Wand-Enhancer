package com.ismael.daybyday.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.PeriodSummary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object Dates {
    private val MONTH_TITLE = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.FRANCE)
    private val DAY_LONG = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRANCE)
    private val DAY_MEDIUM = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE)
    private val DAY_SHORT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRANCE)
    private val MONTH_SHORT = DateTimeFormatter.ofPattern("LLL", Locale.FRANCE)

    fun monthTitle(month: YearMonth): String =
        month.format(MONTH_TITLE).replaceFirstChar { it.uppercase() }

    fun monthShort(month: YearMonth): String =
        month.format(MONTH_SHORT).replaceFirstChar { it.uppercase() }.removeSuffix(".")

    fun dayLong(date: LocalDate): String =
        date.format(DAY_LONG).replaceFirstChar { it.uppercase() }

    fun dayMedium(date: LocalDate): String =
        date.format(DAY_MEDIUM).replaceFirstChar { it.uppercase() }

    fun dayShort(date: LocalDate): String = date.format(DAY_SHORT)

    val weekDayInitials = listOf("L", "M", "M", "J", "V", "S", "D")
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

fun formatAverage(average: Double?): String =
    average?.let { String.format(Locale.FRANCE, "%.1f", it) + " / 3" } ?: "—"

fun formatWeight(weightKg: Double?): String =
    weightKg?.let { String.format(Locale.FRANCE, "%.1f kg", it) } ?: "—"

fun formatSignedKg(delta: Double): String =
    String.format(Locale.FRANCE, "%+.1f kg", delta)

/** Noir ou blanc selon la luminosite du fond, pour rester lisible. */
fun readableOn(background: Color): Color {
    val luminance = 0.299f * background.red + 0.587f * background.green + 0.114f * background.blue
    return if (luminance > 0.6f) Color(0xFF101318) else Color.White
}

@Composable
fun AverageChip(average: Double?, modifier: Modifier = Modifier) {
    val background = average?.let { DayColor.fromAverage(it) } ?: MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (average == null) MaterialTheme.colorScheme.onSurfaceVariant else readableOn(background)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = formatAverage(average),
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
        )
    }
}

/** Petite pastille selectionnable, utilisee pour le sport, les repas, les tags. */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
fun DistributionBar(summary: PeriodSummary, modifier: Modifier = Modifier, height: Int = 10) {
    val total = DayColor.entries.sumOf { summary.countOf(it) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    ) {
        if (total == 0) return@Row
        DayColor.entries.forEach { color ->
            val count = summary.countOf(color)
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .weight(count.toFloat())
                        .fillMaxWidth()
                        .background(color.color),
                )
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    summary: PeriodSummary,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${summary.filledDays} jour(s) noté(s) sur ${summary.totalDays}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AverageChip(summary.average)
        }
        Spacer(Modifier.height(12.dp))
        DistributionBar(summary)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DayColor.entries.forEach { color ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color.color),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        summary.countOf(color).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
