package io.github.mrcoder20.ioclens.domain.repository

import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict

interface ThreatIntelRepository {
    suspend fun analyzeIndicator(indicator: Indicator): TriageVerdict
    suspend fun analyzeIndicator(
        indicator: Indicator,
        onProgress: ((finishedCount: Int, totalCount: Int) -> Unit)?
    ): TriageVerdict = analyzeIndicator(indicator)
}
