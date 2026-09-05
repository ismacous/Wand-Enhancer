package com.ismael.daybyday.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ismael.daybyday.data.MoneyCategory
import com.ismael.daybyday.data.MoneyEntry
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** Saisie d'une rentree ou d'une depense, partagee par l'onglet Argent et le journal. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoneyEntryDialog(
    initial: MoneyEntry?,
    defaultDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (MoneyEntry) -> Unit,
    onDelete: (() -> Unit)? = null,
    allowDateChange: Boolean = true,
) {
    val context = LocalContext.current
    var isIncome by remember { mutableStateOf(initial?.isIncome ?: false) }
    var amountText by remember {
        mutableStateOf(
            initial?.let { String.format(Locale.FRANCE, "%.2f", abs(it.amountCents) / 100.0) }
                .orEmpty()
        )
    }
    var label by remember { mutableStateOf(initial?.label.orEmpty()) }
    var category by remember { mutableStateOf(initial?.category) }
    var date by remember { mutableStateOf(defaultDate) }

    val amountCents = amountText.replace(',', '.').toDoubleOrNull()
        ?.let { (it * 100).roundToLong() } ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nouveau mouvement" else "Modifier") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip(
                        label = "➖ Dépense",
                        selected = !isIncome,
                        onClick = {
                            isIncome = false
                            category = null
                        },
                    )
                    ChoiceChip(
                        label = "➕ Rentrée",
                        selected = isIncome,
                        onClick = {
                            isIncome = true
                            category = null
                        },
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        amountText = input.filter { it.isDigit() || it == ',' || it == '.' }.take(9)
                    },
                    label = { Text("Montant") },
                    suffix = { Text("€") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("money-amount"),
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(40) },
                    label = { Text("C'était quoi ?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))

                Text("Catégorie", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val options = if (isIncome) MoneyCategory.incomes() else MoneyCategory.expenses()
                    options.forEach { option ->
                        ChoiceChip(
                            label = "${option.emoji} ${option.label}",
                            selected = category == option,
                            onClick = { category = if (category == option) null else option },
                        )
                    }
                }

                if (allowDateChange) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Le ${Dates.dayMedium(date)}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = {
                            showMoneyDatePicker(context, date) { picked -> date = picked }
                        }) { Text("Changer") }
                    }
                }

                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Supprimer", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = amountCents > 0L,
                onClick = {
                    onSave(
                        (initial ?: MoneyEntry(epochDay = date.toEpochDay(), amountCents = 0)).copy(
                            epochDay = date.toEpochDay(),
                            amountCents = if (isIncome) amountCents else -amountCents,
                            label = label.trim(),
                            categoryKey = category?.key,
                        )
                    )
                },
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

private fun showMoneyDatePicker(
    context: Context,
    current: LocalDate,
    onPicked: (LocalDate) -> Unit,
) {
    android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth -> onPicked(LocalDate.of(year, month + 1, dayOfMonth)) },
        current.year,
        current.monthValue - 1,
        current.dayOfMonth,
    ).show()
}
