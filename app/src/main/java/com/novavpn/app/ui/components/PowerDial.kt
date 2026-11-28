package com.novavpn.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Dark colour for text/icons drawn on top of the yellow disc. */
val DialInk = Color(0xFF090A02)

private val DialYellow = Color(0xFFE6FF00)
private val DialYellowDim = Color(0xFF7F8A00)
private val DialTick = Color(0xFF8C9A12)
private val DialTickHot = Color(0xFFE4FF4F)

private const val TICK_COUNT = 96

/**
 * Ring of tick marks around a solid yellow disc, with [content] (status + timer) centred
 * inside the disc. The composable's size is the ring's outer diameter.
 *
 * [live] = true  -> fully lit yellow disc, ticks glow softly with a slow travelling shimmer.
 * [live] = false -> (connecting / disconnecting) dimmer disc that pulses, with a bright
 *                   streak of ticks sweeping quickly around the ring.
 */
@Composable
fun PowerDial(
    modifier: Modifier = Modifier,
    live: Boolean,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val infinite = rememberInfiniteTransition(label = "dial")
    val fast by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "fast",
    )
    val slow by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(7000, easing = LinearEasing)),
        label = "slow",
    )
    val breath by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    val litAmt by animateFloatAsState(if (live) 1f else 0f, tween(700), label = "lit")

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val R = size.minDimension / 2f
            val phase = if (live) slow else fast

            // Tick ring
            val inner = R * 0.905f
            val outer = R * 0.995f
            val tickW = (R * 0.014f).coerceAtLeast(1.5f)
            val head = phase * TICK_COUNT
            val trailLen = 22f + 18f * litAmt
            val base = 0.28f + 0.27f * litAmt
            val gain = 0.72f - 0.42f * litAmt
            for (i in 0 until TICK_COUNT) {
                val a = (i.toFloat() / TICK_COUNT) * 2f * PI.toFloat() - (PI / 2).toFloat()
                val delta = (((head - i) % TICK_COUNT) + TICK_COUNT) % TICK_COUNT
                val trail = (1f - delta / trailLen).coerceIn(0f, 1f)
                val t2 = trail * trail
                val color = lerp(DialTick, DialTickHot, t2)
                    .copy(alpha = (base + gain * t2).coerceIn(0f, 1f))
                val cs = cos(a)
                val sn = sin(a)
                drawLine(
                    color,
                    Offset(c.x + cs * inner, c.y + sn * inner),
                    Offset(c.x + cs * outer, c.y + sn * outer),
                    strokeWidth = tickW,
                    cap = StrokeCap.Butt,
                )
            }

            // Yellow disc (dimmer + pulsing while connecting)
            val r2 = R * 0.80f
            val pulse = (1f - litAmt) * 0.35f * breath
            val fill = lerp(lerp(DialYellowDim, DialYellow, litAmt), DialYellowDim, pulse)

            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to fill.copy(alpha = 0.28f * (0.4f + 0.6f * litAmt)),
                    1.0f to Color.Transparent,
                    center = c,
                    radius = r2 * 1.14f,
                ),
                radius = r2 * 1.14f,
                center = c,
            )
            drawCircle(fill, radius = r2, center = c)
            // subtle inset shading at the edge
            drawCircle(
                brush = Brush.radialGradient(
                    0.80f to Color.Transparent,
                    1.00f to Color.Black.copy(alpha = 0.12f),
                    center = c,
                    radius = r2,
                ),
                radius = r2,
                center = c,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}
