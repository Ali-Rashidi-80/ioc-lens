package io.github.mrcoder20.ioclens.core.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import io.github.mrcoder20.ioclens.core.theme.PrimaryCyan
import io.github.mrcoder20.ioclens.core.theme.VerdictMalicious
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Authentic Thanos Dust Particle Model from PortX architecture.
 */
internal data class ThanosDustParticle(
    val relX: Float,
    val relY: Float,
    val birthProgress: Float,
    val vx: Float,
    val vy: Float,
    val baseSize: Float,
    val aspectRatio: Float,
    val isRect: Boolean,
    val isEmber: Boolean,
    val rotation: Float,
    val vRot: Float,
    val color: Color,
    val initialAlpha: Float,
    val lifeSpan: Float,
    val swirlFreq: Float,
    val swirlAmp: Float,
    val phase: Float,
    val divergenceX: Float
)

/**
 * Authentic Thanos Snap Disintegration Container adapted from PortX.
 *
 * Implements:
 * 1. 2D Mesh lattice distribution with 1,800 micro-particles.
 * 2. Directional sweeping wavefront: RTL sweeps Right-to-Left, LTR sweeps Left-to-Right.
 * 3. High-contrast cyber palette: silver titanium powder, burning ember sparks, and cyber cyan sparks.
 * 4. Aerodynamic upward lift, lateral wind drift, and harmonic curl turbulence.
 * 5. Smooth zero-jump layout height collapse before deletion completes.
 */
@Composable
fun DisintegrationContainer(
    isDisintegrating: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = VerdictMalicious,
    materialColors: List<Color>? = null,
    particleCount: Int = 1800,
    collapseHeight: Boolean = true,
    durationMs: Int = 1200,
    onDisintegrated: () -> Unit,
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(0f) }
    val heightScale = remember { Animatable(1f) }
    var hasCompletedDisintegration by remember { mutableStateOf(false) }

    var capturedWidth by remember { mutableStateOf(0f) }
    var capturedHeight by remember { mutableStateOf(0f) }

    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    // Pre-generate micro-particles across a continuous 2D lattice
    val particles = remember(isDisintegrating, materialColors, accent, isRtl) {
        if (!isDisintegrating) emptyList()
        else {
            val palette = materialColors ?: listOf(
                Color(0xFFCBD5E1), // Fine silver ash powder
                Color(0xFF94A3B8), // Light slate dust
                Color(0xFF64748B), // Slate 500
                Color(0xFFE2E8F0), // Titanium ash
                Color.White,       // Header white
                PrimaryCyan,       // Cyber cyan spark
                accent,            // Threat accent
                Color(0xFFFF7043), // Hot burning ember
                Color(0xFFFFCA28)  // Incandescent gold spark
            )

            val count = particleCount.coerceIn(1000, 2600)
            val cols = 50
            val rows = (count / cols).coerceAtLeast(20)
            val cellW = 1f / cols
            val cellH = 1f / rows

            val list = ArrayList<ThanosDustParticle>(cols * rows)
            for (c in 0 until cols) {
                for (r in 0 until rows) {
                    val relX = (c.toFloat() / cols + (Random.nextFloat() - 0.5f) * cellW * 0.95f).coerceIn(0.005f, 0.995f)
                    val relY = (r.toFloat() / rows + (Random.nextFloat() - 0.5f) * cellH * 0.95f).coerceIn(0.005f, 0.995f)

                    // Wave sweeps across the card: in RTL from right to left, in LTR from left to right
                    val normX = if (isRtl) (1f - relX) else relX
                    val rawWave = normX * 0.46f + (1f - relY) * 0.05f + (Random.nextFloat() - 0.5f) * 0.04f
                    val birth = rawWave.coerceIn(0.01f, 0.54f)

                    val randType = Random.nextFloat()
                    val isEmber = randType < 0.15f
                    val isRect = randType in 0.15f..0.75f

                    val baseSize = if (isEmber) (Random.nextFloat() * 1.6f + 1.2f)
                    else if (isRect) (Random.nextFloat() * 1.8f + 1.3f)
                    else (Random.nextFloat() * 1.2f + 0.8f)
                    val aspectRatio = if (isRect) (Random.nextFloat() * 1.3f + 1.1f) else 1.0f

                    // Directional wind aligned with wave propagation (RTL blows left, LTR blows right)
                    val windDir = if (isRtl) -1f else 1f
                    val windBias = windDir * (Random.nextFloat() * 80f + 40f)
                    val vx = (Random.nextFloat() - 0.5f) * 60f + windBias
                    val vy = -(Random.nextFloat() * 135f + 65f) // Upward aerodynamic detachment pop

                    val chosenColor = if (isEmber) {
                        if (Random.nextBoolean()) Color(0xFFFF7043) else Color(0xFFFFCA28)
                    } else {
                        palette[Random.nextInt(palette.size)]
                    }

                    list.add(
                        ThanosDustParticle(
                            relX = relX,
                            relY = relY,
                            birthProgress = birth,
                            vx = vx,
                            vy = vy,
                            baseSize = baseSize,
                            aspectRatio = aspectRatio,
                            isRect = isRect,
                            isEmber = isEmber,
                            rotation = Random.nextFloat() * 360f,
                            vRot = (Random.nextFloat() - 0.5f) * 520f,
                            color = chosenColor,
                            initialAlpha = if (isEmber) 0.95f else (Random.nextFloat() * 0.28f + 0.72f),
                            lifeSpan = Random.nextFloat() * 0.25f + 0.50f,
                            swirlFreq = Random.nextFloat() * 7f + 3f,
                            swirlAmp = Random.nextFloat() * 16f + 8f,
                            phase = Random.nextFloat() * (PI.toFloat() * 2f),
                            divergenceX = (Random.nextFloat() - 0.5f) * 85f
                        )
                    )
                }
            }
            list
        }
    }

    LaunchedEffect(isDisintegrating) {
        if (isDisintegrating && !hasCompletedDisintegration) {
            progress.snapTo(0f)
            heightScale.snapTo(1f)

            // 1. Overall Particle and Wavefront Lifecycle
            val animJob = launch {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = durationMs,
                        easing = LinearEasing
                    )
                )
            }

            // 2. Smooth Height Collapse
            if (collapseHeight) {
                launch {
                    val delayTime = (durationMs * 0.45f).toLong()
                    delay(delayTime)
                    val remainingTime = (durationMs * 0.45f).toInt().coerceAtLeast(180)
                    heightScale.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = remainingTime,
                            easing = CubicBezierEasing(0.35f, 0.0f, 0.15f, 1.0f)
                        )
                    )
                }
            }

            animJob.join()
            hasCompletedDisintegration = true
            onDisintegrated()
        } else if (!isDisintegrating) {
            progress.snapTo(0f)
            heightScale.snapTo(1f)
            hasCompletedDisintegration = false
        }
    }

    val layoutMod = if (collapseHeight && isDisintegrating) {
        Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            if (placeable.height > 0 && capturedHeight == 0f) {
                capturedHeight = placeable.height.toFloat()
            }
            if (placeable.width > 0 && capturedWidth == 0f) {
                capturedWidth = placeable.width.toFloat()
            }
            val h = (placeable.height * heightScale.value).roundToInt()
            layout(placeable.width, h) {
                placeable.placeRelative(0, 0)
            }
        }
    } else {
        Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            if (placeable.height > 0 && capturedHeight == 0f) {
                capturedHeight = placeable.height.toFloat()
            }
            if (placeable.width > 0 && capturedWidth == 0f) {
                capturedWidth = placeable.width.toFloat()
            }
            layout(placeable.width, placeable.height) {
                placeable.placeRelative(0, 0)
            }
        }
    }

    Box(
        modifier = modifier
            .then(layoutMod)
            .graphicsLayer { clip = false }
    ) {
        if (!hasCompletedDisintegration) {
            // Content with sweeping horizontal erosion wavefront
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        if (isDisintegrating) {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                    }
                    .drawWithContent {
                        val p = progress.value
                        if (!isDisintegrating || p <= 0.001f) {
                            drawContent()
                        } else if (p >= 0.54f) {
                            // Beyond 0.54, original solid content is 100% disintegrated into dust
                        } else {
                            drawContent()
                            val w = size.width
                            val waveNorm = (p / 0.50f).coerceIn(0f, 1f)
                            val transitionBand = (w * 0.18f).coerceIn(24f, 120f)

                            val maskBrush = if (isRtl) {
                                // Right-to-Left sweep: Right side dissolves first
                                val waveX = w * (1f - waveNorm)
                                val startFrac = ((waveX - transitionBand) / w).coerceIn(0f, 1f)
                                val endFrac = (waveX / w).coerceIn(0f, 1f)
                                Brush.horizontalGradient(
                                    0.0f to Color.Black,
                                    startFrac to Color.Black,
                                    endFrac to Color.Transparent,
                                    1.0f to Color.Transparent
                                )
                            } else {
                                // Left-to-Right sweep: Left side dissolves first
                                val waveX = w * waveNorm
                                val startFrac = (waveX / w).coerceIn(0f, 1f)
                                val endFrac = ((waveX + transitionBand) / w).coerceIn(0f, 1f)
                                Brush.horizontalGradient(
                                    0.0f to Color.Transparent,
                                    startFrac to Color.Transparent,
                                    endFrac to Color.Black,
                                    1.0f to Color.Black
                                )
                            }

                            drawRect(
                                brush = maskBrush,
                                blendMode = BlendMode.DstIn
                            )
                        }
                    }
            ) {
                content()
            }
        }

        // Real-Time High-Density Dust Particle Canvas Overlay
        if (isDisintegrating && progress.value in 0.001f..0.999f) {
            val animProgress = progress.value
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { clip = false }
            ) {
                val w = if (capturedWidth > 0f) capturedWidth else size.width
                val h = if (capturedHeight > 0f) capturedHeight else size.height

                particles.forEach { p ->
                    if (animProgress >= p.birthProgress) {
                        val localAge = ((animProgress - p.birthProgress) / p.lifeSpan).coerceIn(0f, 1f)
                        if (localAge < 1f) {
                            val fadeCurve = 1f - localAge * localAge
                            val alpha = (p.initialAlpha * fadeCurve).coerceIn(0f, 1f)

                            if (alpha > 0.015f) {
                                val ageEased = 1f - (1f - localAge) * (1f - localAge)
                                val thermalLift = -(localAge * localAge * 125f)
                                val curY = p.relY * h + p.vy * ageEased + thermalLift

                                val windDrift = p.vx * ageEased
                                val curl = sin(localAge * p.swirlFreq + p.phase) * p.swirlAmp * ageEased
                                val curX = p.relX * w + windDrift + curl + (p.relX - 0.5f) * p.divergenceX * ageEased

                                val sizeFraction = if (localAge < 0.20f) 1f else (1f - (localAge - 0.20f) / 0.80f)
                                val curSize = (p.baseSize * sizeFraction).coerceAtLeast(0.6f)
                                val finalAlpha = (alpha * p.color.alpha).coerceIn(0f, 1f)

                                if (p.isEmber) {
                                    // Burning ember spark: intense glowing dot with radiant aura
                                    drawCircle(
                                        color = p.color.copy(alpha = finalAlpha * 0.45f),
                                        radius = curSize * 1.6f,
                                        center = Offset(curX, curY)
                                    )
                                    drawCircle(
                                        color = Color.White.copy(alpha = finalAlpha),
                                        radius = curSize * 0.7f,
                                        center = Offset(curX, curY)
                                    )
                                } else if (p.isRect) {
                                    val curRot = p.rotation + p.vRot * localAge
                                    val hw = (curSize * p.aspectRatio) / 2f
                                    val hh = curSize / 2f
                                    rotate(curRot, Offset(curX, curY)) {
                                        drawRect(
                                            color = p.color.copy(alpha = finalAlpha),
                                            topLeft = Offset(curX - hw, curY - hh),
                                            size = Size(hw * 2f, hh * 2f)
                                        )
                                    }
                                } else {
                                    drawCircle(
                                        color = p.color.copy(alpha = finalAlpha),
                                        radius = curSize * 0.75f,
                                        center = Offset(curX, curY)
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

/**
 * Tactile Spring Press Micro-Interaction Modifier from PortX.
 */
fun Modifier.springPress(
    pressedScale: Float = 0.93f,
    interactionSource: MutableInteractionSource? = null
): Modifier = this.composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    this.graphicsLayer {
        scaleX = animatedScale
        scaleY = animatedScale
    }
}

/**
 * Ambient Breathing Glow Pulse from PortX.
 */
fun Modifier.cyberPulse(
    glowColor: Color,
    enabled: Boolean = true,
    minAlpha: Float = 0.3f,
    maxAlpha: Float = 0.85f,
    durationMs: Int = 1600
): Modifier = this.composed {
    if (!enabled) return@composed this

    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    this.graphicsLayer {
        shadowElevation = 8f * alpha
        ambientShadowColor = glowColor.copy(alpha = alpha)
        spotShadowColor = glowColor.copy(alpha = alpha)
    }
}

