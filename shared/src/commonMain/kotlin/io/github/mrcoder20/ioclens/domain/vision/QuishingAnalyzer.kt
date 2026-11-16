package io.github.mrcoder20.ioclens.domain.vision

import io.github.mrcoder20.ioclens.domain.model.ParsedIoc
import io.github.mrcoder20.ioclens.domain.parser.IOCParser
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object QuishingAnalyzer {

    private const val MAX_PAYLOAD_LEN = 4096

    private val ZERO_WIDTH_REGEX = Regex("[\u200B\u200C\u200D\uFEFF\u200E\u200F\u2060\u00A0]")
    private val BIDI_RLO_REGEX = Regex("[\u202A-\u202E\u2066-\u2069]")

    private val DANGEROUS_SCHEMES = setOf(
        "javascript", "data", "intent", "itms-services", "blob", "file", "vbscript", "about"
    )

    private val HIGH_RISK_TLDS = setOf(
        ".top", ".xyz", ".click", ".country", ".stream", ".gq", ".cf", ".tk", ".ml", ".ga", ".rest", ".work", ".fit"
    )

    private val PHISHING_KEYWORDS = listOf(
        "login", "signin", "verify", "mfa", "docusign", "banking", "secure", "update-account", "password", "session"
    )

    private val URL_SHORTENERS = setOf(
        "bit.ly", "tinyurl.com", "t.co", "cutt.ly", "is.gd", "ow.ly", "rb.gy", "goo.gl"
    )

    private val MALWARE_EXTENSIONS = setOf(
        ".apk", ".exe", ".scr", ".msi", ".ps1", ".bat", ".cmd", ".vbs", ".iso", ".zip", ".tar.gz", ".dmg", ".dll"
    )

    private val REDIRECT_PARAMS = setOf(
        "u", "url", "target", "dest", "destination", "redirect", "next", "link", "r", "forward", "to", "q"
    )

    private val HEX_IP_REGEX = Regex("""^0x[0-9a-fA-F]+$""")
    private val DWORD_IP_REGEX = Regex("""^\d{8,11}$""")
    private val OCTAL_IP_REGEX = Regex("""^0[0-7]+(\.0[0-7]+){3}$""")

    private val iocParser = IOCParser()

    @OptIn(ExperimentalEncodingApi::class)
    fun analyze(rawText: String, type: BarcodeType = BarcodeType.QR_CODE): BarcodePayload {
        if (rawText.isBlank()) {
            return BarcodePayload(
                rawText = "",
                defangedText = "",
                type = type,
                detectedIocType = null,
                quishingRisk = QuishingRisk.SAFE,
                riskReason = null,
                timestamp = 0L
            )
        }

        // Bounded length protection against ReDoS / memory bombs
        val bounded = if (rawText.length > MAX_PAYLOAD_LEN) {
            rawText.substring(0, MAX_PAYLOAD_LEN)
        } else {
            rawText
        }

        var risk = QuishingRisk.SAFE
        var riskReason: String? = null
        var unmaskedTarget: String? = null
        var punycodeHost: String? = null

        // 1. Check for Bidirectional RLO Override Injection
        if (BIDI_RLO_REGEX.containsMatchIn(bounded)) {
            risk = QuishingRisk.HIGH_RISK
            riskReason = appendReason(riskReason, "Bidirectional / RLO (U+202E) override spoofing detected")
        }

        // 2. Check for Invisible Zero-Width Space Injection
        if (ZERO_WIDTH_REGEX.containsMatchIn(bounded)) {
            risk = QuishingRisk.HIGH_RISK
            riskReason = appendReason(riskReason, "Invisible zero-width character injection detected")
        }

        val sanitized = bounded
            .replace(BIDI_RLO_REGEX, "")
            .replace(ZERO_WIDTH_REGEX, "")
            .trim()

        val lower = sanitized.lowercase()

        // 3. Dangerous URI schemes
        val colonIdx = sanitized.indexOf(':')
        if (colonIdx > 0 && colonIdx < 20) {
            val scheme = sanitized.substring(0, colonIdx).lowercase().trim()
            if (scheme in DANGEROUS_SCHEMES) {
                risk = QuishingRisk.HIGH_RISK
                riskReason = appendReason(riskReason, "Dangerous URI scheme detected: $scheme")
            }
        }

        // 4. Wi-Fi Configuration Strings
        if (sanitized.startsWith("WIFI:", ignoreCase = true)) {
            if (risk == QuishingRisk.SAFE) {
                risk = QuishingRisk.SUSPICIOUS
                riskReason = appendReason(riskReason, "Wi-Fi network configuration payload")
            }
        }

        // 5. URL analysis & Redirections
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            val urlRisk = evaluateUrlRisk(sanitized)
            if (urlRisk.first == QuishingRisk.HIGH_RISK) {
                risk = QuishingRisk.HIGH_RISK
                if (urlRisk.second != null) riskReason = appendReason(riskReason, urlRisk.second!!)
            } else if (urlRisk.first == QuishingRisk.SUSPICIOUS && risk == QuishingRisk.SAFE) {
                risk = QuishingRisk.SUSPICIOUS
                if (urlRisk.second != null) riskReason = appendReason(riskReason, urlRisk.second!!)
            }

            // Extract nested redirects
            val unmasked = extractNestedRedirect(sanitized)
            if (unmasked != null) {
                unmaskedTarget = unmasked
                risk = QuishingRisk.HIGH_RISK
                riskReason = appendReason(riskReason, "Nested redirection chain detected -> ${defang(unmasked)}")
            }

            // Detect Punycode or IDN homoglyphs
            val host = extractHost(sanitized)
            if (host.contains("xn--") || containsMixedScripts(host)) {
                punycodeHost = host
                risk = QuishingRisk.HIGH_RISK
                riskReason = appendReason(riskReason, "Punycode / IDN homoglyph spoofing detected: $host")
            }
        }

        // 6. Direct malware dropper extension check
        val pathWithoutQuery = sanitized.substringBefore('?').substringBefore('#')
        for (ext in MALWARE_EXTENSIONS) {
            if (pathWithoutQuery.endsWith(ext, ignoreCase = true)) {
                risk = QuishingRisk.HIGH_RISK
                riskReason = appendReason(riskReason, "Direct executable payload download link ($ext)")
                break
            }
        }

        // 7. Symmetrical defanged representation
        val defanged = defang(sanitized)

        // 8. IOC detection via IOCParser
        val parsedIoc = iocParser.parse(sanitized)
        val iocType = when (parsedIoc) {
            is ParsedIoc.IpAddress -> "IP_ADDRESS"
            is ParsedIoc.Domain -> "DOMAIN"
            is ParsedIoc.Url -> "URL"
            is ParsedIoc.Hash -> "FILE_HASH"
            is ParsedIoc.FilePayload -> "FILE_HASH"
            is ParsedIoc.Unknown -> null
        }

        return BarcodePayload(
            rawText = sanitized,
            defangedText = defanged,
            type = type,
            detectedIocType = iocType,
            quishingRisk = risk,
            riskReason = riskReason,
            unmaskedTargetUrl = unmaskedTarget,
            punycodeHost = punycodeHost,
            timestamp = 0L
        )
    }

    fun defang(input: String): String {
        return input
            .replace("http://", "hxxp://", ignoreCase = true)
            .replace("https://", "hxxps://", ignoreCase = true)
            .replace("ftp://", "fxp://", ignoreCase = true)
            .replace(".", "[.]")
            .replace("@", "[@]")
    }

    private fun evaluateUrlRisk(url: String): Pair<QuishingRisk, String?> {
        val host = extractHost(url)
        val lowerHost = host.lowercase()
        val lowerUrl = url.lowercase()

        // Authority Userinfo spoofing: http://trusted.com@evil.com
        val authorityPart = url.substringAfter("://").substringBefore('/')
        if (authorityPart.contains('@')) {
            val userinfo = authorityPart.substringBefore('@')
            val actualHost = authorityPart.substringAfter('@').substringBefore(':')
            return Pair(QuishingRisk.HIGH_RISK, "Authority userinfo spoofing detected: masquerading as $userinfo -> $actualHost")
        }

        // Numeric IP Host obfuscation
        val hostWithoutPort = lowerHost.substringBefore(':')
        if (HEX_IP_REGEX.matches(hostWithoutPort) || DWORD_IP_REGEX.matches(hostWithoutPort) || OCTAL_IP_REGEX.matches(hostWithoutPort)) {
            return Pair(QuishingRisk.HIGH_RISK, "Hex/DWORD/Octal numeric IP literal obfuscation")
        }

        // Raw IPv4 host literal
        val parts = hostWithoutPort.split('.')
        if (parts.size == 4 && parts.all { it.toIntOrNull() in 0..255 }) {
            return Pair(QuishingRisk.HIGH_RISK, "Raw IP host literal ($hostWithoutPort)")
        }

        // URL Shortener
        if (URL_SHORTENERS.any { lowerHost == it || lowerHost.endsWith(".$it") }) {
            return Pair(QuishingRisk.SUSPICIOUS, "URL Shortener: destination target is masked ($lowerHost)")
        }

        // Suspicious TLD + Phishing keywords
        val hasSuspiciousTld = HIGH_RISK_TLDS.any { lowerHost.endsWith(it) }
        val hasPhishingKeyword = PHISHING_KEYWORDS.any { lowerUrl.contains(it) }

        if (hasSuspiciousTld && hasPhishingKeyword) {
            return Pair(QuishingRisk.HIGH_RISK, "High-risk TLD & credential harvest keyword detected in URL")
        }

        if (hasSuspiciousTld) {
            return Pair(QuishingRisk.HIGH_RISK, "High-risk TLD detected in host ($lowerHost)")
        }

        return Pair(QuishingRisk.SAFE, null)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun extractNestedRedirect(url: String): String? {
        val query = url.substringAfter('?', "")
        if (query.isBlank()) return null

        val pairs = query.split('&')
        for (pair in pairs) {
            val key = pair.substringBefore('=').lowercase().trim()
            val value = pair.substringAfter('=', "").trim()
            if (value.isBlank()) continue

            if (key in REDIRECT_PARAMS) {
                // Check if value is already a plain URL
                if (value.startsWith("http%3A", ignoreCase = true) || value.startsWith("https%3A", ignoreCase = true)) {
                    val decoded = runCatching { decodeUrlComponent(value) }.getOrNull()
                    if (decoded != null && (decoded.startsWith("http://", ignoreCase = true) || decoded.startsWith("https://", ignoreCase = true))) {
                        return decoded
                    }
                }

                // Check Base64 encoded URL
                if (value.length >= 8 && value.length <= 512) {
                    val decoded = runCatching {
                        val cleanVal = value.replace("%3D", "=").replace("%3d", "=")
                        val bytes = try {
                            Base64.UrlSafe.decode(cleanVal)
                        } catch (e: Exception) {
                            Base64.Default.decode(cleanVal)
                        }
                        bytes.decodeToString()
                    }.getOrNull()

                    if (decoded != null && (decoded.startsWith("http://", ignoreCase = true) || decoded.startsWith("https://", ignoreCase = true))) {
                        return decoded
                    }
                }
            }
        }
        return null
    }

    private fun decodeUrlComponent(encoded: String): String {
        return encoded
            .replace("%3A", ":", ignoreCase = true)
            .replace("%2F", "/", ignoreCase = true)
            .replace("%3F", "?", ignoreCase = true)
            .replace("%3D", "=", ignoreCase = true)
            .replace("%26", "&", ignoreCase = true)
            .replace("%40", "@", ignoreCase = true)
    }

    private fun extractHost(url: String): String {
        val afterScheme = url.substringAfter("://", url)
        val authority = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        return authority.substringAfter('@').substringBefore(':')
    }

    private fun containsMixedScripts(host: String): Boolean {
        var hasLatin = false
        var hasCyrillicOrGreek = false

        for (ch in host) {
            val code = ch.code
            if ((code in 0x0041..0x005A) || (code in 0x0061..0x007A)) {
                hasLatin = true
            } else if ((code in 0x0400..0x04FF) || (code in 0x0370..0x03FF)) {
                hasCyrillicOrGreek = true
            }
        }
        return hasLatin && hasCyrillicOrGreek
    }

    private fun appendReason(current: String?, newReason: String): String {
        return if (current == null) newReason else "$current | $newReason"
    }
}
