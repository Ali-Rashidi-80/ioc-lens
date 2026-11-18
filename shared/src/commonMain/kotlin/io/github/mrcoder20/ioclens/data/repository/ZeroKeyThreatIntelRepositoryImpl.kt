package io.github.mrcoder20.ioclens.data.repository

import io.github.mrcoder20.ioclens.data.remote.DEFAULT_ZERO_KEY_PROVIDERS
import io.github.mrcoder20.ioclens.data.remote.ThreatIntelAggregator
import io.github.mrcoder20.ioclens.data.remote.ThreatIntelProvider
import io.github.mrcoder20.ioclens.data.remote.createDefaultZeroKeyProviders
import io.github.mrcoder20.ioclens.data.remote.createZeroKeyHttpClient
import io.github.mrcoder20.ioclens.domain.model.AppSettings
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.domain.parser.PrivacyGuardValidator
import io.github.mrcoder20.ioclens.domain.repository.ZeroKeyThreatIntelRepository
import io.ktor.client.HttpClient

class ZeroKeyThreatIntelRepositoryImpl(
    private val client: HttpClient? = null,
    private val aggregator: ThreatIntelAggregator? = null
) : ZeroKeyThreatIntelRepository {

    private var createdClient: HttpClient? = null

    private val resolvedAggregator: ThreatIntelAggregator by lazy {
        aggregator ?: run {
            val effectiveClient = client ?: createZeroKeyHttpClient().also { createdClient = it }
            ThreatIntelAggregator(
                client = effectiveClient,
                providers = createDefaultZeroKeyProviders()
            )
        }
    }

    override suspend fun analyze(
        indicator: Indicator,
        onProgress: ((completed: Int, total: Int) -> Unit)?
    ): TriageVerdict {
        if (indicator.isInternalOrPrivate || PrivacyGuardValidator.isPrivateOrInternal(indicator)) {
            onProgress?.invoke(1, 1)
            return PrivacyGuardValidator.createPrivacyGuardVerdict(indicator)
        }

        return resolvedAggregator.aggregate(
            indicator = indicator,
            settings = AppSettings(),
            onProgress = onProgress
        )
    }

    override fun getZeroKeyProviders(): List<ThreatIntelProvider> {
        val source = aggregator?.providers ?: DEFAULT_ZERO_KEY_PROVIDERS
        return source.filter { it.isZeroKey }
    }

    fun close() {
        createdClient?.close()
    }
}
