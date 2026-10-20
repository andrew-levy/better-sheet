package dev.better.sheet.findmy

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test

/** Exercises the real tab bar and shared sheet, including devices without content blur. */
class FindMyNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<FindMyActivity>()

    private fun go(point: String, sheet: String = "people") {
        compose.runOnUiThread { DemoControls.go(sheet, point) }
        compose.waitForIdle()
    }

    @Test fun collapsedSheetKeepsTabsAndReopensSelectedContent() {
        go("collapsed")
        compose.onNode(hasStateDescription("collapsed")).assertExists()
        compose.onNodeWithTag("tab-people").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithTag("tab-chat").assertIsDisplayed()
        compose.onNodeWithTag("tab-chat").performClick()
        compose.onNodeWithTag("composer").assertIsDisplayed()
        compose.onNodeWithTag("tab-chat").assertIsSelected()
        go("collapsed", "chat")
        compose.onNode(hasStateDescription("collapsed")).assertExists()
        compose.onNodeWithTag("tab-people").performClick()
        compose.onNodeWithTag("person-kim").assertIsDisplayed()
    }

    @Test fun tabsKeepExpansionAndDraftAndBackCollapsesInsteadOfHiding() {
        go("expanded")
        compose.onNodeWithTag("tab-chat").performClick()
        compose.onNode(hasStateDescription("expanded")).assertExists()
        compose.onNodeWithTag("composer").performTextInput("Keep this draft")
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressBack() // Dismiss the keyboard before changing tabs.
        compose.waitForIdle()
        compose.onNodeWithTag("tab-people").performClick()
        compose.onNodeWithTag("tab-chat").performClick()
        compose.onNodeWithTag("composer").assertTextContains("Keep this draft")
        device.pressBack()
        compose.waitForIdle()
        compose.onNode(hasStateDescription("collapsed")).assertExists()
        compose.onNodeWithTag("main-tabs").assertIsDisplayed()
    }

    @Test fun draggingDownStopsAtTabsAndTabsStayAtBottomAcrossSelection() {
        go("collapsed")
        val before = compose.onNodeWithTag("main-tabs").getUnclippedBoundsInRoot()
        compose.onNodeWithTag("sheet-header").performTouchInput { swipeDown() }
        compose.onNode(hasStateDescription("collapsed")).assertExists()
        compose.onNodeWithTag("tab-chat").performClick()
        val after = compose.onNodeWithTag("main-tabs").getUnclippedBoundsInRoot()
        check(kotlin.math.abs(before.bottom.value - after.bottom.value) < 2f) {
            "The tab bar moved when changing tabs: $before -> $after"
        }
        compose.onNodeWithTag("tab-chat").assertIsDisplayed()
    }

    @Test fun nearbyPeopleUsesAnEdgeAttachedSheetWithThreeSnapPoints() {
        compose.onNodeWithContentDescription("Nearby people").performClick()
        for (point in listOf("peek", "expanded", "medium")) {
            compose.onNodeWithTag("nearby-snap-$point").performClick()
            compose.onNode(hasStateDescription(point) and hasAnyAncestor(hasTestTag("nearby-surface"))).assertExists()
            val bounds = compose.onNodeWithTag("nearby-surface").getUnclippedBoundsInRoot()
            check(kotlin.math.abs(bounds.left.value) < 1f) { "The standalone sheet has a floating side gap: $bounds" }
        }
        compose.onNodeWithTag("close-nearby").performClick()
        compose.onNodeWithTag("main-tabs").assertIsDisplayed()
    }

    @Test fun threeSheetsProgressivelyRecedeAndRevealAQuarterOfThePreviousSheet() {
        go("medium")
        compose.onNodeWithTag("person-kim").performClick()
        go("stack", "person")
        // Layout size is unchanged by graphicsLayer; semantic bounds include its scale.
        val baseSolo = compose.onNodeWithTag("stack-profile-surface").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("stack-open-details").performClick()
        val baseBehindSecond = compose.onNodeWithTag("stack-profile-surface").fetchSemanticsNode().boundsInRoot
        val secondSolo = compose.onNodeWithTag("stack-contact-surface").fetchSemanticsNode().boundsInRoot
        check(baseBehindSecond.width < baseSolo.width) { "The profile did not recede under contact info" }
        fun assertQuarter(back: androidx.compose.ui.geometry.Rect, front: androidx.compose.ui.geometry.Rect) {
            val visibleFraction = (front.top - back.top) / back.height
            check(visibleFraction in 0.20f..0.30f) { "Expected a quarter of the previous sheet to remain visible: $visibleFraction" }
        }
        assertQuarter(baseBehindSecond, secondSolo)
        compose.onNodeWithTag("stack-open-confirm").performClick()
        val baseBehindThird = compose.onNodeWithTag("stack-profile-surface").fetchSemanticsNode().boundsInRoot
        val secondBehindThird = compose.onNodeWithTag("stack-contact-surface").fetchSemanticsNode().boundsInRoot
        val third = compose.onNodeWithTag("stack-notify-surface").fetchSemanticsNode().boundsInRoot
        check(baseBehindThird.width < baseBehindSecond.width) { "The back sheet stopped receding at depth two" }
        check(secondBehindThird.width < secondSolo.width) { "The middle sheet did not recede" }
        assertQuarter(baseBehindThird, secondBehindThird)
        assertQuarter(secondBehindThird, third)
        compose.onNodeWithTag("stack-send-review").performClick()
        val restored = compose.onNodeWithTag("stack-contact-surface").fetchSemanticsNode().boundsInRoot
        check(kotlin.math.abs(restored.width - secondSolo.width) < 2f) { "The contact sheet did not restore its scale" }
    }

}
