package dev.better.sheet.sample

import androidx.activity.compose.setContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import dev.better.sheet.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AnimationCustomizationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun fadeKeepsHeightAndWaitsForExitAndCanReverse() {
        lateinit var state: SheetState
        lateinit var scope: CoroutineScope
        var dismissed = 0
        var mounts = 0
        var disposals = 0
        compose.runOnUiThread { compose.activity.setContent {
            state = rememberSheetState("half", initiallyVisible = false)
            scope = rememberCoroutineScope()
            Box(Modifier.fillMaxSize().background(Color.White).testTag("fade-host")) {
                BetterSheet(state, listOf(SheetSnapPoint.fraction("half", .5f), SheetSnapPoint.expanded()),
                    containerColor = Color(0xFF0033FF),
                    enterTransition = fadeIn(tween(600)), exitTransition = fadeOut(tween(600)),
                    snapAnimationSpec = tween(1000), onDismissed = { dismissed++ },
                    header = { Spacer(Modifier.fillMaxWidth().height(48.dp).testTag("fade-header")) },
                    bodyMode = SheetBodyMode.Fill) {
                    DisposableEffect(Unit) { mounts++; onDispose { disposals++ } }
                    Spacer(Modifier.fillMaxSize())
                }
            }
        } }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.runOnUiThread { scope.launch { state.show() } }
        // Let Android measure the entrance between clock frames.
        repeat(10) {
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
        }
        val early = compose.onNodeWithTag("fade-header").getUnclippedBoundsInRoot().top
        assertTrue(state.isAnimationRunning)
        fun sheetRed(): Float {
            // UiAutomation works on API 24 too; Compose captureToImage requires API 26.
            val drawn = java.util.concurrent.CountDownLatch(1)
            compose.runOnUiThread {
                android.view.Choreographer.getInstance().postFrameCallback {
                    android.view.Choreographer.getInstance().postFrameCallback { drawn.countDown() }
                }
            }
            check(drawn.await(5, java.util.concurrent.TimeUnit.SECONDS))
            val bitmap = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .uiAutomation.takeScreenshot() ?: error("Screenshot failed")
            val red = android.graphics.Color.red(bitmap.getPixel(bitmap.width / 2, bitmap.height * 3 / 4)) / 255f
            bitmap.recycle()
            return red
        }
        val earlyRed = sheetRed()
        compose.mainClock.advanceTimeBy(800)
        compose.waitForIdle()
        val resting = compose.onNodeWithTag("fade-header").getUnclippedBoundsInRoot().top
        assertEquals("A fade must not slide or grow", resting.value, early.value, 1f)
        assertTrue("The sheet actually fades from the white background into blue", earlyRed > sheetRed() + .1f)
        compose.runOnUiThread { scope.launch { state.snapTo("expanded") } }
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        val halfway = compose.onNodeWithTag("fade-header").getUnclippedBoundsInRoot().top
        assertTrue(halfway < resting)
        assertTrue(state.isAnimationRunning)
        compose.mainClock.advanceTimeBy(1200)
        compose.waitForIdle()
        assertEquals("expanded", state.currentSnapPoint)
        compose.runOnUiThread { scope.launch { state.hide() } }
        compose.mainClock.advanceTimeBy(240)
        compose.waitForIdle()
        assertEquals(0, dismissed)
        assertTrue(state.isVisible)
        compose.runOnUiThread { scope.launch { state.show() } }
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
        assertEquals("Reversing an exit must not dismiss", 0, dismissed)
        assertTrue(state.isVisible)
        compose.runOnUiThread { scope.launch { state.hide() } }
        compose.mainClock.advanceTimeBy(240)
        compose.waitForIdle()
        assertEquals(0, dismissed)
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
        assertEquals(1, dismissed)
        assertFalse(state.isVisible)
        compose.runOnUiThread { scope.launch { state.show() } }
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
        assertEquals(1, mounts)
        assertEquals(0, disposals)
        compose.mainClock.autoAdvance = true
    }

    @Test fun composableSpecChangesApplyToExistingStateAndJumpIsImmediate() {
        lateinit var state: SheetState
        lateinit var scope: CoroutineScope
        var duration by mutableIntStateOf(1000)
        compose.runOnUiThread { compose.activity.setContent {
            state = rememberSheetState("half")
            scope = rememberCoroutineScope()
            BetterSheet(state, listOf(SheetSnapPoint.fraction("half", .5f), SheetSnapPoint.expanded()),
                enterTransition = EnterTransition.None, exitTransition = ExitTransition.None,
                snapAnimationSpec = tween(duration)) { Spacer(Modifier.height(120.dp)) }
        } }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.runOnUiThread { scope.launch { state.snapTo("expanded") } }
        compose.mainClock.advanceTimeBy(240)
        compose.waitForIdle()
        assertTrue(state.progress > .5f && state.progress < 1f)
        compose.runOnUiThread { scope.launch { state.jumpTo("half") } }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        assertEquals(.5f, state.progress, .001f)
        assertFalse(state.isAnimationRunning)
        compose.runOnUiThread { duration = 100 }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.runOnUiThread { scope.launch { state.snapTo("expanded") } }
        compose.mainClock.advanceTimeBy(240)
        compose.waitForIdle()
        assertEquals("expanded", state.currentSnapPoint)
        assertFalse(state.isAnimationRunning)
        compose.mainClock.autoAdvance = true
    }
}
