package com.novavpn.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.novavpn.app.ui.theme.Nova
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The glowing lime plasma sphere. Drawn entirely with Canvas so it works on every API level.
 * [speed] > 1 spins the swirls faster (used while connecting).
 */
@Composable
fun OrbView(modifier: Modifier = Modifier, speed: Float = 1f) {
    val infinite = rememberInfiniteTransition(label = "orb")
    val t by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween((36000 / speed).toInt(), easing = LinearEasing)),
        label = "t",
    )

    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f * 0.76f

        // Outer glow
        drawCircle(
            brush = Brush.radialGradient(
                0f to Nova.Lime.copy(alpha = 0.42f),
                0.55f to Nova.Lime.copy(alpha = 0.14f),
                1f to Color.Transparent,
                center = c,
                radius = r * 1.5f,
            ),
            radius = r * 1.5f,
            center = c,
        )

        // Body
        drawCircle(
            brush = Brush.radialGradient(
                0f to Nova.OrbDeep,
                0.5f to Nova.OrbMid,
                0.86f to Nova.OrbBright,
                1f to Nova.OrbGlow,
                center = c,
                radius = r,
            ),
            radius = r,
            center = c,
        )

        // Swirling strands, clipped to the sphere
        val clip = Path().apply { addOval(Rect(c, r)) }
        clipPath(clip) {
            val count = 18
            for (i in 0 until count) {
                val dir = if (i % 2 == 0) 1f else -1f
                val ph = t * (0.6f + (i % 5) * 0.18f) * dir + i * (2f * PI.toFloat() / count)

                fun pt(a: Float, k: Float) = Offset(c.x + cos(a) * r * k, c.y + sin(a) * r * k)

                val s = pt(ph, 1.03f)
                val c1 = pt(ph + 0.9f, 0.62f + 0.18f * sin(t * 2f + i))
                val c2 = pt(ph + 1.9f, 0.46f + 0.18f * cos(t * 1.5f + i))
                val e = pt(ph + 2.8f, 1.03f)

                val path = Path().apply {
                    moveTo(s.x, s.y)
                    cubicTo(c1.x, c1.y, c2.x, c2.y, e.x, e.y)
                }
                drawPath(
                    path,
                    color = lerp(Nova.Lime, Color.White, (i % 4) / 4f)
                        .copy(alpha = 0.16f + 0.09f * (i % 3)),
                    style = Stroke(width = (1.5f + (i % 4)).dp.toPx(), cap = StrokeCap.Round),
                )
            }

            // Soft specular highlight
            drawCircle(
                brush = Brush.radialGradient(
                    0f to Color.White.copy(alpha = 0.28f),
                    1f to Color.Transparent,
                    center = Offset(c.x - r * 0.3f, c.y - r * 0.38f),
                    radius = r * 0.55f,
                ),
                radius = r * 0.55f,
                center = Offset(c.x - r * 0.3f, c.y - r * 0.38f),
            )
        }

        // Rim + fake bloom
        drawCircle(Nova.Lime.copy(alpha = 0.9f), r, c, style = Stroke(2.dp.toPx()))
        for (k in 1..5) {
            drawCircle(
                Nova.Lime.copy(alpha = 0.16f / k),
                radius = r + k * 2.6.dp.toPx(),
                center = c,
                style = Stroke(3.dp.toPx()),
            )
        }
    }
}
