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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mrcoder20.ioclens.core.components.CountryFlag
import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.AppStrings
import io.github.mrcoder20.ioclens.core.localization.AppTheme
import io.github.mrcoder20.ioclens.core.localization.LocalizationProvider
import io.github.mrcoder20.ioclens.core.theme.PrimaryCyan
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.98f))
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .padding(
                    top = 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = if (isMobileOrTablet) 84.dp else 24.dp
                )
                .verticalScroll(scrollState)
        ) {
            // Header with natural BiDi placement (Title at Start, Close at End)
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
                horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                            }
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
 * Top Header ensuring physical right-alignment for Dismiss button (✕) in both LTR & RTL layouts.
 */
@Composable
private fun SettingsHeader(
    strings: AppStrings,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderTitle(strings = strings)
        DismissButton(onClose = onClose)
    }
}

@Composable
private fun HeaderTitle(strings: AppStrings) {
    Column {
        Text(
            text = "⚙️ ${strings.settingsTitle}",
            style = MaterialTheme.typography.headlineMedium,
            color = PrimaryCyan,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = strings.menuSettings,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
 * Language Selector Card with Canvas Vector Flags and Active Indicators.
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppLanguage.entries.forEach { lang ->
                val isSelected = lang == settingsState.appLanguage
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onLanguageSelected(lang) }
                        .pointerHoverIcon(PointerIcon.Hand),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    },
                    border = if (isSelected) {
                        androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryCyan)
                    } else {
                        androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    },
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CountryFlag(language = lang)
                            Column {
                                Text(
                                    text = lang.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (isSelected) {
                            Surface(
                                color = PrimaryCyan.copy(alpha = 0.2f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "✓",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = PrimaryCyan,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
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
 * Theme Selector Card with Cyberpunk Neon Accents.
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            themeOptions.forEach { (theme, pair) ->
                val (icon, label) = pair
                val isSelected = theme == settingsState.appTheme
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onThemeSelected(theme) }
                        .pointerHoverIcon(PointerIcon.Hand),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    },
                    border = if (isSelected) {
                        androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryCyan)
                    } else {
                        androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    },
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
                            Text(text = icon, fontSize = 20.sp)
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (isSelected) {
                            Surface(
                                color = PrimaryCyan.copy(alpha = 0.2f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "✓",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = PrimaryCyan,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
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
 * Privacy Guard Card detailing RFC 1918 Private IP Shielding.
 */
@Composable
private fun PrivacyGuardCard(
    settingsState: AppSettings,
    strings: AppStrings,
    onTogglePrivacyGuard: (Boolean) -> Unit
) {
    SettingsCard(
        title = "🛡️ ${strings.menuPrivacyGuard}",
        badge = if (settingsState.privacyGuardEnabled) "RFC 1918 ACTIVE" else "STANDBY"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = strings.privacyGuardSetting,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
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

            // Cyberpunk Shield Status Banner
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
 * Dedicated Threat Intelligence API Key Manager Card.
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
    onSave: () -> Unit
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
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0D1424),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = strings.apiKeysZeroKeyBanner,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.apiKeysSubtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // 5 Individual Credential Input Fields
            CyberApiKeyField(
                title = strings.vtApiKeyLabel,
                subtitle = strings.vtSubtitle,
                value = vtKey,
                onValueChange = onVtKeyChange,
                strings = strings
            )

            CyberApiKeyField(
                title = strings.abuseApiKeyLabel,
                subtitle = strings.abuseSubtitle,
                value = abuseKey,
                onValueChange = onAbuseKeyChange,
                strings = strings
            )

            CyberApiKeyField(
                title = strings.alienApiKeyLabel,
                subtitle = strings.alienSubtitle,
                value = alienKey,
                onValueChange = onAlienKeyChange,
                strings = strings
            )

            CyberApiKeyField(
                title = strings.pulseApiKeyLabel,
                subtitle = strings.pulseSubtitle,
                value = pulseKey,
                onValueChange = onPulseKeyChange,
                strings = strings
            )

            CyberApiKeyField(
                title = strings.ipqsApiKeyLabel,
                subtitle = strings.ipqsSubtitle,
                value = ipqsKey,
                onValueChange = onIpqsKeyChange,
                strings = strings
            )

            // Save Action Button
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = strings.saveKeys,
                    color = Color(0xFF0A0E17),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }

            if (showSavedMessage) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryCyan.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "✓ ${strings.savedSuccessfully}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = PrimaryCyan,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Cyberpunk Styled Input Field for Threat Intel API Credentials.
 */
@Composable
private fun CyberApiKeyField(
    title: String,
    subtitle: String,
    value: String,
    onValueChange: (String) -> Unit,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val isConfigured = value.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0A0F1D))
            .border(
                1.dp,
                if (isConfigured) PrimaryCyan.copy(alpha = 0.35f) else Color(0xFF1E293B),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }
            Surface(
                color = if (isConfigured) PrimaryCyan.copy(alpha = 0.15f) else Color(0xFF1E293B),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isConfigured) PrimaryCyan.copy(alpha = 0.5f) else Color(0xFF334155)
                )
            ) {
                Text(
                    text = if (isConfigured) strings.keyConfiguredBadge else strings.keyOptionalBadge,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
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
                        style = MaterialTheme.typography.bodyMedium,
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
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = PrimaryCyan,
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedContainerColor = Color(0xFF070B14),
                    unfocusedContainerColor = Color(0xFF070B14),
                    cursorColor = PrimaryCyan
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

/**
 * About & Guide Replay Card.
 */
@Composable
private fun AboutCard(
    strings: AppStrings,
    onReplayGuide: (() -> Unit)?
) {
    SettingsCard(
        title = "ℹ️ ${strings.menuAbout}",
        badge = "KMP"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = strings.appTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryCyan
            )
            Text(
                text = strings.versionLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = strings.appTagline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Cyber Feature Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = Color(0xFF0D1424),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = strings.badgeClientDirect,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = PrimaryCyan
                    )
                }
                Surface(
                    color = Color(0xFF0D1424),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Text(
                        text = strings.badgeZeroCloudDb,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            if (onReplayGuide != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onReplayGuide,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 ${strings.introGuide}",
                        color = PrimaryCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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
