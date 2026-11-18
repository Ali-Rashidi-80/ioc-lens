package io.github.mrcoder20.ioclens.core.components

internal object MarqueeMath {
    fun computeOverflowPx(textWidthPx: Float, containerWidthPx: Float): Float =
        (textWidthPx - containerWidthPx).coerceAtLeast(0f)

    fun computeControlledDurationMs(overflowPx: Float, pxPerSec: Float): Int {
        if (overflowPx <= 0f || pxPerSec <= 0f) return 600
        return ((overflowPx / pxPerSec) * 1000f).toInt().coerceIn(600, 6000)
    }

    fun computeReturnDurationMs(durationMs: Int): Int =
        (durationMs * 0.7f).toInt().coerceIn(400, 4000)

    fun computeControlledTargetTranslation(overflowPx: Float, isRtl: Boolean): Float =
        if (isRtl) overflowPx else -overflowPx

    fun advanceCycleOffset(offsetPx: Float, deltaPx: Float, cycleWidthPx: Float): Float {
        if (cycleWidthPx <= 0f) return 0f
        return (offsetPx + deltaPx) % cycleWidthPx
    }

    fun computeTrainTranslation(offsetPx: Float, cycleWidthPx: Float, isRtl: Boolean): Float {
        val sign = if (isRtl) 1f else -1f
        val safeCycle = if (cycleWidthPx > 0f) cycleWidthPx else 1f
        return sign * (offsetPx % safeCycle)
    }

    fun computeRepeatCopies(containerWidthPx: Float, cycleWidthPx: Float): Int =
        if (cycleWidthPx > 0f) {
            ((containerWidthPx / cycleWidthPx).toInt() + 3).coerceIn(3, 10)
        } else {
            4
        }

    fun wrapDragOffset(offsetPx: Float, dragAmount: Float, cycleWidthPx: Float, isRtl: Boolean): Float {
        if (cycleWidthPx <= 0f) return offsetPx
        val factor = if (isRtl) 1f else -1f
        val next = offsetPx + (factor * dragAmount)
        return if (next < 0f) (next % cycleWidthPx) + cycleWidthPx else next % cycleWidthPx
    }
}
