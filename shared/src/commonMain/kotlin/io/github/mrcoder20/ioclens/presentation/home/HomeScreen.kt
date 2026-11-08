package io.github.mrcoder20.ioclens.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.mrcoder20.ioclens.core.components.AppBackHandler
import io.github.mrcoder20.ioclens.core.components.ControlledMarqueeText
import io.github.mrcoder20.ioclens.core.components.DisintegrationContainer
import io.github.mrcoder20.ioclens.core.components.TrainMarqueeRow
import io.github.mrcoder20.ioclens.core.components.springPress
import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.AppStrings
import io.github.mrcoder20.ioclens.core.localization.LocalizationProvider
import io.github.mrcoder20.ioclens.core.theme.PrimaryCyan
import io.github.mrcoder20.ioclens.core.theme.VerdictClean
import io.github.mrcoder20.ioclens.core.theme.VerdictLowRisk
import io.github.mrcoder20.ioclens.core.theme.VerdictMalicious
import io.github.mrcoder20.ioclens.core.theme.VerdictNoData
import io.github.mrcoder20.ioclens.core.theme.VerdictSuspicious
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.SourceResult
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import io.github.mrcoder20.ioclens.domain.repository.SettingsRepository
import io.github.mrcoder20.ioclens.presentation.intro.IntroEvent
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
    val scrollState = rememberScrollState()

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
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Drawer Header with Physical Right-Aligned Close Button (No overlap in RTL/LTR)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFF090D16), Color(0xFF131B2E))))
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { coroutineScope.launch { drawerState.close() } }
                                        .pointerHoverIcon(PointerIcon.Hand),
                                    color = Color.White.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "✕",
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

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
                        label = {
                            Text(
                                text = "${strings.scanHistoryTitle} (${LocalizationProvider.formatNumber(uiState.scanHistory.size, uiState.appLanguage)})",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
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
                            introViewModel?.onEvent(IntroEvent.Reset)
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val isWideScreen = screenWidth >= 840.dp
            val isMediumScreen = screenWidth >= 560.dp && screenWidth < 840.dp
            val isCompactScreen = screenWidth < 600.dp
            val isMobileOrTablet = screenWidth < 840.dp

            // Ambient Top Glow (Cached Brush to avoid GC pressure and hitch)
            val bgColor = MaterialTheme.colorScheme.background
            val ambientGlowBrush = remember(bgColor) {
                Brush.verticalGradient(
                    colors = listOf(
                        PrimaryCyan.copy(alpha = 0.18f),
                        bgColor
                    )
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(brush = ambientGlowBrush)
            )

            // Main Dashboard Container with Responsive Width & Safe Area Padding
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .widthIn(max = 1380.dp)
                    .align(Alignment.TopCenter)
                    .verticalScroll(scrollState)
            ) {
                // Header Bar (☰ Hamburger & Telemetry on Left, IOC LENS Dead-Centered, Actions on Right)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    // Left / Start Slot: Hamburger & Telemetry Status Indicators (Desktop)
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isMobileOrTablet) {
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

                            Spacer(modifier = Modifier.width(16.dp))

                            // Desktop Telemetry Status Indicators
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    color = Color(0xFF0B192C),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.6.dp, VerdictClean.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(VerdictClean))
                                        Text(text = strings.systemReadyEngines, fontSize = 10.5.sp, color = VerdictClean, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                Surface(
                                    color = Color(0xFF0B192C),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.6.dp, PrimaryCyan.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PrimaryCyan))
                                        Text(text = strings.freeFeedsBadge, fontSize = 10.5.sp, color = PrimaryCyan, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                Surface(
                                    color = Color(0xFF0B192C),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(text = "🛡️", fontSize = 10.sp)
                                        Text(text = strings.privacyGuardActiveBadge, fontSize = 10.5.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    // Center Slot: Dead-Centered App Title with Strict LTR BiDi Protection
                    Box(
                        modifier = Modifier.align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    }

                    // Right / End Slot: History and Settings Action Buttons (Desktop)
                    if (!isMobileOrTablet) {
                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                            text = LocalizationProvider.formatNumber(uiState.scanHistory.size, uiState.appLanguage),
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
                }

                if (isWideScreen) {
                    val dashboardHeight = (screenHeight - 165.dp).coerceAtLeast(535.dp)

                    // ADAPTIVE 2-COLUMN SPLIT DASHBOARD (Desktop & Wide Tablets - Guaranteed Equal Height Without Intrinsics)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dashboardHeight)
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Pane: Search Console, Progress, Verdict & Recent Triage Feed
                        Column(
                            modifier = Modifier
                                .weight(1.08f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            FileHashingBanner(uiState = uiState, appLanguage = uiState.appLanguage)

                            AnimatedVisibility(
                                visible = uiState.isLoading,
                                enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                                exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                            ) {
                                LiveScanBanner(
                                    targetIndicator = uiState.targetIndicator,
                                    finishedEngines = uiState.scanFinishedEngines,
                                    totalEngines = uiState.scanTotalEngines,
                                    appLanguage = uiState.appLanguage,
                                    onCancel = { viewModel.onEvent(HomeEvent.CancelScan) }
                                )
                            }

                            SearchCard(
                                uiState = uiState,
                                strings = strings,
                                isCompact = isCompactScreen,
                                onQueryChange = { viewModel.onEvent(HomeEvent.QueryChanged(it)) },
                                onClear = { viewModel.onEvent(HomeEvent.ClearClicked) },
                                onCancel = { viewModel.onEvent(HomeEvent.CancelScan) },
                                onAnalyze = { viewModel.onEvent(HomeEvent.AnalyzeClicked) }
                            )

                            ErrorMessageBanner(errorMessage = uiState.errorMessage)

                            AnimatedVisibility(
                                visible = uiState.activeVerdict != null,
                                enter = fadeIn(tween(350)) + expandVertically(tween(350)),
                                exit = fadeOut(tween(250)) + shrinkVertically(tween(250))
                            ) {
                                uiState.activeVerdict?.let { verdict ->
                                    OverallVerdictCard(verdict = verdict, appLanguage = uiState.appLanguage)
                                }
                            }

                            // Desktop Recent Triage Scans HUD Feed dynamically fills remaining height to balance Left Pane
                            RecentTriageFeedHUD(
                                history = uiState.scanHistory,
                                appLanguage = uiState.appLanguage,
                                strings = strings,
                                onSelect = { item ->
                                    viewModel.onEvent(HomeEvent.HistoryItemClicked(item))
                                    coroutineScope.launch { scrollState.animateScrollTo(0) }
                                },
                                onDelete = { item ->
                                    viewModel.onEvent(HomeEvent.DeleteHistoryItem(item))
                                },
                                onReScan = { rawValue ->
                                    viewModel.onEvent(HomeEvent.QueryChanged(rawValue))
                                    viewModel.onEvent(HomeEvent.AnalyzeClicked)
                                    coroutineScope.launch { scrollState.animateScrollTo(0) }
                                },
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                        }

                        // Right Pane: Threat Intel Provider Deck (Balanced Column on Widescreen)
                        Column(
                            modifier = Modifier
                                .weight(0.92f)
                                .fillMaxHeight()
                        ) {
                            LiveProviderDeck(
                                uiState = uiState,
                                onToggle = { viewModel.onEvent(HomeEvent.ToggleProviderDeck) },
                                isTwoColumn = false,
                                modifier = Modifier.fillMaxWidth().fillMaxHeight()
                            )
                        }
                    }
                } else {
                    // FLUID SINGLE-COLUMN / FOLDABLE DASHBOARD (Phones & Foldables)
                    FileHashingBanner(uiState = uiState, appLanguage = uiState.appLanguage)

                    AnimatedVisibility(
                        visible = uiState.isLoading,
                        enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                        exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                    ) {
                        LiveScanBanner(
                            targetIndicator = uiState.targetIndicator,
                            finishedEngines = uiState.scanFinishedEngines,
                            totalEngines = uiState.scanTotalEngines,
                            appLanguage = uiState.appLanguage,
                            onCancel = { viewModel.onEvent(HomeEvent.CancelScan) }
                        )
                    }

                    SearchCard(
                        uiState = uiState,
                        strings = strings,
                        isCompact = isCompactScreen,
                        onQueryChange = { viewModel.onEvent(HomeEvent.QueryChanged(it)) },
                        onClear = { viewModel.onEvent(HomeEvent.ClearClicked) },
                        onCancel = { viewModel.onEvent(HomeEvent.CancelScan) },
                        onAnalyze = { viewModel.onEvent(HomeEvent.AnalyzeClicked) }
                    )

                    ErrorMessageBanner(errorMessage = uiState.errorMessage)

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedVisibility(
                        visible = uiState.activeVerdict != null,
                        enter = fadeIn(tween(350)) + expandVertically(tween(350)),
                        exit = fadeOut(tween(250)) + shrinkVertically(tween(250))
                    ) {
                        uiState.activeVerdict?.let { verdict ->
                            OverallVerdictCard(verdict = verdict, appLanguage = uiState.appLanguage)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LiveProviderDeck(
                        uiState = uiState,
                        onToggle = { viewModel.onEvent(HomeEvent.ToggleProviderDeck) },
                        isTwoColumn = screenWidth >= 680.dp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    RecentTriageFeedHUD(
                        history = uiState.scanHistory,
                        appLanguage = uiState.appLanguage,
                        strings = strings,
                        onSelect = { item ->
                            viewModel.onEvent(HomeEvent.HistoryItemClicked(item))
                            coroutineScope.launch { scrollState.animateScrollTo(0) }
                        },
                        onDelete = { item ->
                            viewModel.onEvent(HomeEvent.DeleteHistoryItem(item))
                        },
                        onReScan = { rawValue ->
                            viewModel.onEvent(HomeEvent.QueryChanged(rawValue))
                            viewModel.onEvent(HomeEvent.AnalyzeClicked)
                            coroutineScope.launch { scrollState.animateScrollTo(0) }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cyber Telemetry & Privacy Status Footer
                CyberTelemetryFooter(
                    strings = strings,
                    isMobileOrTablet = isMobileOrTablet
                )

                Spacer(modifier = Modifier.height(if (isMobileOrTablet) 84.dp else 40.dp))
            }

            // History Sheet Overlay (Responsive: Floating Modal on Tablets/Desktop, Bottom Sheet on Phones)
            AnimatedVisibility(
                visible = uiState.isHistorySheetOpen,
                enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.96f),
                exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.96f)
            ) {
                HistorySheet(
                    history = uiState.scanHistory,
                    appLanguage = uiState.appLanguage,
                    isDialogMode = isWideScreen,
                    isMobileOrTablet = isMobileOrTablet,
                    onSelect = { item ->
                        viewModel.onEvent(HomeEvent.HistoryItemClicked(item))
                        coroutineScope.launch { scrollState.animateScrollTo(0) }
                    },
                    onDelete = { item ->
                        viewModel.onEvent(HomeEvent.DeleteHistoryItem(item))
                    },
                    onClear = { viewModel.onEvent(HomeEvent.ClearHistory) },
                    onClose = { viewModel.onEvent(HomeEvent.SetHistorySheetOpen(false)) }
                )
            }

            // Settings Sheet Overlay
            AnimatedVisibility(
                visible = uiState.isSettingsSheetOpen,
                enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 10 },
                exit = fadeOut(tween(160)) + slideOutVertically(tween(160)) { it / 10 }
            ) {
                SettingsScreen(
                    settingsRepository = settingsRepository,
                    isMobileOrTablet = isMobileOrTablet,
                    onClose = { viewModel.onEvent(HomeEvent.SetSettingsSheetOpen(false)) },
                    onReplayGuide = {
                        viewModel.onEvent(HomeEvent.SetSettingsSheetOpen(false))
                        viewModel.onEvent(HomeEvent.SetGuideOverlayOpen(true))
                        introViewModel?.onEvent(IntroEvent.Reset)
                    }
                )
            }

            // Guide Overlay with Embedded Header Close Button (Integrated into top bar to prevent collisions)
            if (uiState.isGuideOverlayOpen && introViewModel != null) {
                androidx.compose.runtime.key(uiState.isGuideOverlayOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF06080D))
                            .zIndex(100f)
                    ) {
                        IntroScreen(
                            viewModel = introViewModel,
                            onIntroFinished = {
                                viewModel.onEvent(HomeEvent.SetGuideOverlayOpen(false))
                            },
                            onClose = {
                                viewModel.onEvent(HomeEvent.SetGuideOverlayOpen(false))
                            }
                        )
                    }
                }
            }

            // Mobile & Tablet Adaptive Cyberpunk Bottom Navigation Bar
            if (isMobileOrTablet && !uiState.isGuideOverlayOpen) {
                CyberpunkBottomNavBar(
                    selectedTab = uiState.selectedBottomNavTab,
                    historyCount = uiState.scanHistory.size,
                    appLanguage = uiState.appLanguage,
                    strings = strings,
                    onTabSelected = { tab ->
                        viewModel.onEvent(HomeEvent.SelectBottomNavTab(tab))
                        when (tab) {
                            0 -> coroutineScope.launch { scrollState.animateScrollTo(0) }
                            1 -> coroutineScope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

/**
 * Animated Cyberpunk Live Status Banner with Radar Sweep, Target Details, Real-Time Engine Counter, and Instant Cancel.
 */
@Composable
private fun LiveScanBanner(
    targetIndicator: Indicator?,
    finishedEngines: Int,
    totalEngines: Int,
    appLanguage: AppLanguage,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalizationProvider.getStrings(appLanguage)
    val infiniteTransition = rememberInfiniteTransition()

    // Rotating radar beam
    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF091424).copy(alpha = 0.95f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            Brush.horizontalGradient(
                listOf(
                    PrimaryCyan.copy(alpha = 0.8f),
                    Color(0xFF00FF88).copy(alpha = 0.7f),
                    PrimaryCyan.copy(alpha = 0.8f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Radar + Target info + Cancel Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    // Cyber Radar Scanner Icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF06101E))
                            .border(1.dp, PrimaryCyan.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(28.dp).rotate(radarRotation)) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = size.width / 2f

                            // Outer radar circle
                            drawCircle(
                                color = PrimaryCyan.copy(alpha = 0.35f),
                                radius = radius,
                                style = Stroke(width = 1.2.dp.toPx())
                            )
                            // Inner circle
                            drawCircle(
                                color = PrimaryCyan.copy(alpha = 0.2f),
                                radius = radius * 0.55f,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            // Sweep needle
                            drawLine(
                                color = PrimaryCyan,
                                start = center,
                                end = Offset(center.x + radius, center.y),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                        // Center blip
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = PrimaryCyan.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(0.6.dp, PrimaryCyan.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = targetIndicator?.let { LocalizationProvider.getIndicatorTypeName(it.type, appLanguage) } ?: strings.statusScanning,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryCyan
                                )
                            }

                            Text(
                                text = strings.liveScanInProgress,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }

                        if (targetIndicator != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                ControlledMarqueeText(
                                    text = targetIndicator.rawValue,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9CA3AF),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Instant Cancel Button
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = VerdictMalicious.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .border(1.dp, VerdictMalicious, RoundedCornerShape(10.dp))
                ) {
                    Text(
                        text = strings.cancelScan,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // Progress Bar & Finished Engines Counter
            val progress = if (totalEngines > 0) (finishedEngines.toFloat() / totalEngines).coerceIn(0f, 1f) else 0f
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${LocalizationProvider.formatNumber(finishedEngines, appLanguage)} / ${LocalizationProvider.formatNumber(totalEngines, appLanguage)} ${strings.enginesCompleted}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9CA3AF),
                        fontSize = 10.5.sp
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = PrimaryCyan,
                    trackColor = Color(0xFF101B2E)
                )
            }
        }
    }
}

/**
 * Adaptive Cyberpunk Bottom Navigation Bar for Mobile and Tablet Viewports (< 720dp).
 */
@Composable
private fun CyberpunkBottomNavBar(
    selectedTab: Int,
    historyCount: Int,
    appLanguage: AppLanguage,
    strings: AppStrings,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color(0xFF090E17).copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    Color.Transparent,
                    PrimaryCyan.copy(alpha = 0.5f),
                    PrimaryCyan.copy(alpha = 0.7f),
                    PrimaryCyan.copy(alpha = 0.5f),
                    Color.Transparent
                )
            )
        ),
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Triple(0, "🔍", strings.tabScanner),
                Triple(1, "🌐", strings.tabEngines),
                Triple(2, "📜", strings.tabHistory),
                Triple(3, "⚙️", strings.tabSettings)
            )

            tabs.forEach { (index, icon, label) ->
                val isSelected = selectedTab == index
                val activeColor = PrimaryCyan
                val inactiveColor = Color(0xFF7E8B9F)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(index) }
                        .pointerHoverIcon(PointerIcon.Hand)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = icon,
                                fontSize = 18.sp
                            )
                            if (index == 2 && historyCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 10.dp, y = (-4).dp)
                                        .background(PrimaryCyan, CircleShape)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = LocalizationProvider.formatNumber(historyCount, appLanguage),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) activeColor else inactiveColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .width(if (isSelected) 18.dp else 0.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (isSelected) PrimaryCyan else Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileHashingBanner(uiState: HomeUiState, appLanguage: AppLanguage) {
    val strings = LocalizationProvider.getStrings(appLanguage)
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
                        text = "⚡ ${strings.streamHashingTitle}: ${uiState.hashingFileName ?: "file"}",
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
}

@Composable
private fun SearchCard(
    uiState: HomeUiState,
    strings: io.github.mrcoder20.ioclens.core.localization.AppStrings,
    isCompact: Boolean,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onCancel: () -> Unit,
    onAnalyze: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            val cardWidth = maxWidth
            val useStackedLayout = isCompact || cardWidth < 580.dp

            Column {
                OutlinedTextField(
                    value = uiState.queryInput,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(
                        textDirection = TextDirection.Ltr,
                        fontFamily = FontFamily.Monospace,
                        fontSize = if (isCompact) 13.5.sp else 15.sp,
                        color = Color.White
                    ),
                    placeholder = {
                        Text(
                            text = strings.searchFieldPlaceholder,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = if (isCompact) 12.sp else 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    trailingIcon = {
                        if (uiState.queryInput.isNotEmpty()) {
                            IconButton(
                                onClick = onClear,
                                modifier = Modifier
                                    .size(34.dp)
                                    .springPress()
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "✕",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
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

                if (useStackedLayout) {
                    // Stacked Layout: Status banner in full width row, buttons on bottom row
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VerdictClean.copy(alpha = 0.10f),
                            border = androidx.compose.foundation.BorderStroke(0.6.dp, VerdictClean.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(VerdictClean))
                                ControlledMarqueeText(
                                    text = strings.zeroKeyInstantScanReady,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VerdictClean,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    val clipText = clipboardManager.getText()?.text
                                    if (!clipText.isNullOrBlank()) {
                                        onQueryChange(clipText.trim())
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = PrimaryCyan)
                            ) {
                                Text(
                                    text = strings.pasteFromClipboard,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (uiState.isLoading) {
                                Button(
                                    onClick = onCancel,
                                    colors = ButtonDefaults.buttonColors(containerColor = VerdictMalicious.copy(alpha = 0.85f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(13.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.cancelScan,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp
                                    )
                                }
                            } else {
                                Button(
                                    onClick = onAnalyze,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text(
                                        text = strings.scanPayload,
                                        color = Color.Black,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Standard Wide Layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(VerdictClean))
                            ControlledMarqueeText(
                                text = strings.zeroKeyInstantScanReady,
                                style = MaterialTheme.typography.labelSmall,
                                color = VerdictClean,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    val clipText = clipboardManager.getText()?.text
                                    if (!clipText.isNullOrBlank()) {
                                        onQueryChange(clipText.trim())
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = PrimaryCyan)
                            ) {
                                Text(
                                    text = strings.pasteFromClipboard,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            if (uiState.isLoading) {
                                Button(
                                    onClick = onCancel,
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
                                        text = strings.cancelScan,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                Button(
                                    onClick = onAnalyze,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Text(
                                        text = strings.scanPayload,
                                        color = Color.Black,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Samples & Drop Target Banner
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.sampleIocsTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = strings.dropFileHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6B7280),
                            fontSize = 10.5.sp
                        )
                    }

                    val samples = listOf(
                        "8.8.8.8" to strings.sampleGoogleDns,
                        "malware-test.com" to strings.sampleMalwareUrl,
                        "44d88612fea8a8f36de82e1278abb02f" to strings.sampleHash
                    )

                    TrainMarqueeRow(
                        items = samples,
                        modifier = Modifier.fillMaxWidth(),
                        velocityDpPerSecond = 26f,
                        spacing = 8.dp,
                        fadeEdges = true,
                        fadeWidth = 20.dp
                    ) { (ioc, label) ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onQueryChange(ioc)
                                    onAnalyze()
                                }
                                .pointerHoverIcon(PointerIcon.Hand),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, PrimaryCyan.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.5.sp,
                                color = PrimaryCyan,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorMessageBanner(errorMessage: String?) {
    errorMessage?.let { error ->
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = error,
            color = VerdictMalicious,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun OverallVerdictCard(
    verdict: TriageVerdict,
    appLanguage: AppLanguage
) {
    val verdictColor = when (verdict.overallVerdict) {
        VerdictLevel.CLEAN -> VerdictClean
        VerdictLevel.LOW_RISK -> VerdictLowRisk
        VerdictLevel.SUSPICIOUS -> VerdictSuspicious
        VerdictLevel.MALICIOUS -> VerdictMalicious
        VerdictLevel.NO_DATA -> VerdictNoData
    }
    val strings = LocalizationProvider.getStrings(appLanguage)

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
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        ControlledMarqueeText(
                            text = verdict.indicator.rawValue,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        text = "${strings.indicatorTypeLabel}: ${LocalizationProvider.getIndicatorTypeName(verdict.indicator.type, appLanguage)}",
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
                        text = LocalizationProvider.getVerdictName(verdict.overallVerdict, appLanguage),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = verdictColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = LocalizationProvider.getLocalizedVerdictSummary(verdict, appLanguage),
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
                    text = strings.threatConfidenceScore,
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryCyan,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = LocalizationProvider.formatScore(verdict.confidenceScore, appLanguage),
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
    onToggle: () -> Unit,
    isTwoColumn: Boolean = false,
    modifier: Modifier = Modifier
) {
    val strings = LocalizationProvider.getStrings(uiState.appLanguage)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = PrimaryCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Text(
                            text = strings.enginesCount,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryCyan,
                            fontSize = 10.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    ControlledMarqueeText(
                        text = strings.threatIntelProvidersTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = PrimaryCyan,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Text(
                    text = if (uiState.isProviderDeckExpanded) "▲" else "▼",
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            AnimatedVisibility(visible = uiState.isProviderDeckExpanded) {
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

                val currentIndicatorType = uiState.targetIndicator?.type ?: uiState.activeVerdict?.indicator?.type

                if (isTwoColumn) {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allProviders.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { (name, isZeroKey) ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        ProviderRow(
                                            name = name,
                                            isZeroKey = isZeroKey,
                                            isLoading = uiState.isLoading,
                                            result = activeMap[name],
                                            indicatorType = currentIndicatorType,
                                            hasActiveVerdict = uiState.activeVerdict != null,
                                            appLanguage = uiState.appLanguage,
                                            isTwoColumn = true
                                        )
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(top = 12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        allProviders.forEach { (name, isZeroKey) ->
                            val result = activeMap[name]
                            ProviderRow(
                                name = name,
                                isZeroKey = isZeroKey,
                                isLoading = uiState.isLoading,
                                result = result,
                                indicatorType = currentIndicatorType,
                                hasActiveVerdict = uiState.activeVerdict != null,
                                appLanguage = uiState.appLanguage,
                                isTwoColumn = false
                            )
                        }
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
    hasActiveVerdict: Boolean,
    appLanguage: AppLanguage,
    isTwoColumn: Boolean = false
) {
    val strings = LocalizationProvider.getStrings(appLanguage)
    val isRateLimited = result?.isRateLimited == true || result?.summary?.contains("Rate Limit", ignoreCase = true) == true
    val isTimedOut = result?.summary?.contains("timed out", ignoreCase = true) == true
    val isApplicable = isProviderApplicable(name, indicatorType)

    val statusColor = when {
        isRateLimited -> Color(0xFFF59E0B) // Amber Rate Limit Warning
        isTimedOut -> Color(0xFF94A3B8)
        isLoading && !isApplicable && indicatorType != null -> VerdictNoData
        isLoading && isApplicable -> PrimaryCyan
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
        isLoading && !isApplicable && indicatorType != null -> strings.statusNotApplicable
        isLoading -> strings.statusScanning
        isRateLimited -> strings.statusRateLimited
        isTimedOut -> strings.statusTimedOut
        result != null -> LocalizationProvider.getVerdictName(result.verdict, appLanguage)
        hasActiveVerdict && !isApplicable -> strings.statusNotApplicable
        hasActiveVerdict -> if (isTwoColumn) (if (appLanguage == AppLanguage.PERSIAN) "نیاز به کلید" else strings.statusKeyRequired) else strings.statusKeyRequired
        isZeroKey -> if (isTwoColumn) strings.statusReady else strings.statusReadyZeroKey
        else -> if (isTwoColumn) (if (appLanguage == AppLanguage.PERSIAN) "نیاز به کلید" else strings.statusKeyRequired) else strings.statusKeyRequired
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D1424), RoundedCornerShape(10.dp))
            .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
            .padding(horizontal = if (isTwoColumn) 8.dp else 12.dp, vertical = if (isTwoColumn) 6.dp else 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ControlledMarqueeText(
                    text = LocalizationProvider.getProviderDisplayName(name, appLanguage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (isTwoColumn) 11.5.sp else 13.sp,
                    modifier = Modifier.weight(1f)
                )
                if (isZeroKey) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.freeBadge,
                        color = VerdictClean,
                        fontSize = if (isTwoColumn) 9.sp else 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            if (result != null) {
                ControlledMarqueeText(
                    text = LocalizationProvider.getLocalizedSourceSummary(result.summary, appLanguage),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = if (isTwoColumn) 9.sp else 10.5.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Surface(
            color = if (isLoading) PrimaryCyan.copy(alpha = 0.15f) else statusColor.copy(alpha = 0.18f),
            shape = RoundedCornerShape(6.dp)
        ) {
            ControlledMarqueeText(
                text = statusText,
                modifier = Modifier
                    .padding(horizontal = if (isTwoColumn) 5.dp else 8.dp, vertical = 2.dp)
                    .widthIn(max = if (isTwoColumn) 115.dp else 160.dp),
                color = if (isLoading) PrimaryCyan else statusColor,
                fontWeight = FontWeight.Bold,
                fontSize = if (isTwoColumn) 8.5.sp else 10.sp
            )
        }
    }
}

@Composable
private fun HistorySheet(
    history: List<TriageVerdict>,
    appLanguage: AppLanguage,
    isDialogMode: Boolean,
    isMobileOrTablet: Boolean = false,
    onSelect: (TriageVerdict) -> Unit,
    onDelete: (TriageVerdict) -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalizationProvider.getStrings(appLanguage)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    var deletingItemRawValue by remember { mutableStateOf<String?>(null) }
    var pendingDeletedRawValues by remember { mutableStateOf<Set<String>>(emptySet()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onClose() },
        contentAlignment = if (isDialogMode) Alignment.Center else Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .let {
                    if (isDialogMode) {
                        it.widthIn(max = 640.dp).fillMaxWidth(0.9f)
                    } else {
                        it.fillMaxWidth().fillMaxHeight(0.88f)
                    }
                }
                .clickable(enabled = false) {}
                .padding(
                    start = if (isDialogMode) 16.dp else 8.dp,
                    end = if (isDialogMode) 16.dp else 8.dp,
                    top = 12.dp,
                    bottom = if (isMobileOrTablet) 84.dp else 12.dp
                )
                .navigationBarsPadding(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header with Right-Aligned Close Button in both LTR & RTL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isRtl) {
                        // In RTL, Start is physical Right -> Close & Clear buttons first
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onClose() }
                                    .pointerHoverIcon(PointerIcon.Hand),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "✕",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (history.isNotEmpty()) {
                                TextButton(
                                    onClick = onClear,
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                                ) {
                                    Text(text = strings.clearHistory, color = VerdictMalicious, fontSize = 12.sp)
                                }
                            }
                        }

                        Text(
                            text = "📜 ${strings.scanHistoryTitle} (${LocalizationProvider.formatNumber(history.size, appLanguage)})",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryCyan,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        // In LTR, Title first, then Clear and Close button on the Right (End)
                        Text(
                            text = "📜 ${strings.scanHistoryTitle} (${LocalizationProvider.formatNumber(history.size, appLanguage)})",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryCyan,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (history.isNotEmpty()) {
                                TextButton(
                                    onClick = onClear,
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                                ) {
                                    Text(text = strings.clearHistory, color = VerdictMalicious, fontSize = 12.sp)
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onClose() }
                                    .pointerHoverIcon(PointerIcon.Hand),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "✕",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.noHistory,
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .let { if (isDialogMode) it.heightIn(max = 480.dp) else it }
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        history.forEach { item ->
                            val isPendingDelete = item.indicator.rawValue in pendingDeletedRawValues
                            if (!isPendingDelete) {
                                val itemColor = when (item.overallVerdict) {
                                    VerdictLevel.CLEAN -> VerdictClean
                                    VerdictLevel.LOW_RISK -> VerdictLowRisk
                                    VerdictLevel.SUSPICIOUS -> VerdictSuspicious
                                    VerdictLevel.MALICIOUS -> VerdictMalicious
                                    VerdictLevel.NO_DATA -> VerdictNoData
                                }

                                DisintegrationContainer(
                                    isDisintegrating = deletingItemRawValue == item.indicator.rawValue,
                                    accent = itemColor,
                                    particleCount = 1800,
                                    collapseHeight = true,
                                    durationMs = 1200,
                                    onDisintegrated = {
                                        pendingDeletedRawValues = pendingDeletedRawValues + item.indicator.rawValue
                                        onDelete(item)
                                        if (deletingItemRawValue == item.indicator.rawValue) {
                                            deletingItemRawValue = null
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF0D1424))
                                            .border(0.5.dp, itemColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                            .clickable { onSelect(item) }
                                            .pointerHoverIcon(PointerIcon.Hand)
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                                ControlledMarqueeText(
                                                    text = item.indicator.rawValue,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                            Text(
                                                text = "${LocalizationProvider.getIndicatorTypeName(item.indicator.type, appLanguage)} • ${strings.threatConfidenceScore}: ${LocalizationProvider.formatScore(item.confidenceScore, appLanguage)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                color = itemColor.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = LocalizationProvider.getVerdictName(item.overallVerdict, appLanguage),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    color = itemColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            // Spring-pressed Thanos Snap Disintegration Delete Button
                                            Surface(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        if (deletingItemRawValue == null) {
                                                            deletingItemRawValue = item.indicator.rawValue
                                                        }
                                                    }
                                                    .pointerHoverIcon(PointerIcon.Hand)
                                                    .springPress(),
                                                color = Color.White.copy(alpha = 0.08f),
                                                shape = CircleShape
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "✕",
                                                        color = Color(0xFFEF4444),
                                                        fontSize = 11.sp,
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
        }
    }
}

/**
 * Desktop Wide Recent Triage HUD Feed: Displays interactive recent scans and telemetry right on the main dashboard.
 */
@Composable
private fun RecentTriageFeedHUD(
    history: List<TriageVerdict>,
    appLanguage: AppLanguage,
    strings: AppStrings,
    onSelect: (TriageVerdict) -> Unit,
    onDelete: (TriageVerdict) -> Unit,
    onReScan: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var deletingItemRawValue by remember { mutableStateOf<String?>(null) }
    var pendingDeletedRawValues by remember { mutableStateOf<Set<String>>(emptySet()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = strings.recentTriageFeed,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = PrimaryCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = LocalizationProvider.formatNumber(history.size, appLanguage),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (history.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .defaultMinSize(minHeight = 150.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF090E17).copy(alpha = 0.75f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = PrimaryCyan.copy(alpha = 0.12f),
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "🛡️",
                                    modifier = Modifier.padding(10.dp),
                                    fontSize = 22.sp
                                )
                            }
                            Text(
                                text = strings.noHistory,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = strings.zeroKeyInstantScanReady,
                                style = MaterialTheme.typography.labelSmall,
                                color = VerdictClean,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                val recentScans = history.take(6)
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recentScans.forEach { item ->
                        val isPendingDelete = item.indicator.rawValue in pendingDeletedRawValues
                        if (!isPendingDelete) {
                            val verdictColor = when (item.overallVerdict) {
                                VerdictLevel.CLEAN -> VerdictClean
                                VerdictLevel.LOW_RISK -> VerdictLowRisk
                                VerdictLevel.SUSPICIOUS -> VerdictSuspicious
                                VerdictLevel.MALICIOUS -> VerdictMalicious
                                VerdictLevel.NO_DATA -> VerdictNoData
                            }

                            DisintegrationContainer(
                                isDisintegrating = deletingItemRawValue == item.indicator.rawValue,
                                accent = verdictColor,
                                particleCount = 1800,
                                collapseHeight = true,
                                durationMs = 1200,
                                onDisintegrated = {
                                    pendingDeletedRawValues = pendingDeletedRawValues + item.indicator.rawValue
                                    onDelete(item)
                                    if (deletingItemRawValue == item.indicator.rawValue) {
                                        deletingItemRawValue = null
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelect(item) }
                                        .pointerHoverIcon(PointerIcon.Hand),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF0B1322).copy(alpha = 0.8f),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, verdictColor.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f).padding(end = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Surface(
                                                color = verdictColor.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(8.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, verdictColor.copy(alpha = 0.6f))
                                            ) {
                                                Text(
                                                    text = LocalizationProvider.getVerdictName(item.overallVerdict, appLanguage),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    color = verdictColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Column {
                                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                                    ControlledMarqueeText(
                                                        text = item.indicator.rawValue,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontFamily = FontFamily.Monospace,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                                Text(
                                                    text = "${LocalizationProvider.getIndicatorTypeName(item.indicator.type, appLanguage)} • ${strings.threatConfidenceScore}: ${LocalizationProvider.formatScore(item.confidenceScore, appLanguage)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF64748B),
                                                    fontSize = 10.5.sp
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { onReScan(item.indicator.rawValue) }
                                                    .pointerHoverIcon(PointerIcon.Hand),
                                                color = PrimaryCyan.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(8.dp),
                                                border = androidx.compose.foundation.BorderStroke(0.8.dp, PrimaryCyan.copy(alpha = 0.5f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(text = "⚡", fontSize = 11.sp)
                                                    Text(
                                                        text = strings.reAnalyze,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = PrimaryCyan,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }

                                            // Spring-pressed Thanos Dust Particle Snap Deletion Button
                                            Surface(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        if (deletingItemRawValue == null) {
                                                            deletingItemRawValue = item.indicator.rawValue
                                                        }
                                                    }
                                                    .pointerHoverIcon(PointerIcon.Hand)
                                                    .springPress(),
                                                color = Color.White.copy(alpha = 0.08f),
                                                shape = CircleShape
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "✕",
                                                        color = Color(0xFFEF4444),
                                                        fontSize = 11.sp,
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
        }
    }
}

/**
 * Cyber Telemetry Footer: Transparent status pill showing serverless client architecture, no DB, and active privacy invariants.
 */
@Composable
private fun CyberTelemetryFooter(
    strings: AppStrings,
    isMobileOrTablet: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF070B14).copy(alpha = 0.85f),
            border = androidx.compose.foundation.BorderStroke(0.8.dp, PrimaryCyan.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(VerdictClean)
                )
                ControlledMarqueeText(
                    text = strings.serverlessStatus,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontSize = if (isMobileOrTablet) 9.5.sp else 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}
