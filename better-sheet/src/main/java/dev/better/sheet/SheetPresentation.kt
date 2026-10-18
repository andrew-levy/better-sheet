package dev.better.sheet

import android.os.Build
import android.view.RoundedCorner
import android.view.View
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Surface geometry, independent of scrim color, touch handling, and animation timing. */
@Immutable
public sealed interface SheetPresentation {
    public data object Standard : SheetPresentation

    /**
     * Floats at lower detents, joining the host edges near full height. Insets are outside
     * the surface; system/IME safe areas are handled separately. [morphStartFraction] is a
     * fraction of available height, not of the largest detent: a short sheet stays a tray.
     * Tray owns its rounded shape (the sheet's `shape` applies to Standard only).
     * [matchDeviceCorners] uses reported Android 12+ bottom corners when the host fills its
     * window; otherwise [cornerRadius] is the fallback. This does not apply a glass material.
     */
    @Immutable
    public data class Tray(
        val horizontalInset: Dp = 12.dp,
        val bottomInset: Dp = 12.dp,
        val cornerRadius: Dp = 32.dp,
        val expandedCornerRadius: Dp = 28.dp,
        val morphStartFraction: Float = 0.6f,
        val matchDeviceCorners: Boolean = true,
    ) : SheetPresentation {
        init {
            require(listOf(horizontalInset, bottomInset, cornerRadius, expandedCornerRadius)
                .all { it.value.isFinite() && it >= 0.dp }) { "Tray dimensions must be finite and nonnegative" }
            require(morphStartFraction.isFinite() && morphStartFraction >= 0f && morphStartFraction < 1f)
        }
    }
}

/** Smooth endpoints, but no time-based animation: reversing a drag reverses this immediately. */
internal fun trayMorphFraction(visibleHeight: Float, availableHeight: Float, start: Float): Float {
    if (availableHeight <= 0f) return 0f
    val t = ((visibleHeight / availableHeight - start) / (1f - start)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

@Composable
internal fun rememberSheetDeviceCornerRadius(): Float {
    val view = LocalView.current
    var radius by remember(view) { mutableFloatStateOf(0f) }
    DisposableEffect(view) {
        fun update() {
            radius = if (Build.VERSION.SDK_INT >= 31 && view.width == view.rootView.width &&
                view.height == view.rootView.height) {
                listOf(RoundedCorner.POSITION_BOTTOM_LEFT, RoundedCorner.POSITION_BOTTOM_RIGHT)
                    .mapNotNull { view.rootWindowInsets?.getRoundedCorner(it)?.radius }
                    .maxOrNull()?.toFloat() ?: 0f
            } else 0f
        }
        val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> update() }
        val refresh = Runnable { update() }
        view.addOnLayoutChangeListener(listener)
        view.post(refresh)
        onDispose { view.removeOnLayoutChangeListener(listener); view.removeCallbacks(refresh) }
    }
    return radius
}
