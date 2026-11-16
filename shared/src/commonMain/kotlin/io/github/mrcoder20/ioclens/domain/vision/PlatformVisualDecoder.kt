package io.github.mrcoder20.ioclens.domain.vision

expect class PlatformVisualDecoder() {
    fun decodeBarcodeFromBytes(imageBytes: ByteArray): List<BarcodePayload>
}
