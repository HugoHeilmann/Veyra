package com.example.veyra.utils

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.example.veyra.model.Music
import com.example.veyra.model.data.MusicHolder
import com.example.veyra.model.metadata.AudioTagWriteResult
import com.example.veyra.model.metadata.MetadataManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BulkDeleteHandlerState(
    val isProcessing: Boolean,
    val handleBulkDelete: (String, String) -> Unit
)

@Composable
fun rememberBulkDeleteHandler(
    context: Context,
    scope: CoroutineScope,
    getMusicsForArtistDeletion: (String) -> List<Music>,
    getMusicsForAlbumDeletion: (String) -> List<Music>,
    onProcessingChanged: (Boolean) -> Unit,
    onCompleted: () -> Unit
): BulkDeleteHandlerState {
    var pendingBulkRequests by remember { mutableStateOf<List<BulkTagEditRequest>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(isProcessing) {
        onProcessingChanged(isProcessing)
    }

    val bulkWriteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode != Activity.RESULT_OK || pendingBulkRequests.isEmpty()) {
            pendingBulkRequests = emptyList()
            isProcessing = false
            Toast.makeText(context, "Modification annulée", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }

        val requests = pendingBulkRequests
        pendingBulkRequests = emptyList()

        scope.launch(Dispatchers.IO) {
            val results = BulkTagEditManager.applyAll(context, requests)

            results.forEach { (request, result) ->
                if (result is AudioTagWriteResult.Success) {
                    val newPath = result.updatedFilePath ?: request.oldFilePath

                    if (newPath != request.oldFilePath) {
                        MetadataManager.renameFilePath(
                            context = context,
                            oldPath = request.oldFilePath,
                            newPath = newPath
                        )
                    }

                    MetadataManager.updateMetadata(
                        context = context,
                        filePath = newPath,
                        title = request.title,
                        artist = request.artist,
                        album = request.album,
                        coverPath = request.coverPath
                    )

                    MusicHolder.applyBulkLocalUpdate(
                        oldFilePath = request.oldFilePath,
                        newFilePath = newPath,
                        title = request.title,
                        artist = request.artist,
                        album = request.album,
                        coverPath = request.coverPath
                    )
                }
            }

            withContext(Dispatchers.Main) {
                isProcessing = false
                onCompleted()
                Toast.makeText(context, "Modification terminée", Toast.LENGTH_LONG).show()
            }
        }
    }

    val handleBulkDelete: (String, String) -> Unit = remember(
        context,
        isProcessing,
        getMusicsForArtistDeletion,
        getMusicsForAlbumDeletion
    ) {
        { type, value ->
            if (isProcessing) return@remember

            val musics = if (type == "artist") {
                getMusicsForArtistDeletion(value)
            } else {
                getMusicsForAlbumDeletion(value)
            }

            if (musics.isEmpty()) {
                Toast.makeText(context, "Aucune musique trouvée", Toast.LENGTH_LONG).show()
                return@remember
            }

            val preparation = if (type == "artist") {
                BulkTagEditManager.prepareDeleteArtist(context, musics)
            } else {
                BulkTagEditManager.prepareDeleteAlbum(context, musics)
            }

            when (preparation) {
                is BulkTagEditResult.Failure -> {
                    Toast.makeText(context, preparation.message, Toast.LENGTH_LONG).show()
                }

                is BulkTagEditResult.Ready -> {
                    val intentSender = BulkTagEditManager.createWriteRequestIntentSender(
                        context = context,
                        uris = preparation.uris
                    )

                    if (intentSender == null) {
                        Toast.makeText(
                            context,
                            "Autorisation d'écriture non supportée",
                            Toast.LENGTH_LONG
                        ).show()
                        return@remember
                    }

                    pendingBulkRequests = preparation.requests
                    isProcessing = true

                    bulkWriteRequestLauncher.launch(
                        IntentSenderRequest.Builder(intentSender).build()
                    )
                }
            }
        }
    }

    return remember(isProcessing, handleBulkDelete) {
        BulkDeleteHandlerState(
            isProcessing = isProcessing,
            handleBulkDelete = handleBulkDelete
        )
    }
}