package com.minimal.carlauncher.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Manifest-registered broadcast receiver to catch vendor radio broadcasts
 * even when the launcher is in the background or starting up.
 */
class RadioBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        // This receiver is exported, so any app can send it data. A malformed
        // intent must never crash the launcher (home screen).
        try {
            val isLauncher = intent.getBooleanExtra("is_launcher_source", false)
            DiagnosticsManager.logIntent(intent, isIncoming = !isLauncher)
            if (isLauncher) return
            RadioManager.onGlobalBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
