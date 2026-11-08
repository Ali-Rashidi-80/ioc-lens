package io.github.mrcoder20.ioclens.presentation.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import io.github.mrcoder20.ioclens.core.localization.AppTheme
import io.github.mrcoder20.ioclens.core.localization.LocalizationProvider
import io.github.mrcoder20.ioclens.core.theme.PrimaryCyan
import io.github.mrcoder20.ioclens.domain.repository.SettingsRepository
import kotlinx.coroutines.delay

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
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val scrollState = rememberScrollState()

    var vtKey by remember(settingsState.virusTotalApiKey) { mutableStateOf(settingsState.virusTotalApiKey) }
    var abuseKey by remember(settingsState.abuseIpDbApiKey) { mutableStateOf(settingsState.abuseIpDbApiKey) }
    var alienKey by remember(settingsState.alienVaultApiKey) { mutableStateOf(settingsState.alienVaultApiKey) }
    var pulseKey by remember(settingsState.pulsediveApiKey) { mutableStateOf(settingsState.pulsediveApiKey) }
    var ipqsKey by remember(settingsState.ipQualityScoreApiKey) { mutableStateOf(settingsState.ipQualityScoreApiKey) }
    var showSavedMessage by remember { mutableStateOf(false) }

    val flushPendingKeys = remember(settingsRepository, vtKey, abuseKey, alienKey, pulseKey, ipqsKey) {
        {
            val s = settingsRepository.getSettings()
            if (vtKey != s.virusTotalApiKey) settingsRepository.updateApiKey("virustotal", vtKey)
            if (abuseKey != s.abuseIpDbApiKey) settingsRepository.updateApiKey("abuseipdb", abuseKey)
            if (alienKey != s.alienVaultApiKey) settingsRepository.updateApiKey("alienvault", alienKey)
            if (pulseKey != s.pulsediveApiKey) settingsRepository.updateApiKey("pulsedive", pulseKey)
            if (ipqsKey != s.ipQualityScoreApiKey) settingsRepository.updateApiKey("ipqualityscore", ipqsKey)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            flushPendingKeys()
        }
    }

    // Automatic debounced persistence for API keys
    LaunchedEffect(vtKey, abuseKey, alienKey, pulseKey, ipqsKey) {
        delay(600)
        flushPendingKeys()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .padding(top = 16.dp, start = 14.dp, end = 14.dp, bottom = if (isMobileOrTablet) 84.dp else 24.dp)
                .verticalScroll(scrollState)
        ) {
            // Header with Right-Aligned Close Button in both LTR & RTL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRtl) {
                    // RTL: Start is Physical Right -> Close button first
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                flushPendingKeys()
                                onClose()
                            }
                            .pointerHoverIcon(PointerIcon.Hand),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "✕",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = "⚙️ ${strings.settingsTitle}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.menuSettings,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // LTR: Start is Left -> Title first, Close button second (Physical Right)
                    Column {
                        Text(
                            text = "⚙️ ${strings.settingsTitle}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.menuSettings,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                flushPendingKeys()
                                onClose()
                            }
                            .pointerHoverIcon(PointerIcon.Hand),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "✕",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Language Section (with Canvas Vector Flags)
            SettingsCard(title = "🌐 ${strings.languageLabel}") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        val isSelected = lang == settingsState.appLanguage
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { settingsRepository.setAppLanguage(lang) },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CountryFlag(language = lang)
                                    Text(
                                        text = lang.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (isSelected) {
                                    Text(
                                        text = "✓",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Theme Section
            SettingsCard(title = "🎨 ${strings.themeLabel}") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val themeOptions = listOf(
                        AppTheme.SYSTEM to Pair("💻", strings.systemDefaultTheme),
                        AppTheme.DARK to Pair("🌙", strings.darkTheme)
                    )

                    themeOptions.forEach { (theme, pair) ->
                        val (icon, label) = pair
                        val isSelected = theme == settingsState.appTheme
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { settingsRepository.setAppTheme(theme) },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
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
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (isSelected) {
                                    Text(
                                        text = "✓",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Security & Toggles Section
            SettingsCard(title = "🛡️ ${strings.menuPrivacyGuard}") {
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
                            onCheckedChange = { settingsRepository.setPrivacyGuardEnabled(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // API Keys Section with Built-in Public Intel Indicator
            SettingsCard(title = "🔑 ${strings.apiKeysTitle}") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = strings.apiKeysSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )

                    OutlinedTextField(
                        value = vtKey,
                        onValueChange = { vtKey = it },
                        label = { Text(strings.vtApiKeyLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = abuseKey,
                        onValueChange = { abuseKey = it },
                        label = { Text(strings.abuseApiKeyLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = alienKey,
                        onValueChange = { alienKey = it },
                        label = { Text(strings.alienApiKeyLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = pulseKey,
                        onValueChange = { pulseKey = it },
                        label = { Text(strings.pulseApiKeyLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = ipqsKey,
                        onValueChange = { ipqsKey = it },
                        label = { Text(strings.ipqsApiKeyLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            settingsRepository.updateApiKey("virustotal", vtKey)
                            settingsRepository.updateApiKey("abuseipdb", abuseKey)
                            settingsRepository.updateApiKey("alienvault", alienKey)
                            settingsRepository.updateApiKey("pulsedive", pulseKey)
                            settingsRepository.updateApiKey("ipqualityscore", ipqsKey)
                            showSavedMessage = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = strings.saveKeys, fontWeight = FontWeight.Bold)
                    }

                    if (showSavedMessage) {
                        Text(
                            text = strings.savedSuccessfully,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About & Version Section
            SettingsCard(title = "ℹ️ ${strings.menuAbout}") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = strings.appTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
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

            Spacer(modifier = Modifier.height(24.dp))
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
        targetValue = if (checked) PrimaryCyan else Color(0xFF94A3B8),
        animationSpec = tween(250)
    )

    val thumbBorderColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF80F5FF) else Color(0xFFCBD5E1),
        animationSpec = tween(250)
    )

    val coreColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF031018) else Color(0xFF1E293B),
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
            Box(
                modifier = Modifier
                    .padding(start = thumbOffset)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(thumbColor)
                    .border(1.dp, thumbBorderColor, CircleShape),
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
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}
