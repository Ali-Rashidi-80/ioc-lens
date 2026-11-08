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

    @Test
    fun testControlledMarqueeOverflowCalculation() {
        val textWidth = 320f
        val containerWidth = 200f
        val detectedOverflow = (textWidth - containerWidth).coerceAtLeast(0f)
        assertEquals(120f, detectedOverflow, "Overflow must match exact difference when text exceeds container")

        val shortTextWidth = 150f
        val shortOverflow = (shortTextWidth - containerWidth).coerceAtLeast(0f)
        assertEquals(0f, shortOverflow, "Overflow must be 0 when text comfortably fits container")
    }

    @Test
    fun testControlledMarqueeTranslationVectors() {
        val overflowPx = 80f

        // In LTR, text starts at left (0). Hidden tail is on the right. Target translation is -80f.
        val isRtlLtr = false
        val targetLtr = if (isRtlLtr) overflowPx else -overflowPx
        assertEquals(-80f, targetLtr)

        // In RTL, text starts at right. Hidden tail is on the left. Target translation is +80f.
        val isRtlFa = true
        val targetRtl = if (isRtlFa) overflowPx else -overflowPx
        assertEquals(80f, targetRtl)
    }

    @Test
    fun testControlledMarqueeDurationBoundaries() {
        val velocityDpPerSec = 32f
        val density = 2f // 2 px per dp
        val pxPerSec = velocityDpPerSec * density // 64 px/sec

        // Short overflow (20px) -> duration clamped to minimum 600ms
        val shortOverflow = 20f
        val shortDurationMs = ((shortOverflow / pxPerSec) * 1000).toInt().coerceIn(600, 6000)
        assertEquals(600, shortDurationMs)

        // Moderate overflow (160px) -> 2.5 seconds (2500ms)
        val modOverflow = 160f
        val modDurationMs = ((modOverflow / pxPerSec) * 1000).toInt().coerceIn(600, 6000)
        assertEquals(2500, modDurationMs)

        // Extreme overflow (600px) -> duration clamped to maximum 6000ms
        val extremeOverflow = 600f
        val extremeDurationMs = ((extremeOverflow / pxPerSec) * 1000).toInt().coerceIn(600, 6000)
        assertEquals(6000, extremeDurationMs)
    }
}

