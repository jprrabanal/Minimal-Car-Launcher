package com.minimal.carlauncher.service

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

/** The current weather, for example 31°C, "Rain", in "Quezon City". */
data class WeatherInfo(
    val temperatureC: Int,
    val description: String,
    val isDay: Boolean,
    val code: Int,
    val place: String?
)

/**
 * Gets the current weather from Open-Meteo (https://open-meteo.com).
 * It is free and needs no account or key. Only the rounded place
 * (latitude and longitude) is sent.
 */
class WeatherService(private val context: Context) {

    suspend fun fetch(latitude: Double, longitude: Double): WeatherInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            // Two decimals is about 1 km: enough for the weather, and more private
            val lat = String.format(Locale.US, "%.2f", latitude)
            val lon = String.format(Locale.US, "%.2f", longitude)
            val url = URL(
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weather_code,is_day&timezone=auto"
            )
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", "MinimalCarLauncher-Android")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val current = JSONObject(body).getJSONObject("current")
            val code = current.optInt("weather_code", -1)
            WeatherInfo(
                temperatureC = current.getDouble("temperature_2m").roundToInt(),
                description = describe(code),
                isDay = current.optInt("is_day", 1) == 1,
                code = code,
                place = placeName(latitude, longitude)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }

    /** City or town name from the head unit's own geocoder, when it has one. */
    private fun placeName(latitude: Double, longitude: Double): String? = try {
        if (!Geocoder.isPresent()) {
            null
        } else {
            @Suppress("DEPRECATION")
            Geocoder(context, Locale.getDefault()).getFromLocation(latitude, longitude, 1)
                ?.firstOrNull()
                ?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
        }
    } catch (e: Exception) {
        null
    }

    companion object {
        /** WMO weather codes in simple words. */
        fun describe(code: Int): String = when (code) {
            0 -> "Clear"
            1 -> "Mostly Clear"
            2 -> "Partly Cloudy"
            3 -> "Cloudy"
            45, 48 -> "Fog"
            51, 53, 55, 56, 57 -> "Drizzle"
            61, 63, 66, 80, 81 -> "Rain"
            65, 67, 82 -> "Heavy Rain"
            71, 73, 75, 77, 85, 86 -> "Snow"
            95, 96, 99 -> "Thunderstorm"
            else -> "—"
        }
    }
}
