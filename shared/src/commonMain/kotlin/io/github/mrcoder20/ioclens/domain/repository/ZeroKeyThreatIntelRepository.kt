package io.github.mrcoder20.ioclens.domain.repository

import io.github.mrcoder20.ioclens.data.remote.ThreatIntelProvider
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict

interface ZeroKeyThreatIntelRepository : ThreatIntelRepository {
    suspend fun analyze(
        indicator: Indicator,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null
    ): TriageVerdict

    fun getZeroKeyProviders(): List<ThreatIntelProvider>

    override suspend fun analyzeIndicator(
        indicator: Indicator,
        onProgress: ((finishedCount: Int, totalCount: Int) -> Unit)?
    ): TriageVerdict = analyze(indicator, onProgress)

    override suspend fun analyzeIndicator(indicator: Indicator): TriageVerdict =
        analyze(indicator, null)
}
