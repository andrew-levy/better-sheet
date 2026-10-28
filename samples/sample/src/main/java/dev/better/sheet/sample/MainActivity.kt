package dev.better.sheet.sample

import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.better.sheet.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher

private val Ink = Color(0xFF1C1917)
private val Accent = Color(0xFF0E6B4F)
private val Paper = Color(0xFFF3F0E8)
private val Cream = Color(0xFFFFFCF7)
private val Clay = Color(0xFFC46A45)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        val autoOpen = intent.getStringExtra(EXTRA_DEMO)?.let { Demo.valueOf(it) }
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Accent,
                onPrimary = Color.White,
                secondary = Accent,
                secondaryContainer = Color(0xFFD7EBE3),
                onSecondaryContainer = Color(0xFF083D2C),
                background = Paper,
                surface = Cream,
                onSurface = Ink,
            )) {
                Gallery(autoOpen)
            }
        }
    }
    companion object {
        /** Demo name to open shortly after launch; used by real-clock timing tests. */
        const val EXTRA_DEMO = "demo"
    }
}

/** Real-clock opening timings, from the open request to the sheet settling at its snap point. */
data class OpenTiming(val firstMotionMillis: Long, val settledMillis: Long, val longestFrameMillis: Long, val frames: Int)

object OpenTimings {
    val results = ConcurrentHashMap<String, OpenTiming>()
}

enum class Demo(val label: String, val subtitle: String) {
    SnapPoints("Blue Bottle", "Peek, half, and full. The list hands off to the sheet."),
    Tray("Dinner at seven", "Floats inset, then meets the screen edges."),
    Chat("Maya Chen", "The composer stays above the keyboard."),
    Heavy("Last year's thread", "Thousands of messages while the sheet is opening."),
    Stack("This week", "Each new sheet sits on top of the last."),
    Form("Reserve a table", "The request stays pinned while the form scrolls."),
    Fit("The order", "The sheet grows and shrinks with the menu."),
    Animations("Spring, fade, slide", "The same card, three ways of arriving."),
}

@Composable
private fun Gallery(autoOpen: Demo?) {
    val context = LocalContext.current
    var selected by rememberSaveable { mutableStateOf<Demo?>(null) }
    var session by remember { mutableIntStateOf(0) }
    var openedAt by remember { mutableLongStateOf(0L) }
    if (autoOpen != null) LaunchedEffect(autoOpen) {
        delay(800)
        openedAt = SystemClock.uptimeMillis()
        selected = autoOpen
    }
    fun open(demo: Demo) {
        openedAt = SystemClock.uptimeMillis()
        session++
        selected = demo
    }
    Box(Modifier.fillMaxSize().background(Paper)) {
        LazyColumn(Modifier.fillMaxSize().safeDrawingPadding().testTag("gallery"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("HARBOR", color = Accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text("Sheets,\nby feature.", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.height(8.dp))
                Text("One evening in the neighborhood. Each section is a different kind of sheet.", color = Muted)
            }
            item { SectionLabel("Snap points") }
            item { PlaceHero { open(Demo.SnapPoints) } }
            item { SectionLabel("Sticky footer") }
            item { StoryRow(Demo.Form, Clay) { open(Demo.Form) } }
            item { SectionLabel("Keyboard") }
            item { StoryRow(Demo.Chat, Accent) { open(Demo.Chat) } }
            item { SectionLabel("Floating tray") }
            item { StoryRow(Demo.Tray, Clay) { open(Demo.Tray) } }
            item { SectionLabel("Content fit") }
            item { StoryRow(Demo.Fit, Accent) { open(Demo.Fit) } }
            item { SectionLabel("Stacked sheets") }
            item { StoryRow(Demo.Stack, Accent) { open(Demo.Stack) } }
            item { SectionLabel("Heavy content") }
            item { StoryRow(Demo.Heavy, Ink) { open(Demo.Heavy) } }
            item { SectionLabel("Fragments") }
            item {
                StoryRow("Navigation comparison", "Compare all three Fragment sheet hosts.", "open-navigation", Clay) {
                    context.startActivity(Intent(context, dev.better.sheet.sample.navigation.NavigationComparisonActivity::class.java))
                }
            }
            item {
                StoryRow("June's", "A saved place, hosted by a Fragment.", "open-saved", Clay) {
                    context.startActivity(Intent(context, FragmentDemoActivity::class.java))
                }
            }
            item { SectionLabel("Motion") }
            item { StoryRow(Demo.Animations, Ink) { open(Demo.Animations) } }
        }
        selected?.let { demo ->
            key(demo, session) {
                val token = session
                DemoSheet(demo, openedAt) { if (session == token) selected = null }
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(title.uppercase(), color = Accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
}

@Composable
private fun PlaceHero(onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().testTag("open-${Demo.SnapPoints.name}").clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Cream),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(96.dp).background(Accent)) {
                Text("PEEK  ·  HALF  ·  FULL", Modifier.align(Alignment.BottomStart).padding(16.dp), color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.padding(18.dp)) {
                Text(Demo.SnapPoints.label, color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(Demo.SnapPoints.subtitle, color = Muted)
            }
        }
    }
}

@Composable
private fun StoryRow(demo: Demo, mark: Color, onClick: () -> Unit) {
    StoryRow(demo.label, demo.subtitle, "open-${demo.name}", mark, onClick)
}

@Composable
private fun StoryRow(title: String, subtitle: String, tag: String, mark: Color, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().testTag(tag).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Cream),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(42.dp).background(mark, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Text(title.take(1).toString(), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = Ink, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DemoSheet(demo: Demo, openedAt: Long, close: () -> Unit) {
    if (demo == Demo.Animations) {
        AnimationGallery(close)
        return
    }
    if (demo == Demo.Stack) {
        StackedSheets(close)
        return
    }
    val scope = rememberCoroutineScope()
    val initial = when (demo) {
        Demo.Fit -> "content"
        Demo.Form -> "reserve"
        Demo.SnapPoints, Demo.Tray -> "peek"
        else -> "medium"
    }
    val state = rememberSheetState(initialSnapPoint = initial)
    val snapPoints = remember(demo) {
        when (demo) {
            Demo.Fit -> listOf(SheetSnapPoint.fitContent(), SheetSnapPoint.expanded())
            Demo.Form -> listOf(SheetSnapPoint.fraction("reserve", 0.78f))
            Demo.Tray -> listOf(SheetSnapPoint.fixed("peek", 460.dp), SheetSnapPoint.fraction("medium", 0.62f), SheetSnapPoint.expanded())
            Demo.Chat, Demo.Heavy -> listOf(SheetSnapPoint.fixed("peek", 300.dp), SheetSnapPoint.fraction("medium", 0.7f), SheetSnapPoint.expanded())
            else -> listOf(SheetSnapPoint.fixed("peek", 340.dp), SheetSnapPoint.fraction("medium", 0.55f), SheetSnapPoint.expanded())
        }
    }
    val behavior = remember(demo) {
        SheetBehavior(keyboardBehavior = if (demo == Demo.Chat || demo == Demo.Heavy) SheetKeyboardBehavior.Expand else SheetKeyboardBehavior.Resize)
    }
    var message by rememberSaveable { mutableStateOf("") }
    var extra by rememberSaveable { mutableStateOf(false) }
    val chatList = rememberLazyListState()
    var heavyThread by remember { mutableStateOf<List<HeavyMessage>?>(null) }
    if (demo == Demo.Heavy) LaunchedEffect(Unit) { heavyThread = loadHeavyThread() }
    LaunchedEffect(state) { recordOpenTiming(demo, state, openedAt) }
    val messages = remember {
        mutableStateListOf(
            "Running a few minutes late.",
            "No rush. I'm at the counter.",
            "Want me to order you the usual?",
            "Yes. And a seat by the window if they have one.",
            "Got it. See you in ten.",
        )
    }
    val chips = when (demo) {
        Demo.SnapPoints -> listOf("peek" to "Card", "medium" to "About", "expanded" to "Reviews")
        Demo.Tray -> listOf("peek" to "Now", "medium" to "Evening", "expanded" to "Night")
        Demo.Chat, Demo.Heavy -> listOf("peek" to "Latest", "medium" to "Thread", "expanded" to "All")
        else -> emptyList()
    }
    val subtitle = when (demo) {
        Demo.SnapPoints -> "Hayes Valley · Open until 7"
        Demo.Tray -> "The Progress · 7:00 PM"
        Demo.Chat -> "Meeting you at the cafe"
        Demo.Heavy -> heavyThread?.let { "%,d messages".format(it.size) } ?: "Opening the archive…"
        Demo.Fit -> "Two courses, unless you want the rest"
        Demo.Form -> "The Progress · tonight"
        else -> demo.subtitle
    }
    val header: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).testTag("sheet-header")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(demo.label, modifier = Modifier.testTag("sheet-title"), color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = Accent, style = MaterialTheme.typography.labelMedium, modifier = Modifier.testTag("sheet-status"))
                }
                TextButton(onClick = { scope.launch { state.hide() } }, modifier = Modifier.testTag("close")) { Text("Close") }
            }
            if (chips.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    chips.forEach { (id, label) ->
                        FilterChip(selected = state.currentSnapPoint == id, onClick = { scope.launch { state.snapTo(id) } }, label = { Text(label) }, modifier = Modifier.testTag("snap-point-$id"))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    val footer: @Composable () -> Unit = {
        Surface(shadowElevation = 6.dp, color = Cream) {
            when (demo) {
                Demo.Chat, Demo.Heavy -> Row(Modifier.fillMaxWidth().padding(16.dp).testTag("footer"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(message, { message = it }, placeholder = { Text("Message Maya") }, modifier = Modifier.weight(1f).testTag("composer"), maxLines = 3, shape = RoundedCornerShape(24.dp))
                    Button(onClick = {
                        if (message.isNotBlank()) {
                            if (demo == Demo.Heavy) heavyThread = heavyThread.orEmpty() + HeavyMessage("You", message, "sent") else {
                                messages.add(message)
                                chatList.requestScrollToItem(messages.lastIndex)
                            }
                            message = ""
                        }
                    }, modifier = Modifier.testTag("send")) { Text("Send") }
                }
                Demo.Fit -> Column(Modifier.fillMaxWidth().padding(16.dp).testTag("footer")) {
                    Button(onClick = { extra = !extra }, modifier = Modifier.fillMaxWidth().testTag("primary-action")) {
                        Text(if (extra) "Just the highlights" else "Add the rest of the menu")
                    }
                }
                Demo.Form -> Column(Modifier.fillMaxWidth().padding(16.dp).testTag("footer")) {
                    Button(onClick = { scope.launch { state.hide() } }, modifier = Modifier.fillMaxWidth().testTag("primary-action")) { Text("Request the table") }
                }
                Demo.Tray -> Column(Modifier.fillMaxWidth().padding(16.dp).testTag("footer")) {
                    Button(onClick = { scope.launch { state.hide() } }, modifier = Modifier.fillMaxWidth()) { Text("I'll be there") }
                }
                else -> Column(Modifier.fillMaxWidth().padding(16.dp).testTag("footer")) {
                    Button(onClick = { scope.launch { state.hide() } }, modifier = Modifier.fillMaxWidth()) { Text("Directions") }
                }
            }
        }
    }
    val body: @Composable ColumnScope.() -> Unit = {
        when (demo) {
            Demo.Heavy -> HeavyThreadBody(heavyThread)
            Demo.Chat -> LazyColumn(state = chatList, modifier = Modifier.fillMaxSize().testTag("body-list"), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(messages.size, key = { it }) { i ->
                    val mine = i % 2 == 1
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                        Text(messages[i], Modifier.widthIn(max = 280.dp).background(if (mine) Accent else Paper, RoundedCornerShape(18.dp)).padding(16.dp), color = if (mine) Color.White else Ink)
                    }
                }
            }
            Demo.Fit -> Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp).testTag("fit-body"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val courses = listOf("Olive oil cake", "Affogato") + if (extra) listOf("Brown butter financier", "Chocolate tart", "Seasonal sorbet", "Espresso") else emptyList()
                courses.forEach { course ->
                    Text(course, Modifier.fillMaxWidth().background(Paper, RoundedCornerShape(14.dp)).padding(16.dp), color = Ink, fontWeight = FontWeight.Medium)
                }
            }
            Demo.Form -> Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("The request stays pinned while you fill this in.", color = Muted)
                listOf("Name", "Party size", "Time", "Occasion", "Note for the kitchen").forEachIndexed { i, label ->
                    var field by rememberSaveable(label) { mutableStateOf("") }
                    OutlinedTextField(field, { field = it }, label = { Text(label) }, modifier = Modifier.fillMaxWidth().testTag("field-$i"))
                }
            }
            Demo.Tray -> LazyColumn(Modifier.fillMaxSize().testTag("body-list"), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(listOf("7:00  Walk over from Blue Bottle" to "Eight minutes", "7:10  Table by the window" to "Reserved under Maya", "8:30  Dessert back at the cafe" to "If you're still hungry"), key = { it.first }) { (title, detail) ->
                    Column(Modifier.fillMaxWidth().background(Paper, RoundedCornerShape(14.dp)).padding(16.dp)) {
                        Text(title, color = Ink, fontWeight = FontWeight.Medium)
                        Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize().testTag("body-list"), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Column(Modifier.fillMaxWidth().background(Paper, RoundedCornerShape(14.dp)).padding(16.dp)) {
                        Text("315 Linden Street", color = Ink, fontWeight = FontWeight.Medium)
                        Text("Single origin on the bar. Quiet after 5.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                items(listOf("Priya" to "The window seat is worth the wait.", "Jon" to "Ask for the filter if the espresso line is long.", "Maya" to "Best stop before dinner.", "Alex" to "They'll hold a pastry if you call."), key = { it.first }) { (name, review) ->
                    Column(Modifier.fillMaxWidth().background(Paper, RoundedCornerShape(14.dp)).padding(16.dp)) {
                        Text(name, color = Accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text(review, color = Ink)
                    }
                }
            }
        }
    }
    val mode = if (demo == Demo.Fit || demo == Demo.Form) SheetBodyMode.Scroll else SheetBodyMode.Fill
    BetterModalSheet(state, snapPoints, close, behavior = behavior, bodyMode = mode, title = demo.label,
        presentation = if (demo == Demo.Tray) SheetPresentation.Tray() else SheetPresentation.Standard,
        header = header, footer = footer, content = body)
}

private val Muted = Color(0xFF6F6A62)

/** App-defined recession; Better only supplies stack lifecycle and gesture progress. */
@Composable
private fun StackedSheets(close: () -> Unit) {
    val stackState = rememberSheetStackState()
    val plans = rememberSheetState()
    val details = rememberSheetState(initiallyVisible = false)
    val review = rememberSheetState(initialSnapPoint = "content", initiallyVisible = false)
    val scope = rememberCoroutineScope()
    val effect = Modifier.sheetStackEffect { entry ->
        val depth = entry.animatedDepth
        scaleX = (1f - 0.06f * depth).coerceAtLeast(0.7f)
        scaleY = (1f - 0.04f * depth).coerceAtLeast(0.8f)
        translationY = (-12).dp * depth
        cornerRadius = (28 + 8 * depth).dp
    }
    val closePlans: () -> Unit = { scope.launch { plans.hide() }; Unit }
    val closeDetails: () -> Unit = { scope.launch { details.hide() }; Unit }
    val closeReview: () -> Unit = { scope.launch { review.hide() }; Unit }
    BetterModalSheet(
        state = plans, stackState = stackState,
        snapPoints = listOf(SheetSnapPoint.fitContent()),
        surfaceModifier = effect, title = "Saturday", onDismissed = close,
        header = { StackHeader("Saturday", "Three things", "stack-close-base", Modifier.testTag("stack-base-title"), closePlans) },
        footer = { StackFooter { Button(onClick = closePlans, modifier = Modifier.fillMaxWidth().testTag("footer")) { Text("Share Saturday") } } },
    ) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PlanRow("01", "Blue Bottle", "5:30 · Hayes Valley")
            PlanRow("02", "Dinner", "The Progress · 7:00", Modifier.testTag("stack-open-details")) { scope.launch { details.show() } }
            PlanRow("03", "Walk home", "After dessert")
        }
    }
    BetterModalSheet(
        state = details, stackState = stackState,
        snapPoints = listOf(SheetSnapPoint.fitContent()),
        surfaceModifier = effect, title = "Dinner",
        header = { StackHeader("Dinner", "The Progress", "stack-close-details", Modifier.testTag("stack-details-title"), closeDetails) },
        footer = { StackFooter {
            Button(onClick = { scope.launch { review.show() } }, modifier = Modifier.fillMaxWidth().testTag("stack-open-confirm")) { Text("Ask for the window") }
        } },
    ) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            DetailRow("When", "7:00 PM")
            DetailRow("Who", "You and Maya")
            DetailRow("Table", "Window, if they have it")
        }
    }
    BetterModalSheet(
        state = review, stackState = stackState,
        snapPoints = listOf(SheetSnapPoint.fitContent()), surfaceModifier = effect, title = "Request review",
        header = { StackHeader("The window", "Two people", "stack-close-confirm", Modifier.testTag("stack-confirm-title"), closeReview) },
        footer = { StackFooter {
            Button(onClick = closeReview, modifier = Modifier.fillMaxWidth().testTag("stack-send-review")) { Text("Send request") }
        } },
    ) {
        Text("They'll hold the window seat until 7:15.",
            Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = Muted)
    }
}

@Composable
private fun StackHeader(title: String, subtitle: String, closeTag: String, titleModifier: Modifier = Modifier, close: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, titleModifier, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Accent, style = MaterialTheme.typography.labelMedium)
            }
            TextButton(onClick = close, modifier = Modifier.testTag(closeTag)) { Text("Close") }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StackFooter(content: @Composable RowScope.() -> Unit) {
    Surface(shadowElevation = 6.dp) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun PlanRow(index: String, title: String, subtitle: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().background(Paper, RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(index, color = Accent, fontWeight = FontWeight.Bold)
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, color = Ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        if (onClick != null) Text("↗", color = Accent, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().background(Paper, RoundedCornerShape(14.dp)).padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Muted)
        Text(value, fontWeight = FontWeight.Medium, color = Ink)
    }
}

class HeavyMessage(val author: String, val text: String, val digest: String)

object HeavyLoad {
    /**
     * Normal-priority workers on every core compete with the UI and render threads and delay the
     * sheet on small devices; background priority leaves frame production first in line.
     */
    @Volatile var backgroundPriority = true
    private val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
    val background = Executors.newFixedThreadPool(cores) { work ->
        Thread({ Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND); work.run() }, "heavy-load")
    }.asCoroutineDispatcher()
    val dispatcher get() = if (backgroundPriority) background else Dispatchers.Default
}

/**
 * Simulates opening a large conversation: a slow database read plus CPU-heavy decoding of every
 * message, spread over all cores, all while the sheet is presenting.
 */
private suspend fun loadHeavyThread(count: Int = 3000, rounds: Int = 400): List<HeavyMessage> = withContext(HeavyLoad.dispatcher) {
    coroutineScope {
        val database = async { delay(900) }
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        val decoded = (0 until cores).map { core ->
            async {
                val sha = MessageDigest.getInstance("SHA-256")
                (core until count step cores).map { i ->
                    var bytes = "message-$i".toByteArray()
                    repeat(rounds) { bytes = sha.digest(bytes) }
                    i to HeavyMessage(
                        author = if (i % 3 == 0) "You" else listOf("Maya", "Jon", "Priya")[i % 3],
                        text = "Message ${i + 1}: decoded ${rounds} hash rounds for a long synced thread.",
                        digest = bytes.take(4).joinToString("") { "%02x".format(it) },
                    )
                }
            }
        }.awaitAll().flatten().sortedBy { it.first }.map { it.second }
        database.await()
        decoded
    }
}

@Composable
private fun HeavyThreadBody(thread: List<HeavyMessage>?) {
    if (thread == null) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp).testTag("heavy-loading"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(8) { i ->
                Box(Modifier.fillMaxWidth(if (i % 2 == 0) .7f else .55f).height(52.dp).align(if (i % 2 == 0) Alignment.End else Alignment.Start).alpha(.6f).background(Color(0xFFE3E6F0), RoundedCornerShape(18.dp)))
            }
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().testTag("body-list"), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(thread.size, key = { it }) { i ->
            val message = thread[i]
            val mine = message.author == "You"
            Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
                Text(message.text, Modifier.widthIn(max = 280.dp).background(if (mine) Accent else Paper, RoundedCornerShape(18.dp)).padding(16.dp), color = if (mine) Color.White else Ink)
                Text("${message.author} • ${message.digest}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF68728A))
            }
        }
    }
}

/** Samples every real frame from the open request until the sheet settles. */
private suspend fun recordOpenTiming(demo: Demo, state: dev.better.sheet.SheetState, openedAt: Long) {
    if (openedAt == 0L) return
    var firstMotion = -1L
    var previous = SystemClock.uptimeMillis()
    var longest = 0L
    var frames = 0
    while (true) {
        withFrameMillis { }
        val now = SystemClock.uptimeMillis()
        if (state.progress > 0.01f) {
            if (firstMotion < 0) firstMotion = now else longest = maxOf(longest, now - previous)
            frames++
        }
        previous = now
        if (firstMotion >= 0 && state.currentSnapPoint != null && !state.isAnimationRunning) break
    }
    OpenTimings.results[demo.name] = OpenTiming(firstMotion - openedAt, previous - openedAt, longest, frames)
}

