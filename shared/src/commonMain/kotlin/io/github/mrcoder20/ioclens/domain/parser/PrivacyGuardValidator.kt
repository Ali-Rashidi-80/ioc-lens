package io.github.mrcoder20.ioclens.domain.parser

import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.SourceResult
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel

object PrivacyGuardValidator {

    private val INTERNAL_DOMAINS = setOf(
        "localhost",
        "localhost.localdomain",
        "ip6-localhost",
        "ip6-loopback"
    )

    private val INTERNAL_TLDS = setOf(
        ".localhost",
        ".local",
        ".internal",
        ".lan",
        ".home",
        ".home.arpa",
        ".corp",
        ".intranet",
        ".private",
        ".localdomain",
        ".onion",
        ".invalid",
        ".test",
        ".in-addr.arpa",
        ".ip6.arpa"
    )

    private val IPV4_REGEX = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})(?::(\d{1,5}))?$""")
    private val OCTET_OR_HEX_IPV4_REGEX = Regex("""^(0x[0-9a-f]+|0[0-7]+|\d{1,3})\.(0x[0-9a-f]+|0[0-7]+|\d{1,3})\.(0x[0-9a-f]+|0[0-7]+|\d{1,3})\.(0x[0-9a-f]+|0[0-7]+|\d{1,3})(?::(\d{1,5}))?$""")
    private val HEX_DWORD_REGEX = Regex("""^0x([0-9a-f]{1,8})$""")
    private val DEC_DWORD_REGEX = Regex("""^(\d{8,10})$""")
    private val BRACKETED_IPV6_REGEX = Regex("""^\[([0-9a-fA-F:.%a-zA-Z0-9_-]+)\](?::(\d{1,5}))?$""")

    fun refang(input: String): String = IOCParser.refang(input)

    fun isPrivateOrInternal(
        indicator: Indicator,
        customRules: List<String> = emptyList()
    ): Boolean {
        if (indicator.isInternalOrPrivate) return true
        val target = indicator.normalizedValue.ifBlank { indicator.rawValue }
        return when (indicator.type) {
            IndicatorType.IP_ADDRESS -> isPrivateIp(target, customRules)
            IndicatorType.DOMAIN -> isInternalDomain(target, customRules)
            IndicatorType.URL -> isInternalUrl(target, customRules)
            IndicatorType.FILE_HASH, IndicatorType.UNKNOWN -> false
        }
    }

    fun isPrivateOrInternal(
        rawInput: String,
        customRules: List<String> = emptyList()
    ): Boolean {
        val trimmed = refang(rawInput).trim()
        if (trimmed.isEmpty()) return false

        val isUrlScheme = trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("ftp://", ignoreCase = true)
        if (isUrlScheme) {
            return isInternalUrl(trimmed, customRules)
        }

        if ((trimmed.contains('/') || trimmed.contains('\\')) && !trimmed.contains(' ')) {
            val host = extractHostFromUrl(trimmed)
            if (host.isNotEmpty() && isInternalHost(host, customRules)) return true
        }

        if (isPrivateIp(trimmed, customRules)) return true
        if (isInternalDomain(trimmed, customRules)) return true

        return false
    }

    fun isPrivateIp(
        ip: String,
        customRules: List<String> = emptyList()
    ): Boolean {
        val clean = refang(ip).trim().lowercase()
        if (clean.isEmpty()) return false

        // Scoped IPv6 (e.g., %eth0 or %12) represents an interface-local/link-local target
        val zoneRaw = clean.removeSurrounding("[", "]")
        if (zoneRaw.contains('%')) {
            val ipBeforeZone = zoneRaw.substringBefore('%')
            if (isIpv6(ipBeforeZone)) return true
        }

        // Check bracketed IPv6 with optional port: [::1] or [::1]:8080
        val bracketedMatch = BRACKETED_IPV6_REGEX.matchEntire(clean)
        if (bracketedMatch != null) {
            val ipPart = bracketedMatch.groupValues[1]
            val portStr = bracketedMatch.groupValues[2]
            val portValid = portStr.isEmpty() || ((portStr.toIntOrNull() ?: -1) in 1..65535)
            if (portValid && isIpv6(ipPart)) {
                return isPrivateIpv6(ipPart) || matchesCustomIpRules(ipPart.substringBefore('%'), customRules)
            }
        }

        // Standard decimal / octal / hex-dotted IPv4 (check BOTH decimal and POSIX octal interpretations)
        val ipv4Match = IPV4_REGEX.matchEntire(clean)
        if (ipv4Match != null) {
            val parts = listOf(
                ipv4Match.groupValues[1],
                ipv4Match.groupValues[2],
                ipv4Match.groupValues[3],
                ipv4Match.groupValues[4]
            )
            val portStr = ipv4Match.groupValues[5]
            val portValid = portStr.isEmpty() || ((portStr.toIntOrNull() ?: -1) in 1..65535)
            if (portValid) {
                val decimalOctets = parts.mapNotNull { it.toIntOrNull() }
                if (decimalOctets.size == 4 && decimalOctets.all { it in 0..255 }) {
                    if (isPrivateIpv4(decimalOctets) || matchesCustomIpv4Rules(decimalOctets, customRules)) {
                        return true
                    }
                }
                val octalOctets = parseOctalIpv4Octets(parts)
                if (octalOctets != null && (isPrivateIpv4(octalOctets) || matchesCustomIpv4Rules(octalOctets, customRules))) {
                    return true
                }
            }
        }

        // Mixed hex/octal/decimal dotted IPv4 (e.g. 0x7f.0.0.1, 0177.0.0.1)
        val mixedMatch = OCTET_OR_HEX_IPV4_REGEX.matchEntire(clean)
        if (mixedMatch != null) {
            val parts = listOf(
                mixedMatch.groupValues[1],
                mixedMatch.groupValues[2],
                mixedMatch.groupValues[3],
                mixedMatch.groupValues[4]
            )
            val portStr = mixedMatch.groupValues[5]
            val portValid = portStr.isEmpty() || ((portStr.toIntOrNull() ?: -1) in 1..65535)
            if (portValid) {
                val octets = parts.mapNotNull { parsePosixOctet(it) }
                if (octets.size == 4 && octets.all { it in 0..255 }) {
                    if (isPrivateIpv4(octets) || matchesCustomIpv4Rules(octets, customRules)) {
                        return true
                    }
                }
            }
        }

        // Single hex or decimal DWORD IPv4 (e.g. 0x7f000001, 2130706433)
        val dwordVal: Long? = HEX_DWORD_REGEX.matchEntire(clean)?.let {
            it.groupValues[1].toLongOrNull(16)
        } ?: DEC_DWORD_REGEX.matchEntire(clean)?.let {
            it.groupValues[1].toLongOrNull()?.takeIf { v -> v in 0..0xFFFFFFFFL }
        }
        if (dwordVal != null) {
            val octets = listOf(
                ((dwordVal ushr 24) and 0xFF).toInt(),
                ((dwordVal ushr 16) and 0xFF).toInt(),
                ((dwordVal ushr 8) and 0xFF).toInt(),
                (dwordVal and 0xFF).toInt()
            )
            if (isPrivateIpv4(octets) || matchesCustomIpv4Rules(octets, customRules)) {
                return true
            }
        }

        // Plain unbracketed IPv6
        val unbracketed = clean.removeSurrounding("[", "]").substringBefore('%')
        if (isIpv6(unbracketed)) {
            return isPrivateIpv6(unbracketed) || matchesCustomIpRules(unbracketed, customRules)
        }

        return false
    }

    private fun parsePosixOctet(token: String): Int? {
        return when {
            token.startsWith("0x") -> token.substring(2).toIntOrNull(16)
            token.length > 1 && token.startsWith('0') && token.all { it in '0'..'7' } -> token.toIntOrNull(8)
            else -> token.toIntOrNull(10)
        }
    }

    private fun parseOctalIpv4Octets(parts: List<String>): List<Int>? {
        val hasLeadingZeroOctal = parts.any { it.length > 1 && it.startsWith('0') && it.all { c -> c in '0'..'7' } }
        if (!hasLeadingZeroOctal) return null
        val octets = parts.map { part ->
            if (part.length > 1 && part.startsWith('0')) {
                if (!part.all { it in '0'..'7' }) return null
                part.toIntOrNull(8) ?: return null
            } else {
                part.toIntOrNull(10) ?: return null
            }
        }
        return if (octets.all { it in 0..255 }) octets else null
    }

    fun isPrivateIpv4(octets: List<Int>): Boolean {
        val (o1, o2, o3, _) = octets
        return when {
            o1 == 10 -> true                               // 10.0.0.0/8 (RFC 1918)
            o1 == 172 && o2 in 16..31 -> true              // 172.16.0.0/12 (RFC 1918)
            o1 == 192 && o2 == 168 -> true                 // 192.168.0.0/16 (RFC 1918)
            o1 == 127 -> true                              // 127.0.0.0/8 Loopback (RFC 1122)
            o1 == 169 && o2 == 254 -> true                 // 169.254.0.0/16 Link-Local (RFC 3927)
            o1 == 100 && o2 in 64..127 -> true             // 100.64.0.0/10 Carrier-grade NAT (RFC 6598)
            o1 == 0 -> true                                // 0.0.0.0/8 Current network (RFC 1122)
            o1 == 192 && o2 == 0 && o3 == 0 -> true        // 192.0.0.0/24 IETF Protocol Assignments (RFC 6890)
            o1 == 198 && o2 in 18..19 -> true              // 198.18.0.0/15 Benchmarking (RFC 2544)
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
        val raw = input.trim().lowercase().removeSurrounding("[", "]")
        if (raw.contains('%') && isIpv6(raw.substringBefore('%'))) {
            return true
        }
        val normalized = raw.substringBefore('%')
        if (normalized == "::1" || normalized == "::") return true

        // Handle dotted-decimal embedded IPv4 forms (IPv4-mapped, IPv4-compatible, NAT64 64:ff9b::/96 & 64:ff9b:1::/48)
        if (normalized.contains('.')) {
            val lastColon = normalized.lastIndexOf(':')
            if (lastColon != -1) {
                val v6Prefix = normalized.substring(0, lastColon)
                val ipv4Part = normalized.substring(lastColon + 1)
                val prefixHextets = expandPrefixToHextets(v6Prefix, expectedCount = 6)
                val match = IPV4_REGEX.matchEntire(ipv4Part)
                if (prefixHextets != null && match != null) {
                    val octets = match.destructured.toList().take(4).mapNotNull { it.toIntOrNull() }
                    if (octets.size == 4 && octets.all { it in 0..255 }) {
                        val isMappedOrCompat = prefixHextets.take(5).all { it == 0 } && (prefixHextets[5] == 0xffff || prefixHextets[5] == 0)
                        val isNat64WellKnown = prefixHextets[0] == 0x0064 && prefixHextets[1] == 0xff9b && prefixHextets.drop(2).all { it == 0 }
                        val isNat64Local = prefixHextets[0] == 0x0064 && prefixHextets[1] == 0xff9b && prefixHextets[2] == 0x0001
                        if (isMappedOrCompat || isNat64WellKnown || isNat64Local) {
                            return isPrivateIpv4(octets)
                        }
                    }
                }
            }
            return false
        }

        // Expand any valid compressed IPv6 (handles 0::1, ::0:1, ::0001, 0:0::1, 0::, ::0)
        val hextets = expandToEightHextets(normalized) ?: return false

        // Unspecified (::) and Loopback (::1)
        if (hextets.take(7).all { it == 0 } && (hextets[7] == 0 || hextets[7] == 1)) {
            return true
        }

        // IPv4-mapped (::ffff:0:0/96) and IPv4-compatible (::/96) in pure hex
        if (hextets.take(5).all { it == 0 } && (hextets[5] == 0xffff || hextets[5] == 0)) {
            val h6 = hextets[6]
            val h7 = hextets[7]
            val octets = listOf((h6 ushr 8) and 0xff, h6 and 0xff, (h7 ushr 8) and 0xff, h7 and 0xff)
            return isPrivateIpv4(octets)
        }

        // NAT64 (64:ff9b::/96 and 64:ff9b:1::/48)
        if (hextets[0] == 0x0064 && hextets[1] == 0xff9b) {
            val isWellKnown = hextets.subList(2, 6).all { it == 0 }
            val isLocalUse = hextets[2] == 0x0001
            if (isWellKnown || isLocalUse) {
                val h6 = hextets[6]
                val h7 = hextets[7]
                val octets = listOf((h6 ushr 8) and 0xff, h6 and 0xff, (h7 ushr 8) and 0xff, h7 and 0xff)
                return isPrivateIpv4(octets)
            }
        }

        // 6to4 (2002::/16) wrapping embedded IPv4 in hextets[1] and hextets[2]
        if (hextets[0] == 0x2002) {
            val h1 = hextets[1]
            val h2 = hextets[2]
            val octets = listOf((h1 ushr 8) and 0xff, h1 and 0xff, (h2 ushr 8) and 0xff, h2 and 0xff)
            if (isPrivateIpv4(octets)) return true
        }

        val firstBlock = hextets[0]
        if (firstBlock in 0xfe80..0xfebf) return true // Link-Local fe80::/10
        if (firstBlock in 0xfc00..0xfdff) return true // Unique-Local fc00::/7
        if (firstBlock in 0xfec0..0xfeff) return true // Deprecated Site-Local fec0::/10
        if ((firstBlock and 0xff00) == 0xff00) return true // Multicast ff00::/8

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

    fun isInternalDomain(
        domain: String,
        customRules: List<String> = emptyList()
    ): Boolean {
        val lower = refang(domain).trim().lowercase().removeSuffix(".")
        if (lower.isEmpty()) return false
        if (lower in INTERNAL_DOMAINS) return true
        if (INTERNAL_TLDS.any { ext -> lower.endsWith(ext) }) return true
        return matchesCustomDomainRules(lower, customRules)
    }

    fun isInternalUrl(
        url: String,
        customRules: List<String> = emptyList()
    ): Boolean {
        val host = extractHostFromUrl(url)
        if (host.isBlank()) return false
        // Single-label hostnames (e.g., http://jenkins:8080/ or http://intranet/) are internal by definition
        if (!host.contains('.') && !host.contains(':')) return true
        return isInternalHost(host, customRules)
    }

    fun isInternalHost(
        host: String,
        customRules: List<String> = emptyList()
    ): Boolean {
        val cleanHost = host.trim().lowercase().removeSuffix(".")
        if (cleanHost.isEmpty()) return false
        if (isInternalDomain(cleanHost, customRules)) return true
        if (isPrivateIp(cleanHost, customRules)) return true
        return false
    }

    fun extractHostFromUrl(url: String): String {
        val refanged = refang(url)
        val noScheme = if (refanged.contains("://")) refanged.substringAfter("://") else refanged
        val authority = noScheme
            .substringBefore('/')
            .substringBefore('\\')
            .substringBefore('?')
            .substringBefore('#')
        val hostPart = if (authority.contains('@')) authority.substringAfterLast('@') else authority
        return if (hostPart.startsWith('[')) {
            hostPart.substringAfter('[').substringBefore(']').lowercase()
        } else {
            hostPart.substringBefore(':').lowercase().removeSuffix(".")
        }
    }

    fun isValidCustomRule(rule: String): Boolean {
        val clean = rule.trim().lowercase()
        if (clean.isEmpty() || clean.length > 255 || clean.any { it.isWhitespace() }) return false
        if (clean.contains('/')) {
            val parts = clean.split('/')
            if (parts.size != 2) return false
            val prefix = parts[1].toIntOrNull() ?: return false
            val ip = parts[0]
            val v4 = IPV4_REGEX.matchEntire(ip)
            return if (v4 != null) {
                prefix in 0..32 && listOf(1, 2, 3, 4).all { (v4.groupValues[it].toIntOrNull() ?: -1) in 0..255 }
            } else {
                prefix in 0..128 && isIpv6(ip)
            }
        }
        if (IPV4_REGEX.matches(clean) || isIpv6(clean)) return true
        val domainLike = clean.removePrefix("*.").removePrefix(".")
        return domainLike.isNotEmpty() && Regex("""^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)*$""").matches(domainLike)
    }

    private fun matchesCustomDomainRules(domain: String, customRules: List<String>): Boolean {
        for (rawRule in customRules) {
            val rule = rawRule.trim().lowercase().removeSuffix(".")
            if (rule.isEmpty() || rule.contains('/')) continue
            val normalizedRule = rule.removePrefix("*.")
            when {
                normalizedRule.startsWith(".") -> {
                    val bare = normalizedRule.removePrefix(".")
                    if (domain == bare || domain.endsWith(normalizedRule)) return true
                }
                else -> {
                    if (domain == normalizedRule || domain.endsWith(".$normalizedRule")) return true
                }
            }
        }
        return false
    }

    private fun matchesCustomIpRules(ip: String, customRules: List<String>): Boolean {
        val clean = ip.trim().lowercase()
        for (rawRule in customRules) {
            val rule = rawRule.trim().lowercase()
            if (rule.isEmpty()) continue
            if (!rule.contains('/')) {
                if (clean == rule) return true
                continue
            }
            val cidrIp = rule.substringBefore('/')
            val prefix = rule.substringAfter('/').toIntOrNull() ?: continue
            val targetHextets = expandToEightHextets(clean) ?: continue
            val ruleHextets = expandToEightHextets(cidrIp) ?: continue
            if (prefix !in 0..128) continue
            if (cidrMatches(targetHextets, ruleHextets, prefix)) return true
        }
        return false
    }

    private fun matchesCustomIpv4Rules(octets: List<Int>, customRules: List<String>): Boolean {
        val targetInt = ((octets[0] and 0xFF) shl 24) or
            ((octets[1] and 0xFF) shl 16) or
            ((octets[2] and 0xFF) shl 8) or
            (octets[3] and 0xFF)
        for (rawRule in customRules) {
            val rule = rawRule.trim().lowercase()
            if (rule.isEmpty()) continue
            if (!rule.contains('/')) {
                val m = IPV4_REGEX.matchEntire(rule) ?: continue
                val ruleOctets = listOf(1, 2, 3, 4).mapNotNull { m.groupValues[it].toIntOrNull() }
                if (ruleOctets == octets) return true
            } else {
                val cidrIp = rule.substringBefore('/')
                val prefix = rule.substringAfter('/').toIntOrNull() ?: continue
                if (prefix !in 0..32) continue
                val m = IPV4_REGEX.matchEntire(cidrIp) ?: continue
                val rOct = listOf(1, 2, 3, 4).mapNotNull { m.groupValues[it].toIntOrNull() }
                if (rOct.size != 4 || !rOct.all { it in 0..255 }) continue
                val ruleInt = ((rOct[0] and 0xFF) shl 24) or
                    ((rOct[1] and 0xFF) shl 16) or
                    ((rOct[2] and 0xFF) shl 8) or
                    (rOct[3] and 0xFF)
                val mask = if (prefix == 0) 0 else -1 shl (32 - prefix)
                if ((targetInt and mask) == (ruleInt and mask)) return true
            }
        }
        return false
    }

    private fun cidrMatches(a: List<Int>, b: List<Int>, prefixBits: Int): Boolean {
        var remaining = prefixBits
        for (i in 0 until 8) {
            if (remaining <= 0) return true
            val bits = minOf(remaining, 16)
            val mask = ((0xFFFF shl (16 - bits)) and 0xFFFF)
            if ((a[i] and mask) != (b[i] and mask)) return false
            remaining -= 16
        }
        return true
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
