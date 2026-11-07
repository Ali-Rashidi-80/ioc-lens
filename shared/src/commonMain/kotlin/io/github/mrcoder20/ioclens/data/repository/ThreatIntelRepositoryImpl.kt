package io.github.mrcoder20.ioclens.data.repository

import io.github.mrcoder20.ioclens.data.remote.ThreatIntelAggregator
import io.github.mrcoder20.ioclens.domain.model.AppSettings
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.domain.repository.SettingsRepository
import io.github.mrcoder20.ioclens.domain.repository.ThreatIntelRepository
import io.ktor.client.HttpClient

class ThreatIntelRepositoryImpl(
    private val settingsRepository: SettingsRepository? = null,
    private val aggregator: ThreatIntelAggregator = ThreatIntelAggregator(HttpClient())
) : ThreatIntelRepository {

    override suspend fun analyzeIndicator(indicator: Indicator): TriageVerdict {
        val settings = settingsRepository?.getSettings() ?: AppSettings()
        return aggregator.aggregate(indicator, settings)
    }
}
