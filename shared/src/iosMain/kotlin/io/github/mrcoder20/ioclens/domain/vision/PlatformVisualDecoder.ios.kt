package io.github.mrcoder20.ioclens.domain.vision

actual class PlatformVisualDecoder actual constructor() {
    actual fun decodeBarcodeFromBytes(imageBytes: ByteArray): List<BarcodePayload> {
        return emptyList()
    }
}
