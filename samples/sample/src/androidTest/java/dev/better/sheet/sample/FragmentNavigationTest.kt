package dev.better.sheet.sample

import android.app.Dialog
import android.graphics.Color
import android.os.Build
import android.view.WindowManager
import android.view.inspector.WindowInspector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import dev.better.sheet.configureForBetterSheet
import dev.better.sheet.sample.navigation.*
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FragmentNavigationTest {
    class EntryProbe : ViewModel()
    @get:Rule val compose = createAndroidComposeRule<FragmentNavigationActivity>()

    private fun details() {
        compose.onNodeWithTag("customers").performClick()
        compose.onNodeWithTag("customer").performClick()
        compose.onNodeWithTag("background-counter").performClick()
    }

    private fun open() {
        compose.onNodeWithTag("open-sheet").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("sheet-title").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }

    private fun awaitDestination(id: Int) {
        compose.waitUntil(10000) {
            var ready = false
            compose.runOnUiThread {
                ready = compose.activity.host.navController.currentDestination?.id == id &&
                    compose.activity.host.childFragmentManager.fragments.none { it is CustomerSheetFragment }
            }
            ready
        }
        compose.waitForIdle()
    }

    private fun activeSheet(): CustomerSheetFragment =
        compose.activity.host.childFragmentManager.fragments.filterIsInstance<CustomerSheetFragment>().last()

    @Test fun saveReturnsResultAndLeavesUnderlyingScreenInteractive() {
        details()
        open()
        compose.onNodeWithTag("sheet-customer").assertTextEquals("Alex Rivera")
        compose.onNodeWithTag("sheet-note").performTextInput("Call tomorrow")
        compose.onNodeWithTag("save-sheet").performClick()
        awaitDestination(R.id.customerDetails)
        compose.onNodeWithTag("result").assertTextContains("Call tomorrow")
        compose.onNodeWithTag("background-counter").assertTextContains("Log a call · 1")
        compose.onNodeWithTag("background-counter").performClick()
        compose.onNodeWithTag("background-counter").assertTextContains("Log a call · 2")
    }

    @Suppress("DEPRECATION")
    @Test fun sheetUsesOneFullWindowWithTransparentSystemBars() {
        details()
        var baseline = 0
        if (Build.VERSION.SDK_INT >= 29) compose.runOnUiThread { baseline = WindowInspector.getGlobalWindowViews().size }
        open()
        compose.runOnUiThread {
            val fragment = activeSheet()
            assertTrue(fragment.showsDialog)
            val window = requireNotNull(fragment.requireDialog().window)
            assertFalse(window.isFloating)
            assertEquals(compose.activity.window.decorView.height, window.decorView.height)
            assertEquals(compose.activity.window.decorView.width, window.decorView.width)
            assertEquals(Color.TRANSPARENT, window.statusBarColor)
            assertEquals(Color.TRANSPARENT, window.navigationBarColor)
            assertEquals(0, window.attributes.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            val host = WindowCompat.getInsetsController(compose.activity.window, compose.activity.window.decorView)
            val sheet = WindowCompat.getInsetsController(window, window.decorView)
            assertEquals(host.isAppearanceLightStatusBars, sheet.isAppearanceLightStatusBars)
            assertEquals(host.isAppearanceLightNavigationBars, sheet.isAppearanceLightNavigationBars)
            if (Build.VERSION.SDK_INT >= 29) {
                assertFalse(window.isStatusBarContrastEnforced)
                assertFalse(window.isNavigationBarContrastEnforced)
                assertEquals(baseline + 1, WindowInspector.getGlobalWindowViews().size)
            }
        }
        compose.onNodeWithTag("vm-close").performClick()
        awaitDestination(R.id.customerDetails)
    }

    @Test fun helperRejectsFloatingThemeWithActionableMessage() {
        compose.runOnUiThread {
            val dialog = Dialog(compose.activity, android.R.style.Theme_Material_Light_Dialog_NoActionBar)
            try {
                // Match onStart: the window decor must exist before its policy is queried.
                dialog.show()
                assertTrue(requireNotNull(dialog.window).isFloating)
                dialog.configureForBetterSheet(compose.activity.window)
                fail("A floating theme must be configured before window creation")
            } catch (expected: IllegalArgumentException) {
                assertTrue(expected.message.orEmpty().contains("BetterSheetDialogTheme"))
            } finally {
                dialog.dismiss()
            }
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
        awaitDestination(R.id.customerDetails)
        compose.waitUntil(10000) { cleared.get() }
        assertFalse(oldView.hasComposition)
        compose.onNodeWithTag("open-sheet").assertIsDisplayed()
        compose.onNodeWithTag("background-counter").performClick()
    }

    @Test fun backAndScrimDismissOnlyTheSheet() {
        details()
        open()
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        awaitDestination(R.id.customerDetails)
        open()
        compose.onNodeWithContentDescription("Dismiss Customer note").performTouchInput {
            click(Offset(centerX, 100f))
        }
        awaitDestination(R.id.customerDetails)
    }

    @Test fun dragDismissesAndReleasesTheDestination() {
        details()
        open()
        compose.onNodeWithTag("sheet-title").performTouchInput {
            swipeDown(startY = centerY, endY = centerY + 1200f, durationMillis = 350)
        }
        awaitDestination(R.id.customerDetails)
    }

    @Test fun stackedDialogsPopOneAtATime() {
        details()
        open()
        compose.onNodeWithTag("stack-sheet").performScrollTo().performClick()
        compose.waitUntil(10000) {
            var count = 0
            compose.runOnUiThread { count = compose.activity.host.childFragmentManager.fragments.filterIsInstance<CustomerSheetFragment>().size }
            count == 2
        }
        compose.waitForIdle()
        compose.onAllNodesWithTag("sheet-customer").onLast().assertTextEquals("Second sheet")
        compose.onAllNodesWithTag("pop-sheet").onLast().performScrollTo().performClick()
        compose.waitUntil(10000) {
            var count = 0
            compose.runOnUiThread { count = compose.activity.host.childFragmentManager.fragments.filterIsInstance<CustomerSheetFragment>().size }
            count == 1
        }
        compose.waitForIdle()
        compose.onNodeWithTag("sheet-customer").assertTextEquals("Alex Rivera")
        compose.onNodeWithTag("vm-close").performClick()
        awaitDestination(R.id.customerDetails)
    }

    @Test fun normalNavigationRemovesDialogDestination() {
        details()
        open()
        compose.onNodeWithTag("sheet-full-screen").performScrollTo().performClick()
        awaitDestination(R.id.navigationReceipt)
        compose.onNodeWithTag("receipt-back").performClick()
        compose.onNodeWithTag("open-sheet").assertIsDisplayed()
    }

    @Test fun recreationRestoresOneSheetWithItsDraftAndVm() {
        details()
        open()
        compose.onNodeWithTag("sheet-note").performTextInput("Keep this draft")
        lateinit var originalVm: CustomerSheetViewModel
        compose.runOnUiThread { originalVm = ViewModelProvider(activeSheet())[CustomerSheetViewModel::class.java] }
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithTag("sheet-note").assertTextContains("Keep this draft")
        compose.runOnUiThread {
            assertEquals(1, compose.activity.host.childFragmentManager.fragments.filterIsInstance<CustomerSheetFragment>().size)
            assertSame(originalVm, ViewModelProvider(activeSheet())[CustomerSheetViewModel::class.java])
        }
        compose.onNodeWithTag("vm-close").performClick()
        awaitDestination(R.id.customerDetails)
    }

    @Test fun immediateNavigateAndPopDoesNotLeaveAnOrphan() {
        details()
        val entryCleared = AtomicBoolean(false)
        compose.runOnUiThread {
            val nav = compose.activity.host.navController
            nav.navigate(R.id.openCustomerSheet)
            ViewModelProvider(requireNotNull(nav.currentBackStackEntry))[EntryProbe::class.java]
                .addCloseable("test", AutoCloseable { entryCleared.set(true) })
            nav.popBackStack()
        }
        awaitDestination(R.id.customerDetails)
        compose.waitUntil(10000) { entryCleared.get() }
        open()
        compose.onNodeWithTag("vm-close").performClick()
        awaitDestination(R.id.customerDetails)
    }

    @Test fun popUpToRemovesStackedDialogsAndUnderlyingScreens() {
        details()
        open()
        compose.onNodeWithTag("stack-sheet").performScrollTo().performClick()
        compose.waitForIdle()
        compose.runOnUiThread {
            compose.activity.host.navController.navigate(R.id.navigationReceipt, null,
                NavOptions.Builder().setPopUpTo(R.id.navigationHome, false).build())
        }
        awaitDestination(R.id.navigationReceipt)
        compose.onNodeWithTag("receipt-back").performClick()
        compose.onNodeWithTag("customers").assertIsDisplayed()
    }
}
