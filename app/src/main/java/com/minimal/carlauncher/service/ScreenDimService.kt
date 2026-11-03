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
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import com.minimal.carlauncher.R
import com.minimal.carlauncher.data.AppRepository
import com.minimal.carlauncher.data.NightDimMode
import com.minimal.carlauncher.ui.MainActivity
import com.minimal.carlauncher.ui.viewmodel.LauncherViewModel
import java.util.Calendar

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
 */
class ScreenDimService : Service() {

    private lateinit var repository: AppRepository
    private val handler = Handler(Looper.getMainLooper())
    private var overlay: View? = null
    private var shownAlpha = -1f

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

    /** Auto mode follows the head unit's night mode, which arrives here. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        update()
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        removeOverlay()
        super.onDestroy()
    }

    private fun update() {
        if (!repository.isDimAllAppsEnabled() || !Settings.canDrawOverlays(this)) {
            removeOverlay()
            stopSelf()
            return
        }
        val alpha = if (shouldDimNow()) {
            repository.getNightDimStrength().amount.coerceAtMost(MAX_ALPHA)
        } else {
            0f
        }
        if (alpha <= 0f) removeOverlay() else showOverlay(alpha)
    }

    private fun shouldDimNow(): Boolean = when (repository.getNightDimMode()) {
        NightDimMode.OFF -> false
        NightDimMode.ALWAYS -> true
        NightDimMode.AUTO ->
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        NightDimMode.SCHEDULE -> {
            val now = Calendar.getInstance()
            val minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            LauncherViewModel.isWithinNight(
                minute,
                repository.getNightDimStartMinute(),
                repository.getNightDimEndMinute()
            )
        }
    }

    private fun showOverlay(alpha: Float) {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val current = overlay
        if (current != null) {
            if (alpha != shownAlpha) {
                val params = current.layoutParams as WindowManager.LayoutParams
                params.alpha = alpha
                runCatching { windowManager.updateViewLayout(current, params) }
                shownAlpha = alpha
            }
            return
        }
        val view = View(this).apply { setBackgroundColor(Color.BLACK) }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            // The window's own alpha (not the view's): Android 12+ lets taps pass
            // through a see-through layer only when this is 0.8 or less.
            this.alpha = alpha
        }
        try {
            windowManager.addView(view, params)
            overlay = view
            shownAlpha = alpha
        } catch (e: Exception) {
            // Permission was taken away, or the head unit refused the layer
            e.printStackTrace()
        }
    }

    private fun removeOverlay() {
        val view = overlay ?: return
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        runCatching { windowManager.removeView(view) }
        overlay = null
        shownAlpha = -1f
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

        /** Android 12+ blocks taps through layers darker than 0.8. */
        private const val MAX_ALPHA = 0.8f

        /** True if the user allowed "Display over other apps". */
        fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

        /**
         * Starts, refreshes or stops the dimmer to match the settings.
         * Safe to call often: the launcher calls it on every resume and
         * whenever a Night Dimming setting changes.
         */
        fun sync(context: Context) {
            val app = context.applicationContext
            val intent = Intent(app, ScreenDimService::class.java)
            val shouldRun = AppRepository(app).isDimAllAppsEnabled() && canDrawOverlays(app)
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
