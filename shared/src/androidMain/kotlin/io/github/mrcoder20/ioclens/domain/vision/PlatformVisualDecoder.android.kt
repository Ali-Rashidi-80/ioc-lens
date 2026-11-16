package io.github.mrcoder20.ioclens.domain.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer

actual class PlatformVisualDecoder actual constructor() {

    private val hints = mapOf<DecodeHintType, Any>(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.ALSO_INVERTED to true,
        DecodeHintType.CHARACTER_SET to "UTF-8"
    )

    private val multiFormatReader = MultiFormatReader()

    // Reusable byte array buffer for zero-allocation CameraX frame processing
    private var reusableBuffer: ByteArray? = null

    actual fun decodeBarcodeFromBytes(imageBytes: ByteArray): List<BarcodePayload> {
        if (imageBytes.isEmpty()) return emptyList()
        return try {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return emptyList()
            decodeBitmap(bitmap)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun decodeBitmap(bitmap: Bitmap): List<BarcodePayload> {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        return try {
            val result = multiFormatReader.decode(binaryBitmap, hints)
            val type = mapZxingFormat(result.barcodeFormat)
            listOf(QuishingAnalyzer.analyze(result.text, type))
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun decodeImageProxy(imageProxy: ImageProxy): List<BarcodePayload> {
        return try {
            val plane = imageProxy.planes[0]
            val buffer = plane.buffer
            val rowStride = plane.rowStride
            val width = imageProxy.width
            val height = imageProxy.height

            val requiredSize = width * height
            var yuvBytes = reusableBuffer
            if (yuvBytes == null || yuvBytes.size < requiredSize) {
                yuvBytes = ByteArray(requiredSize)
                reusableBuffer = yuvBytes
            }

            buffer.rewind()
            if (rowStride == width) {
                buffer.get(yuvBytes, 0, requiredSize)
            } else {
                for (row in 0 until height) {
                    buffer.position(row * rowStride)
                    buffer.get(yuvBytes, row * width, width)
                }
            }

            val source = PlanarYUVLuminanceSource(yuvBytes, width, height, 0, 0, width, height, false)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = multiFormatReader.decode(binaryBitmap, hints)
            val type = mapZxingFormat(result.barcodeFormat)
            listOf(QuishingAnalyzer.analyze(result.text, type))
        } catch (e: Exception) {
            emptyList()
        }
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
