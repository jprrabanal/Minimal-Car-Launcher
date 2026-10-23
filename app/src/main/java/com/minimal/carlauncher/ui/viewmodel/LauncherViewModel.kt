package com.minimal.carlauncher.ui.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minimal.carlauncher.data.AppInfo
import com.minimal.carlauncher.data.AppRepository
import com.minimal.carlauncher.service.UpdateInfo
import com.minimal.carlauncher.service.UpdateManager
import com.minimal.carlauncher.ui.theme.MinimalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val updateManager = UpdateManager(application)

    // Applications State
    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = _allApps.asStateFlow()

    private val _pinnedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val pinnedApps: StateFlow<List<AppInfo>> = _pinnedApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredApps: StateFlow<List<AppInfo>> = combine(_allApps, _searchQuery) { apps, query ->
        if (query.isBlank()) {
            apps
        } else {
            apps.filter { it.label.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _zlinkApp = MutableStateFlow<AppInfo?>(null)
    val zlinkApp: StateFlow<AppInfo?> = _zlinkApp.asStateFlow()

    // Autostart App (opened once after each head unit boot)
    private val _isAutoStartEnabled = MutableStateFlow(repository.isAutoStartEnabled())
    val isAutoStartEnabled: StateFlow<Boolean> = _isAutoStartEnabled.asStateFlow()

    private val _autoStartApp = MutableStateFlow<AppInfo?>(null)
    val autoStartApp: StateFlow<AppInfo?> = _autoStartApp.asStateFlow()

    private val _isStatusBarShown = MutableStateFlow(repository.isStatusBarShown())
    val isStatusBarShown: StateFlow<Boolean> = _isStatusBarShown.asStateFlow()

    private val _isAutoStartPickerOpen = MutableStateFlow(false)
    val isAutoStartPickerOpen: StateFlow<Boolean> = _isAutoStartPickerOpen.asStateFlow()

    private val _isAppDrawerOpen = MutableStateFlow(false)
    val isAppDrawerOpen: StateFlow<Boolean> = _isAppDrawerOpen.asStateFlow()

    // Minimal home screen clock: 12-hour time without seconds, e.g. "9:41" + "PM"
    // and a full date, e.g. "Monday, Aug. 21, 2026"
    private val _minimalTime = MutableStateFlow("")
    val minimalTime: StateFlow<String> = _minimalTime.asStateFlow()

    private val _minimalAmPm = MutableStateFlow("")
    val minimalAmPm: StateFlow<String> = _minimalAmPm.asStateFlow()

    private val _minimalDate = MutableStateFlow("")
    val minimalDate: StateFlow<String> = _minimalDate.asStateFlow()

    private val _minimalPalette = MutableStateFlow(MinimalPalette.fromName(repository.getMinimalPaletteName()))
    val minimalPalette: StateFlow<MinimalPalette> = _minimalPalette.asStateFlow()

    // Long press action dialog states
    private val _selectedDockAppForAction = MutableStateFlow<AppInfo?>(null)
    val selectedDockAppForAction: StateFlow<AppInfo?> = _selectedDockAppForAction.asStateFlow()

    private val _isReplacePickerOpen = MutableStateFlow(false)
    val isReplacePickerOpen: StateFlow<Boolean> = _isReplacePickerOpen.asStateFlow()

    private val _isAddDockPickerOpen = MutableStateFlow(false)
    val isAddDockPickerOpen: StateFlow<Boolean> = _isAddDockPickerOpen.asStateFlow()

    private val _selectedDrawerAppForAction = MutableStateFlow<AppInfo?>(null)
    val selectedDrawerAppForAction: StateFlow<AppInfo?> = _selectedDrawerAppForAction.asStateFlow()

    // About & In-App Updater State
    val currentVersion: String = updateManager.getCurrentVersionName()

    private val _isAboutDialogOpen = MutableStateFlow(false)
    val isAboutDialogOpen: StateFlow<Boolean> = _isAboutDialogOpen.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _updateDownloadProgress = MutableStateFlow<Int?>(null)
    val updateDownloadProgress: StateFlow<Int?> = _updateDownloadProgress.asStateFlow()

    init {
        loadApps()
        startClockTicker()
        runAutoStartIfNeeded()
    }

    private fun runAutoStartIfNeeded() {
        val pkg = repository.getAutoStartPackage()
        if (repository.isAutoStartEnabled() && pkg != null && repository.consumeAutoStartForThisBoot()) {
            viewModelScope.launch {
                // Give the head unit a few seconds to finish booting before opening the app
                delay(AUTOSTART_DELAY_MS)
                repository.launchPackage(pkg)
            }
        }
        // Keep the stored uptime fresh, used by the boot check on head units without a boot counter
        viewModelScope.launch {
            while (isActive) {
                delay(AUTOSTART_HEARTBEAT_MS)
                if (_isAutoStartEnabled.value) repository.markAutoStartHandledForThisBoot()
            }
        }
    }

    fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = repository.getInstalledApps()
            val pinned = repository.getPinnedApps(apps)
            val zlink = apps.firstOrNull { it.isZLink }

            _allApps.value = apps
            _pinnedApps.value = pinned
            _zlinkApp.value = zlink
            _autoStartApp.value = repository.getAutoStartPackage()?.let { pkg ->
                apps.firstOrNull { it.packageName == pkg }
            }
        }
    }

    private fun startClockTicker() {
        viewModelScope.launch {
            val minimalTimeFormat = SimpleDateFormat("h:mm", Locale.getDefault())
            val amPmFormat = SimpleDateFormat("a", Locale.getDefault())

            while (isActive) {
                val now = Date()
                _minimalTime.value = minimalTimeFormat.format(now)
                _minimalAmPm.value = amPmFormat.format(now).uppercase(Locale.getDefault())
                _minimalDate.value = formatMinimalDate(now)
                delay(1000L)
            }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun openAppDrawer() {
        _searchQuery.value = ""
        _isAppDrawerOpen.value = true
    }

    fun closeAppDrawer() {
        _isAppDrawerOpen.value = false
        _searchQuery.value = ""
    }

    fun launchApp(app: AppInfo) {
        repository.launchApp(app)
        closeAppDrawer()
    }

    // --- Dock Customization ---

    fun onDockAppLongClick(app: AppInfo) {
        _selectedDockAppForAction.value = app
    }

    fun dismissDockActionDialog() {
        _selectedDockAppForAction.value = null
    }

    fun removeDockApp(app: AppInfo) {
        repository.removePinnedApp(_allApps.value, app)
        _pinnedApps.value = repository.getPinnedApps(_allApps.value)
        _selectedDockAppForAction.value = null
        Toast.makeText(getApplication(), "Removed from bottom bar", Toast.LENGTH_SHORT).show()
    }

    fun openReplacePicker() {
        _isReplacePickerOpen.value = true
    }

    fun closeReplacePicker() {
        _isReplacePickerOpen.value = false
        _selectedDockAppForAction.value = null
    }

    fun replaceDockAppWith(newApp: AppInfo) {
        val oldApp = _selectedDockAppForAction.value ?: return
        repository.replacePinnedApp(_allApps.value, oldApp, newApp)
        _pinnedApps.value = repository.getPinnedApps(_allApps.value)
        _isReplacePickerOpen.value = false
        _selectedDockAppForAction.value = null
        Toast.makeText(getApplication(), "Replaced with ${newApp.label}", Toast.LENGTH_SHORT).show()
    }

    fun openAddDockPicker() {
        _isAddDockPickerOpen.value = true
    }

    fun closeAddDockPicker() {
        _isAddDockPickerOpen.value = false
    }

    fun addDockApp(newApp: AppInfo) {
        val added = repository.addPinnedApp(_allApps.value, newApp)
        _pinnedApps.value = repository.getPinnedApps(_allApps.value)
        _isAddDockPickerOpen.value = false
        if (added) {
            Toast.makeText(getApplication(), "Added ${newApp.label} to bottom bar", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(getApplication(), "${newApp.label} is already in the bottom bar", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Drawer Long Press to Pin ---

    fun onDrawerAppLongClick(app: AppInfo) {
        _selectedDrawerAppForAction.value = app
    }

    fun dismissDrawerActionDialog() {
        _selectedDrawerAppForAction.value = null
    }

    fun pinDrawerAppToDock(app: AppInfo) {
        val added = repository.addPinnedApp(_allApps.value, app)
        _pinnedApps.value = repository.getPinnedApps(_allApps.value)
        _selectedDrawerAppForAction.value = null
        if (added) {
            Toast.makeText(getApplication(), "Added ${app.label} to bottom bar", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(getApplication(), "${app.label} is already in the bottom bar", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Autostart App ---

    fun setAutoStartEnabled(enabled: Boolean) {
        if (enabled && repository.getAutoStartPackage() == null) {
            // No app chosen yet: ask the user to pick one first
            _isAutoStartPickerOpen.value = true
            return
        }
        // Mark the current boot as handled so turning it on does not open the app mid-drive
        repository.markAutoStartHandledForThisBoot()
        repository.setAutoStartEnabled(enabled)
        _isAutoStartEnabled.value = enabled
    }

    fun setStatusBarShown(shown: Boolean) {
        repository.setStatusBarShown(shown)
        _isStatusBarShown.value = shown
    }

    /**
     * Hardware Back on the home screen (open dialogs close themselves first):
     * close the app drawer if it is open, otherwise go back to the last app
     * opened from the launcher, like Android Auto.
     */
    fun onHomeBack() {
        if (_isAppDrawerOpen.value) {
            closeAppDrawer()
            return
        }
        repository.getLastLaunchedPackage()?.let { repository.launchPackage(it) }
    }

    fun openAutoStartPicker() {
        _isAutoStartPickerOpen.value = true
    }

    fun closeAutoStartPicker() {
        _isAutoStartPickerOpen.value = false
    }

    fun selectAutoStartApp(app: AppInfo) {
        repository.setAutoStartPackage(app.packageName)
        repository.markAutoStartHandledForThisBoot()
        repository.setAutoStartEnabled(true)
        _autoStartApp.value = app
        _isAutoStartEnabled.value = true
        _isAutoStartPickerOpen.value = false
        Toast.makeText(getApplication(), "${app.label} will open when the car starts", Toast.LENGTH_SHORT).show()
    }

    // --- About & In-App GitHub Updater ---

    fun openAboutDialog() {
        _isAboutDialogOpen.value = true
        if (_updateInfo.value == null && !_isCheckingUpdate.value) {
            checkForUpdates(isManualCheck = false)
        }
    }

    fun dismissAboutDialog() {
        if (_updateDownloadProgress.value == null) {
            _isAboutDialogOpen.value = false
        }
    }

    fun checkForUpdates(isManualCheck: Boolean = true) {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val release = updateManager.checkLatestRelease()
            _isCheckingUpdate.value = false

            if (release != null) {
                _updateInfo.value = release
                if (isManualCheck) {
                    if (release.isUpdateAvailable) {
                        Toast.makeText(getApplication(), "Update available: ${release.tagName}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(getApplication(), "You're running the latest version", Toast.LENGTH_SHORT).show()
                    }
                }
            } else if (isManualCheck) {
                Toast.makeText(getApplication(), "No updates found on GitHub", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun startDownloadAndInstall() {
        val info = _updateInfo.value ?: return
        viewModelScope.launch {
            _updateDownloadProgress.value = 0
            val downloadedFile = updateManager.downloadApk(info) { progress ->
                _updateDownloadProgress.value = progress
            }

            if (downloadedFile != null && downloadedFile.exists()) {
                _updateDownloadProgress.value = null
                if (updateManager.promptInstall(downloadedFile)) {
                    _isAboutDialogOpen.value = false
                } else {
                    Toast.makeText(getApplication(), "Allow installing apps for Car Launcher, then tap Install again.", Toast.LENGTH_LONG).show()
                }
            } else {
                _updateDownloadProgress.value = null
                Toast.makeText(getApplication(), "Download failed or could not be verified. Please try again.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- Other Shortcuts ---

    fun launchZLink() {
        val zlink = _zlinkApp.value
        if (zlink != null) {
            repository.launchApp(zlink)
        } else {
            val launched = repository.launchPackage("com.zjinnova.zlink") ||
                    repository.launchPackage("com.zjinnova.zlinkx") ||
                    repository.launchPackage("com.xyauto.zlink") ||
                    repository.launchPackage("com.carletter.zlink") ||
                    repository.launchPackage("com.suding.speedplay")
            if (!launched) {
                openAppDrawer()
            }
        }
    }

    fun selectMinimalPalette(palette: MinimalPalette) {
        _minimalPalette.value = palette
        repository.setMinimalPaletteName(palette.name)
    }

    companion object {
        private const val AUTOSTART_DELAY_MS = 4000L
        private const val AUTOSTART_HEARTBEAT_MS = 60_000L

        /** Formats a date like "Monday, Aug. 21, 2026" ("May" has no dot, as it is not shortened). */
        fun formatMinimalDate(date: Date, locale: Locale = Locale.getDefault()): String {
            val dayName = SimpleDateFormat("EEEE", locale).format(date)
            val shortMonth = SimpleDateFormat("MMM", locale).format(date).trimEnd('.')
            val fullMonth = SimpleDateFormat("MMMM", locale).format(date)
            val month = if (shortMonth == fullMonth) shortMonth else "$shortMonth."
            val dayAndYear = SimpleDateFormat("d, yyyy", locale).format(date)
            return "$dayName, $month $dayAndYear"
        }
    }
}
