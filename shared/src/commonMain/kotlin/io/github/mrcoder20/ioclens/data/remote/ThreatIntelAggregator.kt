package io.github.mrcoder20.ioclens.data.remote

import io.github.mrcoder20.ioclens.domain.model.AppSettings
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.SourceResult
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull

class ThreatIntelAggregator(
    private val client: HttpClient,
    private val timeoutMs: Long = 10000L,
    private val providers: List<ThreatIntelProvider> = listOf(
        UrlhausProvider(),
        MalwareBazaarProvider(),
        ThreatFoxProvider(),
        RdapProvider(),
        CloudflareDnsProvider(),
        VirusTotalProvider(),
        AbuseIpDbProvider(),
        AlienVaultOtxProvider(),
        PulsediveProvider(),
        IpQualityScoreProvider()
    )
) {

    suspend fun aggregate(indicator: Indicator, settings: AppSettings): TriageVerdict {
        if (indicator.isInternalOrPrivate) {
            return TriageVerdict(
                indicator = indicator,
                overallVerdict = VerdictLevel.CLEAN,
                confidenceScore = 0,
                summaryDescription = "🛡️ Privacy Guard Activated: Private/Internal indicator detected. Remote lookups blocked to protect internal infrastructure.",
                sourceResults = listOf(
                    SourceResult("Privacy Guard Local", VerdictLevel.CLEAN, 0, 1, "Internal indicator safely blocked locally")
                )
            )
        }

        if (indicator.type == IndicatorType.UNKNOWN) {
            return TriageVerdict(
                indicator = indicator,
                overallVerdict = VerdictLevel.NO_DATA,
                confidenceScore = 0,
                summaryDescription = "⚪ Unrecognized indicator format. Please provide a valid IP, Domain, URL, or File Hash.",
                sourceResults = emptyList()
            )
        }

        val applicable = providers.filter { it.isApplicable(indicator) }
        if (applicable.isEmpty()) {
            return TriageVerdict(
                indicator = indicator,
                overallVerdict = VerdictLevel.NO_DATA,
                confidenceScore = 0,
                summaryDescription = "⚪ No threat intelligence engines applicable for this indicator type.",
                sourceResults = emptyList()
            )
        }

        val results: List<SourceResult> = supervisorScope {
            applicable.map { provider ->
                async {
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
