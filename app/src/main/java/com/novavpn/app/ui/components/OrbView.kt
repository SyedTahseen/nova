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
        animationSpec = infiniteRepeatable(tween((48000 / speed).toInt(), easing = LinearEasing)),
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
                0.58f to Nova.OrbMid,
                0.86f to Nova.OrbBright,
                1f to Nova.OrbGlow,
                center = c,
                radius = r,
            ),
            radius = r,
            center = c,
        )

        // Swirl strips: broad translucent bands (not thin lines), clipped to the sphere.
        // Their curves bend in only to ~0.5r, so the middle stays dark behind the status text.
        val clip = Path().apply { addOval(Rect(c, r)) }
        clipPath(clip) {
            val count = 12
            for (i in 0 until count) {
                // Free drift, not rotation: every strip wanders on its own with a few slow sine waves.
                // Frequencies are whole numbers of cycles per loop, so the animation repeats without a jump.
                val f = i * 1.3f
                val a0 = i * (2f * PI.toFloat() / count)
                val ph = a0 + 0.8f * sin(t + f) + 0.45f * sin(3f * t + 2.1f * f)
                val span = 2.5f + 0.7f * sin(2f * t + 0.9f * f)                  // how far round the rim it reaches

                fun pt(a: Float, k: Float) = Offset(c.x + cos(a) * r * k, c.y + sin(a) * r * k)

                val s = pt(ph, 1.05f)
                val c1 = pt(ph + 0.9f + 0.5f * sin(2f * t + 1.1f * f), 0.82f + 0.13f * sin(3f * t + 0.7f * f))
                val c2 = pt(ph + 1.9f + 0.5f * cos(t + 1.9f * f), 0.70f + 0.13f * cos(2f * t + 1.3f * f))
                val e = pt(ph + span, 1.05f)

                val path = Path().apply {
                    moveTo(s.x, s.y)
                    cubicTo(c1.x, c1.y, c2.x, c2.y, e.x, e.y)
                }
                val tint = lerp(Nova.Lime, Color.White, (i % 4) / 4f)
                val w = r * (0.15f + 0.04f * (i % 3)) * (0.85f + 0.30f * sin(2f * t + f))   // width slowly swells and thins
                val br = 0.75f + 0.25f * sin(3f * t + 1.7f * f)                          // opacity slowly pulses

                // feathered edge -> body -> brighter core, so the strip reads as translucent smoke
                drawPath(path, tint.copy(alpha = 0.05f * br), style = Stroke(w * 1.5f, cap = StrokeCap.Round))
                drawPath(path, tint.copy(alpha = 0.09f * br), style = Stroke(w, cap = StrokeCap.Round))
                drawPath(path, tint.copy(alpha = (0.11f + 0.03f * (i % 3)) * br), style = Stroke(w * 0.45f, cap = StrokeCap.Round))
                // fine line along the strip
                drawPath(path, tint.copy(alpha = 0.22f * br), style = Stroke(1.dp.toPx(), cap = StrokeCap.Round))
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

            // gentle fade so the very middle stays clear
            drawCircle(
                brush = Brush.radialGradient(
                    0f to Nova.OrbDeep.copy(alpha = 0.9f),
                    0.5f to Nova.OrbDeep.copy(alpha = 0.9f),
                    1f to Color.Transparent,
                    center = c,
                    radius = r * 0.5f,
                ),
                radius = r * 0.5f,
                center = c,
            )

        }

    }
}
