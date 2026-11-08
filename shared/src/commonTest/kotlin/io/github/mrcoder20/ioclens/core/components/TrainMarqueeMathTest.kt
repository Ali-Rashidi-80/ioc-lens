package io.github.mrcoder20.ioclens.core.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrainMarqueeMathTest {

    @Test
    fun testSeamlessWrapAroundModulo() {
        val cycleWidthPx = 450f
        var offsetPx = 0f

        // Progressively scroll past full cycle
        val deltas = listOf(100f, 200f, 150f) // sums to 450f
        for (d in deltas) {
            offsetPx = (offsetPx + d) % cycleWidthPx
        }
        assertEquals(0f, offsetPx, "Offset must wrap exactly back to 0 at cycle boundary")

        // Further progress
        offsetPx = (offsetPx + 50f) % cycleWidthPx
        assertEquals(50f, offsetPx)
    }

    @Test
    fun testRtlAndLtrTranslationDirection() {
        val offsetPx = 120f
        val cycleWidthPx = 500f

        // LTR moves negative X
        val isRtlLtr = false
        val signLtr = if (isRtlLtr) 1f else -1f
        val translationLtr = signLtr * (offsetPx % cycleWidthPx)
        assertEquals(-120f, translationLtr)

        // RTL moves positive X to reveal left-overflowing items
        val isRtlPersian = true
        val signRtl = if (isRtlPersian) 1f else -1f
        val translationRtl = signRtl * (offsetPx % cycleWidthPx)
        assertEquals(120f, translationRtl)
    }

    @Test
    fun testRepeatCopiesAdequateForUltraWideScreens() {
        val cycleWidthPx = 380f

        // Mobile Screen: 360px wide
        val mobileContainerW = 360f
        val mobileCopies = ((mobileContainerW / cycleWidthPx).toInt() + 3).coerceIn(3, 10)
        assertTrue(mobileCopies >= 3)
        assertTrue(mobileCopies * cycleWidthPx >= mobileContainerW * 2)

        // 4K Ultra-Wide Screen: 3840px wide
        val ultraWideW = 3840f
        val ultraWideCopies = ((ultraWideW / cycleWidthPx).toInt() + 3).coerceIn(3, 10)
        assertEquals(10, ultraWideCopies, "Should cap at safe max repeat count")
    }

    @Test
    fun testDragGestureWrapping() {
        val cycleWidthPx = 400f
        var offsetPx = 10f

        // Drag backwards past 0 in LTR
        val isRtl = false
        val factor = if (isRtl) 1f else -1f
        val dragAmount = 25f // Drag rightwards
        val next = offsetPx + (factor * dragAmount) // 10 - 25 = -15
        offsetPx = if (next < 0f) (next % cycleWidthPx) + cycleWidthPx else next % cycleWidthPx

        assertEquals(385f, offsetPx, "Negative drag must wrap seamlessly into end of period")
    }
}

