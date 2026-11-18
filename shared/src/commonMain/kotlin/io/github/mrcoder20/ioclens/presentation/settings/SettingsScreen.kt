package io.github.mrcoder20.ioclens.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.mrcoder20.ioclens.core.components.BackVectorIcon
import io.github.mrcoder20.ioclens.core.components.CountryFlag
import io.github.mrcoder20.ioclens.core.components.EnginesCpuVectorIcon
import io.github.mrcoder20.ioclens.core.components.rememberClipboardActions
import io.github.mrcoder20.ioclens.core.components.springPress
import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.AppStrings
import io.github.mrcoder20.ioclens.core.localization.AppTheme
import io.github.mrcoder20.ioclens.core.localization.LocalizationProvider
import io.github.mrcoder20.ioclens.core.theme.LocalIocColors
import io.github.mrcoder20.ioclens.core.theme.rememberMonospaceFontFamily
import io.github.mrcoder20.ioclens.core.theme.topBorderHighlightBrush
import io.github.mrcoder20.ioclens.data.remote.createZeroKeyHttpClient
import io.github.mrcoder20.ioclens.domain.model.AppSettings
import io.github.mrcoder20.ioclens.domain.model.DEFAULT_PROBE_ENGINES
import io.github.mrcoder20.ioclens.domain.model.EngineProbeStatus
import io.github.mrcoder20.ioclens.domain.model.probeAllEnginesConcurrently
import io.github.mrcoder20.ioclens.domain.model.probeSingleEngine
import io.github.mrcoder20.ioclens.domain.parser.PrivacyGuardValidator
import io.github.mrcoder20.ioclens.domain.repository.SettingsRepository
import io.github.mrcoder20.ioclens.logDiagnostic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isMobileOrTablet: Boolean = false,
    isFullScreen: Boolean = false,
    onReplayGuide: () -> Unit = {}
) {
    val settingsState by settingsRepository.settingsState.collectAsState()
    val strings = LocalizationProvider.getStrings(settingsState.appLanguage)
    val palette = LocalIocColors.current
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
                .background(if (isFullScreen) Color.Transparent else palette.backgroundCanvas.copy(alpha = 0.96f))
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
                SettingsHeader(
                    strings = strings,
                    onClose = onClose
                )

                // 1. Theme Selector Card (System / Daylight / Cyber Dark / IOC Classic)
                ThemeSelectorCard(
                    settingsState = settingsState,
                    strings = strings,
                    isCompact = isCompactLayout,
                    onThemeSelected = { settingsRepository.setAppTheme(it) }
                )

                // 2. Language Card
                LanguageCard(
                    settingsState = settingsState,
                    strings = strings,
                    isCompact = isCompactLayout,
                    onLanguageSelected = { settingsRepository.setAppLanguage(it) }
                )

                // 3. Privacy Guard & Custom Domain/CIDR Rules
                PrivacyGuardCard(
                    settingsState = settingsState,
                    strings = strings,
                    onTogglePrivacyGuard = { settingsRepository.setPrivacyGuardEnabled(it) },
                    onAddRule = { settingsRepository.addCustomPrivacyRule(it) },
                    onRemoveRule = { settingsRepository.removeCustomPrivacyRule(it) }
                )

                // 4. Threat Intel Engines Card (Zero-Key Engines & Per-Provider Toggle Dialog)
                ThreatIntelEnginesCard(
                    settingsState = settingsState,
                    strings = strings,
                    onShowEngines = { showEnginesDialog = true }
                )

                // 5. Network Timeout & History Retention Limits
                NetworkAndRetentionCard(
                    settingsState = settingsState,
                    strings = strings,
                    onTimeoutSelected = { settingsRepository.setRequestTimeoutMs(it) },
                    onHistoryLimitSelected = { settingsRepository.setMaxHistoryEntries(it) }
                )

                // 6. Optional Encrypted API Keys Card
                OptionalApiKeysCard(
                    settingsState = settingsState,
                    strings = strings,
                    onSaveKey = { provider, key -> settingsRepository.updateApiKey(provider, key) }
                )

                // 7. Interactive Tour Replay Card
                ReplayGuideCard(
                    strings = strings,
                    onReplayGuide = {
                        onClose()
                        onReplayGuide()
                    }
                )

                // 8. GitHub Repository Card
                GitHubRepositoryCard(
                    strings = strings,
                    isCompact = isCompactLayout
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            if (showEnginesDialog) {
                ThreatIntelEnginesDialog(
                    strings = strings,
                    isRtl = isRtl,
                    appLanguage = settingsState.appLanguage,
                    settingsRepository = settingsRepository,
                    onDismiss = { showEnginesDialog = false }
                )
            }
        }
    }
}

@Composable
private fun SettingsHeader(
    strings: AppStrings,
    onClose: () -> Unit
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val palette = LocalIocColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .springPress()
                .clickable(onClick = onClose)
                .pointerHoverIcon(PointerIcon.Hand),
            color = palette.surfaceCard.copy(alpha = 0.92f),
            shape = CircleShape,
            border = BorderStroke(1.2.dp, topBorderHighlightBrush(palette.accentPrimary, isDark = palette.isDark)),
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                BackVectorIcon(
                    isRtl = isRtl,
                    tint = palette.accentPrimary,
                    size = 18.dp
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = strings.settingsTitle,
            style = MaterialTheme.typography.headlineMedium,
            color = palette.accentPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ThemeSelectorCard(
    settingsState: AppSettings,
    strings: AppStrings,
    isCompact: Boolean,
    onThemeSelected: (AppTheme) -> Unit
) {
    val palette = LocalIocColors.current
    val options = listOf(
        AppTheme.SYSTEM to strings.systemDefaultTheme,
        AppTheme.LIGHT to strings.lightTheme,
        AppTheme.DARK to strings.darkTheme,
        AppTheme.IOC_LENS_CLASSIC to strings.iocLensClassicTheme
    )
    val badge = when (settingsState.appTheme) {
        AppTheme.SYSTEM -> strings.badgeThemeAutoSystem
        AppTheme.LIGHT -> strings.lightTheme
        AppTheme.DARK -> strings.badgeThemeCyberDark
        AppTheme.IOC_LENS_CLASSIC -> strings.badgeThemeIocClassic
    }

    SettingsCard(
        title = strings.themeLabel,
        badge = badge
    ) {
        val rows = if (isCompact) options.chunked(2) else options.chunked(4)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { (theme, label) ->
                        val isSelected = settingsState.appTheme == theme
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .springPress()
                                .clickable { onThemeSelected(theme) }
                                .pointerHoverIcon(PointerIcon.Hand),
                            color = if (isSelected) palette.accentPrimary.copy(alpha = 0.16f) else palette.surfaceInset,
                            border = BorderStroke(
                                if (isSelected) 1.4.dp else 1.dp,
                                if (isSelected) palette.accentPrimary else palette.borderSubtle
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) palette.accentPrimary else palette.textPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("✓", color = palette.accentPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

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
                    LanguageTile(
                        lang = lang,
                        label = label,
                        isSelected = lang == settingsState.appLanguage,
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
                    Box(modifier = Modifier.weight(1f)) {
                        LanguageTile(
                            lang = lang,
                            label = label,
                            isSelected = lang == settingsState.appLanguage,
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
    val palette = LocalIocColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .springPress()
            .clickable { onClick() }
            .pointerHoverIcon(PointerIcon.Hand),
        color = if (isSelected) {
            palette.accentPrimary.copy(alpha = 0.16f)
        } else {
            palette.surfaceInset
        },
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) palette.accentPrimary else palette.borderSubtle
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
                    color = if (isSelected) palette.accentPrimary else palette.textPrimary
                )
            }
            if (isSelected) {
                Text(
                    text = "✓",
                    color = palette.accentPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrivacyGuardCard(
    settingsState: AppSettings,
    strings: AppStrings,
    onTogglePrivacyGuard: (Boolean) -> Unit,
    onAddRule: (String) -> Boolean,
    onRemoveRule: (String) -> Unit
) {
    val palette = LocalIocColors.current
    val monoFont = rememberMonospaceFontFamily()
    var ruleInput by remember { mutableStateOf("") }
    var ruleError by remember { mutableStateOf<String?>(null) }

    fun submitRule() {
        val candidate = ruleInput.trim()
        if (!PrivacyGuardValidator.isValidCustomRule(candidate)) {
            ruleError = strings.invalidRuleError
            return
        }
        val added = onAddRule(candidate)
        if (added) {
            ruleInput = ""
            ruleError = null
        } else {
            ruleError = strings.invalidRuleError
        }
    }

    SettingsCard(
        title = strings.cardPrivacyGuardTitle,
        badge = if (settingsState.privacyGuardEnabled) strings.badgeGuardActive else strings.badgeGuardDisabled
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
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (settingsState.privacyGuardEnabled) strings.privacyGuardActiveDesc else strings.privacyGuardInactiveDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        lineHeight = 18.sp
                    )
                }
                Switch(
                    checked = settingsState.privacyGuardEnabled,
                    onCheckedChange = onTogglePrivacyGuard,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = palette.onAccentPrimary,
                        checkedTrackColor = palette.verdictClean
                    )
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = strings.customPrivacyRulesTitle,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.accentPrimary
                )
                Text(
                    text = strings.customPrivacyRulesSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = ruleInput,
                        onValueChange = {
                            ruleInput = it
                            ruleError = null
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = monoFont,
                            color = palette.textPrimary
                        ),
                        placeholder = {
                            Text(
                                text = strings.customPrivacyRulesPlaceholder,
                                color = palette.textMuted,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitRule() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = palette.textPrimary,
                            unfocusedTextColor = palette.textPrimary,
                            focusedBorderColor = palette.accentPrimary,
                            unfocusedBorderColor = palette.borderStrong
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = { submitRule() },
                        enabled = ruleInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accentPrimary,
                            contentColor = palette.onAccentPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = strings.addRuleButton,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                if (!ruleError.isNullOrBlank()) {
                    Text(
                        text = ruleError.orEmpty(),
                        color = palette.verdictMalicious,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                if (settingsState.customPrivacyRules.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        settingsState.customPrivacyRules.forEach { rule ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.surfaceInset,
                                border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = rule,
                                        fontFamily = monoFont,
                                        color = palette.textPrimary,
                                        fontSize = 11.5.sp
                                    )
                                    Text(
                                        text = "✕",
                                        color = palette.verdictMalicious,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { onRemoveRule(rule) }
                                            .pointerHoverIcon(PointerIcon.Hand)
                                            .padding(2.dp)
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

@Composable
private fun ThreatIntelEnginesCard(
    settingsState: AppSettings,
    strings: AppStrings,
    onShowEngines: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalIocColors.current
    val activeZeroKey = (DEFAULT_PROBE_ENGINES.size - settingsState.disabledProviders.size).coerceAtLeast(0)
    SettingsCard(
        title = strings.threatIntelProvidersTitle,
        badge = "${LocalizationProvider.formatNumber(activeZeroKey, settingsState.appLanguage)}/${LocalizationProvider.formatNumber(DEFAULT_PROBE_ENGINES.size, settingsState.appLanguage)}",
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = strings.apiKeysZeroKeyBanner,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
                lineHeight = 22.sp
            )

            Button(
                onClick = onShowEngines,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accentPrimary,
                    contentColor = palette.onAccentPrimary
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
                    EnginesCpuVectorIcon(tint = palette.onAccentPrimary, size = 18.dp)
                    Text(
                        text = strings.systemReadyEngines,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = palette.onAccentPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkAndRetentionCard(
    settingsState: AppSettings,
    strings: AppStrings,
    onTimeoutSelected: (Long) -> Unit,
    onHistoryLimitSelected: (Int) -> Unit
) {
    val palette = LocalIocColors.current
    val timeoutOptions = listOf(5_000L, 10_000L, 15_000L, 30_000L)
    val historyLimits = listOf(20, 30, 50, 100)

    SettingsCard(
        title = strings.settingsTabGeneralPrivacy,
        badge = "${settingsState.requestTimeoutMs / 1000}s / ${settingsState.maxHistoryEntries}"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = strings.requestTimeoutTitle,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                timeoutOptions.forEach { ms ->
                    val isSelected = settingsState.requestTimeoutMs == ms
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onTimeoutSelected(ms) }
                            .pointerHoverIcon(PointerIcon.Hand),
                        color = if (isSelected) palette.accentPrimary.copy(alpha = 0.16f) else palette.surfaceInset,
                        border = BorderStroke(1.dp, if (isSelected) palette.accentPrimary else palette.borderSubtle),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "${LocalizationProvider.formatNumber((ms / 1000).toInt(), settingsState.appLanguage)}s",
                                color = if (isSelected) palette.accentPrimary else palette.textPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Text(
                text = strings.historyLimitTitle,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                historyLimits.forEach { limit ->
                    val isSelected = settingsState.maxHistoryEntries == limit
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onHistoryLimitSelected(limit) }
                            .pointerHoverIcon(PointerIcon.Hand),
                        color = if (isSelected) palette.accentPrimary.copy(alpha = 0.16f) else palette.surfaceInset,
                        border = BorderStroke(1.dp, if (isSelected) palette.accentPrimary else palette.borderSubtle),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = LocalizationProvider.formatNumber(limit, settingsState.appLanguage),
                                color = if (isSelected) palette.accentPrimary else palette.textPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionalApiKeysCard(
    settingsState: AppSettings,
    strings: AppStrings,
    onSaveKey: (provider: String, key: String) -> Unit
) {
    val palette = LocalIocColors.current
    val monoFont = rememberMonospaceFontFamily()
    var isExpanded by remember { mutableStateOf(false) }
    var savedBanner by remember { mutableStateOf(false) }

    var vtKey by remember(settingsState.virusTotalApiKey) { mutableStateOf(settingsState.virusTotalApiKey) }
    var abuseKey by remember(settingsState.abuseIpDbApiKey) { mutableStateOf(settingsState.abuseIpDbApiKey) }
    var alienKey by remember(settingsState.alienVaultApiKey) { mutableStateOf(settingsState.alienVaultApiKey) }
    var pulseKey by remember(settingsState.pulsediveApiKey) { mutableStateOf(settingsState.pulsediveApiKey) }
    var ipqsKey by remember(settingsState.ipQualityScoreApiKey) { mutableStateOf(settingsState.ipQualityScoreApiKey) }
    var showKeys by remember { mutableStateOf(false) }

    val configuredCount = listOf(
        settingsState.virusTotalApiKey,
        settingsState.abuseIpDbApiKey,
        settingsState.alienVaultApiKey,
        settingsState.pulsediveApiKey,
        settingsState.ipQualityScoreApiKey
    ).count { it.isNotBlank() }

    SettingsCard(
        title = strings.apiKeysTitle,
        badge = if (configuredCount > 0) "${strings.keyConfiguredBadge} ($configuredCount)" else strings.keyOptionalBadge
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .pointerHoverIcon(PointerIcon.Hand)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.apiKeysSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Text(
                    text = if (isExpanded) "▲" else "▼",
                    color = palette.accentPrimary,
                    fontSize = 12.sp
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val fields = listOf(
                        Triple("virustotal", strings.vtApiKeyLabel, Pair(vtKey, { v: String -> vtKey = v })),
                        Triple("abuseipdb", strings.abuseApiKeyLabel, Pair(abuseKey, { v: String -> abuseKey = v })),
                        Triple("alienvault", strings.alienApiKeyLabel, Pair(alienKey, { v: String -> alienKey = v })),
                        Triple("pulsedive", strings.pulseApiKeyLabel, Pair(pulseKey, { v: String -> pulseKey = v })),
                        Triple("ipqualityscore", strings.ipqsApiKeyLabel, Pair(ipqsKey, { v: String -> ipqsKey = v }))
                    )

                    fields.forEach { (_, label, statePair) ->
                        val (value, onValue) = statePair
                        OutlinedTextField(
                            value = value,
                            onValueChange = { onValue(it) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(label, fontSize = 11.5.sp) },
                            placeholder = { Text(strings.keyInputPlaceholder, fontSize = 11.5.sp, color = palette.textMuted) },
                            visualTransformation = if (showKeys) VisualTransformation.None else PasswordVisualTransformation(),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont, color = palette.textPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = palette.textPrimary,
                                unfocusedTextColor = palette.textPrimary,
                                focusedBorderColor = palette.accentPrimary,
                                unfocusedBorderColor = palette.borderStrong
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showKeys = !showKeys }) {
                            Text(
                                text = if (showKeys) "🙈" else "👁",
                                fontSize = 14.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    vtKey = ""
                                    abuseKey = ""
                                    alienKey = ""
                                    pulseKey = ""
                                    ipqsKey = ""
                                    listOf("virustotal", "abuseipdb", "alienvault", "pulsedive", "ipqualityscore").forEach {
                                        onSaveKey(it, "")
                                    }
                                    savedBanner = true
                                }
                            ) {
                                Text(strings.clear, color = palette.verdictMalicious, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    onSaveKey("virustotal", vtKey)
                                    onSaveKey("abuseipdb", abuseKey)
                                    onSaveKey("alienvault", alienKey)
                                    onSaveKey("pulsedive", pulseKey)
                                    onSaveKey("ipqualityscore", ipqsKey)
                                    savedBanner = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.accentPrimary,
                                    contentColor = palette.onAccentPrimary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(strings.saveKeys, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    if (savedBanner) {
                        Text(
                            text = "✓ ${strings.savedSuccessfully}",
                            color = palette.verdictClean,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplayGuideCard(
    strings: AppStrings,
    onReplayGuide: () -> Unit
) {
    val palette = LocalIocColors.current
    SettingsCard(
        title = strings.replayGuideTitle,
        badge = strings.versionLabel
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = strings.clientManifestoDesc,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                lineHeight = 19.sp
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .springPress()
                    .clickable(onClick = onReplayGuide)
                    .pointerHoverIcon(PointerIcon.Hand),
                color = palette.accentPrimary.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 11.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.replayInteractiveTour,
                        color = palette.accentPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * Modal dialog displaying all Zero-Key Community Threat Intelligence Engines,
 * real probe results (honest unprobed initial state), and per-provider enable/disable switches.
 */
@Composable
internal fun ThreatIntelEnginesDialog(
    strings: AppStrings,
    isRtl: Boolean,
    appLanguage: AppLanguage = AppLanguage.ENGLISH,
    settingsRepository: SettingsRepository? = null,
    onDismiss: () -> Unit
) {
    val palette = LocalIocColors.current
    val monoFont = rememberMonospaceFontFamily()
    val settingsSnapshot by (settingsRepository?.settingsState ?: remember {
        kotlinx.coroutines.flow.MutableStateFlow(AppSettings())
    }).collectAsState()

    var engineStatuses by remember { mutableStateOf(DEFAULT_PROBE_ENGINES) }
    var isProbing by remember { mutableStateOf(false) }
    var probingEngineIds by remember { mutableStateOf(setOf<String>()) }
    val coroutineScope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    val anyProbed = remember(engineStatuses) { engineStatuses.any { it.hasProbed } }
    val onlineCount = remember(engineStatuses) { engineStatuses.count { it.hasProbed && it.isOnline } }
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logDiagnostic("EngineProbe", "Settings batch probe failed", e)
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logDiagnostic("EngineProbe", "Single engine probe failed for ${engine.id}", e)
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
            color = palette.backgroundCanvas,
            border = BorderStroke(1.2.dp, topBorderHighlightBrush(palette.accentPrimary, isDark = palette.isDark)),
            shadowElevation = 28.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(modifier = Modifier.fillMaxWidth()) {
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
                                EnginesCpuVectorIcon(tint = palette.accentPrimary, size = 20.dp)
                                Text(
                                    text = strings.threatIntelProvidersTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accentPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .springPress()
                                    .clickable(onClick = onDismiss)
                                    .pointerHoverIcon(PointerIcon.Hand),
                                color = palette.surfaceInset,
                                border = BorderStroke(1.dp, palette.borderStrong),
                                shape = CircleShape
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "✕",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val badgeColor = when {
                                !anyProbed -> palette.textMuted
                                onlineCount == totalCount -> palette.accentPrimary
                                else -> palette.verdictLowRisk
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = badgeColor.copy(alpha = 0.14f),
                                border = BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.38f))
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
                                            .background(badgeColor)
                                    )
                                    Text(
                                        text = when {
                                            !anyProbed -> "${LocalizationProvider.formatNumber(totalCount, appLanguage)} • ${strings.probeUnchecked}"
                                            isRtl -> "${LocalizationProvider.formatNumber(onlineCount, appLanguage)}/${LocalizationProvider.formatNumber(totalCount, appLanguage)} فعال"
                                            else -> "$onlineCount/$totalCount Online"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = badgeColor,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = !isProbing) { probeEngines() }
                                    .pointerHoverIcon(if (isProbing) PointerIcon.Default else PointerIcon.Hand),
                                color = if (isProbing) palette.accentPrimary.copy(alpha = 0.25f) else palette.surfaceInset,
                                border = BorderStroke(0.8.dp, palette.accentPrimary.copy(alpha = 0.45f))
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
                                        text = if (isProbing) strings.probingInProgress else strings.probeAllButton,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.accentPrimary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                                val dotColor = when {
                                    !engine.hasProbed -> palette.textMuted
                                    engine.isOnline -> palette.verdictClean
                                    else -> palette.verdictMalicious
                                }
                                val isEnabled = engine.fullName !in settingsSnapshot.disabledProviders

                                CompositionLocalProvider(
                                    LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                                ) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = palette.surfaceCard,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(0.8.dp, palette.borderSubtle)
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
                                                    color = palette.textPrimary,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    val isEngineProbing = engine.id in probingEngineIds
                                                    Surface(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .clickable(enabled = !isEngineProbing) {
                                                                probeOneEngine(engine)
                                                            }
                                                            .pointerHoverIcon(if (isEngineProbing) PointerIcon.Default else PointerIcon.Hand),
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = if (isEngineProbing) palette.accentPrimary.copy(alpha = 0.16f) else palette.surfaceInset,
                                                        border = BorderStroke(0.7.dp, if (isEngineProbing) palette.accentPrimary else dotColor.copy(alpha = 0.45f))
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                        ) {
                                                            if (isEngineProbing) {
                                                                Text("⚡", fontSize = 10.sp, color = palette.accentPrimary)
                                                                Text(
                                                                    text = strings.probingInProgress,
                                                                    fontSize = 10.5.sp,
                                                                    fontFamily = monoFont,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = palette.accentPrimary
                                                                )
                                                            } else {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(6.dp)
                                                                        .clip(CircleShape)
                                                                        .background(dotColor)
                                                                )
                                                                Text(
                                                                    text = when {
                                                                        !engine.hasProbed -> strings.probeUnchecked
                                                                        engine.isOnline -> "${engine.pingMs}ms"
                                                                        else -> "Offline"
                                                                    },
                                                                    fontSize = 11.sp,
                                                                    fontFamily = monoFont,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = when {
                                                                        !engine.hasProbed -> palette.textMuted
                                                                        engine.isOnline -> palette.accentPrimary
                                                                        else -> palette.verdictMalicious
                                                                    }
                                                                )
                                                                Text("🔄", fontSize = 9.sp, color = palette.accentPrimary.copy(alpha = 0.8f))
                                                            }
                                                        }
                                                    }

                                                    if (settingsRepository != null) {
                                                        Switch(
                                                            checked = isEnabled,
                                                            onCheckedChange = { checked ->
                                                                settingsRepository.setProviderEnabled(engine.fullName, checked)
                                                            },
                                                            colors = SwitchDefaults.colors(
                                                                checkedThumbColor = palette.onAccentPrimary,
                                                                checkedTrackColor = palette.accentPrimary
                                                            )
                                                        )
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
                                                    color = palette.textMuted,
                                                    fontSize = 11.sp
                                                )
                                                Surface(
                                                    color = palette.accentPrimary.copy(alpha = 0.12f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = engine.indicatorScope,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = palette.accentPrimary,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }

                                            Text(
                                                text = when (appLanguage) {
                                                    AppLanguage.PERSIAN -> engine.descriptionFa
                                                    AppLanguage.RUSSIAN -> engine.descriptionRu
                                                    AppLanguage.ENGLISH -> engine.descriptionEn
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = palette.textSecondary,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (scroll.maxValue > 0) {
                            BoxWithConstraints(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .width(4.dp)
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(palette.borderSubtle)
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
                                        .background(palette.accentPrimary.copy(alpha = 0.55f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GitHubRepositoryCard(
    strings: AppStrings,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalIocColors.current
    val uriHandler = LocalUriHandler.current
    val clipboard = rememberClipboardActions()
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
                color = palette.accentPrimary,
                fontWeight = FontWeight.SemiBold
            )

            if (isCompact) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            runCatching { uriHandler.openUri(strings.githubRepoUrl) }
                                .onFailure { clipboard.copyText(strings.githubRepoUrl) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .springPress(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accentPrimary,
                            contentColor = palette.onAccentPrimary
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
                                clipboard.copyText(strings.githubRepoUrl)
                                isCopied = true
                                coroutineScope.launch {
                                    delay(2000)
                                    isCopied = false
                                }
                            }
                            .pointerHoverIcon(PointerIcon.Hand),
                        color = palette.surfaceInset,
                        border = BorderStroke(1.dp, palette.borderStrong),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isCopied) "✓ ${strings.githubUrlCopied}" else strings.copyGithubUrl,
                                color = if (isCopied) palette.verdictClean else palette.textPrimary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            runCatching { uriHandler.openUri(strings.githubRepoUrl) }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .springPress(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accentPrimary,
                            contentColor = palette.onAccentPrimary
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
                                clipboard.copyText(strings.githubRepoUrl)
                                isCopied = true
                                coroutineScope.launch {
                                    delay(2000)
                                    isCopied = false
                                }
                            }
                            .pointerHoverIcon(PointerIcon.Hand),
                        color = palette.surfaceInset,
                        border = BorderStroke(1.dp, palette.borderStrong),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isCopied) "✓ ${strings.githubUrlCopied}" else strings.copyGithubUrl,
                                color = if (isCopied) palette.verdictClean else palette.textPrimary,
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

@Composable
private fun SettingsCard(
    title: String,
    badge: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val palette = LocalIocColors.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surfaceCard.copy(alpha = 0.94f)),
        border = BorderStroke(1.dp, topBorderHighlightBrush(palette.accentPrimary, isDark = palette.isDark)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (palette.isDark) 8.dp else 3.dp)
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
                    color = palette.accentPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                if (badge != null) {
                    Surface(
                        color = palette.accentPrimary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.42f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = badge,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accentPrimary,
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
