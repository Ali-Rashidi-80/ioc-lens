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
    val enginesCompleted: String
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
                AppLanguage.PERSIAN -> "🛡️ محافظ حریم خصوصی فعال شد: نشانگر داخلی یا خصوصی شناسایی شد. استعلام خارجی جهت محافظت از شبکه مسدود گردید."
                AppLanguage.RUSSIAN -> "🛡️ Privacy Guard активен: обнаружен внутренний индикатор. Запросы заблокированы."
                AppLanguage.ENGLISH -> "🛡️ Privacy Guard Activated: Private/Internal indicator detected. Remote lookups blocked to protect internal infrastructure."
            }
        }

        if (indicator.type == IndicatorType.UNKNOWN) {
            return when (lang) {
                AppLanguage.PERSIAN -> "⚪ فرمت نشانگر شناسایی نشد. لطفاً یک آدرس IP، دامنه، URL یا هش فایل معتبر وارد نمایید."
                AppLanguage.RUSSIAN -> "⚪ Нераспознанный формат индикатора. Введите корректный IP, домен, URL или хеш файла."
                AppLanguage.ENGLISH -> "⚪ Unrecognized indicator format. Please provide a valid IP, Domain, URL, or File Hash."
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
                maliciousSources.isNotEmpty() -> "🚨 پی‌لود آلوده شناسایی شد: تایید شده توسط $malNames."
                suspiciousSources.isNotEmpty() -> "🟠 فعالیت مشکوک: هشدارهای تهدید توسط $suspNames ثبت گردید."
                lowRiskSources.isNotEmpty() -> "🟡 ریسک پایین: سیگنال‌های تله‌متری جزئی گزارش شده اما امضای بدافزار فعالی تایید نشد."
                cleanSources.isNotEmpty() -> "🟢 پاک و بدون تهدید: ${toPersianDigits(cleanSources.size.toString())} موتور هوش تهدید هیچ‌گونه فعالیت مخربی مشاهده نکردند."
                else -> "⚪ هیچ سابقه تهدیدی در موتورهای فعال عمومی یافت نشد."
            }
            AppLanguage.RUSSIAN -> when {
                maliciousSources.isNotEmpty() -> "🚨 ОБНАРУЖЕНА ВРЕДОНОСНАЯ АКТИВНОСТЬ: подтверждено $malNames."
                suspiciousSources.isNotEmpty() -> "🟠 Подозрительная активность: обнаружены сигналы угроз от $suspNames."
                lowRiskSources.isNotEmpty() -> "🟡 Низкий риск: зафиксированы незначительные аномалии, активных сигнатур угроз нет."
                cleanSources.isNotEmpty() -> "🟢 Проверено: ${cleanSources.size} сервисов threat intelligence не выявили угроз."
                else -> "⚪ Записей об угрозах в активных базах данных не обнаружено."
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
                "VirusTotal" -> "ویروس‌توتال (VirusTotal)"
                "AbuseIPDB" -> "ابوز آی‌پی‌دی‌بی (AbuseIPDB)"
                "AlienVault OTX" -> "الین‌والت OTX (AlienVault)"
                "Pulsedive" -> "پالس‌دایو (Pulsedive)"
                "IPQualityScore" -> "آی‌پی کوالیتی‌اسکور (IPQS)"
                else -> name
            }
            AppLanguage.RUSSIAN -> when (name) {
                "RDAP / Registry" -> "RDAP / Реестр"
                else -> name
            }
            AppLanguage.ENGLISH -> name
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
                summary.contains("No records found in VirusTotal database", ignoreCase = true) ->
                    "رکوردی در پایگاه داده ویروس‌توتال ثبت نشده است"
                summary.contains("Rate limit exceeded", ignoreCase = true) ->
                    "محدودیت نرخ درخواست (۴۲۹): سهمیه به اتمام رسید"
                summary.contains("Request timed out", ignoreCase = true) ->
                    "پایان مهلت زمانی ارتباط با سرور هوش تهدید"
                summary.contains("Network connection failure", ignoreCase = true) ->
                    "خطای اتصال به شبکه هنگام استعلام از سرویس"
                summary.contains("engines detected malicious", ignoreCase = true) ->
                    toPersianDigits(summary).replace("engines detected malicious", "موتور این نشانگر را آلوده شناسایی کردند")
                summary.contains("Abuse Confidence Score:", ignoreCase = true) ->
                    toPersianDigits(summary).replace("Abuse Confidence Score:", "شاخص سوءاستفاده:").replace("%", "٪")
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
                summary.contains("No records found in VirusTotal database", ignoreCase = true) ->
                    "Записей в базе VirusTotal не обнаружено"
                summary.contains("Rate limit exceeded", ignoreCase = true) ->
                    "Превышен лимит запросов API (HTTP 429)"
                summary.contains("Request timed out", ignoreCase = true) ->
                    "Тайм-аут запроса к поставщику аналитики"
                summary.contains("Network connection failure", ignoreCase = true) ->
                    "Сбой сетевого подключения к поставщику"
                summary.contains("engines detected malicious", ignoreCase = true) ->
                    summary.replace("engines detected malicious", "движков определили как вредоносный")
                summary.contains("Abuse Confidence Score:", ignoreCase = true) ->
                    summary.replace("Abuse Confidence Score:", "Индекс злоупотреблений:")
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
                next = "Next →",
                previous = "← Previous",
                start = "Get Started 🚀",
                done = "DONE",
                changeLanguage = "🌐 Language",
                searchPlaceholder = "Enter an IP, domain, URL or file hash...",
                searchFieldPlaceholder = "Paste IP, URL, Hash, Domain, or drop file...",
                clear = "Clear",
                analyzeInstant = "Instant Lookup ⚡",
                scanPayload = "⚡ SCAN PAYLOAD",
                cancelScan = "⏹ CANCEL",
                zeroKeyInstantScanReady = "Zero-Key instant scan ready",
                streamHashingTitle = "Stream Hashing (64KB chunks)",
                introGuide = "💡 Guide / Intro",
                enterIndicatorError = "Please enter an IP address, domain, URL, or file hash.",
                threatIntelSourcesResults = "Threat Intel Sources Results:",
                privacyGuardNotice = "🛡️ Privacy Guard Activated: Private/Internal indicator detected. Lookup blocked.",
                themeLabel = "Theme",
                languageLabel = "Language",
                menuHome = "Home / Triage",
                menuSettings = "App Settings",
                menuGuide = "Intro Guide",
                menuProviders = "API Keys",
                menuPrivacyGuard = "Privacy Guard",
                menuAbout = "About IOC Lens",
                settingsTitle = "Settings",
                appVersion = "App Version",
                versionLabel = "v1.0.0 (Open Source)",
                systemDefaultTheme = "System Default",
                darkTheme = "Signature Dark Theme",
                apiKeysTitle = "Threat Intel API Keys",
                apiKeysSubtitle = "⚡ Built-in Public Threat Intel feeds active. You can optionally enter custom private API keys below:",
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
                threatIntelProvidersTitle = "🌐 THREAT INTEL PROVIDERS",
                enginesCount = "10 Engines",
                freeBadge = "● Free",
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
                enginesCompleted = "engines complete"
            )
            AppLanguage.PERSIAN -> AppStrings(
                appTitle = "IOC Lens",
                appTagline = "تریاژ سریع، هوشمند و حریم‌خصوصی‌محور برای تحلیلگران امنیت",
                skip = "رد کردن",
                next = "بعدی ←",
                previous = "→ قبلی",
                start = "شروع کنید 🚀",
                done = "شروع برنامه",
                changeLanguage = "🌐 تغییر زبان",
                searchPlaceholder = "یک IP، دامنه، URL یا هش فایل وارد کنید...",
                searchFieldPlaceholder = "آی‌پی، دامنه، نشانی وب، هش فایل یا فایل رها شده...",
                clear = "پاک‌سازی",
                analyzeInstant = "استعلام فوری ⚡",
                scanPayload = "⚡ اسکن و تریاژ نشانگر",
                cancelScan = "⏹ لغو اسکن",
                zeroKeyInstantScanReady = "آماده استعلام فوری بدون نیاز به کلید",
                streamHashingTitle = "محاسبه هش جریانی (تکه‌های ۶۴ کیلوبایتی)",
                introGuide = "💡 راهنما / اینترو",
                enterIndicatorError = "لطفاً یک آدرس IP، دامنه، URL یا هش فایل وارد کنید.",
                threatIntelSourcesResults = "نتایج سرویس‌های Threat Intel:",
                privacyGuardNotice = "🛡️ Privacy Guard فعال شد: نشانگر داخلی/خصوصی شناسایی شد. استعلام مسدود گردید.",
                themeLabel = "تم و ظاهر",
                languageLabel = "زبان برنامه",
                menuHome = "صفحه اصلی / تریاژ",
                menuSettings = "تنظیمات برنامه",
                menuGuide = "راهنما و اینترو",
                menuProviders = "کلیدهای API",
                menuPrivacyGuard = "حریم خصوصی",
                menuAbout = "درباره برنامه",
                settingsTitle = "تنظیمات",
                appVersion = "نسخه برنامه",
                versionLabel = "نسخه ۱.۰.۰ (متن‌باز)",
                systemDefaultTheme = "پیش‌فرض سیستم",
                darkTheme = "تم تاریک اختصاصی",
                apiKeysTitle = "کلیدهای API سرویس‌های تهدید",
                apiKeysSubtitle = "⚡ فیدهای عمومی و رایگان هوش تهدید فعال هستند. در صورت تمایل می‌توانید کلیدهای اختصاصی خود را وارد نمایید:",
                vtApiKeyLabel = "کلید API سرویس VirusTotal (اختیاری)",
                abuseApiKeyLabel = "کلید API سرویس AbuseIPDB (اختیاری)",
                alienApiKeyLabel = "کلید API سرویس AlienVault OTX (اختیاری)",
                pulseApiKeyLabel = "کلید API سرویس Pulsedive (اختیاری)",
                ipqsApiKeyLabel = "کلید API سرویس IPQualityScore (اختیاری)",
                autoScanClipboard = "اسکن خودکار کلیپ‌بورد",
                privacyGuardSetting = "محافظت از نشانگرهای داخلی",
                saveKeys = "ذخیره تنظیمات 💾",
                savedSuccessfully = "تنظیمات با موفقیت ذخیره شد!",
                scanHistoryTitle = "تاریخچه اسکن",
                clearHistory = "پاک‌سازی تاریخچه",
                noHistory = "هنوز هیچ تاریخچه اسکنی ثبت نشده است. یک نشانگر را اسکن کنید یا فایلی را به اشتراک بگذارید.",
                threatConfidenceScore = "شاخص اطمینان تهدید",
                indicatorTypeLabel = "نوع نشانگر",
                threatIntelProvidersTitle = "🌐 سرویس‌های هوش تهدید (Threat Intel)",
                enginesCount = "۱۰ موتور تحلیلی",
                freeBadge = "● رایگان",
                statusReadyZeroKey = "آماده (بدون نیاز به کلید)",
                statusKeyRequired = "نیازمند کلید API",
                statusScanning = "در حال اسکن...",
                statusRateLimited = "محدودیت نرخ (Rate Limited)",
                statusTimedOut = "پایان مهلت زمانی",
                statusNotApplicable = "نامرتبط با این نوع",
                closeButton = "بستن",
                streamHashingFailed = "محاسبه هش جریانی با خطا مواجه شد",
                errorRateLimit = "محدودیت تعداد درخواست در سرویس‌های خارجی. لطفاً دقایقی دیگر تلاش کنید.",
                errorTimeout = "مهلت اتصال به فیدهای هوش تهدید به پایان رسید. اتصال اینترنت خود را بررسی کنید.",
                errorNetwork = "خطا در تحلیل نشانگر. اتصال اینترنت را بررسی کنید.",
                errorPrivacyGuard = "نشانگر داخلی/خصوصی توسط محافظ حریم خصوصی مسدود شد.",
                tabScanner = "پویشگر",
                tabEngines = "موتورها",
                tabHistory = "تاریخچه",
                tabSettings = "تنظیمات",
                liveScanInProgress = "در حال اسکن زنده موتورها...",
                enginesCompleted = "موتور تکمیل شد"
            )
            AppLanguage.RUSSIAN -> AppStrings(
                appTitle = "IOC Lens",
                appTagline = "Мгновенный триаж индикаторов угроз для аналитиков SOC",
                skip = "Пропустить",
                next = "Далее →",
                previous = "← Назад",
                start = "Начать 🚀",
                done = "ГОТОВО",
                changeLanguage = "🌐 Язык",
                searchPlaceholder = "Введите IP, домен, URL или хеш файла...",
                searchFieldPlaceholder = "Введите IP, URL, хеш, домен или перетащите файл...",
                clear = "Очистить",
                analyzeInstant = "Проверить ⚡",
                scanPayload = "⚡ СКАНИРОВАТЬ ИНДИКАТОР",
                cancelScan = "⏹ ОТМЕНА",
                zeroKeyInstantScanReady = "Готов к мгновенной проверке без ключей",
                streamHashingTitle = "Потоковое хеширование (блоки 64 КБ)",
                introGuide = "💡 Руководство",
                enterIndicatorError = "Пожалуйста, введите IP-адрес, домен, URL или хеш файла.",
                threatIntelSourcesResults = "Результаты источников Threat Intel:",
                privacyGuardNotice = "🛡️ Privacy Guard включён: обнаружен внутренний индикатор. Запрос заблокирован.",
                themeLabel = "Тема",
                languageLabel = "Язык",
                menuHome = "Главная / Триаж",
                menuSettings = "Настройки",
                menuGuide = "Руководство",
                menuProviders = "API-ключи",
                menuPrivacyGuard = "Защита данных",
                menuAbout = "О программе",
                settingsTitle = "Настройки",
                appVersion = "Версия",
                versionLabel = "Версия 1.0.0 (Open Source)",
                systemDefaultTheme = "Системная",
                darkTheme = "Тёмная тема",
                apiKeysTitle = "API-ключи Threat Intel",
                apiKeysSubtitle = "⚡ Общедоступные каналы разведки активны. Дополнительно можно указать персональные ключи:",
                vtApiKeyLabel = "API-ключ VirusTotal (необязательно)",
                abuseApiKeyLabel = "API-ключ AbuseIPDB (необязательно)",
                alienApiKeyLabel = "API-ключ AlienVault OTX (необязательно)",
                pulseApiKeyLabel = "API-ключ Pulsedive (необязательно)",
                ipqsApiKeyLabel = "API-ключ IPQualityScore (необязательно)",
                autoScanClipboard = "Автосканирование буфера",
                privacyGuardSetting = "Защита внутренних адресов",
                saveKeys = "Сохранить 💾",
                savedSuccessfully = "Настройки успешно сохранены!",
                scanHistoryTitle = "История проверок",
                clearHistory = "Очистить историю",
                noHistory = "История проверок пуста. Проверьте индикатор или передайте файл.",
                threatConfidenceScore = "Индекс достоверности угрозы",
                indicatorTypeLabel = "Тип индикатора",
                threatIntelProvidersTitle = "🌐 ИСТОЧНИКИ THREAT INTEL",
                enginesCount = "10 движков",
                freeBadge = "● Бесплатно",
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
                enginesCompleted = "движков проверено"
            )
        }
    }
}
