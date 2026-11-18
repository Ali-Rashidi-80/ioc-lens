package io.github.mrcoder20.ioclens.core.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrainMarqueeMathTest {

    @Test
    fun testSeamlessWrapAroundModulo() {
        val cycleWidthPx = 450f
        var offsetPx = 0f

        val deltas = listOf(100f, 200f, 150f)
        for (d in deltas) {
            offsetPx = MarqueeMath.advanceCycleOffset(offsetPx, d, cycleWidthPx)
        }
        assertEquals(0f, offsetPx, "Offset must wrap exactly back to 0 at cycle boundary")

        offsetPx = MarqueeMath.advanceCycleOffset(offsetPx, 50f, cycleWidthPx)
        assertEquals(50f, offsetPx)
    }

    @Test
    fun testRtlAndLtrTranslationDirection() {
        val offsetPx = 120f
        val cycleWidthPx = 500f

        val translationLtr = MarqueeMath.computeTrainTranslation(offsetPx, cycleWidthPx, isRtl = false)
        assertEquals(-120f, translationLtr)

        val translationRtl = MarqueeMath.computeTrainTranslation(offsetPx, cycleWidthPx, isRtl = true)
        assertEquals(120f, translationRtl)
    }

    @Test
    fun testRepeatCopiesAdequateForUltraWideScreens() {
        val cycleWidthPx = 380f

        val mobileContainerW = 360f
        val mobileCopies = MarqueeMath.computeRepeatCopies(mobileContainerW, cycleWidthPx)
        assertTrue(mobileCopies >= 3)
        assertTrue(mobileCopies * cycleWidthPx >= mobileContainerW * 2)

        val ultraWideW = 3840f
        val ultraWideCopies = MarqueeMath.computeRepeatCopies(ultraWideW, cycleWidthPx)
        assertEquals(10, ultraWideCopies, "Should cap at safe max repeat count")
    }

    @Test
    fun testDragGestureWrapping() {
        val cycleWidthPx = 400f
        val wrapped = MarqueeMath.wrapDragOffset(offsetPx = 10f, dragAmount = 25f, cycleWidthPx = cycleWidthPx, isRtl = false)
        assertEquals(385f, wrapped, "Negative drag must wrap seamlessly into end of period")

        val rtlWrapped = MarqueeMath.wrapDragOffset(offsetPx = 390f, dragAmount = 25f, cycleWidthPx = cycleWidthPx, isRtl = true)
        assertEquals(15f, rtlWrapped, "RTL drag past cycle boundary must wrap seamlessly")
    }

    @Test
    fun testControlledMarqueeOverflowCalculation() {
        assertEquals(120f, MarqueeMath.computeOverflowPx(320f, 200f))
        assertEquals(0f, MarqueeMath.computeOverflowPx(150f, 200f))
    }

    @Test
    fun testControlledMarqueeTranslationVectors() {
        val overflowPx = 80f
        assertEquals(-80f, MarqueeMath.computeControlledTargetTranslation(overflowPx, isRtl = false))
        assertEquals(80f, MarqueeMath.computeControlledTargetTranslation(overflowPx, isRtl = true))
    }

    @Test
    fun testControlledMarqueeDurationBoundaries() {
        val pxPerSec = 64f

        assertEquals(600, MarqueeMath.computeControlledDurationMs(20f, pxPerSec))
        assertEquals(2500, MarqueeMath.computeControlledDurationMs(160f, pxPerSec))
        assertEquals(6000, MarqueeMath.computeControlledDurationMs(600f, pxPerSec))
        assertEquals(1750, MarqueeMath.computeReturnDurationMs(2500))
    }
}

