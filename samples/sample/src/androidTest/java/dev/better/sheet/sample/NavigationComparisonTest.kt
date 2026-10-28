package dev.better.sheet.sample

import android.os.Build
import android.view.inspector.WindowInspector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.navigation.NavOptions
import dev.better.sheet.sample.navigation.*
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class NavigationComparisonTest {
    class EntryProbe : ViewModel()

    @get:Rule val compose = createAndroidComposeRule<NavigationComparisonActivity>()

    private fun details() {
        compose.onNodeWithTag("customers").performClick()
        compose.onNodeWithTag("customer").performClick()
        compose.onNodeWithTag("background-counter").performClick()
    }

    private fun open(modern: Boolean = true, adapter: Boolean = false) {
        compose.onNodeWithTag(if (adapter) "open-adapter" else if (modern) "open-modal" else "open-legacy").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("sheet-title").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }

    private fun awaitDestination(id: Int) {
        compose.waitUntil(10000) {
            var ready = false
            compose.runOnUiThread {
                ready = compose.activity.host.navController.currentDestination?.id == id &&
                    compose.activity.host.childFragmentManager.fragments.none { it is ComposeContentFragment }
            }
            ready
        }
        compose.waitForIdle()
    }

    private fun activeSheet(): ComposeContentFragment =
        compose.activity.host.childFragmentManager.fragments.filterIsInstance<ComposeContentFragment>().last()

    @Test fun legacyAndModalReturnResultsAndLeaveUnderlyingScreenInteractive() {
        details()
        for (approach in 1..3) {
            val modern = approach == 2
            open(modern, adapter = approach == 3)
            compose.onNodeWithTag("sheet-customer").assertTextEquals("Alex Rivera")
            compose.onNodeWithTag("sheet-note").performTextInput(if (modern) "Modal note" else "Legacy note")
            compose.onNodeWithTag("save-sheet").performClick()
            awaitDestination(R.id.comparisonCustomer)
            compose.onNodeWithTag("result").assertTextContains(if (modern) "Modal note" else "Legacy note", substring = true)
            compose.onNodeWithTag("background-counter").assertTextContains("Log a call · 1")
        }
        compose.onNodeWithTag("background-counter").performClick()
        compose.onNodeWithTag("background-counter").assertTextContains("Log a call · 2")
    }

    @Test fun eachApproachCreatesOnlyOneDialogWindow() {
        details()
        var baseline = 0
        if (Build.VERSION.SDK_INT >= 29) compose.runOnUiThread { baseline = WindowInspector.getGlobalWindowViews().size }
        for (approach in 1..3) {
            val modern = approach == 2
            open(modern, adapter = approach == 3)
            compose.runOnUiThread {
                val fragment = activeSheet() as DialogFragment
                assertEquals(!modern, fragment.showsDialog)
                assertEquals(!modern, fragment.dialog != null)
                if (Build.VERSION.SDK_INT >= 29) assertEquals(baseline + 1, WindowInspector.getGlobalWindowViews().size)
            }
            compose.onNodeWithTag("vm-close").performClick()
            awaitDestination(R.id.comparisonCustomer)
        }
    }

    @Test fun programmaticPopDisposesCompositionAndClearsVmWithoutDoublePop() {
        details()
        open()
        lateinit var oldView: ComposeView
        val cleared = AtomicBoolean(false)
        compose.runOnUiThread {
            val fragment = activeSheet()
            oldView = fragment.requireView() as ComposeView
            ViewModelProvider(fragment)[CustomerSheetViewModel::class.java]
                .addCloseable("test", AutoCloseable { cleared.set(true) })
        }
        compose.onNodeWithTag("pop-sheet").performClick()
        awaitDestination(R.id.comparisonCustomer)
        compose.waitUntil(10000) { cleared.get() }
        assertFalse(oldView.hasComposition)
        // If onDismissed popped again, this would have returned to Customers.
        compose.onNodeWithTag("open-modal").assertIsDisplayed()
        compose.onNodeWithTag("background-counter").performClick()
    }

    @Test fun backAndScrimDismissOnlyTheSheet() {
        details()
        open()
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        awaitDestination(R.id.comparisonCustomer)
        open()
        compose.onNodeWithContentDescription("Dismiss Modal customer sheet").performTouchInput {
            // Its semantics bounds fill the window; the center is covered by the sheet surface.
            click(Offset(centerX, 100f))
        }
        awaitDestination(R.id.comparisonCustomer)
    }

    @Test fun dragDismissesAndReleasesTheDestination() {
        details()
        open()
        compose.onNodeWithTag("sheet-title").performTouchInput { swipeDown(startY = centerY, endY = centerY + 1200f, durationMillis = 350) }
        awaitDestination(R.id.comparisonCustomer)
    }

    @Test fun stackedSheetsPopOneAtATime() {
        details()
        open()
        compose.onNodeWithTag("stack-sheet").performScrollTo().performClick()
        compose.waitUntil(10000) {
            var count = 0
            compose.runOnUiThread { count = compose.activity.host.childFragmentManager.fragments.filterIsInstance<ModalSheetFragment>().size }
            count == 2
        }
        compose.waitForIdle()
        compose.onAllNodesWithTag("sheet-customer").onLast().assertTextEquals("Second sheet")
        compose.onAllNodesWithTag("pop-sheet").onLast().performScrollTo().performClick()
        compose.waitUntil(10000) {
            var count = 0
            compose.runOnUiThread { count = compose.activity.host.childFragmentManager.fragments.filterIsInstance<ModalSheetFragment>().size }
            count == 1
        }
        compose.waitForIdle()
        compose.onNodeWithTag("sheet-customer").assertTextEquals("Alex Rivera")
        compose.onNodeWithTag("vm-close").performClick()
        awaitDestination(R.id.comparisonCustomer)
    }

    @Test fun normalNavigationRemovesFloatingDestination() {
        details()
        for (approach in 1..3) {
            val modern = approach == 2
            open(modern, adapter = approach == 3)
            compose.onNodeWithTag("sheet-full-screen").performScrollTo().performClick()
            awaitDestination(R.id.comparisonReceipt)
            compose.onNodeWithTag("receipt-back").performClick()
            compose.onNodeWithTag("open-modal").assertIsDisplayed()
        }
    }

    @Test fun rotationRestoresOneSheetWithItsDraftAndVm() {
        details()
        for (approach in 1..3) {
            val modern = approach == 2
            open(modern, adapter = approach == 3)
            compose.onNodeWithTag("sheet-note").performTextInput("Keep this draft")
            lateinit var originalVm: CustomerSheetViewModel
            compose.runOnUiThread { originalVm = ViewModelProvider(activeSheet())[CustomerSheetViewModel::class.java] }
            compose.activityRule.scenario.recreate()
            compose.waitForIdle()
            compose.onNodeWithTag("sheet-note").assertTextContains("Keep this draft")
            compose.runOnUiThread {
                assertEquals(1, compose.activity.host.childFragmentManager.fragments.filterIsInstance<ComposeContentFragment>().size)
                assertSame(originalVm, ViewModelProvider(activeSheet())[CustomerSheetViewModel::class.java])
            }
            compose.onNodeWithTag("vm-close").performClick()
            awaitDestination(R.id.comparisonCustomer)
        }
    }

    @Test fun immediateNavigateAndPopDoesNotLeaveAnOrphan() {
        details()
        val entryCleared = AtomicBoolean(false)
        compose.runOnUiThread {
            val nav = compose.activity.host.navController
            nav.navigate(R.id.openModalSheet)
            ViewModelProvider(requireNotNull(nav.currentBackStackEntry))[EntryProbe::class.java]
                .addCloseable("test", AutoCloseable { entryCleared.set(true) })
            nav.popBackStack()
        }
        awaitDestination(R.id.comparisonCustomer)
        compose.waitUntil(10000) { entryCleared.get() }
        open()
        compose.onNodeWithTag("vm-close").performClick()
        awaitDestination(R.id.comparisonCustomer)
    }

    @Test fun popUpToRemovesMultipleSheetsAndUnderlyingScreens() {
        details()
        open()
        compose.onNodeWithTag("stack-sheet").performScrollTo().performClick()
        compose.waitForIdle()
        compose.runOnUiThread {
            compose.activity.host.navController.navigate(R.id.comparisonReceipt, null,
                NavOptions.Builder().setPopUpTo(R.id.comparisonHome, false).build())
        }
        awaitDestination(R.id.comparisonReceipt)
        compose.onNodeWithTag("receipt-back").performClick()
        compose.onNodeWithTag("customers").assertIsDisplayed()
    }
}
