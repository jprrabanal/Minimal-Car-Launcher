package com.minimal.carlauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val prefs: SharedPreferences =
        context.getSharedPreferences("car_launcher_dock_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PINNED_PACKAGES = "key_pinned_dock_packages"
        private const val KEY_DOCK_INITIALIZED_EMPTY = "key_dock_initialized_empty_v2"
        private const val KEY_AUTOSTART_ENABLED = "key_autostart_enabled"
        private const val KEY_AUTOSTART_PACKAGE = "key_autostart_package"
        private const val KEY_AUTOSTART_LAST_ELAPSED = "key_autostart_last_elapsed"
        private const val KEY_AUTOSTART_LAST_BOOT_COUNT = "key_autostart_last_boot_count"
        private const val KEY_MINIMAL_PALETTE = "key_minimal_palette"
        private const val KEY_SHOW_STATUS_BAR = "key_show_status_bar"
        private const val KEY_LAST_LAUNCHED_PACKAGE = "key_last_launched_package"
        const val MAX_DOCK_APPS = 6
    }

    // Known ZLink package signatures found on Android head units
    private val zlinkPackages = setOf(
        "com.zjinnova.zlink",
        "com.zjinnova.zlinkx",
        "com.xyauto.zlink",
        "com.carletter.zlink",
        "com.syu.zlink",
        "com.suding.speedplay",
        "cn.manstep.phonemirrorbox",
        "com.autonavi.amapauto"
    )

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfos: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, 0)
            }

            val selfPackage = context.packageName

            val apps = resolveInfos.mapNotNull { resolveInfo ->
                try {
                    val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                    val pkg = activityInfo.packageName ?: return@mapNotNull null
                    if (pkg == selfPackage) return@mapNotNull null

                    val label = try {
                        resolveInfo.loadLabel(packageManager)?.toString() ?: pkg
                    } catch (e: Throwable) {
                        pkg
                    }

                    val icon = try {
                        resolveInfo.loadIcon(packageManager)
                    } catch (e: Throwable) {
                        null
                    }

                    val activityName = activityInfo.name ?: ""

                    val isZLink = isZLinkPackage(pkg, label)

                    AppInfo(
                        label = label,
                        packageName = pkg,
                        activityName = activityName,
                        icon = icon,
                        isZLink = isZLink
                    )
                } catch (e: Throwable) {
                    null
                }
            }

            apps.sortedBy { it.label.lowercase() }
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
        }
    }

    // --- Dock Persistence ---

    fun getPinnedPackageNames(): List<String> {
        val jsonString = prefs.getString(KEY_PINNED_PACKAGES, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun savePinnedPackageNames(packages: List<String>) {
        val jsonArray = JSONArray(packages)
        prefs.edit().putString(KEY_PINNED_PACKAGES, jsonArray.toString()).apply()
    }

    fun getPinnedApps(allApps: List<AppInfo>): List<AppInfo> {
        // By default, the bottom app bar starts empty.
        // If not initialized yet in this empty-by-default version, initialize to empty list.
        if (!prefs.getBoolean(KEY_DOCK_INITIALIZED_EMPTY, false)) {
            savePinnedPackageNames(emptyList())
            prefs.edit().putBoolean(KEY_DOCK_INITIALIZED_EMPTY, true).apply()
            return emptyList()
        }

        val saved = getPinnedPackageNames()
        if (saved.isEmpty()) {
            return emptyList()
        }

        val appMap = allApps.associateBy { it.packageName }
        return saved.mapNotNull { appMap[it] }
    }

    fun addPinnedApp(allApps: List<AppInfo>, newApp: AppInfo): Boolean {
        if (!prefs.getBoolean(KEY_DOCK_INITIALIZED_EMPTY, false)) {
            prefs.edit().putBoolean(KEY_DOCK_INITIALIZED_EMPTY, true).apply()
        }
        val current = getPinnedApps(allApps).toMutableList()
        if (current.any { it.packageName == newApp.packageName }) {
            return false // Already pinned
        }
        if (current.size >= MAX_DOCK_APPS) {
            return false // Full dock
        }
        current.add(newApp)
        savePinnedPackageNames(current.map { it.packageName })
        return true
    }

    fun removePinnedApp(allApps: List<AppInfo>, appToRemove: AppInfo) {
        val current = getPinnedApps(allApps).toMutableList()
        current.removeAll { it.packageName == appToRemove.packageName }
        savePinnedPackageNames(current.map { it.packageName })
    }

    fun replacePinnedApp(allApps: List<AppInfo>, oldApp: AppInfo, newApp: AppInfo) {
        val current = getPinnedApps(allApps).toMutableList()
        val index = current.indexOfFirst { it.packageName == oldApp.packageName }
        if (index != -1) {
            current[index] = newApp
        } else if (current.size < MAX_DOCK_APPS) {
            current.add(newApp)
        }
        savePinnedPackageNames(current.map { it.packageName })
    }

    fun isAutoStartEnabled(): Boolean = prefs.getBoolean(KEY_AUTOSTART_ENABLED, false)

    fun setAutoStartEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOSTART_ENABLED, enabled).apply()
    }

    fun getAutoStartPackage(): String? = prefs.getString(KEY_AUTOSTART_PACKAGE, null)

    fun setAutoStartPackage(packageName: String) {
        prefs.edit().putString(KEY_AUTOSTART_PACKAGE, packageName).apply()
    }

    /**
     * Returns true only the first time it is called after a device boot.
     * Uses the system boot counter when the head unit provides it. Otherwise it
     * falls back to elapsedRealtime (time since boot), which resets on every boot
     * and is not affected by GPS / network wall-clock changes.
     * If the launcher process restarts mid-drive, this returns false, so the
     * autostart app is not opened again.
     */
    fun consumeAutoStartForThisBoot(): Boolean {
        val bootCount = readBootCount()
        val lastBootCount = prefs.getInt(KEY_AUTOSTART_LAST_BOOT_COUNT, -1)
        val lastElapsed = prefs.getLong(KEY_AUTOSTART_LAST_ELAPSED, -1L)
        val isNewBoot = if (bootCount >= 0 && lastBootCount >= 0) {
            bootCount != lastBootCount
        } else {
            lastElapsed < 0L || SystemClock.elapsedRealtime() < lastElapsed
        }
        markAutoStartHandledForThisBoot()
        return isNewBoot
    }

    /** Records the current boot as handled. Also called periodically to keep the uptime fallback fresh. */
    fun markAutoStartHandledForThisBoot() {
        prefs.edit()
            .putInt(KEY_AUTOSTART_LAST_BOOT_COUNT, readBootCount())
            .putLong(KEY_AUTOSTART_LAST_ELAPSED, SystemClock.elapsedRealtime())
            .apply()
    }

    private fun readBootCount(): Int {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
        } catch (e: Exception) {
            -1
        }
    }

    /** Show the head unit's top bar (status bar). On by default. */
    fun isStatusBarShown(): Boolean = prefs.getBoolean(KEY_SHOW_STATUS_BAR, true)

    fun setStatusBarShown(shown: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_STATUS_BAR, shown).apply()
    }

    /** The last app opened from the launcher, so the Back button can return to it. */
    fun getLastLaunchedPackage(): String? = prefs.getString(KEY_LAST_LAUNCHED_PACKAGE, null)

    private fun rememberLaunched(packageName: String) {
        if (packageName != context.packageName) {
            prefs.edit().putString(KEY_LAST_LAUNCHED_PACKAGE, packageName).apply()
        }
    }

    fun getMinimalPaletteName(): String? = prefs.getString(KEY_MINIMAL_PALETTE, null)

    fun setMinimalPaletteName(name: String) {
        prefs.edit().putString(KEY_MINIMAL_PALETTE, name).apply()
    }

    // --- Package Helpers ---

    fun isZLinkPackage(pkg: String, label: String = ""): Boolean {
        val lowerPkg = pkg.lowercase()
        val lowerLabel = label.lowercase()
        return zlinkPackages.contains(pkg) ||
                lowerPkg.contains("zlink") ||
                lowerPkg.contains("phonemirror") ||
                lowerPkg.contains("speedplay") ||
                lowerPkg.contains("autokit") ||
                lowerLabel.contains("zlink") ||
                lowerLabel.contains("android auto")
    }

    fun launchApp(appInfo: AppInfo): Boolean {
        return launchPackageAndActivity(appInfo.packageName, appInfo.activityName)
    }

    fun launchPackage(packageName: String): Boolean {
        return try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                rememberLaunched(packageName)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun launchPackageAndActivity(packageName: String, activityName: String? = null): Boolean {
        return try {
            if (!activityName.isNullOrBlank()) {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    component = ComponentName(packageName, activityName)
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                rememberLaunched(packageName)
                true
            } else {
                launchPackage(packageName)
            }
        } catch (e: Exception) {
            launchPackage(packageName)
        }
    }
}
