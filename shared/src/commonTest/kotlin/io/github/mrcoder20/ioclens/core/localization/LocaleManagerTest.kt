package io.github.mrcoder20.ioclens.core.localization

import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocaleManagerTest {

    private val persianRegex = Regex("[\\u0600-\\u06FF]")

    @BeforeTest
    fun setUp() {
        LocaleManager.setLanguage(AppLanguage.ENGLISH)
    }

    @AfterTest
    fun tearDown() {
        LocaleManager.setLanguage(AppLanguage.ENGLISH)
    }

    @Test
    fun testInitialLanguageIsEnglish() {
        assertEquals(AppLanguage.ENGLISH, LocaleManager.currentLanguage.value)
    }

    @Test
    fun testSetLanguageUpdatesStateFlow() {
        LocaleManager.setLanguage(AppLanguage.PERSIAN)
        assertEquals(AppLanguage.PERSIAN, LocaleManager.currentLanguage.value)

        LocaleManager.setLanguage(AppLanguage.RUSSIAN)
        assertEquals(AppLanguage.RUSSIAN, LocaleManager.currentLanguage.value)

        LocaleManager.setLanguage(AppLanguage.ENGLISH)
        assertEquals(AppLanguage.ENGLISH, LocaleManager.currentLanguage.value)
    }

    @Test
    fun testGetStringsExplicitAndDefaultLanguage() {
        val enStrings = LocaleManager.getStrings(AppLanguage.ENGLISH)
        assertEquals("IOC Lens", enStrings.appTitle)
        assertEquals("Theme", enStrings.themeLabel)

        val faStrings = LocaleManager.getStrings(AppLanguage.PERSIAN)
        assertEquals("IOC Lens", faStrings.appTitle)
        assertEquals("تم و ظاهر", faStrings.themeLabel)

        val ruStrings = LocaleManager.getStrings(AppLanguage.RUSSIAN)
        assertEquals("IOC Lens", ruStrings.appTitle)
        assertEquals("Тема", ruStrings.themeLabel)

        // Test default parameter uses currentLanguage StateFlow
        LocaleManager.setLanguage(AppLanguage.PERSIAN)
        val defaultFaStrings = LocaleManager.getStrings()
        assertEquals("تم و ظاهر", defaultFaStrings.themeLabel)

        LocaleManager.setLanguage(AppLanguage.RUSSIAN)
        val defaultRuStrings = LocaleManager.getStrings()
        assertEquals("Тема", defaultRuStrings.themeLabel)

        LocaleManager.setLanguage(AppLanguage.ENGLISH)
        val defaultEnStrings = LocaleManager.getStrings()
        assertEquals("Theme", defaultEnStrings.themeLabel)
    }

    @Test
    fun testFormatNumberIntAcrossLocales() {
        assertEquals("0", LocaleManager.formatNumber(0, AppLanguage.ENGLISH))
        assertEquals("۰", LocaleManager.formatNumber(0, AppLanguage.PERSIAN))
        assertEquals("0", LocaleManager.formatNumber(0, AppLanguage.RUSSIAN))

        assertEquals("42", LocaleManager.formatNumber(42, AppLanguage.ENGLISH))
        assertEquals("۴۲", LocaleManager.formatNumber(42, AppLanguage.PERSIAN))
        assertEquals("42", LocaleManager.formatNumber(42, AppLanguage.RUSSIAN))

        assertEquals("1234567890", LocaleManager.formatNumber(1234567890, AppLanguage.ENGLISH))
        assertEquals("۱۲۳۴۵۶۷۸۹۰", LocaleManager.formatNumber(1234567890, AppLanguage.PERSIAN))
        assertEquals("1234567890", LocaleManager.formatNumber(1234567890, AppLanguage.RUSSIAN))

        assertEquals("-15", LocaleManager.formatNumber(-15, AppLanguage.ENGLISH))
        assertEquals("-۱۵", LocaleManager.formatNumber(-15, AppLanguage.PERSIAN))
        assertEquals("-15", LocaleManager.formatNumber(-15, AppLanguage.RUSSIAN))
    }

    @Test
    fun testFormatNumberLongAcrossLocales() {
        val largeLong = 9876543210123L
        assertEquals("9876543210123", LocaleManager.formatNumber(largeLong, AppLanguage.ENGLISH))
        assertEquals("۹۸۷۶۵۴۳۲۱۰۱۲۳", LocaleManager.formatNumber(largeLong, AppLanguage.PERSIAN))
        assertEquals("9876543210123", LocaleManager.formatNumber(largeLong, AppLanguage.RUSSIAN))

        val negativeLong = -9876543210123L
        assertEquals("-9876543210123", LocaleManager.formatNumber(negativeLong, AppLanguage.ENGLISH))
        assertEquals("-۹۸۷۶۵۴۳۲۱۰۱۲۳", LocaleManager.formatNumber(negativeLong, AppLanguage.PERSIAN))
        assertEquals("-9876543210123", LocaleManager.formatNumber(negativeLong, AppLanguage.RUSSIAN))

        // Extreme Long Boundaries: Long.MAX_VALUE and Long.MIN_VALUE
        val maxLong = Long.MAX_VALUE
        val minLong = Long.MIN_VALUE
        assertEquals("9223372036854775807", LocaleManager.formatNumber(maxLong, AppLanguage.ENGLISH))
        assertEquals("۹۲۲۳۳۷۲۰۳۶۸۵۴۷۷۵۸۰۷", LocaleManager.formatNumber(maxLong, AppLanguage.PERSIAN))
        assertEquals("9223372036854775807", LocaleManager.formatNumber(maxLong, AppLanguage.RUSSIAN))

        assertEquals("-9223372036854775808", LocaleManager.formatNumber(minLong, AppLanguage.ENGLISH))
        assertEquals("-۹۲۲۳۳۷۲۰۳۶۸۵۴۷۷۵۸۰۸", LocaleManager.formatNumber(minLong, AppLanguage.PERSIAN))
        assertEquals("-9223372036854775808", LocaleManager.formatNumber(minLong, AppLanguage.RUSSIAN))

        // Strict assertion: Persian format contains 0 ASCII digits
        val asciiDigits = Regex("[0-9]")
        assertFalse(asciiDigits.containsMatchIn(LocaleManager.formatNumber(maxLong, AppLanguage.PERSIAN)))
        assertFalse(asciiDigits.containsMatchIn(LocaleManager.formatNumber(minLong, AppLanguage.PERSIAN)))
    }

    @Test
    fun testFormatNumberDoubleAcrossLocales() {
        val pi = 3.14159
        assertEquals("3.14159", LocaleManager.formatNumber(pi, AppLanguage.ENGLISH))
        assertEquals("۳.۱۴۱۵۹", LocaleManager.formatNumber(pi, AppLanguage.PERSIAN))
        assertEquals("3.14159", LocaleManager.formatNumber(pi, AppLanguage.RUSSIAN))

        val zeroDouble = 0.0
        assertEquals("0.0", LocaleManager.formatNumber(zeroDouble, AppLanguage.ENGLISH))
        assertEquals("۰.۰", LocaleManager.formatNumber(zeroDouble, AppLanguage.PERSIAN))
        assertEquals("0.0", LocaleManager.formatNumber(zeroDouble, AppLanguage.RUSSIAN))

        val negativeDouble = -12.5
        assertEquals("-12.5", LocaleManager.formatNumber(negativeDouble, AppLanguage.ENGLISH))
        assertEquals("-۱۲.۵", LocaleManager.formatNumber(negativeDouble, AppLanguage.PERSIAN))
        assertEquals("-12.5", LocaleManager.formatNumber(negativeDouble, AppLanguage.RUSSIAN))

        // Extreme Double Boundaries: Double.MAX_VALUE, MIN_VALUE, NaN, Infinities, negative decimal
        val asciiDigits = Regex("[0-9]")
        val extremeCases = listOf(
            Double.MAX_VALUE,
            Double.MIN_VALUE,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            -0.0001
        )

        for (value in extremeCases) {
            val en = LocaleManager.formatNumber(value, AppLanguage.ENGLISH)
            val fa = LocaleManager.formatNumber(value, AppLanguage.PERSIAN)
            val ru = LocaleManager.formatNumber(value, AppLanguage.RUSSIAN)

            assertEquals(value.toString(), en)
            assertEquals(value.toString(), ru)
            assertFalse(persianRegex.containsMatchIn(en), "English Double format should have 0 Persian chars")

            // When original string representation has ASCII digits, Persian must convert all of them
            if (asciiDigits.containsMatchIn(value.toString())) {
                assertFalse(
                    asciiDigits.containsMatchIn(fa),
                    "Persian formatted output for $value must contain 0 ASCII digits: $fa"
                )
            }
        }
    }

    @Test
    fun testPersianConversionAllDigitsExhaustive() {
        val allDigits = "0123456789"
        val expected = "۰۱۲۳۴۵۶۷۸۹"
        assertEquals(expected, LocalizationProvider.toPersianDigits(allDigits))

        val faDigits = listOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        for (i in 0..9) {
            assertEquals(faDigits[i].toString(), LocalizationProvider.toPersianDigits(i.toString()))
        }
    }

    @Test
    fun testFormatNumberPolymorphicNumberAcrossLocales() {
        val numInt: Number = 100
        val numLong: Number = 9876543210L
        val numDouble: Number = 99.9
        val numFloat: Number = 2.5f
        val numShort: Number = 12.toShort()
        val numByte: Number = 8.toByte()

        // LocaleManager polymorphism
        assertEquals("۱۰۰", LocaleManager.formatNumber(numInt, AppLanguage.PERSIAN))
        assertEquals("۹۸۷۶۵۴۳۲۱۰", LocaleManager.formatNumber(numLong, AppLanguage.PERSIAN))
        assertEquals("۹۹.۹", LocaleManager.formatNumber(numDouble, AppLanguage.PERSIAN))
        assertEquals("۲.۵", LocaleManager.formatNumber(numFloat, AppLanguage.PERSIAN))
        assertEquals("۱۲", LocaleManager.formatNumber(numShort, AppLanguage.PERSIAN))
        assertEquals("۸", LocaleManager.formatNumber(numByte, AppLanguage.PERSIAN))

        // LocalizationProvider polymorphism
        assertEquals("۱۰۰", LocalizationProvider.formatNumber(numInt, AppLanguage.PERSIAN))
        assertEquals("۹۸۷۶۵۴۳۲۱۰", LocalizationProvider.formatNumber(numLong, AppLanguage.PERSIAN))
        assertEquals("۹۹.۹", LocalizationProvider.formatNumber(numDouble, AppLanguage.PERSIAN))
        assertEquals("۲.۵", LocalizationProvider.formatNumber(numFloat, AppLanguage.PERSIAN))
        assertEquals("۱۲", LocalizationProvider.formatNumber(numShort, AppLanguage.PERSIAN))
        assertEquals("۸", LocalizationProvider.formatNumber(numByte, AppLanguage.PERSIAN))

        // LocalizationProvider backward compatibility for Int
        val primitiveInt: Int = 100
        assertEquals("۱۰۰", LocalizationProvider.formatNumber(primitiveInt, AppLanguage.PERSIAN))
        assertEquals("100", LocalizationProvider.formatNumber(primitiveInt, AppLanguage.ENGLISH))
        assertEquals("100", LocalizationProvider.formatNumber(primitiveInt, AppLanguage.RUSSIAN))
    }

    @Test
    fun testFormatNumberDefaultLanguageStateFlow() {
        LocaleManager.setLanguage(AppLanguage.PERSIAN)
        assertEquals("۵۰", LocaleManager.formatNumber(50))
        assertEquals("۹۹۹۹", LocaleManager.formatNumber(9999L))
        assertEquals("۱.۵", LocaleManager.formatNumber(1.5))

        LocaleManager.setLanguage(AppLanguage.ENGLISH)
        assertEquals("50", LocaleManager.formatNumber(50))
        assertEquals("9999", LocaleManager.formatNumber(9999L))
        assertEquals("1.5", LocaleManager.formatNumber(1.5))

        LocaleManager.setLanguage(AppLanguage.RUSSIAN)
        assertEquals("50", LocaleManager.formatNumber(50))
        assertEquals("9999", LocaleManager.formatNumber(9999L))
        assertEquals("1.5", LocaleManager.formatNumber(1.5))
    }

    @Test
    fun testFormatScoreFormattingAndBounds() {
        // Lower bound 0
        assertEquals("0%", LocaleManager.formatScore(0, AppLanguage.ENGLISH))
        assertEquals("۰٪", LocaleManager.formatScore(0, AppLanguage.PERSIAN))
        assertEquals("0%", LocaleManager.formatScore(0, AppLanguage.RUSSIAN))

        // Mid score 50
        assertEquals("50%", LocaleManager.formatScore(50, AppLanguage.ENGLISH))
        assertEquals("۵۰٪", LocaleManager.formatScore(50, AppLanguage.PERSIAN))
        assertEquals("50%", LocaleManager.formatScore(50, AppLanguage.RUSSIAN))

        // Upper bound 100
        assertEquals("100%", LocaleManager.formatScore(100, AppLanguage.ENGLISH))
        assertEquals("۱۰۰٪", LocaleManager.formatScore(100, AppLanguage.PERSIAN))
        assertEquals("100%", LocaleManager.formatScore(100, AppLanguage.RUSSIAN))

        // Negative score bound
        assertEquals("-5%", LocaleManager.formatScore(-5, AppLanguage.ENGLISH))
        assertEquals("-۵٪", LocaleManager.formatScore(-5, AppLanguage.PERSIAN))
        assertEquals("-5%", LocaleManager.formatScore(-5, AppLanguage.RUSSIAN))

        // Exceeding 100 bound
        assertEquals("150%", LocaleManager.formatScore(150, AppLanguage.ENGLISH))
        assertEquals("۱۵۰٪", LocaleManager.formatScore(150, AppLanguage.PERSIAN))
        assertEquals("150%", LocaleManager.formatScore(150, AppLanguage.RUSSIAN))

        // Default language
        LocaleManager.setLanguage(AppLanguage.PERSIAN)
        assertEquals("۸۵٪", LocaleManager.formatScore(85))

        LocaleManager.setLanguage(AppLanguage.ENGLISH)
        assertEquals("85%", LocaleManager.formatScore(85))
    }

    @Test
    fun testZeroPersianUnicodeCharactersInEnglishLocale() {
        val strings = LocaleManager.getStrings(AppLanguage.ENGLISH)

        val properties = listOf(
            "appTitle" to strings.appTitle,
            "appTagline" to strings.appTagline,
            "skip" to strings.skip,
            "next" to strings.next,
            "previous" to strings.previous,
            "start" to strings.start,
            "done" to strings.done,
            "changeLanguage" to strings.changeLanguage,
            "searchPlaceholder" to strings.searchPlaceholder,
            "searchFieldPlaceholder" to strings.searchFieldPlaceholder,
            "clear" to strings.clear,
            "analyzeInstant" to strings.analyzeInstant,
            "scanPayload" to strings.scanPayload,
            "cancelScan" to strings.cancelScan,
            "zeroKeyInstantScanReady" to strings.zeroKeyInstantScanReady,
            "streamHashingTitle" to strings.streamHashingTitle,
            "introGuide" to strings.introGuide,
            "enterIndicatorError" to strings.enterIndicatorError,
            "threatIntelSourcesResults" to strings.threatIntelSourcesResults,
            "privacyGuardNotice" to strings.privacyGuardNotice,
            "themeLabel" to strings.themeLabel,
            "languageLabel" to strings.languageLabel,
            "menuHome" to strings.menuHome,
            "menuSettings" to strings.menuSettings,
            "menuGuide" to strings.menuGuide,
            "menuProviders" to strings.menuProviders,
            "menuPrivacyGuard" to strings.menuPrivacyGuard,
            "menuAbout" to strings.menuAbout,
            "settingsTitle" to strings.settingsTitle,
            "appVersion" to strings.appVersion,
            "versionLabel" to strings.versionLabel,
            "systemDefaultTheme" to strings.systemDefaultTheme,
            "darkTheme" to strings.darkTheme,
            "apiKeysTitle" to strings.apiKeysTitle,
            "apiKeysSubtitle" to strings.apiKeysSubtitle,
            "vtApiKeyLabel" to strings.vtApiKeyLabel,
            "abuseApiKeyLabel" to strings.abuseApiKeyLabel,
            "alienApiKeyLabel" to strings.alienApiKeyLabel,
            "pulseApiKeyLabel" to strings.pulseApiKeyLabel,
            "ipqsApiKeyLabel" to strings.ipqsApiKeyLabel,
            "autoScanClipboard" to strings.autoScanClipboard,
            "privacyGuardSetting" to strings.privacyGuardSetting,
            "saveKeys" to strings.saveKeys,
            "savedSuccessfully" to strings.savedSuccessfully,
            "scanHistoryTitle" to strings.scanHistoryTitle,
            "clearHistory" to strings.clearHistory,
            "noHistory" to strings.noHistory,
            "threatConfidenceScore" to strings.threatConfidenceScore,
            "indicatorTypeLabel" to strings.indicatorTypeLabel,
            "threatIntelProvidersTitle" to strings.threatIntelProvidersTitle,
            "enginesCount" to strings.enginesCount,
            "freeBadge" to strings.freeBadge,
            "statusReady" to strings.statusReady,
            "statusReadyZeroKey" to strings.statusReadyZeroKey,
            "statusKeyRequired" to strings.statusKeyRequired,
            "statusScanning" to strings.statusScanning,
            "statusRateLimited" to strings.statusRateLimited,
            "statusTimedOut" to strings.statusTimedOut,
            "statusNotApplicable" to strings.statusNotApplicable,
            "closeButton" to strings.closeButton,
            "streamHashingFailed" to strings.streamHashingFailed,
            "errorRateLimit" to strings.errorRateLimit,
            "errorTimeout" to strings.errorTimeout,
            "errorNetwork" to strings.errorNetwork,
            "errorPrivacyGuard" to strings.errorPrivacyGuard,
            "tabScanner" to strings.tabScanner,
            "tabEngines" to strings.tabEngines,
            "tabHistory" to strings.tabHistory,
            "tabSettings" to strings.tabSettings,
            "liveScanInProgress" to strings.liveScanInProgress,
            "enginesCompleted" to strings.enginesCompleted,
            "sampleIocsTitle" to strings.sampleIocsTitle,
            "sampleGoogleDns" to strings.sampleGoogleDns,
            "sampleMalwareUrl" to strings.sampleMalwareUrl,
            "sampleHash" to strings.sampleHash,
            "pasteFromClipboard" to strings.pasteFromClipboard,
            "dropFileHint" to strings.dropFileHint,
            "recentTriageFeed" to strings.recentTriageFeed,
            "deleteItem" to strings.deleteItem,
            "reAnalyze" to strings.reAnalyze,
            "categoryNetwork" to strings.categoryNetwork,
            "categoryMalware" to strings.categoryMalware,
            "categoryThreats" to strings.categoryThreats,
            "serverlessStatus" to strings.serverlessStatus,
            "systemReadyEngines" to strings.systemReadyEngines,
            "freeFeedsBadge" to strings.freeFeedsBadge,
            "privacyGuardActiveBadge" to strings.privacyGuardActiveBadge,
            "settingsTabApiKeys" to strings.settingsTabApiKeys,
            "settingsTabGeneralPrivacy" to strings.settingsTabGeneralPrivacy,
            "settingsTabAbout" to strings.settingsTabAbout,
            "apiKeysZeroKeyBanner" to strings.apiKeysZeroKeyBanner,
            "vtSubtitle" to strings.vtSubtitle,
            "abuseSubtitle" to strings.abuseSubtitle,
            "alienSubtitle" to strings.alienSubtitle,
            "pulseSubtitle" to strings.pulseSubtitle,
            "ipqsSubtitle" to strings.ipqsSubtitle,
            "keyConfiguredBadge" to strings.keyConfiguredBadge,
            "keyOptionalBadge" to strings.keyOptionalBadge,
            "keyInputPlaceholder" to strings.keyInputPlaceholder,
            "privacyGuardActiveDesc" to strings.privacyGuardActiveDesc,
            "privacyGuardInactiveDesc" to strings.privacyGuardInactiveDesc,
            "badgeClientDirect" to strings.badgeClientDirect,
            "badgeZeroCloudDb" to strings.badgeZeroCloudDb,
            "settingsSecurityConsoleBadge" to strings.settingsSecurityConsoleBadge,
            "cardPrivacyGuardTitle" to strings.cardPrivacyGuardTitle,
            "cardThreatIntelTitle" to strings.cardThreatIntelTitle,
            "aboutZeroCloudDesc" to strings.aboutZeroCloudDesc,
            "aboutClientDirectDesc" to strings.aboutClientDirectDesc,
            "aboutPrivacyGuardDesc" to strings.aboutPrivacyGuardDesc,
            "aboutThreatIntelDesc" to strings.aboutThreatIntelDesc,
            "replayInteractiveTour" to strings.replayInteractiveTour,
            "specStandalonePortable" to strings.specStandalonePortable,
            "specZeroTelemetry" to strings.specZeroTelemetry,
            "specOpenSource" to strings.specOpenSource,
            "specMultiplatformEngine" to strings.specMultiplatformEngine,
            "statusKeyRequiredShort" to strings.statusKeyRequiredShort,
            "badgeThemeCyberDark" to strings.badgeThemeCyberDark,
            "badgeThemeAutoSystem" to strings.badgeThemeAutoSystem,
            "badgeGuardActive" to strings.badgeGuardActive,
            "badgeGuardDisabled" to strings.badgeGuardDisabled,
            "badgeOpenSourceVersion" to strings.badgeOpenSourceVersion,
            "searchHistoryPlaceholder" to strings.searchHistoryPlaceholder,
            "filterAll" to strings.filterAll,
            "noFilterResults" to strings.noFilterResults,
            "enginesZeroKeySubtitle" to strings.enginesZeroKeySubtitle,
            "visitWebsite" to strings.visitWebsite,
            "copyWebsiteUrl" to strings.copyWebsiteUrl,
            "websiteUrlCopied" to strings.websiteUrlCopied,
            "filterByIocType" to strings.filterByIocType,
            "githubRepoUrl" to strings.githubRepoUrl,
            "openGithubRepo" to strings.openGithubRepo,
            "copyGithubUrl" to strings.copyGithubUrl,
            "githubUrlCopied" to strings.githubUrlCopied,
            "clientManifestoTitle" to strings.clientManifestoTitle,
            "clientManifestoDesc" to strings.clientManifestoDesc,
            "replayGuideTitle" to strings.replayGuideTitle
        )

        for ((propName, propValue) in properties) {
            assertFalse(
                persianRegex.containsMatchIn(propValue),
                "English property '$propName' contains unexpected Persian character: '$propValue'"
            )
        }

        // Also assert formatting functions with English do not introduce Persian characters
        assertFalse(persianRegex.containsMatchIn(LocaleManager.formatScore(88, AppLanguage.ENGLISH)))
        assertFalse(persianRegex.containsMatchIn(LocaleManager.formatNumber(12345, AppLanguage.ENGLISH)))
        assertFalse(persianRegex.containsMatchIn(LocaleManager.formatNumber(9876543210L, AppLanguage.ENGLISH)))
        assertFalse(persianRegex.containsMatchIn(LocaleManager.formatNumber(3.14, AppLanguage.ENGLISH)))
    }

    @Test
    fun testEnglishBranchesOfLocalizationProviderZeroPersianUnicode() {
        // Verdict Names
        for (level in VerdictLevel.entries) {
            val name = LocalizationProvider.getVerdictName(level, AppLanguage.ENGLISH)
            assertFalse(persianRegex.containsMatchIn(name), "English verdict name for $level contains Persian: $name")
        }

        // Indicator Type Names
        for (type in IndicatorType.entries) {
            val name = LocalizationProvider.getIndicatorTypeName(type, AppLanguage.ENGLISH)
            assertFalse(persianRegex.containsMatchIn(name), "English indicator type for $type contains Persian: $name")
        }

        // Provider Display Names and Descriptions
        val providers = listOf(
            "URLhaus (abuse.ch)",
            "MalwareBazaar (abuse.ch)",
            "ThreatFox (abuse.ch)",
            "RDAP / Registry",
            "Cloudflare DoH",
            "Shodan InternetDB",
            "AlienVault OTX",
            "RIPE Stat",
            "crt.sh",
            "Feodo Tracker (abuse.ch)",
            "ipwho.is",
            "VirusTotal",
            "AbuseIPDB",
            "Pulsedive",
            "IPQualityScore"
        )
        for (p in providers) {
            val displayName = LocalizationProvider.getProviderDisplayName(p, AppLanguage.ENGLISH)
            assertFalse(persianRegex.containsMatchIn(displayName), "English provider name $p contains Persian: $displayName")

            val desc = LocalizationProvider.getProviderDescription(p, AppLanguage.ENGLISH)
            assertFalse(persianRegex.containsMatchIn(desc), "English provider desc $p contains Persian: $desc")
        }

        // Source summary English passthrough
        val sampleSummary = "Shodan: 2 open ports (80, 443), 0 CVEs"
        val localizedSummary = LocalizationProvider.getLocalizedSourceSummary(sampleSummary, AppLanguage.ENGLISH)
        assertEquals(sampleSummary, localizedSummary)
        assertFalse(persianRegex.containsMatchIn(localizedSummary))
    }

    @Test
    fun testRapidConcurrentLanguageSwitchingAndCollectorIntegrity() = runTest {
        val languages = listOf(AppLanguage.ENGLISH, AppLanguage.PERSIAN, AppLanguage.RUSSIAN)
        val mutex = Mutex()
        val emissions = mutableListOf<AppLanguage>()

        val started = CompletableDeferred<Unit>()
        val collectorJob = launch(Dispatchers.Default) {
            LocaleManager.currentLanguage.collect { lang ->
                mutex.withLock {
                    emissions.add(lang)
                }
                started.complete(Unit)
            }
        }
        started.await()

        // Rapidly switch languages across 20 concurrent coroutines (1,000 total switches)
        val mutatorJobs = (0 until 20).map { id ->
            launch(Dispatchers.Default) {
                repeat(50) { iter ->
                    val targetLang = languages[(id + iter) % languages.size]
                    LocaleManager.setLanguage(targetLang)
                    val current = LocaleManager.currentLanguage.value
                    assertTrue(current in languages, "Current language must be one of the known languages")
                    yield()
                }
            }
        }
        mutatorJobs.joinAll()

        // Deterministic sequential switching to verify collectors and StateFlow consistency
        for (expected in languages) {
            LocaleManager.setLanguage(expected)
            assertEquals(expected, LocaleManager.currentLanguage.value)
            assertEquals(LocaleManager.getStrings(expected).themeLabel, LocaleManager.getStrings().themeLabel)
            assertEquals(LocaleManager.formatNumber(42, expected), LocaleManager.formatNumber(42))
            assertEquals(LocaleManager.formatScore(90, expected), LocaleManager.formatScore(90))
        }

        delay(50)
        collectorJob.cancel()
        val collectedCount = mutex.withLock { emissions.size }
        assertTrue(collectedCount > 0, "Collector must observe language updates")
        val allValid = mutex.withLock { emissions.all { it in languages } }
        assertTrue(allValid, "All emissions must be valid AppLanguage instances")
    }

    @Test
    fun testCollectorReceivesImmediateReplayValue() = runTest {
        LocaleManager.setLanguage(AppLanguage.PERSIAN)
        val initialFa = LocaleManager.currentLanguage.first()
        assertEquals(AppLanguage.PERSIAN, initialFa)

        LocaleManager.setLanguage(AppLanguage.RUSSIAN)
        val initialRu = LocaleManager.currentLanguage.first()
        assertEquals(AppLanguage.RUSSIAN, initialRu)

        LocaleManager.setLanguage(AppLanguage.ENGLISH)
        val initialEn = LocaleManager.currentLanguage.first()
        assertEquals(AppLanguage.ENGLISH, initialEn)
    }
}
