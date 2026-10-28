package dev.better.sheet.sample

import android.graphics.Bitmap
import android.view.Choreographer
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import dev.better.sheet.sample.navigation.NavigationComparisonActivity
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Rule
import org.junit.Test

/** Whole-device frames include system bars. Compose motion advances at 32 ms per output frame.
 * Window-manager animations still use wall time; do not use these clips to benchmark their timing.
 */
class NavigationRecordingTest {
    @get:Rule val compose = createAndroidComposeRule<NavigationComparisonActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun compareOpening() {
        val approach = requireNotNull(InstrumentationRegistry.getArguments().getString("approach"))
        require(approach in listOf("legacy", "modal", "adapter"))
        val frames = File(instrumentation.targetContext.getExternalFilesDir(null), "navigation-frames/$approach")
        frames.deleteRecursively()
        check(frames.mkdirs())
        compose.onNodeWithTag("customers").performClick()
        compose.onNodeWithTag("customer").performClick()
        compose.onNodeWithTag("open-$approach").performScrollTo()
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        var index = 0
        fun capture(millis: Int) {
            repeat(millis / 32) {
                repeat(2) { compose.mainClock.advanceTimeByFrame() }
                compose.waitForIdle()
                val displayed = CountDownLatch(1)
                compose.runOnUiThread {
                    Choreographer.getInstance().postFrameCallback {
                        Choreographer.getInstance().postFrameCallback { displayed.countDown() }
                    }
                }
                check(displayed.await(5, TimeUnit.SECONDS)) { "Display did not draw" }
                val image = instrumentation.uiAutomation.takeScreenshot() ?: error("Screenshot failed")
                File(frames, "%05d.jpg".format(index++)).outputStream().use {
                    image.compress(Bitmap.CompressFormat.JPEG, 95, it)
                }
                image.recycle()
            }
        }
        capture(640)
        compose.onNodeWithTag("open-$approach").performClick()
        capture(2560)
        compose.onNodeWithTag("sheet-title").assertIsDisplayed()
        compose.onNodeWithTag("vm-close").performClick()
        capture(1600)
        compose.onNodeWithTag("open-$approach").assertIsDisplayed()
    }
}
