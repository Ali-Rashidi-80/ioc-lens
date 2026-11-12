package io.github.mrcoder20.ioclens.domain.parser

import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.SourceResult
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel

object PrivacyGuardValidator {

    private val INTERNAL_DOMAINS = setOf(
        "localhost"
    )

    private val INTERNAL_TLDS = setOf(
        ".localhost",
        ".local",
        ".internal",
        ".lan",
        ".home",
        ".corp"
    )

    private val IPV4_REGEX = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})(?::(\d{1,5}))?$""")
    private val BRACKETED_IPV6_REGEX = Regex("""^\[([0-9a-fA-F:.%a-zA-Z0-9_-]+)\](?::(\d{1,5}))?$""")
    private val INVISIBLE_CHARS_REGEX = Regex("[\u200B\u200C\u200D\uFEFF\u200E\u200F\u00A0]")

    fun refang(input: String): String = IOCParser.refang(input)

    fun isPrivateOrInternal(indicator: Indicator): Boolean {
        if (indicator.isInternalOrPrivate) return true
        val target = indicator.normalizedValue.ifBlank { indicator.rawValue }
        return when (indicator.type) {
            IndicatorType.IP_ADDRESS -> isPrivateIp(target)
            IndicatorType.DOMAIN -> isInternalDomain(target)
            IndicatorType.URL -> isInternalUrl(target)
            IndicatorType.FILE_HASH, IndicatorType.UNKNOWN -> false
        }
    }

    fun isPrivateOrInternal(rawInput: String): Boolean {
        val trimmed = refang(rawInput).trim()
        if (trimmed.isEmpty()) return false

        // 1. URL check (both explicit scheme and scheme-less like 192.168.1.1/admin, [::1]:8080/path)
        val isUrlScheme = trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("ftp://", ignoreCase = true)
        if (isUrlScheme) {
            return isInternalUrl(trimmed)
        }

        if (trimmed.contains('/') && !trimmed.contains(' ')) {
            val host = extractHostFromUrl(trimmed)
            if (host.isNotEmpty() && isInternalHost(host)) return true
        }

        // 2. IP check (IPv4 with optional port or IPv6)
        if (isPrivateIp(trimmed)) return true

        // 3. Domain / hostname check
        if (isInternalDomain(trimmed)) return true

        return false
    }

    fun isPrivateIp(ip: String): Boolean {
        val clean = refang(ip).trim().lowercase()
        if (clean.isEmpty()) return false

        // Check for bracketed IPv6 with optional port: [::1] or [::1]:8080
        val bracketedMatch = BRACKETED_IPV6_REGEX.matchEntire(clean)
        if (bracketedMatch != null) {
            val ipPart = bracketedMatch.groupValues[1]
            val portStr = bracketedMatch.groupValues[2]
            val portValid = portStr.isEmpty() || ((portStr.toIntOrNull() ?: -1) in 1..65535)
            if (portValid && isIpv6(ipPart)) {
                return isPrivateIpv6(ipPart)
            }
        }

        // Check IPv4 with optional port: 192.168.1.1 or 192.168.1.1:8080
        val ipv4Match = IPV4_REGEX.matchEntire(clean)
        if (ipv4Match != null) {
            val (s1, s2, s3, s4, portStr) = ipv4Match.destructured
            val octets = listOf(
                s1.toIntOrNull() ?: -1,
                s2.toIntOrNull() ?: -1,
                s3.toIntOrNull() ?: -1,
                s4.toIntOrNull() ?: -1
            )
            val portValid = portStr.isEmpty() || ((portStr.toIntOrNull() ?: -1) in 1..65535)
            if (octets.all { it in 0..255 } && portValid) {
                return isPrivateIpv4(octets)
            }
        }

        // Plain unbracketed IPv6
        val unbracketed = clean.removeSurrounding("[", "]").substringBefore('%')
        if (isIpv6(unbracketed)) {
            return isPrivateIpv6(unbracketed)
        }

        return false
    }

    fun isPrivateIpv4(octets: List<Int>): Boolean {
        val (o1, o2, o3, o4) = octets
        return when {
            o1 == 10 -> true                               // 10.0.0.0/8 (RFC 1918)
            o1 == 172 && o2 in 16..31 -> true              // 172.16.0.0/12 (RFC 1918)
            o1 == 192 && o2 == 168 -> true                 // 192.168.0.0/16 (RFC 1918)
            o1 == 127 -> true                              // 127.0.0.0/8 Loopback (RFC 1122)
            o1 == 169 && o2 == 254 -> true                 // 169.254.0.0/16 Link-Local (RFC 3927)
            o1 == 100 && o2 in 64..127 -> true             // 100.64.0.0/10 Carrier-grade NAT (RFC 6598)
            o1 == 0 -> true                                // 0.0.0.0/8 Current network (RFC 1122)
            o1 in 224..239 -> true                         // 224.0.0.0/4 Multicast (RFC 5771)
            o1 >= 240 -> true                              // 240.0.0.0/4 Reserved (RFC 1112)
            else -> false
        }
    }

    fun isIpv6(input: String): Boolean {
        val trimmed = input.trim().lowercase().removeSurrounding("[", "]").substringBefore('%')
        if (!trimmed.contains(":")) return false
        if (trimmed == "::1" || trimmed == "::") return true

        val doubleColonCount = trimmed.windowed(2).count { it == "::" }
        if (doubleColonCount > 1) return false
        if (trimmed.startsWith(":") && !trimmed.startsWith("::")) return false
        if (trimmed.endsWith(":") && !trimmed.endsWith("::")) return false

        // Check for embedded IPv4 (e.g. ::ffff:192.0.2.1)
        if (trimmed.contains('.')) {
            val lastColon = trimmed.lastIndexOf(':')
            val ipPart = trimmed.substring(lastColon + 1)
            val v6Prefix = trimmed.substring(0, lastColon)

            val v4Match = IPV4_REGEX.matchEntire(ipPart) ?: return false
            val octets = v4Match.destructured.toList().take(4).mapNotNull { it.toIntOrNull() }
            if (octets.size != 4 || !octets.all { it in 0..255 }) return false

            val prefixParts = v6Prefix.split(":")
            val hexPattern = Regex("^[0-9a-f]{1,4}$")
            val prefixDoubleColon = v6Prefix.windowed(2).count { it == "::" }
            if (prefixDoubleColon > 1) return false

            return if (prefixDoubleColon == 1) {
                val nonEmpty = prefixParts.filter { it.isNotEmpty() }
                nonEmpty.size <= 5 && nonEmpty.all { it.matches(hexPattern) }
            } else {
                prefixParts.size == 6 && prefixParts.all { it.matches(hexPattern) }
            }
        }

        val parts = trimmed.split(":")
        val hexPattern = Regex("^[0-9a-f]{1,4}$")
        return if (doubleColonCount == 1) {
            val nonEmptyParts = parts.filter { it.isNotEmpty() }
            nonEmptyParts.size <= 7 && nonEmptyParts.all { it.matches(hexPattern) }
        } else {
            parts.size == 8 && parts.all { it.matches(hexPattern) }
        }
    }

    fun isPrivateIpv6(input: String): Boolean {
        val normalized = input.trim().lowercase().removeSurrounding("[", "]").substringBefore('%')
        if (normalized == "::1" || normalized == "::") return true

        // Full uncompressed loopback check: 0:0:0:0:0:0:0:1 or 0000:0000:0000:0000:0000:0000:0000:0001
        val parts = normalized.split(":")
        if (parts.size == 8) {
            val allHex = parts.mapNotNull { it.toIntOrNull(16) }
            if (allHex.size == 8) {
                val leadingSeven = allHex.take(7)
                if (leadingSeven.all { it == 0 }) {
                    if (allHex[7] == 0 || allHex[7] == 1) return true
                }
            }
        }

        // IPv4-mapped IPv6 check:
        // Format A: Dotted-decimal suffix, e.g. ::ffff:10.0.0.1 or 0:0:0:0:0:ffff:10.0.0.1
        if (normalized.contains('.')) {
            val lastColon = normalized.lastIndexOf(':')
            if (lastColon != -1) {
                val v6Prefix = normalized.substring(0, lastColon)
                val ipv4Part = normalized.substring(lastColon + 1)
                val prefixHextets = expandPrefixToHextets(v6Prefix, expectedCount = 6)
                if (prefixHextets != null && prefixHextets.take(5).all { it == 0 } && (prefixHextets[5] == 0xffff || prefixHextets[5] == 0)) {
                    val match = IPV4_REGEX.matchEntire(ipv4Part)
                    if (match != null) {
                        val octets = match.destructured.toList().take(4).mapNotNull { it.toIntOrNull() }
                        if (octets.size == 4 && octets.all { it in 0..255 }) {
                            return isPrivateIpv4(octets)
                        }
                    }
                }
            }
        } else {
            // Format B: Hexadecimal format, e.g. ::ffff:0a00:0001 or 0:0:0:0:0:ffff:0a00:0001
            val hextets = expandToEightHextets(normalized)
            if (hextets != null && hextets.take(5).all { it == 0 } && hextets[5] == 0xffff) {
                val h6 = hextets[6]
                val h7 = hextets[7]
                val octets = listOf(
                    (h6 ushr 8) and 0xff,
                    h6 and 0xff,
                    (h7 ushr 8) and 0xff,
                    h7 and 0xff
                )
                return isPrivateIpv4(octets)
            }
        }

        // Link-Local (fe80::/10 -> 0xfe80..0xfebf) and Unique-Local (fc00::/7 -> 0xfc00..0xfdff)
        val firstBlockStr = normalized.substringBefore(':')
        if (firstBlockStr.isNotEmpty()) {
            val firstBlockInt = firstBlockStr.toIntOrNull(16)
            if (firstBlockInt != null) {
                if (firstBlockInt in 0xfe80..0xfebf) return true
                if (firstBlockInt in 0xfc00..0xfdff) return true
            }
        }

        return false
    }

    private fun expandPrefixToHextets(prefix: String, expectedCount: Int): List<Int>? {
        val trimmed = prefix.trim()
        val doubleColonCount = trimmed.windowed(2).count { it == "::" }
        if (doubleColonCount > 1) return null

        return if (doubleColonCount == 1) {
            val left = trimmed.substringBefore("::")
            val right = trimmed.substringAfter("::")
            val leftParts = if (left.isEmpty()) emptyList() else left.split(":")
            val rightParts = if (right.isEmpty()) emptyList() else right.split(":")
            if (leftParts.size + rightParts.size > expectedCount) return null
            val leftHex = leftParts.map { it.toIntOrNull(16) ?: return null }
            val rightHex = rightParts.map { it.toIntOrNull(16) ?: return null }
            val zeros = List(expectedCount - leftHex.size - rightHex.size) { 0 }
            leftHex + zeros + rightHex
        } else {
            val parts = trimmed.split(":")
            if (parts.size != expectedCount) return null
            parts.map { it.toIntOrNull(16) ?: return null }
        }
    }

    private fun expandToEightHextets(input: String): List<Int>? {
        return expandPrefixToHextets(input, expectedCount = 8)
    }

    fun isInternalDomain(domain: String): Boolean {
        val lower = refang(domain).trim().lowercase().removeSuffix(".")
        if (lower.isEmpty()) return false
        if (lower in INTERNAL_DOMAINS) return true
        return INTERNAL_TLDS.any { ext -> lower.endsWith(ext) }
    }

    fun isInternalUrl(url: String): Boolean {
        val host = extractHostFromUrl(url)
        if (host.isBlank()) return false
        return isInternalHost(host)
    }

    fun isInternalHost(host: String): Boolean {
        val cleanHost = host.trim().lowercase()
        if (cleanHost.isEmpty()) return false
        if (isInternalDomain(cleanHost)) return true
        if (isPrivateIp(cleanHost)) return true
        return false
    }

    fun extractHostFromUrl(url: String): String {
        val refanged = refang(url)
        val noScheme = if (refanged.contains("://")) refanged.substringAfter("://") else refanged
        val authority = noScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        val hostPart = authority.substringAfter('@')
        return if (hostPart.startsWith('[')) {
            hostPart.substringAfter('[').substringBefore(']').lowercase()
        } else {
            hostPart.substringBefore(':').lowercase()
        }
    }

    fun createPrivacyGuardVerdict(indicator: Indicator): TriageVerdict {
        val safeIndicator = if (indicator.isInternalOrPrivate) indicator else indicator.copy(isInternalOrPrivate = true)
        return TriageVerdict(
            indicator = safeIndicator,
            overallVerdict = VerdictLevel.CLEAN,
            confidenceScore = 0,
            summaryDescription = "🛡️ Privacy Guard Activated: Private/Internal indicator detected. Remote lookups blocked to protect internal infrastructure.",
            sourceResults = listOf(
                SourceResult(
                    providerName = "Privacy Guard Local",
                    verdict = VerdictLevel.CLEAN,
                    detectionsCount = 0,
                    totalEngineCount = 1,
                    summary = "Internal indicator safely blocked locally",
                    isRateLimited = false
                )
            )
        )
    }
}
