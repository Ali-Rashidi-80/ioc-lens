package io.github.mrcoder20.ioclens.presentation.settings

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.text.font.FontFamily
import io.github.mrcoder20.ioclens.domain.model.EngineProbeStatus
import io.github.mrcoder20.ioclens.domain.model.DEFAULT_PROBE_ENGINES
import io.github.mrcoder20.ioclens.domain.model.probeAllEnginesConcurrently
import io.github.mrcoder20.ioclens.domain.model.probeSingleEngine
import io.github.mrcoder20.ioclens.data.remote.createZeroKeyHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.mrcoder20.ioclens.core.components.BackVectorIcon
import io.github.mrcoder20.ioclens.core.components.CountryFlag
import io.github.mrcoder20.ioclens.core.components.EnginesCpuVectorIcon
import io.github.mrcoder20.ioclens.core.components.springPress
import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.AppStrings
import io.github.mrcoder20.ioclens.core.localization.LocalizationProvider
import io.github.mrcoder20.ioclens.core.theme.BackgroundDark
import io.github.mrcoder20.ioclens.core.theme.CardDark
import io.github.mrcoder20.ioclens.core.theme.CyberAmber
import io.github.mrcoder20.ioclens.core.theme.GlassBorder
import io.github.mrcoder20.ioclens.core.theme.PrimaryCyan
import io.github.mrcoder20.ioclens.core.theme.PrimaryNeon
import io.github.mrcoder20.ioclens.core.theme.SurfaceDark
import io.github.mrcoder20.ioclens.core.theme.topBorderHighlightBrush
import io.github.mrcoder20.ioclens.core.theme.VerdictClean
import io.github.mrcoder20.ioclens.domain.model.AppSettings
import io.github.mrcoder20.ioclens.domain.repository.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isMobileOrTablet: Boolean = false,
    isFullScreen: Boolean = false
) {
    val settingsState by settingsRepository.settingsState.collectAsState()
    val strings = LocalizationProvider.getStrings(settingsState.appLanguage)
    val scrollState = rememberScrollState()
    var showEnginesDialog by remember { mutableStateOf(false) }

    val isRtl = settingsState.appLanguage.isRtl
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection
    ) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            val isCompactLayout = maxWidth < 680.dp || isMobileOrTablet

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
                    .padding(
                        top = 16.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = if (isMobileOrTablet) 84.dp else 24.dp
                    )
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: Back Button + Title (Strict BiDi: Back button on physical right in RTL)
                SettingsHeader(
                    strings = strings,
                    onClose = onClose
                )

                // 1. Threat Intel Engines Card (Zero-Key Engines modal dialog)
                ThreatIntelEnginesCard(
                    strings = strings,
                    isCompact = isCompactLayout,
                    onShowEngines = { showEnginesDialog = true }
                )

                // 2. Language Card
                LanguageCard(
                    settingsState = settingsState,
                    strings = strings,
                    isCompact = isCompactLayout,
                    onLanguageSelected = { settingsRepository.setAppLanguage(it) }
                )

                // 4. GitHub Repository Action Card
                GitHubRepositoryCard(
                    strings = strings,
                    isCompact = isCompactLayout
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Beautiful Modal Dialog for 10 Threat Intelligence Engines
            if (showEnginesDialog) {
                ThreatIntelEnginesDialog(
                    strings = strings,
                    isRtl = isRtl,
                    onDismiss = { showEnginesDialog = false }
                )
            }
        }
    }
}

/**
 * Top Header ensuring standard navigation bar alignment:
 * In RTL (Persian): Back button on the physical right, followed by Title.
 * In LTR (English / Russian): Back button on the physical left, followed by Title.
 * Redundant security console badge and duplicate subtitle are completely eliminated.
 */
@Composable
private fun SettingsHeader(
    strings: AppStrings,
    onClose: () -> Unit
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Modern Sleek Rounded Back Button with Vector Arrow
        Surface(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .springPress()
                .clickable(onClick = onClose)
                .pointerHoverIcon(PointerIcon.Hand),
            color = CardDark.copy(alpha = 0.85f),
            shape = CircleShape,
            border = BorderStroke(1.2.dp, topBorderHighlightBrush(PrimaryNeon, isDark = true)),
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                BackVectorIcon(
                    isRtl = isRtl,
                    tint = PrimaryCyan,
                    size = 18.dp
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title ONLY
        Text(
            text = strings.settingsTitle,
            style = MaterialTheme.typography.headlineMedium,
            color = PrimaryCyan,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Language Selector Card with Canvas Vector Flags and Responsive Layout.
 */
@Composable
private fun LanguageCard(
    settingsState: AppSettings,
    strings: AppStrings,
    isCompact: Boolean = false,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    SettingsCard(
        title = strings.languageLabel,
        badge = settingsState.appLanguage.badgeCode
    ) {
        val languages = AppLanguage.entries.map { it to it.displayName }

        if (isCompact) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                languages.forEach { (lang, label) ->
                    val isSelected = lang == settingsState.appLanguage
                    LanguageTile(
                        lang = lang,
                        label = label,
                        isSelected = isSelected,
                        onClick = { onLanguageSelected(lang) }
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                languages.forEach { (lang, label) ->
                    val isSelected = lang == settingsState.appLanguage
                    Box(modifier = Modifier.weight(1f)) {
                        LanguageTile(
                            lang = lang,
                            label = label,
                            isSelected = isSelected,
                            onClick = { onLanguageSelected(lang) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageTile(
    lang: AppLanguage,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .springPress()
            .clickable { onClick() }
            .pointerHoverIcon(PointerIcon.Hand),
        color = if (isSelected) {
            PrimaryNeon.copy(alpha = 0.18f)
        } else {
            SurfaceDark.copy(alpha = 0.75f)
        },
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) PrimaryNeon else GlassBorder.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CountryFlag(
                    language = lang,
                    modifier = Modifier.size(24.dp, 16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) PrimaryCyan else Color.White
                )
            }
            if (isSelected) {
                Text(
                    text = "✓",
                    color = PrimaryCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * Threat Intelligence Engines Card:
 * Highlights the 10 community zero-key intelligence feeds and provides a trigger
 * to open the rich engines inspection dialog.
 */
@Composable
private fun ThreatIntelEnginesCard(
    strings: AppStrings,
    isCompact: Boolean = false,
    onShowEngines: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(
        title = strings.threatIntelProvidersTitle,
        badge = null,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = strings.apiKeysZeroKeyBanner,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFCBD5E1),
                lineHeight = 22.sp
            )

            Button(
                onClick = onShowEngines,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryCyan,
                    contentColor = Color(0xFF041019)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .springPress()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EnginesCpuVectorIcon(tint = Color(0xFF041019), size = 18.dp)
                    Text(
                        text = strings.systemReadyEngines,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF041019)
                    )
                }
            }
        }
    }
}

/**
 * Beautiful Glassmorphic Modal Dialog displaying all 10 Zero-Key Community Threat Intelligence Engines.
 */
@Composable
internal fun ThreatIntelEnginesDialog(
    strings: AppStrings,
    isRtl: Boolean,
    onDismiss: () -> Unit
) {
    var engineStatuses by remember { mutableStateOf(DEFAULT_PROBE_ENGINES) }
    var isProbing by remember { mutableStateOf(false) }
    var probingEngineIds by remember { mutableStateOf(setOf<String>()) }
    val coroutineScope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    val onlineCount = remember(engineStatuses) { engineStatuses.count { it.isOnline } }
    val totalCount = engineStatuses.size

    fun probeEngines() {
        if (isProbing) return
        isProbing = true
        coroutineScope.launch(Dispatchers.Default) {
            val client = createZeroKeyHttpClient(timeoutMs = 4000L)
            try {
                val updated = probeAllEnginesConcurrently(client, engineStatuses)
                withContext(Dispatchers.Main) {
                    engineStatuses = updated
                    isProbing = false
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    isProbing = false
                }
            } finally {
                client.close()
            }
        }
    }

    fun probeOneEngine(engine: EngineProbeStatus) {
        if (engine.id in probingEngineIds) return
        probingEngineIds = probingEngineIds + engine.id
        coroutineScope.launch(Dispatchers.Default) {
            val client = createZeroKeyHttpClient(timeoutMs = 4000L)
            try {
                val updated = probeSingleEngine(client, engine)
                withContext(Dispatchers.Main) {
                    engineStatuses = engineStatuses.map { if (it.id == engine.id) updated else it }
                    probingEngineIds = probingEngineIds - engine.id
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    probingEngineIds = probingEngineIds - engine.id
                }
            } finally {
                client.close()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 680.dp)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp)),
            color = Color(0xFF070B14),
            border = BorderStroke(1.2.dp, topBorderHighlightBrush(PrimaryNeon, isDark = true)),
            shadowElevation = 28.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Dialog Header: Clean, balanced, non-sloppy design with zero text-wrapping glitches
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header Row 1: Title on Left, Physical Close '✕' on Right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                EnginesCpuVectorIcon(tint = PrimaryCyan, size = 20.dp)
                                Text(
                                    text = strings.threatIntelProvidersTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
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
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Header Row 2: Status Indicator Badge on Left, Ping All Button on Right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (onlineCount == totalCount) PrimaryCyan.copy(alpha = 0.12f) else CyberAmber.copy(alpha = 0.12f),
                                border = BorderStroke(0.8.dp, if (onlineCount == totalCount) PrimaryCyan.copy(alpha = 0.35f) else CyberAmber.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (onlineCount == totalCount) PrimaryCyan else CyberAmber)
                                    )
                                    Text(
                                        text = if (isRtl) "$onlineCount/$totalCount فعال" else "$onlineCount/$totalCount Online",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (onlineCount == totalCount) PrimaryCyan else CyberAmber,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Interactive Probe / Ping All Button
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = !isProbing) { probeEngines() }
                                    .pointerHoverIcon(if (isProbing) PointerIcon.Default else PointerIcon.Hand),
                                color = if (isProbing) PrimaryCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(0.8.dp, PrimaryCyan.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isProbing) "⚡" else "🔄",
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = when {
                                            isProbing && isRtl -> "در حال پینگ..."
                                            isProbing -> "Probing..."
                                            isRtl -> "تست پینگ"
                                            else -> "Ping All"
                                        },
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryCyan,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Engine items list with custom responsive scrollbar pinned to the physical right edge
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = if (scroll.maxValue > 0) 14.dp else 0.dp)
                                .verticalScroll(scroll),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            engineStatuses.forEach { engine ->
                                val dotColor = if (engine.isOnline) Color(0xFF10B981) else Color(0xFFEF4444)
                                CompositionLocalProvider(
                                    LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                                ) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = CardDark.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(0.8.dp, GlassBorder.copy(alpha = 0.35f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = engine.fullName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )

                                                // Live Latency Status Tag with Individual Re-test / Ping capability
                                                val isEngineProbing = engine.id in probingEngineIds
                                                Surface(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .clickable(enabled = !isEngineProbing) {
                                                            probeOneEngine(engine)
                                                        }
                                                        .pointerHoverIcon(if (isEngineProbing) PointerIcon.Default else PointerIcon.Hand),
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isEngineProbing) PrimaryCyan.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f),
                                                    border = BorderStroke(0.7.dp, if (isEngineProbing) PrimaryCyan else dotColor.copy(alpha = 0.35f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                    ) {
                                                        if (isEngineProbing) {
                                                            Text(
                                                                text = "⚡",
                                                                fontSize = 10.sp,
                                                                color = PrimaryCyan
                                                            )
                                                            Text(
                                                                text = if (isRtl) "تست..." else "Ping...",
                                                                fontSize = 10.5.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                fontWeight = FontWeight.Bold,
                                                                color = PrimaryCyan
                                                            )
                                                        } else {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(6.dp)
                                                                    .clip(CircleShape)
                                                                    .background(dotColor)
                                                            )
                                                            Text(
                                                                text = if (engine.isOnline) "${engine.pingMs}ms" else "Offline",
                                                                fontSize = 11.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (engine.isOnline) Color(0xFF38BDF8) else Color(0xFFEF4444)
                                                            )
                                                            Text(
                                                                text = "🔄",
                                                                fontSize = 9.sp,
                                                                color = PrimaryCyan.copy(alpha = 0.8f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${strings.indicatorTypeLabel}:",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 11.sp
                                                )
                                                Surface(
                                                    color = PrimaryCyan.copy(alpha = 0.12f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = engine.indicatorScope,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = PrimaryCyan,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }

                                            Text(
                                                text = if (isRtl) engine.descriptionFa else engine.descriptionEn,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF94A3B8),
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Subtle Minimal Scrollbar
                        if (scroll.maxValue > 0) {
                            BoxWithConstraints(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .width(4.dp)
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .pointerInput(scroll.maxValue) {
                                        detectVerticalDragGestures { change, dragAmount ->
                                            change.consume()
                                            val trackPx = size.height.toFloat()
                                            if (trackPx > 0f) {
                                                val scrollDelta = dragAmount * (scroll.maxValue.toFloat() / trackPx) * 1.5f
                                                coroutineScope.launch {
                                                    scroll.dispatchRawDelta(scrollDelta)
                                                }
                                            }
                                        }
                                    }
                            ) {
                                val trackH = maxHeight
                                val thumbFraction = (maxHeight.value / (maxHeight.value + scroll.maxValue.toFloat())).coerceIn(0.12f, 0.80f)
                                val scrollFraction = if (scroll.maxValue > 0) (scroll.value.toFloat() / scroll.maxValue.toFloat()).coerceIn(0f, 1f) else 0f
                                val thumbOffset = (trackH * (1f - thumbFraction)) * scrollFraction

                                Box(
                                    modifier = Modifier
                                        .offset(y = thumbOffset)
                                        .fillMaxWidth()
                                        .height(trackH * thumbFraction)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(PrimaryCyan.copy(alpha = 0.45f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * GitHub Repository Action Card:
 * Fully responsive on mobile with vertically stacked buttons and fallback copy handling.
 */
@Composable
private fun GitHubRepositoryCard(
    strings: AppStrings,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

    SettingsCard(
        title = "GitHub",
        badge = "MIT LICENSE",
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = strings.githubRepoUrl,
                style = MaterialTheme.typography.labelMedium,
                color = PrimaryCyan,
                fontWeight = FontWeight.SemiBold
            )

            if (isCompact) {
                // Compact / Mobile Layout: Fully stacked responsive buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                uriHandler.openUri(strings.githubRepoUrl)
                            } catch (_: Exception) {
                                clipboardManager.setText(AnnotatedString(strings.githubRepoUrl))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .springPress(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryCyan,
                            contentColor = Color(0xFF070D18)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = strings.openGithubRepo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .springPress()
                            .clickable {
                                clipboardManager.setText(AnnotatedString(strings.githubRepoUrl))
                                isCopied = true
                                coroutineScope.launch {
                                    delay(2000)
                                    isCopied = false
                                }
                            }
                            .pointerHoverIcon(PointerIcon.Hand),
                        color = Color.White.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isCopied) "✓ ${strings.githubUrlCopied}" else strings.copyGithubUrl,
                                color = if (isCopied) VerdictClean else Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                // Wide Desktop Layout: Horizontal Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                uriHandler.openUri(strings.githubRepoUrl)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .springPress(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryCyan,
                            contentColor = Color(0xFF070D18)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = strings.openGithubRepo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .springPress()
                            .clickable {
                                clipboardManager.setText(AnnotatedString(strings.githubRepoUrl))
                                isCopied = true
                                coroutineScope.launch {
                                    delay(2000)
                                    isCopied = false
                                }
                            }
                            .pointerHoverIcon(PointerIcon.Hand),
                        color = Color.White.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isCopied) "✓ ${strings.githubUrlCopied}" else strings.copyGithubUrl,
                                color = if (isCopied) VerdictClean else Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Common Card Container for Settings Screen.
 * Uses Cyberpunk Glassmorphic Surface to match Scanner and History screens.
 */
@Composable
private fun SettingsCard(
    title: String,
    badge: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.88f)),
        border = BorderStroke(1.dp, topBorderHighlightBrush(PrimaryNeon, isDark = true)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryCyan,
                    fontWeight = FontWeight.Bold
                )

                if (badge != null) {
                    Surface(
                        color = PrimaryCyan.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = badge,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            content()
        }
    }
}
