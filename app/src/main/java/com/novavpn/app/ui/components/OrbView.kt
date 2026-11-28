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
import kotlin.math.floor
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

        // Smoke wisps. Each wisp has a life: it fades in at a random spot inside the sphere, drifts and curls
        // while it spreads out, then fades away; the next life starts somewhere else. Nothing is a fixed ring.
        val clip = Path().apply { addOval(Rect(c, r)) }
        clipPath(clip) {
            val tau = 2f * PI.toFloat()
            val count = 10
            for (i in 0 until count) {
                val n = 2 + i % 3                              // 2–4 lives per loop (≈12–24 s each at normal speed)
                val x = n * t / tau + i * 0.37f
                val u = x - floor(x)                           // 0..1 progress through this life
                val id = ((floor(x).toInt() % n) + n) % n      // which life: repeats every loop, so no jump at the seam
                val env = sin(PI.toFloat() * u).let { it * it } // fade in, fade out
                if (env < 0.01f) continue

                // where this life happens (random per wisp and life)
                val ang0 = rnd(i, id, 1) * tau
                val rad0 = 0.5f + 0.4f * rnd(i, id, 2)
                val dirA = rnd(i, id, 3) * tau + 0.8f * (u - 0.5f)   // slowly turns while it lives
                val driftA = rnd(i, id, 4) * tau
                val len = r * (0.7f + 0.5f * rnd(i, id, 5))
                val amp = r * (0.18f + 0.20f * rnd(i, id, 6))
                val seed = rnd(i, id, 7) * tau

                val hx = c.x + cos(ang0) * r * rad0 + cos(driftA) * r * 0.5f * (u - 0.5f)
                val hy = c.y + sin(ang0) * r * rad0 + sin(driftA) * r * 0.5f * (u - 0.5f)
                val dx = cos(dirA); val dy = sin(dirA)
                val px = -dy; val py = dx

                // four points along the wisp, pushed sideways by a travelling wave -> an S that keeps curling
                fun q(j: Int): Offset {
                    val along = (j - 1.5f) * len / 3f
                    val wob = amp * sin(tau * (u + 0.27f * j) + seed)
                    return Offset(hx + dx * along + px * wob, hy + dy * along + py * wob)
                }
                val q0 = q(0); val q1 = q(1); val q2 = q(2); val q3 = q(3)
                val path = Path().apply {
                    moveTo(q0.x, q0.y)
                    cubicTo(q1.x, q1.y, q2.x, q2.y, q3.x, q3.y)
                }

                val tint = lerp(Nova.Lime, Color.White, (i % 4) / 4f)
                val w = r * (0.10f + 0.05f * rnd(i, id, 8)) * (0.7f + 0.9f * u)   // smoke spreads as it ages

                drawPath(path, tint.copy(alpha = 0.07f * env), style = Stroke(w * 1.6f, cap = StrokeCap.Round))
                drawPath(path, tint.copy(alpha = 0.12f * env), style = Stroke(w, cap = StrokeCap.Round))
                drawPath(path, tint.copy(alpha = 0.16f * env), style = Stroke(w * 0.45f, cap = StrokeCap.Round))
                drawPath(path, tint.copy(alpha = 0.28f * env), style = Stroke(1.dp.toPx(), cap = StrokeCap.Round))
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

/** Repeatable pseudo-random number in 0..1 for a (wisp, life, slot) triple. */
private fun rnd(i: Int, life: Int, slot: Int): Float {
    val v = sin(i * 127.1f + life * 311.7f + slot * 74.7f) * 43758.547f
    return v - floor(v)
}
