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
}
