package com.example.veyra.components

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import com.example.veyra.R
import com.example.veyra.ui.theme.ThemeViewModel
import kotlinx.coroutines.delay

private const val TAG = "AudioPreview"

private data class AudioPreviewMetadata(
    val title: String? = null,
    val artist: String? = null,
    val coverBytes: ByteArray? = null,
)

private fun extractMetadataFromUri(
    context: Context,
    uri: Uri,
): AudioPreviewMetadata {
    fun readWith(
        sourceName: String,
        block: (MediaMetadataRetriever) -> Unit,
    ): AudioPreviewMetadata? {
        val retriever = MediaMetadataRetriever()

        return try {
            block(retriever)

            val metadata = AudioPreviewMetadata(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                coverBytes = retriever.embeddedPicture,
            )

            Log.d(
                TAG,
                "Metadata read with $sourceName | title=${metadata.title} | artist=${metadata.artist} | coverBytes=${metadata.coverBytes?.size}"
            )

            metadata
        } catch (e: Exception) {
            Log.e(TAG, "Metadata read failed with $sourceName for uri=$uri", e)
            null
        } finally {
            retriever.release()
        }
    }

    return readWith("fileDescriptor") { retriever ->
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            retriever.setDataSource(pfd.fileDescriptor)
        } ?: error("Unable to open file descriptor")
    } ?: readWith("contextUri") { retriever ->
        retriever.setDataSource(context, uri)
    } ?: AudioPreviewMetadata()
}

@Composable
fun SharedAudioMiniPlayer(
    player: Player,
    uri: Uri,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("Titre inconnu") }
    var artist by remember { mutableStateOf("") }
    var coverBytes by remember { mutableStateOf<ByteArray?>(null) }

    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    val themeVm: ThemeViewModel = viewModel()
    val primaryColor = themeVm.primaryColor.collectAsState().value

    LaunchedEffect(uri) {
        val metadata = extractMetadataFromUri(context, uri)

        title = metadata.title
            ?: uri.lastPathSegment
                    ?: "Titre inconnu"

        artist = metadata.artist ?: ""
        coverBytes = metadata.coverBytes

        Log.d(TAG, "Final uri=$uri")
        Log.d(TAG, "Final title=$title")
        Log.d(TAG, "Final artist=$artist")
        Log.d(TAG, "Final coverBytes=${coverBytes?.size}")
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) {
                isPlaying = value
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                duration = player.duration.coerceAtLeast(0L)
            }
        }

        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
        }
    }

    LaunchedEffect(player) {
        while (true) {
            currentPosition = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.coerceAtLeast(0L)
            isPlaying = player.isPlaying
            delay(300)
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val bitmap = remember(coverBytes) {
                    coverBytes?.let {
                        BitmapFactory.decodeByteArray(it, 0, it.size)
                    }
                }

                LaunchedEffect(bitmap, coverBytes) {
                    Log.d(TAG, "Bitmap decoded=$bitmap | coverBytes=${coverBytes?.size}")
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.default_album_cover),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        maxLines = 1,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (artist.isNotBlank()) {
                        Text(
                            text = artist,
                            maxLines = 1,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                IconButton(
                    onClick = {
                        if (player.isPlaying) player.pause()
                        else player.play()
                    }
                ) {
                    Icon(
                        imageVector = if (isPlaying) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }

                onClose?.let {
                    IconButton(onClick = it) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = {
                    if (duration > 0) {
                        (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                },
                color = primaryColor,
                trackColor = primaryColor.copy(alpha = 0.25f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(50)),
            )
        }
    }
}