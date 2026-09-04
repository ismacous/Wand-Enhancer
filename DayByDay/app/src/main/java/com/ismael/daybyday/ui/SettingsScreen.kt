package com.ismael.daybyday.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.ismael.daybyday.data.Backup
import com.ismael.daybyday.dayByDayApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.dayByDayApp
    val prefs = app.prefs
    val repository = app.repository
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var lockEnabled by remember { mutableStateOf(prefs.lockEnabled) }
    var biometricEnabled by remember { mutableStateOf(prefs.biometricEnabled) }
    var blockScreenshots by remember { mutableStateOf(prefs.blockScreenshots) }
    var hasPin by remember { mutableStateOf(prefs.hasPin) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showEraseDialog by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var mediaBytes by remember { mutableLongStateOf(0L) }
    var exportYear by remember { mutableIntStateOf(LocalDate.now().year) }

    val biometricAvailable = remember {
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    LaunchedEffect(busy) {
        mediaBytes = withContext(Dispatchers.IO) { repository.media.totalBytes() }
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
        topBar = {
            TopAppBar(
                title = { Text("Réglages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            SectionTitle("Confidentialité")

            SettingSwitch(
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

            SettingSwitch(
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

            SettingSwitch(
                title = "Bloquer les captures d'écran",
                subtitle = "Masque aussi l'aperçu dans la liste des applis récentes.",
                checked = blockScreenshots,
                onCheckedChange = {
                    blockScreenshots = it
                    prefs.blockScreenshots = it
                    context.findActivity()?.let { activity ->
                        (activity as? MainActivity)?.applySecureFlag()
                    }
                },
            )

            Row(modifier = Modifier.padding(vertical = 8.dp)) {
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            SectionTitle("Sauvegardes")

            InfoText(
                "Tout est stocké uniquement sur ce téléphone. Fais une sauvegarde " +
                    "régulière : elle contient tes journées, tes notes et tes médias."
            )

            Button(
                onClick = { exportBackup.launch("DayByDay-${LocalDate.now()}.zip") },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Exporter une sauvegarde (.zip)")
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = { importBackup.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Restaurer une sauvegarde")
            }

            Spacer(Modifier.height(16.dp))

            SectionTitle("Résumé annuel")

            InfoText(
                "Exporte une année entière en texte (titres, notes, statistiques) " +
                    "pour préparer ta vidéo de fin d'année."
            )

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

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            SectionTitle("Stockage")
            InfoText("Médias enregistrés : ${formatBytes(mediaBytes)}")

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            SectionTitle("À propos")
            InfoText(
                "DayByDay n'a aucune permission Internet : l'application est " +
                    "techniquement incapable d'envoyer tes données ailleurs. " +
                    "Rien n'est synchronisé, rien n'est partagé."
            )

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = { showEraseDialog = true }) {
                Text("Effacer toutes mes données", color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(32.dp))
        }
    }

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
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
    )
}

@Composable
private fun InfoText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
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
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
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
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> String.format(java.util.Locale.FRANCE, "%.2f Go", bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> String.format(java.util.Locale.FRANCE, "%.1f Mo", bytes / 1_000_000.0)
    bytes >= 1_000 -> "${bytes / 1_000} Ko"
    else -> "$bytes octets"
}
