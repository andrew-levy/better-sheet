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
import androidx.compose.ui.text.font.FontWeight
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
            EditorContent("Opened in a dialog", viewModel, onSave = { saveResult(viewModel) },
                onPop = { findNavController().popBackStack() },
                onFullScreen = { findNavController().navigateSafe(R.id.openReceipt) })
        }
    }
}

/** Option 3 keeps the stock dialog navigator and the same VM-backed editor. */
class AdapterCustomerSheetFragment : SheetDialogAdapter() {
    private val viewModel: CustomerSheetViewModel by viewModels()

    @Composable override fun Content() {
        val state = rememberSheetState(initialSnapPoint = "medium")
        val close by viewModel.closeRequested.collectAsState()
        LaunchedEffect(close) { if (close) state.hide() }
        BetterSheet(
            state = state,
            snapPoints = customerSnapPoints,
            title = "Adapter customer sheet",
            onDismissed = { dismiss() },
        ) {
            EditorContent("Opened in a dialog", viewModel, onSave = { saveResult(viewModel) },
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
            EditorContent("From this account", viewModel, onSave = { saveResult(viewModel) },
                onPop = { findNavController().popBackStack() },
                onFullScreen = { findNavController().navigateSafe(R.id.openReceipt) },
                followUp = if (depth < 2) {
                    {
                        findNavController().navigateSafe(R.id.openModalSheet,
                            bundleOf("customer" to "Second sheet", "depth" to depth + 1))
                    }
                } else null)
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
    host: String,
    viewModel: CustomerSheetViewModel,
    onSave: () -> Unit,
    onPop: () -> Unit,
    onFullScreen: () -> Unit,
    followUp: (() -> Unit)? = null,
) {
    val note by viewModel.note.collectAsState()
    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Note", color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("sheet-title"))
        Text(viewModel.customer, color = Accent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("sheet-customer"))
        Text(host, color = Muted, style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(note, viewModel::edit, label = { Text("What should we remember?") },
            modifier = Modifier.fillMaxWidth().testTag("sheet-note"), minLines = 3)
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth().testTag("save-sheet")) { Text("Save note") }
        OutlinedButton(onClick = viewModel::close, modifier = Modifier.fillMaxWidth().testTag("vm-close")) { Text("Discard") }
        TextButton(onClick = onPop, modifier = Modifier.testTag("pop-sheet")) { Text("Back") }
        TextButton(onClick = onFullScreen, modifier = Modifier.testTag("sheet-full-screen")) { Text("View receipt") }
        if (followUp != null) {
            TextButton(onClick = followUp, modifier = Modifier.testTag("stack-sheet")) { Text("Add a follow-up") }
        }
    }
}
