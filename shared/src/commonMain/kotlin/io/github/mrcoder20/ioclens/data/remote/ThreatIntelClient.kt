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
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "HTTP ${response.status.value}")
        }

        val body = jsonParser.parseToJsonElement(response.bodyAsText()).jsonObject
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
            queryStatus == "no_results" -> {
                SourceResult(name, VerdictLevel.CLEAN, 0, 1, "No active malware payloads recorded in URLhaus")
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
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "HTTP ${response.status.value}")
        }

        val body = jsonParser.parseToJsonElement(response.bodyAsText()).jsonObject
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
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "HTTP ${response.status.value}")
        }

        val body = jsonParser.parseToJsonElement(response.bodyAsText()).jsonObject
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
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Unregistered or no RDAP record")
        }
        if (response.status != HttpStatusCode.OK) {
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "HTTP ${response.status.value}")
        }

        val body = jsonParser.parseToJsonElement(response.bodyAsText()).jsonObject
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
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "HTTP ${response.status.value}")
        }

        val body = jsonParser.parseToJsonElement(response.bodyAsText()).jsonObject
        val dnsStatus = body["Status"]?.jsonPrimitive?.intOrNull ?: -1

        return when (dnsStatus) {
            0 -> SourceResult(name, VerdictLevel.NO_DATA, 0, 1, "DNS Resolution active: NOERROR (Status 0)")
            3 -> SourceResult(name, VerdictLevel.LOW_RISK, 0, 1, "DNS NXDOMAIN: Domain does not resolve (Status 3)")
            else -> SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "DNS Status code: $dnsStatus")
        }
    }
}

// 6. VirusTotal (Authenticated)
class VirusTotalProvider : ThreatIntelProvider {
    override val name = "VirusTotal"
    override val isZeroKey = false

    override fun isApplicable(indicator: Indicator): Boolean = true

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
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "HTTP ${response.status.value}")
        }

        val body = jsonParser.parseToJsonElement(response.bodyAsText()).jsonObject
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

// 7. AbuseIPDB (Authenticated)
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
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "HTTP ${response.status.value}")
        }

        val body = jsonParser.parseToJsonElement(response.bodyAsText()).jsonObject
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

// 8. AlienVault OTX (Authenticated)
class AlienVaultOtxProvider : ThreatIntelProvider {
    override val name = "AlienVault OTX"
    override val isZeroKey = false

    override fun isApplicable(indicator: Indicator): Boolean = true

    override suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult {
        val apiKey = settings.alienVaultApiKey.trim()
        if (apiKey.isBlank()) {
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "API Key required (Configure in Settings)")
        }

        val (section, value) = when (indicator.type) {
            IndicatorType.DOMAIN -> "domain" to indicator.normalizedValue
            IndicatorType.IP_ADDRESS -> "IPv4" to indicator.normalizedValue
            IndicatorType.FILE_HASH -> "file" to indicator.normalizedValue
            IndicatorType.URL -> "url" to indicator.normalizedValue
            IndicatorType.UNKNOWN -> return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "Unknown indicator")
        }

        val response = client.get("https://otx.alienvault.com/api/v1/indicators/$section/$value/general") {
            header("X-OTX-API-KEY", apiKey)
        }

        if (response.status == HttpStatusCode.NotFound) {
            return SourceResult(name, VerdictLevel.CLEAN, 0, 0, "0 threat pulses found in AlienVault OTX")
        }
        if (response.status != HttpStatusCode.OK) {
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "HTTP ${response.status.value}")
        }

        val body = jsonParser.parseToJsonElement(response.bodyAsText()).jsonObject
        val pulseCount = body["pulse_info"]?.jsonObject?.get("count")?.jsonPrimitive?.intOrNull ?: 0

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

// 9. Pulsedive (Authenticated)
class PulsediveProvider : ThreatIntelProvider {
    override val name = "Pulsedive"
    override val isZeroKey = false

    override fun isApplicable(indicator: Indicator): Boolean = true

    override suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult {
        val apiKey = settings.pulsediveApiKey.trim()
        val response = client.get("https://pulsedive.com/api/info.php") {
            parameter("indicator", indicator.normalizedValue)
            if (apiKey.isNotBlank()) parameter("key", apiKey)
        }

        if (response.status != HttpStatusCode.OK) {
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "HTTP ${response.status.value}")
        }

        val body = jsonParser.parseToJsonElement(response.bodyAsText()).jsonObject
        val risk = body["risk"]?.jsonPrimitive?.contentOrNull?.lowercase() ?: "none"

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

// 10. IPQualityScore (Authenticated)
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
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "HTTP ${response.status.value}")
        }

        val body = jsonParser.parseToJsonElement(response.bodyAsText()).jsonObject
        val success = body["success"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: true
        if (!success) {
            val message = body["message"]?.jsonPrimitive?.contentOrNull ?: "IPQS Error"
            return SourceResult(name, VerdictLevel.NO_DATA, 0, 0, "IPQualityScore: $message")
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
