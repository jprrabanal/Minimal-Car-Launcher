package com.minimal.carlauncher.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings

/** Screen brightness levels for the Dashboard's Brightness button. */
enum class BrightnessLevel(val label: String, val value: Int) {
    LOW("Low", 40),
    MEDIUM("Medium", 140),
    HIGH("High", 255);

    fun next(): BrightnessLevel = values()[(ordinal + 1) % values().size]

    companion object {
        fun closestTo(value: Int): BrightnessLevel =
            values().minByOrNull { kotlin.math.abs(it.value - value) } ?: MEDIUM
    }
}

/** Dashboard quick buttons: brightness, volume, Bluetooth and Wi-Fi. */
class QuickControls(private val context: Context) {

    fun canChangeBrightness(): Boolean = Settings.System.canWrite(context)

    fun currentBrightness(): BrightnessLevel = try {
        BrightnessLevel.closestTo(
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        )
    } catch (e: Exception) {
        BrightnessLevel.MEDIUM
    }

    /** Sets the screen brightness. Returns false if the permission is missing. */
    fun setBrightness(level: BrightnessLevel): Boolean {
        if (!canChangeBrightness()) return false
        return try {
            val resolver = context.contentResolver
            Settings.System.putInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, level.value)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Opens the screen where the user allows the launcher to change the brightness. */
    fun openBrightnessPermission() {
        startSettings(
            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
        )
    }

    /** Shows the system volume slider for music. */
    fun showVolumePanel() {
        try {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openBluetoothSettings() = startSettings(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))

    fun openWifiSettings() = startSettings(Intent(Settings.ACTION_WIFI_SETTINGS))

    fun openNotificationAccessSettings() =
        startSettings(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))

    fun openAppSettings() = startSettings(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    )

    /** Opens a settings screen. Falls back to the main Settings app if the head unit lacks it. */
    private fun startSettings(intent: Intent): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } catch (e2: Exception) {
                false
            }
        }
    }
}
