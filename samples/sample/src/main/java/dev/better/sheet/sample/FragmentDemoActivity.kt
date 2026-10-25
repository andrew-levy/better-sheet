package dev.better.sheet.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import dev.better.sheet.*
import kotlinx.coroutines.launch

private val Ink = Color(0xFF1C1917)
private val Accent = Color(0xFF0E6B4F)
private val Paper = Color(0xFFF3F0E8)
private val Cream = Color(0xFFFFFCF7)
private val Muted = Color(0xFF6F6A62)

class FragmentDemoActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        val root = FrameLayout(this)
        val background = ComposeView(this).apply {
            id = R.id.fragment_demo_content
            setContent {
                MaterialTheme(colorScheme = lightColorScheme(primary = Accent, background = Paper, surface = Cream, onSurface = Ink)) {
                    Column(Modifier.fillMaxSize().background(Paper).safeDrawingPadding().padding(20.dp)) {
                        Text("HARBOR", color = Accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Text("Saved", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Ink)
                        Spacer(Modifier.height(8.dp))
                        Text("Places you kept on this phone.", color = Muted)
                        Spacer(Modifier.height(20.dp))
                        Surface(
                            Modifier.fillMaxWidth().testTag("open-fragment").clickable {
                                if (supportFragmentManager.findFragmentByTag("sheet") == null) {
                                    supportFragmentManager.beginTransaction()
                                        .add(R.id.fragment_sheet_container, RootSheetFragment(), "sheet").commit()
                                }
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = Cream,
                            shadowElevation = 1.dp,
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(Modifier.size(42.dp).background(Accent, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                    Text("J", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("June's", color = Ink, fontWeight = FontWeight.SemiBold)
                                    Text("Pizza · Mission", color = Muted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        root.addView(background, FrameLayout.LayoutParams(-1, -1))
        root.addView(FrameLayout(this).apply { id = R.id.fragment_sheet_container }, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
    }
}

class RootSheetFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            id = R.id.fragment_sheet_content
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme(colorScheme = lightColorScheme(primary = Accent, background = Paper, surface = Cream, onSurface = Ink)) {
                    val state = rememberSheetState(initialSnapPoint = "peek")
                    val scope = rememberCoroutineScope()
                    var note by remember { mutableStateOf("Margherita, extra basil.") }
                    BetterSheet(
                        state = state,
                        snapPoints = listOf(SheetSnapPoint.fixed("peek", 280.dp), SheetSnapPoint.expanded()),
                        title = "June's",
                        onDismissed = {
                            parentFragmentManager.beginTransaction().remove(this@RootSheetFragment).commit()
                        },
                        header = {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                                Text("June's", Modifier.testTag("fragment-header"), color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text("Mission · Saved", color = Accent, style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(8.dp))
                            }
                        },
                        footer = {
                            Surface(shadowElevation = 6.dp, color = Cream) {
                                Button(onClick = { scope.launch { state.hide() } },
                                    modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("close-fragment")) {
                                    Text("Done")
                                }
                            }
                        },
                    ) {
                        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf("18th Street" to "Open until 10", "Tonight" to "Wood-fired, no reservations").forEach { (title, detail) ->
                                Column(Modifier.fillMaxWidth().background(Paper, RoundedCornerShape(14.dp)).padding(16.dp)) {
                                    Text(title, color = Ink, fontWeight = FontWeight.Medium)
                                    Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            OutlinedTextField(note, { note = it }, label = { Text("Note for next time") }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
}
