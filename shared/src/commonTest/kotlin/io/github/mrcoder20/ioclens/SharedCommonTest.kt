package io.github.mrcoder20.ioclens

import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.LocalizationProvider
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.parser.IOCParser
import io.github.mrcoder20.ioclens.domain.parser.PrivacyGuardValidator
import io.github.mrcoder20.ioclens.domain.vision.QuishingAnalyzer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedCommonTest {

    @Test
    fun testCommonDomainAndLocalizationContracts() {
        val parser = IOCParser()
        val publicIp = parser.parse("192.0.2.10").toIndicator()
        assertEquals(IndicatorType.IP_ADDRESS, publicIp.type)
        assertFalse(PrivacyGuardValidator.isPrivateOrInternal(publicIp))

        val privateIp = parser.parse("10.20.30.40").toIndicator()
        assertTrue(PrivacyGuardValidator.isPrivateOrInternal(privateIp))

        assertEquals("hxxps://evil[.]example/login", QuishingAnalyzer.defang("https://evil.example/login"))
        for (lang in AppLanguage.entries) {
            assertTrue(LocalizationProvider.getStrings(lang).appTitle.isNotBlank())
        }
    }
}