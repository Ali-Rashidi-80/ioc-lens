package io.github.mrcoder20.ioclens.domain.vision

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.ReaderException
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.GenericMultipleBarcodeReader
import io.github.mrcoder20.ioclens.logDiagnostic
import io.github.mrcoder20.ioclens.presentation.vision.DesktopDragAndDropCoordinator
import java.awt.Image
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max

private const val JVM_MAX_VISION_IMAGE_BYTES = 20L * 1024L * 1024L
private const val JVM_MAX_IMAGE_DIMENSION = 12_000
private const val JVM_MAX_IMAGE_PIXELS = 36_000_000L

actual class PlatformVisualDecoder actual constructor() {

    private val hints = mapOf<DecodeHintType, Any>(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.ALSO_INVERTED to true,
        DecodeHintType.CHARACTER_SET to "UTF-8"
    )

    actual fun decodeBarcodeFromBytes(imageBytes: ByteArray): List<BarcodePayload> {
        if (imageBytes.isEmpty() || imageBytes.size.toLong() > JVM_MAX_VISION_IMAGE_BYTES) {
            return emptyList()
        }
        return try {
            val bufferedImage = decodeSafeBoundedBufferedImage(imageBytes) ?: return emptyList()
            decodeBufferedImage(bufferedImage)
        } catch (e: Exception) {
            logDiagnostic("JvmVisualDecoder", "Failed to decode barcode image bytes", e)
            emptyList()
        }
    }

    private fun decodeSafeBoundedBufferedImage(imageBytes: ByteArray): BufferedImage? {
        ImageIO.createImageInputStream(ByteArrayInputStream(imageBytes))?.use { iis ->
            val readers = ImageIO.getImageReaders(iis)
            if (!readers.hasNext()) return null
            val reader = readers.next()
            try {
                reader.input = iis
                val width = reader.getWidth(0)
                val height = reader.getHeight(0)
                if (width <= 0 || height <= 0 ||
                    width > JVM_MAX_IMAGE_DIMENSION || height > JVM_MAX_IMAGE_DIMENSION ||
                    width.toLong() * height.toLong() > JVM_MAX_IMAGE_PIXELS
                ) {
                    logDiagnostic("JvmVisualDecoder", "Rejected oversized image raster: ${width}x$height")
                    return null
                }
                val param = reader.defaultReadParam
                val maxSide = max(width, height)
                if (maxSide > 1920) {
                    val step = (maxSide / 1920).coerceAtLeast(1)
                    param.setSourceSubsampling(step, step, 0, 0)
                }
                return reader.read(0, param)
            } finally {
                reader.dispose()
            }
        }
        return null
    }

    fun decodeFromClipboard(): List<BarcodePayload> {
        return try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            if (clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
                val img = clipboard.getData(DataFlavor.imageFlavor) as? Image ?: return emptyList()
                val buffered = toBufferedImage(img) ?: return emptyList()
                decodeBufferedImage(buffered)
            } else if (clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
                val files = clipboard.getData(DataFlavor.javaFileListFlavor) as? List<*> ?: return emptyList()
                for (item in files) {
                    val file = item as? File ?: continue
                    if (!DesktopDragAndDropCoordinator.isUnsafeRemotePath(file.path) &&
                        isImageFile(file) &&
                        file.isFile &&
                        file.length() in 1L..JVM_MAX_VISION_IMAGE_BYTES
                    ) {
                        val res = decodeBarcodeFromBytes(file.readBytes())
                        if (res.isNotEmpty()) return res
                    }
                }
                emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            logDiagnostic("JvmVisualDecoder", "Failed to read barcode from clipboard", e)
            emptyList()
        }
    }

    fun decodeBufferedImage(image: BufferedImage): List<BarcodePayload> {
        val processedImage = downsampleIfNeeded(image)
        val source = BufferedImageLuminanceSource(processedImage)
        val bitmap = BinaryBitmap(HybridBinarizer(source))

        val results = mutableListOf<BarcodePayload>()
        val multiReader = GenericMultipleBarcodeReader(MultiFormatReader())

        try {
            val decodedList = multiReader.decodeMultiple(bitmap, hints)
            for (result in decodedList) {
                val type = mapZxingFormat(result.barcodeFormat)
                val payload = QuishingAnalyzer.analyze(result.text, type)
                results.add(payload)
            }
        } catch (_: ReaderException) {
            try {
                val singleReader = MultiFormatReader()
                val result = singleReader.decode(bitmap, hints)
                val type = mapZxingFormat(result.barcodeFormat)
                val payload = QuishingAnalyzer.analyze(result.text, type)
                results.add(payload)
            } catch (_: ReaderException) {
                // No barcode detected in frame
            } catch (ex: Exception) {
                logDiagnostic("JvmVisualDecoder", "Unexpected single-frame decode failure", ex)
            }
        } catch (e: Exception) {
            logDiagnostic("JvmVisualDecoder", "Unexpected multi-frame decode failure", e)
        }

        return results
    }

    private fun toBufferedImage(img: Image): BufferedImage? {
        if (img is BufferedImage) {
            if (img.width <= 0 || img.height <= 0 ||
                img.width > JVM_MAX_IMAGE_DIMENSION || img.height > JVM_MAX_IMAGE_DIMENSION ||
                img.width.toLong() * img.height.toLong() > JVM_MAX_IMAGE_PIXELS
            ) {
                return null
            }
            return img
        }
        val width = img.getWidth(null)
        val height = img.getHeight(null)
        if (width <= 0 || height <= 0 ||
            width > JVM_MAX_IMAGE_DIMENSION || height > JVM_MAX_IMAGE_DIMENSION ||
            width.toLong() * height.toLong() > JVM_MAX_IMAGE_PIXELS
        ) {
            return null
        }
        val bimage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val bGr = bimage.createGraphics()
        try {
            bGr.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            bGr.drawImage(img, 0, 0, null)
        } finally {
            bGr.dispose()
        }
        return bimage
    }

    private fun downsampleIfNeeded(image: BufferedImage): BufferedImage {
        val maxDimension = 1920
        val w = image.width
        val h = image.height
        if (w <= maxDimension && h <= maxDimension) return image

        val ratio = if (w > h) maxDimension.toDouble() / w else maxDimension.toDouble() / h
        val newW = (w * ratio).toInt().coerceAtLeast(1)
        val newH = (h * ratio).toInt().coerceAtLeast(1)

        val scaled = BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB)
        val g2d = scaled.createGraphics()
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g2d.drawImage(image, 0, 0, newW, newH, null)
        } finally {
            g2d.dispose()
        }
        return scaled
    }

    private fun isImageFile(file: File): Boolean {
        val name = file.name.lowercase()
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                name.endsWith(".webp") || name.endsWith(".bmp")
    }

    private fun mapZxingFormat(format: BarcodeFormat): BarcodeType {
        return when (format) {
            BarcodeFormat.QR_CODE -> BarcodeType.QR_CODE
            BarcodeFormat.DATA_MATRIX -> BarcodeType.DATA_MATRIX
            BarcodeFormat.AZTEC -> BarcodeType.AZTEC
            BarcodeFormat.PDF_417 -> BarcodeType.PDF_417
            BarcodeFormat.CODE_128 -> BarcodeType.CODE_128
            BarcodeFormat.CODE_39 -> BarcodeType.CODE_39
            else -> BarcodeType.OTHER
        }
    }
}
