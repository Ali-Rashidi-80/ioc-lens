package io.github.mrcoder20.ioclens.domain.model

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.time.TimeSource

/**
 * Model representing the live connectivity, latency, and capabilities of a Threat Intelligence Engine.
 */
data class EngineProbeStatus(
    val id: String,
    val name: String,
    val fullName: String,
    val probeUrl: String,
    val indicatorScope: String,
    val descriptionEn: String,
    val descriptionFa: String,
    val isOnline: Boolean = true,
    val pingMs: Long = 32L
)

val DEFAULT_PROBE_ENGINES = listOf(
    EngineProbeStatus(
        id = "urlhaus",
        name = "URLhaus",
        fullName = "URLhaus (abuse.ch)",
        probeUrl = "https://urlhaus-api.abuse.ch/v1/url/",
        indicatorScope = "URL / DOMAIN",
        descriptionEn = "Real-time malware distribution URLs, payload tracking & Emotet/QakBot C2 detection.",
        descriptionFa = "سامانه اشتراک‌گذاری آدرس‌های وب آلوده به بدافزار توسط abuse.ch.",
        isOnline = true,
        pingMs = 32L
    ),
    EngineProbeStatus(
        id = "mb",
        name = "MalwareBazaar",
        fullName = "MalwareBazaar (abuse.ch)",
        probeUrl = "https://mb-api.abuse.ch/api/v1/",
        indicatorScope = "FILE_HASH",
        descriptionEn = "Cryptographic malware sample hunting, signature analysis & binary verification.",
        descriptionFa = "ردیابی نمونه‌های بدافزار، تحلیل امضا و اعتبارسنجی باینری توسط abuse.ch.",
        isOnline = true,
        pingMs = 42L
    ),
    EngineProbeStatus(
        id = "tf",
        name = "ThreatFox",
        fullName = "ThreatFox (abuse.ch)",
        probeUrl = "https://threatfox-api.abuse.ch/api/v1/",
        indicatorScope = "IP / DOMAIN / HASH / URL",
        descriptionEn = "Curated IOC feeds, Botnet C2 servers & Indicators of Compromise.",
        descriptionFa = "فیدهای منتخب شاخص‌های آلودگی (IOC)، سرورهای فرماندهی و کنترل بات‌نت.",
        isOnline = true,
        pingMs = 38L
    ),
    EngineProbeStatus(
        id = "feodo",
        name = "Feodo Tracker",
        fullName = "Feodo Tracker (abuse.ch)",
        probeUrl = "https://feodotracker.abuse.ch/downloads/ipblocklist.json",
        indicatorScope = "IP",
        descriptionEn = "Active Dridex, Emotet, TrickBot & QakBot command-and-control server blocklist.",
        descriptionFa = "لیست مسدودسازی سرورهای فعال C2 بات‌نت‌های Dridex، Emotet و QakBot.",
        isOnline = true,
        pingMs = 35L
    ),
    EngineProbeStatus(
        id = "shodan",
        name = "Shodan DB",
        fullName = "Shodan InternetDB",
        probeUrl = "https://internetdb.shodan.io/1.1.1.1",
        indicatorScope = "IP",
        descriptionEn = "Port scanning, service banners, CVE vulnerabilities & global honeypot indicators.",
        descriptionFa = "اسکن پورت‌ها، بنر سرویس‌ها، آسیب‌پذیری‌های CVE و شاخص‌های هانی‌پات.",
        isOnline = true,
        pingMs = 44L
    ),
    EngineProbeStatus(
        id = "otx",
        name = "AlienVault",
        fullName = "AlienVault OTX",
        probeUrl = "https://otx.alienvault.com/",
        indicatorScope = "IP / DOMAIN / URL / HASH",
        descriptionEn = "AT&T Cybersecurity crowd-sourced threat intelligence pulses & malicious campaigns.",
        descriptionFa = "پالس‌های تهدید جمع‌سپاری‌شده و کمپین‌های مخرب AT&T Cybersecurity.",
        isOnline = true,
        pingMs = 58L
    ),
    EngineProbeStatus(
        id = "cf",
        name = "Cloudflare DoH",
        fullName = "Cloudflare 1.1.1.1 DNS",
        probeUrl = "https://cloudflare-dns.com/dns-query",
        indicatorScope = "DOMAIN",
        descriptionEn = "Encrypted DNS-over-HTTPS resolution, DNSSEC validation & authoritative A/AAAA records.",
        descriptionFa = "تفکیک نام دامنه رمزنگاری‌شده DNS-over-HTTPS، تاییدیه DNSSEC و رکوردهای معتبر.",
        isOnline = true,
        pingMs = 14L
    ),
    EngineProbeStatus(
        id = "rdap",
        name = "RDAP",
        fullName = "RDAP Authority",
        probeUrl = "https://rdap.org/ip/1.1.1.1",
        indicatorScope = "DOMAIN / IP",
        descriptionEn = "Authoritative ICANN & RIR domain registrar identity and IP network ownership.",
        descriptionFa = "هویت ثبتی دامنه و مالکیت شبکه IP از رجیستری‌های بین‌المللی ICANN و RIR.",
        isOnline = true,
        pingMs = 46L
    ),
    EngineProbeStatus(
        id = "ripe",
        name = "RIPE Stat",
        fullName = "RIPE Stat / BGPView",
        probeUrl = "https://stat.ripe.net/data/routing-status/data.json?resource=1.1.1.1",
        indicatorScope = "IP",
        descriptionEn = "BGP routing tables, Autonomous System Numbers (ASN), prefix propagation & geolocation.",
        descriptionFa = "جداول مسیریابی BGP، شماره سیستم خودمختار (ASN) و موقعیت‌یابی شبکه.",
        isOnline = true,
        pingMs = 52L
    ),
    EngineProbeStatus(
        id = "crt",
        name = "crt.sh",
        fullName = "crt.sh Transparency",
        probeUrl = "https://crt.sh/?q=github.com&output=json",
        indicatorScope = "DOMAIN",
        descriptionEn = "Sectigo certificate transparency logs, sub-domains & historical SSL/TLS telemetry.",
        descriptionFa = "لاگ‌های شفافیت گواهینامه امنیتی SSL/TLS، ساب‌دامنه‌ها و تله‌متری تاریخی.",
        isOnline = true,
        pingMs = 65L
    )
)

/**
 * Concurrently probe all engines using the provided HTTP client and measure individual latency.
 */
suspend fun probeAllEnginesConcurrently(
    client: HttpClient,
    engines: List<EngineProbeStatus>
): List<EngineProbeStatus> = coroutineScope {
    engines.map { engine ->
        async {
            probeSingleEngine(client, engine)
        }
    }.awaitAll()
}

/**
 * Probe a single engine using the provided HTTP client and measure its latency.
 */
suspend fun probeSingleEngine(
    client: HttpClient,
    engine: EngineProbeStatus
): EngineProbeStatus {
    val mark = TimeSource.Monotonic.markNow()
    val online = try {
        val resp = client.get(engine.probeUrl)
        resp.status.value in 200..499
    } catch (_: Exception) {
        false
    }
    val elapsed = mark.elapsedNow().inWholeMilliseconds.coerceAtLeast(10L)
    return engine.copy(isOnline = online, pingMs = if (online) elapsed else 0L)
}
