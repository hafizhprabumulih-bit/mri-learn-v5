package com.example

import com.example.ui.path.PathGeometry
import com.example.ui.path.xFractionFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PathGeometryTest {

    @Test
    fun zigZagPatternCyclesCenterRightCenterLeft() {
        val pattern = (0 until 8).map { xFractionFor(it) }
        assertEquals(
            listOf(
                PathGeometry.CENTER_X,
                PathGeometry.RIGHT_X,
                PathGeometry.CENTER_X,
                PathGeometry.LEFT_X,
                PathGeometry.CENTER_X,
                PathGeometry.RIGHT_X,
                PathGeometry.CENTER_X,
                PathGeometry.LEFT_X
            ),
            pattern
        )
    }

    @Test
    fun xFractionStaysWithinSafeViewportBoundsAndHandlesNegativeIndices() {
        for (i in -8..24) {
            val x = xFractionFor(i)
            assertTrue("xFraction($i)=$x harus berada dalam rentang aman 0.2..0.8", x in 0.2f..0.8f)
        }
        assertEquals(PathGeometry.LEFT_X, xFractionFor(-1))
        assertEquals(PathGeometry.CENTER_X, xFractionFor(-2))
        assertEquals(PathGeometry.RIGHT_X, xFractionFor(-3))
        assertEquals(PathGeometry.CENTER_X, xFractionFor(-4))
    }
}
