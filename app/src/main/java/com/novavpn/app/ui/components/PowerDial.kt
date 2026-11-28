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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val DialBlack = Color(0xFF090A02)
private val DialYellow = Color(0xFFE6FF00)
private val DialYellowDim = Color(0xFF7F8A00)
private val DialTick = Color(0xFF8C9A12)
private val DialTickHot = Color(0xFFE4FF4F)
private val DialHalo = Color(0xFFB4C200)

private const val TICK_COUNT = 96

/**
 * Power-button dial: soft halo rings, a black disc with a ring of tick marks, a solid yellow
 * disc and a black power glyph.
 *
 * The composable's size is the black disc's diameter; the faint halos are drawn outside it.
 *
 * [live] = true  -> fully lit yellow disc, ticks glow softly with a slow travelling shimmer.
 * [live] = false -> (connecting / disconnecting) dimmer disc that pulses, with a bright
 *                   comet of ticks sweeping around the ring.
 */
@Composable
fun PowerDial(modifier: Modifier = Modifier, live: Boolean) {
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

    Canvas(modifier) {
        val c = center
        val R = size.minDimension / 2f
        val phase = if (live) slow else fast

        // Faint halo rings outside the disc
        val haloA = 0.05f + 0.03f * breath
        drawCircle(DialHalo.copy(alpha = haloA * 0.6f), radius = R * 1.46f, center = c)
        drawCircle(DialHalo.copy(alpha = haloA), radius = R * 1.27f, center = c)

        // Black disc
        drawCircle(DialBlack, radius = R, center = c)

        // Tick ring
        val inner = R * 0.78f
        val outer = R * 0.86f
        val tickW = (R * 0.02f).coerceAtLeast(1.5.dp.toPx() * 0.8f)
        val head = phase * TICK_COUNT
        val trailLen = 22f + 18f * litAmt
        val base = 0.28f + 0.27f * litAmt
        val gain = 0.72f - 0.42f * litAmt
        for (i in 0 until TICK_COUNT) {
            val a = (i.toFloat() / TICK_COUNT) * 2f * PI.toFloat() - (PI / 2).toFloat()
            val delta = (((head - i) % TICK_COUNT) + TICK_COUNT) % TICK_COUNT
            val trail = (1f - delta / trailLen).coerceIn(0f, 1f)
            val t2 = trail * trail
            val color = lerp(DialTick, DialTickHot, t2).copy(alpha = (base + gain * t2).coerceIn(0f, 1f))
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
        val r2 = R * 0.685f
        val pulse = (1f - litAmt) * 0.35f * breath
        val fill = lerp(lerp(DialYellowDim, DialYellow, litAmt), DialYellowDim, pulse)

        // soft glow around the disc
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to fill.copy(alpha = 0.30f * (0.4f + 0.6f * litAmt)),
                1.0f to Color.Transparent,
                center = c,
                radius = r2 * 1.3f,
            ),
            radius = r2 * 1.3f,
            center = c,
        )
        drawCircle(fill, radius = r2, center = c)
        // subtle inset shading at the edge
        drawCircle(
            brush = Brush.radialGradient(
                0.78f to Color.Transparent,
                1.00f to Color.Black.copy(alpha = 0.12f),
                center = c,
                radius = r2,
            ),
            radius = r2,
            center = c,
        )

        // Power glyph
        val sw = r2 * 0.076f
        val ir = r2 * 0.38f
        drawArc(
            color = DialBlack,
            startAngle = -40f,
            sweepAngle = 260f,
            useCenter = false,
            topLeft = Offset(c.x - ir, c.y - ir),
            size = Size(ir * 2f, ir * 2f),
            style = Stroke(width = sw, cap = StrokeCap.Butt),
        )
        drawLine(
            DialBlack,
            Offset(c.x, c.y - r2 * 0.395f),
            Offset(c.x, c.y - r2 * 0.07f),
            strokeWidth = sw,
            cap = StrokeCap.Butt,
        )
    }
}
