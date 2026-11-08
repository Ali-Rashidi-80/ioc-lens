package io.github.mrcoder20.ioclens.presentation.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mrcoder20.ioclens.core.components.CountryFlag
import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.AppStrings
import io.github.mrcoder20.ioclens.core.localization.AppTheme
import io.github.mrcoder20.ioclens.core.localization.LocalizationProvider
import io.github.mrcoder20.ioclens.core.theme.PrimaryCyan
import io.github.mrcoder20.ioclens.core.theme.VerdictClean
import io.github.mrcoder20.ioclens.domain.model.AppSettings
import io.github.mrcoder20.ioclens.domain.repository.SettingsRepository
import kotlinx.coroutines.delay

enum class SettingsTab {
    API_KEYS,
    GENERAL_PRIVACY,
    ABOUT
}

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isMobileOrTablet: Boolean = false,
    onReplayGuide: (() -> Unit)? = null
) {
    val settingsState by settingsRepository.settingsState.collectAsState()
    val strings = LocalizationProvider.getStrings(settingsState.appLanguage)
    val scrollState = rememberScrollState()
    var currentTab by remember { mutableStateOf(SettingsTab.API_KEYS) }

    var vtKey by remember(settingsState.virusTotalApiKey) { mutableStateOf(settingsState.virusTotalApiKey) }
    var abuseKey by remember(settingsState.abuseIpDbApiKey) { mutableStateOf(settingsState.abuseIpDbApiKey) }
    var alienKey by remember(settingsState.alienVaultApiKey) { mutableStateOf(settingsState.alienVaultApiKey) }
    var pulseKey by remember(settingsState.pulsediveApiKey) { mutableStateOf(settingsState.pulsediveApiKey) }
    var ipqsKey by remember(settingsState.ipQualityScoreApiKey) { mutableStateOf(settingsState.ipQualityScoreApiKey) }
    var showSavedMessage by remember { mutableStateOf(false) }

    val flushPendingKeys = remember(settingsRepository, vtKey, abuseKey, alienKey, pulseKey, ipqsKey) {
        {
            val s = settingsRepository.getSettings()
            val trimmedVt = vtKey.trim()
            val trimmedAbuse = abuseKey.trim()
            val trimmedAlien = alienKey.trim()
            val trimmedPulse = pulseKey.trim()
            val trimmedIpqs = ipqsKey.trim()
            if (trimmedVt != s.virusTotalApiKey) settingsRepository.updateApiKey("virustotal", trimmedVt)
            if (trimmedAbuse != s.abuseIpDbApiKey) settingsRepository.updateApiKey("abuseipdb", trimmedAbuse)
            if (trimmedAlien != s.alienVaultApiKey) settingsRepository.updateApiKey("alienvault", trimmedAlien)
            if (trimmedPulse != s.pulsediveApiKey) settingsRepository.updateApiKey("pulsedive", trimmedPulse)
            if (trimmedIpqs != s.ipQualityScoreApiKey) settingsRepository.updateApiKey("ipqualityscore", trimmedIpqs)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            flushPendingKeys()
        }
    }

    // Automatic debounced persistence for API keys and reset saved indicator on input change
    LaunchedEffect(vtKey, abuseKey, alienKey, pulseKey, ipqsKey) {
        showSavedMessage = false
        delay(600)
        flushPendingKeys()
    }

    // Auto-dismiss confirmation banner after 3 seconds
    LaunchedEffect(showSavedMessage) {
        if (showSavedMessage) {
            delay(3000)
            showSavedMessage = false
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.98f))
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
                .verticalScroll(scrollState)
        ) {
            // Header with strict BiDi compliance: physical right corner grouping in Persian RTL
            SettingsHeader(
                strings = strings,
                onClose = {
                    flushPendingKeys()
                    onClose()
                }
            )

            // Sleek Cyberpunk Segmented Tab Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF070D18))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    SettingsTab.API_KEYS to ("🔑 " + strings.settingsTabApiKeys),
                    SettingsTab.GENERAL_PRIVACY to ("🛡️ " + strings.settingsTabGeneralPrivacy),
                    SettingsTab.ABOUT to ("ℹ️ " + strings.settingsTabAbout)
                ).forEach { (tab, title) ->
                    val isSelected = currentTab == tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { currentTab = tab }
                            .pointerHoverIcon(PointerIcon.Hand),
                        color = if (isSelected) PrimaryCyan.copy(alpha = 0.18f) else Color.Transparent,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.55f)) else null,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PrimaryCyan else Color(0xFF94A3B8),
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated Tab Content Area
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.98f) togetherWith
                        fadeOut(tween(150))
                }
            ) { tab ->
                when (tab) {
                    SettingsTab.API_KEYS -> {
                        ApiKeysCard(
                            vtKey = vtKey,
                            onVtKeyChange = { vtKey = it },
                            abuseKey = abuseKey,
                            onAbuseKeyChange = { abuseKey = it },
                            alienKey = alienKey,
                            onAlienKeyChange = { alienKey = it },
                            pulseKey = pulseKey,
                            onPulseKeyChange = { pulseKey = it },
                            ipqsKey = ipqsKey,
                            onIpqsKeyChange = { ipqsKey = it },
                            strings = strings,
                            showSavedMessage = showSavedMessage,
                            onSave = {
                                flushPendingKeys()
                                showSavedMessage = true
                            },
                            isCompact = isCompactLayout
                        )
                    }
                    SettingsTab.GENERAL_PRIVACY -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            LanguageCard(
                                settingsState = settingsState,
                                strings = strings,
                                onLanguageSelected = { settingsRepository.setAppLanguage(it) }
                            )

                            ThemeCard(
                                settingsState = settingsState,
                                strings = strings,
                                onThemeSelected = { settingsRepository.setAppTheme(it) }
                            )

                            PrivacyGuardCard(
                                settingsState = settingsState,
                                strings = strings,
                                onTogglePrivacyGuard = { settingsRepository.setPrivacyGuardEnabled(it) }
                            )
                        }
                    }
                    SettingsTab.ABOUT -> {
                        AboutCard(
                            strings = strings,
                            onReplayGuide = onReplayGuide
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Top Header ensuring physical right-alignment for Dismiss button (✕) and Settings title in Persian RTL.
 */
@Composable
private fun SettingsHeader(
    strings: AppStrings,
    onClose: () -> Unit
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRtl) {
                // Persian RTL: Anchored on the physical right edge
                Surface(
                    color = PrimaryCyan.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.6.dp, PrimaryCyan.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "SECURITY CONSOLE // SETTINGS",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = PrimaryCyan.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                HeaderTitle(strings = strings, isRtl = true)

                Spacer(modifier = Modifier.width(16.dp))

                DismissButton(onClose = onClose)
            } else {
                // LTR (English / Russian): Title at left, Dismiss button at right
                HeaderTitle(strings = strings, isRtl = false)

                Spacer(modifier = Modifier.weight(1f))

                DismissButton(onClose = onClose)
            }
        }
    }
}

@Composable
private fun HeaderTitle(strings: AppStrings, isRtl: Boolean) {
    Column(horizontalAlignment = if (isRtl) Alignment.End else Alignment.Start) {
        Text(
            text = "⚙️ ${strings.settingsTitle}",
            style = MaterialTheme.typography.headlineMedium,
            color = PrimaryCyan,
            fontWeight = FontWeight.Bold,
            textAlign = if (isRtl) TextAlign.End else TextAlign.Start
        )
        Text(
            text = strings.menuSettings,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = if (isRtl) TextAlign.End else TextAlign.Start
        )
    }
}

@Composable
private fun DismissButton(onClose: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClose)
            .pointerHoverIcon(PointerIcon.Hand),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.35f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "✕",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Language Selector Card with Canvas Vector Flags and 3-Column Bento Row.
 */
@Composable
private fun LanguageCard(
    settingsState: AppSettings,
    strings: AppStrings,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    SettingsCard(
        title = "🌐 ${strings.languageLabel}",
        badge = settingsState.appLanguage.badgeCode
    ) {
        val languages = listOf(
            Pair(AppLanguage.ENGLISH, "English"),
            Pair(AppLanguage.PERSIAN, "فارسی"),
            Pair(AppLanguage.RUSSIAN, "Русский")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            languages.forEach { (lang, label) ->
                val isSelected = lang == settingsState.appLanguage
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onLanguageSelected(lang) }
                        .pointerHoverIcon(PointerIcon.Hand),
                    color = if (isSelected) {
                        PrimaryCyan.copy(alpha = 0.15f)
                    } else {
                        Color(0xFF070D18)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected) PrimaryCyan else Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CountryFlag(language = lang, modifier = Modifier.size(22.dp, 15.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (isSelected) {
                            Text(
                                text = "✓",
                                color = PrimaryCyan,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Theme Selector Card with Cyberpunk Accents in 2-Column Bento Row.
 */
@Composable
private fun ThemeCard(
    settingsState: AppSettings,
    strings: AppStrings,
    onThemeSelected: (AppTheme) -> Unit
) {
    val themeOptions = listOf(
        AppTheme.SYSTEM to Pair("💻", strings.systemDefaultTheme),
        AppTheme.DARK to Pair("🌙", strings.darkTheme)
    )

    SettingsCard(
        title = "🎨 ${strings.themeLabel}",
        badge = if (settingsState.appTheme == AppTheme.DARK) "CYBER DARK" else "AUTO SYSTEM"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            themeOptions.forEach { (theme, pair) ->
                val (icon, label) = pair
                val isSelected = theme == settingsState.appTheme
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onThemeSelected(theme) }
                        .pointerHoverIcon(PointerIcon.Hand),
                    color = if (isSelected) {
                        PrimaryCyan.copy(alpha = 0.15f)
                    } else {
                        Color(0xFF070D18)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected) PrimaryCyan else Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = icon, fontSize = 20.sp)
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (isSelected) {
                            Text(
                                text = "✓",
                                color = PrimaryCyan,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Privacy Guard Settings Card with RFC 1918 Invariant Shield.
 */
@Composable
private fun PrivacyGuardCard(
    settingsState: AppSettings,
    strings: AppStrings,
    onTogglePrivacyGuard: (Boolean) -> Unit
) {
    SettingsCard(
        title = "🛡️ ${strings.menuPrivacyGuard}",
        badge = if (settingsState.privacyGuardEnabled) "RFC 1918 ACTIVE" else "GUARD DISABLED"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = strings.privacyGuardSetting,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = strings.privacyGuardNotice,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CyberSwitch(
                    checked = settingsState.privacyGuardEnabled,
                    onCheckedChange = onTogglePrivacyGuard
                )
            }

            // Cyber Shield Status Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (settingsState.privacyGuardEnabled) {
                    Color(0xFF04202C).copy(alpha = 0.8f)
                } else {
                    Color(0xFF221510).copy(alpha = 0.8f)
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (settingsState.privacyGuardEnabled) PrimaryCyan.copy(alpha = 0.35f) else Color(0xFFF59E0B).copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (settingsState.privacyGuardEnabled) "🔒" else "⚠️",
                        fontSize = 18.sp
                    )
                    Text(
                        text = if (settingsState.privacyGuardEnabled) {
                            strings.privacyGuardActiveDesc
                        } else {
                            strings.privacyGuardInactiveDesc
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (settingsState.privacyGuardEnabled) Color(0xFF94A3B8) else Color(0xFFFBBF24),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Threat Intelligence API Key Manager Card in Balanced 3x2 Grid.
 */
@Composable
private fun ApiKeysCard(
    vtKey: String,
    onVtKeyChange: (String) -> Unit,
    abuseKey: String,
    onAbuseKeyChange: (String) -> Unit,
    alienKey: String,
    onAlienKeyChange: (String) -> Unit,
    pulseKey: String,
    onPulseKeyChange: (String) -> Unit,
    ipqsKey: String,
    onIpqsKeyChange: (String) -> Unit,
    strings: AppStrings,
    showSavedMessage: Boolean,
    onSave: () -> Unit,
    isCompact: Boolean = false
) {
    val configuredCount = listOf(vtKey, abuseKey, alienKey, pulseKey, ipqsKey).count { it.isNotBlank() }

    SettingsCard(
        title = "🔑 ${strings.apiKeysTitle}",
        badge = "$configuredCount/5"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Explanatory Information Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF090E17).copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "⚡", fontSize = 22.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.apiKeysZeroKeyBanner,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryCyan
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = strings.apiKeysSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            if (isCompact) {
                // Fluid Single-Column Layout for Mobile Screens
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CyberApiKeyField(title = strings.vtApiKeyLabel, subtitle = strings.vtSubtitle, value = vtKey, onValueChange = onVtKeyChange, strings = strings)
                    CyberApiKeyField(title = strings.abuseApiKeyLabel, subtitle = strings.abuseSubtitle, value = abuseKey, onValueChange = onAbuseKeyChange, strings = strings)
                    CyberApiKeyField(title = strings.alienApiKeyLabel, subtitle = strings.alienSubtitle, value = alienKey, onValueChange = onAlienKeyChange, strings = strings)
                    CyberApiKeyField(title = strings.pulseApiKeyLabel, subtitle = strings.pulseSubtitle, value = pulseKey, onValueChange = onPulseKeyChange, strings = strings)
                    CyberApiKeyField(title = strings.ipqsApiKeyLabel, subtitle = strings.ipqsSubtitle, value = ipqsKey, onValueChange = onIpqsKeyChange, strings = strings)
                    SaveActionHubCard(configuredCount = configuredCount, strings = strings, showSavedMessage = showSavedMessage, onSave = onSave)
                }
            } else {
                // Responsive 3x2 Grid for Desktop & Tablets (Prevents stretched, empty single-row aesthetics)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Row 1: VirusTotal & AbuseIPDB
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            CyberApiKeyField(
                                title = strings.vtApiKeyLabel,
                                subtitle = strings.vtSubtitle,
                                value = vtKey,
                                onValueChange = onVtKeyChange,
                                strings = strings
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CyberApiKeyField(
                                title = strings.abuseApiKeyLabel,
                                subtitle = strings.abuseSubtitle,
                                value = abuseKey,
                                onValueChange = onAbuseKeyChange,
                                strings = strings
                            )
                        }
                    }

                    // Row 2: AlienVault OTX & Pulsedive
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            CyberApiKeyField(
                                title = strings.alienApiKeyLabel,
                                subtitle = strings.alienSubtitle,
                                value = alienKey,
                                onValueChange = onAlienKeyChange,
                                strings = strings
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CyberApiKeyField(
                                title = strings.pulseApiKeyLabel,
                                subtitle = strings.pulseSubtitle,
                                value = pulseKey,
                                onValueChange = onPulseKeyChange,
                                strings = strings
                            )
                        }
                    }

                    // Row 3: IPQualityScore & Save Action Hub
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            CyberApiKeyField(
                                title = strings.ipqsApiKeyLabel,
                                subtitle = strings.ipqsSubtitle,
                                value = ipqsKey,
                                onValueChange = onIpqsKeyChange,
                                strings = strings
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            SaveActionHubCard(
                                configuredCount = configuredCount,
                                strings = strings,
                                showSavedMessage = showSavedMessage,
                                onSave = onSave
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Action Hub Card combining Active Key Counter, Quick Save Action, and Confirmation Banner.
 */
@Composable
private fun SaveActionHubCard(
    configuredCount: Int,
    strings: AppStrings,
    showSavedMessage: Boolean,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(112.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF070E1A),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (configuredCount > 0) VerdictClean else PrimaryCyan))
                    Text(
                        text = if (configuredCount > 0) "$configuredCount/5 ${strings.keyConfiguredBadge}" else strings.zeroKeyInstantScanReady,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (showSavedMessage) {
                    Surface(
                        color = PrimaryCyan.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, PrimaryCyan)
                    ) {
                        Text(
                            text = "✓ ${strings.savedSuccessfully}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = PrimaryCyan,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "💾", fontSize = 16.sp)
                    Text(
                        text = strings.saveKeys,
                        color = Color(0xFF0A0E17),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp
                    )
                }
            }
        }
    }
}

/**
 * Cyberpunk Styled Input Field for Threat Intel API Credentials (Uniform 112dp Grid Cell).
 */
@Composable
private fun CyberApiKeyField(
    title: String,
    subtitle: String,
    value: String,
    onValueChange: (String) -> Unit,
    strings: AppStrings
) {
    val isConfigured = value.isNotBlank()

    Surface(
        modifier = Modifier.fillMaxWidth().height(112.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF070D18),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isConfigured) PrimaryCyan.copy(alpha = 0.5f) else Color(0xFF1E293B)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isConfigured) PrimaryCyan else Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.5.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Surface(
                    color = if (isConfigured) PrimaryCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        0.6.dp,
                        if (isConfigured) PrimaryCyan.copy(alpha = 0.4f) else Color(0xFF334155)
                    )
                ) {
                    Text(
                        text = if (isConfigured) strings.keyConfiguredBadge else strings.keyOptionalBadge,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isConfigured) PrimaryCyan else Color(0xFF94A3B8)
                    )
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = {
                        Text(
                            text = strings.keyInputPlaceholder,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                    },
                    trailingIcon = {
                        if (value.isNotEmpty()) {
                            Text(
                                text = "✕",
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onValueChange("") }
                                    .padding(6.dp)
                                    .pointerHoverIcon(PointerIcon.Hand),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryCyan,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedContainerColor = Color(0xFF030710),
                        unfocusedContainerColor = Color(0xFF030710),
                        cursorColor = PrimaryCyan
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}

/**
 * About Hub: Comprehensive Architectural Presentation for Defenders (Zero-Jargon, 4-Pillar Grid).
 */
@Composable
private fun AboutCard(
    strings: AppStrings,
    onReplayGuide: (() -> Unit)?
) {
    SettingsCard(
        title = "ℹ️ ${strings.menuAbout}",
        badge = "OPEN SOURCE v1.0"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Hero Presentation Hub
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF070E1A),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(54.dp),
                        shape = CircleShape,
                        color = PrimaryCyan.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryCyan)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "🛡️", fontSize = 26.sp)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "IOC LENS",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Surface(
                                color = PrimaryCyan.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(0.6.dp, PrimaryCyan)
                            ) {
                                Text(
                                    text = strings.versionLabel,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryCyan
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.appTagline,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // 4 Architectural Security Pillars in 2x2 Grid
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Row 1: Zero Cloud DB & Client-Direct Engine
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ArchitecturePillarCard(
                            icon = "🔒",
                            title = strings.badgeZeroCloudDb,
                            description = "هیچ پایگاه داده ابری یا سرور میانی وجود ندارد؛ توکن‌ها و سوابق تنها در حافظه رمزنگاری‌شده محلی ذخیره می‌شوند."
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ArchitecturePillarCard(
                            icon = "⚡",
                            title = strings.badgeClientDirect,
                            description = "استعلام‌های تهدید مستقیماً از سیستم شما به APIهای رسمی ارسال می‌شوند بدون واسطه (۱۰۰٪ کلاینت-ساید)."
                        )
                    }
                }

                // Row 2: RFC 1918 Guard & 10 Threat Engines
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ArchitecturePillarCard(
                            icon = "🛡️",
                            title = "سپر حریم خصوصی RFC 1918",
                            description = "تشخیص و مسدودسازی بلادرنگ آی‌پی‌های محلی و خصوصی (10.0.0.0/8 و 192.168.0.0/16) جهت جلوگیری از نشت ترافیک."
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ArchitecturePillarCard(
                            icon = "🌐",
                            title = strings.threatIntelProvidersTitle,
                            description = "تلفیق بلادرنگ ۵ فید باز رایگان و ۵ موتور تخصصی برای تحلیل بدافزار، فیشینگ، دی‌ان‌اس و شهرت آی‌پی."
                        )
                    }
                }
            }

            // Technical Architecture Specification Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Standalone Portable",
                    "Zero Telemetry",
                    "Apache 2.0 Open Source",
                    "Multiplatform Engine"
                ).forEach { spec ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF070D18),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF1E293B))
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = spec,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            if (onReplayGuide != null) {
                Button(
                    onClick = onReplayGuide,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B192C)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "✨", fontSize = 16.sp)
                        Text(
                            text = "مرور راهنمای تعاملی و آموزش برنامه (Replay Interactive Tour)",
                            color = PrimaryCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchitecturePillarCard(
    icon: String,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(108.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF070D18),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = icon, fontSize = 16.sp)
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryCyan,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                lineHeight = 15.sp
            )
        }
    }
}

/**
 * Cyberpunk Neon Switch with High-Contrast Track and Distinct Core Diode.
 * Guaranteed visible contrast in both ON and OFF states with RTL-independent LTR orientation.
 */
@Composable
fun CyberSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 26.dp else 4.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

    val trackBorderColor by animateColorAsState(
        targetValue = if (checked) PrimaryCyan else Color(0xFF334155),
        animationSpec = tween(250)
    )

    val trackBgColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF04202C) else Color(0xFF0D1424),
        animationSpec = tween(250)
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked) Color.White else Color(0xFF94A3B8),
        animationSpec = tween(250)
    )

    val thumbBorderColor by animateColorAsState(
        targetValue = if (checked) PrimaryCyan else Color(0xFFCBD5E1),
        animationSpec = tween(250)
    )

    val coreColor by animateColorAsState(
        targetValue = if (checked) PrimaryCyan else Color(0xFF1E293B),
        animationSpec = tween(250)
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .width(52.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(50))
                .background(trackBgColor)
                .border(1.5.dp, trackBorderColor, RoundedCornerShape(50))
                .clickable(role = Role.Switch) { onCheckedChange(!checked) }
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(vertical = 3.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Track background status indicator symbol
            if (checked) {
                Text(
                    text = "✓",
                    color = PrimaryCyan.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 9.dp).align(Alignment.CenterStart)
                )
            } else {
                Text(
                    text = "○",
                    color = Color(0xFF475569),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 9.dp).align(Alignment.CenterEnd)
                )
            }

            Box(
                modifier = Modifier
                    .padding(start = thumbOffset)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(thumbColor)
                    .border(1.5.dp, thumbBorderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(coreColor)
                )
            }
        }
    }
}

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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1726).copy(alpha = 0.95f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryCyan
                )
                if (badge != null) {
                    Surface(
                        color = PrimaryCyan.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = badge,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryCyan
                        )
                    }
                }
            }
            content()
        }
    }
}
