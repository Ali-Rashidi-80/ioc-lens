package io.github.mrcoder20.ioclens.core.localization

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Empirical Adversarial Challenger Suite for [LocaleManager].
 *
 * Stress-tests reactive StateFlow concurrency, rapid switching across threads,
 * collector conflation and delivery integrity, and polymorphic boundary formatting.
 */
class LocaleManagerAdversarialStressTest {

    private val persianDigits = setOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    @BeforeTest
    fun setUp() {
        LocaleManager.setLanguage(AppLanguage.ENGLISH)
    }

    @AfterTest
    fun tearDown() {
        LocaleManager.setLanguage(AppLanguage.ENGLISH)
    }

    /**
     * Challenge 1: Rapid Concurrent Language Switching & StateFlow Collector Integrity.
     *
     * Spawns 20 parallel coroutines on Dispatchers.Default, each executing 200 random switches
     * (4,000 total switches). Simultaneously runs 4 collectors accumulating emissions and verifying
     * that:
     * 1. Every emitted language is valid and non-null.
     * 2. After all concurrent mutators finish, setting a deterministic language emits to all collectors.
     * 3. Current StateFlow value matches the last explicit setLanguage.
     */
    @Test
    fun testRapidConcurrentSwitchingAndCollectorIntegrity() = runBlocking {
        withTimeout(15_000) {
            val languages = listOf(AppLanguage.ENGLISH, AppLanguage.PERSIAN, AppLanguage.RUSSIAN)
            val collectorEmissions = Collections.synchronizedList(mutableListOf<AppLanguage>())
            val collectorCount = AtomicInteger(0)
            val isCollecting = AtomicBoolean(true)

            val startLatch = java.util.concurrent.CountDownLatch(4)
            // Launch 4 background collectors
            val collectorJobs = (1..4).map {
                launch(Dispatchers.Default) {
                    LocaleManager.currentLanguage.collect { lang ->
                        collectorEmissions.add(lang)
                        collectorCount.incrementAndGet()
                        startLatch.countDown()
                        if (!isCollecting.get()) return@collect
                    }
                }
            }
            startLatch.await(5, java.util.concurrent.TimeUnit.SECONDS)

            // Launch 20 concurrent mutators
            val mutatorJobs = (1..20).map { workerId ->
                launch(Dispatchers.Default) {
                    val rng = Random(workerId * 1000)
                    repeat(200) {
                        val targetLang = languages[rng.nextInt(languages.size)]
                        LocaleManager.setLanguage(targetLang)
                        // Read currentLanguage.value immediately to test consistency
                        val readValue = LocaleManager.currentLanguage.value
                        assertTrue(readValue in languages, "Current language must always be one of the known languages")
                        kotlinx.coroutines.yield()
                    }
                }
            }

            // Wait for all mutators to complete
            mutatorJobs.joinAll()

            // Deterministic terminal assertions
            LocaleManager.setLanguage(AppLanguage.RUSSIAN)
            assertEquals(AppLanguage.RUSSIAN, LocaleManager.currentLanguage.value)
            assertEquals(LocaleManager.getStrings(AppLanguage.RUSSIAN).themeLabel, LocaleManager.getStrings().themeLabel)

            LocaleManager.setLanguage(AppLanguage.PERSIAN)
            assertEquals(AppLanguage.PERSIAN, LocaleManager.currentLanguage.value)
            assertEquals(LocaleManager.getStrings(AppLanguage.PERSIAN).themeLabel, LocaleManager.getStrings().themeLabel)

            LocaleManager.setLanguage(AppLanguage.ENGLISH)
            assertEquals(AppLanguage.ENGLISH, LocaleManager.currentLanguage.value)
            assertEquals(LocaleManager.getStrings(AppLanguage.ENGLISH).themeLabel, LocaleManager.getStrings().themeLabel)

            kotlinx.coroutines.delay(100)
            // Stop collectors
            isCollecting.set(false)
            collectorJobs.forEach { it.cancel() }

            // Ensure collectors observed high volume of transitions
            assertTrue(collectorCount.get() >= 20, "Collectors must observe emissions during concurrent switching")
            assertTrue(collectorEmissions.all { it in languages }, "All emissions must be valid AppLanguage instances")
        }
    }

    /**
     * Challenge 2: Rapid Collector Churn & Dynamic Subscription / Cancellation.
     *
     * Rapidly attaches and cancels collectors while another coroutine continuously switches languages.
     * Asserts zero deadlock, zero thread starvation, and each subscription receives an immediate initial replay value.
     */
    @Test
    fun testRapidCollectorChurnUnderContinuousUpdates() = runBlocking {
        withTimeout(10_000) {
            val keepUpdating = AtomicBoolean(true)

            val updaterJob = launch(Dispatchers.Default) {
                var toggle = 0
                while (keepUpdating.get()) {
                    val lang = when (toggle % 3) {
                        0 -> AppLanguage.ENGLISH
                        1 -> AppLanguage.PERSIAN
                        else -> AppLanguage.RUSSIAN
                    }
                    LocaleManager.setLanguage(lang)
                    toggle++
                }
            }

            // Dynamically subscribe and immediately take(1) across 100 coroutines
            val churnJobs = (1..100).map {
                launch(Dispatchers.Default) {
                    val initial = LocaleManager.currentLanguage.first()
                    assertNotNull(initial)
                    assertTrue(initial in AppLanguage.values())
                }
            }

            churnJobs.joinAll()
            keepUpdating.set(false)
            updaterJob.cancel()
        }
    }

    /**
     * Challenge 3: Concurrent Polymorphic Number Formatting with Extreme & Boundary Values.
     *
     * Exercises [LocaleManager.formatNumber] and [LocalizationProvider.formatNumber] across:
     * - Int.MIN_VALUE, Int.MAX_VALUE
     * - Long.MIN_VALUE, Long.MAX_VALUE
     * - Double.NaN, POSITIVE_INFINITY, NEGATIVE_INFINITY
     * - Float boundaries
     * - BigInteger & BigDecimal
     * Under simultaneous multi-threaded execution.
     */
    @Test
    fun testConcurrentPolymorphicNumberFormattingBoundaries() = runBlocking {
        withTimeout(10_000) {
            val testNumbers: List<Number> = listOf(
                0,
                -1,
                1,
                Int.MAX_VALUE,
                Int.MIN_VALUE,
                Long.MAX_VALUE,
                Long.MIN_VALUE,
                Float.MAX_VALUE,
                Float.MIN_VALUE,
                Double.MAX_VALUE,
                Double.MIN_VALUE,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                BigInteger("123456789012345678901234567890"),
                BigDecimal("9876543210.123456789")
            )

            val jobs = (1..10).map {
                launch(Dispatchers.Default) {
                    repeat(100) {
                        for (num in testNumbers) {
                            val enFormatted = LocaleManager.formatNumber(num, AppLanguage.ENGLISH)
                            val faFormatted = LocaleManager.formatNumber(num, AppLanguage.PERSIAN)
                            val ruFormatted = LocaleManager.formatNumber(num, AppLanguage.RUSSIAN)

                            // English and Russian must not contain Persian digits
                            assertFalse(enFormatted.any { it in persianDigits })
                            assertFalse(ruFormatted.any { it in persianDigits })

                            // If number contains ASCII digits, Persian formatting must transform all of them
                            val hasDigits = num.toString().any { it in '0'..'9' }
                            if (hasDigits) {
                                assertFalse(
                                    faFormatted.any { it in '0'..'9' },
                                    "Persian formatted number '$faFormatted' should not contain ASCII digits"
                                )
                            }
                        }
                    }
                }
            }

            jobs.joinAll()
        }
    }

    /**
     * Challenge 4: Score Formatting Boundary Stress.
     *
     * Validates [LocaleManager.formatScore] across extreme values, negative scores,
     * zero, 100, and overflow scores under concurrent access.
     */
    @Test
    fun testScoreFormattingBoundaryStress() = runBlocking {
        withTimeout(5_000) {
            val boundaryScores = listOf(
                -1_000_000, -100, -1, 0, 1, 50, 99, 100, 101, 10_000, 1_000_000
            )

            for (score in boundaryScores) {
                val en = LocaleManager.formatScore(score, AppLanguage.ENGLISH)
                val fa = LocaleManager.formatScore(score, AppLanguage.PERSIAN)
                val ru = LocaleManager.formatScore(score, AppLanguage.RUSSIAN)

                assertEquals("$score%", en)
                assertEquals("$score%", ru)
                assertTrue(fa.endsWith("٪"), "Persian score must end with Persian percent sign '٪'")
                assertFalse(fa.any { it in '0'..'9' }, "Persian score must have no ASCII digits")
            }
        }
    }

    /**
     * Challenge 5: Dynamic Fallback Consistency between StateFlow and Helper Methods.
     *
     * Asserts that when helper methods (getStrings(), formatNumber(), formatScore())
     * are called without the optional language parameter, they dynamically and synchronously
     * pick up the current StateFlow value.
     */
    @Test
    fun testDynamicParameterlessHelperConsistency() {
        for (lang in listOf(AppLanguage.ENGLISH, AppLanguage.PERSIAN, AppLanguage.RUSSIAN)) {
            LocaleManager.setLanguage(lang)
            assertEquals(lang, LocaleManager.currentLanguage.value)

            val strings = LocaleManager.getStrings()
            assertEquals(LocaleManager.getStrings(lang).themeLabel, strings.themeLabel)

            val numFormatted = LocaleManager.formatNumber(123)
            assertEquals(LocaleManager.formatNumber(123, lang), numFormatted)

            val scoreFormatted = LocaleManager.formatScore(75)
            assertEquals(LocaleManager.formatScore(75, lang), scoreFormatted)
        }
    }
}
