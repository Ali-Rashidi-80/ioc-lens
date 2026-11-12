package io.github.mrcoder20.ioclens.presentation.settings

import androidx.compose.foundation.background
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
import io.github.mrcoder20.ioclens.core.components.CountryFlag
import io.github.mrcoder20.ioclens.core.components.springPress
import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.AppStrings
import io.github.mrcoder20.ioclens.core.localization.LocalizationProvider
import io.github.mrcoder20.ioclens.core.theme.PrimaryCyan
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

                // 1. Language Card
                LanguageCard(
                    settingsState = settingsState,
                    strings = strings,
                    isCompact = isCompactLayout,
                    onLanguageSelected = { settingsRepository.setAppLanguage(it) }
                )

                // 2. Client-Side Manifesto Card
                ClientManifestoCard(
                    strings = strings
                )

                // 3. GitHub Repository Action Card
                GitHubRepositoryCard(
                    strings = strings,
                    isCompact = isCompactLayout
                )

                Spacer(modifier = Modifier.height(20.dp))
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
        // Back Button with directional arrow (→ for RTL, ← for LTR)
        Surface(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .springPress()
                .clickable(onClick = onClose)
                .pointerHoverIcon(PointerIcon.Hand),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.35f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (isRtl) "→" else "←",
                    fontSize = 20.sp,
                    color = PrimaryCyan,
                    fontWeight = FontWeight.Bold
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
        title = "🌐 ${strings.languageLabel}",
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
 * Client-Side Architecture Manifesto Card:
 * Explains 100% direct-to-engine queries, zero cloud DB, zero telemetry,
 * and automatic RFC 1918 on-device privacy protection.
 */
@Composable
private fun ClientManifestoCard(
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    SettingsCard(
        title = "⚡ ${strings.clientManifestoTitle}",
        badge = "100% CLIENT-SIDE",
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = strings.clientManifestoDesc,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFCBD5E1),
                lineHeight = 22.sp
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = PrimaryCyan.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, PrimaryCyan.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🛡️", fontSize = 16.sp)
                    Text(
                        text = strings.privacyGuardNotice,
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryCyan,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * GitHub Repository Action Card:
 * Prominent link to official repository with Open in Browser and Copy Link actions.
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
        title = "🐙 GitHub",
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.35f)),
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
