package dev.better.sheet.sample

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
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

/** No paused Compose clock, forced layout, or captureToImage: inspect the actual window pixels. */
class TrayRenderingTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun realClockSurfaceSurvivesRepeatedMorphsAndNavigationHasNoFooterShadow() {
        lateinit var state: SheetState
        lateinit var scope: CoroutineScope
        val initialized = java.util.concurrent.atomic.AtomicBoolean(false)
        var presentation by mutableStateOf<SheetPresentation>(SheetPresentation.Tray())
        var surfaceBottom = 0f
        var footerBottom = 0f
        var footerCenter = 0f
        ActivityScenario.launch(MainActivity::class.java).use { activity ->
            activity.onActivity { it.setContent {
                state = rememberSheetState("medium")
                scope = rememberCoroutineScope()
                SideEffect { initialized.set(true) }
                Box(Modifier.fillMaxSize().background(Color.Green))
                BetterModalSheet(state,
                    listOf(SheetSnapPoint.fraction("medium", .6f), SheetSnapPoint.expanded()),
                    presentation = presentation,
                    containerColor = Color.Cyan,
                    surfaceModifier = Modifier.onGloballyPositioned { surfaceBottom = it.boundsInWindow().bottom },
                    bodyMode = SheetBodyMode.Fill,
                    header = { Box(Modifier.fillMaxWidth().height(48.dp).background(Color.Magenta)) },
                    footer = {
                        Surface(shadowElevation = 12.dp) {
                            Box(Modifier.fillMaxWidth().height(64.dp).onGloballyPositioned { coordinates ->
                                val bounds = coordinates.boundsInWindow()
                                footerBottom = bounds.bottom
                                footerCenter = bounds.center.x
                            })
                        }
                    }) { Spacer(Modifier.fillMaxSize()) }
            } }
            fun awaitSettled(target: String) {
                val deadline = SystemClock.uptimeMillis() + 15000
                while (true) {
                    var ready = false
                    instrumentation.runOnMainSync {
                        ready = initialized.get() && state.currentSnapPoint == target && !state.isAnimationRunning
                    }
                    if (ready) break
                    check(SystemClock.uptimeMillis() < deadline) { "Tray never settled at $target" }
                    SystemClock.sleep(30)
                }
                // Let RenderThread display it; do not force another layout to repair a missing layer.
                SystemClock.sleep(100)
            }
            fun screenshot(): Bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            awaitSettled("medium")
            for (style in listOf(SheetPresentation.Tray(), SheetPresentation.Standard)) {
                instrumentation.runOnMainSync { presentation = style }
                awaitSettled("medium")
                repeat(3) {
                    for (target in listOf("expanded", "medium")) {
                        instrumentation.runOnMainSync { scope.launch { state.snapTo(target) } }
                        awaitSettled(target)
                        repeat(3) {
                            val shot = screenshot()
                            try {
                                var magenta = 0
                                for (y in 0 until shot.height step 4) for (x in 0 until shot.width step 4) {
                                    val pixel = shot.getPixel(x, y)
                                    if (android.graphics.Color.red(pixel) > 240 && android.graphics.Color.blue(pixel) > 240 &&
                                        android.graphics.Color.green(pixel) < 15) magenta++
                                }
                                assertTrue("Visible tray header disappeared at $target", magenta > 100)
                                if (target == "medium" && style is SheetPresentation.Tray) {
                                    val y = surfaceBottom.toInt() + 2
                                    if (y < shot.height) {
                                        // Uniform host + scrim: pixels below the tray should match
                                        // untouched background on the same scanline, not a gray shelf.
                                        val below = shot.getPixel(footerCenter.toInt(), y)
                                        val outside = shot.getPixel(2, y)
                                        assertEquals("Tray shadow formed a lip below the bottom edge",
                                            android.graphics.Color.green(outside).toFloat(), android.graphics.Color.green(below).toFloat(), 2f)
                                    }
                                }
                                if (target == "expanded" || style == SheetPresentation.Standard) {
                                    val x = footerCenter.toInt().coerceIn(0, shot.width - 1)
                                    val y = footerBottom.toInt() + 2
                                    if (y < shot.height - 2) {
                                        val pixel = shot.getPixel(x, y)
                                        assertTrue("Footer shadow leaked into navigation area",
                                            android.graphics.Color.red(pixel) < 15 &&
                                            android.graphics.Color.green(pixel) > 240 && android.graphics.Color.blue(pixel) > 240)
                                    }
                                }
                            } finally { shot.recycle() }
                            SystemClock.sleep(50)
                        }
                    }
                }
            }
        }
    }
}

