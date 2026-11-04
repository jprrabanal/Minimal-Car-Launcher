package com.minimal.carlauncher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import com.minimal.carlauncher.R
import com.minimal.carlauncher.data.AppRepository
import com.minimal.carlauncher.ui.MainActivity

/**
 * Dim All Apps: a see-through black layer over the whole head unit screen
 * (Android Auto, maps, music apps...), shown when Night Dimming is active.
 *
 * It follows the same Night Dimming settings as the launcher: mode (Auto,
 * Schedule, Always), the schedule times and the strength. The layer never
 * catches taps, so everything under it works as normal.
 *
 * Runs as a foreground service so the head unit is less likely to close it,
 * and START_STICKY so Android starts it again if it is closed anyway. The
 * launcher also restarts it every time the home screen opens.
 *
 * Android keeps the top bar and the navigation bar above this kind of layer.
 * When the user turns on [ScreenDimAccessibilityService], that service draws
 * the layer instead (it can cover the bars too) and this one stops.
 */
class ScreenDimService : Service() {

    private lateinit var repository: AppRepository
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var overlay: DimOverlay

    private val ticker = object : Runnable {
        override fun run() {
            update()
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        repository = AppRepository(this)
        overlay = DimOverlay(this, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        handler.post(ticker)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!goForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }
        update()
        return START_STICKY
    }

    /** Auto mode follows the head unit's night mode, which arrives here. Also catches rotation. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlay.refreshSize()
        update()
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        overlay.hide()
        super.onDestroy()
    }

    private fun update() {
        val shouldRun = repository.isDimAllAppsEnabled() &&
            Settings.canDrawOverlays(this) &&
            !ScreenDimAccessibilityService.isRunning
        if (!shouldRun) {
            overlay.hide()
            stopSelf()
            return
        }
        overlay.show(DimOverlay.currentAlpha(this, repository).coerceAtMost(DimOverlay.MAX_APP_LAYER_ALPHA))
    }

    /** Shows the small "Screen dimmer is on" notification that keeps the service alive. */
    private fun goForeground(): Boolean {
        return try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Screen Dimmer", NotificationManager.IMPORTANCE_MIN).apply {
                    description = "Keeps Dim All Apps running"
                    setShowBadge(false)
                }
            )
            val openLauncher = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notification = Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_car_launcher)
                .setContentTitle("Screen dimmer is on")
                .setContentText("Darkens all apps when Night Dimming is active")
                .setContentIntent(openLauncher)
                .setOngoing(true)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    companion object {
        private const val CHANNEL_ID = "screen_dimmer"
        private const val NOTIFICATION_ID = 4201
        private const val CHECK_INTERVAL_MS = 30_000L

        /** True if the user allowed "Display over other apps". */
        fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

        /**
         * Starts, refreshes or stops the dimmer to match the settings.
         * Safe to call often: the launcher calls it on every resume and
         * whenever a Night Dimming setting changes.
         */
        fun sync(context: Context) {
            val app = context.applicationContext
            // The accessibility layer (covers the bars too) takes over when it is on
            ScreenDimAccessibilityService.refreshNow()
            val intent = Intent(app, ScreenDimService::class.java)
            val shouldRun = AppRepository(app).isDimAllAppsEnabled() &&
                canDrawOverlays(app) &&
                !ScreenDimAccessibilityService.isRunning
            try {
                if (shouldRun) {
                    app.startForegroundService(intent)
                } else {
                    app.stopService(intent)
                }
            } catch (e: Exception) {
                // For example, Android refused to start it while the launcher was in the background
                e.printStackTrace()
            }
        }
    }
}
