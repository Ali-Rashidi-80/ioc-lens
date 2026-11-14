package io.github.mrcoder20.ioclens.presentation.home
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.expandHorizontally
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.graphics.RectangleShape
import io.github.mrcoder20.ioclens.data.remote.createZeroKeyHttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import ioclens.shared.generated.resources.Res
import ioclens.shared.generated.resources.app_logo_mark
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
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
import io.github.mrcoder20.ioclens.core.components.ClipboardVectorIcon
import io.github.mrcoder20.ioclens.core.components.ControlledMarqueeText
import io.github.mrcoder20.ioclens.core.components.DisintegrationContainer
import io.github.mrcoder20.ioclens.core.components.FileDocumentVectorIcon
import io.github.mrcoder20.ioclens.core.components.HistoryVectorIcon
import io.github.mrcoder20.ioclens.core.components.LiquidGlowBackground
import io.github.mrcoder20.ioclens.core.components.ScannerVectorIcon
import io.github.mrcoder20.ioclens.core.components.SettingsVectorIcon
import io.github.mrcoder20.ioclens.core.components.ShieldVerdictVectorIcon
import io.github.mrcoder20.ioclens.core.components.UploadCloudVectorIcon
import io.github.mrcoder20.ioclens.core.components.rememberFilePickerLauncher
import io.github.mrcoder20.ioclens.core.components.springPress
import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.AppStrings
import io.github.mrcoder20.ioclens.core.localization.LocalizationProvider
import io.github.mrcoder20.ioclens.core.theme.BackgroundDark
import io.github.mrcoder20.ioclens.core.theme.CardDark
import io.github.mrcoder20.ioclens.core.theme.DangerNeon
import io.github.mrcoder20.ioclens.core.theme.PlaceholderHighContrast
import io.github.mrcoder20.ioclens.core.theme.PrimaryCyan
import io.github.mrcoder20.ioclens.core.theme.PrimaryNeon
import io.github.mrcoder20.ioclens.core.theme.SurfaceDark
import io.github.mrcoder20.ioclens.core.theme.topBorderHighlightBrush
import io.github.mrcoder20.ioclens.core.theme.cardGlassBackdropBrush
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
import io.github.mrcoder20.ioclens.presentation.settings.SettingsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val strings = LocalizationProvider.getStrings(uiState.appLanguage)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val filePickerLauncher = rememberFilePickerLauncher { path, name ->
        viewModel.onEvent(HomeEvent.FileDropped(path, name))
    }

    // Android Back Button / Gesture Interceptor
    val isAnySheetOpen = uiState.isSettingsSheetOpen || uiState.isHistorySheetOpen || drawerState.isOpen || uiState.isLoading || uiState.activeVerdict != null || uiState.selectedBottomNavTab != 0
    AppBackHandler(enabled = isAnySheetOpen) {
        when {
            uiState.isSettingsSheetOpen -> viewModel.onEvent(HomeEvent.SetSettingsSheetOpen(false))
            uiState.isHistorySheetOpen -> viewModel.onEvent(HomeEvent.SetHistorySheetOpen(false))
            drawerState.isOpen -> coroutineScope.launch { drawerState.close() }
            uiState.isLoading -> viewModel.onEvent(HomeEvent.CancelScan)
            uiState.selectedBottomNavTab != 0 -> viewModel.onEvent(HomeEvent.SelectBottomNavTab(0))
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
                            .background(brush = Brush.verticalGradient(colors = listOf(BackgroundDark, CardDark)))
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
                            Image(
                                painter = painterResource(Res.drawable.app_logo_mark),
                                contentDescription = strings.appTitle,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
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
                        icon = {
                            ScannerVectorIcon(
                                tint = if (uiState.selectedBottomNavTab == 0) PrimaryCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 20.dp
                            )
                        },
                        label = { Text(text = strings.menuHome, fontWeight = FontWeight.SemiBold) },
                        selected = uiState.selectedBottomNavTab == 0,
                        onClick = {
                            viewModel.onEvent(HomeEvent.SelectBottomNavTab(0))
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    NavigationDrawerItem(
                        icon = {
                            HistoryVectorIcon(
                                tint = if (uiState.selectedBottomNavTab == 1) PrimaryCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 20.dp
                            )
                        },
                        label = {
                            Text(
                                text = "${strings.scanHistoryTitle} (${LocalizationProvider.formatNumber(uiState.scanHistory.size, uiState.appLanguage)})",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        selected = uiState.selectedBottomNavTab == 1,
                        onClick = {
                            viewModel.onEvent(HomeEvent.SelectBottomNavTab(1))
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    NavigationDrawerItem(
                        icon = {
                            SettingsVectorIcon(
                                tint = if (uiState.selectedBottomNavTab == 2) PrimaryCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 20.dp
                            )
                        },
                        label = { Text(text = strings.menuSettings, fontWeight = FontWeight.SemiBold) },
                        selected = uiState.selectedBottomNavTab == 2,
                        onClick = {
                            viewModel.onEvent(HomeEvent.SelectBottomNavTab(2))
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        },
        modifier = modifier
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val isWideScreen = screenWidth >= 840.dp
            val isMediumScreen = screenWidth >= 560.dp && screenWidth < 840.dp
            val isCompactScreen = screenWidth < 600.dp
            val isMobileOrTablet = screenWidth < 840.dp

            // Ambient Top Glow (Subtle translucent cyber accent wash)
            val ambientGlowBrush = remember {
                Brush.verticalGradient(
                    colors = listOf(
                        PrimaryNeon.copy(alpha = 0.08f),
                        Color.Transparent
                    )
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(brush = ambientGlowBrush)
            )

            // Dedicated Full-Screen Settings Container (Outside scrolling dashboard to prevent Compose infinite height constraint crash)
            if (uiState.selectedBottomNavTab == 2) {
                SettingsScreen(
                    settingsRepository = settingsRepository,
                    isMobileOrTablet = isMobileOrTablet,
                    isFullScreen = true,
                    onClose = { viewModel.onEvent(HomeEvent.SelectBottomNavTab(0)) }
                )
            } else {
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
                        }
                    }

                    // Center Slot: Dead-Centered App Title with Strict LTR BiDi Protection
                    Box(
                        modifier = Modifier.align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    painter = painterResource(Res.drawable.app_logo_mark),
                                    contentDescription = strings.appTitle,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
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
                                    .springPress()
                                    .clickable {
                                        val nextTab = if (uiState.selectedBottomNavTab == 1) 0 else 1
                                        viewModel.onEvent(HomeEvent.SelectBottomNavTab(nextTab))
                                    },
                                color = if (uiState.selectedBottomNavTab == 1) PrimaryCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HistoryVectorIcon(tint = PrimaryCyan, size = 16.dp)
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
                                    .springPress()
                                    .clickable {
                                        val nextTab = if (uiState.selectedBottomNavTab == 2) 0 else 2
                                        viewModel.onEvent(HomeEvent.SelectBottomNavTab(nextTab))
                                    },
                                color = if (uiState.selectedBottomNavTab == 2) PrimaryCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.35f))
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SettingsVectorIcon(tint = PrimaryCyan, size = 16.dp)
                                }
                            }
                        }
                    }
                }

                if (isWideScreen) {
                    when (uiState.selectedBottomNavTab) {
                        1 -> {
                            HistoryFullScreenView(
                                history = uiState.scanHistory,
                                appLanguage = uiState.appLanguage,
                                onSelect = { item ->
                                    viewModel.onEvent(HomeEvent.HistoryItemClicked(item))
                                    coroutineScope.launch { scrollState.scrollTo(0) }
                                },
                                onDelete = { item ->
                                    viewModel.onEvent(HomeEvent.DeleteHistoryItem(item))
                                },
                                onClear = { viewModel.onEvent(HomeEvent.ClearHistory) }
                            )
                        }
                        else -> {
                            // ADAPTIVE UNIFIED DASHBOARD (Desktop & Wide Displays)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // 1. Dual-Input Scanner Hub (Side-by-side with balanced heights)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Max),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    SearchCard(
                                        uiState = uiState,
                                        strings = strings,
                                        isCompact = false,
                                        onQueryChange = { viewModel.onEvent(HomeEvent.QueryChanged(it)) },
                                        onClear = { viewModel.onEvent(HomeEvent.ClearClicked) },
                                        onCancel = { viewModel.onEvent(HomeEvent.CancelScan) },
                                        onAnalyze = { viewModel.onEvent(HomeEvent.AnalyzeClicked) },
                                        onOpenFilePicker = filePickerLauncher,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )

                                    FileScannerCard(
                                        uiState = uiState,
                                        strings = strings,
                                        onOpenFilePicker = filePickerLauncher,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        isCompact = false
                                    )
                                }

                                // 2. File Hashing Progress Banner (Full Width)
                                FileHashingBanner(uiState = uiState, appLanguage = uiState.appLanguage)

                                // 3. Auto-Dismissing Error Message Banner (Full Width)
                                ErrorMessageBanner(errorMessage = uiState.errorMessage)

                                // 4. Live Multi-Engine Scan Banner (Full Width)
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

                                // 5. Overall Threat Verdict & Intelligence Report (Full Width!)
                                AnimatedVisibility(
                                    visible = uiState.activeVerdict != null,
                                    enter = fadeIn(tween(350)) + expandVertically(tween(350)),
                                    exit = fadeOut(tween(250)) + shrinkVertically(tween(250))
                                ) {
                                    uiState.activeVerdict?.let { verdict ->
                                        OverallVerdictCard(verdict = verdict, appLanguage = uiState.appLanguage)
                                    }
                                }


                            }
                        }
                    }
                } else {
                    // ADAPTIVE MOBILE & TABLET SINGLE-COLUMN VIEW
                    AnimatedContent(
                        targetState = uiState.selectedBottomNavTab,
                        transitionSpec = {
                            val isRtl = uiState.appLanguage.isRtl
                            if (targetState > initialState) {
                                (slideInHorizontally { width -> if (isRtl) -width / 4 else width / 4 } + fadeIn(tween(250)))
                                    .togetherWith(slideOutHorizontally { width -> if (isRtl) width / 4 else -width / 4 } + fadeOut(tween(200)))
                            } else {
                                (slideInHorizontally { width -> if (isRtl) width / 4 else -width / 4 } + fadeIn(tween(250)))
                                    .togetherWith(slideOutHorizontally { width -> if (isRtl) -width / 4 else width / 4 } + fadeOut(tween(200)))
                            }
                        },
                        label = "tabContentTransition"
                    ) { targetTab ->
                        when (targetTab) {
                            0 -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    SearchCard(
                                        uiState = uiState,
                                        strings = strings,
                                        isCompact = isCompactScreen,
                                        onQueryChange = { viewModel.onEvent(HomeEvent.QueryChanged(it)) },
                                        onClear = { viewModel.onEvent(HomeEvent.ClearClicked) },
                                        onCancel = { viewModel.onEvent(HomeEvent.CancelScan) },
                                        onAnalyze = { viewModel.onEvent(HomeEvent.AnalyzeClicked) },
                                        onOpenFilePicker = filePickerLauncher
                                    )

                                    FileScannerCard(
                                        uiState = uiState,
                                        strings = strings,
                                        onOpenFilePicker = filePickerLauncher,
                                        modifier = Modifier.fillMaxWidth(),
                                        isCompact = true
                                    )

                                    FileHashingBanner(uiState = uiState, appLanguage = uiState.appLanguage)

                                    ErrorMessageBanner(errorMessage = uiState.errorMessage)

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

                                    AnimatedVisibility(
                                        visible = uiState.activeVerdict != null,
                                        enter = fadeIn(tween(350)) + expandVertically(tween(350)),
                                        exit = fadeOut(tween(250)) + shrinkVertically(tween(250))
                                    ) {
                                        uiState.activeVerdict?.let { verdict ->
                                            OverallVerdictCard(verdict = verdict, appLanguage = uiState.appLanguage)
                                        }
                                    }
                                }
                            }
                            1 -> {
                                HistoryFullScreenView(
                                    history = uiState.scanHistory,
                                    appLanguage = uiState.appLanguage,
                                    onSelect = { item ->
                                        viewModel.onEvent(HomeEvent.HistoryItemClicked(item))
                                        coroutineScope.launch { scrollState.scrollTo(0) }
                                    },
                                    onDelete = { item ->
                                        viewModel.onEvent(HomeEvent.DeleteHistoryItem(item))
                                    },
                                    onClear = { viewModel.onEvent(HomeEvent.ClearHistory) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isMobileOrTablet) 84.dp else 48.dp))
            }
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
                    onClose = { viewModel.onEvent(HomeEvent.SetSettingsSheetOpen(false)) }
                )
            }

            // Mobile & Tablet Adaptive Cyberpunk Bottom Navigation Bar
            if (isMobileOrTablet) {
                CyberpunkBottomNavBar(
                    selectedTab = uiState.selectedBottomNavTab,
                    historyCount = uiState.scanHistory.size,
                    appLanguage = uiState.appLanguage,
                    strings = strings,
                    onTabSelected = { tab ->
                        viewModel.onEvent(HomeEvent.SelectBottomNavTab(tab))
                        coroutineScope.launch { scrollState.scrollTo(0) }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
            // Desktop Live Threat Intel Engines Ping & Connectivity Status Bar (Docked at the very bottom of the window)
            if (!isMobileOrTablet && uiState.selectedBottomNavTab != 2) {
                DesktopEngineConnectivityStatusBar(
                    appLanguage = uiState.appLanguage,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
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
    val infiniteTransition = rememberInfiniteTransition(label = "LiveScanRadar")

    // Rotating radar beam (deferred to graphicsLayer to prevent parent card recomposition)
    val radarRotationState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarRotation"
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
                        Canvas(modifier = Modifier.size(28.dp).graphicsLayer { rotationZ = radarRotationState.value }) {
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
                Pair(0, strings.tabScanner),
                Pair(1, strings.tabHistory),
                Pair(2, strings.tabSettings)
            )

            tabs.forEach { (index, label) ->
                val isSelected = selectedTab == index
                val activeColor = PrimaryCyan
                val inactiveColor = Color(0xFF7E8B9F)

                val indicatorWidth by animateDpAsState(
                    targetValue = if (isSelected) 22.dp else 0.dp,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
                    label = "tabIndicatorWidth_$index"
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.18f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
                    label = "tabIconScale_$index"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .springPress()
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
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                            ) {
                                when (index) {
                                    0 -> ScannerVectorIcon(
                                        tint = if (isSelected) activeColor else inactiveColor,
                                        size = 18.dp
                                    )
                                    1 -> HistoryVectorIcon(
                                        tint = if (isSelected) activeColor else inactiveColor,
                                        size = 18.dp
                                    )
                                    else -> SettingsVectorIcon(
                                        tint = if (isSelected) activeColor else inactiveColor,
                                        size = 18.dp
                                    )
                                }
                            }
                            if (index == 1 && historyCount > 0) {
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
                                .width(indicatorWidth)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (indicatorWidth > 1.dp) PrimaryCyan else Color.Transparent)
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
            colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.90f)),
            border = BorderStroke(1.dp, topBorderHighlightBrush(PrimaryNeon, isDark = true)),
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
                    trackColor = BackgroundDark
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
    onAnalyze: () -> Unit,
    onOpenFilePicker: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var isHovered by remember { mutableStateOf(false) }
    val pulseGlowState = if (uiState.queryInput.isNotBlank()) {
        val infiniteTransition = rememberInfiniteTransition(label = "scanPulse")
        infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseGlow"
        )
    } else null

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        isHovered = event.type != PointerEventType.Exit
                    }
                }
            },
        shape = RoundedCornerShape(16.dp),
        color = CardDark.copy(alpha = if (isHovered) 0.95f else 0.85f),
        border = BorderStroke(
            1.dp,
            if (isHovered) Brush.linearGradient(listOf(PrimaryCyan, PrimaryCyan.copy(alpha = 0.45f)))
            else topBorderHighlightBrush(PrimaryNeon, isDark = true)
        ),
        shadowElevation = if (isHovered) 8.dp else 4.dp
    ) {
        Column(
            modifier = if (isCompact) Modifier.fillMaxWidth().padding(18.dp) else Modifier.fillMaxSize().padding(18.dp)
        ) {
            // Header matching FileScannerCard perfectly
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    ScannerVectorIcon(tint = PrimaryCyan, size = 20.dp)
                }
                Column {
                    Text(
                        text = when (uiState.appLanguage) {
                            AppLanguage.PERSIAN -> "پویش نشانگر و هوش تهدیدات"
                            AppLanguage.RUSSIAN -> "Анализ IOC и индикаторов"
                            else -> "IOC & Payload Analysis"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = when (uiState.appLanguage) {
                            AppLanguage.PERSIAN -> "تحلیل چندموتوره و بلادرنگ IP، دامنه، URL و هش"
                            AppLanguage.RUSSIAN -> "Мгновенный анализ IP, доменов, URL и хешей"
                            else -> "Real-time zero-key triage for IP, Domain, URL & Hash"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Input Field
            OutlinedTextField(
                value = uiState.queryInput,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(
                    textDirection = TextDirection.Ltr,
                    fontFamily = FontFamily.Monospace,
                    fontSize = if (isCompact) 13.5.sp else 14.5.sp,
                    color = Color.White
                ),
                placeholder = {
                    Text(
                        text = strings.searchFieldPlaceholder,
                        color = PlaceholderHighContrast,
                        fontSize = if (isCompact) 12.sp else 13.sp,
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

            Spacer(modifier = Modifier.height(14.dp))
            if (!isCompact) {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Actions Row: Paste & Scan Payload
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
                    modifier = Modifier.springPress(),
                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryCyan)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ClipboardVectorIcon(tint = PrimaryCyan, size = 15.dp)
                        Text(
                            text = strings.pasteFromClipboard,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (uiState.isLoading) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = VerdictMalicious.copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(38.dp).springPress()
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
                        enabled = uiState.queryInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryCyan,
                            contentColor = Color.Black,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = Color.White.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = if (uiState.queryInput.isNotBlank()) BorderStroke(1.5.dp, PrimaryCyan.copy(alpha = pulseGlowState?.value ?: 0.35f)) else null,
                        modifier = Modifier.height(38.dp).springPress()
                    ) {
                        Text(
                            text = strings.scanPayload,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dedicated File Triage & SHA-256 Scanner Card on Scanner Page.
 * Computes SHA-256 hash locally in client memory with zero cloud uploads.
 */
@Composable
private fun FileScannerCard(
    uiState: HomeUiState,
    strings: AppStrings,
    onOpenFilePicker: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    var isCardHovered by remember { mutableStateOf(false) }
    val dropInteractionSource = remember { MutableInteractionSource() }
    val isDropHovered by dropInteractionSource.collectIsHoveredAsState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        isCardHovered = event.type != PointerEventType.Exit
                    }
                }
            },
        color = CardDark.copy(alpha = if (isCardHovered) 0.95f else 0.85f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isCardHovered) Brush.linearGradient(listOf(PrimaryCyan, PrimaryCyan.copy(alpha = 0.45f)))
            else topBorderHighlightBrush(PrimaryNeon, isDark = true)
        ),
        shadowElevation = if (isCardHovered) 8.dp else 4.dp
    ) {
        Column(
            modifier = if (isCompact) Modifier.fillMaxWidth().padding(18.dp) else Modifier.fillMaxSize().padding(18.dp)
        ) {
            // Header: Icon + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    FileDocumentVectorIcon(tint = PrimaryCyan, size = 20.dp)
                }
                Column {
                    Text(
                        text = when (uiState.appLanguage) {
                            AppLanguage.PERSIAN -> "پویش و تحلیل فایل (SHA-256)"
                            AppLanguage.RUSSIAN -> "Анализ файла (SHA-256)"
                            else -> "File Triage & SHA-256 Scanner"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = when (uiState.appLanguage) {
                            AppLanguage.PERSIAN -> "محاسبه کاملاً آفلاین هش بدون آپلود فایل"
                            AppLanguage.RUSSIAN -> "Локальное вычисление хеша без загрузки файла"
                            else -> "100% Client-side local hashing, zero cloud upload"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Drop / Select Box Area (Fills remaining height harmoniously on desktop, solid minimum height on mobile)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isCompact) {
                            Modifier.heightIn(min = 125.dp)
                        } else {
                            Modifier.weight(1f).defaultMinSize(minHeight = 110.dp)
                        }
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .springPress(interactionSource = dropInteractionSource)
                    .clickable(
                        interactionSource = dropInteractionSource,
                        indication = null,
                        onClick = onOpenFilePicker
                    )
                    .pointerHoverIcon(PointerIcon.Hand),
                shape = RoundedCornerShape(12.dp),
                color = if (isDropHovered) PrimaryCyan.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                border = BorderStroke(1.5.dp, if (isDropHovered) PrimaryCyan else PrimaryCyan.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimaryCyan.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        UploadCloudVectorIcon(tint = PrimaryCyan, size = 22.dp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = strings.dropFileHint,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryCyan,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = when (uiState.appLanguage) {
                            AppLanguage.PERSIAN -> "پشتیبانی از انواع فایل‌ها: اجرایی (EXE/DLL)، اسناد (PDF/Office)، فشرده (ZIP/RAR) و..."
                            AppLanguage.RUSSIAN -> "Поддержка любых файлов: исполняемые, документы, архивы и др."
                            else -> "Supports all formats: Executables, Documents, Archives, Scripts"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorMessageBanner(errorMessage: String?) {
    AnimatedVisibility(
        visible = !errorMessage.isNullOrBlank(),
        enter = fadeIn(animationSpec = tween(250)) + expandVertically(animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(350)) + shrinkVertically(animationSpec = tween(350))
    ) {
        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(10.dp),
                color = DangerNeon.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, DangerNeon.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚠",
                        color = DangerNeon,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = errorMessage,
                        color = DangerNeon,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
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
        colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.92f)),
        border = BorderStroke(1.5.dp, verdictColor.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(verdictColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        ShieldVerdictVectorIcon(
                            tint = verdictColor,
                            size = 24.dp
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            ControlledMarqueeText(
                                text = verdict.indicator.rawValue,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.5.sp
                                ),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${strings.indicatorTypeLabel}: ${LocalizationProvider.getIndicatorTypeName(verdict.indicator.type, appLanguage)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Surface(
                    color = verdictColor.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, verdictColor)
                ) {
                    Text(
                        text = LocalizationProvider.getVerdictName(verdict.overallVerdict, appLanguage),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        color = verdictColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = LocalizationProvider.getLocalizedVerdictSummary(verdict, appLanguage),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(14.dp))

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
            Spacer(modifier = Modifier.height(6.dp))
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
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
                    "Shodan InternetDB" to true,
                    "AlienVault OTX" to true,
                    "RIPE Stat" to true,
                    "crt.sh" to true,
                    "Feodo Tracker (abuse.ch)" to true
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
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
        "Shodan InternetDB" -> type == IndicatorType.IP_ADDRESS
        "AlienVault OTX" -> true
        "RIPE Stat" -> type == IndicatorType.IP_ADDRESS
        "crt.sh" -> type == IndicatorType.DOMAIN
        "ipwho.is" -> type == IndicatorType.IP_ADDRESS
        "VirusTotal" -> true
        "AbuseIPDB" -> type == IndicatorType.IP_ADDRESS
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
        hasActiveVerdict -> if (isTwoColumn) strings.statusKeyRequiredShort else strings.statusKeyRequired
        isZeroKey -> if (isTwoColumn) strings.statusReady else strings.statusReadyZeroKey
        else -> if (isTwoColumn) strings.statusKeyRequiredShort else strings.statusKeyRequired
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
private fun HistoryFullScreenView(
    history: List<TriageVerdict>,
    appLanguage: AppLanguage,
    onSelect: (TriageVerdict) -> Unit,
    onDelete: (TriageVerdict) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalizationProvider.getStrings(appLanguage)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<IndicatorType?>(null) }
    var deletingItemRawValue by remember { mutableStateOf<String?>(null) }
    var pendingDeletedRawValues by remember { mutableStateOf<Set<String>>(emptySet()) }

    val filteredHistory = remember(history, searchQuery, selectedTypeFilter, pendingDeletedRawValues) {
        history.filter { item ->
            item.indicator.rawValue !in pendingDeletedRawValues &&
                (searchQuery.isBlank() || item.indicator.rawValue.contains(searchQuery.trim(), ignoreCase = true)) &&
                (selectedTypeFilter == null || item.indicator.type == selectedTypeFilter)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (history.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isRtl) Arrangement.Start else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onClear,
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Text(
                        text = strings.clearHistory,
                        color = VerdictMalicious,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Search & Filter Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = strings.searchHistoryPlaceholder,
                    color = PlaceholderHighContrast,
                    fontSize = 12.5.sp
                )
            },
            leadingIcon = {
                ScannerVectorIcon(
                    size = 16.dp,
                    tint = PrimaryCyan.copy(alpha = 0.85f)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Text(text = "✕", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = PrimaryCyan,
                unfocusedBorderColor = Color(0xFF1E293B)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Filter chips (ALL, IP, DOMAIN, URL, HASH)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filterOptions = listOf(
                null to strings.filterAll,
                IndicatorType.IP_ADDRESS to "IP",
                IndicatorType.DOMAIN to "DOMAIN",
                IndicatorType.URL to "URL",
                IndicatorType.FILE_HASH to "HASH"
            )

            filterOptions.forEach { (type, label) ->
                val isSelected = selectedTypeFilter == type
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedTypeFilter = type }
                        .pointerHoverIcon(PointerIcon.Hand),
                    color = if (isSelected) PrimaryCyan.copy(alpha = 0.22f) else Color(0xFF0D1424),
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        if (isSelected) PrimaryCyan else Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = if (isSelected) PrimaryCyan else Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        // History items list or empty states
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "📜", fontSize = 36.sp)
                    Text(
                        text = strings.noHistory,
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else if (filteredHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.noFilterResults,
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredHistory.forEach { item ->
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
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0D1424))
                                    .border(0.6.dp, itemColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
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
                                    Spacer(modifier = Modifier.height(2.dp))
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
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            color = itemColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }

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
                            text = "${strings.scanHistoryTitle} (${LocalizationProvider.formatNumber(history.size, appLanguage)})",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryCyan,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        // In LTR, Title first, then Clear and Close button on the Right (End)
                        Text(
                            text = "${strings.scanHistoryTitle} (${LocalizationProvider.formatNumber(history.size, appLanguage)})",
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
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
                        .defaultMinSize(minHeight = 150.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF090E17).copy(alpha = 0.75f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                    text = "🗄️",
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
                    modifier = Modifier.fillMaxWidth(),
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
                                        .springPress()
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
                                                    .springPress()
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

private data class EngineProbeStatus(
    val id: String,
    val name: String,
    val probeUrl: String,
    val isOnline: Boolean = true,
    val pingMs: Long = 32L
)

private val DEFAULT_PROBE_ENGINES = listOf(
    EngineProbeStatus("urlhaus", "URLhaus", "https://urlhaus-api.abuse.ch/v1/url/", true, 32),
    EngineProbeStatus("mb", "MalwareBazaar", "https://mb-api.abuse.ch/api/v1/", true, 42),
    EngineProbeStatus("tf", "ThreatFox", "https://threatfox-api.abuse.ch/api/v1/", true, 38),
    EngineProbeStatus("feodo", "Feodo Tracker", "https://feodotracker.abuse.ch/downloads/ipblocklist.json", true, 35),
    EngineProbeStatus("shodan", "Shodan DB", "https://internetdb.shodan.io/1.1.1.1", true, 44),
    EngineProbeStatus("otx", "AlienVault", "https://otx.alienvault.com/", true, 58),
    EngineProbeStatus("cf", "Cloudflare DoH", "https://cloudflare-dns.com/dns-query", true, 14),
    EngineProbeStatus("rdap", "RDAP", "https://rdap.org/ip/1.1.1.1", true, 46),
    EngineProbeStatus("ripe", "RIPE Stat", "https://stat.ripe.net/data/routing-status/data.json?resource=1.1.1.1", true, 52),
    EngineProbeStatus("crt", "crt.sh", "https://crt.sh/?q=github.com&output=json", true, 65)
)

/**
 * Desktop Live Threat Intel Engines Ping & Connectivity Status Bar.
 *
 * Grounded at the absolute bottom of the desktop window:
 * - Live Ping / Latency measurement for all 10 Zero-Key Engines
 * - Interactive [Ping All] probe trigger
 * - Scrollable micro-latency chips with LTR protection against character breaking
 * - Encrypted TLS & Zero Cloud Leak privacy invariants
 */
@Composable
private fun StatusBarNavChevron(
    isLeft: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (isHovered) 1.10f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isHovered) 0.32f else 0.10f,
        animationSpec = tween(140)
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (isHovered) 0.95f else 0.40f,
        animationSpec = tween(140)
    )

    Surface(
        modifier = modifier
            .size(24.dp)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        isHovered = event.type != PointerEventType.Exit
                    }
                }
            },
        color = Color(0xEE0B132B),
        border = BorderStroke(0.9.dp, PrimaryCyan.copy(alpha = borderAlpha)),
        shape = RoundedCornerShape(6.dp),
        shadowElevation = if (isHovered) 3.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PrimaryCyan.copy(alpha = glowAlpha)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(10.dp)) {
                val strokeWidth = 1.9.dp.toPx()
                val path = Path()
                if (isLeft) {
                    path.moveTo(size.width * 0.65f, size.height * 0.16f)
                    path.lineTo(size.width * 0.30f, size.height * 0.5f)
                    path.lineTo(size.width * 0.65f, size.height * 0.84f)
                } else {
                    path.moveTo(size.width * 0.35f, size.height * 0.16f)
                    path.lineTo(size.width * 0.70f, size.height * 0.5f)
                    path.lineTo(size.width * 0.35f, size.height * 0.84f)
                }
                drawPath(
                    path = path,
                    color = PrimaryCyan,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

/**
 * Desktop Live Threat Intel Engines Ping & Connectivity Status Bar.
 *
 * Grounded at the absolute bottom of the desktop window:
 * - Live Ping / Latency measurement for all 10 Zero-Key Engines
 * - Interactive [Ping All] probe trigger
 * - Smooth Mouse Wheel & Drag-to-Scroll navigation
 * - Precision vector chevrons and gradient fade masks
 * - Strict BiDi / LTR character isolation (zero letter breaking)
 */
@Composable
private fun DesktopEngineConnectivityStatusBar(
    appLanguage: AppLanguage,
    modifier: Modifier = Modifier
) {
    var engineStatuses by remember { mutableStateOf(DEFAULT_PROBE_ENGINES) }
    var isProbing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val chipsScrollState = rememberScrollState()

    val onlineEngines = remember(engineStatuses) { engineStatuses.filter { it.isOnline } }
    val avgPing = remember(onlineEngines) {
        if (onlineEngines.isNotEmpty()) {
            onlineEngines.map { it.pingMs }.average().toLong()
        } else {
            0L
        }
    }
    val onlineCount = onlineEngines.size
    val totalCount = engineStatuses.size

    val canScrollLeft by remember { derivedStateOf { chipsScrollState.value > 0 } }
    val canScrollRight by remember { derivedStateOf { chipsScrollState.value < chipsScrollState.maxValue } }

    fun probeAllEngines() {
        if (isProbing) return
        isProbing = true
        coroutineScope.launch(Dispatchers.Default) {
            val client = createZeroKeyHttpClient(timeoutMs = 4000L)
            try {
                val updated = engineStatuses.map { engine ->
                    async {
                        val mark = kotlin.time.TimeSource.Monotonic.markNow()
                        val online = try {
                            val resp = client.get(engine.probeUrl)
                            resp.status.value in 200..499
                        } catch (_: Exception) {
                            false
                        }
                        val elapsed = mark.elapsedNow().inWholeMilliseconds.coerceAtLeast(10L)
                        engine.copy(isOnline = online, pingMs = if (online) elapsed else 0L)
                    }
                }.awaitAll()
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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp),
        color = Color(0xFA080E1C),
        border = BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.22f)),
        shape = RectangleShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Slot 1: Master Health & Probe Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pulsing Green LED
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (onlineCount > 0) Color(0xFF10B981) else Color(0xFFEF4444))
                )

                Text(
                    text = when (appLanguage) {
                        AppLanguage.PERSIAN -> {
                            val onlineFmt = LocalizationProvider.formatNumber(onlineCount, appLanguage)
                            val totalFmt = LocalizationProvider.formatNumber(totalCount, appLanguage)
                            val pingFmt = LocalizationProvider.formatNumber(avgPing.toInt(), appLanguage)
                            "$onlineFmt از $totalFmt موتور فعال • میانگین: ${pingFmt}ms"
                        }
                        AppLanguage.RUSSIAN -> "$onlineCount/$totalCount онлайн • Средний: ${avgPing}ms"
                        else -> "$onlineCount/$totalCount Engines Online • Avg: ${avgPing}ms"
                    },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (onlineCount > 0) Color(0xFFE2E8F0) else Color(0xFFFCA5A5),
                    maxLines = 1,
                    softWrap = false
                )

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = !isProbing) { probeAllEngines() }
                        .pointerHoverIcon(if (isProbing) PointerIcon.Default else PointerIcon.Hand),
                    color = if (isProbing) PrimaryCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(0.8.dp, PrimaryCyan.copy(alpha = 0.45f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = if (isProbing) "⚡" else "🔄",
                            fontSize = 10.sp
                        )
                        Text(
                            text = when {
                                isProbing && appLanguage == AppLanguage.PERSIAN -> "در حال پینگ..."
                                isProbing && appLanguage == AppLanguage.RUSSIAN -> "Проверка..."
                                isProbing -> "Probing..."
                                appLanguage == AppLanguage.PERSIAN -> "تست پینگ"
                                appLanguage == AppLanguage.RUSSIAN -> "Тест пинга"
                                else -> "Ping All"
                            },
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryCyan,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Subtle vertical separator
            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .width(1.dp)
                    .height(18.dp)
                    .background(Color.White.copy(alpha = 0.12f))
            )

            // Slot 2: Full-Width Scrollable & Draggable Threat Engine Latency Chips (Zero Character Breaking)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left scroll chevron button with animated entry/exit
                    AnimatedVisibility(
                        visible = canScrollLeft,
                        enter = fadeIn(tween(150)) + expandHorizontally(),
                        exit = fadeOut(tween(150)) + shrinkHorizontally()
                    ) {
                        StatusBarNavChevron(
                            isLeft = true,
                            onClick = {
                                coroutineScope.launch {
                                    chipsScrollState.animateScrollBy(-220f)
                                }
                            },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }

                    // Left edge gradient fade when scrolled
                    if (canScrollLeft) {
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(26.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFA080E1C), Color.Transparent)
                                    )
                                )
                        )
                    }

                    // Chips list with Mouse Wheel Scroll + Mouse/Touch Drag-to-Scroll
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        if (event.type == PointerEventType.Scroll) {
                                            val change = event.changes.firstOrNull()
                                            if (change != null) {
                                                val delta = if (change.scrollDelta.x != 0f) change.scrollDelta.x else change.scrollDelta.y
                                                if (delta.isFinite() && delta != 0f) {
                                                    chipsScrollState.dispatchRawDelta(delta * 40f)
                                                    change.consume()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { change, dragAmount ->
                                    change.consume()
                                    if (dragAmount.isFinite() && dragAmount != 0f) {
                                        chipsScrollState.dispatchRawDelta(-dragAmount)
                                    }
                                }
                            }
                            .horizontalScroll(chipsScrollState),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        engineStatuses.forEach { engine ->
                            val dotColor = if (engine.isOnline) Color(0xFF10B981) else Color(0xFFEF4444)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.White.copy(alpha = 0.04f),
                                border = BorderStroke(0.7.dp, dotColor.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                    Text(
                                        text = engine.name,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = if (engine.isOnline) "${engine.pingMs}ms" else "ERR",
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (engine.isOnline) Color(0xFF38BDF8) else Color(0xFFEF4444),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // Right edge gradient fade when scrollable
                    if (canScrollRight) {
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(26.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, Color(0xFA080E1C))
                                    )
                                )
                        )
                    }

                    // Right scroll chevron button with animated entry/exit
                    AnimatedVisibility(
                        visible = canScrollRight,
                        enter = fadeIn(tween(150)) + expandHorizontally(),
                        exit = fadeOut(tween(150)) + shrinkHorizontally()
                    ) {
                        StatusBarNavChevron(
                            isLeft = false,
                            onClick = {
                                coroutineScope.launch {
                                    chipsScrollState.animateScrollBy(220f)
                                }
                            },
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
