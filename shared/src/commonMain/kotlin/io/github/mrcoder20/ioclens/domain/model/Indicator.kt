package io.github.mrcoder20.ioclens.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class IndicatorType {
    IP_ADDRESS,
    DOMAIN,
    URL,
    FILE_HASH,
    UNKNOWN
}

@Serializable
data class Indicator(
    val rawValue: String,
    val normalizedValue: String,
    val type: IndicatorType,
    val isInternalOrPrivate: Boolean = false
)
