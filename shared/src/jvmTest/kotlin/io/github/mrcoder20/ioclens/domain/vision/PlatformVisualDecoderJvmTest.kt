package io.github.mrcoder20.ioclens.domain.vision

import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.oned.Code128Writer
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlatformVisualDecoderJvmTest {

    @Test
    fun testDecodeSyntheticQrCode() {
        val targetText = "https://threat-sample.xyz/login?id=test"
        val qrWriter = QRCodeWriter()
        val bitMatrix = qrWriter.encode(targetText, BarcodeFormat.QR_CODE, 300, 300)
        val bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix)

        val baos = ByteArrayOutputStream()
        ImageIO.write(bufferedImage, "PNG", baos)
        val imageBytes = baos.toByteArray()

        val decoder = PlatformVisualDecoder()
        val results = decoder.decodeBarcodeFromBytes(imageBytes)

        assertTrue(results.isNotEmpty(), "Expected at least one decoded barcode")
        val payload = results.first()
        assertEquals(targetText, payload.rawText)
        assertEquals(BarcodeType.QR_CODE, payload.type)
        assertEquals(QuishingRisk.HIGH_RISK, payload.quishingRisk)
        assertEquals("hxxps://threat-sample[.]xyz/login?id=test", payload.defangedText)
    }

    @Test
    fun testDecodeSyntheticCode128() {
        val targetText = "APT29-C2-INDICATOR"
        val writer = Code128Writer()
        val bitMatrix = writer.encode(targetText, BarcodeFormat.CODE_128, 400, 100)
        val bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix)

        val baos = ByteArrayOutputStream()
        ImageIO.write(bufferedImage, "PNG", baos)
        val imageBytes = baos.toByteArray()

        val decoder = PlatformVisualDecoder()
        val results = decoder.decodeBarcodeFromBytes(imageBytes)

        assertTrue(results.isNotEmpty(), "Expected at least one decoded 1D barcode")
        val payload = results.first()
        assertEquals(targetText, payload.rawText)
        assertEquals(BarcodeType.CODE_128, payload.type)
    }

    @Test
    fun testCorruptBytesReturnEmptyList() {
        val corruptBytes = byteArrayOf(0x00, 0x11, 0x22, 0x33, 0x44, 0x55)
        val decoder = PlatformVisualDecoder()
        val results = decoder.decodeBarcodeFromBytes(corruptBytes)
        assertTrue(results.isEmpty())
    }

    @Test
    fun testEmptyBytesReturnEmptyList() {
        val decoder = PlatformVisualDecoder()
        val results = decoder.decodeBarcodeFromBytes(byteArrayOf())
        assertTrue(results.isEmpty())
    }
}
