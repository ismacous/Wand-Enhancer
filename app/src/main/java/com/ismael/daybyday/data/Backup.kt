package com.ismael.daybyday.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupSummary(val days: Int, val mediaFiles: Int)

/** Ce qu'on sait d'un fichier de sauvegarde sans l'avoir restaure. */
data class BackupInfo(
    val uri: Uri,
    val name: String,
    val exportedAt: Long,
    val days: Int,
    val mediaFiles: Int,
)

/**
 * Sauvegarde / restauration complete sous forme d'un fichier .zip choisi par
 * l'utilisateur (aucun envoi reseau : c'est un simple fichier local).
 */
object Backup {

    private const val JSON_NAME = "daybyday.json"
    private const val MEDIA_PREFIX = "media/"
    private const val FORMAT_VERSION = 3

    const val AUTO_BACKUP_NAME = "DayByDay-sauvegarde-auto.zip"

    // --- Export -----------------------------------------------------------

    suspend fun export(context: Context, repository: DayRepository, target: Uri): BackupSummary =
        withContext(Dispatchers.IO) {
            val output = context.contentResolver.openOutputStream(target)
                ?: error("Impossible d'ouvrir le fichier de destination.")
            writeZip(repository, output)
        }

    /**
     * Ecrit la sauvegarde dans un dossier choisi via SAF, en remplacant le
     * fichier precedent : une seule sauvegarde automatique occupe la place.
     */
    suspend fun exportToFolder(
        context: Context,
        repository: DayRepository,
        treeUri: Uri,
        fileName: String = AUTO_BACKUP_NAME,
    ): BackupSummary = withContext(Dispatchers.IO) {
        val folder = DocumentFile.fromTreeUri(context, treeUri)
            ?: error("Dossier de sauvegarde introuvable.")
        if (!folder.canWrite()) error("Le dossier de sauvegarde n'est plus accessible.")
        folder.findFile(fileName)?.delete()
        val file = folder.createFile("application/zip", fileName)
            ?: error("Impossible de créer le fichier de sauvegarde.")
        val output = context.contentResolver.openOutputStream(file.uri)
            ?: error("Impossible d'écrire dans le dossier choisi.")
        writeZip(repository, output)
    }

    private suspend fun writeZip(repository: DayRepository, output: OutputStream): BackupSummary {
        val days = repository.allDays()
        val mediaItems = repository.allMedia()
        val tags = repository.allTags()
        val links = repository.allDayTags()
        val money = repository.allMoney()

        val root = JSONObject()
        root.put("version", FORMAT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val daysJson = JSONArray()
        days.forEach { day ->
            daysJson.put(
                JSONObject()
                    .put("epochDay", day.epochDay)
                    .put("colorKey", day.colorKey ?: JSONObject.NULL)
                    .put("title", day.title)
                    .put("note", day.note)
                    .put("updatedAt", day.updatedAt)
                    .put("sportLevel", day.sportLevel ?: JSONObject.NULL)
                    .put("foodLevel", day.foodLevel ?: JSONObject.NULL)
                    .put("wentOut", day.wentOut ?: JSONObject.NULL)
                    .put("weightKg", day.weightKg ?: JSONObject.NULL)
                    .put("partMorning", day.partMorning ?: JSONObject.NULL)
                    .put("partAfternoon", day.partAfternoon ?: JSONObject.NULL)
                    .put("partEvening", day.partEvening ?: JSONObject.NULL)
                    .put("partNight", day.partNight ?: JSONObject.NULL)
                    .put("colorManual", day.colorManual ?: JSONObject.NULL)
            )
        }
        root.put("days", daysJson)

        val mediaJson = JSONArray()
        mediaItems.forEach { item ->
            mediaJson.put(
                JSONObject()
                    .put("epochDay", item.epochDay)
                    .put("relativePath", item.relativePath)
                    .put("kindKey", item.kindKey)
                    .put("addedAt", item.addedAt)
            )
        }
        root.put("media", mediaJson)

        val tagsJson = JSONArray()
        tags.forEach { tag ->
            tagsJson.put(
                JSONObject()
                    .put("id", tag.id)
                    .put("name", tag.name)
                    .put("emoji", tag.emoji)
                    .put("sortOrder", tag.sortOrder)
                    .put("category", tag.category ?: JSONObject.NULL)
            )
        }
        root.put("tags", tagsJson)

        val linksJson = JSONArray()
        links.forEach { link ->
            linksJson.put(
                JSONObject()
                    .put("epochDay", link.epochDay)
                    .put("tagId", link.tagId)
            )
        }
        root.put("dayTags", linksJson)

        val moneyJson = JSONArray()
        money.forEach { entry ->
            moneyJson.put(
                JSONObject()
                    .put("epochDay", entry.epochDay)
                    .put("amountCents", entry.amountCents)
                    .put("label", entry.label)
                    .put("categoryKey", entry.categoryKey ?: JSONObject.NULL)
                    .put("createdAt", entry.createdAt)
            )
        }
        root.put("money", moneyJson)

        var copied = 0
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(JSON_NAME))
            zip.write(root.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            mediaItems.forEach { item ->
                val file = repository.media.file(item.relativePath)
                if (file.exists()) {
                    zip.putNextEntry(ZipEntry(MEDIA_PREFIX + item.relativePath))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    copied += 1
                }
            }
        }
        return BackupSummary(days = days.size, mediaFiles = copied)
    }

    // --- Inspection -------------------------------------------------------

    /** Lit uniquement la fiche d'identite d'une sauvegarde, sans rien ecraser. */
    suspend fun peek(context: Context, source: Uri, name: String = ""): BackupInfo? =
        withContext(Dispatchers.IO) {
            runCatching {
                var json: JSONObject? = null
                val input = context.contentResolver.openInputStream(source)
                    ?: return@runCatching null
                ZipInputStream(input.buffered()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null && json == null) {
                        if (entry.name == JSON_NAME) {
                            json = JSONObject(zip.readBytes().toString(Charsets.UTF_8))
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
                json?.let { payload ->
                    BackupInfo(
                        uri = source,
                        name = name,
                        exportedAt = payload.optLong("exportedAt", 0L),
                        days = payload.optJSONArray("days")?.length() ?: 0,
                        mediaFiles = payload.optJSONArray("media")?.length() ?: 0,
                    )
                }
            }.getOrNull()
        }

    /**
     * Cherche la sauvegarde la plus recente dans un dossier choisi par
     * l'utilisateur. Utilise apres une reinstallation pour retrouver les
     * donnees sans rien avoir a chercher a la main.
     */
    suspend fun findLatestInFolder(context: Context, treeUri: Uri): BackupInfo? =
        withContext(Dispatchers.IO) {
            val folder = runCatching { DocumentFile.fromTreeUri(context, treeUri) }.getOrNull()
                ?: return@withContext null
            val candidates = runCatching { folder.listFiles().toList() }.getOrDefault(emptyList())
                .filter { it.isFile && it.name?.endsWith(".zip", ignoreCase = true) == true }
                .sortedByDescending { it.lastModified() }
                .take(15)
            candidates.firstNotNullOfOrNull { file ->
                peek(context, file.uri, file.name.orEmpty())
            }
        }

    // --- Import -----------------------------------------------------------

    suspend fun import(context: Context, repository: DayRepository, source: Uri): BackupSummary =
        withContext(Dispatchers.IO) {
            val staging = File(context.cacheDir, "restore_${System.currentTimeMillis()}")
            staging.mkdirs()
            var payload: JSONObject? = null
            var restoredFiles = 0

            try {
                val input = context.contentResolver.openInputStream(source)
                    ?: error("Impossible d'ouvrir la sauvegarde.")
                ZipInputStream(input.buffered()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        when {
                            entry.isDirectory -> Unit
                            name == JSON_NAME ->
                                payload = JSONObject(zip.readBytes().toString(Charsets.UTF_8))
                            name.startsWith(MEDIA_PREFIX) -> {
                                val relative = name.removePrefix(MEDIA_PREFIX)
                                val destination = safeChild(staging, relative)
                                if (destination != null) {
                                    destination.parentFile?.mkdirs()
                                    destination.outputStream().use { zip.copyTo(it) }
                                    restoredFiles += 1
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }

                val json = payload ?: error("Fichier de sauvegarde invalide (daybyday.json manquant).")

                val days = mutableListOf<DayEntry>()
                val daysJson = json.optJSONArray("days") ?: JSONArray()
                for (i in 0 until daysJson.length()) {
                    val item = daysJson.getJSONObject(i)
                    days += DayEntry(
                        epochDay = item.getLong("epochDay"),
                        colorKey = item.optIntOrNull("colorKey"),
                        title = item.optString("title", ""),
                        note = item.optString("note", ""),
                        updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
                        sportLevel = item.optIntOrNull("sportLevel"),
                        foodLevel = item.optIntOrNull("foodLevel"),
                        wentOut = if (item.isNull("wentOut")) null else item.optBoolean("wentOut"),
                        weightKg = if (item.isNull("weightKg")) null else item.optDouble("weightKg"),
                        partMorning = item.optIntOrNull("partMorning"),
                        partAfternoon = item.optIntOrNull("partAfternoon"),
                        partEvening = item.optIntOrNull("partEvening"),
                        partNight = item.optIntOrNull("partNight"),
                        colorManual = if (item.isNull("colorManual")) {
                            null
                        } else {
                            item.optBoolean("colorManual")
                        },
                    )
                }

                val mediaItems = mutableListOf<MediaItem>()
                val mediaJson = json.optJSONArray("media") ?: JSONArray()
                for (i in 0 until mediaJson.length()) {
                    val item = mediaJson.getJSONObject(i)
                    mediaItems += MediaItem(
                        epochDay = item.getLong("epochDay"),
                        relativePath = item.getString("relativePath"),
                        kindKey = item.optInt("kindKey", 0),
                        addedAt = item.optLong("addedAt", System.currentTimeMillis()),
                    )
                }

                val tags = mutableListOf<Tag>()
                val tagsJson = json.optJSONArray("tags") ?: JSONArray()
                for (i in 0 until tagsJson.length()) {
                    val item = tagsJson.getJSONObject(i)
                    tags += Tag(
                        id = item.getLong("id"),
                        name = item.getString("name"),
                        emoji = item.optString("emoji", ""),
                        sortOrder = item.optInt("sortOrder", i),
                        category = if (item.isNull("category")) null else item.optString("category"),
                    )
                }

                val links = mutableListOf<DayTagCrossRef>()
                val linksJson = json.optJSONArray("dayTags") ?: JSONArray()
                for (i in 0 until linksJson.length()) {
                    val item = linksJson.getJSONObject(i)
                    links += DayTagCrossRef(
                        epochDay = item.getLong("epochDay"),
                        tagId = item.getLong("tagId"),
                    )
                }

                val money = mutableListOf<MoneyEntry>()
                val moneyJson = json.optJSONArray("money") ?: JSONArray()
                for (i in 0 until moneyJson.length()) {
                    val item = moneyJson.getJSONObject(i)
                    money += MoneyEntry(
                        epochDay = item.getLong("epochDay"),
                        amountCents = item.getLong("amountCents"),
                        label = item.optString("label", ""),
                        categoryKey = if (item.isNull("categoryKey")) {
                            null
                        } else {
                            item.optString("categoryKey")
                        },
                        createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                    )
                }

                repository.media.deleteAll()
                staging.walkTopDown().filter { it.isFile }.forEach { file ->
                    val relative = file.relativeTo(staging).path.replace(File.separatorChar, '/')
                    file.inputStream().use { repository.media.writeFrom(relative, it) }
                }
                repository.replaceAll(days, mediaItems, tags, links, money)

                BackupSummary(days = days.size, mediaFiles = restoredFiles)
            } finally {
                staging.deleteRecursively()
            }
        }

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (isNull(key)) null else optInt(key)

    /** Empeche un chemin malveillant du type ../../ de sortir du dossier cible. */
    private fun safeChild(root: File, relative: String): File? {
        val candidate = File(root, relative).canonicalFile
        return if (candidate.path.startsWith(root.canonicalFile.path + File.separator)) candidate else null
    }

    // --- Export texte -----------------------------------------------------

    /**
     * Export texte d'une annee : pratique pour preparer la video annuelle de
     * fin d'annee (titres + notes, jour par jour, avec les statistiques).
     */
    suspend fun exportYearText(
        context: Context,
        repository: DayRepository,
        year: Int,
        target: Uri,
    ): Int = withContext(Dispatchers.IO) {
        val start = LocalDate.of(year, 1, 1)
        val end = LocalDate.of(year, 12, 31)
        val days = repository.allDays()
            .filter { it.epochDay in start.toEpochDay()..end.toEpochDay() }
            .sortedBy { it.epochDay }
        val mediaByDay = repository.allMedia().groupBy { it.epochDay }
        val tagsById = repository.allTags().associateBy { it.id }
        val tagsByDay = repository.allDayTags().groupBy({ it.epochDay }, { it.tagId })

        val dayFormat = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE)
        val builder = StringBuilder()
        builder.appendLine("# DayByDay — $year")
        builder.appendLine()

        val yearSummary = Stats.summarize(year.toString(), days, if (start.isLeapYear) 366 else 365)
        builder.appendLine("Jours notés : ${yearSummary.filledDays} / ${yearSummary.totalDays}")
        yearSummary.average?.let { builder.appendLine("Moyenne de l'année : ${"%.2f".format(it)} / 3") }
        DayColor.entries.forEach { color ->
            builder.appendLine("- ${color.label} : ${yearSummary.countOf(color)} jour(s)")
        }
        builder.appendLine()

        (1..12).forEach { monthValue ->
            val monthDays = days.filter { LocalDate.ofEpochDay(it.epochDay).monthValue == monthValue }
            val monthName = LocalDate.of(year, monthValue, 1)
                .month.getDisplayName(TextStyle.FULL, Locale.FRANCE)
                .replaceFirstChar { it.uppercase() }
            val summary = Stats.summarize(
                monthName,
                monthDays,
                LocalDate.of(year, monthValue, 1).lengthOfMonth(),
            )
            builder.appendLine("## $monthName")
            builder.appendLine(
                summary.average?.let { "Moyenne : ${"%.2f".format(it)} / 3 — ${summary.filledDays} jour(s) noté(s)" }
                    ?: "Aucun jour noté."
            )
            builder.appendLine()
            monthDays.forEach { entry ->
                val date = LocalDate.ofEpochDay(entry.epochDay)
                val label = entry.color?.label ?: "Sans couleur"
                builder.appendLine("### ${date.format(dayFormat)} — $label")
                if (entry.title.isNotBlank()) builder.appendLine("**${entry.title}**")
                if (entry.note.isNotBlank()) builder.appendLine(entry.note)

                val moments = DayPart.entries.mapNotNull { part ->
                    entry.partColor(part)?.let { "${part.label} : ${it.label}" }
                }
                if (moments.isNotEmpty()) builder.appendLine(moments.joinToString(" · "))

                val details = buildList {
                    entry.sport?.let { add("Sport : ${it.label}") }
                    entry.food?.let { add("Alimentation : ${it.label}") }
                    entry.wentOut?.let { add(if (it) "Sorti" else "Pas sorti") }
                    entry.weightKg?.let { add("Poids : ${"%.1f".format(it)} kg") }
                }
                if (details.isNotEmpty()) builder.appendLine(details.joinToString(" · "))

                val dayTags = tagsByDay[entry.epochDay].orEmpty().mapNotNull { tagsById[it]?.display }
                if (dayTags.isNotEmpty()) builder.appendLine(dayTags.joinToString(" "))

                val mediaCount = mediaByDay[entry.epochDay]?.size ?: 0
                if (mediaCount > 0) builder.appendLine("_($mediaCount média(s) dans l'application)_")
                builder.appendLine()
            }
        }

        val output = context.contentResolver.openOutputStream(target)
            ?: error("Impossible d'ouvrir le fichier de destination.")
        output.bufferedWriter(Charsets.UTF_8).use { it.write(builder.toString()) }
        days.size
    }
}
