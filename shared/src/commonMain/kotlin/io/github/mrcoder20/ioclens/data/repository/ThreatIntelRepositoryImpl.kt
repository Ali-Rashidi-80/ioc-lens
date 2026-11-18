package io.github.mrcoder20.ioclens.data.repository

import io.github.mrcoder20.ioclens.data.remote.ThreatIntelAggregator
import io.github.mrcoder20.ioclens.data.remote.createZeroKeyHttpClient
import io.github.mrcoder20.ioclens.domain.model.AppSettings
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.domain.repository.SettingsRepository
import io.github.mrcoder20.ioclens.domain.repository.ThreatIntelRepository
import io.ktor.client.HttpClient

import kotlinx.coroutines.Dispatchers

class ThreatIntelRepositoryImpl(
    private val settingsRepository: SettingsRepository? = null,
    private val aggregator: ThreatIntelAggregator? = null
) : ThreatIntelRepository {

    private var createdClient: HttpClient? = null

    private val resolvedAggregator: ThreatIntelAggregator by lazy {
        aggregator ?: run {
            val httpClient = createZeroKeyHttpClient()
            createdClient = httpClient
            ThreatIntelAggregator(httpClient, workDispatcher = Dispatchers.Default)
        }
    }

    override suspend fun analyzeIndicator(indicator: Indicator): TriageVerdict {
        return analyzeIndicator(indicator, null)
    }

    override suspend fun analyzeIndicator(
        indicator: Indicator,
        onProgress: ((finishedCount: Int, totalCount: Int) -> Unit)?
    ): TriageVerdict {
        val settings = settingsRepository?.getSettings() ?: AppSettings()
        return resolvedAggregator.aggregate(indicator, settings, onProgress)
    }

    fun close() {
        createdClient?.close()
    }
}
