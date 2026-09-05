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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.DayPart
import com.ismael.daybyday.data.FoodLevel
import com.ismael.daybyday.data.MediaItem
import com.ismael.daybyday.data.MediaKind
import com.ismael.daybyday.data.MoneyEntry
import com.ismael.daybyday.data.SportLevel
import com.ismael.daybyday.data.TagCategory
import com.ismael.daybyday.dayByDayApp
import com.ismael.daybyday.health.HealthConnectSource
import com.ismael.daybyday.health.ScreenTimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale

private const val SAVE_DEBOUNCE_MS = 400L

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val birthday = app.prefs.birthDate
    val isBirthday = date.dayOfMonth == birthday.dayOfMonth && date.monthValue == birthday.monthValue

    var colorKey by remember { mutableStateOf<Int?>(null) }
    var colorManual by remember { mutableStateOf(false) }
    var parts by remember { mutableStateOf<Map<DayPart, Int>>(emptyMap()) }
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var sportLevel by remember { mutableStateOf<Int?>(null) }
    var foodLevel by remember { mutableStateOf<Int?>(null) }
    var wentOut by remember { mutableStateOf<Boolean?>(null) }
    var weightText by remember { mutableStateOf("") }
    var stepsValue by remember { mutableStateOf<Int?>(null) }
    var screenValue by remember { mutableStateOf<Int?>(null) }
    var loadedFor by remember { mutableStateOf<Long?>(null) }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    var addingMoney by remember { mutableStateOf(false) }
    var editingMoney by remember { mutableStateOf<MoneyEntry?>(null) }

    val mediaItems by remember(epochDay) { repository.observeMediaForDay(date) }
        .collectAsStateWithLifecycle(emptyList())
    val allTags by remember { repository.observeTags() }
        .collectAsStateWithLifecycle(emptyList())
    val dayTags by remember(epochDay) { repository.observeTagsForDay(date) }
        .collectAsStateWithLifecycle(emptyList())
    val dayMoney by remember(epochDay) { repository.observeMoneyBetween(date, date) }
        .collectAsStateWithLifecycle(emptyList())
    val selectedTagIds = dayTags.map { it.id }.toSet()

    fun currentEntry(day: Long) = DayEntry(
        epochDay = day,
        colorKey = colorKey,
        title = title.trim(),
        note = note,
        sportLevel = sportLevel,
        foodLevel = foodLevel,
        wentOut = wentOut,
        weightKg = weightText.replace(',', '.').toDoubleOrNull(),
        steps = stepsValue,
        screenMinutes = screenValue,
        partMorning = parts[DayPart.MORNING],
        partAfternoon = parts[DayPart.AFTERNOON],
        partEvening = parts[DayPart.EVENING],
        partNight = parts[DayPart.NIGHT],
        colorManual = colorManual,
    )

    /** Applique la couleur d'un moment, puis recalcule la couleur du jour. */
    fun setPart(part: DayPart, key: Int?) {
        parts = if (key == null) parts - part else parts + (part to key)
        if (!colorManual) colorKey = averageColorKey(parts.values)
    }

    LaunchedEffect(epochDay) {
        loadedFor = null
        val entry = repository.observeDay(LocalDate.ofEpochDay(epochDay)).first()
        colorKey = entry?.colorKey
        colorManual = entry?.colorManual ?: (entry?.colorKey != null)
        parts = DayPart.entries.mapNotNull { part ->
            entry?.partColorKey(part)?.let { part to it }
        }.toMap()
        title = entry?.title.orEmpty()
        note = entry?.note.orEmpty()
        sportLevel = entry?.sportLevel
        foodLevel = entry?.foodLevel
        wentOut = entry?.wentOut
        weightText = entry?.weightKg?.let { String.format(Locale.FRANCE, "%.1f", it) }.orEmpty()
        stepsValue = entry?.steps
        screenValue = entry?.screenMinutes
        loadedFor = epochDay

        // Pas et temps d'ecran du jour, lus en local si les acces sont donnes.
        val day = LocalDate.ofEpochDay(epochDay)
        withContext(Dispatchers.IO) {
            HealthConnectSource.stepsFor(context, day)?.let { stepsValue = it }
            ScreenTimeSource.minutesFor(context, day)?.let { screenValue = it }
        }
    }

    LaunchedEffect(
        epochDay,
        loadedFor,
        colorKey,
        colorManual,
        parts,
        title,
        note,
        sportLevel,
        foodLevel,
        wentOut,
        weightText,
        stepsValue,
        screenValue,
    ) {
        if (loadedFor != epochDay) return@LaunchedEffect
        delay(SAVE_DEBOUNCE_MS)
        repository.saveDay(currentEntry(epochDay))
    }

    DisposableEffect(epochDay, loadedFor) {
        // Le jour est fige ici : au moment du onDispose, epochDay peut deja
        // pointer vers le jour suivant alors que les champs contiennent encore
        // le contenu du jour precedent.
        val dayOfThisEffect = epochDay
        val contentIsLoaded = loadedFor == epochDay
        onDispose {
            if (contentIsLoaded) {
                val snapshot = currentEntry(dayOfThisEffect)
                app.appScope.launch { repository.saveDay(snapshot) }
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
                // imePadding avant verticalScroll : la zone visible se reduit
                // quand le clavier s'ouvre, donc le curseur reste au-dessus.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { epochDay -= 1 }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Jour précédent",
                    )
                }
                Text(
                    text = Dates.dayLong(date),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { epochDay += 1 }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Jour suivant",
                    )
                }
            }

            if (isBirthday) {
                Text(
                    text = "🎂 Ton anniversaire",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
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

            // 1. Comment tu te sens ---------------------------------------
            SectionCard(title = "Comment tu te sens") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DayColor.entries.forEach { dayColor ->
                        ColorChoice(
                            dayColor = dayColor,
                            selected = colorKey == dayColor.key,
                            onClick = {
                                if (colorKey == dayColor.key && colorManual) {
                                    colorManual = false
                                    colorKey = averageColorKey(parts.values)
                                } else {
                                    colorKey = dayColor.key
                                    colorManual = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = DayColor.fromKey(colorKey)?.label ?: "Aucune couleur pour l'instant",
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (parts.isNotEmpty()) {
                    Text(
                        text = if (colorManual) {
                            "Choisie à la main. Touche-la à nouveau pour revenir à la moyenne de tes moments."
                        } else {
                            "Calculée à partir de tes moments."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                Text(
                    "Moment par moment",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "Ton humeur bouge dans la journée : la couleur du jour se calcule à partir d'ici.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                DayPart.entries.forEach { part ->
                    PartRow(
                        part = part,
                        selectedKey = parts[part],
                        onPick = { key -> setPart(part, key) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 2. Journal ---------------------------------------------------
            SectionCard(title = "Ton journal") {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de la journée") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("day-title-field"),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ce que tu as vécu") },
                    placeholder = { Text("Ce que tu as ressenti, ce qui a aidé, ce qui a pesé…") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 320.dp)
                        .testTag("day-note-field"),
                )
            }

            Spacer(Modifier.height(16.dp))

            // 3. Bouger ----------------------------------------------------
            SectionCard(title = "🏃 Bouger") {
                MeasureRow(
                    emoji = "👟",
                    label = "Pas aujourd'hui",
                    value = stepsValue?.let { "${formatSteps(it)} pas" },
                    hint = "Autorise Health Connect dans les réglages pour les voir.",
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Une vraie séance ?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SportLevel.entries.forEach { level ->
                        ChoiceChip(
                            label = "${level.emoji} ${level.label}",
                            selected = sportLevel == level.key,
                            onClick = {
                                sportLevel = if (sportLevel == level.key) null else level.key
                            },
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { input ->
                        weightText = input.filter { it.isDigit() || it == ',' || it == '.' }.take(6)
                    },
                    label = { Text("Poids du jour (optionnel)") },
                    suffix = { Text("kg") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(16.dp))

            // 4. Manger ----------------------------------------------------
            SectionCard(title = "🍽️ Manger") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FoodLevel.entries.forEach { level ->
                        ChoiceChip(
                            label = "${level.emoji} ${level.label}",
                            selected = foodLevel == level.key,
                            onClick = {
                                foodLevel = if (foodLevel == level.key) null else level.key
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 5. Dehors et ecrans ------------------------------------------
            SectionCard(title = "🚪 Dehors & écrans") {
                Text(
                    "Tu es sorti aujourd'hui ?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChoiceChip(
                        label = "🚪 Oui, je suis sorti",
                        selected = wentOut == true,
                        onClick = { wentOut = if (wentOut == true) null else true },
                    )
                    ChoiceChip(
                        label = "🛋️ Resté à la maison",
                        selected = wentOut == false,
                        onClick = { wentOut = if (wentOut == false) null else false },
                    )
                }
                Spacer(Modifier.height(14.dp))
                MeasureRow(
                    emoji = "📱",
                    label = "Temps sur le téléphone",
                    value = screenValue?.let { formatScreenTime(it) },
                    hint = "Autorise l'accès aux données d'utilisation dans les réglages.",
                )
            }

            Spacer(Modifier.height(16.dp))

            // 6. Etiquettes ------------------------------------------------
            SectionCard(title = "Étiquettes") {
                if (allTags.isEmpty()) {
                    Text(
                        "Les étiquettes arrivent avec l'application.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    TagCategory.entries.forEach { category ->
                        val categoryTags = allTags.filter { it.group == category }
                        if (categoryTags.isNotEmpty()) {
                            Text(
                                text = category.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                categoryTags.forEach { tag ->
                                    val selected = tag.id in selectedTagIds
                                    ChoiceChip(
                                        label = tag.display,
                                        selected = selected,
                                        onClick = {
                                            scope.launch {
                                                repository.toggleTag(
                                                    LocalDate.ofEpochDay(epochDay),
                                                    tag,
                                                    !selected,
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 7. Argent du jour --------------------------------------------
            SectionCard(title = "💶 Argent du jour") {
                if (dayMoney.isEmpty()) {
                    Text(
                        "Rien noté ce jour-là. Ce que tu ajoutes ici remonte tout de suite dans l'onglet Argent.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    dayMoney.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingMoney = entry }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(entry.category?.emoji ?: if (entry.isIncome) "➕" else "➖")
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = entry.label.ifBlank { entry.category?.label ?: "Mouvement" },
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = formatSignedMoney(entry.amountCents),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (entry.isIncome) DayColor.GREEN.color else DayColor.RED.color,
                            )
                        }
                    }
                    val total = dayMoney.sumOf { it.amountCents }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Bilan du jour", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = formatSignedMoney(total),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (total < 0) DayColor.RED.color else DayColor.GREEN.color,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { addingMoney = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add-day-money"),
                ) {
                    Text("Ajouter une dépense ou une rentrée")
                }
            }

            Spacer(Modifier.height(16.dp))

            // 8. Medias ----------------------------------------------------
            SectionCard(title = "📷 Photos & vidéos") {
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
                            repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        pickMedia.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Ajouter une photo ou une vidéo")
                }
            }

            Spacer(Modifier.height(48.dp))
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

    if (addingMoney) {
        MoneyEntryDialog(
            initial = null,
            defaultDate = date,
            allowDateChange = false,
            onDismiss = { addingMoney = false },
            onSave = { entry ->
                addingMoney = false
                scope.launch { repository.saveMoney(entry) }
            },
        )
    }

    editingMoney?.let { current ->
        MoneyEntryDialog(
            initial = current,
            defaultDate = LocalDate.ofEpochDay(current.epochDay),
            onDismiss = { editingMoney = null },
            onSave = { entry ->
                editingMoney = null
                scope.launch { repository.saveMoney(entry) }
            },
            onDelete = {
                editingMoney = null
                scope.launch { repository.deleteMoney(current) }
            },
        )
    }
}

/** Donnee relevee automatiquement par le telephone, en lecture seule. */
@Composable
private fun MeasureRow(emoji: String, label: String, value: String?, hint: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (value == null) {
                Text(
                    hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = value ?: "—",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Ligne d'un moment de la journee : le libelle et les quatre couleurs. */
@Composable
private fun PartRow(part: DayPart, selectedKey: Int?, onPick: (Int?) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = "${part.emoji} ${part.label}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DayColor.entries.forEach { dayColor ->
                val selected = selectedKey == dayColor.key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
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
                            RoundedCornerShape(12.dp),
                        )
                        .clickable { onPick(if (selected) null else dayColor.key) }
                        .testTag("part-${part.name}-${dayColor.name}"),
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
    }
}

@Composable
private fun ColorChoice(
    dayColor: DayColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
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
                RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .testTag("color-${dayColor.name}"),
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
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
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
                Icon(Icons.Default.PlayArrow, contentDescription = "Vidéo", tint = Color.White)
            }
        }
    }
}

/** Couleur moyenne (arrondie) d'une liste de moments notes. */
private fun averageColorKey(partKeys: Collection<Int>): Int? {
    val colors = partKeys.mapNotNull { DayColor.fromKey(it) }
    if (colors.isEmpty()) return null
    val average = colors.sumOf { it.score }.toDouble() / colors.size
    return DayColor.entries.minByOrNull { kotlin.math.abs(it.score - average) }?.key
}

private fun formatSteps(steps: Int): String =
    steps.toString().reversed().chunked(3).joinToString(" ").reversed()

private fun formatScreenTime(minutes: Int): String =
    "${minutes / 60} h ${String.format(Locale.FRANCE, "%02d", minutes % 60)}"
