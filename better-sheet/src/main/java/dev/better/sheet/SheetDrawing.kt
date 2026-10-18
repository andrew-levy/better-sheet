package dev.better.sheet

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath

/**
 * Clip pixels in the draw pass, not the RenderNode layer. Animated layer outlines can leave
 * a sheet's display list empty on some renderers (see PR #16). Keep transforms on layers,
 * but use this one clipping path for surface, stack, and viewport boundaries.
 * The provider is read inside the cache so animated shapes invalidate drawing directly.
 */
internal fun Modifier.sheetDrawClip(shape: () -> Shape?): Modifier = drawWithCache {
    val path = shape()?.let { value ->
        Path().apply { addOutline(value.createOutline(size, layoutDirection, this@drawWithCache)) }
    }
    onDrawWithContent {
        if (path == null) drawContent() else clipPath(path) { this@onDrawWithContent.drawContent() }
    }
}

/** Reserved navigation pixels belong to the surface, never to a child's elevation shadow. */
internal fun Modifier.sheetNavigationCover(color: Color, heightPx: Float): Modifier = drawWithContent {
    drawContent()
    val height = heightPx.coerceIn(0f, size.height)
    if (height > 0f) drawRect(color, Offset(0f, size.height - height), Size(size.width, height))
}
