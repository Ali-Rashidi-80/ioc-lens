package io.github.mrcoder20.ioclens.data.repository

import io.github.mrcoder20.ioclens.data.remote.DEFAULT_ZERO_KEY_PROVIDERS
import io.github.mrcoder20.ioclens.data.remote.ThreatIntelAggregator
import io.github.mrcoder20.ioclens.data.remote.ThreatIntelProvider
import io.github.mrcoder20.ioclens.data.remote.createZeroKeyHttpClient
import io.github.mrcoder20.ioclens.domain.model.AppSettings
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.domain.parser.PrivacyGuardValidator
import io.github.mrcoder20.ioclens.domain.repository.ZeroKeyThreatIntelRepository
import io.ktor.client.HttpClient

class ZeroKeyThreatIntelRepositoryImpl(
    client: HttpClient = createZeroKeyHttpClient(),
    private val aggregator: ThreatIntelAggregator = ThreatIntelAggregator(
        client = client,
        providers = DEFAULT_ZERO_KEY_PROVIDERS
    )
) : ZeroKeyThreatIntelRepository {

    override suspend fun analyze(
        indicator: Indicator,
        onProgress: ((completed: Int, total: Int) -> Unit)?
    ): TriageVerdict {
        // Step 1: Immediate zero-network privacy guard check
        if (indicator.isInternalOrPrivate || PrivacyGuardValidator.isPrivateOrInternal(indicator)) {
            onProgress?.invoke(1, 1)
            return PrivacyGuardValidator.createPrivacyGuardVerdict(indicator)
        }

        // Step 2: Zero-key query execution with empty AppSettings (0 API keys)
        return aggregator.aggregate(
            indicator = indicator,
            settings = AppSettings(),
            onProgress = onProgress
        )
    }

    override fun getZeroKeyProviders(): List<ThreatIntelProvider> {
        return aggregator.providers.filter { it.isZeroKey }
    }
}
