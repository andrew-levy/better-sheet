package dev.better.sheet.findmy

import android.graphics.Bitmap
import android.view.Choreographer
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

/**
 * Captures demo clips frame by frame; excluded from normal CI.
 *
 * Emulators without a host GPU composite only a few frames per second, so screenrecord misses
 * most of a sheet animation. Instead, each step advances the paused Compose clock by one output
 * frame, waits for that frame to reach the display, and screenshots the whole screen (including
 * dialog and IME windows). scripts/record-demos.py assembles the frames at [FRAME_MILLIS] per
 * frame, so Compose motion plays at its real speed. The IME animates on the device clock and may
 * appear abruptly.
 */
class DemoRecordingTest {
    @get:Rule val compose = createAndroidComposeRule<FindMyActivity>()
    @get:Rule val testName = TestName()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private lateinit var frames: File
    private var frameIndex = 0

    @Before fun prepare() {
        // MapLibre uses the device clock. Wait for real tiles before pausing Compose motion.
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodes(hasStateDescription("Map ready")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.mainClock.autoAdvance = false
        frames = File(instrumentation.targetContext.getExternalFilesDir(null), "demo-frames/${testName.methodName}")
        frames.deleteRecursively()
        check(frames.mkdirs()) { "Cannot create $frames" }
    }

    private fun awaitDisplayedFrame() {
        val drawn = CountDownLatch(1)
        compose.runOnUiThread {
            Choreographer.getInstance().postFrameCallback {
                Choreographer.getInstance().postFrameCallback { drawn.countDown() }
            }
        }
        drawn.await(5, TimeUnit.SECONDS)
    }

    private fun capture() {
        compose.waitForIdle()
        awaitDisplayedFrame()
        val screenshot = instrumentation.uiAutomation.takeScreenshot() ?: error("Screenshot failed")
        File(frames, "%05d.jpg".format(frameIndex++)).outputStream().use {
            screenshot.compress(Bitmap.CompressFormat.JPEG, 92, it)
        }
        screenshot.recycle()
    }

    /** Records [millis] of Compose time; each captured frame is [FRAME_MILLIS] long. */
    private fun pause(millis: Long = 1000) {
        repeat((millis / FRAME_MILLIS).toInt()) {
            repeat((FRAME_MILLIS / CLOCK_FRAME_MILLIS).toInt()) { compose.mainClock.advanceTimeByFrame() }
            capture()
        }
    }

    private fun go(point: String, sheet: String = "people") {
        compose.runOnUiThread { DemoControls.go(sheet, point) }
        pause(1100)
    }

    @Test fun snapPoints() {
        pause()
        go("peek"); go("expanded"); go("medium"); go("collapsed")
        compose.onNodeWithTag("tab-people").performClick(); pause(1200)
    }

    @Test fun tabs() {
        pause()
        compose.onNodeWithTag("tab-chat").performClick(); pause(1200)
        compose.onNodeWithTag("tab-people").performClick(); pause(1200)
        go("collapsed")
        compose.onNodeWithTag("tab-chat").performClick(); pause(1200)
        go("collapsed", "chat")
        compose.onNodeWithTag("tab-people").performClick(); pause(1200)
    }

    @Test fun tray() {
        pause()
        go("collapsed")
        val header = compose.onNodeWithTag("sheet-header")
        header.performTouchInput { down(center) }
        repeat(34) {
            header.performTouchInput { moveBy(androidx.compose.ui.geometry.Offset(0f, -12f), delayMillis = FRAME_MILLIS) }
            pause(FRAME_MILLIS)
        }
        repeat(18) {
            header.performTouchInput { moveBy(androidx.compose.ui.geometry.Offset(0f, 12f), delayMillis = FRAME_MILLIS) }
            pause(FRAME_MILLIS)
        }
        header.performTouchInput { up() }; pause()
        go("expanded"); go("collapsed")
    }

    @Test fun chat() {
        compose.onNodeWithTag("tab-chat").performClick(); pause(1200)
        compose.onNodeWithTag("composer").performClick(); pause(1200)
        compose.onNodeWithTag("composer").performTextInput("See you at Brightskies!"); pause(700)
        compose.onNodeWithTag("send").performClick(); pause(900)
        UiDevice.getInstance(instrumentation).pressBack(); pause(1200)
        go("collapsed", "chat")
    }

    @Test fun contentFit() {
        pause()
        compose.onNodeWithTag("person-kim").performClick(); pause(1400)
        compose.onNodeWithTag("primary-action").performClick(); pause(1400)
        compose.onNodeWithTag("primary-action").performClick(); pause(1400)
        compose.onNodeWithTag("stack-close-base").performClick(); pause(1400)
    }

    @Test fun stacked() {
        pause()
        compose.onNodeWithTag("person-kim").performClick(); pause(1400)
        compose.onNodeWithTag("stack-open-details").performClick(); pause(1400)
        compose.onNodeWithTag("stack-open-confirm").performClick(); pause(1400)
        compose.onNodeWithTag("stack-send-review").performClick(); pause(1200)
        compose.onNodeWithTag("stack-close-details").performClick(); pause(1200)
        compose.onNodeWithTag("stack-close-base").performClick(); pause(1200)
    }

    @Test fun scrollHandoff() {
        pause()
        compose.onNodeWithTag("people-list").performTouchInput { swipeUp() }; pause()
        compose.onNodeWithTag("people-list").performTouchInput { swipeDown() }; pause()
        val header = compose.onNodeWithTag("sheet-header")
        header.performTouchInput { down(center) }
        repeat(30) {
            header.performTouchInput { moveBy(androidx.compose.ui.geometry.Offset(0f, -10f), delayMillis = FRAME_MILLIS) }
            pause(FRAME_MILLIS)
        }
        header.performTouchInput { up() }; pause()
        go("medium"); go("collapsed")
    }

    companion object {
        private const val CLOCK_FRAME_MILLIS = 16L
        /** Output frame duration; record-demos.py uses the same value. */
        const val FRAME_MILLIS = 32L
    }
}

