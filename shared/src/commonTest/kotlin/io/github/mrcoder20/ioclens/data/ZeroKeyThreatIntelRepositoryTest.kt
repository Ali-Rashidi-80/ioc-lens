package io.github.mrcoder20.ioclens.data

import io.github.mrcoder20.ioclens.data.remote.ThreatIntelAggregator
import io.github.mrcoder20.ioclens.data.remote.ThreatIntelProvider
import io.github.mrcoder20.ioclens.data.repository.ZeroKeyThreatIntelRepositoryImpl
import io.github.mrcoder20.ioclens.domain.model.AppSettings
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.SourceResult
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZeroKeyThreatIntelRepositoryTest {

    @Test
    fun testRepositoryBlocksPrivateIpWithoutNetworkDispatch() = runTest {
        var networkDispatched = 0
        val mockEngine = MockEngine { request ->
            networkDispatched++
            error("Privacy Guard VIOLATION: Network call dispatched to ${request.url}")
        }
        val client = HttpClient(mockEngine)
        val repository = ZeroKeyThreatIntelRepositoryImpl(
            client = client,
            aggregator = ThreatIntelAggregator(client)
        )

        // RFC 1918 Private IP
        val privateIp = Indicator("192.168.1.1", "192.168.1.1", IndicatorType.IP_ADDRESS)
        val verdict1 = repository.analyze(privateIp)

        assertEquals(0, networkDispatched)
        assertEquals(VerdictLevel.CLEAN, verdict1.overallVerdict)
        assertEquals(0, verdict1.confidenceScore)
        assertTrue(verdict1.indicator.isInternalOrPrivate)
        assertEquals("Privacy Guard Local", verdict1.sourceResults.first().providerName)

        // Internal domain
        val localDomain = Indicator("localhost", "localhost", IndicatorType.DOMAIN)
        val verdict2 = repository.analyze(localDomain)

        assertEquals(0, networkDispatched)
        assertEquals(VerdictLevel.CLEAN, verdict2.overallVerdict)
        assertEquals(0, verdict2.confidenceScore)
        assertTrue(verdict2.indicator.isInternalOrPrivate)
    }

    @Test
    fun testRepositoryBlocksPrivateIndicatorEvenIfFlagIsFalse() = runTest {
        var networkDispatched = 0
        val mockEngine = MockEngine { request ->
            networkDispatched++
            error("Privacy Guard VIOLATION: Network call dispatched to ${request.url}")
        }
        val client = HttpClient(mockEngine)
        val repository = ZeroKeyThreatIntelRepositoryImpl(
            client = client,
            aggregator = ThreatIntelAggregator(client)
        )

        // Explicitly pass isInternalOrPrivate = false to test gatekeeper interception
        val sneakyIndicator = Indicator("10.0.0.1", "10.0.0.1", IndicatorType.IP_ADDRESS, isInternalOrPrivate = false)
        val verdict = repository.analyze(sneakyIndicator)

        assertEquals(0, networkDispatched)
        assertEquals(VerdictLevel.CLEAN, verdict.overallVerdict)
        assertEquals(0, verdict.confidenceScore)
        assertTrue(verdict.indicator.isInternalOrPrivate)
    }

    @Test
    fun testGetZeroKeyProvidersReturnsTenEngines() {
        val repository = ZeroKeyThreatIntelRepositoryImpl()
        val providers = repository.getZeroKeyProviders()

        assertEquals(10, providers.size)
        assertTrue(providers.all { it.isZeroKey })

        val names = providers.map { it.name }
        assertTrue(names.any { it.contains("URLhaus") })
        assertTrue(names.any { it.contains("MalwareBazaar") })
        assertTrue(names.any { it.contains("ThreatFox") })
        assertTrue(names.any { it.contains("RDAP") })
        assertTrue(names.any { it.contains("Cloudflare") })
        assertTrue(names.any { it.contains("Shodan") })
        assertTrue(names.any { it.contains("AlienVault") })
        assertTrue(names.any { it.contains("RIPE") })
        assertTrue(names.any { it.contains("crt.sh") })
        assertTrue(names.any { it.contains("Feodo Tracker") })
    }

    @Test
    fun testRepositoryQueriesPublicIndicatorWithZeroUserKeys() = runTest {
        val dummyProvider = object : ThreatIntelProvider {
            override val name = "Mock ZeroKey Engine"
            override val isZeroKey = true
            override fun isApplicable(indicator: Indicator): Boolean = true
            override suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult {
                // Assert zero user API keys were supplied
                assertEquals("", settings.virusTotalApiKey)
                assertEquals("", settings.abuseIpDbApiKey)
                assertEquals("", settings.alienVaultApiKey)
                return SourceResult(name, VerdictLevel.CLEAN, 0, 1, "Mock engine verified clean")
            }
        }

        val client = HttpClient(MockEngine { respond("OK") })
        val aggregator = ThreatIntelAggregator(
            client = client,
            providers = listOf(dummyProvider)
        )
        val repository = ZeroKeyThreatIntelRepositoryImpl(
            client = client,
            aggregator = aggregator
        )

        val progressUpdates = mutableListOf<Pair<Int, Int>>()
        val indicator = Indicator("8.8.8.8", "8.8.8.8", IndicatorType.IP_ADDRESS)
        val verdict = repository.analyze(indicator) { completed, total ->
            progressUpdates.add(completed to total)
        }

        assertEquals(VerdictLevel.CLEAN, verdict.overallVerdict)
        assertEquals(1, verdict.sourceResults.size)
        assertEquals("Mock ZeroKey Engine", verdict.sourceResults.first().providerName)
        assertTrue(progressUpdates.contains(0 to 1))
        assertTrue(progressUpdates.contains(1 to 1))
    }

    @Test
    fun testRepositoryProgressReportingForPrivacyGuard() = runTest {
        val repository = ZeroKeyThreatIntelRepositoryImpl()
        var reported = false
        val privateIp = Indicator("127.0.0.1", "127.0.0.1", IndicatorType.IP_ADDRESS)

        repository.analyze(privateIp) { completed, total ->
            assertEquals(1, completed)
            assertEquals(1, total)
            reported = true
        }

        assertTrue(reported)
    }

    @Test
    fun testRepositoryLegacyAnalyzeIndicatorDelegatesToAnalyze() = runTest {
        val repository = ZeroKeyThreatIntelRepositoryImpl()
        val privateIp = Indicator("192.168.0.1", "192.168.0.1", IndicatorType.IP_ADDRESS)

        val verdict1 = repository.analyzeIndicator(privateIp)
        assertEquals(VerdictLevel.CLEAN, verdict1.overallVerdict)

        var called = false
        val verdict2 = repository.analyzeIndicator(privateIp) { _, _ -> called = true }
        assertEquals(VerdictLevel.CLEAN, verdict2.overallVerdict)
        assertTrue(called)
    }
}
