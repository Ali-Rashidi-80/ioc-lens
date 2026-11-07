package io.github.mrcoder20.ioclens.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mrcoder20.ioclens.core.components.AppBackHandler
import io.github.mrcoder20.ioclens.core.localization.LocalizationProvider
import io.github.mrcoder20.ioclens.core.theme.PrimaryCyan
import io.github.mrcoder20.ioclens.core.theme.SecondaryTeal
import io.github.mrcoder20.ioclens.core.theme.VerdictClean
import io.github.mrcoder20.ioclens.core.theme.VerdictLowRisk
import io.github.mrcoder20.ioclens.core.theme.VerdictMalicious
import io.github.mrcoder20.ioclens.core.theme.VerdictNoData
import io.github.mrcoder20.ioclens.core.theme.VerdictSuspicious
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.SourceResult
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import io.github.mrcoder20.ioclens.domain.repository.SettingsRepository
import io.github.mrcoder20.ioclens.presentation.intro.IntroScreen
import io.github.mrcoder20.ioclens.presentation.intro.IntroViewModel
import io.github.mrcoder20.ioclens.presentation.settings.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    settingsRepository: SettingsRepository,
    introViewModel: IntroViewModel? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val strings = LocalizationProvider.getStrings(uiState.appLanguage)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Android Back Button / Gesture Interceptor
    val isAnySheetOpen = uiState.isGuideOverlayOpen || uiState.isSettingsSheetOpen || uiState.isHistorySheetOpen || drawerState.isOpen || uiState.isLoading || uiState.activeVerdict != null
    AppBackHandler(enabled = isAnySheetOpen) {
        when {
            uiState.isGuideOverlayOpen -> viewModel.onEvent(HomeEvent.SetGuideOverlayOpen(false))
            uiState.isSettingsSheetOpen -> viewModel.onEvent(HomeEvent.SetSettingsSheetOpen(false))
            uiState.isHistorySheetOpen -> viewModel.onEvent(HomeEvent.SetHistorySheetOpen(false))
            drawerState.isOpen -> coroutineScope.launch { drawerState.close() }
            uiState.isLoading -> viewModel.onEvent(HomeEvent.CancelScan)
            uiState.activeVerdict != null -> viewModel.onEvent(HomeEvent.ClearClicked)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Drawer Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFF090D16), Color(0xFF131B2E))))
                            .statusBarsPadding()
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "🛡️", fontSize = 22.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = strings.appTitle,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = strings.versionLabel,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = strings.appTagline,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    NavigationDrawerItem(
                        icon = { Text(text = "🔍", fontSize = 18.sp) },
                        label = { Text(text = strings.menuHome, fontWeight = FontWeight.SemiBold) },
                        selected = !uiState.isSettingsSheetOpen && !uiState.isHistorySheetOpen && !uiState.isGuideOverlayOpen,
                        onClick = {
                            viewModel.onEvent(HomeEvent.SetSettingsSheetOpen(false))
                            viewModel.onEvent(HomeEvent.SetHistorySheetOpen(false))
                            viewModel.onEvent(HomeEvent.SetGuideOverlayOpen(false))
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Text(text = "📜", fontSize = 18.sp) },
                        label = { Text(text = "Scan History (${uiState.scanHistory.size})", fontWeight = FontWeight.SemiBold) },
                        selected = uiState.isHistorySheetOpen,
                        onClick = {
                            viewModel.onEvent(HomeEvent.SetHistorySheetOpen(true))
                            coroutineScope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Text(text = "⚙️", fontSize = 18.sp) },
                        label = { Text(text = strings.menuSettings, fontWeight = FontWeight.SemiBold) },
                        selected = uiState.isSettingsSheetOpen,
                        onClick = {
                            viewModel.onEvent(HomeEvent.SetSettingsSheetOpen(true))
                            coroutineScope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Text(text = "💡", fontSize = 18.sp) },
                        label = { Text(text = strings.menuGuide, fontWeight = FontWeight.SemiBold) },
                        selected = uiState.isGuideOverlayOpen,
                        onClick = {
                            viewModel.onEvent(HomeEvent.SetGuideOverlayOpen(true))
                            coroutineScope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.versionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Ambient Top Glow
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                PrimaryCyan.copy(alpha = 0.18f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )

            // Main Dashboard Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Bar (☰ Hamburger, Title, Actions)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { coroutineScope.launch { drawerState.open() } },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "☰",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryCyan
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Row {
                            Text(
                                text = "IOC ",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "LENS",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryCyan,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // History Action Button with Counter
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.onEvent(HomeEvent.SetHistorySheetOpen(true)) },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "📜", fontSize = 14.sp)
                                if (uiState.scanHistory.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${uiState.scanHistory.size}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryCyan
                                    )
                                }
                            }
                        }

                        // Settings Action Button
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.onEvent(HomeEvent.SetSettingsSheetOpen(true)) },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = "⚙️",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // File Hashing Progress Banner (if active)
                AnimatedVisibility(visible = uiState.isHashingFile) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131F38)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = PrimaryCyan,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "⚡ Stream Hashing (64KB chunks): ${uiState.hashingFileName ?: "file"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PrimaryCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(3.dp),
                                color = PrimaryCyan,
                                trackColor = Color(0xFF0A1226)
                            )
                        }
                    }
                }

                // Unified Indicator Search & Action Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = uiState.queryInput,
                            onValueChange = { viewModel.onEvent(HomeEvent.QueryChanged(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    text = "Paste IP, URL, Hash, Domain, or drop file...",
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 14.sp
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = PrimaryCyan,
                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Zero-Key instant scan ready",
                                style = MaterialTheme.typography.labelSmall,
                                color = VerdictClean,
                                fontWeight = FontWeight.Medium
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.queryInput.isNotEmpty()) {
                                    TextButton(
                                        onClick = { viewModel.onEvent(HomeEvent.ClearClicked) }
                                    ) {
                                        Text(strings.clear, color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                if (uiState.isLoading) {
                                    Button(
                                        onClick = { viewModel.onEvent(HomeEvent.CancelScan) },
                                        colors = ButtonDefaults.buttonColors(containerColor = VerdictMalicious.copy(alpha = 0.85f)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "⏹ CANCEL",
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.onEvent(HomeEvent.AnalyzeClicked) },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        Text(
                                            text = "⚡ SCAN PAYLOAD",
                                            color = Color.Black,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                uiState.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = error,
                        color = VerdictMalicious,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Instant Overall Verdict Banner (if verdict available)
                AnimatedVisibility(
                    visible = uiState.activeVerdict != null,
                    enter = fadeIn(tween(350)) + expandVertically(tween(350)),
                    exit = fadeOut(tween(250)) + shrinkVertically(tween(250))
                ) {
                    uiState.activeVerdict?.let { verdict ->
                        OverallVerdictCard(verdict = verdict)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Collapsible Live Provider Deck (10 Threat Intel Engines)
                LiveProviderDeck(
                    uiState = uiState,
                    onToggle = { viewModel.onEvent(HomeEvent.ToggleProviderDeck) }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // History Sheet Overlay
            if (uiState.isHistorySheetOpen) {
                HistorySheet(
                    history = uiState.scanHistory,
                    onSelect = { viewModel.onEvent(HomeEvent.HistoryItemClicked(it)) },
                    onClear = { viewModel.onEvent(HomeEvent.ClearHistory) },
                    onClose = { viewModel.onEvent(HomeEvent.SetHistorySheetOpen(false)) }
                )
            }

            // Settings Sheet Overlay
            if (uiState.isSettingsSheetOpen) {
                SettingsScreen(
                    settingsRepository = settingsRepository,
                    onClose = { viewModel.onEvent(HomeEvent.SetSettingsSheetOpen(false)) }
                )
            }

            // Guide Overlay
            if (uiState.isGuideOverlayOpen && introViewModel != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF06080D))
                ) {
                    IntroScreen(
                        viewModel = introViewModel,
                        onIntroFinished = {
                            viewModel.onEvent(HomeEvent.SetGuideOverlayOpen(false))
                        }
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(16.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.onEvent(HomeEvent.SetGuideOverlayOpen(false)) },
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "✕",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverallVerdictCard(verdict: TriageVerdict) {
    val verdictColor = when (verdict.overallVerdict) {
        VerdictLevel.CLEAN -> VerdictClean
        VerdictLevel.LOW_RISK -> VerdictLowRisk
        VerdictLevel.SUSPICIOUS -> VerdictSuspicious
        VerdictLevel.MALICIOUS -> VerdictMalicious
        VerdictLevel.NO_DATA -> VerdictNoData
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, verdictColor.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = verdict.indicator.rawValue,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "Type: ${verdict.indicator.type.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Surface(
                    color = verdictColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, verdictColor)
                ) {
                    Text(
                        text = verdict.overallVerdict.name,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = verdictColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = verdict.summaryDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Threat Confidence Score",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryCyan,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${verdict.confidenceScore}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = verdictColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { verdict.confidenceScore / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = verdictColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun LiveProviderDeck(
    uiState: HomeUiState,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🌐 THREAT INTEL PROVIDERS",
                        style = MaterialTheme.typography.labelLarge,
                        color = PrimaryCyan,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = PrimaryCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "10 Engines",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryCyan,
                            fontSize = 10.sp
                        )
                    }
                }

                Text(
                    text = if (uiState.isProviderDeckExpanded) "▲" else "▼",
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 12.sp
                )
            }

            AnimatedVisibility(visible = uiState.isProviderDeckExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    val activeSources = uiState.activeVerdict?.sourceResults ?: emptyList()
                    val activeMap = activeSources.associateBy { it.providerName }

                    val allProviders = listOf(
                        "URLhaus (abuse.ch)" to true,
                        "MalwareBazaar (abuse.ch)" to true,
                        "ThreatFox (abuse.ch)" to true,
                        "RDAP / Registry" to true,
                        "Cloudflare DoH" to true,
                        "VirusTotal" to false,
                        "AbuseIPDB" to false,
                        "AlienVault OTX" to false,
                        "Pulsedive" to false,
                        "IPQualityScore" to false
                    )

                    allProviders.forEach { (name, isZeroKey) ->
                        val result = activeMap[name]
                        ProviderRow(
                            name = name,
                            isZeroKey = isZeroKey,
                            isLoading = uiState.isLoading,
                            result = result,
                            indicatorType = uiState.activeVerdict?.indicator?.type,
                            hasActiveVerdict = uiState.activeVerdict != null
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

private fun isProviderApplicable(name: String, type: IndicatorType?): Boolean {
    if (type == null || type == IndicatorType.UNKNOWN) return false
    return when (name) {
        "URLhaus (abuse.ch)" -> type == IndicatorType.URL || type == IndicatorType.DOMAIN
        "MalwareBazaar (abuse.ch)" -> type == IndicatorType.FILE_HASH
        "ThreatFox (abuse.ch)" -> true
        "RDAP / Registry" -> type == IndicatorType.IP_ADDRESS || type == IndicatorType.DOMAIN
        "Cloudflare DoH" -> type == IndicatorType.DOMAIN
        "VirusTotal" -> true
        "AbuseIPDB" -> type == IndicatorType.IP_ADDRESS
        "AlienVault OTX" -> true
        "Pulsedive" -> true
        "IPQualityScore" -> type == IndicatorType.IP_ADDRESS
        else -> false
    }
}

@Composable
private fun ProviderRow(
    name: String,
    isZeroKey: Boolean,
    isLoading: Boolean,
    result: SourceResult?,
    indicatorType: IndicatorType?,
    hasActiveVerdict: Boolean
) {
    val isRateLimited = result?.isRateLimited == true || result?.summary?.contains("Rate Limit", ignoreCase = true) == true
    val isTimedOut = result?.summary?.contains("timed out", ignoreCase = true) == true

    val statusColor = when {
        isRateLimited -> Color(0xFFF59E0B) // Amber Rate Limit Warning
        isTimedOut -> Color(0xFF94A3B8)
        result != null -> when (result.verdict) {
            VerdictLevel.CLEAN -> VerdictClean
            VerdictLevel.LOW_RISK -> VerdictLowRisk
            VerdictLevel.SUSPICIOUS -> VerdictSuspicious
            VerdictLevel.MALICIOUS -> VerdictMalicious
            VerdictLevel.NO_DATA -> VerdictNoData
        }
        else -> VerdictNoData
    }

    val statusText = when {
        isLoading -> "Scanning..."
        isRateLimited -> "Rate Limited"
        isTimedOut -> "Timed Out"
        result != null -> result.verdict.name
        hasActiveVerdict && !isProviderApplicable(name, indicatorType) -> "N/A for Type"
        hasActiveVerdict -> "Key Required"
        isZeroKey -> "Ready (Zero-Key)"
        else -> "Key Required"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D1424), RoundedCornerShape(10.dp))
            .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                if (isZeroKey) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "● Free",
                        color = VerdictClean,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (result != null) {
                Text(
                    text = result.summary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            color = if (isLoading) PrimaryCyan.copy(alpha = 0.15f) else statusColor.copy(alpha = 0.18f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = statusText,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                color = if (isLoading) PrimaryCyan else statusColor,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun HistorySheet(
    history: List<TriageVerdict>,
    onSelect: (TriageVerdict) -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {}
                .padding(horizontal = 8.dp, vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📜 Recent Scans (${history.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = PrimaryCyan,
                        fontWeight = FontWeight.Bold
                    )

                    Row {
                        if (history.isNotEmpty()) {
                            TextButton(onClick = onClear) {
                                Text(text = "Clear", color = VerdictMalicious, fontSize = 12.sp)
                            }
                        }
                        TextButton(onClick = onClose) {
                            Text(text = "✕", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))

                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No scan history yet. Scan an indicator or share a file to get started.",
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        history.forEach { item ->
                            val itemColor = when (item.overallVerdict) {
                                VerdictLevel.CLEAN -> VerdictClean
                                VerdictLevel.LOW_RISK -> VerdictLowRisk
                                VerdictLevel.SUSPICIOUS -> VerdictSuspicious
                                VerdictLevel.MALICIOUS -> VerdictMalicious
                                VerdictLevel.NO_DATA -> VerdictNoData
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0D1424))
                                    .border(0.5.dp, itemColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .clickable { onSelect(item) }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.indicator.rawValue,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${item.indicator.type} • Score: ${item.confidenceScore}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Surface(
                                    color = itemColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = item.overallVerdict.name,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        color = itemColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
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
