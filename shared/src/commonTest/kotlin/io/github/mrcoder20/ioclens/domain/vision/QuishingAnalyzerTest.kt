package io.github.mrcoder20.ioclens.domain.vision

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class QuishingAnalyzerTest {

    @Test
    fun testUrlDefanging() {
        val raw = "https://evil.com/login?token=abc"
        val payload = QuishingAnalyzer.analyze(raw)
        assertEquals("hxxps://evil[.]com/login?token=abc", payload.defangedText)
    }

    @Test
    fun testBidirectionalRloOverrideSpoofing() {
        val raw = "https://corp-share.com/download/report\u202Egnp.exe"
        val payload = QuishingAnalyzer.analyze(raw)
        assertEquals(QuishingRisk.HIGH_RISK, payload.quishingRisk)
        assertTrue(payload.riskReason?.contains("RLO") == true)
        assertTrue(!payload.rawText.contains("\u202E"))
    }

    @Test
    fun testZeroWidthSpaceInjection() {
        val raw = "https://p\u200Ba\u200By\u200Bp\u200Ba\u200Bl.com/verify"
        val payload = QuishingAnalyzer.analyze(raw)
        assertEquals(QuishingRisk.HIGH_RISK, payload.quishingRisk)
        assertTrue(payload.riskReason?.contains("zero-width") == true)
        assertEquals("hxxps://paypal[.]com/verify", payload.defangedText)
    }

    @Test
    fun testAuthorityUserinfoSpoofing() {
        val raw = "http://chase.com@phishing-target.xyz/auth"
        val payload = QuishingAnalyzer.analyze(raw)
        assertEquals(QuishingRisk.HIGH_RISK, payload.quishingRisk)
        assertTrue(payload.riskReason?.contains("Authority userinfo spoofing") == true)
        assertEquals("hxxp://chase[.]com[@]phishing-target[.]xyz/auth", payload.defangedText)
    }

    @Test
    fun testNestedBase64Redirection() {
        // Target: https://malicious-login.com/verify
        // Base64: aHR0cHM6Ly9tYWxpY2lvdXMtbG9naW4uY29tL3ZlcmlmeQ==
        val raw = "https://tracking.analytics-service.com/redirect?u=aHR0cHM6Ly9tYWxpY2lvdXMtbG9naW4uY29tL3ZlcmlmeQ=="
        val payload = QuishingAnalyzer.analyze(raw)
        assertEquals(QuishingRisk.HIGH_RISK, payload.quishingRisk)
        assertNotNull(payload.unmaskedTargetUrl)
        assertEquals("https://malicious-login.com/verify", payload.unmaskedTargetUrl)
    }

    @Test
    fun testPunycodeHomoglyphDetection() {
        // Ukrainian 'і' (U+0456) in microsofthost
        val raw = "https://m\u0456crosoft.com/login"
        val payload = QuishingAnalyzer.analyze(raw)
        assertEquals(QuishingRisk.HIGH_RISK, payload.quishingRisk)
        assertTrue(payload.riskReason?.contains("homoglyph") == true)
    }

    @Test
    fun testNumericIpHexDword() {
        val rawHex = "http://0x7f000001/malware"
        val payloadHex = QuishingAnalyzer.analyze(rawHex)
        assertEquals(QuishingRisk.HIGH_RISK, payloadHex.quishingRisk)

        val rawDword = "http://2130706433/login"
        val payloadDword = QuishingAnalyzer.analyze(rawDword)
        assertEquals(QuishingRisk.HIGH_RISK, payloadDword.quishingRisk)
    }

    @Test
    fun testRawIpHostLiteral() {
        val raw = "http://185.220.101.5/payload.sh"
        val payload = QuishingAnalyzer.analyze(raw)
        assertEquals(QuishingRisk.HIGH_RISK, payload.quishingRisk)
        assertTrue(payload.riskReason?.contains("Raw IP host literal") == true)
    }

    @Test
    fun testDangerousUriSchemes() {
        val js = "javascript:alert(document.cookie)"
        assertEquals(QuishingRisk.HIGH_RISK, QuishingAnalyzer.analyze(js).quishingRisk)

        val data = "data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg=="
        assertEquals(QuishingRisk.HIGH_RISK, QuishingAnalyzer.analyze(data).quishingRisk)

        val intent = "intent://scan/#Intent;scheme=malware;package=com.evil.app;end"
        assertEquals(QuishingRisk.HIGH_RISK, QuishingAnalyzer.analyze(intent).quishingRisk)

        val itms = "itms-services://?action=download-manifest&url=https://evil.com/app.plist"
        assertEquals(QuishingRisk.HIGH_RISK, QuishingAnalyzer.analyze(itms).quishingRisk)
    }

    @Test
    fun testMalwareDropperExtensions() {
        val apk = "https://safe-looking-portal.com/update.apk"
        val payloadApk = QuishingAnalyzer.analyze(apk)
        assertEquals(QuishingRisk.HIGH_RISK, payloadApk.quishingRisk)
        assertTrue(payloadApk.riskReason?.contains(".apk") == true)

        val exe = "https://files.storage.net/download/payload.exe?token=123"
        val payloadExe = QuishingAnalyzer.analyze(exe)
        assertEquals(QuishingRisk.HIGH_RISK, payloadExe.quishingRisk)
        assertTrue(payloadExe.riskReason?.contains(".exe") == true)
    }

    @Test
    fun testWifiConfigurationString() {
        val wifi = "WIFI:S:Guest-Office;T:WPA;P:SecretPassword123;;"
        val payload = QuishingAnalyzer.analyze(wifi)
        assertEquals(QuishingRisk.SUSPICIOUS, payload.quishingRisk)
        assertTrue(payload.riskReason?.contains("Wi-Fi network configuration") == true)
    }

    @Test
    fun testUrlShortenerSuspicious() {
        val raw = "https://bit.ly/3xY8zQp"
        val payload = QuishingAnalyzer.analyze(raw)
        assertEquals(QuishingRisk.SUSPICIOUS, payload.quishingRisk)
        assertTrue(payload.riskReason?.contains("URL Shortener") == true)
    }

    @Test
    fun testHighRiskTldCredentialHarvest() {
        val raw = "https://auth-portal.top/login"
        val payload = QuishingAnalyzer.analyze(raw)
        assertEquals(QuishingRisk.HIGH_RISK, payload.quishingRisk)
        assertTrue(payload.riskReason?.contains("High-risk TLD") == true)
    }

    @Test
    fun testPayloadBoundaryProtection() {
        // String of 10,000 characters
        val huge = "A".repeat(10000)
        val payload = QuishingAnalyzer.analyze(huge)
        assertTrue(payload.rawText.length <= 4096)
    }

    @Test
    fun testHashInBarcodeRecognizedByIocParser() {
        val sha256 = "b3e93fda43d72109f6d17cf6aa0fa6017db1f8ee9c237ec49083385bb0248bf5"
        val payload = QuishingAnalyzer.analyze(sha256, BarcodeType.DATA_MATRIX)
        assertEquals("FILE_HASH", payload.detectedIocType)
    }
}
