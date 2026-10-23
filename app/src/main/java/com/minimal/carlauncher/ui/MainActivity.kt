package com.minimal.carlauncher.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.minimal.carlauncher.ui.dashboard.DashboardScreen
import com.minimal.carlauncher.ui.theme.CarBg
import com.minimal.carlauncher.ui.theme.CarLauncherTheme
import com.minimal.carlauncher.ui.theme.minimalCarColors
import com.minimal.carlauncher.ui.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.speedometer.startTracking()
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

        try {
            checkAndRequestPermissions()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val isMinimalLayout by viewModel.isMinimalLayout.collectAsState()
            val minimalPalette by viewModel.minimalPalette.collectAsState()
            val isStatusBarShown by viewModel.isStatusBarShown.collectAsState()
            // The Minimal style always uses its own soft dark color theme
            CarLauncherTheme(
                isDarkMode = isDarkMode,
                colorsOverride = if (isMinimalLayout) minimalCarColors(minimalPalette) else null
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
                DashboardScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applySystemBars()
        viewModel.loadApps()
        viewModel.refreshRadio()
        if (hasLocationPermission()) {
            viewModel.speedometer.startTracking()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.speedometer.stopTracking()
    }

    private fun checkAndRequestPermissions() {
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        return try {
            val fine = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            fine || coarse
        } catch (e: Exception) {
            false
        }
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
