package com.minimal.carlauncher.ui

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.minimal.carlauncher.data.NightDimMode
import com.minimal.carlauncher.ui.dashboard.HomeScreen
import com.minimal.carlauncher.ui.theme.CarBg
import com.minimal.carlauncher.ui.theme.CarLauncherTheme
import com.minimal.carlauncher.ui.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    // Location for the Dashboard's speed and weather
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.onLocationPermissionResult() }

    /**
     * Asks for location. If the user said "Don't ask again" before, Android
     * shows nothing, so open the app's settings page instead.
     */
    private fun requestLocationPermission() {
        val askedBefore = getPreferences(MODE_PRIVATE).getBoolean(KEY_ASKED_LOCATION, false)
        val canShowDialog = !askedBefore ||
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
        if (canShowDialog) {
            getPreferences(MODE_PRIVATE).edit().putBoolean(KEY_ASKED_LOCATION, true).apply()
            locationPermissionRequest.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else {
            viewModel.openLocationAppSettings()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applySystemBars()

        // Hardware Back: open dialogs close themselves; on the home screen the
        // launcher never closes, and Back returns to the last opened app.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.onHomeBack()
            }
        })

        setContent {
            val minimalPalette by viewModel.minimalPalette.collectAsState()
            val isStatusBarShown by viewModel.isStatusBarShown.collectAsState()
            val nightDimMode by viewModel.nightDimMode.collectAsState()
            val isScheduledDim by viewModel.isNightDimmed.collectAsState()
            // Auto: follow the head unit's own night mode (on many units, the headlights)
            val headUnitNight = isSystemInDarkTheme()
            val isNightDimmed = if (nightDimMode == NightDimMode.AUTO) headUnitNight else isScheduledDim
            val nightDimStrength by viewModel.nightDimStrength.collectAsState()
            CarLauncherTheme(
                palette = minimalPalette,
                isNightDimmed = isNightDimmed,
                nightDimStrength = nightDimStrength.amount
            ) {
                // The top bar blends into the launcher background
                val barColor = CarBg
                SideEffect {
                    applySystemBars(isStatusBarShown)
                    try {
                        @Suppress("DEPRECATION")
                        window.statusBarColor = barColor.toArgb()
                        WindowCompat.getInsetsController(window, window.decorView)
                            .isAppearanceLightStatusBars = barColor.luminance() > 0.5f
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                HomeScreen(
                    viewModel = viewModel,
                    onRequestLocationPermission = { requestLocationPermission() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applySystemBars()
        viewModel.loadApps()
        viewModel.onScreenVisible()
    }

    override fun onPause() {
        viewModel.onScreenHidden()
        super.onPause()
    }

    companion object {
        private const val KEY_ASKED_LOCATION = "asked_location_permission"
    }

    /** Shows or hides the head unit's top bar (status bar), as chosen in settings. */
    private fun applySystemBars(show: Boolean = viewModel.isStatusBarShown.value) {
        window.decorView.post {
            try {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                if (show) {
                    @Suppress("DEPRECATION")
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    WindowCompat.setDecorFitsSystemWindows(window, true)
                    controller.show(WindowInsetsCompat.Type.statusBars())
                } else {
                    @Suppress("DEPRECATION")
                    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    controller.hide(WindowInsetsCompat.Type.statusBars())
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
