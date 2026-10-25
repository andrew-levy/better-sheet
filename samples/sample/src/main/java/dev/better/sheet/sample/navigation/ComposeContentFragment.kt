package dev.better.sheet.sample.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import dev.better.sheet.sample.R

/** Both examples preserve the enterprise-style Fragment VM and Content() override. */
abstract class ComposeContentFragment : DialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            id = R.id.navigation_sheet_compose
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme(colorScheme = lightColorScheme(primary = Accent, background = Paper, surface = Color.White, onSurface = Ink)) {
                    this@ComposeContentFragment.Content()
                }
            }
        }

    @Composable protected abstract fun Content()
}
