package io.github.mrcoder20.ioclens.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.mrcoder20.ioclens.core.components.CameraController
import io.github.mrcoder20.ioclens.core.components.CameraVectorIcon
import io.github.mrcoder20.ioclens.core.components.ClipboardVectorIcon
import io.github.mrcoder20.ioclens.core.components.FileDocumentVectorIcon
import io.github.mrcoder20.ioclens.core.components.QrCodeVectorIcon
import io.github.mrcoder20.ioclens.core.components.UploadCloudVectorIcon
import io.github.mrcoder20.ioclens.core.components.springPress
import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.LocalizationProvider
import io.github.mrcoder20.ioclens.core.theme.CardDark
import io.github.mrcoder20.ioclens.core.theme.PrimaryCyan
import io.github.mrcoder20.ioclens.core.theme.PrimaryNeon
import io.github.mrcoder20.ioclens.core.theme.topBorderHighlightBrush
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class DiscoveredIoc(
    val id: String,
    val type: String, // "IP", "HASH", "URL", "DOMAIN", "EMAIL", "CVE"
    val subtype: String,
    val value: String,
    val defanged: String,
    val metaTag: String = ""
)

private fun refangText(raw: String): String {
    return raw
        .replace(Regex("""hxxps?://""", RegexOption.IGNORE_CASE)) { it.value.lowercase().replace("hxxp", "http") }
        .replace(Regex("""fxp://""", RegexOption.IGNORE_CASE), "ftp://")
        .replace(Regex("""\[\.\]|\(\.\)|\{\.\}|\[dot\]|\(dot\)""", RegexOption.IGNORE_CASE), ".")
        .replace(Regex("""\[@\]|\(@\)|\{@\}|\[at\]|\(at\)""", RegexOption.IGNORE_CASE), "@")
        .replace("[:]", ":")
        .replace("(:)", ":")
        .replace("{:}", ":")
        .replace("[/]", "/")
        .replace("(/)", "/")
}

private fun defangText(value: String): String {
    return value
        .replace(Regex("""^http://""", RegexOption.IGNORE_CASE), "hxxp://")
        .replace(Regex("""^https://""", RegexOption.IGNORE_CASE), "hxxps://")
        .replace(".", "[.]")
        .replace("@", "[@]")
}

private fun extractAllIocsFromText(rawText: String): List<DiscoveredIoc> {
    if (rawText.isBlank()) return emptyList()
    val normalized = refangText(rawText)
    val map = linkedMapOf<String, DiscoveredIoc>()

    fun addIoc(type: String, subtype: String, value: String, meta: String = "") {
        val key = "$type:${value.lowercase()}"
        if (!map.containsKey(key)) {
            map[key] = DiscoveredIoc(
                id = key,
                type = type,
                subtype = subtype,
                value = value,
                defanged = defangText(value),
                metaTag = meta
            )
        }
    }

    // 1. Hashes (SHA-512, SHA-256, SHA-1, MD5)
    val hashRegex = Regex("""\b([a-fA-F0-9]{128}|[a-fA-F0-9]{64}|[a-fA-F0-9]{40}|[a-fA-F0-9]{32})\b""")
    hashRegex.findAll(normalized).forEach { m ->
        val h = m.value.lowercase()
        if (!Regex("""^\d+$""").matches(h)) {
            when (h.length) {
                128 -> addIoc("HASH", "SHA-512", h, "512-bit")
                64 -> addIoc("HASH", "SHA-256", h, "256-bit")
                40 -> addIoc("HASH", "SHA-1", h, "160-bit")
                32 -> addIoc("HASH", "MD5", h, "128-bit")
            }
        }
    }

    // 2. CVEs
    val cveRegex = Regex("""\b(CVE-\d{4}-\d{4,7})\b""", RegexOption.IGNORE_CASE)
    cveRegex.findAll(normalized).forEach { m ->
        val cve = m.value.uppercase()
        val year = cve.split("-").getOrNull(1) ?: "CVE"
        addIoc("CVE", "CVE ID", cve, "Year $year")
    }

    // 3. IPv4
    val ipv4Regex = Regex("""\b((?:\d{1,3}\.){3}\d{1,3})\b""")
    ipv4Regex.findAll(normalized).forEach { m ->
        val ip = m.value
        val parts = ip.split(".").mapNotNull { it.toIntOrNull() }
        if (parts.size == 4 && parts.all { it in 0..255 }) {
            val scope = when {
                parts[0] == 10 || (parts[0] == 172 && parts[1] in 16..31) || (parts[0] == 192 && parts[1] == 168) -> "RFC1918 Private"
                parts[0] == 127 -> "Loopback"
                else -> "Public IPv4"
            }
            addIoc("IP", "IPv4", ip, scope)
        }
    }

    // 4. URLs
    val urlRegex = Regex("""\b((?:https?|ftp)://[^\s"'<>\]\)]+)""", RegexOption.IGNORE_CASE)
    val urlHosts = mutableSetOf<String>()
    urlRegex.findAll(normalized).forEach { m ->
        val u = m.value.trimEnd('.', ',', ';', ':', '!', '?')
        addIoc("URL", "URL", u, if (u.startsWith("https", ignoreCase = true)) "HTTPS" else "HTTP")
        try {
            val afterScheme = u.substringAfter("://").substringBefore("/").substringBefore(":")
            if (afterScheme.isNotBlank()) urlHosts.add(afterScheme.lowercase())
        } catch (_: Exception) {}
    }

    // 5. Emails
    val emailRegex = Regex("""\b([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})\b""")
    val emailDomains = mutableSetOf<String>()
    emailRegex.findAll(normalized).forEach { m ->
        val email = m.value.lowercase()
        val dom = email.substringAfter("@")
        emailDomains.add(dom)
        addIoc("EMAIL", "Email", email, "@$dom")
    }

    // 6. Domains
    val ignoredExts = setOf(
        "exe", "dll", "sys", "bat", "ps1", "vbs", "js", "jar", "apk", "msi",
        "txt", "log", "json", "csv", "xml", "yml", "yaml", "ini", "cfg", "conf",
        "pdf", "docx", "xlsx", "zip", "rar", "7z", "tar", "gz", "png", "jpg", "jpeg", "webp"
    )
    val domainRegex = Regex("""\b((?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,24})\b""")
    domainRegex.findAll(normalized).forEach { m ->
        val dom = m.value.lowercase()
        val tld = dom.substringAfterLast(".")
        if (tld !in ignoredExts && !urlHosts.contains(dom) && !emailDomains.contains(dom)) {
            val parts = dom.split(".").mapNotNull { it.toIntOrNull() }
            if (parts.size != 4) { // Not an IPv4
                addIoc("DOMAIN", "Domain", dom, ".$tld")
            }
        }
    }

    return map.values.toList()
}

private fun getThreatIntelLinks(ioc: DiscoveredIoc): List<Pair<String, String>> {
    val encoded = ioc.value
    return when (ioc.type) {
        "IP" -> listOf(
            "VirusTotal" to "https://www.virustotal.com/gui/ip-address/$encoded",
            "AbuseIPDB" to "https://www.abuseipdb.com/check/$encoded"
        )
        "HASH" -> listOf(
            "VirusTotal" to "https://www.virustotal.com/gui/file/$encoded",
            "MalwareBazaar" to "https://bazaar.abuse.ch/browse.php?search=$encoded"
        )
        "DOMAIN" -> listOf(
            "VirusTotal" to "https://www.virustotal.com/gui/domain/$encoded",
            "URLScan.io" to "https://urlscan.io/search/#domain%3A$encoded"
        )
        "URL" -> listOf(
            "VirusTotal" to "https://www.virustotal.com/gui/search/$encoded",
            "URLScan.io" to "https://urlscan.io/search/#$encoded"
        )
        "CVE" -> listOf(
            "NVD (NIST)" to "https://nvd.nist.gov/vuln/detail/$encoded",
            "CVE.org" to "https://www.cve.org/CVERecord?id=$encoded"
        )
        else -> listOf(
            "VirusTotal" to "https://www.virustotal.com/gui/search/$encoded"
        )
    }
}

/**
 * Google Lens Live Camera & Visual IOC Scanner Modal.
 * Unified 3-Mode Vision Hub (Live Camera, File/Image Upload, Manual Text Entry).
 */
@Composable
fun CameraScannerDialog(
    cameraController: CameraController,
    appLanguage: AppLanguage,
    onPayloadDetected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isRtl = appLanguage.isRtl
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Active Vision Mode: 0 = دوربین زنده (Live Camera), 1 = آپلود فایل / تصویر, 2 = ورود دستی
    var activeMode by remember { mutableStateOf(0) }
    // OCR Precision Language: "eng" (high precision) vs "eng+fas"
    var ocrLang by remember { mutableStateOf("eng") }
    // Viewfinder framing shape: false = standard box, true = linear strip for long hashes/links
    var isLinearFrame by remember { mutableStateOf(false) }

    // Manual input text
    var manualInput by remember { mutableStateOf("") }
    // Extracted text workspace (editable)
    var extractedText by remember { mutableStateOf("") }
    // Extracted IOC list
    var discoveredIocs by remember { mutableStateOf(emptyList<DiscoveredIoc>()) }
    // Active category filter: ALL, IP, HASH, URL_DOM, EMAIL, CVE
    var activeFilter by remember { mutableStateOf("ALL") }

    // Toast message state
    var toastMessage by remember { mutableStateOf<String?>(null) }
    // Structural Inspection modal target
    var inspectingIoc by remember { mutableStateOf<DiscoveredIoc?>(null) }
    // OCR processing simulation progress
    var isProcessingOcr by remember { mutableStateOf(false) }

    fun showToast(msg: String) {
        toastMessage = msg
        coroutineScope.launch {
            delay(2400)
            if (toastMessage == msg) toastMessage = null
        }
    }

    fun applyExtractedText(text: String) {
        val trimmed = text.trim()
        extractedText = trimmed
        discoveredIocs = extractAllIocsFromText(trimmed)
        activeFilter = "ALL"
        showToast(if (isRtl) "استخراج متن و تحلیل شاخص‌ها انجام شد" else "Text & IOC analysis completed")
    }

    val infiniteTransition = rememberInfiniteTransition(label = "GoogleLensLaser")
    val laserFraction by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserSweep"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 760.dp)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(22.dp)),
            color = Color(0xFF070B14),
            border = BorderStroke(1.2.dp, topBorderHighlightBrush(PrimaryNeon, isDark = true)),
            shadowElevation = 32.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    // 1. Top Header: Brand Identity on Left, OCR Lang Mode, ALWAYS-PINNED 'X' on Physical Right
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PrimaryCyan.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CameraVectorIcon(tint = PrimaryCyan, size = 20.dp)
                                }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "IOC Lens Vision",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = PrimaryCyan.copy(alpha = 0.12f),
                                            border = BorderStroke(0.6.dp, PrimaryCyan.copy(alpha = 0.35f))
                                        ) {
                                            Text(
                                                text = if (isRtl) "اسکنر هوشمند" else "Smart Vision",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryCyan
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (isRtl) "استخراج دقیق متون، هش‌ها، IP و شاخص‌های تهدید" else "Accurate OCR text, hash, IP & threat payload triage",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Precision OCR Language Mode Toggle
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0F172A),
                                    border = BorderStroke(0.8.dp, Color(0xFF1E293B))
                                ) {
                                    Row(modifier = Modifier.padding(2.dp)) {
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { ocrLang = "eng" },
                                            color = if (ocrLang == "eng") PrimaryCyan.copy(alpha = 0.20f) else Color.Transparent,
                                            border = if (ocrLang == "eng") BorderStroke(0.8.dp, PrimaryCyan.copy(alpha = 0.40f)) else null
                                        ) {
                                            Text(
                                                text = "EN (هش/کد)",
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (ocrLang == "eng") PrimaryCyan else Color(0xFF94A3B8)
                                            )
                                        }
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { ocrLang = "eng+fas" },
                                            color = if (ocrLang == "eng+fas") PrimaryCyan.copy(alpha = 0.20f) else Color.Transparent,
                                            border = if (ocrLang == "eng+fas") BorderStroke(0.8.dp, PrimaryCyan.copy(alpha = 0.40f)) else null
                                        ) {
                                            Text(
                                                text = "فارسی + EN",
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (ocrLang == "eng+fas") PrimaryCyan else Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                }

                                // Physical Top-Right 'X' Close Button (Always visible on any screen width!)
                                Surface(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .springPress()
                                        .clickable(onClick = onDismiss)
                                        .pointerHoverIcon(PointerIcon.Hand),
                                    color = Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                                    shape = CircleShape
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "✕",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE2E8F0)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Unified 3-Mode Segmented Navigation Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.90f),
                        border = BorderStroke(0.8.dp, Color(0xFF1E293B))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val modes = listOf(
                                Triple(0, if (isRtl) "دوربین زنده" else "Live Camera", "📷"),
                                Triple(1, if (isRtl) "آپلود فایل / تصویر" else "File & Image", "📁"),
                                Triple(2, if (isRtl) "ورود دستی" else "Manual Entry", "✍")
                            )

                            modes.forEach { (index, title, icon) ->
                                val isSelected = activeMode == index
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .springPress()
                                        .clickable { activeMode = index }
                                        .pointerHoverIcon(PointerIcon.Hand),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) PrimaryCyan.copy(alpha = 0.18f) else Color.Transparent,
                                    border = if (isSelected) BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.45f)) else null
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(text = icon, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = title,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) PrimaryCyan else Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Scrollable Interactive Workspace Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (activeMode) {
                            // ================= MODE 0: GOOGLE LENS LIVE CAMERA =================
                            0 -> {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp)),
                                    color = Color(0xFF040812),
                                    border = BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.45f))
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                        // Viewfinder Viewport with Google Lens Reticles & Laser
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(if (isLinearFrame) 130.dp else 190.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF020409))
                                        ) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                val w = size.width
                                                val h = size.height
                                                val stroke = 3.dp.toPx()
                                                val cornerLen = 26.dp.toPx()

                                                // Top-Left corner (Cyan)
                                                drawLine(PrimaryCyan, Offset(16f, 16f), Offset(16f + cornerLen, 16f), stroke, StrokeCap.Round)
                                                drawLine(PrimaryCyan, Offset(16f, 16f), Offset(16f, 16f + cornerLen), stroke, StrokeCap.Round)
                                                // Top-Right corner (Cyan)
                                                drawLine(PrimaryCyan, Offset(w - 16f, 16f), Offset(w - 16f - cornerLen, 16f), stroke, StrokeCap.Round)
                                                drawLine(PrimaryCyan, Offset(w - 16f, 16f), Offset(w - 16f, 16f + cornerLen), stroke, StrokeCap.Round)
                                                // Bottom-Left corner (Emerald)
                                                val emerald = Color(0xFF10B981)
                                                drawLine(emerald, Offset(16f, h - 16f), Offset(16f + cornerLen, h - 16f), stroke, StrokeCap.Round)
                                                drawLine(emerald, Offset(16f, h - 16f), Offset(16f, h - 16f - cornerLen), stroke, StrokeCap.Round)
                                                // Bottom-Right corner (Emerald)
                                                drawLine(emerald, Offset(w - 16f, h - 16f), Offset(w - 16f - cornerLen, h - 16f), stroke, StrokeCap.Round)
                                                drawLine(emerald, Offset(w - 16f, h - 16f), Offset(w - 16f, h - 16f - cornerLen), stroke, StrokeCap.Round)

                                                // Sweeping Laser Beam
                                                val y = h * laserFraction
                                                drawLine(
                                                    brush = Brush.horizontalGradient(
                                                        listOf(Color.Transparent, PrimaryCyan, Color(0xFF34D399), PrimaryCyan, Color.Transparent)
                                                    ),
                                                    start = Offset(16f, y),
                                                    end = Offset(w - 16f, y),
                                                    strokeWidth = 2.dp.toPx()
                                                )
                                            }

                                            // Viewfinder center instructions
                                            Column(
                                                modifier = Modifier.align(Alignment.Center).padding(horizontal = 16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                QrCodeVectorIcon(tint = PrimaryCyan.copy(alpha = 0.85f), size = 36.dp)
                                                Text(
                                                    text = if (isRtl) "متن، هش یا IP را داخل کادر قرار دهید" else "Point camera at text, hash or indicator",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFFE2E8F0),
                                                    textAlign = TextAlign.Center
                                                )
                                                Text(
                                                    text = "Engine: ${cameraController.platformName} • Zero-Cloud Privacy",
                                                    fontSize = 9.5.sp,
                                                    color = PrimaryCyan.copy(alpha = 0.70f),
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }

                                            // Top-Right Framing Aspect Toggle
                                            Surface(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(8.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { isLinearFrame = !isLinearFrame }
                                                    .pointerHoverIcon(PointerIcon.Hand),
                                                color = Color.Black.copy(alpha = 0.65f),
                                                border = BorderStroke(0.8.dp, PrimaryCyan.copy(alpha = 0.35f))
                                            ) {
                                                Text(
                                                    text = if (isLinearFrame) (if (isRtl) "کادر خطی (هش/لینک)" else "Linear Frame") else (if (isRtl) "کادر استاندارد" else "Standard Frame"),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = PrimaryCyan
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Camera Hardware Triggers & Actions
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { cameraController.launchCamera() },
                                                modifier = Modifier.weight(1f).height(40.dp).springPress(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = PrimaryCyan.copy(alpha = 0.15f),
                                                    contentColor = PrimaryCyan
                                                ),
                                                border = BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.45f)),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    CameraVectorIcon(tint = PrimaryCyan, size = 16.dp)
                                                    Text(text = if (isRtl) "دوربین دیوایس" else "Device Camera", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Button(
                                                onClick = { cameraController.launchScreenSnip() },
                                                modifier = Modifier.weight(1f).height(40.dp).springPress(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color.White.copy(alpha = 0.08f),
                                                    contentColor = Color.White
                                                ),
                                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(text = "✂️", fontSize = 13.sp)
                                                    Text(text = if (isRtl) "برش تصویر / اسنیپ" else "Screen Snip", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Quick Lab Samples for Instant Visual Verification
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = CardDark.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(0.8.dp, PrimaryCyan.copy(alpha = 0.25f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = if (isRtl) "نمونه‌های آماده تست آزمایشگاهی (Lab Samples)" else "Simulated Visual Lab Samples",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryCyan
                                        )
                                        Text(
                                            text = if (isRtl) "جهت ارزیابی فوری پایپ‌لاین تشخیص، یک نمونه را انتخاب و استخراج کنید:" else "Select any simulated sample to test extraction instantly:",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )

                                        val samples = listOf(
                                            Triple("WannaCry SHA-256 Ransomware", "eceb2f25bac4cbca1da5f4e390124912cd91f541ad1ccada2ae2b46f4aceb414", "MALICIOUS HASH"),
                                            Triple("URLhaus Malware Dropper", "http://61.92.220.176:37567/bin.sh", "MALICIOUS URL"),
                                            Triple("Feodo Botnet C2 IP", "195.123.245.11", "BOTNET IP"),
                                            Triple("ICANN Verified Domain", "github.com", "CLEAN DOMAIN")
                                        )

                                        samples.forEach { (label, value, tag) ->
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { applyExtractedText(value) }
                                                    .pointerHoverIcon(PointerIcon.Hand),
                                                color = Color.White.copy(alpha = 0.04f),
                                                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.10f)),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                                        Text(text = value, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF94A3B8), maxLines = 1)
                                                    }
                                                    Surface(
                                                        color = PrimaryCyan.copy(alpha = 0.12f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(text = tag, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = PrimaryCyan)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // ================= MODE 1: FILE & IMAGE UPLOAD =================
                            1 -> {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                                    border = BorderStroke(1.2.dp, PrimaryCyan.copy(alpha = 0.35f))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(PrimaryCyan.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            UploadCloudVectorIcon(tint = PrimaryCyan, size = 26.dp)
                                        }

                                        Text(
                                            text = if (isRtl) "انتخاب یا کشیدن تصویر و فایل لاگ" else "Select or drop image & log file",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )

                                        Text(
                                            text = if (isRtl)
                                                "پشتیبانی از اسکرین‌شات‌ها (PNG, JPG, WEBP) با پردازش OCR و فایل‌های لاگ (.txt, .log, .json, .csv, .xml)"
                                            else
                                                "Supports screenshots (PNG, JPG, WEBP) with OCR and log files (.txt, .log, .json, .csv, .xml)",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8),
                                            textAlign = TextAlign.Center
                                        )

                                        Button(
                                            onClick = {
                                                cameraController.launchScreenSnip()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan, contentColor = Color.Black),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.height(42.dp).springPress()
                                        ) {
                                            Text(
                                                text = if (isRtl) "انتخاب تصویر / برش صفحه" else "Select Image / Snip",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // ================= MODE 2: MANUAL TEXT ENTRY =================
                            2 -> {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isRtl) "متن خام، لاگ امنیتی یا هشدار را وارد کنید:" else "Enter raw log, text or security alert:",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFE2E8F0)
                                            )
                                            Text(
                                                text = if (isRtl) "پشتیبانی خودکار از فرمت‌های Defanged (hxxp, [.])" else "Auto-refangs hxxp & [.]",
                                                fontSize = 10.sp,
                                                color = PrimaryCyan
                                            )
                                        }

                                        OutlinedTextField(
                                            value = manualInput,
                                            onValueChange = { manualInput = it },
                                            modifier = Modifier.fillMaxWidth().height(130.dp),
                                            placeholder = {
                                                Text(
                                                    text = if (isRtl)
                                                        "متن، خروجی لاگ، ایمیل مشکوک، آدرس‌های IP، هش‌های MD5/SHA256 یا دامنه‌ها را اینجا جایگذاری (Paste) کنید..."
                                                    else
                                                        "Paste raw logs, firewall alerts, suspicious emails, IP addresses, MD5/SHA-256 hashes or domains...",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryCyan,
                                                unfocusedBorderColor = Color(0xFF334155),
                                                focusedContainerColor = Color(0xFF060A13),
                                                unfocusedContainerColor = Color(0xFF060A13),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = { manualInput = "" },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFEF4444).copy(alpha = 0.12f),
                                                    contentColor = Color(0xFFEF4444)
                                                ),
                                                border = BorderStroke(0.7.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(38.dp).springPress()
                                            ) {
                                                Text(text = if (isRtl) "پاک کردن" else "Clear", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = {
                                                        val clip = clipboardManager.getText()?.text
                                                        if (!clip.isNullOrBlank()) {
                                                            manualInput = clip.trim()
                                                            showToast(if (isRtl) "متن از کلیپ‌بورد جایگذاری شد" else "Pasted from clipboard")
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color.White.copy(alpha = 0.08f),
                                                        contentColor = Color.White
                                                    ),
                                                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.18f)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(38.dp).springPress()
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        ClipboardVectorIcon(tint = PrimaryCyan, size = 14.dp)
                                                        Text(text = if (isRtl) "جایگذاری (Paste)" else "Paste", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                    }
                                                }

                                                Button(
                                                    onClick = { applyExtractedText(manualInput) },
                                                    enabled = manualInput.isNotBlank(),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = PrimaryCyan,
                                                        contentColor = Color.Black
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(38.dp).springPress()
                                                ) {
                                                    Text(text = if (isRtl) "استخراج شاخص‌ها" else "Extract IOCs", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ================= 4. EDITABLE EXTRACTED TEXT PANEL =================
                        if (extractedText.isNotBlank()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF0F172A).copy(alpha = 0.90f),
                                border = BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PrimaryCyan))
                                            Text(
                                                text = if (isRtl) "متن استخراج‌شده (قابل ویرایش)" else "Extracted Text (Editable)",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF1E293B)
                                            ) {
                                                val charCount = LocalizationProvider.formatNumber(extractedText.length, appLanguage)
                                                Text(
                                                    text = if (isRtl) "$charCount کاراکتر" else "$charCount chars",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF94A3B8),
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(
                                                onClick = {
                                                    discoveredIocs = extractAllIocsFromText(extractedText)
                                                    showToast(if (isRtl) "شاخص‌ها بروزرسانی شدند" else "IOCs updated")
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                                                    contentColor = Color(0xFF10B981)
                                                ),
                                                border = BorderStroke(0.8.dp, Color(0xFF10B981).copy(alpha = 0.40f)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(34.dp).springPress()
                                            ) {
                                                Text(text = if (isRtl) "بروزرسانی IOCها" else "Reparse IOCs", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(extractedText))
                                                    showToast(if (isRtl) "متن استخراج‌شده کپی شد" else "Copied to clipboard")
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color.White.copy(alpha = 0.08f),
                                                    contentColor = Color.White
                                                ),
                                                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.18f)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(34.dp).springPress()
                                            ) {
                                                Text(text = if (isRtl) "کپی متن" else "Copy Text", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = extractedText,
                                        onValueChange = { extractedText = it },
                                        modifier = Modifier.fillMaxWidth().height(100.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PrimaryCyan,
                                            unfocusedBorderColor = Color(0xFF334155),
                                            focusedContainerColor = Color(0xFF060A13),
                                            unfocusedContainerColor = Color(0xFF060A13),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }

                        // ================= 5. EXTRACTED IOCS DASHBOARD =================
                        if (discoveredIocs.isNotEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF0F172A).copy(alpha = 0.90f),
                                border = BorderStroke(1.dp, Color(0xFF1E293B))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(
                                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF10B981).copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = "🛡️", fontSize = 16.sp)
                                            }
                                            Column {
                                                Text(
                                                    text = if (isRtl) "شاخص‌های تهدید شناسایی‌شده (IOCs)" else "Discovered Indicators (IOCs)",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                val iocCountFmt = LocalizationProvider.formatNumber(discoveredIocs.size, appLanguage)
                                                Text(
                                                    text = if (isRtl) "$iocCountFmt شاخص یکتا آماده بررسی و تریاژ" else "$iocCountFmt unique IOCs ready for triage",
                                                    fontSize = 10.5.sp,
                                                    color = Color(0xFF94A3B8)
                                                )
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(
                                                onClick = {
                                                    val allText = discoveredIocs.joinToString("\n") { "[${it.subtype}] ${it.value}" }
                                                    clipboardManager.setText(AnnotatedString(allText))
                                                    showToast(if (isRtl) "تمام شاخص‌ها در حافظه کپی شدند" else "All IOCs copied to clipboard")
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color.White.copy(alpha = 0.08f),
                                                    contentColor = Color.White
                                                ),
                                                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.18f)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(34.dp).springPress()
                                            ) {
                                                Text(text = if (isRtl) "کپی همه" else "Copy All", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                            }

                                            Button(
                                                onClick = {
                                                    val jsonLines = discoveredIocs.joinToString(",\n") {
                                                        """    {"type": "${it.type}", "subtype": "${it.subtype}", "value": "${it.value}", "defanged": "${it.defanged}"}"""
                                                    }
                                                    val json = "[\n$jsonLines\n]"
                                                    clipboardManager.setText(AnnotatedString(json))
                                                    showToast(if (isRtl) "خروجی JSON شاخص‌ها در کلیپ‌بورد کپی شد" else "JSON payload copied to clipboard")
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = PrimaryCyan.copy(alpha = 0.15f),
                                                    contentColor = PrimaryCyan
                                                ),
                                                border = BorderStroke(0.8.dp, PrimaryCyan.copy(alpha = 0.40f)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(34.dp).springPress()
                                            ) {
                                                Text(text = if (isRtl) "خروجی JSON" else "Export JSON", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // Category Filter Pills
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val categories = listOf(
                                            Triple("ALL", if (isRtl) "همه" else "All", discoveredIocs.size),
                                            Triple("IP", "IP", discoveredIocs.count { it.type == "IP" }),
                                            Triple("HASH", if (isRtl) "هش" else "Hash", discoveredIocs.count { it.type == "HASH" }),
                                            Triple("URL_DOM", if (isRtl) "دامنه و لینک" else "URL/Domain", discoveredIocs.count { it.type == "URL" || it.type == "DOMAIN" }),
                                            Triple("EMAIL", if (isRtl) "ایمیل" else "Email", discoveredIocs.count { it.type == "EMAIL" }),
                                            Triple("CVE", "CVE", discoveredIocs.count { it.type == "CVE" })
                                        )

                                        categories.filter { it.third > 0 || it.first == "ALL" }.forEach { (key, label, count) ->
                                            val isSelected = activeFilter == key
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { activeFilter = key }
                                                    .pointerHoverIcon(PointerIcon.Hand),
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) PrimaryCyan.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.04f),
                                                border = BorderStroke(0.7.dp, if (isSelected) PrimaryCyan else Color.White.copy(alpha = 0.12f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = label,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) PrimaryCyan else Color(0xFFCBD5E1)
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = if (isSelected) PrimaryCyan.copy(alpha = 0.25f) else Color(0xFF1E293B)
                                                    ) {
                                                        Text(
                                                            text = LocalizationProvider.formatNumber(count, appLanguage),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                            fontSize = 9.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Filtered IOC Cards
                                    val filtered = discoveredIocs.filter {
                                        when (activeFilter) {
                                            "ALL" -> true
                                            "URL_DOM" -> it.type == "URL" || it.type == "DOMAIN"
                                            else -> it.type == activeFilter
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        filtered.forEach { ioc ->
                                            val badgeColor = when (ioc.type) {
                                                "IP" -> Color(0xFF0284C7)
                                                "HASH" -> Color(0xFF8B5CF6)
                                                "URL" -> Color(0xFFF59E0B)
                                                "DOMAIN" -> Color(0xFF10B981)
                                                "EMAIL" -> Color(0xFFEC4899)
                                                "CVE" -> Color(0xFFEF4444)
                                                else -> PrimaryCyan
                                            }

                                            val links = getThreatIntelLinks(ioc)
                                            val primaryLink = links.firstOrNull()

                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                color = Color(0xFF070D1B),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(0.8.dp, Color(0xFF1E293B))
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = badgeColor.copy(alpha = 0.15f),
                                                                border = BorderStroke(0.6.dp, badgeColor.copy(alpha = 0.40f))
                                                            ) {
                                                                Text(
                                                                    text = ioc.subtype,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                    fontSize = 9.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = badgeColor
                                                                )
                                                            }
                                                            if (ioc.metaTag.isNotBlank()) {
                                                                Surface(
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = Color.White.copy(alpha = 0.05f)
                                                                ) {
                                                                    Text(
                                                                        text = ioc.metaTag,
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                        fontSize = 9.sp,
                                                                        fontFamily = FontFamily.Monospace,
                                                                        color = Color(0xFF94A3B8)
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        // Threat-Intel Quick Actions
                                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Surface(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .clickable {
                                                                        clipboardManager.setText(AnnotatedString(ioc.value))
                                                                        showToast(if (isRtl) "کپی شد: ${ioc.value.take(24)}..." else "Copied: ${ioc.value.take(24)}...")
                                                                    }
                                                                    .pointerHoverIcon(PointerIcon.Hand),
                                                                color = Color.White.copy(alpha = 0.08f),
                                                                border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.15f))
                                                            ) {
                                                                Text(
                                                                    text = if (isRtl) "کپی" else "Copy",
                                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                                    fontSize = 10.sp,
                                                                    color = Color.White
                                                                )
                                                            }

                                                            if (primaryLink != null) {
                                                                Surface(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(6.dp))
                                                                        .clickable {
                                                                            try { uriHandler.openUri(primaryLink.second) } catch (_: Exception) {}
                                                                        }
                                                                        .pointerHoverIcon(PointerIcon.Hand),
                                                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                                                    border = BorderStroke(0.6.dp, Color(0xFF10B981).copy(alpha = 0.40f))
                                                                ) {
                                                                    Text(
                                                                        text = "اسکن (${primaryLink.first})",
                                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color(0xFF10B981)
                                                                    )
                                                                }
                                                            }

                                                            Surface(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .clickable { inspectingIoc = ioc }
                                                                    .pointerHoverIcon(PointerIcon.Hand),
                                                                color = PrimaryCyan.copy(alpha = 0.12f),
                                                                border = BorderStroke(0.6.dp, PrimaryCyan.copy(alpha = 0.35f))
                                                            ) {
                                                                Text(
                                                                    text = "ℹ️",
                                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                                    fontSize = 10.sp
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Text(
                                                        text = ioc.value,
                                                        fontSize = 11.5.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color(0xFFE2E8F0),
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    // One-click Triage in 10 Engines Button
                                                    Button(
                                                        onClick = {
                                                            onPayloadDetected(ioc.value)
                                                            onDismiss()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = PrimaryNeon,
                                                            contentColor = Color.Black
                                                        ),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.fillMaxWidth().height(36.dp).springPress()
                                                    ) {
                                                        Text(
                                                            text = if (isRtl) "⚡ تحلیل و تریاژ در موتورهای ۱۰گانه" else "⚡ Direct 10-Engine Threat Triage",
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Toast Notification Overlay
                AnimatedVisibility(
                    visible = toastMessage != null,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(150)),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xEE0B132B),
                        border = BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.50f)),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                            Text(
                                text = toastMessage ?: "",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Structural Inspection Modal
                inspectingIoc?.let { ioc ->
                    Dialog(onDismissRequest = { inspectingIoc = null }) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .widthIn(max = 500.dp)
                                .clip(RoundedCornerShape(18.dp)),
                            color = Color(0xFF0B132B),
                            border = BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.55f)),
                            shadowElevation = 24.dp
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isRtl) "بررسی ساختار و استعلام امنیتی" else "Structural Inspection & Lookup",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryCyan
                                    )
                                    Surface(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .clickable { inspectingIoc = null },
                                        color = Color.White.copy(alpha = 0.08f),
                                        shape = CircleShape
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(text = "✕", fontSize = 12.sp, color = Color.White)
                                        }
                                    }
                                }

                                Surface(
                                    color = Color(0xFF040812),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(0.8.dp, Color(0xFF1E293B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(text = "مقدار استاندارد (Refanged):", fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                                        Text(text = ioc.value, fontSize = 11.5.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF34D399))
                                    }
                                }

                                Surface(
                                    color = Color(0xFF040812),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(0.8.dp, Color(0xFF1E293B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(text = "فرمت امن (Defanged):", fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                                        Text(text = ioc.defanged, fontSize = 11.5.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFCBD5E1))
                                    }
                                }

                                Text(text = "اسکن مستقیم در منابع هوش تهدیدات:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                val links = getThreatIntelLinks(ioc)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    links.forEach { (name, url) ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    try { uriHandler.openUri(url) } catch (_: Exception) {}
                                                }
                                                .pointerHoverIcon(PointerIcon.Hand),
                                            color = Color.White.copy(alpha = 0.05f),
                                            border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.15f)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = "استعلام در $name", fontSize = 11.sp, color = Color.White)
                                                Text(text = "↗", fontSize = 12.sp, color = PrimaryCyan)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
