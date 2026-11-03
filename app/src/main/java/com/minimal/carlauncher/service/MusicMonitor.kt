package com.minimal.carlauncher.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What a music app is playing right now. */
data class NowPlaying(
    val title: String,
    val artist: String,
    val appLabel: String,
    val packageName: String,
    val albumArt: Bitmap?,
    val isPlaying: Boolean
)

/**
 * Follows the music app that is playing (Spotify, YouTube Music, radio apps...)
 * and sends Play/Pause, Next and Previous to it.
 *
 * Needs Notification Access for [MediaListenerService]. Without it,
 * [hasAccess] is false and nothing else works.
 */
class MusicMonitor(private val context: Context) {

    private val sessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
    private val listenerComponent = ComponentName(context, MediaListenerService::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val _hasAccess = MutableStateFlow(false)
    val hasAccess: StateFlow<Boolean> = _hasAccess.asStateFlow()

    private var controllers: List<MediaController> = emptyList()
    private var isStarted = false

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
        override fun onMetadataChanged(metadata: MediaMetadata?) = refresh()
        override fun onSessionDestroyed() = refresh()
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        watchControllers(list.orEmpty())
    }

    /** True if the user turned on Notification Access for this launcher. */
    fun checkAccess(): Boolean {
        val enabled = try {
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                ?.split(":")
                ?.mapNotNull { ComponentName.unflattenFromString(it) }
                ?.any { it.packageName == context.packageName }
                ?: false
        } catch (e: Exception) {
            false
        }
        _hasAccess.value = enabled
        return enabled
    }

    fun start() {
        if (isStarted || sessionManager == null) return
        if (!checkAccess()) return
        try {
            sessionManager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, mainHandler)
            watchControllers(sessionManager.getActiveSessions(listenerComponent))
            isStarted = true
        } catch (e: SecurityException) {
            // Access was turned off in the meantime
            _hasAccess.value = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        if (!isStarted) return
        try {
            sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        controllers.forEach { runCatching { it.unregisterCallback(controllerCallback) } }
        controllers = emptyList()
        isStarted = false
    }

    private fun watchControllers(list: List<MediaController>) {
        controllers.forEach { runCatching { it.unregisterCallback(controllerCallback) } }
        controllers = list
        controllers.forEach { runCatching { it.registerCallback(controllerCallback, mainHandler) } }
        refresh()
    }

    /** The playing app first; otherwise the most recent one (Android lists it first). */
    private fun activeController(): MediaController? =
        controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull()

    private fun refresh() {
        val controller = activeController()
        if (controller == null) {
            _nowPlaying.value = null
            return
        }
        try {
            val metadata = controller.metadata
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                ?: ""
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
                ?: ""
            val art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            _nowPlaying.value = NowPlaying(
                title = title,
                artist = artist,
                appLabel = appLabel(controller.packageName),
                packageName = controller.packageName,
                albumArt = art,
                isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun appLabel(packageName: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        packageName
    }

    fun playPause() {
        val controller = activeController() ?: return
        runCatching {
            if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
        }
    }

    fun next() {
        activeController()?.let { runCatching { it.transportControls.skipToNext() } }
    }

    fun previous() {
        activeController()?.let { runCatching { it.transportControls.skipToPrevious() } }
    }
}
