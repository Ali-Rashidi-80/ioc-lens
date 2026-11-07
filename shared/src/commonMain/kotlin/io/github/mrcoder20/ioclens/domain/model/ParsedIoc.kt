package io.github.mrcoder20.ioclens.domain.model

enum class HashAlgorithm {
    MD5,
    SHA1,
    SHA256
}

sealed class ParsedIoc {
    abstract val rawValue: String
    abstract val normalizedValue: String

    data class IpAddress(
        override val rawValue: String,
        override val normalizedValue: String,
        val isIpv6: Boolean,
        val isPrivate: Boolean
    ) : ParsedIoc()

    data class Domain(
        override val rawValue: String,
        override val normalizedValue: String,
        val isInternal: Boolean
    ) : ParsedIoc()

    data class Url(
        override val rawValue: String,
        override val normalizedValue: String,
        val host: String,
        val isInternal: Boolean
    ) : ParsedIoc()

    data class Hash(
        override val rawValue: String,
        override val normalizedValue: String,
        val algorithm: HashAlgorithm
    ) : ParsedIoc()

    data class FilePayload(
        override val rawValue: String,
        override val normalizedValue: String,
        val path: String,
        val name: String,
        val size: Long,
        val sha256: String,
        val md5: String
    ) : ParsedIoc()

    data class Unknown(
        override val rawValue: String,
        override val normalizedValue: String
    ) : ParsedIoc()

    fun toIndicator(): Indicator {
        val type = when (this) {
            is IpAddress -> IndicatorType.IP_ADDRESS
            is Domain -> IndicatorType.DOMAIN
            is Url -> IndicatorType.URL
            is Hash, is FilePayload -> IndicatorType.FILE_HASH
            is Unknown -> IndicatorType.UNKNOWN
        }
        val isInternal = when (this) {
            is IpAddress -> this.isPrivate
            is Domain -> this.isInternal
            is Url -> this.isInternal
            else -> false
        }
        return Indicator(
            rawValue = rawValue,
            normalizedValue = normalizedValue,
            type = type,
            isInternalOrPrivate = isInternal
        )
    }
}
