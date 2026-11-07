package io.github.mrcoder20.ioclens.domain.parser

import io.github.mrcoder20.ioclens.domain.model.HashAlgorithm
import io.github.mrcoder20.ioclens.domain.model.ParsedIoc

class IOCParser {

    companion object {
        private const val MAX_NON_URL_LENGTH = 512
        private const val MAX_URL_LENGTH = 8192
        private const val MAX_DOMAIN_LENGTH = 253

        private val INVISIBLE_CHARS_REGEX = Regex("[\u200B\u200C\u200D\uFEFF\u200E\u200F\u00A0]")

        private val NON_DOMAIN_EXTENSIONS = setOf(
            "apk", "exe", "bin", "pdf", "zip", "rar", "7z", "tar", "gz", "msi",
            "dll", "dmg", "iso", "img", "deb", "rpm", "jar", "png", "jpg", "jpeg",
            "gif", "webp", "docx", "xlsx", "pptx", "txt", "csv", "sys", "bat", "cmd",
            "ps1", "sh", "vbs", "elf", "so", "dylib", "ipa"
        )
    }

    fun refang(input: String): String {
        return input.trim()
            .replace(INVISIBLE_CHARS_REGEX, "")
            .replace(Regex("""\[\.\]|\(\.\)"""), ".")
            .replace(Regex("""\[://\]|\(://\)"""), "://")
            .replace(Regex("""\[:\]|\(:\)"""), ":")
            .replace(Regex("""\[@\]|\[at\]""", RegexOption.IGNORE_CASE), "@")
            .replace(Regex("""^hx[xt]ps://""", RegexOption.IGNORE_CASE), "https://")
            .replace(Regex("""^hx[xt]p://""", RegexOption.IGNORE_CASE), "http://")
            .replace(Regex("""^fxp://""", RegexOption.IGNORE_CASE), "ftp://")
    }

    fun parse(rawInput: String): ParsedIoc {
        val trimmed = rawInput.trim().replace(INVISIBLE_CHARS_REGEX, "")
        if (trimmed.isEmpty()) {
            return ParsedIoc.Unknown(rawInput, "")
        }

        val refanged = refang(trimmed)
        val normalized = refanged.lowercase()

        // 1. File Hash Check (exact lengths: MD5=32, SHA1=40, SHA256=64 hex)
        if (normalized.length in listOf(32, 40, 64) && normalized.all { it in '0'..'9' || it in 'a'..'f' }) {
            when (normalized.length) {
                32 -> return ParsedIoc.Hash(trimmed, normalized, HashAlgorithm.MD5)
                40 -> return ParsedIoc.Hash(trimmed, normalized, HashAlgorithm.SHA1)
                64 -> return ParsedIoc.Hash(trimmed, normalized, HashAlgorithm.SHA256)
            }
        }

        // 2. IPv4 Check (including optional port, e.g. 1.1.1.1:8080)
        val ipv4Regex = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})(?::(\d{1,5}))?$""")
        val ipv4Match = ipv4Regex.matchEntire(refanged)
        if (ipv4Match != null) {
            val (s1, s2, s3, s4, portStr) = ipv4Match.destructured
            val octets = listOf(s1.toIntOrNull() ?: -1, s2.toIntOrNull() ?: -1, s3.toIntOrNull() ?: -1, s4.toIntOrNull() ?: -1)
            val portValid = portStr.isEmpty() || ((portStr.toIntOrNull() ?: -1) in 1..65535)
            if (octets.all { it in 0..255 } && portValid) {
                val ipOnly = "$s1.$s2.$s3.$s4"
                val isPrivate = isPrivateIpv4(octets)
                return ParsedIoc.IpAddress(trimmed, ipOnly, isIpv6 = false, isPrivate = isPrivate)
            }
        }

        // 3. IPv6 Check (with optional wrapping brackets, e.g. [::1])
        val unbracketedIpv6 = refanged.removeSurrounding("[", "]")
        if (isIpv6(unbracketedIpv6)) {
            val isPrivate = isPrivateIpv6(unbracketedIpv6)
            return ParsedIoc.IpAddress(trimmed, unbracketedIpv6.lowercase(), isIpv6 = true, isPrivate = isPrivate)
        }

        // 4. URL Check
        if (refanged.startsWith("http://", ignoreCase = true) || refanged.startsWith("https://", ignoreCase = true)) {
            if (refanged.length <= MAX_URL_LENGTH) {
                val host = extractHostFromUrl(refanged)
                if (host.isNotBlank() && isValidHost(host)) {
                    val isInternal = isInternalHost(host)
                    return ParsedIoc.Url(trimmed, refanged, host = host, isInternal = isInternal)
                }
            }
            return ParsedIoc.Unknown(trimmed, refanged)
        }

        // Input length guard for non-URL tokens
        if (refanged.length > MAX_NON_URL_LENGTH) {
            return ParsedIoc.Unknown(trimmed, refanged)
        }

        // 5. Domain Check
        // A valid domain: RFC 1035 max 253 chars, labels separated by dots, valid TLD (2+ alpha)
        // Must NOT match a known file extension (e.g. malware.apk)
        if (refanged.length <= MAX_DOMAIN_LENGTH && !refanged.contains('/') && !refanged.contains('\\') && !refanged.contains(' ') && !refanged.contains('<') && !refanged.contains('>')) {
            val domainRegex = Regex("""^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+([a-zA-Z]{2,})$""")
            val domainMatch = domainRegex.matchEntire(refanged)
            if (domainMatch != null) {
                val tld = domainMatch.groupValues[1].lowercase()
                if (tld !in NON_DOMAIN_EXTENSIONS) {
                    val isInternal = isInternalDomain(refanged)
                    return ParsedIoc.Domain(trimmed, normalized, isInternal = isInternal)
                }
            }
        }

        // 6. Unknown / Plain Text / Internal Hostname (e.g. localhost)
        if (isInternalDomain(refanged)) {
            return ParsedIoc.Domain(trimmed, normalized, isInternal = true)
        }

        return ParsedIoc.Unknown(trimmed, refanged)
    }

    private fun isPrivateIpv4(octets: List<Int>): Boolean {
        val (o1, o2, o3, o4) = octets
        return when {
            o1 == 10 -> true // 10.0.0.0/8
            o1 == 172 && o2 in 16..31 -> true // 172.16.0.0/12
            o1 == 192 && o2 == 168 -> true // 192.168.0.0/16
            o1 == 127 -> true // 127.0.0.0/8 Loopback
            o1 == 169 && o2 == 254 -> true // 169.254.0.0/16 Link-Local
            o1 == 100 && o2 in 64..127 -> true // 100.64.0.0/10 Carrier-grade NAT
            o1 == 0 -> true // 0.0.0.0/8
            o1 in 224..239 -> true // Multicast
            o1 >= 240 -> true // Reserved
            else -> false
        }
    }

    private fun isIpv6(input: String): Boolean {
        val trimmed = input.trim().lowercase()
        if (!trimmed.contains(":")) return false
        if (trimmed == "::1" || trimmed == "::") return true

        // Cannot have multiple "::"
        val doubleColonCount = trimmed.windowed(2).count { it == "::" }
        if (doubleColonCount > 1) return false

        // Check invalid leading/trailing single colon
        if (trimmed.startsWith(":") && !trimmed.startsWith("::")) return false
        if (trimmed.endsWith(":") && !trimmed.endsWith("::")) return false

        val parts = trimmed.split(":")
        val hexPattern = Regex("^[0-9a-f]{1,4}$")

        if (doubleColonCount == 1) {
            // Compressed notation: 3 to 8 parts
            val nonEmptyParts = parts.filter { it.isNotEmpty() }
            return nonEmptyParts.size <= 7 && nonEmptyParts.all { it.matches(hexPattern) }
        } else {
            // Full notation: must have exactly 8 groups
            return parts.size == 8 && parts.all { it.matches(hexPattern) }
        }
    }

    private fun isPrivateIpv6(input: String): Boolean {
        val normalized = input.trim().lowercase()
        return when {
            normalized == "::1" || normalized == "::" -> true
            normalized.startsWith("fe80:") || normalized.startsWith("fe8") || normalized.startsWith("fe9") || normalized.startsWith("fea") || normalized.startsWith("feb") -> true // Link-Local
            normalized.startsWith("fc00:") || normalized.startsWith("fc") || normalized.startsWith("fd") -> true // Unique Local Address
            else -> false
        }
    }

    private fun extractHostFromUrl(url: String): String {
        val noScheme = url.substringAfter("://")
        val authority = noScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        val hostPart = authority.substringAfter('@')
        return hostPart.substringBefore(':').lowercase().removeSurrounding("[", "]")
    }

    private fun isValidHost(host: String): Boolean {
        if (host.isBlank()) return false
        if (host.contains(' ') || host.contains('<') || host.contains('>') || host.contains('"') || host.contains('\'') || host.contains('\\') || host.contains('/')) {
            return false
        }
        // Valid host is either IPv4, IPv6, valid domain name, or internal domain
        val ipv4Regex = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""")
        if (ipv4Regex.matches(host)) {
            val octets = host.split('.').mapNotNull { it.toIntOrNull() }
            return octets.size == 4 && octets.all { it in 0..255 }
        }
        if (isIpv6(host)) return true
        if (isInternalDomain(host)) return true
        val domainRegex = Regex("""^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+([a-zA-Z]{2,})$""")
        return domainRegex.matches(host)
    }

    private fun isInternalHost(host: String): Boolean {
        val cleanHost = host.lowercase()
        if (isInternalDomain(cleanHost)) return true
        val ipv4Match = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""").matchEntire(cleanHost)
        if (ipv4Match != null) {
            val octets = ipv4Match.destructured.toList().mapNotNull { it.toIntOrNull() }
            if (octets.size == 4 && octets.all { it in 0..255 }) {
                return isPrivateIpv4(octets)
            }
        }
        if (isIpv6(cleanHost)) {
            return isPrivateIpv6(cleanHost)
        }
        return false
    }

    private fun isInternalDomain(domain: String): Boolean {
        val lower = domain.lowercase()
        return lower == "localhost" ||
                lower.endsWith(".local") ||
                lower.endsWith(".internal") ||
                lower.endsWith(".lan") ||
                lower.endsWith(".home") ||
                lower.endsWith(".corp")
    }
}
