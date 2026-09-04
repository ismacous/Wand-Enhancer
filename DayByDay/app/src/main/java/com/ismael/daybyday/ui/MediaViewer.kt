package com.ismael.daybyday.ui

import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ismael.daybyday.data.MediaItem
import com.ismael.daybyday.data.MediaKind
import com.ismael.daybyday.dayByDayApp
import java.io.File

@Composable
fun MediaViewerDialog(
    items: List<MediaItem>,
    startIndex: Int,
    onDismiss: () -> Unit,
    onDelete: (MediaItem) -> Unit,
) {
    val repository = LocalContext.current.dayByDayApp.repository
    val pagerState = rememberPagerState(initialPage = startIndex) { items.size }
    var confirmDelete by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF2000000)),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val item = items[page]
                val file = remember(item.id) { repository.media.file(item.relativePath) }
                when (item.kind) {
                    MediaKind.PHOTO -> MediaImage(
                        file = file,
                        kind = item.kind,
                        modifier = Modifier.fillMaxSize(),
                        maxSize = 1920,
                        contentScale = ContentScale.Fit,
                    )
                    MediaKind.VIDEO -> VideoPlayer(
                        file = file,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
            ) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.White)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                }
            }

            if (items.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${items.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp),
                )
            }
        }
    }

    if (confirmDelete) {
        val current = items.getOrNull(pagerState.currentPage)
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer ce média ?") },
            text = { Text("Le fichier sera définitivement effacé de l'application.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    current?.let(onDelete)
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Annuler") }
            },
        )
    }
}

@Composable
private fun VideoPlayer(file: File, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                VideoView(context).apply {
                    setVideoPath(file.path)
                    setOnPreparedListener { player ->
                        player.isLooping = false
                        start()
                    }
                    setOnClickListener {
                        if (isPlaying) pause() else start()
                    }
                }
            },
            onRelease = { view ->
                runCatching { view.stopPlayback() }
            },
        )
    }
}
