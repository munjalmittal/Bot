package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VortexCyan
import com.example.ui.theme.VortexCyanGlow
import com.example.ui.theme.VortexDarkBg
import com.example.ui.theme.VortexPurple
import com.example.ui.theme.VortexPurpleGlow
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SonicOrb(
    isListening: Boolean,
    audioRmsLevel: Float,
    statusText: String,
    wakeWord: String,
    onOrbClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_rotation")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 3000 else 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    val dynamicScale = if (isListening) {
        1.0f + (audioRmsLevel * 0.35f)
    } else {
        breathingPulse
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(200.dp)
                .scale(dynamicScale)
                .testTag("sonic_orb_button")
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 90.dp),
                    onClick = onOrbClick
                )
        ) {
            // Background Canvas for futuristic energetic soundwaves
            Canvas(modifier = Modifier.size(200.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.width * 0.42f

                // Outer ambient glow ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isListening) VortexCyanGlow.copy(alpha = 0.45f) else VortexPurpleGlow.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.width / 2f
                    )
                )

                // Secondary sonic pulse ring
                drawCircle(
                    color = if (isListening) VortexCyan.copy(alpha = 0.6f) else VortexPurple.copy(alpha = 0.3f),
                    radius = baseRadius,
                    style = Stroke(width = if (isListening) 4.dp.toPx() else 2.dp.toPx())
                )

                // High-tech orbiting tick marks
                val tickCount = 24
                for (i in 0 until tickCount) {
                    val angleDeg = (i * (360f / tickCount) + rotationAngle).toDouble()
                    val rad = Math.toRadians(angleDeg)
                    val outerR = baseRadius + (if (i % 2 == 0) 12.dp.toPx() else 6.dp.toPx())
                    val innerR = baseRadius + 3.dp.toPx()

                    val startX = (center.x + innerR * cos(rad)).toFloat()
                    val startY = (center.y + innerR * sin(rad)).toFloat()
                    val endX = (center.x + outerR * cos(rad)).toFloat()
                    val endY = (center.y + outerR * sin(rad)).toFloat()

                    drawLine(
                        color = if (isListening) VortexCyan else VortexPurpleGlow.copy(alpha = 0.5f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            // Inner Orb core showing Blacki Cat
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(116.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFDE68A))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.blacki_cat_logo),
                    contentDescription = "Blacki Cat",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(116.dp)
                )

                // Corner Mic badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isListening) VortexCyan else VortexDarkBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = if (isListening) "Listening active" else "Mic paused",
                        tint = if (isListening) VortexDarkBg else VortexCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // State indicator text
        Text(
            text = if (isListening) "LISTENING FOR \"$wakeWord\"" else "TAP OR CALL \"$wakeWord\"",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = if (isListening) VortexCyan else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = statusText,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}
