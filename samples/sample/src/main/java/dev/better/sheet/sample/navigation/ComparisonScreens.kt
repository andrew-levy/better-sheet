package dev.better.sheet.sample.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dev.better.sheet.sample.R

abstract class ComparisonScreenFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            id = R.id.navigation_screen_compose
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MaterialTheme { this@ComparisonScreenFragment.Content() } }
        }

    @Composable protected abstract fun Content()
}

@Composable
private fun Screen(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF5F5F0)).safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("FRAGMENT NAVIGATION", style = MaterialTheme.typography.labelLarge)
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(subtitle, style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider()
        content()
    }
}

class ComparisonHomeFragment : ComparisonScreenFragment() {
    @Composable override fun Content() {
        Screen("Sheet comparison", "Three regular Fragment screens. Two ways to host the same sheet.") {
            Text("Legacy: DialogFragment + BetterSheet\nNew: dialog-less Fragment + BetterModalSheet")
            Button(onClick = { findNavController().navigateSafe(R.id.openCustomers) },
                modifier = Modifier.testTag("customers")) { Text("Browse customers") }
        }
    }
}

class ComparisonCustomersFragment : ComparisonScreenFragment() {
    @Composable override fun Content() {
        Screen("Customers", "A normal Fragment destination in the same navigation graph.") {
            Text("Alex Rivera", style = MaterialTheme.typography.titleLarge)
            Text("Enterprise account · New York")
            Button(onClick = { findNavController().navigateSafe(R.id.openCustomer) },
                modifier = Modifier.testTag("customer")) { Text("View customer") }
            TextButton(onClick = { findNavController().popBackStack() }) { Text("Back") }
        }
    }
}

class ComparisonCustomerFragment : ComparisonScreenFragment() {
    @Composable override fun Content() {
        val nav = findNavController()
        // On recreation a sheet may be current; subscribe to this screen's entry explicitly.
        val entry = remember(nav) { nav.getBackStackEntry(R.id.comparisonCustomer) }
        val result by entry.savedStateHandle.getStateFlow("customer-result", "No changes yet").collectAsState()
        var count by rememberSaveable { mutableIntStateOf(0) }
        Screen("Alex Rivera", "Open either version. Both use navigateSafe(), a Fragment VM, and Content().") {
            Text("Last result: $result", Modifier.testTag("result"))
            Button(onClick = { count++ }, modifier = Modifier.testTag("background-counter")) {
                Text("Screen counter: $count")
            }
            Text("The counter proves this screen stays mounted and becomes interactive after dismissal.")
            Button(onClick = { nav.navigateSafe(R.id.openLegacySheet) },
                modifier = Modifier.fillMaxWidth().testTag("open-legacy")) { Text("Old · DialogFragment") }
            Button(onClick = { nav.navigateSafe(R.id.openModalSheet) },
                modifier = Modifier.fillMaxWidth().testTag("open-modal")) { Text("New · Modal sheet destination") }
            TextButton(onClick = { nav.popBackStack() }) { Text("Back to customers") }
        }
    }
}

class ComparisonReceiptFragment : ComparisonScreenFragment() {
    @Composable override fun Content() {
        Screen("Full screen destination", "Navigating here removes the floating sheet destinations.") {
            Button(onClick = { findNavController().popBackStack() }, modifier = Modifier.testTag("receipt-back")) {
                Text("Back to customer")
            }
        }
    }
}
