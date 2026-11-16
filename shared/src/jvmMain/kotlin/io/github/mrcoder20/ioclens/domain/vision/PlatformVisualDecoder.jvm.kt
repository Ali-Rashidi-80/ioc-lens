package io.github.mrcoder20.ioclens.domain.vision

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.GenericMultipleBarcodeReader
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

actual class PlatformVisualDecoder actual constructor() {

    private val hints = mapOf<DecodeHintType, Any>(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.ALSO_INVERTED to true,
        DecodeHintType.CHARACTER_SET to "UTF-8"
    )

    actual fun decodeBarcodeFromBytes(imageBytes: ByteArray): List<BarcodePayload> {
        if (imageBytes.isEmpty()) return emptyList()
        return try {
            val bufferedImage = ImageIO.read(ByteArrayInputStream(imageBytes)) ?: return emptyList()
            decodeBufferedImage(bufferedImage)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun decodeFromClipboard(): List<BarcodePayload> {
        return try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            if (clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
                val img = clipboard.getData(DataFlavor.imageFlavor) as? Image ?: return emptyList()
                val buffered = toBufferedImage(img)
                decodeBufferedImage(buffered)
            } else if (clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
                val files = clipboard.getData(DataFlavor.javaFileListFlavor) as? List<*> ?: return emptyList()
                for (item in files) {
                    val file = item as? File ?: continue
                    if (isImageFile(file)) {
                        val img = ImageIO.read(file) ?: continue
                        val res = decodeBufferedImage(img)
                        if (res.isNotEmpty()) return res
                    }
                }
                emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            try {
                val singleReader = MultiFormatReader()
                val result = singleReader.decode(bitmap, hints)
                val type = mapZxingFormat(result.barcodeFormat)
                val payload = QuishingAnalyzer.analyze(result.text, type)
                results.add(payload)
            } catch (ex: Exception) {
                // No barcode detected in frame
            }
        }

        return results
    }

    private fun toBufferedImage(img: Image): BufferedImage {
        if (img is BufferedImage) return img
        val width = img.getWidth(null).coerceAtLeast(1)
        val height = img.getHeight(null).coerceAtLeast(1)
        val bimage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val bGr = bimage.createGraphics()
        bGr.drawImage(img, 0, 0, null)
        bGr.dispose()
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
        g2d.drawImage(image.getScaledInstance(newW, newH, Image.SCALE_SMOOTH), 0, 0, null)
        g2d.dispose()
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
