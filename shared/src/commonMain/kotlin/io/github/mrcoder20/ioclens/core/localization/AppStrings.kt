package io.github.mrcoder20.ioclens.core.localization

import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel

data class AppStrings(
    val appTitle: String,
    val appTagline: String,
    val skip: String,
    val next: String,
    val previous: String,
    val start: String,
    val done: String,
    val changeLanguage: String,
    val searchPlaceholder: String,
    val searchFieldPlaceholder: String,
    val clear: String,
    val analyzeInstant: String,
    val scanPayload: String,
    val cancelScan: String,
    val zeroKeyInstantScanReady: String,
    val streamHashingTitle: String,
    val introGuide: String,
    val enterIndicatorError: String,
    val threatIntelSourcesResults: String,
    val privacyGuardNotice: String,
    val themeLabel: String,
    val languageLabel: String,
    val menuHome: String,
    val menuSettings: String,
    val menuGuide: String,
    val menuProviders: String,
    val menuPrivacyGuard: String,
    val menuAbout: String,
    val settingsTitle: String,
    val appVersion: String,
    val versionLabel: String,
    val systemDefaultTheme: String,
    val darkTheme: String,
    val portxDarkTheme: String,
    val iocLensClassicTheme: String,
    val apiKeysTitle: String,
    val apiKeysSubtitle: String,
    val vtApiKeyLabel: String,
    val abuseApiKeyLabel: String,
    val alienApiKeyLabel: String,
    val pulseApiKeyLabel: String,
    val ipqsApiKeyLabel: String,
    val autoScanClipboard: String,
    val privacyGuardSetting: String,
    val saveKeys: String,
    val savedSuccessfully: String,
    val scanHistoryTitle: String,
    val clearHistory: String,
    val noHistory: String,
    val threatConfidenceScore: String,
    val indicatorTypeLabel: String,
    val threatIntelProvidersTitle: String,
    val enginesCount: String,
    val freeBadge: String,
    val statusReady: String,
    val statusReadyZeroKey: String,
    val statusKeyRequired: String,
    val statusScanning: String,
    val statusRateLimited: String,
    val statusTimedOut: String,
    val statusNotApplicable: String,
    val closeButton: String,
    val streamHashingFailed: String,
    val errorRateLimit: String,
    val errorTimeout: String,
    val errorNetwork: String,
    val errorPrivacyGuard: String,
    val tabScanner: String,
    val tabEngines: String,
    val tabHistory: String,
    val tabSettings: String,
    val liveScanInProgress: String,
    val enginesCompleted: String,
    val sampleIocsTitle: String,
    val sampleGoogleDns: String,
    val sampleMalwareUrl: String,
    val sampleHash: String,
    val pasteFromClipboard: String,
    val dropFileHint: String,
    val recentTriageFeed: String,
    val deleteItem: String,
    val reAnalyze: String,
    val categoryNetwork: String,
    val categoryMalware: String,
    val categoryThreats: String,
    val serverlessStatus: String,
    val systemReadyEngines: String,
    val freeFeedsBadge: String,
    val privacyGuardActiveBadge: String,
    val settingsTabApiKeys: String,
    val settingsTabGeneralPrivacy: String,
    val settingsTabAbout: String,
    val apiKeysZeroKeyBanner: String,
    val vtSubtitle: String,
    val abuseSubtitle: String,
    val alienSubtitle: String,
    val pulseSubtitle: String,
    val ipqsSubtitle: String,
    val keyConfiguredBadge: String,
    val keyOptionalBadge: String,
    val keyInputPlaceholder: String,
    val privacyGuardActiveDesc: String,
    val privacyGuardInactiveDesc: String,
    val badgeClientDirect: String,
    val badgeZeroCloudDb: String,
    val settingsSecurityConsoleBadge: String,
    val cardPrivacyGuardTitle: String,
    val cardThreatIntelTitle: String,
    val aboutZeroCloudDesc: String,
    val aboutClientDirectDesc: String,
    val aboutPrivacyGuardDesc: String,
    val aboutThreatIntelDesc: String,
    val replayInteractiveTour: String,
    val specStandalonePortable: String,
    val specZeroTelemetry: String,
    val specOpenSource: String,
    val specMultiplatformEngine: String,
    val statusKeyRequiredShort: String,
    val badgeThemeCyberDark: String,
    val badgeThemeIocClassic: String,
    val badgeThemeAutoSystem: String,
    val badgeGuardActive: String,
    val badgeGuardDisabled: String,
    val badgeOpenSourceVersion: String,
    val searchHistoryPlaceholder: String,
    val filterAll: String,
    val noFilterResults: String,
    val enginesZeroKeySubtitle: String,
    val visitWebsite: String,
    val copyWebsiteUrl: String,
    val websiteUrlCopied: String,
    val filterByIocType: String,
    val githubRepoUrl: String,
    val openGithubRepo: String,
    val copyGithubUrl: String,
    val githubUrlCopied: String,
    val clientManifestoTitle: String,
    val clientManifestoDesc: String,
    val replayGuideTitle: String
)

object LocalizationProvider {

    fun toPersianDigits(input: String): String {
        val faDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        return input.map { ch ->
            if (ch in '0'..'9') faDigits[ch - '0'] else ch
        }.joinToString("")
    }

    fun formatNumber(number: Int, lang: AppLanguage): String {
        return if (lang == AppLanguage.PERSIAN) {
            toPersianDigits(number.toString())
        } else {
            number.toString()
        }
    }

    fun formatScore(score: Int, lang: AppLanguage): String {
        return if (lang == AppLanguage.PERSIAN) {
            "${toPersianDigits(score.toString())}٪"
        } else {
            "$score%"
        }
    }

    fun getVerdictName(level: VerdictLevel, lang: AppLanguage): String {
        return when (lang) {
            AppLanguage.PERSIAN -> when (level) {
                VerdictLevel.CLEAN -> "پاک / امن"
                VerdictLevel.LOW_RISK -> "کم‌خطر"
                VerdictLevel.SUSPICIOUS -> "مشکوک"
                VerdictLevel.MALICIOUS -> "مخرب / آلوده"
                VerdictLevel.NO_DATA -> "بدون داده"
            }
            AppLanguage.RUSSIAN -> when (level) {
                VerdictLevel.CLEAN -> "БЕЗОПАСНО"
                VerdictLevel.LOW_RISK -> "НИЗКИЙ РИСК"
                VerdictLevel.SUSPICIOUS -> "ПОДОЗРИТЕЛЬНО"
                VerdictLevel.MALICIOUS -> "ВРЕДОНОСНО"
                VerdictLevel.NO_DATA -> "НЕТ ДАННЫХ"
            }
            AppLanguage.ENGLISH -> when (level) {
                VerdictLevel.CLEAN -> "CLEAN"
                VerdictLevel.LOW_RISK -> "LOW RISK"
                VerdictLevel.SUSPICIOUS -> "SUSPICIOUS"
                VerdictLevel.MALICIOUS -> "MALICIOUS"
                VerdictLevel.NO_DATA -> "NO DATA"
            }
        }
    }

    fun getIndicatorTypeName(type: IndicatorType, lang: AppLanguage): String {
        return when (lang) {
            AppLanguage.PERSIAN -> when (type) {
                IndicatorType.IP_ADDRESS -> "آدرس IP"
                IndicatorType.DOMAIN -> "دامنه اینترنتی"
                IndicatorType.URL -> "نشانی وب (URL)"
                IndicatorType.FILE_HASH -> "هش فایل"
                IndicatorType.UNKNOWN -> "نامشخص"
            }
            AppLanguage.RUSSIAN -> when (type) {
                IndicatorType.IP_ADDRESS -> "IP-адрес"
                IndicatorType.DOMAIN -> "Домен"
                IndicatorType.URL -> "URL-адрес"
                IndicatorType.FILE_HASH -> "Хеш файла"
                IndicatorType.UNKNOWN -> "Неизвестно"
            }
            AppLanguage.ENGLISH -> when (type) {
                IndicatorType.IP_ADDRESS -> "IP Address"
                IndicatorType.DOMAIN -> "Domain"
                IndicatorType.URL -> "URL"
                IndicatorType.FILE_HASH -> "File Hash"
                IndicatorType.UNKNOWN -> "Unknown"
            }
        }
    }

    fun getLocalizedVerdictSummary(verdict: TriageVerdict, lang: AppLanguage): String {
        val indicator = verdict.indicator
        val sources = verdict.sourceResults

        if (indicator.isInternalOrPrivate) {
            return when (lang) {
                AppLanguage.PERSIAN -> "🔒 محافظ حریم خصوصی فعال شد: نشانگر داخلی یا خصوصی شناسایی شد. استعلام خارجی جهت محافظت از شبکه مسدود گردید."
                AppLanguage.RUSSIAN -> "🔒 Privacy Guard активен: обнаружен внутренний индикатор. Запросы заблокированы."
                AppLanguage.ENGLISH -> "🔒 Privacy Guard Activated: Private/Internal indicator detected. Remote lookups blocked to protect internal infrastructure."
            }
        }

        if (indicator.type == IndicatorType.UNKNOWN) {
            return when (lang) {
                AppLanguage.PERSIAN -> "☣️ فرمت نشانگر شناسایی نشد. لطفاً یک آدرس IP، دامنه، URL یا هش فایل معتبر وارد نمایید."
                AppLanguage.RUSSIAN -> "☣️ Нераспознанный формат индикатора. Введите корректный IP, домен, URL или хеш файла."
                AppLanguage.ENGLISH -> "☣️ Unrecognized indicator format. Please provide a valid IP, Domain, URL, or File Hash."
            }
        }

        val maliciousSources = sources.filter { it.verdict == VerdictLevel.MALICIOUS }
        val suspiciousSources = sources.filter { it.verdict == VerdictLevel.SUSPICIOUS }
        val lowRiskSources = sources.filter { it.verdict == VerdictLevel.LOW_RISK }
        val cleanSources = sources.filter { it.verdict == VerdictLevel.CLEAN }

        val malNames = maliciousSources.joinToString(", ") { it.providerName }
        val suspNames = suspiciousSources.joinToString(", ") { it.providerName }

        return when (lang) {
            AppLanguage.PERSIAN -> when {
                maliciousSources.isNotEmpty() -> "💀 پی‌لود آلوده شناسایی شد: تایید شده توسط $malNames."
                suspiciousSources.isNotEmpty() -> "☣️ فعالیت مشکوک: هشدارهای تهدید توسط $suspNames ثبت گردید."
                lowRiskSources.isNotEmpty() -> "⚠️ ریسک پایین: سیگنال‌های تله‌متری جزئی گزارش شده اما امضای بدافزار فعالی تایید نشد."
                cleanSources.isNotEmpty() -> "🛡️ پاک و بدون تهدید: ${toPersianDigits(cleanSources.size.toString())} موتور تحلیلی هیچ‌گونه فعالیت مخربی مشاهده نکردند."
                else -> "🗄️ هیچ سابقه تهدیدی در موتورهای فعال عمومی یافت نشد."
            }
            AppLanguage.RUSSIAN -> when {
                maliciousSources.isNotEmpty() -> "💀 ОБНАРУЖЕНА ВРЕДОНОСНАЯ АКТИВНОСТЬ: подтверждено $malNames."
                suspiciousSources.isNotEmpty() -> "☣️ Подозрительная активность: обнаружены сигналы угроз от $suspNames."
                lowRiskSources.isNotEmpty() -> "⚠️ Низкий риск: зафиксированы незначительные аномалии, активных сигнатур угроз нет."
                cleanSources.isNotEmpty() -> "🛡️ Проверено: ${cleanSources.size} сервисов threat intelligence не выявили угроз."
                else -> "🗄️ Записей об угрозах в активных базах данных не обнаружено."
            }
            AppLanguage.ENGLISH -> verdict.summaryDescription
        }
    }

    fun getProviderDisplayName(name: String, lang: AppLanguage): String {
        return when (lang) {
            AppLanguage.PERSIAN -> when (name) {
                "URLhaus (abuse.ch)" -> "یوآرال‌هاوس (URLhaus)"
                "MalwareBazaar (abuse.ch)" -> "بدافزاربازار (MalwareBazaar)"
                "ThreatFox (abuse.ch)" -> "ترت‌فاکس (ThreatFox)"
                "RDAP / Registry" -> "ثبت‌کننده دامنه و آی‌پی (RDAP)"
                "Cloudflare DoH" -> "کلودفلر DoH (دی‌ان‌اس امن)"
                "Shodan InternetDB" -> "شودان اینترنت دی‌بی (Shodan)"
                "AlienVault OTX" -> "الین‌والت OTX (AlienVault)"
                "RIPE Stat" -> "رایپ استت (RIPE Stat)"
                "crt.sh" -> "لاگ شفافیت گواهینامه (crt.sh)"
                "Feodo Tracker (abuse.ch)" -> "فئودو ترکر (Feodo Tracker)"
                "ipwho.is" -> "آی‌پی هوایز (ipwho.is)"
                "VirusTotal" -> "ویروس‌توتال (VirusTotal)"
                "AbuseIPDB" -> "ابوز آی‌پی‌دی‌بی (AbuseIPDB)"
                "Pulsedive" -> "پالس‌دایو (Pulsedive)"
                "IPQualityScore" -> "آی‌پی کوالیتی‌اسکور (IPQS)"
                else -> name
            }
            AppLanguage.RUSSIAN -> when (name) {
                "RDAP / Registry" -> "RDAP / Реестр"
                "crt.sh" -> "crt.sh (Сертификаты)"
                "Feodo Tracker (abuse.ch)" -> "Feodo Tracker (Ботнеты C2)"
                "ipwho.is" -> "ipwho.is (Гео и ISP)"
                else -> name
            }
            AppLanguage.ENGLISH -> name
        }
    }

    fun getProviderSupportedTypes(name: String): List<IndicatorType> {
        return when (name) {
            "URLhaus (abuse.ch)" -> listOf(IndicatorType.URL, IndicatorType.DOMAIN)
            "MalwareBazaar (abuse.ch)" -> listOf(IndicatorType.FILE_HASH)
            "ThreatFox (abuse.ch)" -> listOf(IndicatorType.IP_ADDRESS, IndicatorType.DOMAIN, IndicatorType.URL, IndicatorType.FILE_HASH)
            "RDAP / Registry" -> listOf(IndicatorType.IP_ADDRESS, IndicatorType.DOMAIN)
            "Cloudflare DoH" -> listOf(IndicatorType.DOMAIN)
            "Shodan InternetDB" -> listOf(IndicatorType.IP_ADDRESS)
            "AlienVault OTX" -> listOf(IndicatorType.IP_ADDRESS, IndicatorType.DOMAIN, IndicatorType.URL, IndicatorType.FILE_HASH)
            "RIPE Stat" -> listOf(IndicatorType.IP_ADDRESS)
            "crt.sh" -> listOf(IndicatorType.DOMAIN)
            "Feodo Tracker (abuse.ch)" -> listOf(IndicatorType.IP_ADDRESS)
            "ipwho.is" -> listOf(IndicatorType.IP_ADDRESS)
            "VirusTotal" -> listOf(IndicatorType.IP_ADDRESS, IndicatorType.DOMAIN, IndicatorType.URL, IndicatorType.FILE_HASH)
            "AbuseIPDB" -> listOf(IndicatorType.IP_ADDRESS)
            "Pulsedive" -> listOf(IndicatorType.IP_ADDRESS, IndicatorType.DOMAIN, IndicatorType.URL, IndicatorType.FILE_HASH)
            "IPQualityScore" -> listOf(IndicatorType.IP_ADDRESS)
            else -> listOf(IndicatorType.UNKNOWN)
        }
    }

    fun getProviderWebsiteUrl(name: String): String {
        return when (name) {
            "URLhaus (abuse.ch)" -> "https://urlhaus.abuse.ch"
            "MalwareBazaar (abuse.ch)" -> "https://bazaar.abuse.ch"
            "ThreatFox (abuse.ch)" -> "https://threatfox.abuse.ch"
            "RDAP / Registry" -> "https://about.rdap.org"
            "Cloudflare DoH" -> "https://cloudflare-dns.com"
            "Shodan InternetDB" -> "https://internetdb.shodan.io"
            "AlienVault OTX" -> "https://otx.alienvault.com"
            "RIPE Stat" -> "https://stat.ripe.net"
            "crt.sh" -> "https://crt.sh"
            "Feodo Tracker (abuse.ch)" -> "https://feodotracker.abuse.ch"
            "ipwho.is" -> "https://ipwho.is"
            "VirusTotal" -> "https://www.virustotal.com"
            "AbuseIPDB" -> "https://www.abuseipdb.com"
            "Pulsedive" -> "https://pulsedive.com"
            "IPQualityScore" -> "https://www.ipqualityscore.com"
            else -> "https://abuse.ch"
        }
    }

    fun getProviderDomain(name: String): String {
        return when (name) {
            "URLhaus (abuse.ch)" -> "urlhaus.abuse.ch"
            "MalwareBazaar (abuse.ch)" -> "bazaar.abuse.ch"
            "ThreatFox (abuse.ch)" -> "threatfox.abuse.ch"
            "RDAP / Registry" -> "about.rdap.org"
            "Cloudflare DoH" -> "cloudflare-dns.com"
            "Shodan InternetDB" -> "internetdb.shodan.io"
            "AlienVault OTX" -> "otx.alienvault.com"
            "RIPE Stat" -> "stat.ripe.net"
            "crt.sh" -> "crt.sh"
            "Feodo Tracker (abuse.ch)" -> "feodotracker.abuse.ch"
            "ipwho.is" -> "ipwho.is"
            "VirusTotal" -> "virustotal.com"
            "AbuseIPDB" -> "abuseipdb.com"
            "Pulsedive" -> "pulsedive.com"
            "IPQualityScore" -> "ipqualityscore.com"
            else -> "abuse.ch"
        }
    }

    fun getProviderDescription(name: String, lang: AppLanguage): String {
        return when (lang) {
            AppLanguage.PERSIAN -> when (name) {
                "URLhaus (abuse.ch)" -> "سامانه اشتراک‌گذاری آدرس‌های وب آلوده به بدافزار توسط abuse.ch."
                "MalwareBazaar (abuse.ch)" -> "مخزن اشتراک نمونه‌های بدافزار و هشدارهای هش رمزنگاری توسط abuse.ch."
                "ThreatFox (abuse.ch)" -> "پایگاه داده جمعی نشانگرهای تهدید و سرنخ‌های نفوذ (IOC) توسط abuse.ch."
                "RDAP / Registry" -> "پروتکل رسمی دسترسی به داده‌های ثبت دامنه، آی‌پی و Autonomous System."
                "Cloudflare DoH" -> "تحلیل‌گر امن دی‌ان‌اس بر بستر HTTPS برای شناسایی وضعیت دامنه‌ها."
                "Shodan InternetDB" -> "پویشگر مستقیم پورت‌های باز، آسیب‌پذیری‌های CVE و نام‌های دامنه مرتبط بدون نیاز به کلید."
                "AlienVault OTX" -> "شبکه تبادل آزاد اطلاعات تهدیدات سایبری و پالس‌های جامعه کاربری بدون نیاز به کلید."
                "RIPE Stat" -> "تله‌متری مسیریابی اینترنت، ثبت نام سیستم خودگردان (ASN) و پیشوندهای شبکه RIPE."
                "crt.sh" -> "جستجوی سوابق لاگ شفافیت گواهینامه‌های SSL/TLS و کشف خودکار زیردامنه‌ها."
                "Feodo Tracker (abuse.ch)" -> "ردیاب و مسدودکننده سرورهای فرماندهی و کنترل بات‌نت‌ها (C2) توسط abuse.ch."
                "ipwho.is" -> "موقعیت‌یابی جغرافیایی آی‌پی، اطلاعات ارائه‌دهنده اینترنت (ISP) و بررسی اتصال پروکسی، VPN و تور."
                "VirusTotal" -> "تلفیق تحلیل تهدید و پویش بیش از ۷۰ موتور آنتی‌ویروس و امنیت وب."
                "AbuseIPDB" -> "پایگاه رتبه‌بندی شهرت نشانی‌های آی‌پی و رهگیری فعالیت‌های خرابکارانه و اسپم."
                "Pulsedive" -> "بستر هوش تهدیدات ارائه دهنده امتیاز ریسک و روابط نشانگرهای آلودگی."
                "IPQualityScore" -> "سیستم پیشگیری از تقلب و شناسایی بی‌درنگ پروکسی، VPN و آی‌پی‌های مشکوک."
                else -> name
            }
            AppLanguage.RUSSIAN -> when (name) {
                "URLhaus (abuse.ch)" -> "Платформа сбора и обмена вредоносными URL-адресами от abuse.ch."
                "MalwareBazaar (abuse.ch)" -> "Репозиторий вредоносных файлов и криптографических хэшей от abuse.ch."
                "ThreatFox (abuse.ch)" -> "База данных актуальных индикаторов компрометации (IOC) от abuse.ch."
                "RDAP / Registry" -> "Протокол доступа к регистрационным данным доменов, IP и номеров ASN."
                "Cloudflare DoH" -> "Криптографический резолвер DNS-over-HTTPS для проверки доменов."
                "Shodan InternetDB" -> "Бесплатная база данных Shodan для проверки открытых портов, уязвимостей (CVE) и хостов."
                "AlienVault OTX" -> "Открытая сеть обмена данными об угрозах и индикаторах компрометации (OTX Pulses)."
                "RIPE Stat" -> "Маршрутная телеметрия, автономные системы (ASN) и префиксы от координационного центра RIPE NCC."
                "crt.sh" -> "Поиск по логам прозрачности сертификатов (CT Logs) и обнаружение поддоменов."
                "Feodo Tracker (abuse.ch)" -> "Отслеживание серверов управления (C2) ботнетами Dridex, Emotet и QakBot от abuse.ch."
                "ipwho.is" -> "Геолокация IP-адресов, метаданные провайдеров (ISP) и выявление прокси/VPN/TOR."
                "VirusTotal" -> "Агрегированное сканирование более 70 антивирусных движков и баз угроз."
                "AbuseIPDB" -> "База репутации IP-адресов для отслеживания атак и спам-активности."
                "Pulsedive" -> "Платформа threat intelligence с оценкой рисков и связями индикаторов."
                "IPQualityScore" -> "Оценка репутации IP, выявление прокси, VPN, TOR и мошеннической активности."
                else -> name
            }
            AppLanguage.ENGLISH -> when (name) {
                "URLhaus (abuse.ch)" -> "Community-driven malware URL sharing and threat feed platform by abuse.ch."
                "MalwareBazaar (abuse.ch)" -> "Malware sample exchange and cryptographic hash intelligence repository by abuse.ch."
                "ThreatFox (abuse.ch)" -> "Crowdsourced database of actionable Indicators of Compromise (IOCs) by abuse.ch."
                "RDAP / Registry" -> "Authoritative Registration Data Access Protocol for domain, IP, and ASN registration data."
                "Cloudflare DoH" -> "Cryptographically authenticated DNS-over-HTTPS resolver for live domain query resolution."
                "Shodan InternetDB" -> "Zero-Key host intelligence indexing open ports, vulnerabilities (CVEs), and hostnames."
                "AlienVault OTX" -> "Open Threat Exchange public pulse and indicator reputation lookups without authentication."
                "RIPE Stat" -> "Authoritative RIPE NCC routing telemetry, Autonomous System Number (ASN), and prefix data."
                "crt.sh" -> "Certificate Transparency log search for active SSL/TLS certificates and subdomain discovery."
                "Feodo Tracker (abuse.ch)" -> "Botnet Command & Control (C2) tracker and blocklist provided by abuse.ch."
                "ipwho.is" -> "Zero-Key IP geolocation, ISP metadata, and VPN/TOR proxy risk triage."
                "VirusTotal" -> "Aggregated threat telemetry and scanning across 70+ antivirus and domain security engines."
                "AbuseIPDB" -> "Global IP reputation database tracking abusive network activity, spamming, and brute-force attacks."
                "Pulsedive" -> "Threat intelligence platform offering community risk scores, threat feeds, and IOC graphs."
                "IPQualityScore" -> "Real-time fraud scoring, proxy/VPN/TOR detection, and IP reputation intelligence."
                else -> name
            }
        }
    }

    fun getLocalizedSourceSummary(summary: String, lang: AppLanguage): String {
        if (summary.isBlank()) return summary
        return when (lang) {
            AppLanguage.PERSIAN -> when {
                summary.contains("Internal indicator safely blocked locally", ignoreCase = true) ->
                    "نشانگر داخلی با موفقیت در لایه محلی مسدود شد"
                summary.contains("No active malware payloads recorded in URLhaus", ignoreCase = true) ->
                    "هیچ پی‌لود بدافزار فعالی در URLhaus ثبت نشده است"
                summary.contains("Hash not observed in MalwareBazaar repository", ignoreCase = true) ->
                    "این هش در مخزن بدافزار MalwareBazaar یافت نشد"
                summary.contains("No IOC records associated in ThreatFox", ignoreCase = true) ->
                    "هیچ رکورد تهدیدی در ThreatFox ثبت نشده است"
                summary.contains("Unregistered or no RDAP record", ignoreCase = true) ->
                    "دامنه/آی‌پی ثبت‌نشده یا فاقد رکورد RDAP"
                summary.contains("DNS Resolution active: NOERROR", ignoreCase = true) ->
                    "تفکیک موفقیت‌آمیز DNS فعال (وضعیت: NOERROR)"
                summary.contains("DNS NXDOMAIN", ignoreCase = true) ->
                    "دامنه در DNS وجود ندارد (NXDOMAIN)"
                summary.contains("API Key required", ignoreCase = true) ->
                    "نیازمند کلید API (در تنظیمات وارد کنید)"
                summary.contains("0 threat pulses found in AlienVault OTX", ignoreCase = true) ->
                    "۰ پالس تهدید در شبکه AlienVault OTX یافت شد"
                summary.contains("active threat intelligence pulses recorded", ignoreCase = true) ->
                    toPersianDigits(summary).replace("active threat intelligence pulses recorded", "پالس فعال هوش تهدیدات ثبت شده است")
                summary.contains("No open ports or known vulnerabilities found", ignoreCase = true) ->
                    "هیچ پورت باز یا آسیب‌پذیری شناخته‌شده‌ای در شودان ثبت نشده است"
                summary.contains("Shodan:", ignoreCase = true) ->
                    toPersianDigits(summary)
                        .replace("Shodan:", "شودان:")
                        .replace("open ports", "پورت باز")
                        .replace("CVEs", "آسیب‌پذیری CVE")
                summary.contains("RIPE Stat Routing:", ignoreCase = true) ->
                    toPersianDigits(summary).replace("RIPE Stat Routing:", "مسیریابی RIPE Stat:").replace("Prefix:", "پیشوند:")
                summary.contains("Certificate Transparency:", ignoreCase = true) ->
                    toPersianDigits(summary).replace("Certificate Transparency:", "شفافیت گواهی:").replace("logged certificates discovered", "گواهی ثبت‌شده کشف شد")
                summary.contains("No SSL certificates logged", ignoreCase = true) ->
                    "هیچ گواهینامه SSL در لاگ‌های شفافیت ثبت نشده است"
                summary.contains("No active C2 botnets recorded in Feodo Tracker", ignoreCase = true) ->
                    "هیچ سرور فعال بات‌نت C2 در Feodo Tracker ثبت نشده است"
                summary.contains("Botnet C2 host observed in Feodo Tracker", ignoreCase = true) ->
                    toPersianDigits(summary).replace("Botnet C2 host observed in Feodo Tracker", "میزبان سرور فرماندهی بات‌نت در Feodo Tracker شناسایی شد")
                summary.contains("Active TOR exit node", ignoreCase = true) ->
                    toPersianDigits(summary).replace("ipwho.is: Active TOR exit node", "نود خروجی فعال شبکه تور (TOR)")
                summary.contains("Proxy/VPN anonymous endpoint", ignoreCase = true) ->
                    toPersianDigits(summary).replace("ipwho.is: Proxy/VPN anonymous endpoint", "نقطه اتصال ناشناس پروکسی / VPN")
                summary.contains("Clean verified ISP connection", ignoreCase = true) ->
                    toPersianDigits(summary).replace("ipwho.is: Clean verified ISP connection", "اتصال پاک و تایید شده ارائه‌دهنده اینترنت")
                summary.contains("No records found in VirusTotal database", ignoreCase = true) ->
                    "رکوردی در پایگاه داده ویروس‌توتال ثبت نشده است"
                summary.contains("Rate limit exceeded", ignoreCase = true) ->
                    "محدودیت نرخ درخواست (۴۲۹): سهمیه به اتمام رسید"
                summary.contains("timed out", ignoreCase = true) ->
                    "پایان مهلت زمانی ارتباط با سرور"
                summary.contains("Network connection failure", ignoreCase = true) ||
                summary.contains("Service unavailable or filtered", ignoreCase = true) ->
                    "سرویس در دسترس نیست یا فیلتر شده است"
                summary.contains("vendors flagged as malicious", ignoreCase = true) ||
                summary.contains("engines detected malicious", ignoreCase = true) ->
                    toPersianDigits(summary)
                        .replace("vendors flagged as malicious", "فروشنده امنیتی این نشانگر را آلوده شناسایی کردند")
                        .replace("engines detected malicious", "موتور این نشانگر را آلوده شناسایی کردند")
                        .replace("suspicious", "مشکوک")
                        .replace("(out of", "(از مجموع")
                summary.contains("Abuse confidence score", ignoreCase = true) ->
                    toPersianDigits(summary)
                        .replace("Abuse confidence score:", "شاخص سوءاستفاده:")
                        .replace("Abuse Confidence Score:", "شاخص سوءاستفاده:")
                        .replace("reports in last 90 days", "گزارش در ۹۰ روز اخیر")
                        .replace("%", "٪")
                summary.contains("Fraud Score:", ignoreCase = true) ->
                    toPersianDigits(summary).replace("Fraud Score:", "شاخص تقلب:")
                summary.contains("RDAP Network Allocation:", ignoreCase = true) ->
                    toPersianDigits(summary).replace("RDAP Network Allocation:", "تخصیص شبکه RDAP:")
                else -> toPersianDigits(summary)
            }
            AppLanguage.RUSSIAN -> when {
                summary.contains("Internal indicator safely blocked locally", ignoreCase = true) ->
                    "Внутренний индикатор безопасно заблокирован локально"
                summary.contains("No active malware payloads recorded in URLhaus", ignoreCase = true) ->
                    "Активных вредоносных программ в URLhaus не обнаружено"
                summary.contains("Hash not observed in MalwareBazaar repository", ignoreCase = true) ->
                    "Хеш не найден в репозитории MalwareBazaar"
                summary.contains("No IOC records associated in ThreatFox", ignoreCase = true) ->
                    "Записей об угрозах в ThreatFox не найдено"
                summary.contains("Unregistered or no RDAP record", ignoreCase = true) ->
                    "Не зарегистрирован или отсутствует запись RDAP"
                summary.contains("DNS Resolution active: NOERROR", ignoreCase = true) ->
                    "Успешное разрешение DNS (Статус: NOERROR)"
                summary.contains("DNS NXDOMAIN", ignoreCase = true) ->
                    "Домен не существует в DNS (NXDOMAIN)"
                summary.contains("API Key required", ignoreCase = true) ->
                    "Требуется ключ API (настройте в Настройках)"
                summary.contains("0 threat pulses found in AlienVault OTX", ignoreCase = true) ->
                    "0 импульсов угроз в AlienVault OTX"
                summary.contains("active threat intelligence pulses recorded", ignoreCase = true) ->
                    summary.replace("active threat intelligence pulses recorded", "активных импульсов угроз зафиксировано")
                summary.contains("No open ports or known vulnerabilities found", ignoreCase = true) ->
                    "В Shodan не найдено открытых портов или уязвимостей"
                summary.contains("Shodan:", ignoreCase = true) ->
                    summary
                        .replace("open ports", "открытых портов")
                        .replace("CVEs", "уязвимостей CVE")
                summary.contains("RIPE Stat Routing:", ignoreCase = true) ->
                    summary.replace("RIPE Stat Routing:", "Маршрутизация RIPE Stat:").replace("Prefix:", "Префикс:")
                summary.contains("Certificate Transparency:", ignoreCase = true) ->
                    summary.replace("Certificate Transparency:", "Прозрачность сертификатов:").replace("logged certificates discovered", "сертификатов обнаружено")
                summary.contains("No SSL certificates logged", ignoreCase = true) ->
                    "Сертификаты SSL в логах прозрачности не найдены"
                summary.contains("No active C2 botnets recorded in Feodo Tracker", ignoreCase = true) ->
                    "Активных C2-серверов ботнетов в Feodo Tracker не зафиксировано"
                summary.contains("Botnet C2 host observed in Feodo Tracker", ignoreCase = true) ->
                    summary.replace("Botnet C2 host observed in Feodo Tracker", "Узел управления ботнетом (C2) обнаружен в Feodo Tracker")
                summary.contains("Active TOR exit node", ignoreCase = true) ->
                    summary.replace("ipwho.is: Active TOR exit node", "Активный выходной узел сети TOR")
                summary.contains("Proxy/VPN anonymous endpoint", ignoreCase = true) ->
                    summary.replace("ipwho.is: Proxy/VPN anonymous endpoint", "Анонимная конечная точка прокси/VPN")
                summary.contains("Clean verified ISP connection", ignoreCase = true) ->
                    summary.replace("ipwho.is: Clean verified ISP connection", "Чистое подтвержденное подключение провайдера")
                summary.contains("No records found in VirusTotal database", ignoreCase = true) ->
                    "Записей в базе VirusTotal не обнаружено"
                summary.contains("Rate limit exceeded", ignoreCase = true) ->
                    "Превышен лимит запросов API (HTTP 429)"
                summary.contains("timed out", ignoreCase = true) ->
                    "Тайм-аут запроса к поставщику аналитики"
                summary.contains("Network connection failure", ignoreCase = true) ||
                summary.contains("Service unavailable or filtered", ignoreCase = true) ->
                    "Сбой сетевого подключения или сервис недоступен"
                summary.contains("vendors flagged as malicious", ignoreCase = true) ||
                summary.contains("engines detected malicious", ignoreCase = true) ->
                    summary
                        .replace("vendors flagged as malicious", "поставщиков безопасности определили как вредоносный")
                        .replace("engines detected malicious", "движков определили как вредоносный")
                        .replace("suspicious", "подозрительно")
                        .replace("(out of", "(из")
                summary.contains("Abuse confidence score", ignoreCase = true) ->
                    summary
                        .replace("Abuse confidence score:", "Индекс злоупотреблений:")
                        .replace("Abuse Confidence Score:", "Индекс злоупотреблений:")
                        .replace("reports in last 90 days", "отчетов за последние 90 дней")
                summary.contains("Fraud Score:", ignoreCase = true) ->
                    summary.replace("Fraud Score:", "Индекс мошенничества:")
                summary.contains("RDAP Network Allocation:", ignoreCase = true) ->
                    summary.replace("RDAP Network Allocation:", "Выделение сети RDAP:")
                else -> summary
            }
            AppLanguage.ENGLISH -> summary
        }
    }

    fun getStrings(language: AppLanguage): AppStrings {
        return when (language) {
            AppLanguage.ENGLISH -> AppStrings(
                appTitle = "IOC Lens",
                appTagline = "Instant, privacy-first threat-indicator triage for defenders",
                skip = "Skip",
                next = "Next",
                previous = "Previous",
                start = "Get Started",
                done = "DONE",
                changeLanguage = "Language",
                searchPlaceholder = "Enter an IP, domain, URL or file hash...",
                searchFieldPlaceholder = "Enter IP, Domain, URL, or Hash...",
                clear = "Clear",
                analyzeInstant = "Instant Lookup",
                scanPayload = "SCAN PAYLOAD",
                cancelScan = "CANCEL",
                zeroKeyInstantScanReady = "Zero-Key instant scan ready",
                streamHashingTitle = "Stream Hashing (64KB chunks)",
                introGuide = "Guide / Intro",
                enterIndicatorError = "Please enter an IP address, domain, URL, or file hash.",
                threatIntelSourcesResults = "Threat Intel Sources Results:",
                privacyGuardNotice = "🛡️ Privacy Guard Activated: Private/Internal indicator detected. Lookup blocked.",
                themeLabel = "Theme",
                languageLabel = "Language",
                menuHome = "Home / Triage",
                menuSettings = "App Settings",
                menuGuide = "Intro Guide",
                menuProviders = "Threat Feeds",
                menuPrivacyGuard = "Privacy Guard",
                menuAbout = "About IOC Lens",
                settingsTitle = "Settings",
                appVersion = "App Version",
                versionLabel = "v1.0.0 (Open Source)",
                systemDefaultTheme = "System Default",
                darkTheme = "Signature Dark Theme",
                portxDarkTheme = "Cyber PortX Theme",
                iocLensClassicTheme = "IOC Lens Classic Theme",
                apiKeysTitle = "Advanced Custom API Keys (Optional)",
                apiKeysSubtitle = "⚡ Built-in Zero-Key threat intelligence feeds are fully active. You may optionally configure custom private API keys below:",
                vtApiKeyLabel = "VirusTotal API Key (Optional)",
                abuseApiKeyLabel = "AbuseIPDB API Key (Optional)",
                alienApiKeyLabel = "AlienVault OTX API Key (Optional)",
                pulseApiKeyLabel = "Pulsedive API Key (Optional)",
                ipqsApiKeyLabel = "IPQualityScore API Key (Optional)",
                autoScanClipboard = "Auto-scan Clipboard",
                privacyGuardSetting = "Internal Indicator Guard",
                saveKeys = "Save Settings 💾",
                savedSuccessfully = "Settings updated successfully!",
                scanHistoryTitle = "Scan History",
                clearHistory = "Clear",
                noHistory = "No scan history yet. Scan an indicator or share a file to get started.",
                threatConfidenceScore = "Threat Confidence Score",
                indicatorTypeLabel = "Type",
                threatIntelProvidersTitle = "THREAT INTEL PROVIDERS",
                enginesCount = "10 Engines",
                freeBadge = "● Free",
                statusReady = "Ready",
                statusReadyZeroKey = "Ready (Zero-Key)",
                statusKeyRequired = "Key Required",
                statusScanning = "Scanning...",
                statusRateLimited = "Rate Limited",
                statusTimedOut = "Timed Out",
                statusNotApplicable = "N/A for Type",
                closeButton = "Close",
                streamHashingFailed = "Stream hashing failed",
                errorRateLimit = "Rate limit exceeded on external intelligence services. Please retry shortly.",
                errorTimeout = "Timeout contacting threat intelligence feeds. Check your internet connection.",
                errorNetwork = "Failed to analyze indicator. Check your network connection.",
                errorPrivacyGuard = "Private/Internal indicator blocked by Privacy Guard.",
                tabScanner = "Scanner",
                tabEngines = "Engines",
                tabHistory = "History",
                tabSettings = "Settings",
                liveScanInProgress = "Live engine scan in progress...",
                enginesCompleted = "engines complete",
                sampleIocsTitle = "Quick Samples:",
                sampleGoogleDns = "8.8.8.8 (Google DNS)",
                sampleMalwareUrl = "malware-test.com (Threat URL)",
                sampleHash = "WannaCry Hash",
                pasteFromClipboard = "Paste",
                dropFileHint = "Select File to Scan",
                recentTriageFeed = "Recent Scans Feed",
                deleteItem = "Delete",
                reAnalyze = "Re-Scan",
                categoryNetwork = "Network & DNS Infrastructure",
                categoryMalware = "Malware & Binary Repositories",
                categoryThreats = "Threat Feeds & Botnet C2",
                serverlessStatus = "Serverless Direct Client · Zero Database · On-Device Privacy Guard (RFC 1918)",
                systemReadyEngines = "10 Engines Ready",
                freeFeedsBadge = "10 Zero-Key Feeds",
                privacyGuardActiveBadge = "Privacy Guard Active",
                settingsTabApiKeys = "Advanced Keys (Optional)",
                settingsTabGeneralPrivacy = "General & Privacy",
                settingsTabAbout = "About",
                apiKeysZeroKeyBanner = "10 threat intelligence engines active without API keys",
                vtSubtitle = "70+ Antivirus & Sandbox Engines (v3 API)",
                abuseSubtitle = "IP Reputation & Abuse Database (v2 API)",
                alienSubtitle = "Global Threat Pulses & IOC Database (v1 API)",
                pulseSubtitle = "Indicator Risk Scoring & Feed Intelligence",
                ipqsSubtitle = "Fraud Scoring, Proxy & Bot Detection API",
                keyConfiguredBadge = "● Configured",
                keyOptionalBadge = "○ Optional",
                keyInputPlaceholder = "Paste API key token here...",
                privacyGuardActiveDesc = "Client-Side Zero-Leak: Queries for RFC 1918 private intranet (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16) and 127.0.0.1 are intercepted locally without outbound traffic.",
                privacyGuardInactiveDesc = "Private IP Guard inactive: Local intranet and loopback addresses will not be filtered automatically.",
                badgeClientDirect = "⚡ 100% Client-Direct",
                badgeZeroCloudDb = "🛡️ Zero Cloud Database",
                settingsSecurityConsoleBadge = "SECURITY CONSOLE // SETTINGS",
                cardPrivacyGuardTitle = "Privacy Guard (RFC 1918)",
                cardThreatIntelTitle = "10 Threat Intel Engines",
                aboutZeroCloudDesc = "No intermediate server database exists. Reports and scan history are stored exclusively in encrypted local storage.",
                aboutClientDirectDesc = "Threat queries are dispatched directly from your device via official HTTPS APIs without middleman proxies (100% client-side).",
                aboutPrivacyGuardDesc = "Real-time detection and blocking of local and private IP ranges (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16) to prevent data leakage.",
                aboutThreatIntelDesc = "Real-time aggregation of 10 zero-key intelligence feeds and specialized engines for phishing, malware, and IP reputation triage.",
                replayInteractiveTour = "Replay Interactive Tour & App Guide",
                specStandalonePortable = "Standalone Portable",
                specZeroTelemetry = "Zero Telemetry",
                specOpenSource = "MIT License Open Source",
                specMultiplatformEngine = "Multiplatform Engine",
                statusKeyRequiredShort = "Key Required",
                badgeThemeCyberDark = "CYBER DARK",
                badgeThemeIocClassic = "IOC CLASSIC",
                badgeThemeAutoSystem = "AUTO SYSTEM",
                badgeGuardActive = "RFC 1918 ACTIVE",
                badgeGuardDisabled = "GUARD DISABLED",
                badgeOpenSourceVersion = "OPEN SOURCE v1.0",
                searchHistoryPlaceholder = "Filter history by IOC...",
                filterAll = "ALL",
                noFilterResults = "No matching records found for filter",
                enginesZeroKeySubtitle = "10 Direct Zero-Key Threat Feeds • 100% Free & Tokenless",
                visitWebsite = "Visit Website",
                copyWebsiteUrl = "Copy Link",
                websiteUrlCopied = "Link copied to clipboard",
                filterByIocType = "Filter by Indicator Type",
                githubRepoUrl = "https://github.com/mr-coder20/ioc-lens",
                openGithubRepo = "Open GitHub Repository",
                copyGithubUrl = "Copy GitHub Link",
                githubUrlCopied = "GitHub link copied",
                clientManifestoTitle = "100% Client-Side Architecture",
                clientManifestoDesc = "All 10 threat intel feeds query directly from your device—zero middleman servers, zero cloud database, and zero telemetry. RFC 1918 local IP protection is enforced automatically on-device.",
                replayGuideTitle = "Replay App Guide"
            )
            AppLanguage.PERSIAN -> AppStrings(
                appTitle = "IOC Lens",
                appTagline = "تریاژ سریع، هوشمند و حریم‌خصوصی‌محور برای تحلیلگران امنیت",
                skip = "رد کردن",
                next = "بعدی",
                previous = "قبلی",
                start = "شروع برنامه",
                done = "شروع برنامه",
                changeLanguage = "تغییر زبان",
                searchPlaceholder = "یک آدرس IP، دامنه، نشانی وب یا هش فایل وارد کنید...",
                searchFieldPlaceholder = "آی‌پی، دامنه، نشانی وب یا هش فایل...",
                clear = "پاک‌سازی",
                analyzeInstant = "استعلام فوری",
                scanPayload = "اسکن و تریاژ نشانگر",
                cancelScan = "لغو اسکن",
                zeroKeyInstantScanReady = "آماده استعلام فوری بدون نیاز به کلید",
                streamHashingTitle = "محاسبه هش جریانی (تکه‌های ۶۴ کیلوبایتی)",
                introGuide = "راهنمای برنامه",
                enterIndicatorError = "لطفاً یک آدرس IP، دامنه، URL یا هش فایل وارد کنید.",
                threatIntelSourcesResults = "نتایج منابع هوش تهدیدات:",
                privacyGuardNotice = "🛡️ سپر حریم خصوصی فعال شد: نشانگر داخلی/خصوصی شناسایی شد. استعلام مسدود گردید.",
                themeLabel = "تم و ظاهر",
                languageLabel = "زبان برنامه",
                menuHome = "صفحه اصلی / تریاژ",
                menuSettings = "تنظیمات برنامه",
                menuGuide = "راهنمای برنامه",
                menuProviders = "موتورهای تحلیلی",
                menuPrivacyGuard = "حریم خصوصی",
                menuAbout = "درباره برنامه",
                settingsTitle = "تنظیمات",
                appVersion = "نسخه برنامه",
                versionLabel = "نسخه ۱.۰.۰ (متن‌باز)",
                systemDefaultTheme = "پیش‌فرض سیستم",
                darkTheme = "تم تاریک اختصاصی",
                portxDarkTheme = "تم سایبری PortX",
                iocLensClassicTheme = "تم اورجینال IOC Lens",
                apiKeysTitle = "کلیدهای سفارشی پیشرفته (اختیاری)",
                apiKeysSubtitle = "⚡ تمامی ۱۰ فید هوش تهدیدات بدون نیاز به کلید فعال هستند. در صورت تمایل می‌توانید کلیدهای اختصاصی را در زیر وارد کنید:",
                vtApiKeyLabel = "کلید API سرویس VirusTotal (اختیاری)",
                abuseApiKeyLabel = "کلید API سرویس AbuseIPDB (اختیاری)",
                alienApiKeyLabel = "کلید API سرویس AlienVault OTX (اختیاری)",
                pulseApiKeyLabel = "کلید API سرویس Pulsedive (اختیاری)",
                ipqsApiKeyLabel = "کلید API سرویس IPQualityScore (اختیاری)",
                autoScanClipboard = "اسکن خودکار کلیپ‌بورد",
                privacyGuardSetting = "محافظت از نشانگرهای داخلی (RFC 1918)",
                saveKeys = "ذخیره تنظیمات",
                savedSuccessfully = "تنظیمات با موفقیت ذخیره شد!",
                scanHistoryTitle = "تاریخچه اسکن",
                clearHistory = "پاک‌سازی تاریخچه",
                noHistory = "هنوز هیچ سابقه اسکنی ثبت نشده است. یک نشانگر را اسکن کنید یا فایلی را برای هش انتخاب نمایید.",
                threatConfidenceScore = "شاخص اطمینان تهدید",
                indicatorTypeLabel = "نوع نشانگر",
                threatIntelProvidersTitle = "موتورهای تحلیل هوش تهدیدات",
                enginesCount = "۱۰ موتور تحلیلی",
                freeBadge = "● رایگان",
                statusReady = "آماده",
                statusReadyZeroKey = "آماده (بدون نیاز به کلید)",
                statusKeyRequired = "نیازمند کلید API",
                statusScanning = "در حال اسکن...",
                statusRateLimited = "محدودیت نرخ درخواست",
                statusTimedOut = "پایان مهلت زمانی",
                statusNotApplicable = "نامرتبط با این نوع",
                closeButton = "بستن",
                streamHashingFailed = "محاسبه هش جریانی با خطا مواجه شد",
                errorRateLimit = "محدودیت تعداد درخواست در سرویس‌های خارجی؛ لطفاً دقایقی دیگر تلاش کنید.",
                errorTimeout = "مهلت اتصال به فیدهای تحلیلی به پایان رسید. اتصال اینترنت خود را بررسی کنید.",
                errorNetwork = "خطا در تحلیل نشانگر. اتصال اینترنت را بررسی کنید.",
                errorPrivacyGuard = "نشانگر داخلی/خصوصی توسط سپر حریم خصوصی مسدود شد.",
                tabScanner = "پویشگر",
                tabEngines = "موتورها",
                tabHistory = "تاریخچه",
                tabSettings = "تنظیمات",
                liveScanInProgress = "در حال استعلام هم‌زمان موتورها...",
                enginesCompleted = "موتور تکمیل شد",
                sampleIocsTitle = "نمونه‌های سریع:",
                sampleGoogleDns = "۸.۸.۸.۸ (دی‌ان‌اس گوگل)",
                sampleMalwareUrl = "malware-test.com (آدرس آلوده)",
                sampleHash = "هش واناکرای (WannaCry)",
                pasteFromClipboard = "چسباندن",
                dropFileHint = "انتخاب فایل برای اسکن",
                recentTriageFeed = "سوابق و پویش‌های اخیر",
                deleteItem = "حذف",
                reAnalyze = "پویش مجدد",
                categoryNetwork = "زیرساخت شبکه و دی‌ان‌اس",
                categoryMalware = "مخازن بدافزار و فایل‌ها",
                categoryThreats = "فیدهای تهدید و کنترل بات‌نت",
                serverlessStatus = "ارتباط مستقیم کلاینت سرورلس · بدون پایگاه داده واسط · سپر حریم خصوصی محلی (RFC 1918)",
                systemReadyEngines = "۱۰ موتور آماده",
                freeFeedsBadge = "۱۰ فید بدون کلید",
                privacyGuardActiveBadge = "حریم خصوصی محلی",
                settingsTabApiKeys = "کلیدهای سفارشی (اختیاری)",
                settingsTabGeneralPrivacy = "عمومی و حریم خصوصی",
                settingsTabAbout = "درباره برنامه",
                apiKeysZeroKeyBanner = "۱۰ موتور هوش تهدیدات فعال و بدون نیاز به کلید API",
                vtSubtitle = "بیش از ۷۰ موتور آنتی‌ویروس و سندباکس تحلیلی",
                abuseSubtitle = "پایگاه داده بررسی شهرت و سوءاستفاده از آی‌پی",
                alienSubtitle = "پالس‌های جهانی شاخص‌ها و شبکه‌های آلودگی",
                pulseSubtitle = "امتیازدهی به ریسک نشانگرها و فیدهای امنیتی",
                ipqsSubtitle = "تشخیص تقلب، پروکسی، بات‌نت و اعتبار آی‌پی",
                keyConfiguredBadge = "● تنظیم‌شده",
                keyOptionalBadge = "○ اختیاری",
                keyInputPlaceholder = "کلید اختصاصی API را اینجا جای‌گذاری کنید...",
                privacyGuardActiveDesc = "حفاظت سمت کلاینت فعال است: استعلام آدرس‌های خصوصی و لوپ‌بک (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, 127.0.0.1) به صورت محلی مسدود شده و ترافیکی به اینترنت ارسال نمی‌شود.",
                privacyGuardInactiveDesc = "حفاظت از آی‌پی‌های خصوصی غیرفعال است: آدرس‌های لوپ‌بک و شبکه محلی فیلتر نخواهند شد.",
                badgeClientDirect = "⚡ ارتباط ۱۰۰٪ مستقیم کلاینت",
                badgeZeroCloudDb = "🛡️ بدون پایگاه داده ابری",
                settingsSecurityConsoleBadge = "کنسول امنیت // تنظیمات",
                cardPrivacyGuardTitle = "سپر حریم خصوصی (RFC 1918)",
                cardThreatIntelTitle = "۱۰ موتور تحلیل تهدید",
                aboutZeroCloudDesc = "هیچ پایگاه داده ابری یا سرور میانی وجود ندارد؛ گزارش‌ها و سوابق تنها در حافظه رمزنگاری‌شده محلی ذخیره می‌شوند.",
                aboutClientDirectDesc = "استعلام‌های تهدید مستقیماً از دستگاه شما به APIهای رسمی ارسال می‌شوند بدون هیچ سرور واسطه‌ای (۱۰۰٪ کلاینت-ساید).",
                aboutPrivacyGuardDesc = "تشخیص و مسدودسازی بلادرنگ محدوده‌های آی‌پی محلی و خصوصی (10.0.0.0/8، 172.16.0.0/12 و 192.168.0.0/16) جهت جلوگیری از نشت اطلاعات.",
                aboutThreatIntelDesc = "تلفیق بلادرنگ ۱۰ فید هوش تهدیدات بدون نیاز به کلید و موتورهای تخصصی برای تریاژ فیشینگ، بدافزار و شهرت آی‌پی.",
                replayInteractiveTour = "مرور راهنمای تعاملی و آموزش برنامه",
                specStandalonePortable = "پرتابل و مستقل",
                specZeroTelemetry = "بدون تله‌متری",
                specOpenSource = "متن‌باز با مجوز MIT",
                specMultiplatformEngine = "موتور چندسکویی",
                statusKeyRequiredShort = "نیاز به کلید",
                badgeThemeCyberDark = "تم تاریک سایبری",
                badgeThemeIocClassic = "اورجینال IOC LENS",
                badgeThemeAutoSystem = "پیش‌فرض سیستم",
                badgeGuardActive = "سپر فعال (RFC 1918)",
                badgeGuardDisabled = "سپر غیرفعال",
                badgeOpenSourceVersion = "متن‌باز نسخه ۱.۰",
                searchHistoryPlaceholder = "فیلتر تاریخچه بر اساس نشانگر...",
                filterAll = "همه",
                noFilterResults = "هیچ موردی با فیلتر فعلی یافت نشد",
                enginesZeroKeySubtitle = "۱۰ فید مستقیم و بدون کلید • ۱۰۰٪ رایگان و آنی",
                visitWebsite = "مشاهده تارنما",
                copyWebsiteUrl = "کپی پیوند",
                websiteUrlCopied = "پیوند در حافظه کپی شد",
                filterByIocType = "فیلتر بر اساس نوع نشانگر",
                githubRepoUrl = "https://github.com/mr-coder20/ioc-lens",
                openGithubRepo = "مشاهده مخزن در گیت‌هاب",
                copyGithubUrl = "کپی پیوند گیت‌هاب",
                githubUrlCopied = "پیوند گیت‌هاب در حافظه کپی شد",
                clientManifestoTitle = "معماری ۱۰۰٪ کلاینت‌ساید و مستقیم",
                clientManifestoDesc = "تمامی ۱۰ موتور هوش تهدیدات مستقیماً از روی دستگاه شما استعلام می‌شوند؛ بدون سرور واسط، بدون پایگاه داده ابری و بدون رهگیری. حفاظت از نشانگرهای محلی (RFC 1918) به‌طور خودکار فعال است.",
                replayGuideTitle = "مرور مجدد راهنمای برنامه"
            )
            AppLanguage.RUSSIAN -> AppStrings(
                appTitle = "IOC Lens",
                appTagline = "Мгновенный триаж индикаторов угроз для аналитиков SOC",
                skip = "Пропустить",
                next = "Далее",
                previous = "Назад",
                start = "Начать",
                done = "ГОТОВО",
                changeLanguage = "Язык",
                searchPlaceholder = "Введите IP, домен, URL или хеш файла...",
                searchFieldPlaceholder = "Введите IP, домен, URL или хеш...",
                clear = "Очистить",
                analyzeInstant = "Проверить",
                scanPayload = "СКАНИРОВАТЬ ИНДИКАТОР",
                cancelScan = "ОТМЕНА",
                zeroKeyInstantScanReady = "Готов к мгновенной проверке без ключей",
                streamHashingTitle = "Потоковое хеширование (блоки 64 КБ)",
                introGuide = "Руководство",
                enterIndicatorError = "Пожалуйста, введите IP-адрес, домен, URL или хеш файла.",
                threatIntelSourcesResults = "Результаты источников Threat Intel:",
                privacyGuardNotice = "🛡️ Privacy Guard включён: обнаружен внутренний индикатор. Запрос заблокирован.",
                themeLabel = "Тема",
                languageLabel = "Язык",
                menuHome = "Главная / Триаж",
                menuSettings = "Настройки",
                menuGuide = "Руководство",
                menuProviders = "Движки аналитики",
                menuPrivacyGuard = "Защита данных (RFC 1918)",
                menuAbout = "О программе",
                settingsTitle = "Настройки",
                appVersion = "Версия",
                versionLabel = "Версия 1.0.0 (Open Source)",
                systemDefaultTheme = "Системная",
                darkTheme = "Тёмная тема",
                portxDarkTheme = "Кибер-тема PortX",
                iocLensClassicTheme = "Классическая тема IOC Lens",
                apiKeysTitle = "Дополнительные API-ключи (Опционально)",
                apiKeysSubtitle = "⚡ Все 10 каналов разведки угроз активны без ключей. При желании вы можете указать собственные API-ключи ниже:",
                vtApiKeyLabel = "API-ключ VirusTotal (необязательно)",
                abuseApiKeyLabel = "API-ключ AbuseIPDB (необязательно)",
                alienApiKeyLabel = "API-ключ AlienVault OTX (необязательно)",
                pulseApiKeyLabel = "API-ключ Pulsedive (необязательно)",
                ipqsApiKeyLabel = "API-ключ IPQualityScore (необязательно)",
                autoScanClipboard = "Автосканирование буфера",
                privacyGuardSetting = "Защита внутренних адресов (RFC 1918)",
                saveKeys = "Сохранить 💾",
                savedSuccessfully = "Настройки успешно сохранены!",
                scanHistoryTitle = "История проверок",
                clearHistory = "Очистить историю",
                noHistory = "История проверок пуста. Проверьте индикатор или выберите файл для хеширования.",
                threatConfidenceScore = "Индекс достоверности угрозы",
                indicatorTypeLabel = "Тип индикатора",
                threatIntelProvidersTitle = "ИСТОЧНИКИ THREAT INTEL",
                enginesCount = "10 движков",
                freeBadge = "● Бесплатно",
                statusReady = "Готов",
                statusReadyZeroKey = "Готов (Без ключа)",
                statusKeyRequired = "Требуется ключ",
                statusScanning = "Сканирование...",
                statusRateLimited = "Лимит запросов",
                statusTimedOut = "Тайм-аут",
                statusNotApplicable = "Неприменимо к типу",
                closeButton = "Закрыть",
                streamHashingFailed = "Ошибка потокового хеширования",
                errorRateLimit = "Превышен лимит запросов к внешним сервисам. Повторите позже.",
                errorTimeout = "Время ожидания ответа сервисов истекло. Проверьте интернет-соединение.",
                errorNetwork = "Не удалось проанализировать индикатор. Проверьте сетевое соединение.",
                errorPrivacyGuard = "Внутренний индикатор заблокирован Privacy Guard.",
                tabScanner = "Сканер",
                tabEngines = "Движки",
                tabHistory = "История",
                tabSettings = "Настройки",
                liveScanInProgress = "Идет сканирование движков...",
                enginesCompleted = "движков проверено",
                sampleIocsTitle = "Примеры для теста:",
                sampleGoogleDns = "8.8.8.8 (Google DNS)",
                sampleMalwareUrl = "malware-test.com (Угроза)",
                sampleHash = "Хеш WannaCry",
                pasteFromClipboard = "Вставить",
                dropFileHint = "Выбрать файл для сканирования",
                recentTriageFeed = "Недавние сканирования",
                deleteItem = "Удалить",
                reAnalyze = "Повторить",
                categoryNetwork = "Сетевая инфраструктура и DNS",
                categoryMalware = "Репозитории вредоносного ПО",
                categoryThreats = "Каналы угроз и C2 ботнетов",
                serverlessStatus = "Прямой бессерверный клиент · Без промежуточных БД · Защита данных активна (RFC 1918)",
                systemReadyEngines = "10 движков готовы",
                freeFeedsBadge = "10 бесплатных фидов",
                privacyGuardActiveBadge = "Защита активна",
                settingsTabApiKeys = "Доп. ключи (Опционально)",
                settingsTabGeneralPrivacy = "Общие и приватность",
                settingsTabAbout = "О программе",
                apiKeysZeroKeyBanner = "10 движков киберразведки активны без API-ключей",
                vtSubtitle = "Более 70 антивирусных и песочных движков (v3 API)",
                abuseSubtitle = "База данных репутации и злоупотреблений IP (v2 API)",
                alienSubtitle = "Глобальные импульсы угроз и индикаторы (v1 API)",
                pulseSubtitle = "Оценка рисков индикаторов и разведданные",
                ipqsSubtitle = "Оценка мошенничества, прокси и ботов",
                keyConfiguredBadge = "● Настроено",
                keyOptionalBadge = "○ Необязательно",
                keyInputPlaceholder = "Вставьте ключ API...",
                privacyGuardActiveDesc = "Защита активна: запросы к приватным сетям RFC 1918 (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, 127.0.0.1) перехватываются без отправки в интернет.",
                privacyGuardInactiveDesc = "Защита частных IP отключена: локальные адреса не будут фильтроваться.",
                badgeClientDirect = "⚡ 100% Прямое подключение",
                badgeZeroCloudDb = "🛡️ Без облачных баз",
                settingsSecurityConsoleBadge = "КОНСОЛЬ БЕЗОПАСНОСТИ // НАСТРОЙКИ",
                cardPrivacyGuardTitle = "Защита приватных IP (RFC 1918)",
                cardThreatIntelTitle = "10 движков Threat Intel",
                aboutZeroCloudDesc = "Промежуточная база данных на сервере отсутствует. Отчеты и история проверок хранятся исключительно в зашифрованном локальном хранилище.",
                aboutClientDirectDesc = "Запросы отправляются напрямую с устройства через официальные HTTPS API без сторонних прокси-серверов (100% на стороне клиента).",
                aboutPrivacyGuardDesc = "Обнаружение и блокировка запросов к локальным и приватным IP-диапазонам (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16) в реальном времени для защиты от утечек данных.",
                aboutThreatIntelDesc = "Агрегация в реальном времени 10 бесплатных фидов угроз и специализированных движков для анализа фишинга, вредоносного ПО и репутации IP.",
                replayInteractiveTour = "Повторить интерактивный тур и руководство",
                specStandalonePortable = "Портативная сборка",
                specZeroTelemetry = "Без телеметрии",
                specOpenSource = "Лицензия MIT (Open Source)",
                specMultiplatformEngine = "Кроссплатформенный движок",
                statusKeyRequiredShort = "Нужен ключ",
                badgeThemeCyberDark = "ТЁМНАЯ ТЕМА",
                badgeThemeIocClassic = "IOC CLASSIC",
                badgeThemeAutoSystem = "СИСТЕМНАЯ",
                badgeGuardActive = "RFC 1918 АКТИВЕН",
                badgeGuardDisabled = "ЗАЩИТА ОТКЛЮЧЕНА",
                badgeOpenSourceVersion = "OPEN SOURCE v1.0",
                searchHistoryPlaceholder = "Фильтр истории по IOC...",
                filterAll = "Все",
                noFilterResults = "Нет записей, соответствующих фильтру",
                enginesZeroKeySubtitle = "10 прямых фидов без ключей • 100% бесплатно и без токенов",
                visitWebsite = "Открыть сайт",
                copyWebsiteUrl = "Копировать ссылку",
                websiteUrlCopied = "Ссылка скопирована",
                filterByIocType = "Фильтр по типу индикатора",
                githubRepoUrl = "https://github.com/mr-coder20/ioc-lens",
                openGithubRepo = "Репозиторий на GitHub",
                copyGithubUrl = "Скопировать ссылку GitHub",
                githubUrlCopied = "Ссылка GitHub скопирована",
                clientManifestoTitle = "100% Клиентская архитектура",
                clientManifestoDesc = "Все 10 источников аналитики угроз опрашиваются напрямую с вашего устройства: без промежуточных серверов, облачных баз данных и телеметрии. Защита локальных IP (RFC 1918) активна постоянно.",
                replayGuideTitle = "Повторить руководство программы"
            )
        }
    }
}
