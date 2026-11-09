package io.github.mrcoder20.ioclens.data.remote

import io.github.mrcoder20.ioclens.domain.model.AppSettings
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.SourceResult
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.parameters
import io.ktor.util.encodeBase64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

val jsonParser = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

fun handleHttpError(providerName: String, status: HttpStatusCode): SourceResult {
    return when (status) {
        HttpStatusCode.TooManyRequests -> SourceResult(
            providerName = providerName,
            verdict = VerdictLevel.NO_DATA,
            detectionsCount = 0,
            totalEngineCount = 0,
            summary = "Rate limit exceeded (HTTP 429): API quota reached, retry later",
            isRateLimited = true
        )
        HttpStatusCode.InternalServerError,
        HttpStatusCode.BadGateway,
        HttpStatusCode.ServiceUnavailable,
        HttpStatusCode.GatewayTimeout -> SourceResult(
            providerName = providerName,
            verdict = VerdictLevel.NO_DATA,
            detectionsCount = 0,
            totalEngineCount = 0,
            summary = "Server error (HTTP ${status.value} ${status.description}): Provider temporarily unavailable"
        )
        HttpStatusCode.Unauthorized,
        HttpStatusCode.Forbidden -> SourceResult(
            providerName = providerName,
            verdict = VerdictLevel.NO_DATA,
            detectionsCount = 0,
            totalEngineCount = 0,
            summary = "Authentication failed (HTTP ${status.value}): Invalid or unauthorized API key"
        )
        else -> SourceResult(
            providerName = providerName,
            verdict = VerdictLevel.NO_DATA,
            detectionsCount = 0,
            totalEngineCount = 0,
            summary = "HTTP ${status.value}: ${status.description}"
        )
    }
}

fun safeParseJsonObject(text: String): JsonObject? {
    if (text.isBlank()) return null
    return try {
        jsonParser.parseToJsonElement(text).jsonObject
    } catch (_: Exception) {
        null
    }
}

interface ThreatIntelProvider {
    val name: String
    val isZeroKey: Boolean
    fun isApplicable(indicator: Indicator): Boolean
    suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult
}

// 1. abuse.ch URLhaus (Zero-Key)
class UrlhausProvider : ThreatIntelProvider {
    override val name = "URLhaus (abuse.ch)"
    override val isZeroKey = true

    override fun isApplicable(indicator: Indicator): Boolean {
        return indicator.type == IndicatorType.URL || indicator.type == IndicatorType.DOMAIN
    }

    override suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult {
        val endpoint = if (indicator.type == IndicatorType.URL) {
            "https://urlhaus-api.abuse.ch/v1/url/"
        } else {
            "https://urlhaus-api.abuse.ch/v1/host/"
        }

        val paramName = if (indicator.type == IndicatorType.URL) "url" else "host"
        val response = client.submitForm(
            url = endpoint,
            formParameters = parameters { append(paramName, indicator.normalizedValue) }
        )

        if (response.status != HttpStatusCode.OK) {
            return handleHttpError(name, response.status)
        }

        val body = safeParseJsonObject(response.bodyAsText())
            ?: return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Invalid or corrupted URLhaus response payload")

        val queryStatus = body["query_status"]?.jsonPrimitive?.contentOrNull ?: "unknown"

        return when {
            queryStatus == "ok" -> {
                val threat = body["threat"]?.jsonPrimitive?.contentOrNull ?: "malware"
                val urlStatus = body["url_status"]?.jsonPrimitive?.contentOrNull ?: "active"
                SourceResult(
                    name,
                    VerdictLevel.MALICIOUS,
                    detectionsCount = 1,
                    totalEngineCount = 1,
                    summary = "Known malicious URLhaus threat: $threat ($urlStatus)"
                )
            }
            queryStatus == "no_results" || queryStatus == "no_result" -> {
                SourceResult(name, VerdictLevel.CLEAN, 0, 1, "No active malware payloads recorded in URLhaus")
            }
            queryStatus.contains("rate_limit", ignoreCase = true) -> {
                SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Rate limit exceeded (HTTP 429): API quota reached, retry later", isRateLimited = true)
            }
            else -> {
                SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "URLhaus: $queryStatus")
            }
        }
    }
}

// 2. abuse.ch MalwareBazaar (Zero-Key)
class MalwareBazaarProvider : ThreatIntelProvider {
    override val name = "MalwareBazaar (abuse.ch)"
    override val isZeroKey = true

    override fun isApplicable(indicator: Indicator): Boolean {
        return indicator.type == IndicatorType.FILE_HASH
    }

    override suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult {
        val response = client.submitForm(
            url = "https://mb-api.abuse.ch/api/v1/",
            formParameters = parameters {
                append("query", "get_info")
                append("hash", indicator.normalizedValue)
            }
        )

        if (response.status != HttpStatusCode.OK) {
            return handleHttpError(name, response.status)
        }

        val body = safeParseJsonObject(response.bodyAsText())
            ?: return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Invalid or corrupted MalwareBazaar response payload")

        val queryStatus = body["query_status"]?.jsonPrimitive?.contentOrNull ?: "unknown"

        return when {
            queryStatus == "ok" -> {
                val dataObj = body["data"]?.jsonArray?.firstOrNull()?.jsonObject
                val signature = dataObj?.get("signature")?.jsonPrimitive?.contentOrNull ?: "Known Malware"
                val fileType = dataObj?.get("file_type")?.jsonPrimitive?.contentOrNull ?: "sample"
                SourceResult(
                    name,
                    VerdictLevel.MALICIOUS,
                    detectionsCount = 1,
                    totalEngineCount = 1,
                    summary = "MalwareBazaar confirmed malware: $signature ($fileType)"
                )
            }
            queryStatus == "hash_not_found" -> {
                SourceResult(name, VerdictLevel.CLEAN, 0, 1, "Hash not observed in MalwareBazaar repository")
            }
            else -> {
                SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "MalwareBazaar: $queryStatus")
            }
        }
    }
}

// 3. abuse.ch ThreatFox (Zero-Key)
class ThreatFoxProvider : ThreatIntelProvider {
    override val name = "ThreatFox (abuse.ch)"
    override val isZeroKey = true

    override fun isApplicable(indicator: Indicator): Boolean {
        return indicator.type != IndicatorType.UNKNOWN
    }

    override suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult {
        val response = client.post("https://threatfox-api.abuse.ch/api/v1/") {
            contentType(ContentType.Application.Json)
            setBody("""{"query":"search_ioc","search_term":"${indicator.normalizedValue}"}""")
        }

        if (response.status != HttpStatusCode.OK) {
            return handleHttpError(name, response.status)
        }

        val body = safeParseJsonObject(response.bodyAsText())
            ?: return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Invalid or corrupted ThreatFox response payload")

        val queryStatus = body["query_status"]?.jsonPrimitive?.contentOrNull ?: "unknown"

        return when {
            queryStatus == "ok" -> {
                val dataList = body["data"]?.jsonArray
                val matchCount = dataList?.size ?: 1
                val firstMatch = dataList?.firstOrNull()?.jsonObject
                val threatType = firstMatch?.get("threat_type")?.jsonPrimitive?.contentOrNull ?: "malicious_ioc"
                val confidence = firstMatch?.get("confidence_level")?.jsonPrimitive?.intOrNull ?: 100
                SourceResult(
                    name,
                    VerdictLevel.MALICIOUS,
                    detectionsCount = matchCount,
                    totalEngineCount = matchCount,
                    summary = "ThreatFox IOC matched: $threatType (Confidence: $confidence%)"
                )
            }
            queryStatus == "no_result" || queryStatus == "no_results" -> {
                SourceResult(name, VerdictLevel.CLEAN, 0, 1, "No IOC records associated in ThreatFox")
            }
            else -> {
                SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "ThreatFox: $queryStatus")
            }
        }
    }
}

// 4. RDAP Network Registration (Zero-Key)
class RdapProvider : ThreatIntelProvider {
    override val name = "RDAP / Registry"
    override val isZeroKey = true

    override fun isApplicable(indicator: Indicator): Boolean {
        return indicator.type == IndicatorType.IP_ADDRESS || indicator.type == IndicatorType.DOMAIN
    }

    override suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult {
        val path = if (indicator.type == IndicatorType.IP_ADDRESS) "ip" else "domain"
        val response = client.get("https://rdap.org/$path/${indicator.normalizedValue}")

        if (response.status == HttpStatusCode.NotFound) {
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Unregistered or no RDAP record (HTTP 404)")
        }
        if (response.status != HttpStatusCode.OK) {
            return handleHttpError(name, response.status)
        }

        val body = safeParseJsonObject(response.bodyAsText())
            ?: return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Invalid or corrupted RDAP response payload")

        val handle = body["handle"]?.jsonPrimitive?.contentOrNull
        val netName = body["name"]?.jsonPrimitive?.contentOrNull ?: handle ?: "Allocated Network"
        val country = body["country"]?.jsonPrimitive?.contentOrNull ?: ""

        val info = if (country.isNotBlank()) "$netName [$country]" else netName
        return SourceResult(name, VerdictLevel.NO_DATA, 0, 1, "RDAP Network Allocation: $info")
    }
}

// 5. Cloudflare DNS over HTTPS (Zero-Key)
class CloudflareDnsProvider : ThreatIntelProvider {
    override val name = "Cloudflare DoH"
    override val isZeroKey = true

    override fun isApplicable(indicator: Indicator): Boolean {
        return indicator.type == IndicatorType.DOMAIN
    }

    override suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult {
        val response = client.get("https://cloudflare-dns.com/dns-query") {
            parameter("name", indicator.normalizedValue)
            parameter("type", "A")
            header("Accept", "application/dns-json")
        }

        if (response.status != HttpStatusCode.OK) {
            return handleHttpError(name, response.status)
        }

        val body = safeParseJsonObject(response.bodyAsText())
            ?: return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Invalid or corrupted DNS response payload")

        val dnsStatus = body["Status"]?.jsonPrimitive?.intOrNull ?: -1

        return when (dnsStatus) {
            0 -> SourceResult(name, VerdictLevel.NO_DATA, 0, 1, "DNS Resolution active: NOERROR (Status 0): Domain successfully resolved")
            1 -> SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "DNS FORMERR: Format error in DNS query (Status 1)")
            2 -> SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "DNS SERVFAIL: Server failure while resolving domain (Status 2)")
            3 -> SourceResult(name, VerdictLevel.LOW_RISK, 0, 1, "DNS NXDOMAIN: Domain does not resolve (Status 3)")
            4 -> SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "DNS NOTIMP: Query type not implemented (Status 4)")
            5 -> SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "DNS REFUSED: DNS server refused query (Status 5)")
            else -> SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "DNS Status code: $dnsStatus")
        }
    }
}

// 6. Shodan InternetDB (Zero-Key)
class ShodanInternetDbProvider : ThreatIntelProvider {
    override val name = "Shodan InternetDB"
    override val isZeroKey = true

    override fun isApplicable(indicator: Indicator): Boolean {
        return indicator.type == IndicatorType.IP_ADDRESS
    }

    override suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult {
        val response = client.get("https://internetdb.shodan.io/${indicator.normalizedValue}")

        if (response.status == HttpStatusCode.NotFound) {
            return SourceResult(
                name,
                VerdictLevel.CLEAN,
                detectionsCount = 0,
                totalEngineCount = 1,
                summary = "No exposed ports or CVE vulnerabilities in Shodan InternetDB"
            )
        }
        if (response.status != HttpStatusCode.OK) {
            return handleHttpError(name, response.status)
        }

        val body = safeParseJsonObject(response.bodyAsText())
            ?: return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Invalid Shodan InternetDB response payload")

        val vulns = (body["vulns"] ?: body["cves"])?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
        val ports = body["ports"]?.jsonArray?.mapNotNull { it.jsonPrimitive.intOrNull } ?: emptyList()
        val hostnames = body["hostnames"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()

        val verdict = when {
            vulns.isNotEmpty() -> VerdictLevel.MALICIOUS
            ports.isNotEmpty() -> VerdictLevel.LOW_RISK
            else -> VerdictLevel.CLEAN
        }

        val summary = when {
            vulns.isNotEmpty() -> "Shodan: ${vulns.size} CVEs (${vulns.take(3).joinToString()}), ${ports.size} open ports"
            ports.isNotEmpty() -> "Shodan: ${ports.size} open ports (${ports.take(5).joinToString()})" + if (hostnames.isNotEmpty()) " [${hostnames.first()}]" else ""
            else -> "Shodan: No open ports or known vulnerabilities found"
        }

        return SourceResult(
            name,
            verdict,
            detectionsCount = vulns.size,
            totalEngineCount = if (ports.isNotEmpty()) ports.size else 1,
            summary = summary
        )
    }
}

// 7. AlienVault OTX Public API (Zero-Key)
class AlienVaultOtxProvider : ThreatIntelProvider {
    override val name = "AlienVault OTX"
    override val isZeroKey = true

    override fun isApplicable(indicator: Indicator): Boolean {
        return indicator.type != IndicatorType.UNKNOWN
    }

    override suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult {
        val (section, value) = when (indicator.type) {
            IndicatorType.DOMAIN -> "domain" to indicator.normalizedValue
            IndicatorType.IP_ADDRESS -> "IPv4" to indicator.normalizedValue
            IndicatorType.FILE_HASH -> "file" to indicator.normalizedValue
            IndicatorType.URL -> "url" to indicator.normalizedValue
            IndicatorType.UNKNOWN -> return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Unknown indicator")
        }

        val apiKey = settings.alienVaultApiKey.trim()
        val response = client.get("https://otx.alienvault.com/api/v1/indicators/$section/$value/general") {
            if (apiKey.isNotBlank()) {
                header("X-OTX-API-KEY", apiKey)
            }
        }

        if (response.status == HttpStatusCode.NotFound) {
            return SourceResult(name, VerdictLevel.CLEAN, 0, 0, "0 threat pulses found in AlienVault OTX")
        }
        if (response.status != HttpStatusCode.OK) {
            return handleHttpError(name, response.status)
        }

        val body = safeParseJsonObject(response.bodyAsText())
            ?: return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Invalid AlienVault OTX response payload")

        val pulseInfo = body["pulse_info"]?.jsonObject
            ?: return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Invalid AlienVault OTX response payload: missing pulse_info")

        val pulseCount = pulseInfo["count"]?.jsonPrimitive?.intOrNull ?: 0

        val verdict = when {
            pulseCount >= 3 -> VerdictLevel.MALICIOUS
            pulseCount in 1..2 -> VerdictLevel.SUSPICIOUS
            else -> VerdictLevel.CLEAN
        }

        return SourceResult(
            name,
            verdict,
            detectionsCount = pulseCount,
            totalEngineCount = 50,
            summary = "$pulseCount active threat intelligence pulses recorded"
        )
    }
}

// 8. RIPE Stat / BGPView (Zero-Key)
class RipeStatProvider : ThreatIntelProvider {
    override val name = "RIPE Stat"
    override val isZeroKey = true

    override fun isApplicable(indicator: Indicator): Boolean {
        return indicator.type == IndicatorType.IP_ADDRESS
    }

    override suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult {
        val response = client.get("https://stat.ripe.net/data/network-info/data.json") {
            parameter("resource", indicator.normalizedValue)
        }

        if (response.status != HttpStatusCode.OK) {
            return handleHttpError(name, response.status)
        }

        val body = safeParseJsonObject(response.bodyAsText())
            ?: return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Invalid RIPE Stat response payload")

        val data = body["data"]?.jsonObject
        val asns = data?.get("asns")?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
        val prefix = data?.get("prefix")?.jsonPrimitive?.contentOrNull ?: "Unknown Prefix"

        val asnStr = if (asns.isNotEmpty()) asns.joinToString { "AS$it" } else "Unannounced ASN"
        return SourceResult(
            name,
            VerdictLevel.NO_DATA,
            detectionsCount = 0,
            totalEngineCount = 1,
            summary = "RIPE Stat Routing: $asnStr (Prefix: $prefix)"
        )
    }
}

// 9. crt.sh Certificate Transparency (Zero-Key)
class CrtShProvider : ThreatIntelProvider {
    override val name = "crt.sh"
    override val isZeroKey = true

    override fun isApplicable(indicator: Indicator): Boolean {
        return indicator.type == IndicatorType.DOMAIN
    }

    override suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult {
        val response = client.get("https://crt.sh/") {
            parameter("q", indicator.normalizedValue)
            parameter("output", "json")
        }

        if (response.status == HttpStatusCode.NotFound) {
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "No SSL certificates logged in Certificate Transparency")
        }
        if (response.status != HttpStatusCode.OK) {
            return handleHttpError(name, response.status)
        }

        val text = response.bodyAsText().trim()
        if (text.isEmpty() || text == "[]") {
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "No SSL certificates logged in Certificate Transparency")
        }

        val certCount = try {
            jsonParser.parseToJsonElement(text).jsonArray.size
        } catch (_: Exception) {
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Invalid Certificate Transparency response payload")
        }

        return SourceResult(
            name,
            VerdictLevel.NO_DATA,
            detectionsCount = 0,
            totalEngineCount = certCount,
            summary = "Certificate Transparency: $certCount logged certificates discovered"
        )
    }
}

// 10. ipwho.is Threat & Geolocation (Zero-Key)
class IpWhoIsProvider : ThreatIntelProvider {
    override val name = "ipwho.is"
    override val isZeroKey = true

    override fun isApplicable(indicator: Indicator): Boolean {
        return indicator.type == IndicatorType.IP_ADDRESS
    }

    override suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult {
        val response = client.get("https://ipwho.is/${indicator.normalizedValue}")

        if (response.status != HttpStatusCode.OK) {
            return handleHttpError(name, response.status)
        }

        val body = safeParseJsonObject(response.bodyAsText())
            ?: return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Invalid ipwho.is response payload")

        val success = body["success"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
        if (!success) {
            val message = body["message"]?.jsonPrimitive?.contentOrNull ?: "IP lookup unsuccessful"
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "ipwho.is: $message")
        }

        val security = body["security"]?.jsonObject
        val isTor = security?.get("tor")?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
        val isProxy = security?.get("proxy")?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
        val isVpn = security?.get("vpn")?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
        val isAnonymous = security?.get("anonymous")?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false

        val connection = body["connection"]?.jsonObject
        val isp = connection?.get("isp")?.jsonPrimitive?.contentOrNull ?: "Unknown ISP"
        val country = body["country"]?.jsonPrimitive?.contentOrNull ?: ""

        val verdict = when {
            isTor -> VerdictLevel.MALICIOUS
            isProxy || isVpn || isAnonymous -> VerdictLevel.SUSPICIOUS
            else -> VerdictLevel.CLEAN
        }

        val summary = when {
            isTor -> "ipwho.is: Active TOR exit node ($isp, $country)"
            isProxy || isVpn -> "ipwho.is: Proxy/VPN anonymous endpoint ($isp, $country)"
            else -> "ipwho.is: Clean verified ISP connection ($isp, $country)"
        }

        return SourceResult(
            name,
            verdict,
            detectionsCount = if (isTor || isProxy || isVpn) 1 else 0,
            totalEngineCount = 1,
            summary = summary
        )
    }
}

// 10. abuse.ch Feodo Tracker (Zero-Key C2 Botnet & IP Reputation)
class FeodoTrackerProvider(
    private val endpointUrl: String = "https://feodotracker.abuse.ch/downloads/ipblocklist.json"
) : ThreatIntelProvider {
    override val name = "Feodo Tracker (abuse.ch)"
    override val isZeroKey = true

    override fun isApplicable(indicator: Indicator): Boolean {
        return indicator.type == IndicatorType.IP_ADDRESS
    }

    override suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult {
        val response = try {
            client.get(endpointUrl)
        } catch (e: Exception) {
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Feodo Tracker unavailable: ${e.message ?: "Network error"}")
        }

        if (response.status != HttpStatusCode.OK) {
            return handleHttpError(name, response.status)
        }

        val text = response.bodyAsText().trim()
        if (text.isEmpty() || text == "[]") {
            return SourceResult(name, VerdictLevel.CLEAN, 0, 1, "No active C2 botnets recorded in Feodo Tracker")
        }

        val jsonArray = try {
            jsonParser.parseToJsonElement(text).jsonArray
        } catch (_: Exception) {
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Invalid or corrupted Feodo Tracker response payload")
        }

        val targetIp = indicator.normalizedValue.substringBefore(':').lowercase()
        val match = jsonArray.mapNotNull { it as? JsonObject }.firstOrNull { obj ->
            obj["ip_address"]?.jsonPrimitive?.contentOrNull?.lowercase() == targetIp
        }

        return if (match != null) {
            val malware = match["malware"]?.jsonPrimitive?.contentOrNull ?: "Botnet C2"
            val status = match["status"]?.jsonPrimitive?.contentOrNull ?: "active"
            val port = match["port"]?.jsonPrimitive?.intOrNull
            val portSuffix = if (port != null) ":$port" else ""
            SourceResult(
                providerName = name,
                verdict = VerdictLevel.MALICIOUS,
                detectionsCount = 1,
                totalEngineCount = 1,
                summary = "Feodo Tracker confirmed C2 botnet node: $malware ($status$portSuffix)"
            )
        } else {
            SourceResult(
                providerName = name,
                verdict = VerdictLevel.CLEAN,
                detectionsCount = 0,
                totalEngineCount = 1,
                summary = "No active C2 botnet activity identified in Feodo Tracker"
            )
        }
    }
}

// 6. VirusTotal (Authenticated - Legacy)
@Deprecated("Retired in M1: Zero-Key Threat Intel Architecture.", level = DeprecationLevel.WARNING)
class VirusTotalProvider : ThreatIntelProvider {
    override val name = "VirusTotal"
    override val isZeroKey = false

    override fun isApplicable(indicator: Indicator): Boolean {
        return indicator.type != IndicatorType.UNKNOWN
    }

    override suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult {
        val apiKey = settings.virusTotalApiKey.trim()
        if (apiKey.isBlank()) {
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "API Key required (Configure in Settings)")
        }

        val endpoint = when (indicator.type) {
            IndicatorType.FILE_HASH -> "https://www.virustotal.com/api/v3/files/${indicator.normalizedValue}"
            IndicatorType.IP_ADDRESS -> "https://www.virustotal.com/api/v3/ip_addresses/${indicator.normalizedValue}"
            IndicatorType.DOMAIN -> "https://www.virustotal.com/api/v3/domains/${indicator.normalizedValue}"
            IndicatorType.URL -> {
                val urlId = indicator.normalizedValue.encodeBase64().trimEnd('=').replace('+', '-').replace('/', '_')
                "https://www.virustotal.com/api/v3/urls/$urlId"
            }
            IndicatorType.UNKNOWN -> return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Unknown indicator")
        }

        val response = client.get(endpoint) {
            header("x-apikey", apiKey)
        }

        if (response.status == HttpStatusCode.NotFound) {
            return SourceResult(name, VerdictLevel.CLEAN, 0, 70, "No records found in VirusTotal database")
        }
        if (response.status != HttpStatusCode.OK) {
            return handleHttpError(name, response.status)
        }

        val body = safeParseJsonObject(response.bodyAsText())
            ?: return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Invalid or corrupted VirusTotal response payload")

        val stats = body["data"]?.jsonObject?.get("attributes")?.jsonObject?.get("last_analysis_stats")?.jsonObject
        val malicious = stats?.get("malicious")?.jsonPrimitive?.intOrNull ?: 0
        val suspicious = stats?.get("suspicious")?.jsonPrimitive?.intOrNull ?: 0
        val harmless = stats?.get("harmless")?.jsonPrimitive?.intOrNull ?: 0
        val undetected = stats?.get("undetected")?.jsonPrimitive?.intOrNull ?: 0
        val total = malicious + suspicious + harmless + undetected

        val verdict = when {
            malicious >= 3 -> VerdictLevel.MALICIOUS
            malicious in 1..2 || suspicious > 1 -> VerdictLevel.SUSPICIOUS
            malicious == 0 && suspicious <= 1 && total > 0 -> VerdictLevel.CLEAN
            else -> VerdictLevel.NO_DATA
        }

        return SourceResult(
            name,
            verdict,
            detectionsCount = malicious + suspicious,
            totalEngineCount = if (total > 0) total else 70,
            summary = "$malicious vendors flagged as malicious, $suspicious suspicious (out of $total)"
        )
    }
}

// 7. AbuseIPDB (Authenticated - Legacy)
@Deprecated("Retired in M1: Zero-Key Threat Intel Architecture.", level = DeprecationLevel.WARNING)
class AbuseIpDbProvider : ThreatIntelProvider {
    override val name = "AbuseIPDB"
    override val isZeroKey = false

    override fun isApplicable(indicator: Indicator): Boolean {
        return indicator.type == IndicatorType.IP_ADDRESS
    }

    override suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult {
        val apiKey = settings.abuseIpDbApiKey.trim()
        if (apiKey.isBlank()) {
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "API Key required (Configure in Settings)")
        }

        val response = client.get("https://api.abuseipdb.com/api/v2/check") {
            header("Key", apiKey)
            header("Accept", "application/json")
            parameter("ipAddress", indicator.normalizedValue)
            parameter("maxAgeInDays", "90")
        }

        if (response.status != HttpStatusCode.OK) {
            return handleHttpError(name, response.status)
        }

        val body = safeParseJsonObject(response.bodyAsText())
            ?: return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Invalid or corrupted AbuseIPDB response payload")

        val data = body["data"]?.jsonObject
        val score = data?.get("abuseConfidenceScore")?.jsonPrimitive?.intOrNull ?: 0
        val reports = data?.get("totalReports")?.jsonPrimitive?.intOrNull ?: 0

        val verdict = when {
            score >= 50 -> VerdictLevel.MALICIOUS
            score in 20..49 -> VerdictLevel.SUSPICIOUS
            score in 1..19 -> VerdictLevel.LOW_RISK
            else -> VerdictLevel.CLEAN
        }

        return SourceResult(
            name,
            verdict,
            detectionsCount = reports,
            totalEngineCount = 100,
            summary = "Abuse confidence score: $score% ($reports reports in last 90 days)"
        )
    }
}


// 9. Pulsedive (Authenticated - Legacy)
@Deprecated("Retired in M1: Zero-Key Threat Intel Architecture.", level = DeprecationLevel.WARNING)
class PulsediveProvider : ThreatIntelProvider {
    override val name = "Pulsedive"
    override val isZeroKey = false

    override fun isApplicable(indicator: Indicator): Boolean {
        return indicator.type != IndicatorType.UNKNOWN
    }

    override suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult {
        val apiKey = settings.pulsediveApiKey.trim()
        val response = client.get("https://pulsedive.com/api/info.php") {
            parameter("indicator", indicator.normalizedValue)
            if (apiKey.isNotBlank()) parameter("key", apiKey)
        }

        if (response.status != HttpStatusCode.OK) {
            return handleHttpError(name, response.status)
        }

        val body = safeParseJsonObject(response.bodyAsText())
            ?: return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Invalid or corrupted Pulsedive response payload")

        val error = body["error"]?.jsonPrimitive?.contentOrNull
        if (error != null) {
            val isRate = error.contains("rate limit", ignoreCase = true) || error.contains("quota", ignoreCase = true)
            return SourceResult(
                name,
                VerdictLevel.NO_DATA,
                0,
                0,
                if (isRate) "Rate limit exceeded (HTTP 429): API quota reached, retry later" else "Pulsedive: $error",
                isRateLimited = isRate
            )
        }

        val risk = body["risk"]?.jsonPrimitive?.contentOrNull?.lowercase()
        if (risk == null) {
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Pulsedive: No risk assessment in response")
        }

        val verdict = when (risk) {
            "critical", "high" -> VerdictLevel.MALICIOUS
            "medium" -> VerdictLevel.SUSPICIOUS
            "low" -> VerdictLevel.LOW_RISK
            "none" -> VerdictLevel.CLEAN
            else -> VerdictLevel.NO_DATA
        }

        val threats = body["threats"]?.jsonArray?.size ?: 0
        return SourceResult(
            name,
            verdict,
            detectionsCount = threats,
            totalEngineCount = 10,
            summary = "Pulsedive risk level: ${risk.uppercase()} ($threats associated threats)"
        )
    }
}

// 10. IPQualityScore (Authenticated - Legacy)
@Deprecated("Retired in M1: Zero-Key Threat Intel Architecture.", level = DeprecationLevel.WARNING)
class IpQualityScoreProvider : ThreatIntelProvider {
    override val name = "IPQualityScore"
    override val isZeroKey = false

    override fun isApplicable(indicator: Indicator): Boolean {
        return indicator.type == IndicatorType.IP_ADDRESS
    }

    override suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult {
        val apiKey = settings.ipQualityScoreApiKey.trim()
        if (apiKey.isBlank()) {
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "API Key required (Configure in Settings)")
        }

        val response = client.get("https://www.ipqualityscore.com/api/json/ip/$apiKey/${indicator.normalizedValue}") {
            parameter("strictness", "1")
        }

        if (response.status != HttpStatusCode.OK) {
            return handleHttpError(name, response.status)
        }

        val body = safeParseJsonObject(response.bodyAsText())
            ?: return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Invalid or corrupted IPQualityScore response payload")

        val success = body["success"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: true
        if (!success) {
            val message = body["message"]?.jsonPrimitive?.contentOrNull ?: "IPQS Error"
            val isRate = message.contains("quota", ignoreCase = true) || message.contains("rate limit", ignoreCase = true)
            return SourceResult(
                name,
                VerdictLevel.NO_DATA,
                0,
                0,
                if (isRate) "Rate limit exceeded (HTTP 429): API quota reached, retry later" else "IPQualityScore: $message",
                isRateLimited = isRate
            )
        }

        val fraudScore = body["fraud_score"]?.jsonPrimitive?.intOrNull ?: 0
        val recentAbuse = body["recent_abuse"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
        val isBot = body["bot_status"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false

        val verdict = when {
            fraudScore >= 75 || recentAbuse -> VerdictLevel.MALICIOUS
            fraudScore in 50..74 || isBot -> VerdictLevel.SUSPICIOUS
            fraudScore in 20..49 -> VerdictLevel.LOW_RISK
            else -> VerdictLevel.CLEAN
        }

        return SourceResult(
            name,
            verdict,
            detectionsCount = if (fraudScore >= 50) 1 else 0,
            totalEngineCount = 100,
            summary = "IPQualityScore Fraud Score: $fraudScore% (Recent abuse: $recentAbuse, Bot: $isBot)"
        )
    }
}
