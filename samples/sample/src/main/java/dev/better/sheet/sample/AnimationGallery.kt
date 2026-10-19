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
            Triple(
                "Spring",
                "Spring",
                "Enter/exit: vertical slide with a stiff spring (slight overshoot). " +
                    "Snap (Expand / Half height): low-damping spring — watch for a quick bounce past the detent, then settle. " +
                    "No opacity change on show/hide.",
            ),
            Triple(
                "Fade",
                "Fade",
                "Enter/exit: opacity only (~900 ms in, ~750 ms out) — the sheet should not slide; it fades in place. " +
                    "Scrim and sheet blue wash in together. Snap uses a slow tween (~1.4 s) so height changes are easy to see.",
            ),
            Triple(
                "SlideFade",
                "Slide + fade",
                "Enter/exit: slide and fade together (~750 ms in, ~550 ms out). " +
                    "Snap: high-stiffness spring — should reach the detent fast with little float. " +
                    "Compare to Spring: more motion on open/close, snappier detent changes.",
            ),
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
        "Fade" -> fadeIn(tween(900))
        "SlideFade" -> slideInVertically(tween(750)) { it } + fadeIn(tween(750))
        else -> slideInVertically(spring(dampingRatio = .85f, stiffness = 920f)) { it }
    }
    val exit = when (preset) {
        "Fade" -> fadeOut(tween(750))
        "SlideFade" -> slideOutVertically(tween(550)) { it } + fadeOut(tween(550))
        else -> slideOutVertically(spring(dampingRatio = .85f, stiffness = 920f)) { it }
    }
    BetterModalSheet(state, points, title = "Animation examples",
        enterTransition = enter, exitTransition = exit,
        snapAnimationSpec = when (preset) {
            "Fade" -> tween(1400)
            "SlideFade" -> spring(dampingRatio = .9f, stiffness = 1400f)
            else -> spring(dampingRatio = .5f, stiffness = 720f)
        },
        header = {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(
                    when (preset) {
                        "Fade" -> "Fade + slow tween snap"
                        "SlideFade" -> "Slide + fade · snappy snap"
                        else -> "Bouncy spring slide + snap"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    when (preset) {
                        "Fade" ->
                            "Open/Close: fade only. Expand/Half height: ~1.4 s height tween — no bounce."
                        "SlideFade" ->
                            "Open/Close: slide + fade. Expand/Half height: stiff spring — fast settle."
                        else ->
                            "Open/Close: spring slide. Expand/Half height: undershoot/overshoot bounce."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
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
            Text(
                "1) Note how the sheet appears and disappears. " +
                    "2) Tap Expand, then Half height — watch snap timing. " +
                    "3) Close and reopen another preset to compare.",
            )
            repeat(3) { index ->
                Surface(color = Color(0xFFF3F5FA), shape = MaterialTheme.shapes.medium) {
                    Text(listOf("Sticky actions stay at the bottom", "Dragging still follows your finger", "The same API works in fragments")[index],
                        Modifier.fillMaxWidth().padding(16.dp))
                }
            }
        }
    }
}
