package com.ismael.daybyday.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ismael.daybyday.data.Backup
import com.ismael.daybyday.data.Tag
import com.ismael.daybyday.dayByDayApp
import com.ismael.daybyday.work.DailyScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = context.dayByDayApp
    val prefs = app.prefs
    val repository = app.repository
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var firstName by remember { mutableStateOf(prefs.firstName) }
    var heightText by remember { mutableStateOf(prefs.heightCm.toString()) }
    var birthDate by remember { mutableStateOf(prefs.birthDate) }

    var reminderEnabled by remember { mutableStateOf(prefs.reminderEnabled) }
    var reminderHour by remember { mutableIntStateOf(prefs.reminderHour) }
    var reminderMinute by remember { mutableIntStateOf(prefs.reminderMinute) }

    var autoBackupEnabled by remember { mutableStateOf(prefs.autoBackupEnabled) }
    var autoBackupHour by remember { mutableIntStateOf(prefs.autoBackupHour) }
    var autoBackupMinute by remember { mutableIntStateOf(prefs.autoBackupMinute) }
    var autoBackupFolder by remember { mutableStateOf(prefs.autoBackupFolder) }
    var lastAutoBackup by remember { mutableLongStateOf(prefs.lastAutoBackupAt) }

    var lockEnabled by remember { mutableStateOf(prefs.lockEnabled) }
    var biometricEnabled by remember { mutableStateOf(prefs.biometricEnabled) }
    var blockScreenshots by remember { mutableStateOf(prefs.blockScreenshots) }
    var hasPin by remember { mutableStateOf(prefs.hasPin) }

    var showPinDialog by remember { mutableStateOf(false) }
    var showEraseDialog by remember { mutableStateOf(false) }
    var showNewTagDialog by remember { mutableStateOf(false) }
    var searchingBackup by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var mediaBytes by remember { mutableLongStateOf(0L) }
    var exportYear by remember { mutableIntStateOf(LocalDate.now().year) }

    val tags by remember { repository.observeTags() }.collectAsStateWithLifecycle(emptyList())

    val biometricAvailable = remember {
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    LaunchedEffect(busy) {
        mediaBytes = withContext(Dispatchers.IO) { repository.media.totalBytes() }
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            scope.launch {
                snackbar.showSnackbar(
                    "Sans l'autorisation de notification, le rappel ne pourra pas s'afficher."
                )
            }
        }
    }

    val pickBackupFolder = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            prefs.autoBackupFolder = uri.toString()
            autoBackupFolder = uri.toString()
            DailyScheduler.scheduleAutoBackup(context, prefs)
            scope.launch { snackbar.showSnackbar("Dossier de sauvegarde enregistré.") }
        }
    }

    val exportBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            busy = true
            scope.launch {
                val result = runCatching { Backup.export(context, repository, uri) }
                busy = false
                snackbar.showSnackbar(
                    result.fold(
                        onSuccess = { "Sauvegarde créée : ${it.days} jour(s), ${it.mediaFiles} média(s)." },
                        onFailure = { "Échec de la sauvegarde : ${it.message}" },
                    )
                )
            }
        }
    }

    val importBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            busy = true
            scope.launch {
                val result = runCatching { Backup.import(context, repository, uri) }
                busy = false
                MediaLoader.clear()
                snackbar.showSnackbar(
                    result.fold(
                        onSuccess = { "Restauration terminée : ${it.days} jour(s), ${it.mediaFiles} média(s)." },
                        onFailure = { "Échec de la restauration : ${it.message}" },
                    )
                )
            }
        }
    }

    val exportText = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            busy = true
            scope.launch {
                val result = runCatching {
                    Backup.exportYearText(context, repository, exportYear, uri)
                }
                busy = false
                snackbar.showSnackbar(
                    result.fold(
                        onSuccess = { "Résumé de $exportYear exporté ($it jour(s))." },
                        onFailure = { "Échec de l'export : ${it.message}" },
                    )
                )
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Réglages") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // --- Profil ---------------------------------------------------
            SectionCard(title = "Toi") {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = {
                        firstName = it.take(20)
                        prefs.firstName = firstName
                    },
                    label = { Text("Ton prénom") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { input ->
                        heightText = input.filter { it.isDigit() }.take(3)
                        heightText.toIntOrNull()?.let { prefs.heightCm = it }
                    },
                    label = { Text("Ta taille") },
                    suffix = { Text("cm") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Date de naissance", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            Dates.dayShort(birthDate),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = {
                        showDatePicker(context, birthDate) { picked ->
                            birthDate = picked
                            prefs.birthDate = picked
                        }
                    }) { Text("Modifier") }
                }
                val age = java.time.Period.between(birthDate, LocalDate.now()).years
                Text(
                    "Tu as $age ans. Ta taille sert à calculer ton IMC dans le bilan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))

            // --- Rappel ---------------------------------------------------
            SectionCard(title = "Rappel quotidien") {
                SettingSwitchRow(
                    title = "Me rappeler de noter ma journée",
                    subtitle = "Une notification, seulement si la journée n'est pas encore notée.",
                    checked = reminderEnabled,
                    onCheckedChange = { enabled ->
                        reminderEnabled = enabled
                        prefs.reminderEnabled = enabled
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        DailyScheduler.scheduleReminder(context, prefs)
                    },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Heure du rappel",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        showTimePicker(context, reminderHour, reminderMinute) { hour, minute ->
                            reminderHour = hour
                            reminderMinute = minute
                            prefs.reminderHour = hour
                            prefs.reminderMinute = minute
                            DailyScheduler.scheduleReminder(context, prefs)
                        }
                    }) {
                        Text(formatTime(reminderHour, reminderMinute))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Sauvegarde automatique -----------------------------------
            SectionCard(title = "Sauvegarde automatique") {
                Text(
                    "Une sauvegarde par jour dans le dossier de ton choix. Le fichier " +
                        "précédent est remplacé, donc ça ne prend pas de place en plus.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                SettingSwitchRow(
                    title = "Sauvegarder tous les jours",
                    subtitle = folderLabel(context, autoBackupFolder),
                    checked = autoBackupEnabled,
                    enabled = autoBackupFolder != null,
                    onCheckedChange = { enabled ->
                        autoBackupEnabled = enabled
                        prefs.autoBackupEnabled = enabled
                        DailyScheduler.scheduleAutoBackup(context, prefs)
                    },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Heure de la sauvegarde",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        showTimePicker(context, autoBackupHour, autoBackupMinute) { hour, minute ->
                            autoBackupHour = hour
                            autoBackupMinute = minute
                            prefs.autoBackupHour = hour
                            prefs.autoBackupMinute = minute
                            DailyScheduler.scheduleAutoBackup(context, prefs)
                        }
                    }) {
                        Text(formatTime(autoBackupHour, autoBackupMinute))
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { pickBackupFolder.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (autoBackupFolder == null) "Choisir le dossier" else "Changer de dossier")
                }
                if (autoBackupFolder != null) {
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = {
                            busy = true
                            scope.launch {
                                val result = runCatching {
                                    Backup.exportToFolder(
                                        context,
                                        repository,
                                        Uri.parse(autoBackupFolder),
                                    )
                                }
                                busy = false
                                result.onSuccess {
                                    prefs.lastAutoBackupAt = System.currentTimeMillis()
                                    lastAutoBackup = prefs.lastAutoBackupAt
                                }
                                snackbar.showSnackbar(
                                    result.fold(
                                        onSuccess = { "Sauvegarde faite : ${it.days} jour(s)." },
                                        onFailure = { "Échec : ${it.message}" },
                                    )
                                )
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Sauvegarder maintenant")
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (lastAutoBackup == 0L) {
                        "Aucune sauvegarde automatique pour l'instant."
                    } else {
                        "Dernière sauvegarde : ${formatDateTime(lastAutoBackup)}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))

            // --- Sauvegarde manuelle --------------------------------------
            SectionCard(title = "Sauvegarde manuelle") {
                Button(
                    onClick = { exportBackup.launch("DayByDay-${LocalDate.now()}.zip") },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Exporter une sauvegarde (.zip)")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { searchingBackup = true },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Chercher une sauvegarde dans un dossier")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        importBackup.launch(
                            arrayOf("application/zip", "application/octet-stream", "*/*")
                        )
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Choisir un fichier de sauvegarde")
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Resume annuel --------------------------------------------
            SectionCard(title = "Résumé annuel") {
                Text(
                    "Exporte une année entière en texte (titres, notes, détails, " +
                        "statistiques) pour préparer ta vidéo de fin d'année.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { exportYear -= 1 }) { Text("−") }
                    Text(exportYear.toString(), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { exportYear += 1 }) { Text("+") }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { exportText.launch("DayByDay-$exportYear.txt") },
                        enabled = !busy,
                    ) {
                        Text("Exporter $exportYear")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Etiquettes -----------------------------------------------
            SectionCard(title = "Mes étiquettes") {
                if (tags.isEmpty()) {
                    Text(
                        "Aucune étiquette pour l'instant.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    tags.forEach { tag ->
                        TagRow(
                            tag = tag,
                            onDelete = { scope.launch { repository.deleteTag(tag) } },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showNewTagDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Ajouter une étiquette")
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Confidentialite ------------------------------------------
            SectionCard(title = "Confidentialité") {
                SettingSwitchRow(
                    title = "Verrouiller l'application",
                    subtitle = if (hasPin) {
                        "Code demandé à l'ouverture et après 15 s en arrière-plan."
                    } else {
                        "Définis d'abord un code à 4 chiffres ou plus."
                    },
                    checked = lockEnabled,
                    enabled = hasPin,
                    onCheckedChange = {
                        lockEnabled = it
                        prefs.lockEnabled = it
                        app.lock.refresh()
                    },
                )
                SettingSwitchRow(
                    title = "Empreinte / reconnaissance",
                    subtitle = if (biometricAvailable) {
                        "Déverrouiller avec la biométrie du téléphone."
                    } else {
                        "Aucune biométrie configurée sur ce téléphone."
                    },
                    checked = biometricEnabled && biometricAvailable,
                    enabled = biometricAvailable,
                    onCheckedChange = {
                        biometricEnabled = it
                        prefs.biometricEnabled = it
                    },
                )
                SettingSwitchRow(
                    title = "Bloquer les captures d'écran",
                    subtitle = "Masque aussi l'aperçu dans la liste des applis récentes.",
                    checked = blockScreenshots,
                    onCheckedChange = {
                        blockScreenshots = it
                        prefs.blockScreenshots = it
                        (context.findActivity() as? MainActivity)?.applySecureFlag()
                    },
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { showPinDialog = true }) {
                        Text(if (hasPin) "Changer le code" else "Définir un code")
                    }
                    if (hasPin) {
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            prefs.clearPin()
                            prefs.lockEnabled = false
                            hasPin = false
                            lockEnabled = false
                            app.lock.refresh()
                        }) {
                            Text("Supprimer le code")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Stockage et a propos -------------------------------------
            SectionCard(title = "Stockage") {
                Text(
                    "Médias enregistrés : ${formatBytes(mediaBytes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))

            SectionCard(title = "À propos") {
                Text(
                    "DayByDay n'a aucune permission Internet : l'application est " +
                        "techniquement incapable d'envoyer tes données ailleurs. " +
                        "Rien n'est synchronisé, rien n'est partagé.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { showEraseDialog = true }) {
                    Text("Effacer toutes mes données", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    FolderBackupRestorer(
        active = searchingBackup,
        onDismiss = { searchingBackup = false },
        onRestored = { summary ->
            searchingBackup = false
            autoBackupFolder = prefs.autoBackupFolder
            autoBackupEnabled = prefs.autoBackupEnabled
            scope.launch {
                snackbar.showSnackbar(
                    "Restauration terminée : ${summary.days} jour(s), ${summary.mediaFiles} média(s)."
                )
            }
        },
    )

    if (showPinDialog) {
        PinDialog(
            onDismiss = { showPinDialog = false },
            onConfirm = { pin ->
                prefs.setPin(pin)
                hasPin = true
                if (!prefs.lockEnabled) {
                    prefs.lockEnabled = true
                    lockEnabled = true
                }
                showPinDialog = false
                scope.launch { snackbar.showSnackbar("Code enregistré.") }
            },
        )
    }

    if (showNewTagDialog) {
        NewTagDialog(
            onDismiss = { showNewTagDialog = false },
            onCreate = { emoji, name ->
                showNewTagDialog = false
                scope.launch { repository.createTag(name, emoji) }
            },
        )
    }

    if (showEraseDialog) {
        AlertDialog(
            onDismissRequest = { showEraseDialog = false },
            title = { Text("Tout effacer ?") },
            text = {
                Text(
                    "Toutes les journées, notes, photos et vidéos seront " +
                        "définitivement supprimées de l'application."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showEraseDialog = false
                    scope.launch {
                        repository.clearEverything()
                        MediaLoader.clear()
                        snackbar.showSnackbar("Toutes les données ont été effacées.")
                    }
                }) {
                    Text("Tout effacer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEraseDialog = false }) { Text("Annuler") }
            },
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun PinDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val valid = pin.length >= 4 && pin == confirmation

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Code de l'application") },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { input -> pin = input.filter { it.isDigit() }.take(8) },
                    label = { Text("Code (4 à 8 chiffres)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { input -> confirmation = input.filter { it.isDigit() }.take(8) },
                    label = { Text("Confirmer le code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                if (pin.isNotEmpty() && confirmation.isNotEmpty() && pin != confirmation) {
                    Text(
                        "Les deux codes ne correspondent pas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pin) }, enabled = valid) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

private fun showTimePicker(
    context: Context,
    hour: Int,
    minute: Int,
    onPicked: (Int, Int) -> Unit,
) {
    TimePickerDialog(
        context,
        { _, pickedHour, pickedMinute -> onPicked(pickedHour, pickedMinute) },
        hour,
        minute,
        true,
    ).show()
}

private fun showDatePicker(
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

private fun folderLabel(context: Context, folderUri: String?): String {
    if (folderUri == null) return "Choisis d'abord un dossier de destination."
    val name = runCatching {
        DocumentFile.fromTreeUri(context, Uri.parse(folderUri))?.name
    }.getOrNull()
    return "Dossier : ${name ?: "sélectionné"} · fichier ${Backup.AUTO_BACKUP_NAME}"
}

private fun formatTime(hour: Int, minute: Int): String =
    String.format(Locale.FRANCE, "%02d:%02d", hour, minute)

private fun formatDateTime(millis: Long): String {
    val dateTime = LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(millis),
        ZoneId.systemDefault(),
    )
    return dateTime.format(DateTimeFormatter.ofPattern("d MMM yyyy 'à' HH:mm", Locale.FRANCE))
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> String.format(Locale.FRANCE, "%.2f Go", bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> String.format(Locale.FRANCE, "%.1f Mo", bytes / 1_000_000.0)
    bytes >= 1_000 -> "${bytes / 1_000} Ko"
    else -> "$bytes octets"
}
