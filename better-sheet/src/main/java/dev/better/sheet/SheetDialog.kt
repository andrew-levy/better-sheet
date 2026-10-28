package dev.better.sheet

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat

/**
 * Configures a DialogFragment's window to host [BetterSheet], including its Compose-drawn scrim.
 * Call after `super.onStart()`, passing the Activity window to preserve system-bar icon appearance.
 *
 * Create the dialog with `R.style.BetterSheetDialogTheme` (or an equivalent non-floating,
 * transparent theme) before calling this helper. Window floating policy is fixed at creation;
 * this helper cannot turn a floating dialog into a non-floating one after it has been shown.
 * Keep the stock Fragment Navigation `<dialog>` destination and render [BetterSheet] as the root
 * Compose content. Use `onDismissed` to dismiss the Fragment after the sheet finishes hiding.
 * Direct Navigation pops retain the standard DialogFragment dismissal behavior.
 *
 * No Fragment or Navigation dependency is required by this helper.
 */
@Suppress("DEPRECATION")
public fun Dialog.configureForBetterSheet(hostWindow: Window) {
    val sheetWindow = requireNotNull(window) { "The dialog must have a window before configuring BetterSheet." }
    require(!sheetWindow.isFloating) {
        "Create this dialog with BetterSheetDialogTheme or a theme with android:windowIsFloating=false."
    }
    sheetWindow.apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        WindowCompat.setDecorFitsSystemWindows(this, false)
        statusBarColor = Color.TRANSPARENT
        navigationBarColor = Color.TRANSPARENT
        attributes = attributes.apply {
            windowAnimations = 0
            if (Build.VERSION.SDK_INT >= 30) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= 28) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        if (Build.VERSION.SDK_INT >= 29) {
            isStatusBarContrastEnforced = false
            isNavigationBarContrastEnforced = false
        }
        // Match Compose Dialog's IME policy; BetterSheet consumes the delivered insets.
        setSoftInputMode(if (Build.VERSION.SDK_INT >= 31)
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        else WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val host = WindowCompat.getInsetsController(hostWindow, hostWindow.decorView)
        WindowCompat.getInsetsController(this, decorView).apply {
            isAppearanceLightStatusBars = host.isAppearanceLightStatusBars
            isAppearanceLightNavigationBars = host.isAppearanceLightNavigationBars
        }
        setLayout(MATCH_PARENT, MATCH_PARENT)
    }
}
