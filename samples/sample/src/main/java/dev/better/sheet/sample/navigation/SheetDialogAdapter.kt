package dev.better.sheet.sample.navigation

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat
import dev.better.sheet.sample.R

/** Option 3: a stock <dialog> destination whose single window hosts BetterSheet.
 * Window policy mirrors BetterModalSheet; navigation and VM ownership stay with AndroidX.
 */
abstract class SheetDialogAdapter : ComposeContentFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.SheetDialogAdapterTheme)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).also { dialog ->
            dialog.window?.configureSheetWindow()
        }

    override fun onStart() {
        super.onStart()
        requireDialog().window?.apply {
            configureSheetWindow()
            setLayout(MATCH_PARENT, MATCH_PARENT)
        }
    }

    @Suppress("DEPRECATION")
    private fun Window.configureSheetWindow() {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        WindowCompat.setDecorFitsSystemWindows(this, false)
        statusBarColor = Color.TRANSPARENT
        navigationBarColor = Color.TRANSPARENT
        attributes = attributes.apply {
            // BetterSheet owns the animation, not the outer window.
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
        val activityWindow = requireActivity().window
        val host = WindowCompat.getInsetsController(activityWindow, activityWindow.decorView)
        WindowCompat.getInsetsController(this, decorView).apply {
            isAppearanceLightStatusBars = host.isAppearanceLightStatusBars
            isAppearanceLightNavigationBars = host.isAppearanceLightNavigationBars
        }
    }
}
