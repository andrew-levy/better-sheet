package dev.better.sheet.sample

import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import dev.better.sheet.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.Test

/** Real Choreographer frames and window pixels, without a paused Compose test clock. */
class EntranceRenderingTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun inlineFirstOpenAndReopenSlideFromBottom() = verifyEntrance(false, false)
    @Test fun modalFirstOpenAndReopenSlideFromBottom() = verifyEntrance(true, false)
    @Test fun initiallyVisibleInlineSlidesFromBottom() = verifyEntrance(false, true)
    @Test fun initiallyVisibleModalSlidesFromBottom() = verifyEntrance(true, true)

    private fun verifyEntrance(modal: Boolean, initiallyVisible: Boolean) {
        lateinit var state: SheetState
        lateinit var scope: CoroutineScope
        var hostBottom = 0f
        var height = 0f
        val initialized = java.util.concurrent.atomic.AtomicBoolean(false)
        fun awaitCondition(message: String, condition: () -> Boolean) {
            val deadline = SystemClock.uptimeMillis() + 15000
            while (true) {
                var ready = false
                instrumentation.runOnMainSync { ready = initialized.get() && condition() }
                if (ready) return
                check(SystemClock.uptimeMillis() < deadline) { message }
                SystemClock.sleep(16)
            }
        }
        fun renderedTop(): Float? {
            val shot = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            try {
                for (y in 0 until shot.height) {
                    val pixel = shot.getPixel(shot.width / 2, y)
                    val red = android.graphics.Color.red(pixel)
                    val green = android.graphics.Color.green(pixel)
                    val blue = android.graphics.Color.blue(pixel)
                    // Include the same blue under legacy navigation-bar contrast tint.
                    if (red < 15 && blue > 100 && green in (blue * .16f).toInt()..(blue * .25f).toInt()) {
                        return y.toFloat()
                    }
                }
                return null
            } finally { shot.recycle() }
        }
        ActivityScenario.launch(MainActivity::class.java).use { activity ->
            activity.onActivity { it.setContent {
                state = rememberSheetState("half", initiallyVisible = initiallyVisible)
                scope = rememberCoroutineScope()
                SideEffect { initialized.set(true) }
                Box(Modifier.fillMaxSize().background(Color.White)
                    .onGloballyPositioned { hostBottom = it.boundsInWindow().bottom }) {
                    val points = listOf(SheetSnapPoint.fraction("half", .5f))
                    val enter = slideInVertically(tween(2000, easing = LinearEasing)) { it }
                    val surface = Modifier.onGloballyPositioned { height = it.size.height.toFloat() }
                    if (modal) {
                        BetterModalSheet(state, points, enterTransition = enter, exitTransition = ExitTransition.None,
                            containerColor = Color(0xFF0033FF), scrimColor = Color.Transparent,
                            surfaceModifier = surface, bodyMode = SheetBodyMode.Fill) { Spacer(Modifier.fillMaxSize()) }
                    } else {
                        BetterSheet(state, points, enterTransition = enter, exitTransition = ExitTransition.None,
                            containerColor = Color(0xFF0033FF), scrimColor = Color.Transparent,
                            surfaceModifier = surface, bodyMode = SheetBodyMode.Fill) { Spacer(Modifier.fillMaxSize()) }
                    }
                }
            } }
            awaitCondition("Sheet never initialized") { height > 0f || !initiallyVisible }
            repeat(2) { opening ->
                if (!initiallyVisible || opening > 0) instrumentation.runOnMainSync { scope.launch { state.show() } }
                val deadline = SystemClock.uptimeMillis() + 15000
                var firstTop: Float? = null
                while (firstTop == null) {
                    firstTop = renderedTop()
                    check(SystemClock.uptimeMillis() < deadline) { "No rendered sheet pixels for opening $opening" }
                    if (firstTop == null) SystemClock.sleep(16)
                }
                var bottom = 0f
                var fullHeight = 0f
                instrumentation.runOnMainSync { bottom = hostBottom; fullHeight = height }
                assertTrue("Opening $opening appeared too high: top=$firstTop, bottom=$bottom, height=$fullHeight",
                    fullHeight > 0f && firstTop!! >= bottom - fullHeight * .25f)
                awaitCondition("Entrance never settled") { state.currentSnapPoint == "half" && !state.isAnimationRunning }
                SystemClock.sleep(100)
                val finalTop = checkNotNull(renderedTop()) { "Settled sheet did not render" }
                assertTrue("Opening $opening skipped most of its travel: first=$firstTop, final=$finalTop, height=$fullHeight",
                    firstTop!! - finalTop > fullHeight * .65f)
                instrumentation.runOnMainSync { scope.launch { state.hide() } }
                awaitCondition("Exit never settled") { !state.isVisible && !state.isAnimationRunning }
                // Wait for RenderThread before capturing the next entrance.
                val hiddenDeadline = SystemClock.uptimeMillis() + 5000
                while (renderedTop() != null) {
                    check(SystemClock.uptimeMillis() < hiddenDeadline) { "Hidden sheet still renders" }
                    SystemClock.sleep(16)
                }
            }
        }
    }
}
