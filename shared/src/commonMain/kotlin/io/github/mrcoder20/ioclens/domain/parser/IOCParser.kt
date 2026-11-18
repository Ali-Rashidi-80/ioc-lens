package io.github.mrcoder20.ioclens.domain.parser

import io.github.mrcoder20.ioclens.domain.model.HashAlgorithm
import io.github.mrcoder20.ioclens.domain.model.ParsedIoc

class IOCParser {

    companion object {
        private const val MAX_NON_URL_LENGTH = 512
        private const val MAX_URL_LENGTH = 8192
        private const val MAX_DOMAIN_LENGTH = 253
        private const val MAX_EXTRACT_INPUT_LENGTH = 100_000

        private val INVISIBLE_CHARS_REGEX = Regex("[\u200B\u200C\u200D\uFEFF\u200E\u200F\u00A0]")

        private val NON_DOMAIN_EXTENSIONS = setOf(
            "apk", "exe", "bin", "pdf", "zip", "rar", "7z", "tar", "gz", "msi",
            "dll", "dmg", "iso", "img", "deb", "rpm", "jar", "png", "jpg", "jpeg",
            "gif", "webp", "docx", "xlsx", "pptx", "txt", "csv", "sys", "bat", "cmd",
            "ps1", "sh", "vbs", "elf", "so", "dylib", "ipa"
        )

        // Real ICANN TLDs that overlap with file extensions are permitted when inside an explicit scheme URL
        private val URL_ALLOWED_ICANN_OVERLAP_TLDS = setOf("zip", "sh", "so")

        private val DOMAIN_REGEX = Regex("""^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+(xn--[a-zA-Z0-9-]{2,}|[a-zA-Z]{2,})$""")

        fun refang(input: String): String {
            var cleaned = input.trim()
                .replace(INVISIBLE_CHARS_REGEX, "")

            if (cleaned.startsWith("<") && cleaned.endsWith(">") && cleaned.length >= 2) {
                cleaned = cleaned.substring(1, cleaned.length - 1).trim()
            }
            if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length >= 2) {
                cleaned = cleaned.substring(1, cleaned.length - 1).trim()
            }
            if (cleaned.startsWith("'") && cleaned.endsWith("'") && cleaned.length >= 2) {
                cleaned = cleaned.substring(1, cleaned.length - 1).trim()
            }
            if (cleaned.startsWith("`") && cleaned.endsWith("`") && cleaned.length >= 2) {
                cleaned = cleaned.substring(1, cleaned.length - 1).trim()
            }

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
    }

    fun refang(input: String): String = Companion.refang(input)

    fun parse(rawInput: String): ParsedIoc {
        val exact = parseExact(rawInput)
        if (exact !is ParsedIoc.Unknown) return exact

        if (rawInput.contains(Regex("""\s""")) || (rawInput.startsWith("(") && rawInput.endsWith(")"))) {
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

        // 1. File Hash Check (MD5=32, SHA1=40, SHA256=64, SHA384=96, SHA512=128)
        if (normalized.length in listOf(32, 40, 64, 96, 128) && normalized.all { it in '0'..'9' || it in 'a'..'f' }) {
            when (normalized.length) {
                32 -> return ParsedIoc.Hash(trimmed, normalized, HashAlgorithm.MD5)
                40 -> return ParsedIoc.Hash(trimmed, normalized, HashAlgorithm.SHA1)
                64 -> return ParsedIoc.Hash(trimmed, normalized, HashAlgorithm.SHA256)
                96 -> return ParsedIoc.Hash(trimmed, normalized, HashAlgorithm.SHA384)
                128 -> return ParsedIoc.Hash(trimmed, normalized, HashAlgorithm.SHA512)
            }
        }

        // 2. IPv4 Check (including optional port; checks POSIX octal & decimal leading-zero canonicalization)
        val ipv4Regex = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})(?::(\d{1,5}))?$""")
        val ipv4Match = ipv4Regex.matchEntire(refanged)
        if (ipv4Match != null) {
            val (s1, s2, s3, s4, portStr) = ipv4Match.destructured
            val octets = listOf(s1.toIntOrNull() ?: -1, s2.toIntOrNull() ?: -1, s3.toIntOrNull() ?: -1, s4.toIntOrNull() ?: -1)
            val portValid = portStr.isEmpty() || ((portStr.toIntOrNull() ?: -1) in 1..65535)
            if (octets.all { it in 0..255 } && portValid) {
                val ipOnly = octets.joinToString(".")
                val isPrivate = isPrivateIpv4(octets) || PrivacyGuardValidator.isPrivateIp(refanged)
                return ParsedIoc.IpAddress(trimmed, ipOnly, isIpv6 = false, isPrivate = isPrivate)
            }
        }

        // 3. IPv6 Check (strip zone ID %eth0 from normalizedValue while treating scoped addresses as private)
        val bracketedWithPortRegex = Regex("""^\[([0-9a-fA-F:.%a-zA-Z0-9_-]+)\](?::(\d{1,5}))?$""")
        val bracketedMatch = bracketedWithPortRegex.matchEntire(refanged)
        if (bracketedMatch != null) {
            val (ipPart, portStr) = bracketedMatch.destructured
            val portValid = portStr.isEmpty() || ((portStr.toIntOrNull() ?: -1) in 1..65535)
            if (portValid && isIpv6(ipPart)) {
                val isPrivate = isPrivateIpv6(ipPart)
                val cleanIp = ipPart.substringBefore('%').lowercase()
                return ParsedIoc.IpAddress(trimmed, cleanIp, isIpv6 = true, isPrivate = isPrivate)
            }
        }

        val unbracketedIpv6 = refanged.removeSurrounding("[", "]")
        if (isIpv6(unbracketedIpv6)) {
            val isPrivate = isPrivateIpv6(unbracketedIpv6)
            val cleanIp = unbracketedIpv6.substringBefore('%').lowercase()
            return ParsedIoc.IpAddress(trimmed, cleanIp, isIpv6 = true, isPrivate = isPrivate)
        }

        // 4. URL Check (explicit scheme)
        val isUrlScheme = refanged.startsWith("http://", ignoreCase = true) ||
            refanged.startsWith("https://", ignoreCase = true) ||
            refanged.startsWith("ftp://", ignoreCase = true)
        if (isUrlScheme) {
            if (refanged.length <= MAX_URL_LENGTH) {
                val host = extractHostFromUrl(refanged)
                val portOk = hasValidOptionalUrlPort(refanged)
                if (portOk && host.isNotBlank() && isValidHost(host, allowExplicitSchemeTlds = true)) {
                    val (sanitizedUrl, hadCreds) = stripUrlCredentials(refanged)
                    val isInternal = isInternalHost(host) || (!host.contains('.') && !host.contains(':'))
                    return ParsedIoc.Url(
                        rawValue = trimmed,
                        normalizedValue = sanitizedUrl,
                        host = host,
                        isInternal = isInternal,
                        hadEmbeddedCredentials = hadCreds
                    )
                }
            }
            return ParsedIoc.Unknown(trimmed, refanged)
        }

        // 4b. Scheme-less URL Check (e.g. evil.com/login.php, 1.2.3.4:8080/malware)
        if (refanged.contains('/') && !refanged.contains(' ') && refanged.length <= MAX_URL_LENGTH) {
            val hostClean = extractHostFromUrl(refanged)
            val portOk = hasValidOptionalUrlPort(refanged)
            if (portOk && isValidHost(hostClean, allowExplicitSchemeTlds = false)) {
                val (sanitizedNoScheme, hadCreds) = stripUrlCredentials("http://$refanged")
                val isInternal = isInternalHost(hostClean) || (!hostClean.contains('.') && !hostClean.contains(':'))
                return ParsedIoc.Url(
                    rawValue = trimmed,
                    normalizedValue = sanitizedNoScheme,
                    host = hostClean,
                    isInternal = isInternal,
                    hadEmbeddedCredentials = hadCreds
                )
            }
        }

        // Input length guard for non-URL tokens
        if (refanged.length > MAX_NON_URL_LENGTH) {
            return ParsedIoc.Unknown(trimmed, refanged)
        }

        // 5. Domain Check (supports standard TLDs and Punycode xn-- IDN TLDs)
        if (refanged.length <= MAX_DOMAIN_LENGTH && !refanged.contains('/') && !refanged.contains('\\') && !refanged.contains(' ') && !refanged.contains('<') && !refanged.contains('>')) {
            val cleanDomain = refanged.removeSuffix(".")
            val domainMatch = DOMAIN_REGEX.matchEntire(cleanDomain)
            if (domainMatch != null) {
                val tld = domainMatch.groupValues[1].lowercase()
                if (tld !in NON_DOMAIN_EXTENSIONS) {
                    val isInternal = isInternalDomain(cleanDomain)
                    return ParsedIoc.Domain(trimmed, cleanDomain.lowercase(), isInternal = isInternal)
                }
            }
        }

        // 6. Unknown / Plain Text / Internal Hostname (e.g. localhost)
        if (isInternalDomain(refanged)) {
            return ParsedIoc.Domain(trimmed, normalized.removeSuffix("."), isInternal = true)
        }

        return ParsedIoc.Unknown(trimmed, refanged)
    }

    fun extractFirstIoc(rawInput: String): ParsedIoc? {
        val exact = parseExact(rawInput)
        if (exact !is ParsedIoc.Unknown) return exact
        return extractAllIocs(rawInput).firstOrNull()
    }

    fun extractAllIocs(rawInput: String): List<ParsedIoc> {
        val trimmed = rawInput.trim().take(MAX_EXTRACT_INPUT_LENGTH)
        if (trimmed.isEmpty()) return emptyList()

        val refanged = refang(trimmed)
        val results = mutableListOf<ParsedIoc>()
        val seenValues = mutableSetOf<String>()

        fun addIfValid(candidate: String) {
            val stripped = candidate.trimEnd('.', ',', ';', ')', ']', '>')
            val parsed = parseExact(stripped)
            if (parsed !is ParsedIoc.Unknown && seenValues.add(parsed.normalizedValue)) {
                results.add(parsed)
            }
        }

        // 1. Scan for URLs (explicit scheme)
        val urlRegex = Regex("""(?:https?|ftp)://[^\s<>"'{}|\\^`]+""", RegexOption.IGNORE_CASE)
        urlRegex.findAll(refanged).forEach { match ->
            addIfValid(match.value)
        }

        // 2. Scan for Scheme-less URLs
        val schemelessUrlRegex = Regex("""(?:\b(?:[a-zA-Z0-9-]+\.)+(?:xn--[a-zA-Z0-9-]{2,}|[a-zA-Z]{2,})(?::\d{1,5})?|\b(?:\d{1,3}\.){3}\d{1,3}(?::\d{1,5})?|\[[0-9a-fA-F:.%a-zA-Z0-9_-]+\](?::\d{1,5})?)/[^\s<>"'{}|\\^`]+""")
        schemelessUrlRegex.findAll(refanged).forEach { match ->
            addIfValid(match.value)
        }

        // 3. Scan for Hashes (SHA-512, SHA-384, SHA-256, SHA-1, MD5)
        val hashRegex = Regex("""\b([a-fA-F0-9]{128}|[a-fA-F0-9]{96}|[a-fA-F0-9]{64}|[a-fA-F0-9]{40}|[a-fA-F0-9]{32})\b""")
        hashRegex.findAll(refanged).forEach { match ->
            addIfValid(match.value)
        }

        // 4. Scan for Bracketed IPv6
        val bracketedIpv6Regex = Regex("""\[[0-9a-fA-F:.%a-zA-Z0-9_-]+\](?::\d{1,5})?""")
        bracketedIpv6Regex.findAll(refanged).forEach { match ->
            addIfValid(match.value)
        }

        // 5. Scan for Unbracketed IPv6
        val unbracketedIpv6Regex = Regex("""(?<![0-9a-fA-F:])(?:(?:[0-9a-fA-F]{1,4}:){1,7}[0-9a-fA-F]{1,4}|::(?:[0-9a-fA-F]{1,4}:){0,6}[0-9a-fA-F]{1,4}|(?:[0-9a-fA-F]{1,4}:)+:(?:[0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4}|(?:[0-9a-fA-F]{1,4}:){1,6}:(?:\d{1,3}\.){3}\d{1,3}|::(?:ffff:)?(?:\d{1,3}\.){3}\d{1,3})(?:%[a-zA-Z0-9_-]+)?(?![0-9a-fA-F:])""")
        unbracketedIpv6Regex.findAll(refanged).forEach { match ->
            addIfValid(match.value)
        }

        // 6. Scan for IPv4
        val ipv4Regex = Regex("""\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?::\d{1,5})?\b""")
        ipv4Regex.findAll(refanged).forEach { match ->
            addIfValid(match.value)
        }

        // 7. Scan for Domains (including Punycode TLDs)
        val domainRegex = Regex("""\b(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+(?:xn--[a-zA-Z0-9-]{2,}|[a-zA-Z]{2,})\b""")
        domainRegex.findAll(refanged).forEach { match ->
            addIfValid(match.value)
        }

        return results
    }

    private fun stripUrlCredentials(url: String): Pair<String, Boolean> {
        val schemeEnd = url.indexOf("://")
        if (schemeEnd == -1) return url to false
        val schemePrefix = url.substring(0, schemeEnd + 3)
        val rest = url.substring(schemeEnd + 3)
        val delimiterIdx = rest.indexOfFirst { it == '/' || it == '\\' || it == '?' || it == '#' }
        val authority = if (delimiterIdx == -1) rest else rest.substring(0, delimiterIdx)
        val suffix = if (delimiterIdx == -1) "" else rest.substring(delimiterIdx)
        if (!authority.contains('@')) return url to false
        val cleanAuthority = authority.substringAfterLast('@')
        return "$schemePrefix$cleanAuthority$suffix" to true
    }

    private fun hasValidOptionalUrlPort(url: String): Boolean {
        val noScheme = if (url.contains("://")) url.substringAfter("://") else url
        val authority = noScheme
            .substringBefore('/')
            .substringBefore('\\')
            .substringBefore('?')
            .substringBefore('#')
        val hostPort = if (authority.contains('@')) authority.substringAfterLast('@') else authority
        val portCandidate = if (hostPort.startsWith('[')) {
            val closeIdx = hostPort.indexOf(']')
            if (closeIdx == -1) return false
            val after = hostPort.substring(closeIdx + 1)
            if (after.isEmpty()) return true
            if (!after.startsWith(':')) return false
            after.substring(1)
        } else if (hostPort.contains(':')) {
            hostPort.substringAfter(':')
        } else {
            return true
        }
        val port = portCandidate.toIntOrNull() ?: return false
        return port in 1..65535
    }

    private fun isPrivateIpv4(octets: List<Int>): Boolean =
        PrivacyGuardValidator.isPrivateIpv4(octets)

    private fun isIpv6(input: String): Boolean =
        PrivacyGuardValidator.isIpv6(input)

    private fun isPrivateIpv6(input: String): Boolean =
        PrivacyGuardValidator.isPrivateIpv6(input)

    private fun extractHostFromUrl(url: String): String =
        PrivacyGuardValidator.extractHostFromUrl(url)

    private fun isValidHost(host: String, allowExplicitSchemeTlds: Boolean): Boolean {
        if (host.isBlank()) return false
        if (host.contains(' ') || host.contains('<') || host.contains('>') || host.contains('"') || host.contains('\'') || host.contains('\\') || host.contains('/')) {
            return false
        }
        val ipv4Regex = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""")
        if (ipv4Regex.matches(host)) {
            val octets = host.split('.').mapNotNull { it.toIntOrNull() }
            return octets.size == 4 && octets.all { it in 0..255 }
        }
        if (isIpv6(host)) return true
        if (isInternalDomain(host)) return true
        val domainMatch = DOMAIN_REGEX.matchEntire(host.removeSuffix(".")) ?: return false
        val tld = domainMatch.groupValues[1].lowercase()
        if (tld in NON_DOMAIN_EXTENSIONS) {
            return allowExplicitSchemeTlds && tld in URL_ALLOWED_ICANN_OVERLAP_TLDS
        }
        return true
    }

    private fun isInternalHost(host: String): Boolean =
        PrivacyGuardValidator.isInternalHost(host)

    private fun isInternalDomain(domain: String): Boolean =
        PrivacyGuardValidator.isInternalDomain(domain)
}
