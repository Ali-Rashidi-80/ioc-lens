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
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import io.github.mrcoder20.ioclens.logDiagnostic

private const val ANDROID_MAX_VISION_IMAGE_BYTES = 20 * 1024 * 1024
private const val ANDROID_MAX_IMAGE_DIMENSION = 12_000
private const val ANDROID_MAX_IMAGE_PIXELS = 36_000_000L

actual class PlatformVisualDecoder actual constructor() {

    private val hints = mapOf<DecodeHintType, Any>(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.ALSO_INVERTED to true,
        DecodeHintType.CHARACTER_SET to "UTF-8"
    )

    // Reusable byte array buffer for CameraX analyzer thread
    private var reusableBuffer: ByteArray? = null

    actual fun decodeBarcodeFromBytes(imageBytes: ByteArray): List<BarcodePayload> {
        if (imageBytes.isEmpty() || imageBytes.size > ANDROID_MAX_VISION_IMAGE_BYTES) {
            return emptyList()
        }
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, boundsOptions)
            val rawW = boundsOptions.outWidth
            val rawH = boundsOptions.outHeight
            if (rawW <= 0 || rawH <= 0 ||
                rawW > ANDROID_MAX_IMAGE_DIMENSION || rawH > ANDROID_MAX_IMAGE_DIMENSION ||
                rawW.toLong() * rawH.toLong() > ANDROID_MAX_IMAGE_PIXELS
            ) {
                logDiagnostic("AndroidVisualDecoder", "Rejected oversized image raster: ${rawW}x$rawH")
                return emptyList()
            }

            var sampleSize = 1
            while ((rawW / sampleSize) > 1920 || (rawH / sampleSize) > 1920) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, decodeOptions)
                ?: return emptyList()
            try {
                decodeBitmap(bitmap)
            } finally {
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        } catch (e: Exception) {
            logDiagnostic("AndroidVisualDecoder", "Failed to decode barcode image bytes", e)
            emptyList()
        }
    }

    fun decodeBitmap(bitmap: Bitmap): List<BarcodePayload> {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0 || width.toLong() * height.toLong() > ANDROID_MAX_IMAGE_PIXELS) {
            return emptyList()
        }
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        return try {
            val result = MultiFormatReader().decode(binaryBitmap, hints)
            val type = mapZxingFormat(result.barcodeFormat)
            listOf(QuishingAnalyzer.analyze(result.text, type))
        } catch (_: ReaderException) {
            emptyList()
        } catch (e: Exception) {
            logDiagnostic("AndroidVisualDecoder", "Unexpected bitmap barcode decode failure", e)
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
            val result = MultiFormatReader().decode(binaryBitmap, hints)
            val type = mapZxingFormat(result.barcodeFormat)
            listOf(QuishingAnalyzer.analyze(result.text, type))
        } catch (_: ReaderException) {
            emptyList()
        } catch (e: Exception) {
            logDiagnostic("AndroidVisualDecoder", "Unexpected camera frame decode failure", e)
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
