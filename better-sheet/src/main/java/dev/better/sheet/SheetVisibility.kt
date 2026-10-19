package dev.better.sheet

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout

/**
 * Uses Compose's public animateEnterExit modifier while retaining the mounted body.
 * AnimatedVisibility itself disposes its body after exit, which would lose scroll/form state
 * and prevent hidden sheets from measuring content-fit snap points before their next entrance.
 */
@Composable
internal fun rememberSheetVisibilityScope(state: SheetState): AnimatedVisibilityScope {
    val visibilityTransition = rememberTransition(state.visibility, label = "Sheet visibility")
    val fraction = visibilityTransition.animateFloat(
        transitionSpec = { tween(150) }, label = "Sheet scrim and stack progress",
    ) { if (it == EnterExitState.Visible) 1f else 0f }
    SideEffect { state.presentationProgress = fraction }
    return remember(visibilityTransition) {
        object : AnimatedVisibilityScope {
            override val transition: Transition<EnterExitState> = visibilityTransition
        }
    }
}

/** Measure hidden content without placing it: no drawing, hit targets or accessibility nodes. */
internal fun Modifier.sheetPresentationPlacement(state: SheetState): Modifier = layout { measurable, constraints ->
    val child = measurable.measure(constraints)
    layout(child.width, child.height) {
        if (state.presentationActive && state.isVisible) child.place(0, 0)
    }
}
