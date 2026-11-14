package io.github.mrcoder20.ioclens.core.localization

import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD Automated Test Suite verifying that English (and Russian) locales contain
 * ZERO hardcoded Persian unicode characters ([\u0600-\u06FF]) across all strings,
 * slide data, verdicts, indicators, badges, and card contents.
 */
class EnglishLocaleSanityTest {

    private val persianRegex = Regex("[\\u0600-\\u06FF]")

    @Test
    fun testEnglishAppStringsContainZeroPersianCharacters() {
        val strings = LocalizationProvider.getStrings(AppLanguage.ENGLISH)

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
            "filterByIocType" to strings.filterByIocType
        )

        for ((propName, propValue) in properties) {
            assertFalse(
                persianRegex.containsMatchIn(propValue),
                "English string '$propName' contains Persian characters: '$propValue'"
            )
        }

        val providers = listOf(
            "URLhaus (abuse.ch)", "MalwareBazaar (abuse.ch)", "ThreatFox (abuse.ch)",
            "RDAP / Registry", "Cloudflare DoH", "Shodan InternetDB", "AlienVault OTX",
            "RIPE Stat", "crt.sh", "ipwho.is", "VirusTotal", "AbuseIPDB",
            "Pulsedive", "IPQualityScore"
        )
        for (provider in providers) {
            val desc = LocalizationProvider.getProviderDescription(provider, AppLanguage.ENGLISH)
            assertFalse(
                persianRegex.containsMatchIn(desc),
                "Provider '$provider' English description contains Persian characters: '$desc'"
            )
        }
    }

    @Test
    fun testEnglishAboutTabCardTranslationsMatchSpecification() {
        val strings = LocalizationProvider.getStrings(AppLanguage.ENGLISH)

        assertEquals(
            "No intermediate server database exists. Reports and scan history are stored exclusively in encrypted local storage.",
            strings.aboutZeroCloudDesc
        )
        assertEquals(
            "Threat queries are dispatched directly from your device via official HTTPS APIs without middleman proxies (100% client-side).",
            strings.aboutClientDirectDesc
        )
        assertEquals(
            "Real-time detection and blocking of local and private IP ranges (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16) to prevent data leakage.",
            strings.aboutPrivacyGuardDesc
        )
        assertEquals(
            "Real-time aggregation of 10 zero-key intelligence feeds and specialized engines for phishing, malware, and IP reputation triage.",
            strings.aboutThreatIntelDesc
        )
        assertEquals(
            "Replay Interactive Tour & App Guide",
            strings.replayInteractiveTour
        )
        assertEquals(
            "SECURITY CONSOLE // SETTINGS",
            strings.settingsSecurityConsoleBadge
        )
        assertEquals(
            "Privacy Guard (RFC 1918)",
            strings.cardPrivacyGuardTitle
        )
        assertEquals(
            "10 Threat Intel Engines",
            strings.cardThreatIntelTitle
        )
    }

    @Test
    fun testEnglishVerdictNamesContainZeroPersianCharacters() {
        VerdictLevel.entries.forEach { level ->
            val name = LocalizationProvider.getVerdictName(level, AppLanguage.ENGLISH)
            assertFalse(
                persianRegex.containsMatchIn(name),
                "English verdict name for $level contains Persian: '$name'"
            )
        }
    }

    @Test
    fun testEnglishIndicatorTypeNamesContainZeroPersianCharacters() {
        IndicatorType.entries.forEach { type ->
            val name = LocalizationProvider.getIndicatorTypeName(type, AppLanguage.ENGLISH)
            assertFalse(
                persianRegex.containsMatchIn(name),
                "English indicator type name for $type contains Persian: '$name'"
            )
        }
    }

    @Test
    fun testRussianAppStringsContainZeroPersianCharacters() {
        val strings = LocalizationProvider.getStrings(AppLanguage.RUSSIAN)
        val sampleProps = listOf(
            strings.settingsSecurityConsoleBadge,
            strings.aboutZeroCloudDesc,
            strings.aboutClientDirectDesc,
            strings.aboutPrivacyGuardDesc,
            strings.aboutThreatIntelDesc,
            strings.replayInteractiveTour,
            strings.cardPrivacyGuardTitle,
            strings.cardThreatIntelTitle
        )

        sampleProps.forEach { text ->
            assertFalse(
                persianRegex.containsMatchIn(text),
                "Russian string contains Persian characters: '$text'"
            )
        }
    }

    @Test
    fun testPersianSettingsBadgeAndCardsAreProperlyLocalized() {
        val strings = LocalizationProvider.getStrings(AppLanguage.PERSIAN)

        assertEquals("کنسول امنیت // تنظیمات", strings.settingsSecurityConsoleBadge)
        assertEquals("سپر حریم خصوصی (RFC 1918)", strings.cardPrivacyGuardTitle)
        assertEquals("۱۰ موتور تحلیل تهدید", strings.cardThreatIntelTitle)
        assertEquals("مرور راهنمای تعاملی و آموزش برنامه", strings.replayInteractiveTour)
        assertTrue(strings.aboutZeroCloudDesc.contains("هیچ پایگاه داده ابری"))
        assertTrue(strings.aboutClientDirectDesc.contains("۱۰۰٪ کلاینت-ساید"))
    }

    @Test
    fun testBadgeAndLicenseLocalizationAcrossLocales() {
        val en = LocalizationProvider.getStrings(AppLanguage.ENGLISH)
        val fa = LocalizationProvider.getStrings(AppLanguage.PERSIAN)
        val ru = LocalizationProvider.getStrings(AppLanguage.RUSSIAN)

        assertEquals("MIT License Open Source", en.specOpenSource)
        assertEquals("متن‌باز با مجوز MIT", fa.specOpenSource)
        assertEquals("Лицензия MIT (Open Source)", ru.specOpenSource)

        assertEquals("CYBER DARK", en.badgeThemeCyberDark)
        assertEquals("IOC CLASSIC", en.badgeThemeIocClassic)
        assertEquals("AUTO SYSTEM", en.badgeThemeAutoSystem)
        assertEquals("RFC 1918 ACTIVE", en.badgeGuardActive)
        assertEquals("GUARD DISABLED", en.badgeGuardDisabled)
        assertEquals("OPEN SOURCE v1.0", en.badgeOpenSourceVersion)

        assertEquals("تم تاریک سایبری", fa.badgeThemeCyberDark)
        assertEquals("اورجینال IOC LENS", fa.badgeThemeIocClassic)
        assertEquals("پیش‌فرض سیستم", fa.badgeThemeAutoSystem)
        assertEquals("سپر فعال (RFC 1918)", fa.badgeGuardActive)
        assertEquals("سپر غیرفعال", fa.badgeGuardDisabled)
        assertEquals("متن‌باز نسخه ۱.۰", fa.badgeOpenSourceVersion)

        assertEquals("ТЁМНАЯ ТЕМА", ru.badgeThemeCyberDark)
        assertEquals("IOC CLASSIC", ru.badgeThemeIocClassic)
        assertEquals("СИСТЕМНАЯ", ru.badgeThemeAutoSystem)
        assertEquals("RFC 1918 АКТИВЕН", ru.badgeGuardActive)
        assertEquals("ЗАЩИТА ОТКЛЮЧЕНА", ru.badgeGuardDisabled)
        assertEquals("OPEN SOURCE v1.0", ru.badgeOpenSourceVersion)
    }

    @Test
    fun testMobileFirstCopyAndNoDesktopShortcutsAcrossLocales() {
        val en = LocalizationProvider.getStrings(AppLanguage.ENGLISH)
        val fa = LocalizationProvider.getStrings(AppLanguage.PERSIAN)
        val ru = LocalizationProvider.getStrings(AppLanguage.RUSSIAN)

        // Verify (Ctrl+V) desktop shortcut is removed across all locales
        assertEquals("Paste", en.pasteFromClipboard)
        assertEquals("چسباندن", fa.pasteFromClipboard)
        assertEquals("Вставить", ru.pasteFromClipboard)
        assertFalse(en.pasteFromClipboard.contains("Ctrl+V"))
        assertFalse(fa.pasteFromClipboard.contains("Ctrl+V"))
        assertFalse(ru.pasteFromClipboard.contains("Ctrl+V"))

        // Verify mobile file picker copy replaces desktop drop copy
        assertEquals("Select File to Scan", en.dropFileHint)
        assertEquals("انتخاب فایل برای اسکن", fa.dropFileHint)
        assertEquals("Выбрать файл для сканирования", ru.dropFileHint)
        assertFalse(en.dropFileHint.contains("Drop any file"))
        assertFalse(fa.dropFileHint.contains("رهاسازی فایل"))
        assertFalse(ru.dropFileHint.contains("Перетащите файл"))

        // Verify shortened, non-clipping placeholder
        assertEquals("Enter IP, Domain, URL, or Hash...", en.searchFieldPlaceholder)
        assertEquals("آی‌پی، دامنه، نشانی وب یا هش فایل...", fa.searchFieldPlaceholder)
        assertEquals("Введите IP, домен, URL или хеш...", ru.searchFieldPlaceholder)
    }
}

