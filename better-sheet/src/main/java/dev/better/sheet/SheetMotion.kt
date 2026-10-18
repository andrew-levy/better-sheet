package dev.better.sheet

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sign

/**
 * iOS sheet settle: short and critically damped.
 * A half-second system spring feels late next to a flicked sheet, which is mostly done by ~300ms.
 */
internal const val IosSheetSpringDurationSeconds = 0.3
internal const val IosSheetSpringDampingRatio = 1f

/** stiffness = (2π / duration)^2, mass = 1. */
internal val IosSheetSpringStiffness: Float =
    (2.0 * PI / IosSheetSpringDurationSeconds).let { (it * it).toFloat() }

internal fun <T> defaultSheetSpring(): SpringSpec<T> = spring(
    dampingRatio = IosSheetSpringDampingRatio,
    stiffness = IosSheetSpringStiffness,
)

/** UIScrollView rubber-band coefficient used by iOS sheets. */
internal const val RubberBandCoefficient = 0.55f

/**
 * Resisted distance for a raw finger [overscroll] past a bound.
 * `dimension` is the sheet's available height, matching UIScrollView.
 */
internal fun rubberBand(
    overscroll: Float,
    dimension: Float,
    coefficient: Float = RubberBandCoefficient,
): Float {
    if (overscroll == 0f || dimension <= 0f) return 0f
    val distance = abs(overscroll)
    val resisted = (1f - 1f / (distance * coefficient / dimension + 1f)) * dimension
    return sign(overscroll) * resisted
}

/** Inverse of [rubberBand], so a spring on the visual offset can be dragged again. */
internal fun rubberRawFromVisual(
    visual: Float,
    dimension: Float,
    coefficient: Float = RubberBandCoefficient,
): Float {
    if (visual == 0f || dimension <= 0f) return 0f
    val y = abs(visual).coerceAtMost(dimension * 0.999f)
    val raw = dimension / coefficient * (1f / (1f - y / dimension) - 1f)
    return sign(visual) * raw
}

/**
 * An inward flick past [threshold] leaves the edge and settles to the next detent.
 * An outward or gentle release stays on the edge and springs the rubber band home.
 */
internal fun rubberReleaseSettlesAnchor(translation: Float, velocity: Float, threshold: Float): Boolean {
    if (translation == 0f) return true
    return velocity != 0f && sign(velocity) != sign(translation) && abs(velocity) >= threshold
}

/** Upward rubber band, in px. The sheet grows by this much; it does not lift its bottom edge. */
internal fun upwardStretch(rubberTranslation: Float): Float =
    if (rubberTranslation < 0f) -rubberTranslation else 0f

/** Downward rubber band, in px. The whole sheet, including its bottom edge, follows this. */
internal fun downwardShift(rubberTranslation: Float): Float =
    if (rubberTranslation > 0f) rubberTranslation else 0f

/**
 * True once a dismiss is far enough along that the scrim is effectively gone.
 * Finger-down drags stay interactive so the sheet can still be caught.
 */
internal fun releasesBackgroundPointer(
    targetHidden: Boolean,
    dragInProgress: Boolean,
    visibleHeight: Float,
    slop: Float,
): Boolean = targetHidden && !dragInProgress && visibleHeight <= slop

/**
 * Visibility and anchor completion must agree before notifying dismissal. The transition
 * can finish a frame before hide() snaps the anchor; that frame is not a second dismissal.
 */
internal fun SheetState.releaseDismissed(): Boolean = hasPresented && !modalAwaitingPresentation &&
    targetSnapPoint == null && currentSnapPoint == null && !isVisible && !isAnimationRunning && !dragInProgress

/** 0 at hidden, 1 at the lowest detent, held at 1 above that. Ignored when [followsDrag] is false. */
internal fun scrimDragFraction(visibleHeight: Float, smallestDetentHeight: Float, followsDrag: Boolean): Float {
    if (!followsDrag) return 1f
    if (smallestDetentHeight <= 0f) return 1f
    return (visibleHeight / smallestDetentHeight).coerceIn(0f, 1f)
}

