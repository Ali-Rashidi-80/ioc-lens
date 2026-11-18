package io.github.mrcoder20.ioclens

import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.LocalizationProvider
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.parser.IOCParser
import io.github.mrcoder20.ioclens.domain.vision.QuishingAnalyzer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SharedLogicIOSTest {

    @Test
    fun testIosSharedParserAndQuishingContracts() {
        val indicator = IOCParser().parse("1.1.1.1").toIndicator()
        assertEquals(IndicatorType.IP_ADDRESS, indicator.type)
        assertTrue(QuishingAnalyzer.defang("https://phish.example/a").startsWith("hxxps://"))
        assertEquals("IOC Lens", LocalizationProvider.getStrings(AppLanguage.ENGLISH).appTitle)
    }
}