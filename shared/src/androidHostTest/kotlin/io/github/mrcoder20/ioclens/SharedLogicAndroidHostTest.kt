package io.github.mrcoder20.ioclens

import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.parser.IOCParser
import io.github.mrcoder20.ioclens.domain.parser.PrivacyGuardValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SharedLogicAndroidHostTest {

    @Test
    fun testAndroidHostIndicatorAndPrivacyGuard() {
        val parser = IOCParser()
        val domain = parser.parse("portal.home.arpa").toIndicator()
        assertEquals(IndicatorType.DOMAIN, domain.type)
        assertTrue(PrivacyGuardValidator.isPrivateOrInternal(domain))
    }
}