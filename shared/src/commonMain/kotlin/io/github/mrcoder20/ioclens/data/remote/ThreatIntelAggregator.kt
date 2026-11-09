package io.github.mrcoder20.ioclens.data.remote

import io.github.mrcoder20.ioclens.domain.model.AppSettings
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.SourceResult
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import io.github.mrcoder20.ioclens.domain.parser.PrivacyGuardValidator
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

val DEFAULT_ZERO_KEY_PROVIDERS: List<ThreatIntelProvider> = listOf(
    UrlhausProvider(),
    MalwareBazaarProvider(),
    ThreatFoxProvider(),
    RdapProvider(),
    CloudflareDnsProvider(),
    ShodanInternetDbProvider(),
    AlienVaultOtxProvider(),
    RipeStatProvider(),
    CrtShProvider(),
    FeodoTrackerProvider()
)

val ALL_PROVIDERS: List<ThreatIntelProvider> = listOf(
    @Suppress("DEPRECATION") VirusTotalProvider(),
    @Suppress("DEPRECATION") AbuseIpDbProvider(),
    @Suppress("DEPRECATION") PulsediveProvider(),
    @Suppress("DEPRECATION") IpQualityScoreProvider()
) + DEFAULT_ZERO_KEY_PROVIDERS + listOf(
    IpWhoIsProvider()
)

class ThreatIntelAggregator(
    private val client: HttpClient = createZeroKeyHttpClient(),
    val timeoutMs: Long = 10000L,
    val providers: List<ThreatIntelProvider> = ALL_PROVIDERS
) {

    suspend fun aggregate(
        indicator: Indicator,
        settings: AppSettings = AppSettings(),
        onProgress: ((finishedCount: Int, totalCount: Int) -> Unit)? = null
    ): TriageVerdict {
        if (indicator.isInternalOrPrivate || PrivacyGuardValidator.isPrivateOrInternal(indicator)) {
            onProgress?.invoke(1, 1)
            return PrivacyGuardValidator.createPrivacyGuardVerdict(indicator)
        }

        if (indicator.type == IndicatorType.UNKNOWN) {
            onProgress?.invoke(0, 0)
            return TriageVerdict(
                indicator = indicator,
                overallVerdict = VerdictLevel.NO_DATA,
                confidenceScore = 0,
                summaryDescription = "⚪ Unrecognized indicator format. Please provide a valid IP, Domain, URL, or File Hash.",
                sourceResults = emptyList()
            )
        }

        val applicable = providers.filter { provider ->
            if (!provider.isApplicable(indicator)) return@filter false
            if (provider.isZeroKey) return@filter true
            // Authenticated providers run only when their specific key is configured in settings
            when (provider.name) {
                "VirusTotal" -> settings.virusTotalApiKey.isNotBlank()
                "AbuseIPDB" -> settings.abuseIpDbApiKey.isNotBlank()
                "Pulsedive" -> settings.pulsediveApiKey.isNotBlank()
                "IPQualityScore" -> settings.ipQualityScoreApiKey.isNotBlank()
                else -> true
            }
        }
        if (applicable.isEmpty()) {
            onProgress?.invoke(0, 0)
            return TriageVerdict(
                indicator = indicator,
                overallVerdict = VerdictLevel.NO_DATA,
                confidenceScore = 0,
                summaryDescription = "⚪ No threat intelligence engines applicable for this indicator type.",
                sourceResults = emptyList()
            )
        }

        val totalCount = applicable.size
        val counterMutex = Mutex()
        var finishedCount = 0
        onProgress?.invoke(0, totalCount)

        val semaphore = Semaphore(permits = 6)

        val results: List<SourceResult> = supervisorScope {
            applicable.map { provider ->
                async {
                    val res = semaphore.withPermit {
                        try {
                            withTimeoutOrNull(timeoutMs) {
                                provider.query(client, indicator, settings)
                            } ?: SourceResult(
                                providerName = provider.name,
                                verdict = VerdictLevel.NO_DATA,
                                detectionsCount = 0,
                                totalEngineCount = 0,
                                summary = "Connection timed out after ${timeoutMs}ms (Provider skipped)"
                            )
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            SourceResult(
                                providerName = provider.name,
                                verdict = VerdictLevel.NO_DATA,
                                detectionsCount = 0,
                                totalEngineCount = 0,
                                summary = "Service unavailable or filtered: ${e.message ?: "Skipped"}"
                            )
                        }
                    }

                    val current = counterMutex.withLock { ++finishedCount }
                    onProgress?.invoke(current, totalCount)

                    res
                }
            }.awaitAll()
        }

        return synthesizeVerdict(indicator, results)
    }

    private fun synthesizeVerdict(indicator: Indicator, results: List<SourceResult>): TriageVerdict {
        val maliciousSources = results.filter { it.verdict == VerdictLevel.MALICIOUS }
        val suspiciousSources = results.filter { it.verdict == VerdictLevel.SUSPICIOUS }
        val lowRiskSources = results.filter { it.verdict == VerdictLevel.LOW_RISK }
        val cleanSources = results.filter { it.verdict == VerdictLevel.CLEAN }

        val maliciousCount = maliciousSources.size
        val suspiciousCount = suspiciousSources.size
        val cleanCount = cleanSources.size

        val overallVerdict: VerdictLevel
        val score: Int
        val summary: String

        when {
            maliciousCount > 0 -> {
                overallVerdict = VerdictLevel.MALICIOUS
                score = (70 + maliciousCount * 15 + suspiciousCount * 5).coerceAtMost(100)
                val names = maliciousSources.joinToString(", ") { it.providerName }
                summary = "🚨 MALICIOUS PAYLOAD DETECTED: Corroborated by $names."
            }
            suspiciousCount > 0 -> {
                overallVerdict = VerdictLevel.SUSPICIOUS
                score = (35 + suspiciousCount * 15 + lowRiskSources.size * 5).coerceAtMost(69)
                val names = suspiciousSources.joinToString(", ") { it.providerName }
                summary = "🟠 Suspicious Activity: Threat signals flagged by $names."
            }
            lowRiskSources.isNotEmpty() -> {
                overallVerdict = VerdictLevel.LOW_RISK
                score = 20
                summary = "🟡 Low Risk: Minor telemetry flags observed, but no active malware signature confirmed."
            }
            cleanCount > 0 -> {
                overallVerdict = VerdictLevel.CLEAN
                score = 5
                summary = "🟢 Verified Clean: $cleanCount threat intelligence engines observed no malicious activity."
            }
            else -> {
                overallVerdict = VerdictLevel.NO_DATA
                score = 0
                summary = "⚪ No threat records found across active community intelligence engines."
            }
        }

        return TriageVerdict(
            indicator = indicator,
            overallVerdict = overallVerdict,
            confidenceScore = score,
            summaryDescription = summary,
            sourceResults = results
        )
    }
}
