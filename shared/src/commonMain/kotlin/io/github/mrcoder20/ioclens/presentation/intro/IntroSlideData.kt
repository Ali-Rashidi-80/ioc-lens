package io.github.mrcoder20.ioclens.presentation.intro

import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.domain.model.IntroSlide

object IntroSlideData {
    fun getSlides(language: AppLanguage): List<IntroSlide> {
        return when (language) {
            AppLanguage.PERSIAN -> listOf(
                IntroSlide(
                    id = 1,
                    badgeText = "تریاژ آنی نشانگرها",
                    title = "تحلیل بلادرنگ نشانگرهای تهدید (IOC)",
                    subtitle = "بررسی خودکار و سریع بدون اتلاف وقت",
                    description = "بررسی فوری نشانی‌های IP، دامنه‌ها، URLها و هش فایل‌ها؛ دریافت ارزیابی یکپارچه و دقیق از معتبرترین پایگاه‌های جهانی هوش تهدیدات.",
                    iconEmoji = "🔍"
                ),
                IntroSlide(
                    id = 2,
                    badgeText = "حریم خصوصی محلی",
                    title = "سپر محافظت از داده‌های داخلی (RFC 1918)",
                    subtitle = "مسدودسازی خودکار پیش از ارسال",
                    description = "شناسایی و مسدودسازی خودکار نشانگرهای خصوصی و درون‌شبکه‌ای (آی‌پی‌های محلی و لوپ‌بک) به صورت کاملاً آفلاین جهت جلوگیری از هرگونه نشت داده.",
                    iconEmoji = "🛡️"
                ),
                IntroSlide(
                    id = 3,
                    badgeText = "۱۰ منبع هوش تهدیدات",
                    title = "ارزیابی هم‌زمان پایگاه‌های داده جهانی",
                    subtitle = "URLhaus · MalwareBazaar · ThreatFox · Shodan ...",
                    description = "تلفیق هوشمند سیگنال‌های امنیتی از ۱۰ منبع معتبر جهانی هوش تهدیدات و ارائه ارزیابی یکپارچه همراه با تفکیک دقیق نتایج هر سرویس.",
                    iconEmoji = "🌐"
                ),
                IntroSlide(
                    id = 4,
                    badgeText = "معماری کلاینت‌ساید مستقیم",
                    title = "بدون سرور میانی · بدون نیاز به کلید API",
                    subtitle = "مستقیم از دستگاه شما به منابع امنیتی",
                    description = "بدون سرور واسط، بدون پایگاه داده ابری و بدون رهگیری؛ تمامی استعلام‌ها با رمزنگاری امن HTTPS مستقیماً از دستگاه شما انجام می‌شوند.",
                    iconEmoji = "⚡"
                )
            )
            AppLanguage.ENGLISH -> listOf(
                IntroSlide(
                    id = 1,
                    badgeText = "Instant IOC Triage",
                    title = "Real-Time Threat Indicator Triage",
                    subtitle = "Instant multi-engine correlation",
                    description = "Instantly check suspicious IP addresses, domains, URLs, and file hashes. Receive a consolidated, clear verdict from top threat databases at once.",
                    iconEmoji = "🔍"
                ),
                IntroSlide(
                    id = 2,
                    badgeText = "Local Privacy Guard",
                    title = "Internal RFC 1918 Data Protection",
                    subtitle = "Zero leakage of private infrastructure",
                    description = "Automatically blocks private IP ranges and internal domain lookups locally before any external request is sent. Zero data leakage.",
                    iconEmoji = "🛡️"
                ),
                IntroSlide(
                    id = 3,
                    badgeText = "10 Threat Feeds",
                    title = "Corroborated Global Intelligence",
                    subtitle = "URLhaus · MalwareBazaar · ThreatFox · Shodan ...",
                    description = "Consolidates detection signals from 10 global security feeds into one clear, explainable score backed by transparent vendor breakdown.",
                    iconEmoji = "🌐"
                ),
                IntroSlide(
                    id = 4,
                    badgeText = "100% Zero-Key Client",
                    title = "Direct HTTPS · Zero Middleman",
                    subtitle = "Direct encrypted communication",
                    description = "No backend server, no registration, no tracking, and zero API keys required. Threat queries dispatch directly from your device over encrypted HTTPS.",
                    iconEmoji = "⚡"
                )
            )
            AppLanguage.RUSSIAN -> listOf(
                IntroSlide(
                    id = 1,
                    badgeText = "Мгновенный триаж IOC",
                    title = "Экспресс-анализ индикаторов угроз",
                    subtitle = "Быстрая многопоточная проверка",
                    description = "Быстрая проверка IP-адресов, доменов, URL и хешей файлов. Единый понятный вердикт из ведущих баз данных угроз.",
                    iconEmoji = "🔍"
                ),
                IntroSlide(
                    id = 2,
                    badgeText = "Локальная защита данных",
                    title = "Модуль Privacy Guard (RFC 1918)",
                    subtitle = "Защита внутренней инфраструктуры",
                    description = "Автоматическая локальная блокировка запросов внутренних IP и приватных доменов для исключения утечек информации.",
                    iconEmoji = "🛡️"
                ),
                IntroSlide(
                    id = 3,
                    badgeText = "10 источников угроз",
                    title = "Единый вердикт из мировых баز",
                    subtitle = "URLhaus · MalwareBazaar · ThreatFox · Shodan ...",
                    description = "Объединение сигналов 10 ведущих глобальных сервисов threat intelligence в один понятный вердикт с подробной детализацией.",
                    iconEmoji = "🌐"
                ),
                IntroSlide(
                    id = 4,
                    badgeText = "100% Zero-Key клиент",
                    title = "Прямой HTTPS · Без посредников",
                    subtitle = "Прямое шифрованное соединение",
                    description = "Без промежуточных серверов, облачных баз, регистрации и ключей API. Все проверки выполняются напрямую с вашего устройства по защищённому HTTPS.",
                    iconEmoji = "⚡"
                )
            )
        }
    }
}

