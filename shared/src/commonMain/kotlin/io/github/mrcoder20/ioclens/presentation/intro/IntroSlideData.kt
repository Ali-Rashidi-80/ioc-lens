package io.github.mrcoder20.ioclens.presentation.intro

import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.domain.model.IntroSlide

object IntroSlideData {
    fun getSlides(language: AppLanguage): List<IntroSlide> {
        return when (language) {
            AppLanguage.ENGLISH -> listOf(
                IntroSlide(
                    id = 1,
                    badgeText = "Instant IOC Triage",
                    title = "Real-Time Threat Indicator Triage",
                    subtitle = "",
                    description = "Instantly check suspicious IP addresses, domains, URLs, and file hashes. Receive a consolidated, clear verdict from top threat databases at once.",
                    iconEmoji = "🔍"
                ),
                IntroSlide(
                    id = 2,
                    badgeText = "100% Privacy Protection",
                    title = "Local Privacy Guard (RFC 1918)",
                    subtitle = "",
                    description = "Automatically blocks private IP ranges and internal domain lookups locally before any external request is sent. Zero data leakage.",
                    iconEmoji = "🛡️"
                ),
                IntroSlide(
                    id = 3,
                    badgeText = "Multi-Source Threat Intel",
                    title = "Corroborated Global Intelligence",
                    subtitle = "",
                    description = "Consolidates detection signals from 10 global security feeds into one clear, explainable score backed by transparent vendor breakdown.",
                    iconEmoji = "🌐"
                ),
                IntroSlide(
                    id = 4,
                    badgeText = "100% Zero-Key Client",
                    title = "Direct HTTPS · Zero Middleman",
                    subtitle = "",
                    description = "No backend server, no registration, no tracking, and zero API keys required. Threat queries dispatch directly from your device over encrypted HTTPS.",
                    iconEmoji = "⚡"
                )
            )
            AppLanguage.PERSIAN -> listOf(
                IntroSlide(
                    id = 1,
                    badgeText = "تریاژ آنی نشانگرها",
                    title = "تحلیل بلادرنگ نشانگرهای تهدید (IOC)",
                    subtitle = "",
                    description = "بررسی فوری نشانی‌های IP، دامنه‌ها، URLها و هش فایل‌ها؛ دریافت ارزیابی یکپارچه و دقیق از معتبرترین پایگاه‌های جهانی هوش تهدیدات.",
                    iconEmoji = "🔍"
                ),
                IntroSlide(
                    id = 2,
                    badgeText = "حریم خصوصی ۱۰۰٪",
                    title = "سپر محافظت از داده‌های داخلی (RFC 1918)",
                    subtitle = "",
                    description = "شناسایی و مسدودسازی خودکار نشانگرهای خصوصی و درون‌شبکه‌ای (آی‌پی‌های داخلی و لوپ‌بک) به صورت محلی جهت جلوگیری از هرگونه نشت اطلاعات.",
                    iconEmoji = "🛡️"
                ),
                IntroSlide(
                    id = 3,
                    badgeText = "هوش تهدیدات چندمنبعی",
                    title = "ارزیابی هم‌زمان پایگاه‌های داده جهانی",
                    subtitle = "",
                    description = "تلفیق هوشمند سیگنال‌های امنیتی از ۱۰ منبع معتبر جهانی هوش تهدیدات و ارائه ارزیابی یکپارچه همراه با تفکیک دقیق نتایج هر سرویس.",
                    iconEmoji = "🌐"
                ),
                IntroSlide(
                    id = 4,
                    badgeText = "معماری ۱۰۰٪ بدون کلید",
                    title = "ارتباط مستقیم کلاینت بدون واسطه",
                    subtitle = "",
                    description = "بدون سرور میانی، بدون پایگاه داده ابری، بدون تله‌متری و بدون نیاز به کلید API؛ استعلام‌ها مستقیماً با پروتکل امن HTTPS بین دستگاه شما و منابع تهدید انجام می‌شود.",
                    iconEmoji = "⚡"
                )
            )
            AppLanguage.RUSSIAN -> listOf(
                IntroSlide(
                    id = 1,
                    badgeText = "Мгновенный триаж IOC",
                    title = "Экспресс-анализ индикаторов угроз",
                    subtitle = "",
                    description = "Быстрая проверка IP-адресов, доменов, URL и хешей файлов. Единый понятный вердикт из ведущих баз данных угроз.",
                    iconEmoji = "🔍"
                ),
                IntroSlide(
                    id = 2,
                    badgeText = "100% Защита данных",
                    title = "Модуль Privacy Guard (RFC 1918)",
                    subtitle = "",
                    description = "Автоматическая локальная блокировка запросов внутренних IP и приватных доменов для исключения утечек информации.",
                    iconEmoji = "🛡️"
                ),
                IntroSlide(
                    id = 3,
                    badgeText = "Мультипровайдерная разведка угроз",
                    title = "Единый вердикт из мировых баз",
                    subtitle = "",
                    description = "Объединение сигналов 10 ведущих глобальных сервисов threat intelligence в один понятный вердикт с подробной детализацией.",
                    iconEmoji = "🌐"
                ),
                IntroSlide(
                    id = 4,
                    badgeText = "100% Zero-Key архитектура",
                    title = "Прямой клиент без посредников",
                    subtitle = "",
                    description = "Без промежуточных серверов, облачных баз, регистрации и ключей API. Все проверки выполняются напрямую с вашего устройства по защищённому HTTPS.",
                    iconEmoji = "⚡"
                )
            )
        }
    }
}
