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
    Column(Modifier.fillMaxSize().background(Color(0xFFF3F0E8)).safeDrawingPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("MOTION", color = Color(0xFF0E6B4F), style = MaterialTheme.typography.labelLarge)
        Text("How a sheet arrives.", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF1C1917))
        Text("Same place card. Three ways it can enter, leave, and change height.", color = Color(0xFF6F6A62))
        for ((id, title, detail) in listOf(
            Triple("Spring", "Spring", "Slides in and settles with a little bounce."),
            Triple("Fade", "Fade", "Appears in place. Height changes are slow and even."),
            Triple("SlideFade", "Slide + fade", "Slides and fades together, then snaps quickly."),
        )) {
            OutlinedButton(onClick = { preset = id; scope.launch { state.show("medium") } },
                modifier = Modifier.fillMaxWidth().testTag("animation-open-$id")) {
                Column(Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(detail, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        TextButton(onClick = close) { Text("Back") }
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
                        "Fade" -> "Fade"
                        "SlideFade" -> "Slide and fade"
                        else -> "Spring"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    when (preset) {
                        "Fade" -> "Fades in place. Expand and half height move slowly."
                        "SlideFade" -> "Slides and fades. Height changes settle fast."
                        else -> "Springs in from the bottom, then bounces into place."
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
            Text("Blue Bottle · Hayes Valley", style = MaterialTheme.typography.titleMedium)
            listOf("Open until 7" to "315 Linden Street", "Window seat" to "Quiet after 5").forEach { (title, detail) ->
                Surface(color = Color(0xFFF3F0E8), shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(title)
                        Text(detail, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
