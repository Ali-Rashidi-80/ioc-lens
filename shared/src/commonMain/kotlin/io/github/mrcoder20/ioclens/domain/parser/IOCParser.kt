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
        var cleaned = input.trim()
            .replace(INVISIBLE_CHARS_REGEX, "")

        // Remove surrounding wrappers: <url>, (url), [url], 'url', "url", `url`
        if (cleaned.startsWith("<") && cleaned.endsWith(">")) cleaned = cleaned.substring(1, cleaned.length - 1).trim()
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) cleaned = cleaned.substring(1, cleaned.length - 1).trim()
        if (cleaned.startsWith("'") && cleaned.endsWith("'")) cleaned = cleaned.substring(1, cleaned.length - 1).trim()
        if (cleaned.startsWith("`") && cleaned.endsWith("`")) cleaned = cleaned.substring(1, cleaned.length - 1).trim()

        return cleaned
            .replace("[.]", ".")
            .replace("(.)", ".")
            .replace("{.}", ".")
            .replace("[dot]", ".", ignoreCase = true)
            .replace("(dot)", ".", ignoreCase = true)
            .replace("{dot}", ".", ignoreCase = true)
            .replace("[://]", "://")
            .replace("(://)", "://")
            .replace("[:]//", "://")
            .replace("(:)//", "://")
            .replace("[:]", ":")
            .replace("(:)", ":")
            .replace("[@]", "@")
            .replace("[at]", "@", ignoreCase = true)
            .replace("(at)", "@", ignoreCase = true)
            .replace(Regex("""(?:^|\b)h(?:[xt]{2}|\[t\]\[t\]|\[tt\])ps://""", RegexOption.IGNORE_CASE), "https://")
            .replace(Regex("""(?:^|\b)h(?:[xt]{2}|\[t\]\[t\]|\[tt\])p://""", RegexOption.IGNORE_CASE), "http://")
            .replace(Regex("""(?:^|\b)fxp://""", RegexOption.IGNORE_CASE), "ftp://")
    }

    fun parse(rawInput: String): ParsedIoc {
        val exact = parseExact(rawInput)
        if (exact !is ParsedIoc.Unknown) return exact

        // If input contains spaces or surrounding sentence words (e.g. from Telegram, Eitaa, logs),
        // extract the first recognized threat indicator automatically (Google Lens style).
        if (rawInput.contains(Regex("""\s"""))) {
            val extracted = extractFirstIoc(rawInput)
            if (extracted != null) return extracted
        }

        return exact
    }

    fun parseExact(rawInput: String): ParsedIoc {
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

        // 3. IPv6 Check (with optional wrapping brackets, e.g. [::1], and optional port, e.g. [::1]:8080)
        val bracketedWithPortRegex = Regex("""^\[([0-9a-fA-F:.]+)\](?::(\d{1,5}))?$""")
        val bracketedMatch = bracketedWithPortRegex.matchEntire(refanged)
        if (bracketedMatch != null) {
            val (ipPart, portStr) = bracketedMatch.destructured
            val portValid = portStr.isEmpty() || ((portStr.toIntOrNull() ?: -1) in 1..65535)
            if (portValid && isIpv6(ipPart)) {
                val isPrivate = isPrivateIpv6(ipPart)
                return ParsedIoc.IpAddress(trimmed, ipPart.lowercase(), isIpv6 = true, isPrivate = isPrivate)
            }
        }

        val unbracketedIpv6 = refanged.removeSurrounding("[", "]")
        if (isIpv6(unbracketedIpv6)) {
            val isPrivate = isPrivateIpv6(unbracketedIpv6)
            return ParsedIoc.IpAddress(trimmed, unbracketedIpv6.lowercase(), isIpv6 = true, isPrivate = isPrivate)
        }

        // 4. URL Check
        val isUrlScheme = refanged.startsWith("http://", ignoreCase = true) ||
                refanged.startsWith("https://", ignoreCase = true) ||
                refanged.startsWith("ftp://", ignoreCase = true)
        if (isUrlScheme) {
            if (refanged.length <= MAX_URL_LENGTH) {
                val host = extractHostFromUrl(refanged)
                if (host.isNotBlank() && isValidHost(host)) {
                    val isInternal = isInternalHost(host)
                    return ParsedIoc.Url(trimmed, refanged, host = host, isInternal = isInternal)
                }
            }
            return ParsedIoc.Unknown(trimmed, refanged)
        }

        // 4b. Scheme-less URL Check (e.g. evil.com/login.php, 1.2.3.4:8080/malware)
        if (refanged.contains('/') && !refanged.contains(' ') && refanged.length <= MAX_URL_LENGTH) {
            val hostPart = refanged.substringBefore('/')
            val hostClean = if (hostPart.contains(':')) hostPart.substringBefore(':') else hostPart
            if (isValidHost(hostClean)) {
                val fullUrl = "http://$refanged"
                val isInternal = isInternalHost(hostClean)
                return ParsedIoc.Url(trimmed, fullUrl, host = hostClean, isInternal = isInternal)
            }
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

    /**
     * Extracts the first valid threat indicator from freeform, noisy, or sentence-wrapped text
     * (e.g. shared Telegram/Eitaa messages, security alerts, firewall logs).
     */
    fun extractFirstIoc(rawInput: String): ParsedIoc? {
        val exact = parseExact(rawInput)
        if (exact !is ParsedIoc.Unknown) return exact
        return extractAllIocs(rawInput).firstOrNull()
    }

    /**
     * Extracts all unique valid threat indicators from freeform or formatted multi-indicator text.
     */
    fun extractAllIocs(rawInput: String): List<ParsedIoc> {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return emptyList()

        val refanged = refang(trimmed)
        val results = mutableListOf<ParsedIoc>()
        val seenValues = mutableSetOf<String>()

        // 1. Scan for URLs
        val urlRegex = Regex("""(?:https?|ftp)://[^\s<>"'{}|\\^`]+""", RegexOption.IGNORE_CASE)
        urlRegex.findAll(refanged).forEach { match ->
            val parsed = parseExact(match.value)
            if (parsed !is ParsedIoc.Unknown && seenValues.add(parsed.normalizedValue)) {
                results.add(parsed)
            }
        }

        // 2. Scan for Hashes (SHA-256, SHA-1, MD5)
        val hashRegex = Regex("""\b([a-fA-F0-9]{64}|[a-fA-F0-9]{40}|[a-fA-F0-9]{32})\b""")
        hashRegex.findAll(refanged).forEach { match ->
            val parsed = parseExact(match.value)
            if (parsed !is ParsedIoc.Unknown && seenValues.add(parsed.normalizedValue)) {
                results.add(parsed)
            }
        }

        // 3. Scan for IPv4
        val ipv4Regex = Regex("""\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?::\d{1,5})?\b""")
        ipv4Regex.findAll(refanged).forEach { match ->
            val parsed = parseExact(match.value)
            if (parsed !is ParsedIoc.Unknown && seenValues.add(parsed.normalizedValue)) {
                results.add(parsed)
            }
        }

        // 4. Scan for Domains
        val domainRegex = Regex("""\b(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+([a-zA-Z]{2,})\b""")
        domainRegex.findAll(refanged).forEach { match ->
            val parsed = parseExact(match.value)
            if (parsed !is ParsedIoc.Unknown && seenValues.add(parsed.normalizedValue)) {
                results.add(parsed)
            }
        }

        return results
    }

    private fun isPrivateIpv4(octets: List<Int>): Boolean =
        PrivacyGuardValidator.isPrivateIpv4(octets)

    private fun isIpv6(input: String): Boolean =
        PrivacyGuardValidator.isIpv6(input)

    private fun isPrivateIpv6(input: String): Boolean =
        PrivacyGuardValidator.isPrivateIpv6(input)

    private fun extractHostFromUrl(url: String): String =
        PrivacyGuardValidator.extractHostFromUrl(url)

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
        val domainMatch = domainRegex.matchEntire(host) ?: return false
        val tld = domainMatch.groupValues[1].lowercase()
        return tld !in NON_DOMAIN_EXTENSIONS
    }

    private fun isInternalHost(host: String): Boolean =
        PrivacyGuardValidator.isInternalHost(host)

    private fun isInternalDomain(domain: String): Boolean =
        PrivacyGuardValidator.isInternalDomain(domain)
}
