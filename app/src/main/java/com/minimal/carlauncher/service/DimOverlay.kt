package com.minimal.carlauncher.service

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.minimal.carlauncher.data.AppRepository
import com.minimal.carlauncher.data.NightDimMode
import com.minimal.carlauncher.ui.viewmodel.LauncherViewModel
import java.util.Calendar

/**
 * The see-through black layer used by Dim All Apps. Shared by the two ways
 * of drawing it:
 * - [ScreenDimService]: a normal app layer ("Display over other apps").
 *   Android always keeps the top bar and the navigation bar above it.
 * - [ScreenDimAccessibilityService]: an accessibility layer, which Android
 *   allows above the top bar and the navigation bar too.
 *
 * The layer never catches taps.
 */
class DimOverlay(private val context: Context, private val windowType: Int) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: View? = null
    private var shownAlpha = -1f

    /** Shows the layer at [alpha] (0 = hidden, 1 = black), or hides it. */
    fun show(alpha: Float) {
        if (alpha <= 0f) {
            hide()
            return
        }
        val current = view
        if (current != null) {
            if (alpha != shownAlpha) {
                val params = current.layoutParams as WindowManager.LayoutParams
                params.alpha = alpha
                runCatching { windowManager.updateViewLayout(current, params) }
                shownAlpha = alpha
            }
            return
        }
        val newView = View(context).apply { setBackgroundColor(Color.BLACK) }
        try {
            windowManager.addView(newView, layoutParams(alpha))
            view = newView
            shownAlpha = alpha
        } catch (e: Exception) {
            // Permission was taken away, or the head unit refused the layer
            e.printStackTrace()
        }
    }

    fun hide() {
        val current = view ?: return
        runCatching { windowManager.removeView(current) }
        view = null
        shownAlpha = -1f
    }

    /** Call when the screen size or rotation changes, so the layer still covers everything. */
    fun refreshSize() {
        if (view == null) return
        val alpha = shownAlpha
        hide()
        show(alpha)
    }

    private fun layoutParams(alpha: Float): WindowManager.LayoutParams {
        // The full screen size, including the top bar and the navigation bar
        val (width, height) = fullScreenSize()
        return WindowManager.LayoutParams(
            width,
            height,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            // The window's own alpha (not the view's): on Android 12+ taps pass
            // through a see-through app layer only when this is 0.8 or less.
            this.alpha = alpha
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Do not shrink the layer to stay clear of the system bars
                fitInsetsTypes = 0
            }
        }
    }

    private fun fullScreenSize(): Pair<Int, Int> = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            metrics.widthPixels to metrics.heightPixels
        }
    } catch (e: Exception) {
        WindowManager.LayoutParams.MATCH_PARENT to WindowManager.LayoutParams.MATCH_PARENT
    }

    companion object {
        /** Android 12+ blocks taps through app layers darker than 0.8. */
        const val MAX_APP_LAYER_ALPHA = 0.8f

        /**
         * How dark the layer should be right now, from the Night Dimming
         * settings (mode, schedule, strength). 0 means no dimming.
         */
        fun currentAlpha(context: Context, repository: AppRepository): Float {
            if (!repository.isDimAllAppsEnabled()) return 0f
            val dimNow = when (repository.getNightDimMode()) {
                NightDimMode.OFF -> false
                NightDimMode.ALWAYS -> true
                NightDimMode.AUTO ->
                    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                        Configuration.UI_MODE_NIGHT_YES
                NightDimMode.SCHEDULE -> {
                    val now = Calendar.getInstance()
                    val minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                    LauncherViewModel.isWithinNight(
                        minute,
                        repository.getNightDimStartMinute(),
                        repository.getNightDimEndMinute()
                    )
                }
            }
            return if (dimNow) repository.getNightDimStrength().amount else 0f
        }
    }
}
