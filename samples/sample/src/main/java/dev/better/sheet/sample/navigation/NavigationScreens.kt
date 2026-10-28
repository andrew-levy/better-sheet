package dev.better.sheet.sample.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dev.better.sheet.sample.R

abstract class NavigationScreenFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            id = R.id.navigation_screen_compose
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme(colorScheme = lightColorScheme(primary = Accent, background = Paper, surface = Color.White, onSurface = Ink)) {
                    this@NavigationScreenFragment.Content()
                }
            }
        }

    @Composable protected abstract fun Content()
}

internal val Ink = Color(0xFF172033)
internal val Accent = Color(0xFF6554E8)
internal val Paper = Color(0xFFF5F6FB)
internal val Muted = Color(0xFF68728A)

@Composable
private fun Screen(eyebrow: String, title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Paper).safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(eyebrow, color = Accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(title, color = Ink, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun AccountRow(title: String, subtitle: String, tag: String, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().testTag(tag).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = Ink, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Text("↗", color = Accent, style = MaterialTheme.typography.titleLarge)
        }
    }
}

class HomeFragment : NavigationScreenFragment() {
    @Composable override fun Content() {
        Screen("ACCOUNTS", "People you\nwork with.", "Open an account, leave a note, and come back to this screen.") {
            AccountRow("Alex Rivera", "Enterprise · New York", "customers") {
                findNavController().navigateSafe(R.id.openCustomers)
            }
        }
    }
}

class CustomersFragment : NavigationScreenFragment() {
    @Composable override fun Content() {
        Screen("CUSTOMERS", "Accounts", "One account in this sample.") {
            AccountRow("Alex Rivera", "Enterprise account · New York", "customer") {
                findNavController().navigateSafe(R.id.openCustomer)
            }
            TextButton(onClick = { findNavController().popBackStack() }) { Text("Back") }
        }
    }
}

class CustomerFragment : NavigationScreenFragment() {
    @Composable override fun Content() {
        val nav = findNavController()
        // On recreation a sheet may be current; subscribe to this screen's entry explicitly.
        val entry = remember(nav) { nav.getBackStackEntry(R.id.customerDetails) }
        val result by entry.savedStateHandle.getStateFlow("customer-result", "No changes yet").collectAsState()
        var calls by rememberSaveable { mutableIntStateOf(0) }
        Screen("ACCOUNT", "Alex Rivera", "Enterprise · New York") {
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("NOTE", color = Accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (result == "No changes yet") "Nothing saved yet" else result,
                        Modifier.testTag("result"),
                        color = Ink,
                    )
                }
            }
            OutlinedButton(onClick = { calls++ }, modifier = Modifier.testTag("background-counter")) {
                Text("Log a call · $calls")
            }
            Button(onClick = { nav.navigateSafe(R.id.openCustomerSheet) },
                modifier = Modifier.fillMaxWidth().testTag("open-sheet")) { Text("Add a note") }
            TextButton(onClick = { nav.popBackStack() }) { Text("Back to accounts") }
        }
    }
}

class ReceiptFragment : NavigationScreenFragment() {
    @Composable override fun Content() {
        Screen("RECEIPT", "Note saved", "The sheet closed on the way here. Back returns to the account.") {
            Button(onClick = { findNavController().popBackStack() }, modifier = Modifier.testTag("receipt-back")) {
                Text("Back to account")
            }
        }
    }
}
