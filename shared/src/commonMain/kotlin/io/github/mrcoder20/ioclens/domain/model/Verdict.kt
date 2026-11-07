package io.github.mrcoder20.ioclens.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class VerdictLevel {
    CLEAN,
    LOW_RISK,
    SUSPICIOUS,
    MALICIOUS,
    NO_DATA
}

@Serializable
data class SourceResult(
    val providerName: String,
    val verdict: VerdictLevel,
    val detectionsCount: Int,
    val totalEngineCount: Int,
    val summary: String,
    val isRateLimited: Boolean = false
)

@Serializable
data class TriageVerdict(
    val indicator: Indicator,
    val overallVerdict: VerdictLevel,
    val confidenceScore: Int, // 0 to 100
    val summaryDescription: String,
    val sourceResults: List<SourceResult>
)
