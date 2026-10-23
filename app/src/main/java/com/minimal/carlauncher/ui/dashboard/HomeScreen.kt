package com.minimal.carlauncher.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minimal.carlauncher.ui.dialogs.AboutDialog
import com.minimal.carlauncher.ui.dialogs.AppPickerDialog
import com.minimal.carlauncher.ui.dialogs.DockActionDialog
import com.minimal.carlauncher.ui.dialogs.DrawerActionDialog
import com.minimal.carlauncher.ui.drawer.AppDrawerDialog
import com.minimal.carlauncher.ui.theme.CarBg
import com.minimal.carlauncher.ui.viewmodel.LauncherViewModel

/** The launcher's only screen: the Minimal home (clock, date, app bar) and its dialogs. */
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val allApps by viewModel.allApps.collectAsState()
    val pinnedApps by viewModel.pinnedApps.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isDrawerOpen by viewModel.isAppDrawerOpen.collectAsState()
    val zlinkApp by viewModel.zlinkApp.collectAsState()
    val minimalPalette by viewModel.minimalPalette.collectAsState()
    val minimalTime by viewModel.minimalTime.collectAsState()
    val minimalAmPm by viewModel.minimalAmPm.collectAsState()
    val minimalDate by viewModel.minimalDate.collectAsState()

    // Dialog States
    val selectedDockApp by viewModel.selectedDockAppForAction.collectAsState()
    val isReplacePickerOpen by viewModel.isReplacePickerOpen.collectAsState()
    val isAddDockPickerOpen by viewModel.isAddDockPickerOpen.collectAsState()
    val selectedDrawerApp by viewModel.selectedDrawerAppForAction.collectAsState()
    val isAutoStartPickerOpen by viewModel.isAutoStartPickerOpen.collectAsState()
    val isAutoStartEnabled by viewModel.isAutoStartEnabled.collectAsState()
    val autoStartApp by viewModel.autoStartApp.collectAsState()
    val isStatusBarShown by viewModel.isStatusBarShown.collectAsState()

    val currentVersion = viewModel.currentVersion
    val isAboutDialogOpen by viewModel.isAboutDialogOpen.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val updateInfo by viewModel.updateInfo.collectAsState()
    val updateProgress by viewModel.updateDownloadProgress.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CarBg)
            .padding(14.dp)
    ) {
        MinimalHomeScreen(
            time = minimalTime,
            amPm = minimalAmPm,
            date = minimalDate,
            zlinkApp = zlinkApp,
            pinnedApps = pinnedApps,
            isUpdateAvailable = updateInfo?.isUpdateAvailable == true,
            onOpenAppDrawer = { viewModel.openAppDrawer() },
            onLaunchZLink = { viewModel.launchZLink() },
            onLaunchApp = { app -> viewModel.launchApp(app) },
            onLongClickApp = { app -> viewModel.onDockAppLongClick(app) },
            onAddApp = { viewModel.openAddDockPicker() },
            onOpenAbout = { viewModel.openAboutDialog() }
        )

        // Overlay All Apps Drawer
        AppDrawerDialog(
            isOpen = isDrawerOpen,
            apps = filteredApps,
            searchQuery = searchQuery,
            onSearchChange = { viewModel.onSearchQueryChange(it) },
            onAppClick = { app -> viewModel.launchApp(app) },
            onAppLongClick = { app -> viewModel.onDrawerAppLongClick(app) },
            onClose = { viewModel.closeAppDrawer() }
        )

        // Dock Long-Press Action Dialog (Remove or Replace)
        DockActionDialog(
            app = selectedDockApp,
            onReplace = { viewModel.openReplacePicker() },
            onRemove = { selectedDockApp?.let { viewModel.removeDockApp(it) } },
            onDismiss = { viewModel.dismissDockActionDialog() }
        )

        // App Picker Dialog (when Replace is chosen for dock)
        AppPickerDialog(
            isOpen = isReplacePickerOpen,
            apps = allApps,
            title = "Choose App to Place in Bottom Bar",
            onAppSelected = { newApp -> viewModel.replaceDockAppWith(newApp) },
            onDismiss = { viewModel.closeReplacePicker() }
        )

        // App Picker Dialog (when + is clicked to add to bottom bar)
        AppPickerDialog(
            isOpen = isAddDockPickerOpen,
            apps = allApps.filter { app -> pinnedApps.none { it.packageName == app.packageName } },
            title = "Add App to Bottom Bar",
            onAppSelected = { newApp -> viewModel.addDockApp(newApp) },
            onDismiss = { viewModel.closeAddDockPicker() }
        )

        // Autostart App Picker Dialog (from the About & Settings dialog)
        AppPickerDialog(
            isOpen = isAutoStartPickerOpen,
            apps = allApps,
            title = "Select App to Open on Startup",
            onAppSelected = { app -> viewModel.selectAutoStartApp(app) },
            onDismiss = { viewModel.closeAutoStartPicker() }
        )

        // Drawer Long-Press Action Dialog (Add to Bottom Bar)
        DrawerActionDialog(
            app = selectedDrawerApp,
            onAddToDock = { selectedDrawerApp?.let { viewModel.pinDrawerAppToDock(it) } },
            onDismiss = { viewModel.dismissDrawerActionDialog() }
        )

        // About App & Software Update Dialog
        AboutDialog(
            isOpen = isAboutDialogOpen,
            currentVersion = currentVersion,
            updateInfo = updateInfo,
            isCheckingUpdate = isCheckingUpdate,
            downloadProgress = updateProgress,
            onCheckUpdate = { viewModel.checkForUpdates(isManualCheck = true) },
            onInstall = { viewModel.startDownloadAndInstall() },
            onDismiss = { viewModel.dismissAboutDialog() },
            minimalPalette = minimalPalette,
            onSelectMinimalPalette = { palette -> viewModel.selectMinimalPalette(palette) },
            isAutoStartEnabled = isAutoStartEnabled,
            autoStartAppLabel = autoStartApp?.label,
            onAutoStartEnabledChange = { enabled -> viewModel.setAutoStartEnabled(enabled) },
            onChooseAutoStartApp = { viewModel.openAutoStartPicker() },
            isStatusBarShown = isStatusBarShown,
            onStatusBarShownChange = { shown -> viewModel.setStatusBarShown(shown) }
        )
    }
}
