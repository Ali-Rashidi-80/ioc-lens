package io.github.mrcoder20.ioclens.data.remote

import io.github.mrcoder20.ioclens.currentTimeMillis
import io.github.mrcoder20.ioclens.domain.model.AppSettings
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.SourceResult
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import io.github.mrcoder20.ioclens.domain.parser.PrivacyGuardValidator
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

fun createDefaultZeroKeyProviders(): List<ThreatIntelProvider> = listOf(
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

fun createAllProviders(): List<ThreatIntelProvider> = listOf(
    VirusTotalProvider(),
    AbuseIpDbProvider(),
    PulsediveProvider(),
    IpQualityScoreProvider()
) + createDefaultZeroKeyProviders() + listOf(
    IpWhoIsProvider()
)

val DEFAULT_ZERO_KEY_PROVIDERS: List<ThreatIntelProvider> = createDefaultZeroKeyProviders()

val ALL_PROVIDERS: List<ThreatIntelProvider> = createAllProviders()

class ThreatIntelAggregator(
    private val client: HttpClient = createZeroKeyHttpClient(),
    val timeoutMs: Long = AppSettings.DEFAULT_REQUEST_TIMEOUT_MS,
    val providers: List<ThreatIntelProvider> = createAllProviders(),
    private val workDispatcher: CoroutineDispatcher? = null
) {
    private data class CircuitState(val consecutiveFailures: Int = 0, val openUntilMs: Long = 0L)

    private val circuitMutex = Mutex()
    private val circuits = mutableMapOf<String, CircuitState>()
    private val inFlightMutex = Mutex()
    private val inFlightQueries = mutableMapOf<String, CompletableDeferred<TriageVerdict>>()

    suspend fun aggregate(
        indicator: Indicator,
        settings: AppSettings = AppSettings(),
        onProgress: ((finishedCount: Int, totalCount: Int) -> Unit)? = null
    ): TriageVerdict {
        if (settings.privacyGuardEnabled &&
            (indicator.isInternalOrPrivate || PrivacyGuardValidator.isPrivateOrInternal(indicator, settings.customPrivacyRules))
        ) {
            onProgress?.invoke(1, 1)
            return PrivacyGuardValidator.createPrivacyGuardVerdict(indicator).copy(
                timestampEpochMs = currentTimeMillis()
            )
        }

        if (indicator.type == IndicatorType.UNKNOWN) {
            onProgress?.invoke(0, 0)
            return TriageVerdict(
                indicator = indicator,
                overallVerdict = VerdictLevel.NO_DATA,
                confidenceScore = 0,
                summaryDescription = "Unrecognized indicator format. Please provide a valid IP, Domain, URL, or File Hash.",
                sourceResults = emptyList(),
                timestampEpochMs = currentTimeMillis()
            )
        }

        val dedupKey = buildDedupKey(indicator, settings)
        val (deferred, isLeader) = inFlightMutex.withLock {
            val existing = inFlightQueries[dedupKey]
            if (existing != null) {
                existing to false
            } else {
                val created = CompletableDeferred<TriageVerdict>()
                inFlightQueries[dedupKey] = created
                created to true
            }
        }

        if (!isLeader) {
            val shared = deferred.await()
            onProgress?.invoke(shared.sourceResults.size, shared.sourceResults.size)
            return shared
        }

        return try {
            val verdict = executeAggregate(indicator, settings, onProgress)
            deferred.complete(verdict)
            verdict
        } catch (t: Throwable) {
            deferred.completeExceptionally(t)
            throw t
        } finally {
            inFlightMutex.withLock {
                if (inFlightQueries[dedupKey] === deferred) {
                    inFlightQueries.remove(dedupKey)
                }
            }
        }
    }

    private suspend fun executeAggregate(
        indicator: Indicator,
        settings: AppSettings,
        onProgress: ((finishedCount: Int, totalCount: Int) -> Unit)?
    ): TriageVerdict {
        val effectiveTimeoutMs = if (timeoutMs != AppSettings.DEFAULT_REQUEST_TIMEOUT_MS) {
            timeoutMs
        } else {
            settings.requestTimeoutMs.coerceIn(AppSettings.MIN_REQUEST_TIMEOUT_MS, AppSettings.MAX_REQUEST_TIMEOUT_MS)
        }

        val applicable = providers.filter { provider ->
            if (provider.name in settings.disabledProviders) return@filter false
            if (!provider.isApplicable(indicator)) return@filter false
            if (provider.isZeroKey) return@filter true
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
                summaryDescription = "No threat intelligence engines applicable or enabled for this indicator type.",
                sourceResults = emptyList(),
                timestampEpochMs = currentTimeMillis()
            )
        }

        val totalCount = applicable.size
        val counterMutex = Mutex()
        var finishedCount = 0
        onProgress?.invoke(0, totalCount)

        val semaphore = Semaphore(permits = 6)

        val results: List<SourceResult> = supervisorScope {
            applicable.map { provider ->
                val block: suspend kotlinx.coroutines.CoroutineScope.() -> SourceResult = {
                    val res = semaphore.withPermit {
                        val openUntil = circuitMutex.withLock { circuits[provider.name]?.openUntilMs ?: 0L }
                        val now = currentTimeMillis()
                        if (openUntil > now) {
                            SourceResult(
                                providerName = provider.name,
                                verdict = VerdictLevel.NO_DATA,
                                detectionsCount = 0,
                                totalEngineCount = 0,
                                summary = "Circuit breaker active: engine cooling down after repeated errors",
                                isError = true
                            )
                        } else {
                            try {
                                val timed = withTimeoutOrNull(effectiveTimeoutMs) {
                                    provider.query(client, indicator, settings)
                                }
                                val result = timed ?: SourceResult(
                                    providerName = provider.name,
                                    verdict = VerdictLevel.NO_DATA,
                                    detectionsCount = 0,
                                    totalEngineCount = 0,
                                    summary = "Connection timed out after ${effectiveTimeoutMs}ms (Provider skipped)",
                                    isError = true
                                )
                                updateCircuitBreaker(provider.name, failed = result.isError || result.isRateLimited)
                                result
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                updateCircuitBreaker(provider.name, failed = true)
                                val safeMsg = redactSecrets(e.message ?: "Skipped")
                                SourceResult(
                                    providerName = provider.name,
                                    verdict = VerdictLevel.NO_DATA,
                                    detectionsCount = 0,
                                    totalEngineCount = 0,
                                    summary = "Service unavailable or filtered: $safeMsg",
                                    isError = true
                                )
                            }
                        }
                    }

                    counterMutex.withLock {
                        val current = ++finishedCount
                        onProgress?.invoke(current, totalCount)
                    }

                    res
                }
                if (workDispatcher != null) async(workDispatcher, block = block) else async(block = block)
            }.awaitAll()
        }

        return synthesizeVerdict(indicator, results)
    }

    private suspend fun updateCircuitBreaker(providerName: String, failed: Boolean) {
        circuitMutex.withLock {
            if (!failed) {
                circuits.remove(providerName)
            } else {
                val prev = circuits[providerName] ?: CircuitState()
                val nextCount = prev.consecutiveFailures + 1
                val openUntil = if (nextCount >= 3) currentTimeMillis() + 30_000L else 0L
                circuits[providerName] = CircuitState(nextCount, openUntil)
            }
        }
    }

    private fun buildDedupKey(indicator: Indicator, settings: AppSettings): String {
        val disabled = settings.disabledProviders.sorted().joinToString(",")
        val keyState = listOf(
            settings.virusTotalApiKey.isNotBlank(),
            settings.abuseIpDbApiKey.isNotBlank(),
            settings.alienVaultApiKey.isNotBlank(),
            settings.pulsediveApiKey.isNotBlank(),
            settings.ipQualityScoreApiKey.isNotBlank()
        ).joinToString(",")
        return "${indicator.type.name}|${indicator.normalizedValue}|$disabled|$keyState"
    }

    private fun redactSecrets(input: String): String {
        return input
            .replace(Regex("""([?&]key=)[^&\s]+""", RegexOption.IGNORE_CASE), "$1[REDACTED]")
            .replace(Regex("""(/api/json/ip/)[^/\s]+/""", RegexOption.IGNORE_CASE), "$1[REDACTED]/")
    }

    private fun synthesizeVerdict(indicator: Indicator, results: List<SourceResult>): TriageVerdict {
        val maliciousSources = results.filter { it.verdict == VerdictLevel.MALICIOUS }
        val suspiciousSources = results.filter { it.verdict == VerdictLevel.SUSPICIOUS }
        val lowRiskSources = results.filter { it.verdict == VerdictLevel.LOW_RISK }
        val cleanSources = results.filter { it.verdict == VerdictLevel.CLEAN }
        val allErrored = results.isNotEmpty() && results.all { it.isError }

        val maliciousCount = maliciousSources.size
        val suspiciousCount = suspiciousSources.size
        val cleanCount = cleanSources.size

        val overallVerdict: VerdictLevel
        val score: Int
        val summary: String

        when {
            maliciousCount > 0 -> {
                overallVerdict = VerdictLevel.MALICIOUS
                val highFidelityCount = maliciousSources.count {
                    it.providerName.contains("abuse.ch", ignoreCase = true) ||
                        it.providerName.contains("Feodo", ignoreCase = true) ||
                        it.providerName.contains("ThreatFox", ignoreCase = true) ||
                        it.providerName.contains("MalwareBazaar", ignoreCase = true) ||
                        it.providerName.contains("VirusTotal", ignoreCase = true)
                }
                score = (75 + highFidelityCount * 10 + (maliciousCount - highFidelityCount) * 5 + suspiciousCount * 3).coerceAtMost(100)
                val names = maliciousSources.joinToString(", ") { it.providerName }
                summary = "MALICIOUS PAYLOAD DETECTED: Corroborated by $names."
            }
            suspiciousCount > 0 -> {
                overallVerdict = VerdictLevel.SUSPICIOUS
                score = (38 + suspiciousCount * 12 + lowRiskSources.size * 4).coerceAtMost(74)
                val names = suspiciousSources.joinToString(", ") { it.providerName }
                summary = "Suspicious Activity: Threat signals flagged by $names."
            }
            lowRiskSources.isNotEmpty() -> {
                overallVerdict = VerdictLevel.LOW_RISK
                score = 20
                summary = "Low Risk: Minor telemetry flags observed, but no active malware signature confirmed."
            }
            cleanCount > 0 -> {
                overallVerdict = VerdictLevel.CLEAN
                score = 5
                summary = "Verified Clean: $cleanCount threat intelligence engines observed no malicious activity."
            }
            allErrored -> {
                overallVerdict = VerdictLevel.NO_DATA
                score = 0
                summary = "Network or provider timeout: Could not reach active intelligence engines."
            }
            else -> {
                overallVerdict = VerdictLevel.NO_DATA
                score = 0
                summary = "No threat records found across active community intelligence engines."
            }
        }

        return TriageVerdict(
            indicator = indicator,
            overallVerdict = overallVerdict,
            confidenceScore = score,
            summaryDescription = summary,
            sourceResults = results,
            timestampEpochMs = currentTimeMillis(),
            hasNetworkError = allErrored
        )
    }
}
