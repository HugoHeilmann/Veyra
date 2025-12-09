package com.example.veyra.service

import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.example.veyra.model.data.MusicHolder
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class VeyraMediaService : MediaLibraryService() {

    private lateinit var player: ExoPlayer
    private var mediaLibrarySession: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        player = ExoPlayer.Builder(this).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    Log.d(
                        TAG,
                        "Player events=$events state=${player.playbackState} " +
                                "playWhenReady=${player.playWhenReady} " +
                                "item=${player.currentMediaItem?.mediaMetadata?.title}"
                    )
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "Player error", error)
                }
            })
        }

        val callback = object : MediaLibrarySession.Callback {

            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<MediaItem>> {
                Log.d(TAG, "onGetLibraryRoot from ${browser.packageName}, params=$params")

                val rootItem = MediaItem.Builder()
                    .setMediaId(ROOT_ID)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle("Veyra")
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .build()
                    )
                    .build()

                return Futures.immediateFuture(
                    LibraryResult.ofItem(rootItem, params)
                )
            }

            override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                Log.d(TAG, "onGetChildren parentId=$parentId, from=${browser.packageName}")

                if (parentId != ROOT_ID) {
                    Log.w(TAG, "Unknown parentId=$parentId, returning empty list")
                    return Futures.immediateFuture(
                        LibraryResult.ofItemList(ImmutableList.of(), params)
                    )
                }

                val items: ImmutableList<MediaItem> = try {
                    val musics = MusicHolder.getMusicList()
                    Log.d(TAG, "MusicHolder size=${musics.size}")

                    val list = musics.map { music ->
                        buildMediaItem(music.uri, music.name, music.artist, music.album)
                    }

                    ImmutableList.copyOf(list)
                } catch (e: Exception) {
                    Log.e(TAG, "Error while building children list", e)
                    ImmutableList.of()
                }

                return Futures.immediateFuture(
                    LibraryResult.ofItemList(items, params)
                )
            }

            override fun onGetItem(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                mediaId: String
            ): ListenableFuture<LibraryResult<MediaItem>> {
                Log.d(TAG, "onGetItem mediaId=$mediaId from=${browser.packageName}")

                val music = MusicHolder.getMusicList().find { it.uri == mediaId }
                if (music == null) {
                    Log.w(TAG, "onGetItem: unknown mediaId=$mediaId")
                    return Futures.immediateFuture(
                        LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                    )
                }

                val item = buildMediaItem(music.uri, music.name, music.artist, music.album)
                return Futures.immediateFuture(LibraryResult.ofItem(item, /* params = */ null))
            }

            override fun onAddMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>
            ): ListenableFuture<List<MediaItem>> {
                Log.d(
                    TAG,
                    "onAddMediaItems from=${controller.packageName} count=${mediaItems.size}"
                )

                if (mediaItems.isEmpty()) {
                    return Futures.immediateFuture(emptyList())
                }

                // ID de la piste sur laquelle l'utilisateur a cliqué
                val clickedId = mediaItems.first().mediaId
                val allMusics = MusicHolder.getMusicList().shuffled()

                // On construit la playlist complète
                val fullList = allMusics.map { music ->
                    buildMediaItem(music.uri, music.name, music.artist, music.album)
                }

                // On met la piste cliquée en premier, puis le reste dans l'ordre
                val startIndex = allMusics.indexOfFirst { it.uri == clickedId }
                val ordered: List<MediaItem> = if (startIndex >= 0) {
                    fullList.drop(startIndex) + fullList.take(startIndex)
                } else {
                    fullList
                }

                Log.d(TAG, "onAddMediaItems -> queue size=${ordered.size}, startIndex=$startIndex")

                return Futures.immediateFuture(ordered)
            }
        }

        mediaLibrarySession =
            MediaLibrarySession.Builder(this, player, callback).build()

        Log.d(TAG, "mediaLibrarySession created")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        Log.d(TAG, "onGetSession from ${controllerInfo.packageName}")
        return mediaLibrarySession
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        mediaLibrarySession?.release()
        player.release()
        super.onDestroy()
    }


    private fun buildMediaItem(
        uriString: String,
        title: String?,
        artist: String?,
        album: String?
    ): MediaItem {
        return MediaItem.Builder()
            .setMediaId(uriString)
            .setUri(Uri.parse(uriString))
            .setMimeType(MimeTypes.AUDIO_MPEG)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
    }

    companion object {
        private const val TAG = "VeyraMediaService"
        private const val ROOT_ID = "root"
    }
}
