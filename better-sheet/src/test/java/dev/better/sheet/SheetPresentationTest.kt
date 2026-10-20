package dev.better.sheet

import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Test

class SheetPresentationTest {
    @Test fun shortSheetsAndHiddenSheetsStayFloating() {
        for (height in listOf(-10f, 0f, 250f, 600f)) {
            assertEquals(0f, trayMorphFraction(height, 1000f, .6f), 0f)
        }
    }
    @Test fun finalStretchMorphsContinuouslyAndClampsOvershoot() {
        assertEquals(.5f, trayMorphFraction(800f, 1000f, .6f), .0001f)
        assertEquals(1f, trayMorphFraction(1000f, 1000f, .6f), 0f)
        assertEquals(1f, trayMorphFraction(1100f, 1000f, .6f), 0f)
        var previous = 0f
        for (height in 600..1000) {
            val next = trayMorphFraction(height.toFloat(), 1000f, .6f)
            assertTrue(next >= previous)
            assertTrue(next - previous < .004f)
            previous = next
        }
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidStart() { SheetPresentation.Tray(morphStartFraction = 1f) }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeInsets() { SheetPresentation.Tray(bottomInset = (-1).dp) }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnspecifiedRadius() { SheetPresentation.Tray(cornerRadius = androidx.compose.ui.unit.Dp.Unspecified) }
}
