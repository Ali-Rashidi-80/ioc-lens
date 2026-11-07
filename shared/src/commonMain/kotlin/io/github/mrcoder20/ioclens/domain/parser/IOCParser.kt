package io.github.mrcoder20.ioclens.domain.parser

import io.github.mrcoder20.ioclens.domain.model.HashAlgorithm
import io.github.mrcoder20.ioclens.domain.model.ParsedIoc

class IOCParser {

    companion object {
        private val NON_DOMAIN_EXTENSIONS = setOf(
            "apk", "exe", "bin", "pdf", "zip", "rar", "7z", "tar", "gz", "msi",
            "dll", "dmg", "iso", "img", "deb", "rpm", "jar", "png", "jpg", "jpeg",
            "gif", "webp", "docx", "xlsx", "pptx", "txt", "csv", "sys", "bat", "cmd",
            "ps1", "sh", "vbs", "elf", "so", "dylib", "ipa"
        )
    }

    fun refang(input: String): String {
        return input.trim()
            .replace(Regex("""\[\.\]|\(\.\)"""), ".")
            .replace(Regex("""\[://\]|\(://\)"""), "://")
            .replace(Regex("""\[:\]|\(:\)"""), ":")
            .replace(Regex("""\[@\]|\[at\]""", RegexOption.IGNORE_CASE), "@")
            .replace(Regex("""^hxxps://""", RegexOption.IGNORE_CASE), "https://")
            .replace(Regex("""^hxxp://""", RegexOption.IGNORE_CASE), "http://")
            .replace(Regex("""^fxp://""", RegexOption.IGNORE_CASE), "ftp://")
    }

    fun parse(rawInput: String): ParsedIoc {
        val trimmed = rawInput.trim()
        val refanged = refang(trimmed)
        val normalized = refanged.lowercase()

        // 1. File Hash Check
        // MD5 (32 hex)
        if (normalized.matches(Regex("^[a-f0-9]{32}$"))) {
            return ParsedIoc.Hash(trimmed, normalized, HashAlgorithm.MD5)
        }
        // SHA1 (40 hex)
        if (normalized.matches(Regex("^[a-f0-9]{40}$"))) {
            return ParsedIoc.Hash(trimmed, normalized, HashAlgorithm.SHA1)
        }
        // SHA256 (64 hex)
        if (normalized.matches(Regex("^[a-f0-9]{64}$"))) {
            return ParsedIoc.Hash(trimmed, normalized, HashAlgorithm.SHA256)
        }

        // 2. IPv4 Check (including optional port, e.g. 1.1.1.1:8080)
        val ipv4Regex = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})(?::(\d{1,5}))?$""")
        val ipv4Match = ipv4Regex.matchEntire(refanged)
        if (ipv4Match != null) {
            val (s1, s2, s3, s4, portStr) = ipv4Match.destructured
            val octets = listOf(s1.toInt(), s2.toInt(), s3.toInt(), s4.toInt())
            val portValid = portStr.isEmpty() || (portStr.toIntOrNull() in 1..65535)
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
            val host = extractHostFromUrl(refanged)
            val isInternal = isInternalHost(host)
            return ParsedIoc.Url(trimmed, refanged, host = host, isInternal = isInternal)
        }

        // 5. Domain Check
        // A valid domain: label characters separated by dots, with a valid TLD (2+ alpha)
        // Must NOT match a known file extension (e.g. malware.apk)
        val domainRegex = Regex("""^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+([a-zA-Z]{2,})$""")
        val domainMatch = domainRegex.matchEntire(refanged)
        if (domainMatch != null) {
            val tld = domainMatch.groupValues[1].lowercase()
            if (tld !in NON_DOMAIN_EXTENSIONS) {
                val isInternal = isInternalDomain(refanged)
                return ParsedIoc.Domain(trimmed, normalized, isInternal = isInternal)
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
        val parts = trimmed.split(":")
        if (parts.size in 3..8) {
            val hexPattern = Regex("^[0-9a-f]{0,4}$")
            return parts.all { it.matches(hexPattern) }
        }
        return false
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
        return hostPart.substringBefore(':').lowercase()
    }

    private fun isInternalHost(host: String): Boolean {
        val cleanHost = host.lowercase()
        if (isInternalDomain(cleanHost)) return true
        val ipv4Match = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""").matchEntire(cleanHost)
        if (ipv4Match != null) {
            val octets = ipv4Match.destructured.toList().map { it.toInt() }
            if (octets.all { it in 0..255 }) {
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
