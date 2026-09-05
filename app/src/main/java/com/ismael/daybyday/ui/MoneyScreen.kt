package com.ismael.daybyday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.MoneyCategory
import com.ismael.daybyday.data.MoneyEntry
import com.ismael.daybyday.data.Stats
import com.ismael.daybyday.dayByDayApp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(onDayClick: (LocalDate) -> Unit) {
    val app = LocalContext.current.dayByDayApp
    val repository = app.repository
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()

    var monthIndex by rememberSaveable {
        mutableIntStateOf(YearMonth.now().year * 12 + YearMonth.now().monthValue - 1)
    }
    val month = YearMonth.of(monthIndex / 12, monthIndex % 12 + 1)

    var editing by remember { mutableStateOf<MoneyEntry?>(null) }
    var creating by remember { mutableStateOf(false) }
    var adjusting by remember { mutableStateOf(false) }

    val balance by remember { repository.observeMoneyBalance() }
        .collectAsStateWithLifecycle(0L)
    val monthEntries by remember(month) {
        repository.observeMoneyBetween(month.atDay(1), month.atEndOfMonth())
    }.collectAsStateWithLifecycle(emptyList())

    val summary = Stats.summarizeMoney(monthEntries)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mon argent") }) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            item {
                SectionCard {
                    Text(
                        "Ce qu'il te reste",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formatMoney(balance),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (balance < 0) DayColor.RED.color else MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { adjusting = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Corriger mon solde")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { monthIndex -= 1 }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Mois précédent",
                        )
                    }
                    Text(
                        text = Dates.monthTitle(month),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { monthIndex += 1 }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Mois suivant",
                        )
                    }
                }
            }

            item {
                SectionCard {
                    MoneyLine("Rentrées", summary.incomeCents, DayColor.GREEN.color)
                    MoneyLine("Dépenses", -summary.spentCents, DayColor.RED.color)
                    Spacer(Modifier.height(6.dp))
                    MoneyLine(
                        label = "Différence du mois",
                        cents = summary.netCents,
                        color = if (summary.netCents < 0) DayColor.RED.color else DayColor.GREEN.color,
                        strong = true,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { creating = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add-money"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Ajouter un mouvement")
                }
                Spacer(Modifier.height(16.dp))
            }

            if (monthEntries.isEmpty()) {
                item {
                    Text(
                        "Aucun mouvement ce mois-ci.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(monthEntries, key = { it.id }) { entry ->
                    MoneyRow(
                        entry = entry,
                        onClick = { editing = entry },
                        onOpenDay = { onDayClick(LocalDate.ofEpochDay(entry.epochDay)) },
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (creating) {
        MoneyEntryDialog(
            initial = null,
            defaultDate = if (month == YearMonth.from(today)) today else month.atDay(1),
            onDismiss = { creating = false },
            onSave = { entry ->
                creating = false
                scope.launch { repository.saveMoney(entry) }
            },
            onDelete = null,
        )
    }

    editing?.let { current ->
        MoneyEntryDialog(
            initial = current,
            defaultDate = LocalDate.ofEpochDay(current.epochDay),
            onDismiss = { editing = null },
            onSave = { entry ->
                editing = null
                scope.launch { repository.saveMoney(entry) }
            },
            onDelete = {
                editing = null
                scope.launch { repository.deleteMoney(current) }
            },
        )
    }

    if (adjusting) {
        AdjustBalanceDialog(
            currentCents = balance,
            onDismiss = { adjusting = false },
            onConfirm = { targetCents ->
                adjusting = false
                val difference = targetCents - balance
                if (difference != 0L) {
                    scope.launch {
                        repository.saveMoney(
                            MoneyEntry(
                                epochDay = today.toEpochDay(),
                                amountCents = difference,
                                label = "Ajustement du solde",
                            )
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun MoneyLine(label: String, cents: Long, color: androidx.compose.ui.graphics.Color, strong: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = formatSignedMoney(cents),
            style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}

@Composable
private fun MoneyRow(entry: MoneyEntry, onClick: () -> Unit, onOpenDay: () -> Unit) {
    SectionCard(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (entry.isIncome) {
                            DayColor.GREEN.color.copy(alpha = 0.2f)
                        } else {
                            DayColor.RED.color.copy(alpha = 0.2f)
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Text(entry.category?.emoji ?: if (entry.isIncome) "➕" else "➖")
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.label.ifBlank { entry.category?.label ?: "Mouvement" },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = Dates.dayMedium(LocalDate.ofEpochDay(entry.epochDay)) +
                        (entry.category?.let { " · ${it.label}" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = onOpenDay),
                )
            }
            Text(
                text = formatSignedMoney(entry.amountCents),
                style = MaterialTheme.typography.titleMedium,
                color = if (entry.isIncome) DayColor.GREEN.color else DayColor.RED.color,
            )
        }
    }
}

@Composable
private fun AdjustBalanceDialog(
    currentCents: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var text by remember {
        mutableStateOf(String.format(java.util.Locale.FRANCE, "%.2f", currentCents / 100.0))
    }
    val target = text.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Corriger mon solde") },
        text = {
            Column {
                Text(
                    "Indique ce que tu as réellement. J'ajoute la différence comme un " +
                        "mouvement « Ajustement » pour que le compte tombe juste.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { input ->
                        text = input.filter { it.isDigit() || it == ',' || it == '.' || it == '-' }.take(10)
                    },
                    label = { Text("Solde réel") },
                    suffix = { Text("€") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().imePadding(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = target != null,
                onClick = { target?.let(onConfirm) },
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
