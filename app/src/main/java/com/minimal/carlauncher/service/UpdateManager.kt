package com.minimal.carlauncher.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class UpdateInfo(
    val tagName: String,
    val title: String,
    val changelog: String,
    val apkDownloadUrl: String,
    val apkSize: Long,
    val isUpdateAvailable: Boolean,
    /** URL of the "<apk>.sha256" release asset used to verify the download. */
    val checksumUrl: String
)

class UpdateManager(private val context: Context) {

    private val releasesApiBase = "https://api.github.com/repos/jprrabanal/Minimal-Car-Launcher/releases"
    private val releasesUrl = "$releasesApiBase/latest"

    fun getCurrentVersionName(): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    fun isNewerVersion(remoteTag: String, currentVersion: String = getCurrentVersionName()): Boolean {
        val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V")
        val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")

        val remoteParts = cleanRemote.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = cleanCurrent.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    suspend fun checkLatestRelease(): UpdateInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(releasesUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "MinimalCarLauncher-Android")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                // If "latest" release returns 404 (e.g. no release marked latest yet), fallback to list of releases
                return@withContext fetchFromReleasesList()
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)
            parseReleaseJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            fetchFromReleasesList()
        } finally {
            connection?.disconnect()
        }
    }

    private fun fetchFromReleasesList(): UpdateInfo? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(releasesApiBase)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "MinimalCarLauncher-Android")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(responseText)
                if (array.length() > 0) {
                    parseReleaseJson(array.getJSONObject(0))
                } else null
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseReleaseJson(json: JSONObject): UpdateInfo? {
        val tagName = json.optString("tag_name", "")
        val title = json.optString("name", tagName)
        val changelog = json.optString("body", "No changelog provided.")

        val assets = json.optJSONArray("assets") ?: return null
        val urlsByName = mutableMapOf<String, String>()
        val sizesByName = mutableMapOf<String, Long>()
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.optString("name", "")
            val downloadUrl = asset.optString("browser_download_url", "")
            if (name.isNotBlank() && downloadUrl.isNotBlank()) {
                urlsByName[name] = downloadUrl
                sizesByName[name] = asset.optLong("size", 0L)
            }
        }

        // Only offer releases that publish a SHA-256 checksum next to the APK,
        // so the download can be verified before it is installed.
        for ((name, downloadUrl) in urlsByName) {
            if (!name.endsWith(".apk", ignoreCase = true)) continue
            val checksumUrl = urlsByName["$name.sha256"] ?: continue
            return UpdateInfo(
                tagName = tagName,
                title = title,
                changelog = changelog,
                apkDownloadUrl = downloadUrl,
                apkSize = sizesByName[name] ?: 0L,
                isUpdateAvailable = isNewerVersion(tagName),
                checksumUrl = checksumUrl
            )
        }
        return null
    }

    /**
     * Opens an HTTPS connection, following up to 5 redirects (GitHub release
     * downloads redirect to a CDN). Any non-HTTPS URL is refused, so the APK
     * can never be fetched over an unencrypted connection.
     */
    private fun openHttpsFollowingRedirects(startUrl: String): HttpURLConnection {
        var currentUrl = URL(startUrl)
        repeat(6) {
            if (currentUrl.protocol != "https") {
                throw SecurityException("Refusing non-HTTPS URL: $currentUrl")
            }
            val connection = (currentUrl.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("User-Agent", "MinimalCarLauncher-Android")
            }
            val status = connection.responseCode
            if (status in listOf(301, 302, 303, 307, 308)) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                if (location.isNullOrBlank()) throw java.io.IOException("Redirect without Location")
                currentUrl = URL(currentUrl, location)
            } else {
                return connection
            }
        }
        throw java.io.IOException("Too many redirects")
    }

    private fun fetchExpectedSha256(checksumUrl: String): String? {
        val connection = openHttpsFollowingRedirects(checksumUrl)
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            // Format written by sha256sum: "<64 hex chars>  <file name>"
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            text.trim().split(Regex("\\s+")).firstOrNull()
                ?.lowercase()
                ?.takeIf { it.matches(Regex("[0-9a-f]{64}")) }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Downloads the release APK and checks it against the published SHA-256.
     * Returns null (and deletes the file) if anything fails or does not match.
     */
    suspend fun downloadApk(
        info: UpdateInfo,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val targetFile = File(context.cacheDir, "update.apk")
        var connection: HttpURLConnection? = null
        try {
            if (targetFile.exists()) {
                targetFile.delete()
            }

            val expectedSha256 = fetchExpectedSha256(info.checksumUrl)
                ?: return@withContext null

            val activeConnection = openHttpsFollowingRedirects(info.apkDownloadUrl)
            connection = activeConnection
            if (activeConnection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }
            val fileLength = activeConnection.contentLength
            val digest = MessageDigest.getInstance("SHA-256")

            BufferedInputStream(activeConnection.inputStream).use { input ->
                FileOutputStream(targetFile).use { output ->
                    val data = ByteArray(8192)
                    var total: Long = 0
                    var count: Int
                    while (input.read(data).also { count = it } != -1) {
                        total += count
                        output.write(data, 0, count)
                        digest.update(data, 0, count)
                        if (fileLength > 0) {
                            val progress = ((total * 100) / fileLength).toInt()
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                    output.flush()
                }
            }

            val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
            if (actualSha256 != expectedSha256) {
                targetFile.delete()
                return@withContext null
            }
            targetFile
        } catch (e: Exception) {
            e.printStackTrace()
            targetFile.delete()
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Opens the system installer. If this app is not yet allowed to install
     * apps, opens that permission screen instead and returns false; the user
     * then taps Install again.
     */
    fun promptInstall(apkFile: File): Boolean {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(permissionIntent)
                    return false
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
