package dev.better.sheet

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.MutatorMutex
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

/** Hoist this state. Animations are mutually exclusive through Compose's AnchoredDraggableState. */
@Stable
public class SheetState internal constructor(
    internal val initialSnapPoint: String,
    animationSpec: AnimationSpec<Float> = defaultSheetSpring(),
    internal val initiallyVisible: Boolean = true,
) {
    internal var snapAnimationSpec: AnimationSpec<Float> by mutableStateOf(animationSpec)
    internal var visibility = MutableTransitionState(EnterExitState.PreEnter)
        private set
    internal var presentationFraction by mutableFloatStateOf(0f)
    internal var transitionsManaged = false
    private val operations = MutatorMutex()
    internal val presentationActive: Boolean get() = visibility.currentState == EnterExitState.Visible ||
        visibility.targetState == EnterExitState.Visible || !visibility.isIdle
    internal val presentationExiting: Boolean get() = transitionsManaged &&
        visibility.targetState == EnterExitState.PostExit
    internal fun configureAnimations(spec: AnimationSpec<Float>) {
        transitionsManaged = true
        snapAnimationSpec = spec
    }
    private suspend fun awaitPresentation(target: EnterExitState) {
        snapshotFlow { visibility.targetState != target || visibility.isIdle }.first { it }
        if (visibility.targetState != target) throw CancellationException("Sheet presentation interrupted")
    }

    internal val anchored = AnchoredDraggableState(if (initiallyVisible) initialSnapPoint else HiddenId)
    internal var geometry: SheetGeometry? by mutableStateOf(null)
    private var dragDismiss = true
    private var programmaticHide = false
    private var managedPresentation = false
    internal var measurementsReady by mutableStateOf(false)
    internal var hasPresented by mutableStateOf(false)
    internal var lastVisibleSnapPoint = initialSnapPoint
    private val rubberTranslationState = mutableFloatStateOf(0f)
    /**
     * Edge resistance, in px. Negative stretches the sheet upward (bottom stays put);
     * positive pulls the whole sheet down.
     */
    internal val rubberTranslation: Float get() = rubberTranslationState.floatValue
    private val dragInProgressState = mutableStateOf(false)
    /** True while a finger is dragging the sheet. Settle animations are not drags. */
    internal val dragInProgress: Boolean get() = dragInProgressState.value
    private var rubberRaw = 0f
    private var rubberGeneration = 0

    internal var modalPresentationRequested by mutableStateOf(initiallyVisible)
    internal var modalAwaitingPresentation by mutableStateOf(false)
    internal var initialPresentationPending = initiallyVisible
    internal var managedModal = false

    internal fun preparePresentation() { managedPresentation = true }
    internal fun prepareModalPresentation() {
        preparePresentation()
        managedModal = true
    }
    private fun requestPresentation() {
        initialPresentationPending = false
        modalPresentationRequested = true
        modalAwaitingPresentation = true
    }
    internal fun finishModalPresentation() {
        modalPresentationRequested = false
        modalAwaitingPresentation = false
        measurementsReady = false
    }

    /** The snap point actually reached; does not change half way through an animation. */
    public val currentSnapPoint: String? get() = anchored.settledValue.takeUnless { it == HiddenId }
    public val targetSnapPoint: String? get() = if (presentationExiting) null else anchored.targetValue.takeUnless { it == HiddenId }
    public val isAnimationRunning: Boolean get() = anchored.isAnimationRunning || (transitionsManaged && !visibility.isIdle)
    public val isVisible: Boolean get() = (!transitionsManaged || presentationActive) && (geometry?.let { anchored.offset.isFinite() && anchored.offset < it.availableHeight - 0.5f } ?: false)
    /** Continuous normalized visible height, useful for coordinated background animation. */
    public val progress: Float get() = geometry?.let { ((it.availableHeight - offsetOrHidden()) / it.availableHeight).coerceIn(0f, 1f) * if (transitionsManaged) presentationFraction else 1f } ?: 0f

    internal fun offsetOrHidden(): Float = anchored.offset.takeIf { it.isFinite() } ?: (geometry?.availableHeight ?: 0f)

    internal fun updateGeometry(next: SheetGeometry, dismissOnDrag: Boolean, layoutReady: Boolean = true) {
        measurementsReady = layoutReady
        val firstGeometry = geometry == null
        if (next == geometry && dragDismiss == dismissOnDrag) return
        dragDismiss = dismissOnDrag
        geometry = next
        val desired = next.canonical(anchored.targetValue)
        val target = when {
            firstGeometry && managedPresentation -> HiddenId
            desired == HiddenId -> HiddenId
            desired in next.offsets -> desired
            else -> next.smallest
        }
        updateAnchors(target)
    }

    private fun updateAnchors(target: String = anchored.targetValue) {
        val g = geometry ?: return
        val includeHidden = dragDismiss || programmaticHide || target == HiddenId
        anchored.updateAnchors(DraggableAnchors {
            g.offsets.forEach { (id, offset) -> id at offset }
            if (includeHidden) HiddenId at g.availableHeight
        }, newTarget = target)
    }

    private suspend fun ready(): SheetGeometry {
        val result = snapshotFlow {
            Pair(geometry.takeIf { measurementsReady }, managedModal && !modalPresentationRequested)
        }.first { it.first != null || it.second }
        if (result.second) throw CancellationException("Modal presentation was cancelled before layout")
        return result.first!!
    }

    /** Animates to a named snap point, entering first if hidden. Suspends until complete. */
    public suspend fun snapTo(snapPoint: String) = operations.mutate {
        moveTo(snapPoint, immediate = false)
    }

    /** Moves immediately, skipping both snap and entrance animations. */
    public suspend fun jumpTo(snapPoint: String) = operations.mutate {
        moveTo(snapPoint, immediate = true)
    }

    private suspend fun moveTo(snapPoint: String, immediate: Boolean) {
        requestPresentation()
        val g = ready()
        val target = g.canonical(snapPoint)
        require(target in g.offsets) { "Unknown snap point '$snapPoint'. Available: ${g.offsets.keys + g.aliases.keys}" }
        val entering = transitionsManaged && (!presentationActive ||
            (anchored.settledValue == HiddenId && !anchored.isAnimationRunning))
        if (entering && visibility.currentState == EnterExitState.Visible) {
            visibility = MutableTransitionState(EnterExitState.PreEnter)
            presentationFraction = 0f
        }
        clearRubber()
        programmaticHide = false
        lastVisibleSnapPoint = target
        updateAnchors()
        if (transitionsManaged && !entering && !immediate) visibility.targetState = EnterExitState.Visible
        if (immediate || entering) anchored.snapTo(target)
        else anchored.animateTo(target, snapAnimationSpec)
        hasPresented = true
        if (transitionsManaged) {
            if (immediate) {
                visibility = MutableTransitionState(EnterExitState.Visible)
                presentationFraction = 1f
            } else {
                visibility.targetState = EnterExitState.Visible
            }
        }
        modalAwaitingPresentation = false
        updateAnchors()
        if (transitionsManaged && !immediate) awaitPresentation(EnterExitState.Visible)
    }

    /** Show a mounted sheet again, restoring its last requested snap point by default. */
    public suspend fun show(snapPoint: String? = null) = operations.mutate {
        requestPresentation()
        val g = ready()
        moveTo(snapPoint ?: g.canonical(lastVisibleSnapPoint).takeIf { it in g.offsets } ?: g.smallest, immediate = false)
    }

    public suspend fun expand() = operations.mutate {
        requestPresentation(); moveTo(ready().largest, immediate = false)
    }
    public suspend fun collapse() = operations.mutate {
        requestPresentation(); moveTo(ready().smallest, immediate = false)
    }

    /** Programmatic dismissal uses the composable's exit transition and waits for it to finish. */
    public suspend fun hide() = operations.mutate {
        if (managedModal && !modalPresentationRequested) return@mutate
        if (managedModal && !measurementsReady) {
            initialPresentationPending = false
            finishModalPresentation()
            return@mutate
        }
        ready()
        modalAwaitingPresentation = false
        if (currentSnapPoint == null && !isVisible && !isAnimationRunning) return@mutate
        (targetSnapPoint ?: currentSnapPoint)?.let { lastVisibleSnapPoint = it }
        clearRubber()
        programmaticHide = true
        updateAnchors()
        try {
            if (transitionsManaged) {
                visibility.targetState = EnterExitState.PostExit
                awaitPresentation(EnterExitState.PostExit)
                anchored.snapTo(HiddenId)
            } else anchored.animateTo(HiddenId, snapAnimationSpec)
        } finally { programmaticHide = false }
    }

    internal suspend fun enter() = show()

    internal fun consumeDrag(delta: Float): Float {
        if (geometry == null || anchored.offset.isNaN()) return 0f
        beginDrag()
        val afterRubber = consumeRubber(delta)
        val consumed = if (afterRubber == 0f) 0f else anchored.dispatchRawDelta(afterRubber)
        val edgeConsumed = absorbEdge(afterRubber - consumed)
        return (delta - afterRubber) + consumed + edgeConsumed
    }

    /** Nested content must receive the remainder at a sheet bound, without sheet rubber-band. */
    internal fun consumeNestedDrag(delta: Float): Float {
        if (!delta.isFinite() || delta == 0f || geometry == null || !anchored.offset.isFinite()) return 0f
        val consumed = anchored.dispatchRawDelta(delta)
        if (consumed != 0f) beginDrag()
        return consumed
    }

    internal fun canConsumeNestedDrag(delta: Float): Boolean {
        if (delta == 0f || !anchored.offset.isFinite()) return false
        return if (delta < 0f) anchored.offset > anchored.anchors.minPosition()
        else anchored.offset < anchored.anchors.maxPosition()
    }

    /** Invalidates an in-flight edge spring so the finger keeps the current visual offset. */
    internal fun catchExitingPresentation() {
        if (presentationExiting) visibility.targetState = EnterExitState.Visible
    }

    internal fun beginDrag() {
        rubberGeneration++
        // A finger can catch an exiting sheet. The pending hide must not dismiss it afterward.
        catchExitingPresentation()
        if (!dragInProgressState.value) dragInProgressState.value = true
    }

    internal fun endDrag() {
        if (dragInProgressState.value) dragInProgressState.value = false
    }

    /** Consumes [delta] against an existing rubber band. Returns the leftover for the anchors. */
    internal fun consumeRubber(delta: Float): Float {
        if (rubberRaw == 0f) return delta
        val next = rubberRaw + delta
        val crossedZero = (rubberRaw > 0f && next < 0f) || (rubberRaw < 0f && next > 0f)
        if (!crossedZero) {
            setRubberRaw(next)
            return 0f
        }
        setRubberRaw(0f)
        return next
    }

    /** Resists leftover drag past the top, or past the bottom when the sheet cannot dismiss. */
    internal fun absorbEdge(remainder: Float): Float {
        if (remainder == 0f || !canRubberBand(remainder)) return 0f
        setRubberRaw(rubberRaw + remainder)
        return remainder
    }

    internal fun clearRubber() {
        rubberGeneration++
        rubberRaw = 0f
        rubberTranslationState.floatValue = 0f
    }

    internal fun edgeReleaseSettles(velocity: Float, velocityThreshold: Float): Boolean =
        rubberReleaseSettlesAnchor(rubberTranslation, velocity, velocityThreshold)

    internal suspend fun finishGesture(velocity: Float, velocityThreshold: Float) {
        endDrag()
        if (edgeReleaseSettles(velocity, velocityThreshold)) {
            clearRubber()
            settle(velocity, velocityThreshold)
        } else {
            animateRubberHome(velocity)
        }
    }

    internal suspend fun animateRubberHome(velocity: Float) {
        val generation = rubberGeneration
        val dimension = geometry?.availableHeight ?: 0f
        val anim = Animatable(rubberTranslation)
        try {
            anim.animateTo(0f, defaultSheetSpring(), initialVelocity = velocity) {
                if (generation != rubberGeneration) throw CancellationException()
                rubberTranslationState.floatValue = value
                rubberRaw = if (dimension > 0f) rubberRawFromVisual(value, dimension) else 0f
            }
        } catch (handedOff: CancellationException) {
            if (generation != rubberGeneration) return
            throw handedOff
        }
        if (generation != rubberGeneration) return
        rubberRaw = 0f
        rubberTranslationState.floatValue = 0f
    }

    private fun setRubberRaw(raw: Float) {
        rubberRaw = raw
        val dimension = geometry?.availableHeight ?: 0f
        rubberTranslationState.floatValue = rubberBand(raw, dimension)
    }

    private fun canRubberBand(remainder: Float): Boolean {
        val g = geometry ?: return false
        if (g.availableHeight <= 0f) return false
        val offset = offsetOrHidden()
        val min = g.offsets.values.minOrNull() ?: return false
        if (remainder < 0f && offset <= min + 0.5f) return true
        if (remainder > 0f && !dragDismiss) {
            val max = g.offsets.values.maxOrNull() ?: return false
            if (offset >= max - 0.5f) return true
        }
        return false
    }

    internal suspend fun settle(velocity: Float, velocityThreshold: Float) {
        val g = geometry ?: return
        val anchors = g.offsets.toMutableMap().apply { if (dragDismiss) put(HiddenId, g.availableHeight) }
        val target = settleTarget(offsetOrHidden(), velocity, anchors, velocityThreshold)
        // dispatchRawDelta records a velocity of 0, so the finger's fling has to be applied here.
        animateToAnchor(target, velocity)
    }

    private suspend fun animateToAnchor(target: String, velocity: Float) {
        endDrag()
        anchored.anchoredDrag(target) { dragAnchors, latestTarget ->
            val drag = this
            val targetOffset = dragAnchors.positionOf(latestTarget)
            val current = anchored.offset
            if (targetOffset.isNaN() || current.isNaN() || current == targetOffset) return@anchoredDrag
            val anim = Animatable(current)
            anim.animateTo(targetOffset, snapAnimationSpec, initialVelocity = velocity) {
                drag.dragTo(value, this.velocity)
            }
        }
    }
}

/** The default settle matches an iOS sheet: short, critically damped, and it keeps the flick. */
@Composable
public fun rememberSheetState(
    initialSnapPoint: String = "expanded",
    initiallyVisible: Boolean = true,
): SheetState {
    val saver = Saver<SheetState, List<String>>(
        save = {
            val visibleId = if (it.isAnimationRunning) it.targetSnapPoint
                else it.currentSnapPoint ?: it.lastVisibleSnapPoint.takeIf { _ -> it.isVisible }
            listOf(visibleId ?: it.lastVisibleSnapPoint, (visibleId != null).toString())
        },
        restore = { SheetState(it[0], initiallyVisible = it[1].toBoolean()) },
    )
    return rememberSaveable(saver = saver) { SheetState(initialSnapPoint, initiallyVisible = initiallyVisible) }
}

