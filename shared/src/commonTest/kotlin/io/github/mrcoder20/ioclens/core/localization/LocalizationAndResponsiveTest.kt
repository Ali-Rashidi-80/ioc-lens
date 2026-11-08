package io.github.mrcoder20.ioclens.core.localization

import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.SourceResult
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalizationAndResponsiveTest {

    @Test
    fun testPersianDigitConversion() {
        val input = "Test 1234567890 Count 10 Engines 100%"
        val expected = "Test ۱۲۳۴۵۶۷۸۹۰ Count ۱۰ Engines ۱۰۰%"
        assertEquals(expected, LocalizationProvider.toPersianDigits(input))
    }

    @Test
    fun testPersianFormatNumberAndScore() {
        assertEquals("۱۰", LocalizationProvider.formatNumber(10, AppLanguage.PERSIAN))
        assertEquals("10", LocalizationProvider.formatNumber(10, AppLanguage.ENGLISH))

        assertEquals("۹۵٪", LocalizationProvider.formatScore(95, AppLanguage.PERSIAN))
        assertEquals("95%", LocalizationProvider.formatScore(95, AppLanguage.ENGLISH))
    }

    @Test
    fun testPersianVerdictNamesCoverage() {
        assertEquals("پاک / امن", LocalizationProvider.getVerdictName(VerdictLevel.CLEAN, AppLanguage.PERSIAN))
        assertEquals("کم‌خطر", LocalizationProvider.getVerdictName(VerdictLevel.LOW_RISK, AppLanguage.PERSIAN))
        assertEquals("مشکوک", LocalizationProvider.getVerdictName(VerdictLevel.SUSPICIOUS, AppLanguage.PERSIAN))
        assertEquals("مخرب / آلوده", LocalizationProvider.getVerdictName(VerdictLevel.MALICIOUS, AppLanguage.PERSIAN))
        assertEquals("بدون داده", LocalizationProvider.getVerdictName(VerdictLevel.NO_DATA, AppLanguage.PERSIAN))
    }

    @Test
    fun testPersianIndicatorTypesCoverage() {
        assertEquals("آدرس IP", LocalizationProvider.getIndicatorTypeName(IndicatorType.IP_ADDRESS, AppLanguage.PERSIAN))
        assertEquals("دامنه اینترنتی", LocalizationProvider.getIndicatorTypeName(IndicatorType.DOMAIN, AppLanguage.PERSIAN))
        assertEquals("نشانی وب (URL)", LocalizationProvider.getIndicatorTypeName(IndicatorType.URL, AppLanguage.PERSIAN))
        assertEquals("هش فایل", LocalizationProvider.getIndicatorTypeName(IndicatorType.FILE_HASH, AppLanguage.PERSIAN))
        assertEquals("نامشخص", LocalizationProvider.getIndicatorTypeName(IndicatorType.UNKNOWN, AppLanguage.PERSIAN))
    }

    @Test
    fun testPersianVerdictSummaryGeneration() {
        // 1. Malicious verdict
        val malVerdict = TriageVerdict(
            indicator = Indicator("evil.com", "evil.com", IndicatorType.DOMAIN),
            overallVerdict = VerdictLevel.MALICIOUS,
            confidenceScore = 95,
            summaryDescription = "MALICIOUS",
            sourceResults = listOf(SourceResult("URLhaus", VerdictLevel.MALICIOUS, 1, 1, "Malicious URL"))
        )
        val faMalSummary = LocalizationProvider.getLocalizedVerdictSummary(malVerdict, AppLanguage.PERSIAN)
        assertTrue(faMalSummary.contains("پی‌لود آلوده شناسایی شد"))
        assertTrue(faMalSummary.contains("URLhaus"))

        // 2. Clean verdict with Persian digit count
        val cleanVerdict = TriageVerdict(
            indicator = Indicator("8.8.8.8", "8.8.8.8", IndicatorType.IP_ADDRESS),
            overallVerdict = VerdictLevel.CLEAN,
            confidenceScore = 5,
            summaryDescription = "CLEAN",
            sourceResults = listOf(
                SourceResult("Cloudflare", VerdictLevel.CLEAN, 0, 1, "Clean"),
                SourceResult("RDAP", VerdictLevel.CLEAN, 0, 1, "Clean")
            )
        )
        val faCleanSummary = LocalizationProvider.getLocalizedVerdictSummary(cleanVerdict, AppLanguage.PERSIAN)
        assertTrue(faCleanSummary.contains("پاک و بدون تهدید"))
        assertTrue(faCleanSummary.contains("۲ موتور"))

        // 3. Privacy Guard indicator
        val privateVerdict = TriageVerdict(
            indicator = Indicator("192.168.1.1", "192.168.1.1", IndicatorType.IP_ADDRESS, isInternalOrPrivate = true),
            overallVerdict = VerdictLevel.CLEAN,
            confidenceScore = 0,
            summaryDescription = "PRIVACY",
            sourceResults = emptyList()
        )
        val faPrivacySummary = LocalizationProvider.getLocalizedVerdictSummary(privateVerdict, AppLanguage.PERSIAN)
        assertTrue(faPrivacySummary.contains("محافظ حریم خصوصی"))
    }

    @Test
    fun testAllStringsNonEmptyForPersian() {
        val strings = LocalizationProvider.getStrings(AppLanguage.PERSIAN)
        assertFalse(strings.appTitle.isBlank())
        assertFalse(strings.appTagline.isBlank())
        assertFalse(strings.scanHistoryTitle.isBlank())
        assertFalse(strings.clearHistory.isBlank())
        assertFalse(strings.noHistory.isBlank())
        assertFalse(strings.threatIntelProvidersTitle.isBlank())
        assertFalse(strings.enginesCount.isBlank())
        assertFalse(strings.statusReadyZeroKey.isBlank())
        assertFalse(strings.statusKeyRequired.isBlank())
        assertFalse(strings.statusScanning.isBlank())
        assertFalse(strings.statusRateLimited.isBlank())
        assertFalse(strings.statusTimedOut.isBlank())
        assertFalse(strings.statusNotApplicable.isBlank())
        assertFalse(strings.streamHashingTitle.isBlank())
        assertFalse(strings.zeroKeyInstantScanReady.isBlank())
        assertFalse(strings.scanPayload.isBlank())
        assertFalse(strings.cancelScan.isBlank())
        assertFalse(strings.done.isBlank())
        assertFalse(strings.closeButton.isBlank())
        assertFalse(strings.errorRateLimit.isBlank())
        assertFalse(strings.errorTimeout.isBlank())
        assertFalse(strings.errorNetwork.isBlank())
        assertFalse(strings.errorPrivacyGuard.isBlank())
        assertFalse(strings.streamHashingFailed.isBlank())
        assertFalse(strings.tabScanner.isBlank())
        assertFalse(strings.tabEngines.isBlank())
        assertFalse(strings.tabHistory.isBlank())
        assertFalse(strings.tabSettings.isBlank())
        assertFalse(strings.liveScanInProgress.isBlank())
        assertFalse(strings.enginesCompleted.isBlank())
    }

    @Test
    fun testProviderDisplayNameLocalization() {
        val providers = listOf(
            "URLhaus (abuse.ch)",
            "MalwareBazaar (abuse.ch)",
            "ThreatFox (abuse.ch)",
            "RDAP / Registry",
            "Cloudflare DoH",
            "VirusTotal",
            "AbuseIPDB",
            "AlienVault OTX",
            "Pulsedive",
            "IPQualityScore"
        )
        for (name in providers) {
            val faName = LocalizationProvider.getProviderDisplayName(name, AppLanguage.PERSIAN)
            assertFalse(faName.isBlank())
            val enName = LocalizationProvider.getProviderDisplayName(name, AppLanguage.ENGLISH)
            assertEquals(name, enName)
        }
        assertEquals("یوآرال‌هاوس (URLhaus)", LocalizationProvider.getProviderDisplayName("URLhaus (abuse.ch)", AppLanguage.PERSIAN))
        assertEquals("ویروس‌توتال (VirusTotal)", LocalizationProvider.getProviderDisplayName("VirusTotal", AppLanguage.PERSIAN))
        assertEquals("کلودفلر DoH (دی‌ان‌اس امن)", LocalizationProvider.getProviderDisplayName("Cloudflare DoH", AppLanguage.PERSIAN))
    }

    @Test
    fun testLocalizedSourceSummary() {
        val blockedMsg = "Internal indicator safely blocked locally"
        assertEquals(
            "نشانگر داخلی با موفقیت در لایه محلی مسدود شد",
            LocalizationProvider.getLocalizedSourceSummary(blockedMsg, AppLanguage.PERSIAN)
        )

        val cleanMsg = "No active malware payloads recorded in URLhaus"
        assertEquals(
            "هیچ پی‌لود بدافزار فعالی در URLhaus ثبت نشده است",
            LocalizationProvider.getLocalizedSourceSummary(cleanMsg, AppLanguage.PERSIAN)
        )

        val rateLimitMsg = "Rate limit exceeded (HTTP 429): API quota reached, retry later"
        assertEquals(
            "محدودیت نرخ درخواست (۴۲۹): سهمیه به اتمام رسید",
            LocalizationProvider.getLocalizedSourceSummary(rateLimitMsg, AppLanguage.PERSIAN)
        )

        val detectedMsg = "VirusTotal: 5/70 engines detected malicious"
        val localizedDetected = LocalizationProvider.getLocalizedSourceSummary(detectedMsg, AppLanguage.PERSIAN)
        assertTrue(localizedDetected.contains("موتور این نشانگر را آلوده شناسایی کردند"))
        assertTrue(localizedDetected.contains("۵/۷۰"))
    }

    @Test
    fun testAllLanguagesHaveFullStringCoverage() {
        for (lang in AppLanguage.entries) {
            val strings = LocalizationProvider.getStrings(lang)
            assertFalse(strings.appTitle.isBlank())
            assertFalse(strings.appTagline.isBlank())
            assertFalse(strings.done.isBlank())
            assertFalse(strings.closeButton.isBlank())
            assertFalse(strings.streamHashingFailed.isBlank())
            assertFalse(strings.zeroKeyInstantScanReady.isBlank())
            assertFalse(strings.scanPayload.isBlank())
            assertFalse(strings.cancelScan.isBlank())
            assertFalse(strings.noHistory.isBlank())
            assertFalse(strings.clearHistory.isBlank())
            assertFalse(strings.threatConfidenceScore.isBlank())
        }
    }
}

