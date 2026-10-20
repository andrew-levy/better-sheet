package dev.better.sheet

import android.os.Build
import android.view.RoundedCorner
import android.view.ViewTreeObserver
import androidx.compose.runtime.*
import androidx.compose.ui.window.DialogWindowProvider
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

/**
 * Let the full rounded shadow fade before the tray enters the navigation area. Keeping a
 * directional elevation shadow during that morph makes an offset, translucent shelf below it;
 * cutting it off at the bottom instead leaves hard seams beside the rounded corners.
 */
internal fun trayShadowFraction(visibleHeight: Float, availableHeight: Float, morphStart: Float): Float {
    if (availableHeight <= 0f || morphStart <= 0f) return 0f
    val fadeStart = (morphStart - 0.1f).coerceAtLeast(0f)
    val t = ((visibleHeight / availableHeight - fadeStart) / (morphStart - fadeStart)).coerceIn(0f, 1f)
    return 1f - t * t * (3f - 2f * t)
}

@Composable
internal fun rememberSheetDeviceCornerRadius(): Float {
    val view = LocalView.current
    var radius by remember(view) { mutableFloatStateOf(0f) }
    DisposableEffect(view) {
        // Dialog Compose roots need not have the decor's exact measured height. Read the
        // window metadata directly and refresh after inset dispatch without replacing the
        // host's inset listener. Some devices supply corners only after the first layout.
        val observer = view.viewTreeObserver
        val listener = ViewTreeObserver.OnPreDrawListener {
            radius = if (Build.VERSION.SDK_INT >= 31) {
                val decor = (view.parent as? DialogWindowProvider)?.window?.decorView ?: view.rootView
                val insets = decor.rootWindowInsets ?: view.rootWindowInsets
                listOf(RoundedCorner.POSITION_BOTTOM_LEFT, RoundedCorner.POSITION_BOTTOM_RIGHT)
                    .mapNotNull { insets?.getRoundedCorner(it)?.radius }
                    .maxOrNull()?.toFloat() ?: 0f
            } else 0f
            true
        }
        observer.addOnPreDrawListener(listener)
        onDispose { if (observer.isAlive) observer.removeOnPreDrawListener(listener) }
    }
    return radius
}
