package dev.better.sheet.sample.navigation

import android.os.Bundle
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.*
import dev.better.sheet.BetterModalSheet
import dev.better.sheet.SheetSnapPoint
import dev.better.sheet.SheetState

/** New path: <sheet> + an attached ComposeView; only BetterModalSheet creates a dialog. */
abstract class ModalSheetFragment : ComposeContentFragment() {
    private var navigationCloseRequested by mutableStateOf(false)
    private var finished = false
    private val navigator get() = (requireParentFragment() as SheetNavHostFragment).sheets

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowsDialog(false)
        navigationCloseRequested = savedInstanceState?.getBoolean("navigation-close") ?: false
        finished = savedInstanceState?.getBoolean("sheet-finished") ?: false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("navigation-close", navigationCloseRequested)
        outState.putBoolean("sheet-finished", finished)
    }

    override fun onResume() {
        super.onResume()
        if (finished) navigator.onSheetHidden(this)
    }

    internal fun requestClose() { navigationCloseRequested = true }

    private fun onHidden() {
        finished = true
        navigator.onSheetHidden(this)
    }

    @Composable
    protected fun ModalContent(
        state: SheetState,
        snapPoints: List<SheetSnapPoint>,
        closeRequested: Boolean,
        title: String,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        val close = navigationCloseRequested || closeRequested
        // Capture before layout updates the initial anchors; only restored hidden state should close.
        val restoredHidden = remember(state) { state.currentSnapPoint == null && state.targetSnapPoint == null }
        LaunchedEffect(Unit) {
            if (finished || restoredHidden) {
                onHidden()
            }
        }
        LaunchedEffect(close) {
            if (close) {
                state.hide()
                // hide() can finish before the modal has measured and installed its callback.
                onHidden()
            }
        }
        BetterModalSheet(
            state = state,
            snapPoints = snapPoints,
            title = title,
            onDismissed = ::onHidden,
            content = content,
        )
    }
}
