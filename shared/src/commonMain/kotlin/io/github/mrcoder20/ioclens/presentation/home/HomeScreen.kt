package io.github.mrcoder20.ioclens.presentation.home

import io.github.mrcoder20.ioclens.core.components.rememberBatchFilePickerLauncher
import io.github.mrcoder20.ioclens.core.components.PickedFile
import io.github.mrcoder20.ioclens.domain.model.EngineProbeStatus
import io.github.mrcoder20.ioclens.domain.model.DEFAULT_PROBE_ENGINES
import io.github.mrcoder20.ioclens.domain.model.probeAllEnginesConcurrently
import io.github.mrcoder20.ioclens.presentation.settings.ThreatIntelEnginesDialog
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mrcoder20.ioclens.core.components.AppBackHandler
import io.github.mrcoder20.ioclens.core.components.CameraLensVectorIcon
import io.github.mrcoder20.ioclens.core.components.ClipboardVectorIcon
import io.github.mrcoder20.ioclens.core.components.rememberClipboardActions
import io.github.mrcoder20.ioclens.presentation.vision.CyberLensDialog
import io.github.mrcoder20.ioclens.core.components.ControlledMarqueeText
import io.github.mrcoder20.ioclens.core.components.DisintegrationContainer
import io.github.mrcoder20.ioclens.core.components.FileDocumentVectorIcon
import io.github.mrcoder20.ioclens.core.components.HistoryVectorIcon
import io.github.mrcoder20.ioclens.core.components.ScannerVectorIcon
import io.github.mrcoder20.ioclens.core.components.SettingsVectorIcon
import io.github.mrcoder20.ioclens.core.components.ShieldVerdictVectorIcon
import io.github.mrcoder20.ioclens.core.components.UploadCloudVectorIcon
import io.github.mrcoder20.ioclens.core.components.rememberFilePickerLauncher
import io.github.mrcoder20.ioclens.core.components.springPress
import io.github.mrcoder20.ioclens.core.localization.AppLanguage
import io.github.mrcoder20.ioclens.core.localization.AppStrings
import io.github.mrcoder20.ioclens.core.localization.LocalizationProvider
import io.github.mrcoder20.ioclens.core.theme.IocSemanticColors
import io.github.mrcoder20.ioclens.core.theme.LocalIocColors
import io.github.mrcoder20.ioclens.core.theme.PrimaryNeon
import io.github.mrcoder20.ioclens.core.theme.rememberMonospaceFontFamily
import io.github.mrcoder20.ioclens.core.theme.topBorderHighlightBrush
import io.github.mrcoder20.ioclens.domain.model.Indicator
import io.github.mrcoder20.ioclens.domain.model.IndicatorType
import io.github.mrcoder20.ioclens.domain.model.SourceResult
import io.github.mrcoder20.ioclens.domain.model.TriageVerdict
import io.github.mrcoder20.ioclens.domain.model.VerdictLevel
import io.github.mrcoder20.ioclens.domain.repository.SettingsRepository
import io.github.mrcoder20.ioclens.domain.vision.QuishingAnalyzer
import io.github.mrcoder20.ioclens.logDiagnostic
import io.github.mrcoder20.ioclens.presentation.settings.SettingsScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val exportJsonFormat = Json {
    prettyPrint = true
    encodeDefaults = true
}

internal fun filterScanHistory(
    history: List<TriageVerdict>,
    searchQuery: String = "",
    selectedTypeFilter: IndicatorType? = null,
    selectedVerdictFilter: VerdictLevel? = null,
    excludedRawValues: Set<String> = emptySet()
): List<TriageVerdict> {
    val trimmed = searchQuery.trim()
    return history.filter { item ->
        item.indicator.rawValue !in excludedRawValues &&
            (trimmed.isEmpty() ||
                item.indicator.rawValue.contains(trimmed, ignoreCase = true) ||
                item.indicator.normalizedValue.contains(trimmed, ignoreCase = true) ||
                item.summaryDescription.contains(trimmed, ignoreCase = true)) &&
            (selectedTypeFilter == null || item.indicator.type == selectedTypeFilter) &&
            (selectedVerdictFilter == null || item.overallVerdict == selectedVerdictFilter)
    }
}

private fun verdictColorFor(level: VerdictLevel, palette: IocSemanticColors): Color = when (level) {
    VerdictLevel.CLEAN -> palette.verdictClean
    VerdictLevel.LOW_RISK -> palette.verdictLowRisk
    VerdictLevel.SUSPICIOUS -> palette.verdictSuspicious
    VerdictLevel.MALICIOUS -> palette.verdictMalicious
    VerdictLevel.NO_DATA -> palette.verdictNoData
}

private fun padTwoDigits(value: Int): String = if (value < 10) "0$value" else value.toString()

internal fun formatUtcTimestamp(epochMs: Long): String {
    if (epochMs <= 0L) return ""
    val totalSeconds = epochMs / 1000L
    val days = totalSeconds.floorDiv(86400L)
    val secOfDay = totalSeconds.mod(86400L)
    val hour = (secOfDay / 3600L).toInt()
    val minute = ((secOfDay % 3600L) / 60L).toInt()

    val z = days + 719468L
    val era = (if (z >= 0) z else z - 146096L) / 146097L
    val doe = z - era * 146097L
    val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L
    val y = yoe + era * 400L
    val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
    val mp = (5L * doy + 2L) / 153L
    val day = (doy - (153L * mp + 2L) / 5L + 1L).toInt()
    val month = (mp + if (mp < 10L) 3L else -9L).toInt()
    val year = (y + if (month <= 2) 1L else 0L).toInt()
    return "$year-${padTwoDigits(month)}-${padTwoDigits(day)} ${padTwoDigits(hour)}:${padTwoDigits(minute)} UTC"
}

private fun formatFileSize(bytes: Long, language: AppLanguage): String {
    if (bytes <= 0L) return ""
    val label = when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
        else -> {
            val tenths = (bytes * 10L) / (1024L * 1024L)
            "${tenths / 10}.${tenths % 10} MB"
        }
    }
    return if (language == AppLanguage.PERSIAN) LocalizationProvider.toPersianDigits(label) else label
}

private fun buildVerdictMarkdownReport(verdict: TriageVerdict, appLanguage: AppLanguage): String {
    val strings = LocalizationProvider.getStrings(appLanguage)
    val sb = StringBuilder()
    sb.appendLine("# IOC Lens Triage Report")
    sb.appendLine("- **Indicator:** `${QuishingAnalyzer.defang(verdict.indicator.rawValue)}`")
    sb.appendLine("- **Type:** ${LocalizationProvider.getIndicatorTypeName(verdict.indicator.type, appLanguage)}")
    sb.appendLine("- **Verdict:** ${LocalizationProvider.getVerdictName(verdict.overallVerdict, appLanguage)} (${verdict.confidenceScore}%)")
    val ts = formatUtcTimestamp(verdict.timestampEpochMs)
    if (ts.isNotBlank()) {
        sb.appendLine("- **Timestamp:** $ts")
    }
    sb.appendLine("- **Summary:** ${LocalizationProvider.getLocalizedVerdictSummary(verdict, appLanguage)}")
    sb.appendLine()
    sb.appendLine("## ${strings.threatIntelSourcesResults}")
    verdict.sourceResults.forEach { src ->
        sb.appendLine("- **${src.providerName}**: ${src.verdict.name} (${src.detectionsCount}/${src.totalEngineCount}) — ${src.summary}")
    }
    return sb.toString().trim()
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
    onReplayGuide: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val strings = LocalizationProvider.getStrings(uiState.appLanguage)
    val palette = LocalIocColors.current
    val clipboardActions = rememberClipboardActions()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val filePickerLauncher = rememberFilePickerLauncher { path, name ->
        viewModel.onEvent(HomeEvent.FileDropped(path, name))
    }
    val batchFilePickerLauncher = rememberBatchFilePickerLauncher { files ->
        viewModel.onEvent(HomeEvent.BatchFilesSelected(files))
    }

    var showThreatIntelDialog by remember { mutableStateOf(false) }

    // Android Back Button / Gesture Interceptor
    val isAnySheetOpen = uiState.isVisionLensOpen || showThreatIntelDialog || uiState.isSettingsSheetOpen || uiState.isHistorySheetOpen || drawerState.isOpen || uiState.isLoading || uiState.activeVerdict != null || uiState.selectedBottomNavTab != 0
    AppBackHandler(enabled = isAnySheetOpen) {
        when {
            uiState.isVisionLensOpen -> viewModel.onEvent(HomeEvent.CloseVisionLens)
            showThreatIntelDialog -> showThreatIntelDialog = false
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
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(palette.surfaceElevated, palette.surfaceCard)
                                )
                            )
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
                                    color = palette.surfaceInset
                                ) {
                                    Text(
                                        text = "✕",
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        color = palette.textPrimary,
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
                                tint = if (uiState.selectedBottomNavTab == 0) palette.accentPrimary else palette.textSecondary,
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
                                tint = if (uiState.selectedBottomNavTab == 1) palette.accentPrimary else palette.textSecondary,
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
                                tint = if (uiState.selectedBottomNavTab == 2) palette.accentPrimary else palette.textSecondary,
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

                    NavigationDrawerItem(
                        icon = {
                            ShieldVerdictVectorIcon(
                                tint = palette.textSecondary,
                                size = 20.dp
                            )
                        },
                        label = { Text(text = strings.menuGuide, fontWeight = FontWeight.SemiBold) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            onReplayGuide()
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
            val ambientGlowBrush = remember(palette.isDark, palette.accentPrimary) {
                Brush.verticalGradient(
                    colors = listOf(
                        palette.accentPrimary.copy(alpha = if (palette.isDark) 0.08f else 0.05f),
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
                    onReplayGuide = {
                        viewModel.onEvent(HomeEvent.SelectBottomNavTab(0))
                        onReplayGuide()
                    },
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
                                    .clickable { coroutineScope.launch { drawerState.open() } }
                                    .pointerHoverIcon(PointerIcon.Hand),
                                color = palette.surfaceElevated.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "☰",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accentPrimary
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
                                        color = palette.textPrimary,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "LENS",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = palette.accentPrimary,
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
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable {
                                        val nextTab = if (uiState.selectedBottomNavTab == 1) 0 else 1
                                        viewModel.onEvent(HomeEvent.SelectBottomNavTab(nextTab))
                                    },
                                color = if (uiState.selectedBottomNavTab == 1) palette.accentPrimary.copy(alpha = 0.18f) else palette.surfaceElevated.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HistoryVectorIcon(tint = palette.accentPrimary, size = 16.dp)
                                    if (uiState.scanHistory.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = LocalizationProvider.formatNumber(uiState.scanHistory.size, uiState.appLanguage),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.accentPrimary
                                        )
                                    }
                                }
                            }

                            // Settings Action Button
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .springPress()
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable {
                                        val nextTab = if (uiState.selectedBottomNavTab == 2) 0 else 2
                                        viewModel.onEvent(HomeEvent.SelectBottomNavTab(nextTab))
                                    },
                                color = if (uiState.selectedBottomNavTab == 2) palette.accentPrimary.copy(alpha = 0.18f) else palette.surfaceElevated.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.35f))
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SettingsVectorIcon(tint = palette.accentPrimary, size = 16.dp)
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
                                onExportJson = { items ->
                                    clipboardActions.copyText(exportJsonFormat.encodeToString(items))
                                    viewModel.onEvent(HomeEvent.ShowStatusFeedback(strings.copiedFeedback))
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
                                // 1. Dual-Input Threat Triage Grid (Left: IOC Text & Payload Analysis, Right: File Triage & SHA-256 Queue)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Max),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Column 1: Left (IOC & Payload Analysis)
                                    SearchCard(
                                        uiState = uiState,
                                        strings = strings,
                                        isCompact = false,
                                        onQueryChange = { viewModel.onEvent(HomeEvent.QueryChanged(it)) },
                                        onClear = { viewModel.onEvent(HomeEvent.ClearClicked) },
                                        onCancel = { viewModel.onEvent(HomeEvent.CancelScan) },
                                        onAnalyze = { viewModel.onEvent(HomeEvent.AnalyzeClicked) },
                                        onSelectSample = { sample -> viewModel.onEvent(HomeEvent.SelectSampleQuery(sample)) },
                                        onOpenFilePicker = batchFilePickerLauncher,
                                        onOpenVisionLens = { viewModel.onEvent(HomeEvent.OpenVisionLens) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )

                                    // Column 2: Right (File Triage & SHA-256 Batch Queue)
                                    FileScannerCard(
                                        uiState = uiState,
                                        strings = strings,
                                        onOpenFilePicker = batchFilePickerLauncher,
                                        onClearQueue = { viewModel.onEvent(HomeEvent.ClearFileQueue) },
                                        onSelectQueueItem = { viewModel.onEvent(HomeEvent.SelectQueueItemVerdict(it)) },
                                        onRetryQueueItem = { id -> viewModel.onEvent(HomeEvent.RetryQueueItem(id)) },
                                        onRemoveQueueItem = { id -> viewModel.onEvent(HomeEvent.RemoveQueueItem(id)) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        isCompact = false
                                    )
                                }

                                // 2. File Hashing Progress Banner (Full Width)
                                FileHashingBanner(
                                    uiState = uiState,
                                    appLanguage = uiState.appLanguage,
                                    onCancel = { viewModel.onEvent(HomeEvent.CancelScan) }
                                )

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
                                        OverallVerdictCard(
                                            verdict = verdict,
                                            appLanguage = uiState.appLanguage,
                                            onCopyRaw = {
                                                clipboardActions.copyText(verdict.indicator.rawValue)
                                                viewModel.onEvent(HomeEvent.ShowStatusFeedback(strings.copiedFeedback))
                                            },
                                            onCopyDefanged = {
                                                clipboardActions.copyText(QuishingAnalyzer.defang(verdict.indicator.rawValue))
                                                viewModel.onEvent(HomeEvent.ShowStatusFeedback(strings.copiedFeedback))
                                            },
                                            onCopyReport = {
                                                clipboardActions.copyText(buildVerdictMarkdownReport(verdict, uiState.appLanguage))
                                                viewModel.onEvent(HomeEvent.ShowStatusFeedback(strings.copiedFeedback))
                                            },
                                            onReScan = { viewModel.onEvent(HomeEvent.ReScanIndicator) }
                                        )
                                    }
                                }

                                // 6. Per-Engine Telemetry Breakdown Matrix (Desktop Two-Column)
                                AnimatedVisibility(
                                    visible = uiState.isLoading || uiState.activeVerdict != null,
                                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                                    exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                                ) {
                                    LiveProviderDeck(
                                        uiState = uiState,
                                        onToggle = { viewModel.onEvent(HomeEvent.ToggleProviderDeck) },
                                        isTwoColumn = true
                                    )
                                }

                                // 7. Desktop Recent Triage HUD Feed
                                RecentTriageFeedHUD(
                                    history = uiState.scanHistory,
                                    appLanguage = uiState.appLanguage,
                                    strings = strings,
                                    onSelect = { item -> viewModel.onEvent(HomeEvent.HistoryItemClicked(item)) },
                                    onDelete = { item -> viewModel.onEvent(HomeEvent.DeleteHistoryItem(item)) },
                                    onReScan = { raw ->
                                        viewModel.onEvent(HomeEvent.QueryChanged(raw))
                                        viewModel.onEvent(HomeEvent.AnalyzeClicked)
                                    }
                                )
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
                                val isRtl = uiState.appLanguage.isRtl
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    var activeTriageTab by remember { mutableStateOf(0) }

                                    // Compact / Mobile Triage Segmented Selector (All, IOC Text, File Queue)
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = palette.surfaceCard.copy(alpha = 0.94f),
                                        border = BorderStroke(0.8.dp, palette.borderSubtle)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(3.dp),
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            val triageModes = listOf(
                                                Triple(
                                                    0,
                                                    when (uiState.appLanguage) {
                                                        AppLanguage.PERSIAN -> "همه بخش‌ها"
                                                        AppLanguage.RUSSIAN -> "Все"
                                                        AppLanguage.ENGLISH -> "All"
                                                    },
                                                    "🌐"
                                                ),
                                                Triple(
                                                    1,
                                                    when (uiState.appLanguage) {
                                                        AppLanguage.PERSIAN -> "تحلیل شاخص"
                                                        AppLanguage.RUSSIAN -> "IOC-текст"
                                                        AppLanguage.ENGLISH -> "IOC Text"
                                                    },
                                                    "🔍"
                                                ),
                                                Triple(
                                                    2,
                                                    when (uiState.appLanguage) {
                                                        AppLanguage.PERSIAN -> "صف فایل‌ها"
                                                        AppLanguage.RUSSIAN -> "Файлы"
                                                        AppLanguage.ENGLISH -> "Files"
                                                    },
                                                    "📁"
                                                )
                                            )
                                            triageModes.forEach { (idx, title, icon) ->
                                                val isSelected = activeTriageTab == idx
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(36.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { activeTriageTab = idx }
                                                        .pointerHoverIcon(PointerIcon.Hand),
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSelected) palette.accentPrimary.copy(alpha = 0.16f) else Color.Transparent,
                                                    border = if (isSelected) BorderStroke(0.9.dp, palette.accentPrimary.copy(alpha = 0.45f)) else null
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxSize(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(text = icon, fontSize = 11.5.sp)
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            text = title,
                                                            fontSize = 10.5.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) palette.accentPrimary else palette.textSecondary,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (activeTriageTab == 0 || activeTriageTab == 1) {
                                        SearchCard(
                                            uiState = uiState,
                                            strings = strings,
                                            isCompact = isCompactScreen,
                                            onQueryChange = { viewModel.onEvent(HomeEvent.QueryChanged(it)) },
                                            onClear = { viewModel.onEvent(HomeEvent.ClearClicked) },
                                            onCancel = { viewModel.onEvent(HomeEvent.CancelScan) },
                                            onAnalyze = { viewModel.onEvent(HomeEvent.AnalyzeClicked) },
                                            onSelectSample = { sample -> viewModel.onEvent(HomeEvent.SelectSampleQuery(sample)) },
                                            onOpenFilePicker = batchFilePickerLauncher,
                                            onOpenVisionLens = { viewModel.onEvent(HomeEvent.OpenVisionLens) }
                                        )
                                    }

                                    if (activeTriageTab == 0 || activeTriageTab == 2) {
                                        FileScannerCard(
                                            uiState = uiState,
                                            strings = strings,
                                            onOpenFilePicker = batchFilePickerLauncher,
                                            onClearQueue = { viewModel.onEvent(HomeEvent.ClearFileQueue) },
                                            onSelectQueueItem = { viewModel.onEvent(HomeEvent.SelectQueueItemVerdict(it)) },
                                            onRetryQueueItem = { id -> viewModel.onEvent(HomeEvent.RetryQueueItem(id)) },
                                            onRemoveQueueItem = { id -> viewModel.onEvent(HomeEvent.RemoveQueueItem(id)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            isCompact = true
                                        )
                                    }

                                    FileHashingBanner(
                                        uiState = uiState,
                                        appLanguage = uiState.appLanguage,
                                        onCancel = { viewModel.onEvent(HomeEvent.CancelScan) }
                                    )

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
                                            OverallVerdictCard(
                                                verdict = verdict,
                                                appLanguage = uiState.appLanguage,
                                                onCopyRaw = {
                                                    clipboardActions.copyText(verdict.indicator.rawValue)
                                                    viewModel.onEvent(HomeEvent.ShowStatusFeedback(strings.copiedFeedback))
                                                },
                                                onCopyDefanged = {
                                                    clipboardActions.copyText(QuishingAnalyzer.defang(verdict.indicator.rawValue))
                                                    viewModel.onEvent(HomeEvent.ShowStatusFeedback(strings.copiedFeedback))
                                                },
                                                onCopyReport = {
                                                    clipboardActions.copyText(buildVerdictMarkdownReport(verdict, uiState.appLanguage))
                                                    viewModel.onEvent(HomeEvent.ShowStatusFeedback(strings.copiedFeedback))
                                                },
                                                onReScan = { viewModel.onEvent(HomeEvent.ReScanIndicator) }
                                            )
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = uiState.isLoading || uiState.activeVerdict != null,
                                        enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                                        exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                                    ) {
                                        LiveProviderDeck(
                                            uiState = uiState,
                                            onToggle = { viewModel.onEvent(HomeEvent.ToggleProviderDeck) },
                                            isTwoColumn = false
                                        )
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
                                    onExportJson = { items ->
                                        clipboardActions.copyText(exportJsonFormat.encodeToString(items))
                                        viewModel.onEvent(HomeEvent.ShowStatusFeedback(strings.copiedFeedback))
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
                    onExportJson = { items ->
                        clipboardActions.copyText(exportJsonFormat.encodeToString(items))
                        viewModel.onEvent(HomeEvent.ShowStatusFeedback(strings.copiedFeedback))
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
                    onReplayGuide = {
                        viewModel.onEvent(HomeEvent.SetSettingsSheetOpen(false))
                        onReplayGuide()
                    },
                    onClose = { viewModel.onEvent(HomeEvent.SetSettingsSheetOpen(false)) }
                )
            }

            // Status Feedback Toast Banner
            val feedbackMessage = uiState.statusFeedbackMessage
            LaunchedEffect(feedbackMessage) {
                if (feedbackMessage != null) {
                    delay(2400L)
                    viewModel.onEvent(HomeEvent.DismissStatusFeedback)
                }
            }
            AnimatedVisibility(
                visible = feedbackMessage != null,
                enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 3 },
                exit = fadeOut(tween(180)) + slideOutVertically(tween(180)) { it / 3 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (isMobileOrTablet) 76.dp else 48.dp, start = 16.dp, end = 16.dp)
            ) {
                if (feedbackMessage != null) {
                    Surface(
                        color = palette.surfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.65f)),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.onEvent(HomeEvent.DismissStatusFeedback) }
                    ) {
                        Text(
                            text = feedbackMessage,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
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
                    onOpenEnginesDialog = { showThreatIntelDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                )
            }

            // Modal Threat Intel Engines Dialog (shared between Desktop Footer & Settings)
            if (showThreatIntelDialog) {
                ThreatIntelEnginesDialog(
                    strings = strings,
                    isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl,
                    appLanguage = uiState.appLanguage,
                    settingsRepository = settingsRepository,
                    onDismiss = { showThreatIntelDialog = false }
                )
            }

            // Cyber Vision Lens & Quishing Triage Modal Dialog
            CyberLensDialog(
                isOpen = uiState.isVisionLensOpen,
                strings = strings,
                onDismiss = { viewModel.onEvent(HomeEvent.CloseVisionLens) },
                onTriagePayload = { payload ->
                    viewModel.onEvent(HomeEvent.VisionPayloadScanned(payload))
                }
            )

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
    val palette = LocalIocColors.current
    val monoFont = rememberMonospaceFontFamily()
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
            containerColor = palette.surfaceCard.copy(alpha = 0.96f)
        ),
        border = BorderStroke(
            1.2.dp,
            Brush.horizontalGradient(
                listOf(
                    palette.accentPrimary.copy(alpha = 0.8f),
                    palette.accentSecondary.copy(alpha = 0.7f),
                    palette.accentPrimary.copy(alpha = 0.8f)
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
                            .background(palette.surfaceInset)
                            .border(1.dp, palette.accentPrimary.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(28.dp).graphicsLayer { rotationZ = radarRotationState.value }) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = size.width / 2f

                            drawCircle(
                                color = palette.accentPrimary.copy(alpha = 0.35f),
                                radius = radius,
                                style = Stroke(width = 1.2.dp.toPx())
                            )
                            drawCircle(
                                color = palette.accentPrimary.copy(alpha = 0.2f),
                                radius = radius * 0.55f,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            drawLine(
                                color = palette.accentPrimary,
                                start = center,
                                end = Offset(center.x + radius, center.y),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(palette.textPrimary)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = palette.accentPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.6.dp, palette.accentPrimary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = targetIndicator?.let { LocalizationProvider.getIndicatorTypeName(it.type, appLanguage) } ?: strings.statusScanning,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accentPrimary
                                )
                            }

                            Text(
                                text = strings.liveScanInProgress,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textPrimary,
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
                                    color = palette.textSecondary,
                                    fontFamily = monoFont,
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
                    colors = ButtonDefaults.buttonColors(containerColor = palette.verdictMalicious),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .border(1.dp, palette.verdictMalicious, RoundedCornerShape(10.dp))
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
                        color = palette.accentPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        fontSize = 10.5.sp
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = palette.accentPrimary,
                    trackColor = palette.surfaceInset
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
    val palette = LocalIocColors.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = palette.surfaceElevated.copy(alpha = 0.97f),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    Color.Transparent,
                    palette.accentPrimary.copy(alpha = 0.5f),
                    palette.accentPrimary.copy(alpha = 0.7f),
                    palette.accentPrimary.copy(alpha = 0.5f),
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
                val activeColor = palette.accentPrimary
                val inactiveColor = palette.textMuted

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
                                        .background(palette.accentPrimary, CircleShape)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = LocalizationProvider.formatNumber(historyCount, appLanguage),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.onAccentPrimary
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
                                .background(if (indicatorWidth > 1.dp) palette.accentPrimary else Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileHashingBanner(
    uiState: HomeUiState,
    appLanguage: AppLanguage,
    onCancel: () -> Unit = {}
) {
    val strings = LocalizationProvider.getStrings(appLanguage)
    val palette = LocalIocColors.current
    AnimatedVisibility(visible = uiState.isHashingFile) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = palette.surfaceCard.copy(alpha = 0.94f)),
            border = BorderStroke(1.dp, topBorderHighlightBrush(palette.accentPrimary, isDark = palette.isDark)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = palette.accentPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "⚡ ${strings.streamHashingTitle}: ${uiState.hashingFileName ?: "file"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.accentPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(
                        onClick = onCancel,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Text(
                            text = strings.cancelScan,
                            color = palette.verdictMalicious,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = palette.accentPrimary,
                    trackColor = palette.surfaceInset
                )
            }
        }
    }
}

@Composable
private fun SearchCard(
    uiState: HomeUiState,
    strings: AppStrings,
    isCompact: Boolean,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onCancel: () -> Unit,
    onAnalyze: () -> Unit,
    onSelectSample: (String) -> Unit = {},
    onOpenFilePicker: () -> Unit = {},
    onOpenVisionLens: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalIocColors.current
    val monoFont = rememberMonospaceFontFamily()
    val clipboardActions = rememberClipboardActions()
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
        color = palette.surfaceCard.copy(alpha = if (isHovered) 0.98f else 0.92f),
        border = BorderStroke(
            1.dp,
            if (isHovered) Brush.linearGradient(listOf(palette.accentPrimary, palette.accentPrimary.copy(alpha = 0.45f)))
            else topBorderHighlightBrush(palette.accentPrimary, isDark = palette.isDark)
        ),
        shadowElevation = if (isHovered) 8.dp else 4.dp
    ) {
        Column(
            modifier = if (isCompact) Modifier.fillMaxWidth().padding(18.dp) else Modifier.fillMaxSize().padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.accentPrimary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    ScannerVectorIcon(tint = palette.accentPrimary, size = 20.dp)
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
                        color = palette.textPrimary
                    )
                    Text(
                        text = when (uiState.appLanguage) {
                            AppLanguage.PERSIAN -> "تحلیل چندموتوره و بلادرنگ IP، دامنه، URL و هش"
                            AppLanguage.RUSSIAN -> "Мгновенный анализ IP, доменов, URL и хешей"
                            else -> "Real-time zero-key triage for IP, Domain, URL & Hash"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Input Field
            OutlinedTextField(
                value = uiState.queryInput,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown &&
                            (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) &&
                            uiState.queryInput.isNotBlank() &&
                            !uiState.isLoading
                        ) {
                            onAnalyze()
                            true
                        } else {
                            false
                        }
                    },
                textStyle = LocalTextStyle.current.copy(
                    textDirection = TextDirection.Ltr,
                    fontFamily = monoFont,
                    fontSize = if (isCompact) 13.5.sp else 14.5.sp,
                    color = palette.textPrimary
                ),
                placeholder = {
                    Text(
                        text = strings.searchFieldPlaceholder,
                        color = palette.textMuted,
                        fontSize = if (isCompact) 12.sp else 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (uiState.queryInput.isNotBlank() && !uiState.isLoading) {
                            onAnalyze()
                        }
                    }
                ),
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        // Vision Lens Trigger Button [ 📷 ]
                        IconButton(
                            onClick = onOpenVisionLens,
                            modifier = Modifier
                                .size(34.dp)
                                .springPress()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = palette.accentPrimary.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.4f)),
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CameraLensVectorIcon(
                                        tint = palette.accentPrimary,
                                        size = 14.dp
                                    )
                                }
                            }
                        }

                        if (uiState.queryInput.isNotEmpty()) {
                            IconButton(
                                onClick = onClear,
                                modifier = Modifier
                                    .size(34.dp)
                                    .springPress()
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = palette.surfaceInset,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "✕",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary,
                    focusedBorderColor = palette.accentPrimary,
                    unfocusedBorderColor = palette.borderStrong
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Quick Sample IOC Chips when query is empty & idle
            AnimatedVisibility(visible = uiState.queryInput.isBlank() && !uiState.isLoading) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Text(
                        text = strings.sampleIocsTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textMuted,
                        fontSize = 10.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val samples = listOf(
                            strings.sampleGoogleDns to "8.8.8.8",
                            strings.sampleMalwareUrl to "malware-test.com",
                            strings.sampleHash to "ed01ebfbc9eb5bbea545af4d01bf5f1071661840480439c6e5babe8e080e41aa"
                        )
                        samples.forEach { (label, sampleValue) ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelectSample(sampleValue) }
                                    .pointerHoverIcon(PointerIcon.Hand),
                                color = palette.surfaceInset,
                                border = BorderStroke(0.8.dp, palette.borderSubtle),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    fontSize = 10.5.sp,
                                    color = palette.accentPrimary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

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
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            val clipText = clipboardActions.readText()
                            if (!clipText.isNullOrBlank()) {
                                onQueryChange(clipText.trim())
                            }
                        },
                        modifier = Modifier.springPress().pointerHoverIcon(PointerIcon.Hand),
                        colors = ButtonDefaults.textButtonColors(contentColor = palette.accentPrimary)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ClipboardVectorIcon(tint = palette.accentPrimary, size = 15.dp)
                            Text(
                                text = strings.pasteFromClipboard,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (uiState.isLoading) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.verdictMalicious),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(38.dp).defaultMinSize(minWidth = 110.dp).springPress()
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
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    Button(
                        onClick = onAnalyze,
                        enabled = uiState.queryInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accentPrimary,
                            contentColor = palette.onAccentPrimary,
                            disabledContainerColor = palette.surfaceInset,
                            disabledContentColor = palette.textMuted
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = if (uiState.queryInput.isNotBlank()) {
                            BorderStroke(1.5.dp, palette.accentPrimary.copy(alpha = pulseGlowState?.value ?: 0.35f))
                        } else {
                            BorderStroke(1.dp, palette.borderSubtle)
                        },
                        modifier = Modifier.height(38.dp).defaultMinSize(minWidth = 110.dp).springPress()
                    ) {
                        Text(
                            text = strings.scanPayload,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
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
    onClearQueue: () -> Unit = {},
    onSelectQueueItem: (FileQueueItem) -> Unit = {},
    onRetryQueueItem: (String) -> Unit = {},
    onRemoveQueueItem: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val palette = LocalIocColors.current
    val monoFont = rememberMonospaceFontFamily()
    var isCardHovered by remember { mutableStateOf(false) }
    val dropInteractionSource = remember { MutableInteractionSource() }
    val isDropHovered by dropInteractionSource.collectIsHoveredAsState()
    val queueScrollState = rememberScrollState()

    val totalQueueCount = uiState.fileQueue.size
    val doneQueueCount = remember(uiState.fileQueue) {
        uiState.fileQueue.count { it.status == QueueItemStatus.DONE || it.status == QueueItemStatus.ERROR }
    }
    val progressFraction = if (totalQueueCount > 0) doneQueueCount.toFloat() / totalQueueCount.toFloat() else 0f

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
        color = palette.surfaceCard.copy(alpha = if (isCardHovered) 0.98f else 0.92f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isCardHovered) Brush.linearGradient(listOf(palette.accentPrimary, palette.accentPrimary.copy(alpha = 0.45f)))
            else topBorderHighlightBrush(palette.accentPrimary, isDark = palette.isDark)
        ),
        shadowElevation = if (isCardHovered) 8.dp else 4.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp)
        ) {
            // Header: Icon + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.accentPrimary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        FileDocumentVectorIcon(tint = palette.accentPrimary, size = 20.dp)
                    }
                    Column {
                        Text(
                            text = when (uiState.appLanguage) {
                                AppLanguage.PERSIAN -> "پویش و تریاژ فایل (SHA-256)"
                                AppLanguage.RUSSIAN -> "Анализ файлов (SHA-256)"
                                else -> "File Triage & SHA-256 Scanner"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = when (uiState.appLanguage) {
                                AppLanguage.PERSIAN -> "محاسبه کاملاً آفلاین هش با پشتیبانی از صف چندتایی"
                                AppLanguage.RUSSIAN -> "Клиентское хеширование с очередью файлов"
                                else -> "100% Client-side local hashing with multi-file queue"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }

                if (totalQueueCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = palette.accentPrimary.copy(alpha = 0.12f),
                        border = BorderStroke(0.6.dp, palette.accentPrimary.copy(alpha = 0.35f))
                    ) {
                        val doneFmt = LocalizationProvider.formatNumber(doneQueueCount, uiState.appLanguage)
                        val totalFmt = LocalizationProvider.formatNumber(totalQueueCount, uiState.appLanguage)
                        Text(
                            text = when (uiState.appLanguage) {
                                AppLanguage.PERSIAN -> "$doneFmt/$totalFmt پردازش"
                                AppLanguage.RUSSIAN -> "$doneFmt/$totalFmt готово"
                                AppLanguage.ENGLISH -> "$doneFmt/$totalFmt Done"
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Content Area: Interactive Queue List OR Drag-and-Drop Ingestion Target
            if (totalQueueCount > 0) {
                // Batch Queue Active View
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Progress & Quick Queue Controls Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = when (uiState.appLanguage) {
                                    AppLanguage.PERSIAN -> "صف فایل‌ها:"
                                    AppLanguage.RUSSIAN -> "Очередь:"
                                    AppLanguage.ENGLISH -> "Triage Queue:"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.textSecondary
                            )
                            if (uiState.isProcessingQueue) {
                                Text(
                                    text = when (uiState.appLanguage) {
                                        AppLanguage.PERSIAN -> "در حال پردازش..."
                                        AppLanguage.RUSSIAN -> "Обработка..."
                                        AppLanguage.ENGLISH -> "Processing..."
                                    },
                                    fontSize = 10.5.sp,
                                    color = palette.accentPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(onClick = onOpenFilePicker)
                                    .pointerHoverIcon(PointerIcon.Hand),
                                color = palette.surfaceInset,
                                border = BorderStroke(0.7.dp, palette.borderSubtle)
                            ) {
                                Text(
                                    text = when (uiState.appLanguage) {
                                        AppLanguage.PERSIAN -> "+ افزودن فایل"
                                        AppLanguage.RUSSIAN -> "+ Добавить"
                                        AppLanguage.ENGLISH -> "+ Add More"
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.textPrimary
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(onClick = onClearQueue)
                                    .pointerHoverIcon(PointerIcon.Hand),
                                color = palette.verdictMalicious.copy(alpha = 0.12f),
                                border = BorderStroke(0.7.dp, palette.verdictMalicious.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = when (uiState.appLanguage) {
                                        AppLanguage.PERSIAN -> "پاکسازی صف"
                                        AppLanguage.RUSSIAN -> "Очистить"
                                        AppLanguage.ENGLISH -> "Clear"
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.verdictMalicious
                                )
                            }
                        }
                    }

                    // Linear Progress Bar
                    LinearProgressIndicator(
                        progress = { progressFraction.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = palette.accentPrimary,
                        trackColor = palette.surfaceInset
                    )

                    // Scrollable List of Queued Files
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(queueScrollState),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        uiState.fileQueue.forEach { item ->
                            val isSelected = uiState.activeVerdict != null && uiState.activeVerdict == item.verdict
                            val statusColor = when (item.status) {
                                QueueItemStatus.QUEUED -> palette.textMuted
                                QueueItemStatus.HASHING -> palette.accentPrimary
                                QueueItemStatus.TRIAGING -> palette.verdictLowRisk
                                QueueItemStatus.DONE -> verdictColorFor(item.verdict?.overallVerdict ?: VerdictLevel.NO_DATA, palette)
                                QueueItemStatus.ERROR -> palette.verdictMalicious
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = item.verdict != null) { onSelectQueueItem(item) }
                                    .pointerHoverIcon(if (item.verdict != null) PointerIcon.Hand else PointerIcon.Default),
                                color = if (isSelected) palette.accentPrimary.copy(alpha = 0.12f) else palette.surfaceInset,
                                border = BorderStroke(
                                    0.8.dp,
                                    if (isSelected) palette.accentPrimary else statusColor.copy(alpha = 0.35f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(text = "📄", fontSize = 12.sp)
                                        Column {
                                            Text(
                                                text = item.fileName,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val sizeLabel = formatFileSize(item.fileSizeBytes, uiState.appLanguage)
                                            if (!item.sha256.isNullOrBlank() || sizeLabel.isNotBlank()) {
                                                val hashShort = item.sha256?.takeIf { it.length >= 24 }
                                                    ?.let { "${it.take(12)}...${it.takeLast(8)}" }
                                                    ?: item.sha256.orEmpty()
                                                val meta = listOf(sizeLabel, hashShort).filter { it.isNotBlank() }.joinToString(" • ")
                                                Text(
                                                    text = meta,
                                                    fontSize = 9.5.sp,
                                                    fontFamily = monoFont,
                                                    color = palette.textMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (item.status == QueueItemStatus.ERROR && !item.errorMessage.isNullOrBlank()) {
                                                Text(
                                                    text = item.errorMessage,
                                                    fontSize = 9.5.sp,
                                                    color = palette.verdictMalicious,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (item.status == QueueItemStatus.ERROR) {
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable { onRetryQueueItem(item.id) }
                                                    .pointerHoverIcon(PointerIcon.Hand),
                                                shape = RoundedCornerShape(6.dp),
                                                color = palette.accentPrimary.copy(alpha = 0.14f),
                                                border = BorderStroke(0.6.dp, palette.accentPrimary.copy(alpha = 0.45f))
                                            ) {
                                                Text(
                                                    text = "↻",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = palette.accentPrimary
                                                )
                                            }
                                        }

                                        // Item Status Tag
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = statusColor.copy(alpha = 0.12f),
                                            border = BorderStroke(0.6.dp, statusColor.copy(alpha = 0.40f))
                                        ) {
                                            Text(
                                                text = when (item.status) {
                                                    QueueItemStatus.QUEUED -> when (uiState.appLanguage) {
                                                        AppLanguage.PERSIAN -> "در صف"
                                                        AppLanguage.RUSSIAN -> "В очереди"
                                                        AppLanguage.ENGLISH -> "Queued"
                                                    }
                                                    QueueItemStatus.HASHING -> when (uiState.appLanguage) {
                                                        AppLanguage.PERSIAN -> "محاسبه هش..."
                                                        AppLanguage.RUSSIAN -> "Хеш..."
                                                        AppLanguage.ENGLISH -> "Hashing..."
                                                    }
                                                    QueueItemStatus.TRIAGING -> when (uiState.appLanguage) {
                                                        AppLanguage.PERSIAN -> "تریاژ موتورها..."
                                                        AppLanguage.RUSSIAN -> "Проверка..."
                                                        AppLanguage.ENGLISH -> "Triaging..."
                                                    }
                                                    QueueItemStatus.DONE -> item.verdict?.let {
                                                        LocalizationProvider.getVerdictName(it.overallVerdict, uiState.appLanguage)
                                                    } ?: strings.done
                                                    QueueItemStatus.ERROR -> when (uiState.appLanguage) {
                                                        AppLanguage.PERSIAN -> "خطا"
                                                        AppLanguage.RUSSIAN -> "Ошибка"
                                                        AppLanguage.ENGLISH -> "ERROR"
                                                    }
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = statusColor
                                            )
                                        }

                                        if (item.status != QueueItemStatus.HASHING && item.status != QueueItemStatus.TRIAGING) {
                                            Surface(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .clickable { onRemoveQueueItem(item.id) }
                                                    .pointerHoverIcon(PointerIcon.Hand),
                                                color = palette.verdictMalicious.copy(alpha = 0.10f),
                                                shape = CircleShape
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "✕",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = palette.verdictMalicious
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
            } else {
                // Drop Target Area (When queue is empty)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isCompact) {
                                Modifier.heightIn(min = 125.dp)
                            } else {
                                Modifier.defaultMinSize(minHeight = 110.dp)
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
                    color = if (isDropHovered) palette.accentPrimary.copy(alpha = 0.14f) else palette.surfaceInset,
                    border = BorderStroke(1.5.dp, if (isDropHovered) palette.accentPrimary else palette.accentPrimary.copy(alpha = 0.4f))
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
                                .background(palette.accentPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            UploadCloudVectorIcon(tint = palette.accentPrimary, size = 22.dp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = when (uiState.appLanguage) {
                                AppLanguage.PERSIAN -> "انتخاب فایل‌ها برای اسکن (تکی یا گروهی)"
                                AppLanguage.RUSSIAN -> "Выбрать файлы для сканирования (один или несколько)"
                                else -> "Select File(s) to Scan"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = when (uiState.appLanguage) {
                                AppLanguage.PERSIAN -> "پشتیبانی از انواع فایل‌ها با صف‌بندی هوشمند و پردازش گروهی بدون آپلود"
                                AppLanguage.RUSSIAN -> "Поддержка любых форматов с очередью пакетной обработки без загрузки"
                                else -> "Supports all formats: Executables, Documents, Archives • Multi-file queue"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun ErrorMessageBanner(errorMessage: String?) {
    val palette = LocalIocColors.current
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
                color = palette.verdictMalicious.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, palette.verdictMalicious.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚠",
                        color = palette.verdictMalicious,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = errorMessage,
                        color = palette.verdictMalicious,
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
    appLanguage: AppLanguage,
    onCopyRaw: () -> Unit = {},
    onCopyDefanged: () -> Unit = {},
    onCopyReport: () -> Unit = {},
    onReScan: () -> Unit = {}
) {
    val palette = LocalIocColors.current
    val monoFont = rememberMonospaceFontFamily()
    val verdictColor = verdictColorFor(verdict.overallVerdict, palette)
    val strings = LocalizationProvider.getStrings(appLanguage)
    val timestampText = remember(verdict.timestampEpochMs) { formatUtcTimestamp(verdict.timestampEpochMs) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surfaceCard.copy(alpha = 0.96f)),
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
                                    fontFamily = monoFont,
                                    fontSize = 14.5.sp
                                ),
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        val typeLabel = "${strings.indicatorTypeLabel}: ${LocalizationProvider.getIndicatorTypeName(verdict.indicator.type, appLanguage)}"
                        Text(
                            text = if (timestampText.isNotBlank()) "$typeLabel • $timestampText" else typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }

                Surface(
                    color = verdictColor.copy(alpha = 0.18f),
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

            if (verdict.hasNetworkError) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = palette.verdictSuspicious.copy(alpha = 0.12f),
                    border = BorderStroke(0.9.dp, palette.verdictSuspicious.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "⚠ ${strings.networkOutageBanner}",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.verdictSuspicious,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onReScan() }
                                .pointerHoverIcon(PointerIcon.Hand),
                            color = palette.verdictSuspicious.copy(alpha = 0.2f),
                            border = BorderStroke(0.8.dp, palette.verdictSuspicious),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = strings.retryScanButton,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.verdictSuspicious
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = LocalizationProvider.getLocalizedVerdictSummary(verdict, appLanguage),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textPrimary
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
                    color = palette.accentPrimary,
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
                trackColor = palette.surfaceInset
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VerdictActionChip(label = strings.copyRawIoc, accent = palette.accentPrimary, onClick = onCopyRaw)
                VerdictActionChip(label = strings.copyDefangedIoc, accent = palette.accentPrimary, onClick = onCopyDefanged)
                VerdictActionChip(label = strings.copyReportMarkdown, accent = palette.accentSecondary, onClick = onCopyReport)
                VerdictActionChip(label = "⚡ ${strings.reAnalyze}", accent = verdictColor, onClick = onReScan)
            }
        }
    }
}

@Composable
private fun VerdictActionChip(
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    val palette = LocalIocColors.current
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand),
        color = palette.surfaceInset,
        border = BorderStroke(0.8.dp, accent.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            maxLines = 1
        )
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
    val palette = LocalIocColors.current
    val activeSources = uiState.activeVerdict?.sourceResults ?: emptyList()
    val activeMap = remember(activeSources) { activeSources.associateBy { it.providerName } }
    val zeroKeyProviders = remember {
        listOf(
            "URLhaus (abuse.ch)" to true,
            "MalwareBazaar (abuse.ch)" to true,
            "ThreatFox (abuse.ch)" to true,
            "RDAP / Registry" to true,
            "Cloudflare DoH" to true,
            "Shodan InternetDB" to true,
            "AlienVault OTX" to true,
            "RIPE Stat" to true,
            "crt.sh" to true,
            "Feodo Tracker (abuse.ch)" to true,
            "ipwho.is" to true
        )
    }
    val allProviders = remember(activeSources) {
        val zeroKeyNames = zeroKeyProviders.map { it.first }.toSet()
        val optionalActive = activeSources
            .filter { it.providerName !in zeroKeyNames }
            .map { it.providerName to false }
        zeroKeyProviders + optionalActive
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surfaceCard.copy(alpha = 0.96f)),
        border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .pointerHoverIcon(PointerIcon.Hand),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = palette.accentPrimary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Text(
                            text = "${LocalizationProvider.formatNumber(allProviders.size, uiState.appLanguage)} ${strings.enginesCount}",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accentPrimary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    ControlledMarqueeText(
                        text = strings.threatIntelProvidersTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.accentPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Text(
                    text = if (uiState.isProviderDeckExpanded) "▲" else "▼",
                    color = palette.textMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            AnimatedVisibility(visible = uiState.isProviderDeckExpanded) {
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
        "Feodo Tracker (abuse.ch)" -> type == IndicatorType.IP_ADDRESS
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
    val palette = LocalIocColors.current
    val isRateLimited = result?.isRateLimited == true || result?.summary?.contains("Rate Limit", ignoreCase = true) == true
    val isTimedOut = result?.summary?.contains("timed out", ignoreCase = true) == true
    val isProviderError = result?.isError == true && !isRateLimited && !isTimedOut
    val isApplicable = isProviderApplicable(name, indicatorType)

    val statusColor = when {
        isRateLimited -> palette.verdictLowRisk
        isTimedOut -> palette.textMuted
        isProviderError -> palette.verdictMalicious
        isLoading && !isApplicable && indicatorType != null -> palette.verdictNoData
        isLoading && isApplicable -> palette.accentPrimary
        result != null -> verdictColorFor(result.verdict, palette)
        else -> palette.verdictNoData
    }

    val statusText = when {
        isLoading && !isApplicable && indicatorType != null -> strings.statusNotApplicable
        isLoading -> strings.statusScanning
        isRateLimited -> strings.statusRateLimited
        isTimedOut -> strings.statusTimedOut
        isProviderError -> when (appLanguage) {
            AppLanguage.PERSIAN -> "خطا"
            AppLanguage.RUSSIAN -> "Ошибка"
            AppLanguage.ENGLISH -> "ERR"
        }
        result != null -> LocalizationProvider.getVerdictName(result.verdict, appLanguage)
        hasActiveVerdict && !isApplicable -> strings.statusNotApplicable
        hasActiveVerdict -> if (isTwoColumn) strings.statusKeyRequiredShort else strings.statusKeyRequired
        isZeroKey -> if (isTwoColumn) strings.statusReady else strings.statusReadyZeroKey
        else -> if (isTwoColumn) strings.statusKeyRequiredShort else strings.statusKeyRequired
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surfaceInset, RoundedCornerShape(10.dp))
            .border(0.6.dp, palette.borderSubtle, RoundedCornerShape(10.dp))
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
                    color = palette.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (isTwoColumn) 11.5.sp else 13.sp,
                    modifier = Modifier.weight(1f)
                )
                if (isZeroKey) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.freeBadge,
                        color = palette.verdictClean,
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
                    color = palette.textSecondary,
                    fontSize = if (isTwoColumn) 9.sp else 10.5.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Surface(
            color = if (isLoading) palette.accentPrimary.copy(alpha = 0.15f) else statusColor.copy(alpha = 0.16f),
            shape = RoundedCornerShape(6.dp)
        ) {
            ControlledMarqueeText(
                text = statusText,
                modifier = Modifier
                    .padding(horizontal = if (isTwoColumn) 5.dp else 8.dp, vertical = 2.dp)
                    .widthIn(max = if (isTwoColumn) 115.dp else 160.dp),
                color = if (isLoading) palette.accentPrimary else statusColor,
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
    onExportJson: (List<TriageVerdict>) -> Unit = {},
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalizationProvider.getStrings(appLanguage)
    val palette = LocalIocColors.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<IndicatorType?>(null) }
    var selectedVerdictFilter by remember { mutableStateOf<VerdictLevel?>(null) }
    var deletingItemRawValue by remember { mutableStateOf<String?>(null) }
    var pendingDeletedRawValues by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val filteredHistory = remember(
        history,
        searchQuery,
        selectedTypeFilter,
        selectedVerdictFilter,
        pendingDeletedRawValues
    ) {
        filterScanHistory(
            history = history,
            searchQuery = searchQuery,
            selectedTypeFilter = selectedTypeFilter,
            selectedVerdictFilter = selectedVerdictFilter,
            excludedRawValues = pendingDeletedRawValues
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = palette.surfaceElevated,
            title = {
                Text(
                    text = strings.confirmClearHistoryTitle,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = strings.confirmClearHistoryMessage,
                    color = palette.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmDialog = false
                        onClear()
                    }
                ) {
                    Text(text = strings.confirmButton, color = palette.verdictMalicious, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(text = strings.cancelButton, color = palette.textSecondary)
                }
            }
        )
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { onExportJson(filteredHistory.ifEmpty { history }) },
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Text(
                            text = strings.exportHistoryButton,
                            color = palette.accentPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Text(
                            text = strings.clearHistory,
                            color = palette.verdictMalicious,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                    color = palette.textMuted,
                    fontSize = 12.5.sp
                )
            },
            leadingIcon = {
                ScannerVectorIcon(
                    size = 16.dp,
                    tint = palette.accentPrimary.copy(alpha = 0.85f)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Text(text = "✕", color = palette.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = palette.textPrimary,
                unfocusedTextColor = palette.textPrimary,
                focusedBorderColor = palette.accentPrimary,
                unfocusedBorderColor = palette.borderStrong
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Indicator type filter chips (ALL, IP, DOMAIN, URL, HASH)
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
                    color = if (isSelected) palette.accentPrimary.copy(alpha = 0.18f) else palette.surfaceInset,
                    border = BorderStroke(
                        0.8.dp,
                        if (isSelected) palette.accentPrimary else palette.borderSubtle
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = if (isSelected) palette.accentPrimary else palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Verdict filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.filterByVerdictLabel,
                fontSize = 10.5.sp,
                color = palette.textMuted,
                fontWeight = FontWeight.Medium
            )
            val verdictOptions = listOf(
                null to strings.filterAll,
                VerdictLevel.MALICIOUS to LocalizationProvider.getVerdictName(VerdictLevel.MALICIOUS, appLanguage),
                VerdictLevel.SUSPICIOUS to LocalizationProvider.getVerdictName(VerdictLevel.SUSPICIOUS, appLanguage),
                VerdictLevel.CLEAN to LocalizationProvider.getVerdictName(VerdictLevel.CLEAN, appLanguage),
                VerdictLevel.NO_DATA to LocalizationProvider.getVerdictName(VerdictLevel.NO_DATA, appLanguage)
            )
            verdictOptions.forEach { (verdictLevel, label) ->
                val isSelected = selectedVerdictFilter == verdictLevel
                val chipColor = verdictLevel?.let { verdictColorFor(it, palette) } ?: palette.accentPrimary
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedVerdictFilter = verdictLevel }
                        .pointerHoverIcon(PointerIcon.Hand),
                    color = if (isSelected) chipColor.copy(alpha = 0.18f) else palette.surfaceInset,
                    border = BorderStroke(0.8.dp, if (isSelected) chipColor else palette.borderSubtle),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        color = if (isSelected) chipColor else palette.textSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        HorizontalDivider(color = palette.borderSubtle)

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
                        color = palette.textMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
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
                    color = palette.textMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
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
                        val itemColor = verdictColorFor(item.overallVerdict, palette)

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
                                    .background(palette.surfaceInset)
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
                                            color = palette.textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val ts = formatUtcTimestamp(item.timestampEpochMs)
                                    val baseMeta = "${LocalizationProvider.getIndicatorTypeName(item.indicator.type, appLanguage)} • ${strings.threatConfidenceScore}: ${LocalizationProvider.formatScore(item.confidenceScore, appLanguage)}"
                                    Text(
                                        text = if (ts.isNotBlank()) "$baseMeta • $ts" else baseMeta,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = itemColor.copy(alpha = 0.18f),
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
                                        color = palette.verdictMalicious.copy(alpha = 0.12f),
                                        shape = CircleShape
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "✕",
                                                color = palette.verdictMalicious,
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
    onExportJson: (List<TriageVerdict>) -> Unit = {},
    onClear: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalizationProvider.getStrings(appLanguage)
    val palette = LocalIocColors.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    var deletingItemRawValue by remember { mutableStateOf<String?>(null) }
    var pendingDeletedRawValues by remember { mutableStateOf<Set<String>>(emptySet()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedVerdictFilter by remember { mutableStateOf<VerdictLevel?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val filteredHistory = remember(history, searchQuery, selectedVerdictFilter, pendingDeletedRawValues) {
        filterScanHistory(
            history = history,
            searchQuery = searchQuery,
            selectedVerdictFilter = selectedVerdictFilter,
            excludedRawValues = pendingDeletedRawValues
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = palette.surfaceElevated,
            title = {
                Text(
                    text = strings.confirmClearHistoryTitle,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = strings.confirmClearHistoryMessage,
                    color = palette.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmDialog = false
                        onClear()
                    }
                ) {
                    Text(text = strings.confirmButton, color = palette.verdictMalicious, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(text = strings.cancelButton, color = palette.textSecondary)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (palette.isDark) 0.75f else 0.45f))
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
            colors = CardDefaults.cardColors(containerColor = palette.surfaceCard),
            border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.4f))
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
                    if (isRtl) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onClose() }
                                    .pointerHoverIcon(PointerIcon.Hand),
                                color = palette.surfaceInset,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "✕",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (history.isNotEmpty()) {
                                TextButton(
                                    onClick = { onExportJson(filteredHistory.ifEmpty { history }) },
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                                ) {
                                    Text(text = strings.exportHistoryButton, color = palette.accentPrimary, fontSize = 12.sp)
                                }
                                TextButton(
                                    onClick = { showClearConfirmDialog = true },
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                                ) {
                                    Text(text = strings.clearHistory, color = palette.verdictMalicious, fontSize = 12.sp)
                                }
                            }
                        }

                        Text(
                            text = "${strings.scanHistoryTitle} (${LocalizationProvider.formatNumber(history.size, appLanguage)})",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.accentPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "${strings.scanHistoryTitle} (${LocalizationProvider.formatNumber(history.size, appLanguage)})",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.accentPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (history.isNotEmpty()) {
                                TextButton(
                                    onClick = { onExportJson(filteredHistory.ifEmpty { history }) },
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                                ) {
                                    Text(text = strings.exportHistoryButton, color = palette.accentPrimary, fontSize = 12.sp)
                                }
                                TextButton(
                                    onClick = { showClearConfirmDialog = true },
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                                ) {
                                    Text(text = strings.clearHistory, color = palette.verdictMalicious, fontSize = 12.sp)
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onClose() }
                                    .pointerHoverIcon(PointerIcon.Hand),
                                color = palette.surfaceInset,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "✕",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (history.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(text = strings.searchHistoryPlaceholder, color = palette.textMuted, fontSize = 12.sp)
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = palette.textPrimary,
                            unfocusedTextColor = palette.textPrimary,
                            focusedBorderColor = palette.accentPrimary,
                            unfocusedBorderColor = palette.borderStrong
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val verdictOptions = listOf(
                            null to strings.filterAll,
                            VerdictLevel.MALICIOUS to LocalizationProvider.getVerdictName(VerdictLevel.MALICIOUS, appLanguage),
                            VerdictLevel.SUSPICIOUS to LocalizationProvider.getVerdictName(VerdictLevel.SUSPICIOUS, appLanguage),
                            VerdictLevel.CLEAN to LocalizationProvider.getVerdictName(VerdictLevel.CLEAN, appLanguage)
                        )
                        verdictOptions.forEach { (verdictLevel, label) ->
                            val isSelected = selectedVerdictFilter == verdictLevel
                            val chipColor = verdictLevel?.let { verdictColorFor(it, palette) } ?: palette.accentPrimary
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedVerdictFilter = verdictLevel }
                                    .pointerHoverIcon(PointerIcon.Hand),
                                color = if (isSelected) chipColor.copy(alpha = 0.18f) else palette.surfaceInset,
                                border = BorderStroke(0.8.dp, if (isSelected) chipColor else palette.borderSubtle),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = if (isSelected) chipColor else palette.textSecondary,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = palette.borderSubtle)

                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.noHistory,
                            color = palette.textMuted,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (filteredHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.noFilterResults,
                            color = palette.textMuted,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
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
                        filteredHistory.forEach { item ->
                            val isPendingDelete = item.indicator.rawValue in pendingDeletedRawValues
                            if (!isPendingDelete) {
                                val itemColor = verdictColorFor(item.overallVerdict, palette)

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
                                            .background(palette.surfaceInset)
                                            .border(0.6.dp, itemColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
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
                                                    color = palette.textPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                            val ts = formatUtcTimestamp(item.timestampEpochMs)
                                            val baseMeta = "${LocalizationProvider.getIndicatorTypeName(item.indicator.type, appLanguage)} • ${strings.threatConfidenceScore}: ${LocalizationProvider.formatScore(item.confidenceScore, appLanguage)}"
                                            Text(
                                                text = if (ts.isNotBlank()) "$baseMeta • $ts" else baseMeta,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textSecondary
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                color = itemColor.copy(alpha = 0.18f),
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
                                                color = palette.verdictMalicious.copy(alpha = 0.12f),
                                                shape = CircleShape
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "✕",
                                                        color = palette.verdictMalicious,
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
    val palette = LocalIocColors.current
    val monoFont = rememberMonospaceFontFamily()
    var deletingItemRawValue by remember { mutableStateOf<String?>(null) }
    var pendingDeletedRawValues by remember { mutableStateOf<Set<String>>(emptySet()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surfaceCard.copy(alpha = 0.96f)),
        border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.25f))
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
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = palette.accentPrimary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = LocalizationProvider.formatNumber(history.size, appLanguage),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accentPrimary,
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
                    color = palette.surfaceInset,
                    border = BorderStroke(1.dp, palette.borderSubtle)
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
                                color = palette.accentPrimary.copy(alpha = 0.12f),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.35f))
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
                                color = palette.textSecondary,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = strings.zeroKeyInstantScanReady,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.verdictClean,
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
                            val verdictColor = verdictColorFor(item.overallVerdict, palette)

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
                                    color = palette.surfaceInset,
                                    border = BorderStroke(0.8.dp, verdictColor.copy(alpha = 0.35f))
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
                                                border = BorderStroke(1.dp, verdictColor.copy(alpha = 0.6f))
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
                                                        color = palette.textPrimary,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontFamily = monoFont,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                                val ts = formatUtcTimestamp(item.timestampEpochMs)
                                                val baseMeta = "${LocalizationProvider.getIndicatorTypeName(item.indicator.type, appLanguage)} • ${strings.threatConfidenceScore}: ${LocalizationProvider.formatScore(item.confidenceScore, appLanguage)}"
                                                Text(
                                                    text = if (ts.isNotBlank()) "$baseMeta • $ts" else baseMeta,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.textSecondary,
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
                                                color = palette.accentPrimary.copy(alpha = 0.14f),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(0.8.dp, palette.accentPrimary.copy(alpha = 0.5f))
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
                                                        color = palette.accentPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                }
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
                                                color = palette.verdictMalicious.copy(alpha = 0.12f),
                                                shape = CircleShape
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "✕",
                                                        color = palette.verdictMalicious,
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

@Composable
private fun StatusBarNavChevron(
    isLeft: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalIocColors.current
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
        color = palette.surfaceElevated,
        border = BorderStroke(0.9.dp, palette.accentPrimary.copy(alpha = borderAlpha)),
        shape = RoundedCornerShape(6.dp),
        shadowElevation = if (isHovered) 3.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.accentPrimary.copy(alpha = glowAlpha)),
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
                    color = palette.accentPrimary,
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
    onOpenEnginesDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalIocColors.current
    val monoFont = rememberMonospaceFontFamily()
    val strings = LocalizationProvider.getStrings(appLanguage)
    var engineStatuses by remember { mutableStateOf(DEFAULT_PROBE_ENGINES) }
    var isProbing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val chipsScrollState = rememberScrollState()

    val anyProbed = remember(engineStatuses) { engineStatuses.any { it.hasProbed } }
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

    val infiniteTransition = rememberInfiniteTransition(label = "StatusBarAnimations")
    val beaconPulse by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconPulse"
    )
    val probeSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "probeSpin"
    )

    fun probeAllEngines() {
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
                logDiagnostic("EngineProbe", "Status bar batch probe failed", e)
                withContext(Dispatchers.Main) {
                    isProbing = false
                }
            } finally {
                client.close()
            }
        }
    }

    val barColor = palette.surfaceElevated.copy(alpha = 0.98f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp),
        color = barColor,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    palette.accentPrimary.copy(alpha = 0.15f),
                    palette.accentPrimary.copy(alpha = 0.45f),
                    palette.accentSecondary.copy(alpha = 0.55f),
                    palette.accentPrimary.copy(alpha = 0.45f),
                    palette.accentPrimary.copy(alpha = 0.15f)
                )
            )
        ),
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
                // Interactive Master Status Chip (Click to open full Threat Intel Providers modal)
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onOpenEnginesDialog)
                        .pointerHoverIcon(PointerIcon.Hand),
                    color = palette.surfaceInset,
                    border = BorderStroke(0.6.dp, palette.accentPrimary.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        val beaconColor = when {
                            !anyProbed -> palette.accentPrimary
                            onlineCount > 0 -> palette.verdictClean
                            else -> palette.verdictMalicious
                        }
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .graphicsLayer { alpha = if (!anyProbed || onlineCount > 0) beaconPulse else 1.0f }
                                .background(beaconColor)
                        )

                        Text(
                            text = if (!anyProbed) {
                                strings.probeUnchecked
                            } else {
                                when (appLanguage) {
                                    AppLanguage.PERSIAN -> {
                                        val onlineFmt = LocalizationProvider.formatNumber(onlineCount, appLanguage)
                                        val totalFmt = LocalizationProvider.formatNumber(totalCount, appLanguage)
                                        val pingFmt = LocalizationProvider.formatNumber(avgPing.toInt(), appLanguage)
                                        "$onlineFmt از $totalFmt موتور فعال • میانگین: ${pingFmt}ms"
                                    }
                                    AppLanguage.RUSSIAN -> "$onlineCount/$totalCount онлайн • Средний: ${avgPing}ms"
                                    AppLanguage.ENGLISH -> "$onlineCount/$totalCount Engines Online • Avg: ${avgPing}ms"
                                }
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                !anyProbed -> palette.textPrimary
                                onlineCount > 0 -> palette.textPrimary
                                else -> palette.verdictMalicious
                            },
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Interactive Probe / Ping All Button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = !isProbing) { probeAllEngines() }
                        .pointerHoverIcon(if (isProbing) PointerIcon.Default else PointerIcon.Hand),
                    color = if (isProbing) palette.accentPrimary.copy(alpha = 0.22f) else palette.surfaceInset,
                    border = BorderStroke(0.8.dp, palette.accentPrimary.copy(alpha = 0.45f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = if (isProbing) "⚡" else "🔄",
                            fontSize = 10.sp,
                            modifier = if (isProbing) Modifier.graphicsLayer { rotationZ = probeSpin } else Modifier
                        )
                        Text(
                            text = if (isProbing) strings.probingInProgress else strings.probeAllButton,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentPrimary,
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
                    .background(palette.borderSubtle)
            )

            // Slot 2: Full-Width Scrollable & Draggable Threat Engine Latency Chips (Zero Character Breaking)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canScrollLeft) {
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

                    if (canScrollLeft) {
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(26.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(barColor, Color.Transparent)
                                    )
                                )
                        )
                    }

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
                            val dotColor = when {
                                !engine.hasProbed -> palette.textMuted
                                engine.isOnline -> palette.verdictClean
                                else -> palette.verdictMalicious
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = palette.surfaceInset,
                                border = BorderStroke(0.7.dp, dotColor.copy(alpha = 0.30f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable(onClick = onOpenEnginesDialog)
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
                                        fontFamily = monoFont,
                                        fontWeight = FontWeight.Medium,
                                        color = palette.textSecondary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = when {
                                            !engine.hasProbed -> "—"
                                            engine.isOnline -> "${engine.pingMs}ms"
                                            else -> "ERR"
                                        },
                                        fontSize = 9.5.sp,
                                        fontFamily = monoFont,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            !engine.hasProbed -> palette.textMuted
                                            engine.isOnline -> palette.accentPrimary
                                            else -> palette.verdictMalicious
                                        },
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    if (canScrollRight) {
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(26.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, barColor)
                                    )
                                )
                        )
                    }

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
