package io.github.mrcoder20.ioclens.domain.usecase

import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.ParsedIoc
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.domain.parser.IOCParser
import io.github.mrcoder20.ioclens.domain.repository.ThreatIntelRepository

class AnalyzeIndicatorUseCase(
    private val repository: ThreatIntelRepository,
    private val parser: IOCParser = IOCParser()
) {
    fun parseIndicator(rawInput: String): Indicator {
        val parsed = parser.parse(rawInput)
        return parsed.toIndicator()
    }

    fun parseIoc(rawInput: String): ParsedIoc {
        return parser.parse(rawInput)
    }

    suspend operator fun invoke(
        indicator: Indicator,
        onProgress: ((finishedCount: Int, totalCount: Int) -> Unit)? = null
    ): TriageVerdict {
        return repository.analyzeIndicator(indicator, onProgress)
    }
}
