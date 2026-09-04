package com.ismael.daybyday.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.MediaItem
import com.ismael.daybyday.data.MediaKind
import com.ismael.daybyday.dayByDayApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val SAVE_DEBOUNCE_MS = 400L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScreen(
    initialDate: LocalDate,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.dayByDayApp
    val repository = app.repository
    val scope = rememberCoroutineScope()

    var epochDay by rememberSaveable { mutableLongStateOf(initialDate.toEpochDay()) }
    val date = LocalDate.ofEpochDay(epochDay)

    var colorKey by remember { mutableStateOf<Int?>(null) }
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var loadedFor by remember { mutableStateOf<Long?>(null) }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

    val mediaItems by remember(epochDay) { repository.observeMediaForDay(date) }
        .collectAsStateWithLifecycle(emptyList())

    // Chargement du contenu de la journee affichee.
    LaunchedEffect(epochDay) {
        loadedFor = null
        val entry = repository.observeDay(LocalDate.ofEpochDay(epochDay)).first()
        colorKey = entry?.colorKey
        title = entry?.title.orEmpty()
        note = entry?.note.orEmpty()
        loadedFor = epochDay
    }

    // Sauvegarde automatique, un court instant apres la derniere modification.
    LaunchedEffect(epochDay, loadedFor, colorKey, title, note) {
        if (loadedFor != epochDay) return@LaunchedEffect
        delay(SAVE_DEBOUNCE_MS)
        repository.saveDay(LocalDate.ofEpochDay(epochDay), colorKey, title, note)
    }

    // Sauvegarde immediate quand on quitte l'ecran (ou qu'on change de jour).
    DisposableEffect(epochDay, loadedFor) {
        // Le jour est fige ici : au moment du onDispose, epochDay peut deja
        // pointer vers le jour suivant alors que les champs contiennent encore
        // le texte du jour precedent.
        val dayOfThisEffect = epochDay
        val contentIsLoaded = loadedFor == epochDay
        onDispose {
            if (contentIsLoaded) {
                val savedColor = colorKey
                val savedTitle = title
                val savedNote = note
                app.appScope.launch {
                    repository.saveDay(
                        LocalDate.ofEpochDay(dayOfThisEffect),
                        savedColor,
                        savedTitle,
                        savedNote,
                    )
                }
            }
        }
    }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(30)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val targetDate = LocalDate.ofEpochDay(epochDay)
            app.appScope.launch {
                uris.forEach { uri -> repository.addMedia(targetDate, uri) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ma journée") },
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
                IconButton(onClick = { epochDay -= 1 }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Jour précédent")
                }
                Text(
                    text = Dates.dayLong(date),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { epochDay += 1 }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Jour suivant")
                }
            }

            if (date != LocalDate.now()) {
                TextButton(
                    onClick = { epochDay = LocalDate.now().toEpochDay() },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Aller à aujourd'hui")
                }
            }

            Spacer(Modifier.height(8.dp))

            Text("Couleur du jour", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DayColor.entries.forEach { dayColor ->
                    ColorChoice(
                        dayColor = dayColor,
                        selected = colorKey == dayColor.key,
                        onClick = {
                            colorKey = if (colorKey == dayColor.key) null else dayColor.key
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = DayColor.fromKey(colorKey)?.label ?: "Aucune couleur pour l'instant",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre de la journée") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("day-title-field"),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Journal") },
                placeholder = { Text("Ce que tu as vécu, ressenti, ce qui a aidé…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp),
            )

            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Photos & vidéos",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = {
                    pickMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Ajouter")
                }
            }

            Spacer(Modifier.height(10.dp))

            if (mediaItems.isEmpty()) {
                Text(
                    "Aucun média pour ce jour. Les fichiers ajoutés sont copiés dans " +
                        "l'espace privé de l'application.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                mediaItems.chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowItems.forEach { item ->
                            MediaThumb(
                                item = item,
                                onClick = { viewerIndex = mediaItems.indexOf(item) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - rowItems.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    val index = viewerIndex
    if (index != null && index in mediaItems.indices) {
        MediaViewerDialog(
            items = mediaItems,
            startIndex = index,
            onDismiss = { viewerIndex = null },
            onDelete = { item ->
                viewerIndex = null
                scope.launch { repository.deleteMedia(item) }
            },
        )
    }
}

@Composable
private fun ColorChoice(
    dayColor: DayColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .testTag("color-${dayColor.name}")
                .clip(RoundedCornerShape(16.dp))
                .background(dayColor.color)
                .border(
                    BorderStroke(
                        if (selected) 3.dp else 1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    ),
                    RoundedCornerShape(16.dp),
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Sélectionné",
                    tint = readableOn(dayColor.color),
                )
            }
        }
    }
}

@Composable
private fun MediaThumb(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val repository = LocalContext.current.dayByDayApp.repository
    val file = remember(item.id) { repository.media.file(item.relativePath) }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        MediaImage(
            file = file,
            kind = item.kind,
            modifier = Modifier.fillMaxSize(),
            maxSize = 512,
        )
        if (item.kind == MediaKind.VIDEO) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Vidéo",
                    tint = Color.White,
                )
            }
        }
    }
}
