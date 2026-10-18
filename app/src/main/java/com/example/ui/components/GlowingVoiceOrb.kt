package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AssistantStatus
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.RoseGlow
import com.example.ui.theme.StatusError
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GlowingVoiceOrb(
    assistantStatus: AssistantStatus,
    userAudioLevel: Float,
    mayraAudioLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    val basePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "base_pulse"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbital_rotation"
    )

    val fastRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_rotation"
    )

    // Smooth reactive levels
    val animatedUserLevel by animateFloatAsState(
        targetValue = userAudioLevel.coerceIn(0f, 1f),
        animationSpec = tween(80),
        label = "smooth_user_level"
    )

    val animatedMayraLevel by animateFloatAsState(
        targetValue = mayraAudioLevel.coerceIn(0f, 1f),
        animationSpec = tween(80),
        label = "smooth_mayra_level"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(300.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("glowing_voice_orb"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f

            val effectiveMayraScale = basePulse + (animatedMayraLevel * 0.45f)
            val effectiveUserScale = basePulse + (animatedUserLevel * 0.45f)

            // Determine primary colors based on status
            val primaryColor: Color
            val secondaryColor: Color
            val glowColor: Color

            when (assistantStatus) {
                AssistantStatus.Speaking -> {
                    primaryColor = NeonMagenta
                    secondaryColor = ElectricPurple
                    glowColor = RoseGlow
                }
                AssistantStatus.Listening -> {
                    primaryColor = ElectricCyan
                    secondaryColor = ElectricPurple
                    glowColor = ElectricCyan
                }
                AssistantStatus.Thinking -> {
                    primaryColor = ElectricPurple
                    secondaryColor = NeonMagenta
                    glowColor = ElectricCyan
                }
                AssistantStatus.Interrupted -> {
                    primaryColor = StatusError
                    secondaryColor = RoseGlow
                    glowColor = StatusError
                }
                AssistantStatus.Idle -> {
                    primaryColor = NeonMagenta.copy(alpha = 0.8f)
                    secondaryColor = ElectricCyan.copy(alpha = 0.7f)
                    glowColor = ElectricPurple
                }
            }

            // 1. Outermost aura / atmospheric glow
            val outerAuraRadius = maxRadius * 0.95f * (if (assistantStatus == AssistantStatus.Speaking) effectiveMayraScale else effectiveUserScale)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.28f),
                        glowColor.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = outerAuraRadius
                ),
                radius = outerAuraRadius,
                center = center
            )

            // 2. Dynamic acoustic ripples (rings)
            val ringCount = 3
            for (i in 1..ringCount) {
                val ringMultiplier = 0.55f + (i * 0.15f)
                val ringRadius = maxRadius * ringMultiplier * (if (assistantStatus == AssistantStatus.Speaking) effectiveMayraScale else effectiveUserScale)
                val ringAlpha = (0.4f / i) * (0.5f + (animatedMayraLevel + animatedUserLevel) * 0.5f)
                drawCircle(
                    color = primaryColor.copy(alpha = ringAlpha.coerceIn(0f, 1f)),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // 3. Rotating orbital frequency ring with nodes
            rotate(rotationAngle, pivot = center) {
                val orbitRadius = maxRadius * 0.68f * basePulse
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(
                            primaryColor.copy(alpha = 0.8f),
                            secondaryColor.copy(alpha = 0.2f),
                            primaryColor.copy(alpha = 0.9f),
                            secondaryColor.copy(alpha = 0.3f),
                            primaryColor.copy(alpha = 0.8f)
                        ),
                        center = center
                    ),
                    radius = orbitRadius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Particle nodes on the orbit
                val nodeCount = 6
                for (j in 0 until nodeCount) {
                    val angle = (j * (2 * PI / nodeCount)).toFloat()
                    val nodeOffset = Offset(
                        center.x + orbitRadius * cos(angle),
                        center.y + orbitRadius * sin(angle)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = 3.dp.toPx(),
                        center = nodeOffset
                    )
                }
            }

            // 4. Counter-rotating inner energy ring
            rotate(fastRotation, pivot = center) {
                val innerOrbitRadius = maxRadius * 0.52f
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.6f),
                    radius = innerOrbitRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // 5. Core glowing sphere
            val coreRadius = maxRadius * 0.42f * (if (assistantStatus == AssistantStatus.Speaking) effectiveMayraScale else effectiveUserScale)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryColor,
                        secondaryColor,
                        primaryColor.copy(alpha = 0.6f)
                    ),
                    center = Offset(center.x - coreRadius * 0.25f, center.y - coreRadius * 0.25f),
                    radius = coreRadius * 1.2f
                ),
                radius = coreRadius,
                center = center
            )

            // 6. Highlight specular shine
            val specularRadius = coreRadius * 0.28f
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = specularRadius,
                center = Offset(center.x - coreRadius * 0.35f, center.y - coreRadius * 0.35f)
            )
        }
    }
}
