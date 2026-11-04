package com.minimal.carlauncher.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.minimal.carlauncher.data.AppRepository

/**
 * Dim All Apps, full cover: draws the dark layer as an accessibility layer.
 * Android allows this kind of layer above the top bar (status bar) and the
 * navigation bar, so the whole screen gets darker, not only the apps.
 *
 * It does not read the screen or any app content: it listens to no events.
 * Android also keeps accessibility services running, so the head unit is
 * much less likely to close it than a normal app.
 *
 * While it runs, [ScreenDimService] (the normal app layer) stays off.
 */
class ScreenDimAccessibilityService : AccessibilityService() {

    private lateinit var repository: AppRepository
    private lateinit var overlay: DimOverlay
    private val handler = Handler(Looper.getMainLooper())

    private val ticker = object : Runnable {
        override fun run() {
            update()
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        repository = AppRepository(this)
        overlay = DimOverlay(this, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
        instance = this
        isRunning = true
        // Stop the normal app layer, so the screen is not dimmed twice
        ScreenDimService.sync(this)
        handler.post(ticker)
    }

    /** Auto mode follows the head unit's night mode, which arrives here. Also catches rotation. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isRunning) {
            overlay.refreshSize()
            update()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used: this service only draws the dimming layer
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        shutDown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        shutDown()
        super.onDestroy()
    }

    private fun shutDown() {
        if (!isRunning) return
        isRunning = false
        instance = null
        handler.removeCallbacks(ticker)
        overlay.hide()
        // Let the normal app layer take over again, if it is allowed
        ScreenDimService.sync(this)
    }

    private fun update() {
        // No 0.8 limit here: Android trusts accessibility layers to let taps through
        overlay.show(DimOverlay.currentAlpha(this, repository))
    }

    companion object {
        private const val CHECK_INTERVAL_MS = 30_000L

        /** True while the user has this service turned on and it is drawing. */
        @Volatile
        var isRunning = false
            private set

        private var instance: ScreenDimAccessibilityService? = null

        /** Applies changed Night Dimming settings right away (call on the main thread). */
        fun refreshNow() {
            instance?.update()
        }

        /** True if the user turned this service on in Accessibility settings. */
        fun isEnabledInSettings(context: Context): Boolean {
            val expected = ComponentName(context, ScreenDimAccessibilityService::class.java)
            return try {
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
                    ?.split(':')
                    ?.mapNotNull { ComponentName.unflattenFromString(it) }
                    ?.any { it == expected }
                    ?: false
            } catch (e: Exception) {
                false
            }
        }
    }
}
