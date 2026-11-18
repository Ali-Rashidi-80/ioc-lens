package io.github.mrcoder20.ioclens.domain.model

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.time.TimeSource

/**
 * Model representing the live connectivity, latency, and capabilities of a Threat Intelligence Engine.
 * Initial state is strictly unprobed (`hasProbed = false`, `isOnline = false`, `pingMs = 0L`) until tested.
 */
data class EngineProbeStatus(
    val id: String,
    val name: String,
    val fullName: String,
    val probeUrl: String,
    val indicatorScope: String,
    val descriptionEn: String,
    val descriptionFa: String,
    val descriptionRu: String,
    val isOnline: Boolean = false,
    val pingMs: Long = 0L,
    val hasProbed: Boolean = false,
    val supportsPostOnlyProbe: Boolean = false,
    val useHeadProbe: Boolean = false
)

val DEFAULT_PROBE_ENGINES = listOf(
    EngineProbeStatus(
        id = "urlhaus",
        name = "URLhaus",
        fullName = "URLhaus (abuse.ch)",
        probeUrl = "https://urlhaus-api.abuse.ch/v1/url/",
        indicatorScope = "URL / DOMAIN",
        descriptionEn = "Real-time malware distribution URLs & malicious payload hosts.",
        descriptionFa = "سامانه اشتراک‌گذاری آدرس‌های وب آلوده به بدافزار توسط abuse.ch.",
        descriptionRu = "Оперативная база вредоносных URL и хостов распространения от abuse.ch.",
        supportsPostOnlyProbe = true
    ),
    EngineProbeStatus(
        id = "mb",
        name = "MalwareBazaar",
        fullName = "MalwareBazaar (abuse.ch)",
        probeUrl = "https://mb-api.abuse.ch/api/v1/",
        indicatorScope = "FILE_HASH",
        descriptionEn = "Cryptographic malware sample hunting & signature verification.",
        descriptionFa = "ردیابی نمونه‌های بدافزار، تحلیل امضا و اعتبارسنجی باینری توسط abuse.ch.",
        descriptionRu = "Поиск образцов вредоносного ПО по хешам и сигнатурам в abuse.ch.",
        supportsPostOnlyProbe = true
    ),
    EngineProbeStatus(
        id = "tf",
        name = "ThreatFox",
        fullName = "ThreatFox (abuse.ch)",
        probeUrl = "https://threatfox-api.abuse.ch/api/v1/",
        indicatorScope = "IP / DOMAIN / HASH / URL",
        descriptionEn = "Curated IOC feeds, Botnet C2 servers & confirmed malware indicators.",
        descriptionFa = "فیدهای منتخب شاخص‌های آلودگی (IOC)، سرورهای فرماندهی و کنترل بات‌نت.",
        descriptionRu = "Курируемые фиды IOC, C2-серверов ботнетов и вредоносной активности.",
        supportsPostOnlyProbe = true
    ),
    EngineProbeStatus(
        id = "feodo",
        name = "Feodo Tracker",
        fullName = "Feodo Tracker (abuse.ch)",
        probeUrl = "https://feodotracker.abuse.ch/",
        indicatorScope = "IP",
        descriptionEn = "Active Dridex, Emotet, TrickBot & QakBot C2 blocklist telemetry.",
        descriptionFa = "لیست مسدودسازی سرورهای فعال C2 بات‌نت‌های Dridex، Emotet و QakBot.",
        descriptionRu = "Блок-лист активных C2-узлов ботнетов Dridex, Emotet, TrickBot и QakBot.",
        useHeadProbe = true
    ),
    EngineProbeStatus(
        id = "shodan",
        name = "Shodan DB",
        fullName = "Shodan InternetDB",
        probeUrl = "https://internetdb.shodan.io/1.1.1.1",
        indicatorScope = "IP",
        descriptionEn = "Exposed ports, service banners, hostnames & passive CVE metadata.",
        descriptionFa = "اسکن پورت‌ها، بنر سرویس‌ها، آسیب‌پذیری‌های CVE و شاخص‌های هاست.",
        descriptionRu = "Открытые порты, баннеры сервисов, имена хостов и пассивные CVE.",
        useHeadProbe = true
    ),
    EngineProbeStatus(
        id = "otx",
        name = "AlienVault",
        fullName = "AlienVault OTX",
        probeUrl = "https://otx.alienvault.com/",
        indicatorScope = "IP / DOMAIN / URL / HASH",
        descriptionEn = "Crowd-sourced threat intelligence pulses & adversary campaigns.",
        descriptionFa = "پالس‌های تهدید جمع‌سپاری‌شده و کمپین‌های مخرب AlienVault OTX.",
        descriptionRu = "Открытые импульсы угроз (pulses) и кампании от AlienVault OTX.",
        useHeadProbe = true
    ),
    EngineProbeStatus(
        id = "cf",
        name = "Cloudflare DoH",
        fullName = "Cloudflare DoH",
        probeUrl = "https://cloudflare-dns.com/dns-query?name=example.com&type=A",
        indicatorScope = "DOMAIN",
        descriptionEn = "Encrypted DNS-over-HTTPS resolution & DNS status diagnostics.",
        descriptionFa = "تفکیک نام دامنه رمزنگاری‌شده DNS-over-HTTPS و وضعیت رکوردهای معتبر.",
        descriptionRu = "Шифрованное разрешение DNS-over-HTTPS и проверка статуса домена.",
        useHeadProbe = true
    ),
    EngineProbeStatus(
        id = "rdap",
        name = "RDAP",
        fullName = "RDAP / Registry",
        probeUrl = "https://rdap.org/ip/1.1.1.1",
        indicatorScope = "DOMAIN / IP",
        descriptionEn = "Authoritative ICANN & RIR domain registrar and IP network ownership.",
        descriptionFa = "هویت ثبتی دامنه و مالکیت شبکه IP از رجیستری‌های بین‌المللی ICANN و RIR.",
        descriptionRu = "Регистрационные данные доменов и владельцы IP-сетей ICANN/RIR.",
        useHeadProbe = true
    ),
    EngineProbeStatus(
        id = "ripe",
        name = "RIPE Stat",
        fullName = "RIPE Stat",
        probeUrl = "https://stat.ripe.net/data/network-info/data.json?resource=1.1.1.1",
        indicatorScope = "IP",
        descriptionEn = "BGP routing tables, Autonomous System Numbers (ASN) & announced prefixes.",
        descriptionFa = "جداول مسیریابی BGP، شماره سیستم خودمختار (ASN) و پیشوندهای شبکه.",
        descriptionRu = "Маршрутизация BGP, номера автономных систем (ASN) и префиксы.",
        useHeadProbe = true
    ),
    EngineProbeStatus(
        id = "crt",
        name = "crt.sh",
        fullName = "crt.sh",
        probeUrl = "https://crt.sh/",
        indicatorScope = "DOMAIN",
        descriptionEn = "Sectigo Certificate Transparency logs & issued SSL/TLS telemetry.",
        descriptionFa = "لاگ‌های شفافیت گواهینامه امنیتی SSL/TLS و تله‌متری گواهی‌ها.",
        descriptionRu = "Журналы Certificate Transparency и выпущенные SSL/TLS-сертификаты.",
        useHeadProbe = true
    ),
    EngineProbeStatus(
        id = "ipwhois",
        name = "ipwho.is",
        fullName = "ipwho.is",
        probeUrl = "https://ipwho.is/1.1.1.1",
        indicatorScope = "IP",
        descriptionEn = "ISP telemetry, geolocation, VPN/Proxy & Tor exit-node detection.",
        descriptionFa = "شناسایی ارائه‌دهنده اینترنت، موقعیت جغرافیایی، پروکسی، VPN و خروجی Tor.",
        descriptionRu = "Данные о провайдере, геолокации, VPN, прокси и выходных узлах Tor.",
        useHeadProbe = true
    )
)

/**
 * Concurrently probe all engines using the provided HTTP client and measure real latency.
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
 * Probe a single engine using lightweight HEAD/GET requests without triggering false 4xx "online" states.
 */
suspend fun probeSingleEngine(
    client: HttpClient,
    engine: EngineProbeStatus
): EngineProbeStatus {
    val mark = TimeSource.Monotonic.markNow()
    val online = try {
        val resp = if (engine.useHeadProbe) {
            client.head(engine.probeUrl)
        } else {
            client.get(engine.probeUrl)
        }
        val code = resp.status.value
        when {
            code in 200..299 -> true
            code in 301..308 -> true
            engine.supportsPostOnlyProbe && (resp.status == HttpStatusCode.BadRequest || resp.status == HttpStatusCode.MethodNotAllowed) -> true
            else -> false
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        false
    }
    val elapsed = mark.elapsedNow().inWholeMilliseconds.coerceAtLeast(1L)
    return engine.copy(
        isOnline = online,
        pingMs = if (online) elapsed else 0L,
        hasProbed = true
    )
}
