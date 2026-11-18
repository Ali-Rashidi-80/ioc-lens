package io.github.mrcoder20.ioclens.domain.vision

import io.github.mrcoder20.ioclens.logDiagnostic
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy
import kotlin.concurrent.Volatile

private const val IOS_MAX_VISION_IMAGE_BYTES = 20L * 1024L * 1024L

data class IosDecodedBarcode(
    val text: String,
    val formatHint: String = "OTHER"
)

interface IosBarcodeDecoderDelegate {
    fun decodeBarcodes(imageBytes: ByteArray): List<IosDecodedBarcode>
}

object IosVisualDecoderBridge {
    @Volatile
    var delegate: IosBarcodeDecoderDelegate? = null
}

internal fun resolveBarcodeTypeFromHint(hint: String): BarcodeType = when (hint.uppercase()) {
    "QR_CODE", "QR" -> BarcodeType.QR_CODE
    "DATA_MATRIX" -> BarcodeType.DATA_MATRIX
    "AZTEC" -> BarcodeType.AZTEC
    "PDF_417", "PDF417" -> BarcodeType.PDF_417
    "CODE_128", "CODE128" -> BarcodeType.CODE_128
    "CODE_39", "CODE39" -> BarcodeType.CODE_39
    else -> BarcodeType.OTHER
}

actual class PlatformVisualDecoder actual constructor() {
    actual fun decodeBarcodeFromBytes(imageBytes: ByteArray): List<BarcodePayload> {
        if (imageBytes.isEmpty() || imageBytes.size.toLong() > IOS_MAX_VISION_IMAGE_BYTES) {
            return emptyList()
        }
        val activeDelegate = IosVisualDecoderBridge.delegate
        if (activeDelegate == null) {
            logDiagnostic("IosVisualDecoder", "No IosBarcodeDecoderDelegate registered")
            return emptyList()
        }
        return try {
            activeDelegate.decodeBarcodes(imageBytes)
                .filter { it.text.isNotBlank() }
                .map { decoded -> QuishingAnalyzer.analyze(decoded.text, resolveBarcodeTypeFromHint(decoded.formatHint)) }
        } catch (e: Exception) {
            logDiagnostic("IosVisualDecoder", "Vision barcode decode failed", e)
            emptyList()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun decodeFileAtPath(filePath: String): List<BarcodePayload> {
        if (filePath.isBlank()) return emptyList()
        val data = NSData.create(contentsOfFile = filePath) ?: return emptyList()
        val length = data.length.toLong()
        if (length <= 0L || length > IOS_MAX_VISION_IMAGE_BYTES) {
            return emptyList()
        }
        val bytes = ByteArray(length.toInt())
        val rawPtr = data.bytes ?: return emptyList()
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), rawPtr, data.length)
        }
        return decodeBarcodeFromBytes(bytes)
    }
}
