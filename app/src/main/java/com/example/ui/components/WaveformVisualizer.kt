package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AssistantStatus
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.RoseGlow
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    assistantStatus: AssistantStatus,
    userAudioLevel: Float,
    mayraAudioLevel: Float,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "waveform_phase")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val activeLevel = if (assistantStatus == AssistantStatus.Speaking) {
        mayraAudioLevel
    } else {
        userAudioLevel
    }

    val primaryColor = if (assistantStatus == AssistantStatus.Speaking) {
        NeonMagenta
    } else {
        ElectricCyan
    }

    val secondaryColor = if (assistantStatus == AssistantStatus.Speaking) {
        RoseGlow
    } else {
        ElectricPurple
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("waveform_visualizer")
    ) {
        val barCount = 28
        val spacing = 6.dp.toPx()
        val totalSpacing = (barCount - 1) * spacing
        val barWidth = (size.width - totalSpacing) / barCount
        val centerY = size.height / 2f
        val maxBarHeight = size.height * 0.9f

        for (i in 0 until barCount) {
            val normalizedX = i.toFloat() / barCount.toFloat()
            val wave = ((sin(normalizedX.toDouble() * 4.0 * Math.PI + phase.toDouble()) + 1.0) / 2.0).toFloat()
            val envelope = sin(normalizedX.toDouble() * Math.PI).toFloat()

            val baseMinHeight = 6.dp.toPx()
            val dynamicHeight = (baseMinHeight + (wave * 0.5f + 0.5f) * envelope * activeLevel * maxBarHeight)
                .coerceIn(baseMinHeight, maxBarHeight)

            val x = (i * (barWidth + spacing))
            val y = centerY - (dynamicHeight / 2f)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor, secondaryColor, primaryColor),
                    startY = y,
                    endY = y + dynamicHeight
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, dynamicHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
