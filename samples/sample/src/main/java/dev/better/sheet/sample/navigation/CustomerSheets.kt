package dev.better.sheet.sample.navigation

import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.fragment.app.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import dev.better.sheet.*
import dev.better.sheet.sample.R

class CustomerSheetViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    val note = savedState.getStateFlow("note", "")
    val closeRequested = savedState.getStateFlow("close", false)
    val customer: String = savedState["customer"] ?: "Alex Rivera"
    fun edit(value: String) { savedState["note"] = value }
    fun close() { savedState["close"] = true }
}

/** Old path: <dialog> and the stock DialogFragmentNavigator own the modal window. */
class LegacyCustomerSheetFragment : ComposeContentFragment() {
    private val viewModel: CustomerSheetViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        requireDialog().window?.apply {
            setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            WindowCompat.setDecorFitsSystemWindows(this, false)
        }
    }

    @Composable override fun Content() {
        val state = rememberSheetState(initialSnapPoint = "medium")
        val close by viewModel.closeRequested.collectAsState()
        LaunchedEffect(close) { if (close) state.hide() }
        BetterSheet(
            state = state,
            snapPoints = customerSnapPoints,
            title = "Legacy customer sheet",
            onDismissed = { dismiss() },
        ) {
            EditorContent("Old · DialogFragment", "The Fragment owns the dialog; BetterSheet draws inside it.",
                viewModel, onSave = { saveResult(viewModel) },
                onPop = { findNavController().popBackStack() },
                onFullScreen = { findNavController().navigateSafe(R.id.openReceipt) })
        }
    }
}

class ModalCustomerSheetFragment : ModalSheetFragment() {
    private val viewModel: CustomerSheetViewModel by viewModels()

    @Composable override fun Content() {
        val closeRequested by viewModel.closeRequested.collectAsState()
        val state = rememberSheetState(initialSnapPoint = "medium")
        ModalContent(state, customerSnapPoints, closeRequested, title = "Modal customer sheet") {
            val depth = requireArguments().getInt("depth", 1)
            EditorContent("New · Modal sheet destination", "The Fragment has no dialog. BetterModalSheet owns the only window.",
                viewModel, onSave = { saveResult(viewModel) },
                onPop = { findNavController().popBackStack() },
                onFullScreen = { findNavController().navigateSafe(R.id.openReceipt) })
            if (depth < 2) {
                TextButton(onClick = {
                    findNavController().navigateSafe(R.id.openModalSheet,
                        bundleOf("customer" to "Second sheet", "depth" to depth + 1))
                }, modifier = Modifier.padding(horizontal = 24.dp).testTag("stack-sheet")) { Text("Open a second sheet") }
            }
        }
    }
}

private fun Fragment.saveResult(viewModel: CustomerSheetViewModel) {
    findNavController().previousBackStackEntry?.savedStateHandle
        ?.set("customer-result", viewModel.note.value.ifBlank { "Saved" })
    viewModel.close()
}

private val customerSnapPoints = listOf(SheetSnapPoint.fraction("medium", .8f), SheetSnapPoint.expanded())

@Composable
private fun ColumnScope.EditorContent(
    title: String,
    explanation: String,
    viewModel: CustomerSheetViewModel,
    onSave: () -> Unit,
    onPop: () -> Unit,
    onFullScreen: () -> Unit,
) {
    val note by viewModel.note.collectAsState()
    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("sheet-title"))
        Text(explanation)
        Text(viewModel.customer, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("sheet-customer"))
        OutlinedTextField(note, viewModel::edit, label = { Text("Customer note") },
            modifier = Modifier.fillMaxWidth().testTag("sheet-note"))
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth().testTag("save-sheet")) { Text("Save & close") }
        OutlinedButton(onClick = viewModel::close, modifier = Modifier.fillMaxWidth().testTag("vm-close")) { Text("Close from ViewModel") }
        TextButton(onClick = onPop, modifier = Modifier.testTag("pop-sheet")) { Text("Pop through NavController") }
        TextButton(onClick = onFullScreen, modifier = Modifier.testTag("sheet-full-screen")) { Text("Navigate to a full screen") }
    }
}
