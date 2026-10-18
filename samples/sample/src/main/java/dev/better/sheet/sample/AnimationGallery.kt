package dev.better.sheet.sample

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.better.sheet.*
import kotlinx.coroutines.launch

/** The same content and snap points with three configurations; used by the README recording. */
@Composable
internal fun AnimationGallery(close: () -> Unit) {
    var preset by remember { mutableStateOf("Spring") }
    val state = rememberSheetState("medium", initiallyVisible = false)
    val scope = rememberCoroutineScope()
    val points = remember { listOf(SheetSnapPoint.fraction("medium", .5f), SheetSnapPoint.expanded()) }
    Column(Modifier.fillMaxSize().background(Color(0xFFF3F5FA)).safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Make it move your way.", style = MaterialTheme.typography.headlineMedium)
        Text("One sheet. Standard Compose transitions. A separate spec for snapping.")
        for ((id, title, detail) in listOf(
            Triple("Spring", "Spring", "Slide in/out · bouncy spring snapping"),
            Triple("Fade", "Fade", "220 ms in / 180 ms out · 600 ms tween snapping"),
            Triple("SlideFade", "Slide + fade", "450 ms enter / 300 ms exit · firm spring snapping"),
        )) {
            OutlinedButton(onClick = { preset = id; scope.launch { state.show("medium") } },
                modifier = Modifier.fillMaxWidth().testTag("animation-open-$id")) {
                Column(Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(detail, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        TextButton(onClick = close) { Text("Back to demos") }
    }
    val enter = when (preset) {
        "Fade" -> fadeIn(tween(220))
        "SlideFade" -> slideInVertically(tween(450)) { it } + fadeIn(tween(450))
        else -> slideInVertically(spring(dampingRatio = 1f, stiffness = 440f)) { it }
    }
    val exit = when (preset) {
        "Fade" -> fadeOut(tween(180))
        "SlideFade" -> slideOutVertically(tween(300)) { it } + fadeOut(tween(300))
        else -> slideOutVertically(spring(dampingRatio = 1f, stiffness = 440f)) { it }
    }
    BetterModalSheet(state, points, title = "Animation examples",
        enterTransition = enter, exitTransition = exit,
        snapAnimationSpec = when (preset) {
            "Fade" -> tween(600)
            "SlideFade" -> spring(dampingRatio = 1f, stiffness = 600f)
            else -> spring(dampingRatio = .65f, stiffness = 180f)
        },
        header = {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(when (preset) { "Fade" -> "Fade + tween"; "SlideFade" -> "Slide + fade"; else -> "Bouncy spring" },
                    style = MaterialTheme.typography.headlineSmall)
                Text("Enter / exit describe appearance. Snap spec controls movement.")
            }
        },
        footer = {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { scope.launch { state.snapTo("expanded") } },
                        modifier = Modifier.weight(1f).testTag("animation-expanded")) { Text("Expand") }
                    OutlinedButton(onClick = { scope.launch { state.snapTo("medium") } },
                        modifier = Modifier.weight(1f).testTag("animation-medium")) { Text("Half height") }
                }
                TextButton(onClick = { scope.launch { state.hide() } },
                    modifier = Modifier.fillMaxWidth().testTag("animation-close")) { Text("Close") }
            }
        }) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Watch the entrance, try both snap points, then close the sheet.")
            repeat(3) { index ->
                Surface(color = Color(0xFFF3F5FA), shape = MaterialTheme.shapes.medium) {
                    Text(listOf("Sticky actions stay at the bottom", "Dragging still follows your finger", "The same API works in fragments")[index],
                        Modifier.fillMaxWidth().padding(16.dp))
                }
            }
        }
    }
}
