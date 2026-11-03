package com.minimal.carlauncher.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/**
 * Reads the head unit's GPS while the Dashboard is on screen: the current
 * speed (km/h) and the last known place (used for the weather).
 */
class LocationMonitor(private val context: Context) {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    /** Speed in km/h, or null when there is no recent GPS fix. */
    private val _speedKmh = MutableStateFlow<Int?>(null)
    val speedKmh: StateFlow<Int?> = _speedKmh.asStateFlow()

    private val _lastLocation = MutableStateFlow<Location?>(null)
    val lastLocation: StateFlow<Location?> = _lastLocation.asStateFlow()

    private var lastSpeedFixElapsed = 0L
    private var isStarted = false

    private val gpsListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _lastLocation.value = location
            if (location.hasSpeed()) {
                // Below about 2 km/h the GPS speed is mostly noise: show 0
                val kmh = location.speed * 3.6f
                _speedKmh.value = if (kmh < 2f) 0 else kmh.roundToInt()
                lastSpeedFixElapsed = SystemClock.elapsedRealtime()
            }
        }

        // Needed on Android 9 and older, where these methods have no default
        @Deprecated("Deprecated in Android")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {
            _speedKmh.value = null
        }
    }

    private val networkListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Rough place from Wi-Fi / mobile data; only used when GPS has nothing better
            val current = _lastLocation.value
            if (current == null || current.provider != LocationManager.GPS_PROVIDER) {
                _lastLocation.value = location
            }
        }

        @Deprecated("Deprecated in Android")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // Checked by hasPermission()
    fun start() {
        // A local copy, so Kotlin knows it stays non-null inside the lambdas below
        val locationManager = locationManager ?: return
        if (isStarted || !hasPermission()) return
        try {
            val providers = locationManager.getProviders(true)
            if (LocationManager.GPS_PROVIDER in providers) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1000L, 0f, gpsListener, Looper.getMainLooper()
                )
            }
            if (LocationManager.NETWORK_PROVIDER in providers) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 5 * 60_000L, 500f, networkListener, Looper.getMainLooper()
                )
            }
            if (_lastLocation.value == null) {
                _lastLocation.value = providers
                    .mapNotNull { runCatching { locationManager.getLastKnownLocation(it) }.getOrNull() }
                    .maxByOrNull { it.time }
            }
            isStarted = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        if (!isStarted) return
        runCatching { locationManager?.removeUpdates(gpsListener) }
        runCatching { locationManager?.removeUpdates(networkListener) }
        isStarted = false
        _speedKmh.value = null
    }

    /** Call about once a second: hides the speed when the GPS stops sending it. */
    fun clearStaleSpeed() {
        if (_speedKmh.value != null &&
            SystemClock.elapsedRealtime() - lastSpeedFixElapsed > STALE_SPEED_MS
        ) {
            _speedKmh.value = null
        }
    }

    companion object {
        private const val STALE_SPEED_MS = 5_000L
    }
}
