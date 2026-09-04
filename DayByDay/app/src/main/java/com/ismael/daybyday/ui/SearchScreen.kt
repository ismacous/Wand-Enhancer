package com.ismael.daybyday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.dayByDayApp
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    val repository = LocalContext.current.dayByDayApp.repository
    var query by rememberSaveable { mutableStateOf("") }

    val results by remember(query) {
        if (query.trim().length >= 2) repository.search(query.trim()) else flowOf(emptyList())
    }.collectAsStateWithLifecycle(emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rechercher") },
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
                .imePadding()
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Un mot, un prénom, un lieu…") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search-field"),
            )

            Spacer(Modifier.height(12.dp))

            when {
                query.trim().length < 2 -> Text(
                    "Tape au moins deux lettres pour chercher dans tes titres et tes notes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                results.isEmpty() -> Text(
                    "Aucune journée ne contient « ${query.trim()} ».",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> {
                    Text(
                        "${results.size} journée(s) trouvée(s)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn {
                        items(results, key = { it.epochDay }) { entry ->
                            ResultRow(
                                entry = entry,
                                query = query.trim(),
                                onClick = { onDayClick(LocalDate.ofEpochDay(entry.epochDay)) },
                            )
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(entry: DayEntry, query: String, onClick: () -> Unit) {
    val date = LocalDate.ofEpochDay(entry.epochDay)
    SectionCard(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        entry.color?.color
                            ?: MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(Dates.dayMedium(date), style = MaterialTheme.typography.labelLarge)
                if (entry.title.isNotBlank()) {
                    Text(entry.title, style = MaterialTheme.typography.bodyLarge)
                }
                val snippet = snippetAround(entry.note, query)
                if (snippet.isNotBlank()) {
                    Text(
                        snippet,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Extrait le morceau de note autour du mot recherche. */
private fun snippetAround(note: String, query: String, radius: Int = 60): String {
    if (note.isBlank()) return ""
    val index = note.indexOf(query, ignoreCase = true)
    if (index < 0) return note.take(radius * 2).replace('\n', ' ')
    val start = (index - radius).coerceAtLeast(0)
    val end = (index + query.length + radius).coerceAtMost(note.length)
    val prefix = if (start > 0) "…" else ""
    val suffix = if (end < note.length) "…" else ""
    return prefix + note.substring(start, end).replace('\n', ' ') + suffix
}
