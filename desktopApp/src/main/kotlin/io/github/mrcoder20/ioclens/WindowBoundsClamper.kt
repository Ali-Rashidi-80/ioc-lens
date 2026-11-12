package io.github.mrcoder20.ioclens

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import java.awt.GraphicsConfiguration
import java.awt.GraphicsEnvironment
import java.awt.Insets
import java.awt.Rectangle
import java.awt.Toolkit

data class ClampedWindowBounds(
    val size: DpSize,
    val position: WindowPosition
)

object WindowBoundsClamper {
    const val PREFERRED_WIDTH_DP = 1040
    const val PREFERRED_HEIGHT_DP = 740
    const val MIN_WIDTH_DP = 380
    const val MIN_HEIGHT_DP = 520
    const val DEFAULT_SAFETY_MARGIN_DP = 16

    fun computeClampedBounds(
        screenBounds: Rectangle,
        screenInsets: Insets,
        preferredWidth: Int = PREFERRED_WIDTH_DP,
        preferredHeight: Int = PREFERRED_HEIGHT_DP,
        minWidth: Int = MIN_WIDTH_DP,
        minHeight: Int = MIN_HEIGHT_DP,
        safetyMargin: Int = DEFAULT_SAFETY_MARGIN_DP
    ): ClampedWindowBounds {
        val usableX = screenBounds.x + screenInsets.left
        val usableY = screenBounds.y + screenInsets.top
        val usableWidth = (screenBounds.width - screenInsets.left - screenInsets.right).coerceAtLeast(minWidth)
        val usableHeight = (screenBounds.height - screenInsets.top - screenInsets.bottom).coerceAtLeast(minHeight)

        val maxAllowedWidth = if (usableWidth > minWidth + safetyMargin * 2) {
            usableWidth - safetyMargin * 2
        } else {
            usableWidth
        }
        val targetWidth = preferredWidth.coerceIn(minWidth, maxAllowedWidth.coerceAtLeast(minWidth))

        val maxAllowedHeight = if (usableHeight > minHeight + safetyMargin * 2) {
            usableHeight - safetyMargin * 2
        } else {
            usableHeight
        }
        val targetHeight = preferredHeight.coerceIn(minHeight, maxAllowedHeight.coerceAtLeast(minHeight))

        val posX = usableX + (usableWidth - targetWidth) / 2
        val posY = usableY + (usableHeight - targetHeight) / 2

        return ClampedWindowBounds(
            size = DpSize(targetWidth.dp, targetHeight.dp),
            position = WindowPosition(posX.dp, posY.dp)
        )
    }

    fun resolveInitialBounds(): ClampedWindowBounds {
        return runCatching {
            if (GraphicsEnvironment.isHeadless()) {
                return@runCatching defaultBounds()
            }
            val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
            val device = ge.defaultScreenDevice ?: return@runCatching defaultBounds()
            val gc = device.defaultConfiguration ?: return@runCatching defaultBounds()
            val bounds = gc.bounds
            val insets = Toolkit.getDefaultToolkit().getScreenInsets(gc)
            computeClampedBounds(bounds, insets)
        }.getOrDefault(defaultBounds())
    }

    fun resolveRestoreBounds(gc: GraphicsConfiguration): Rectangle {
        return runCatching {
            val bounds = gc.bounds
            val insets = Toolkit.getDefaultToolkit().getScreenInsets(gc)
            val clamped = computeClampedBounds(bounds, insets)
            val width = clamped.size.width.value.toInt()
            val height = clamped.size.height.value.toInt()
            val x = (clamped.position as? WindowPosition.Absolute)?.x?.value?.toInt()
                ?: (bounds.x + (bounds.width - width) / 2)
            val y = (clamped.position as? WindowPosition.Absolute)?.y?.value?.toInt()
                ?: (bounds.y + (bounds.height - height) / 2)
            Rectangle(x, y, width, height)
        }.getOrDefault(Rectangle(0, 0, PREFERRED_WIDTH_DP, PREFERRED_HEIGHT_DP))
    }

    fun defaultBounds(): ClampedWindowBounds = ClampedWindowBounds(
        size = DpSize(PREFERRED_WIDTH_DP.dp, PREFERRED_HEIGHT_DP.dp),
        position = WindowPosition.Aligned(Alignment.Center)
    )
}
