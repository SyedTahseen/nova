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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import com.novavpn.app.ui.theme.Nova
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * The glowing lime sphere with soft smoke inside. Drawn with plain Canvas (no shaders), so it is light and works
 * on every API level. [speed] > 1 makes the smoke move faster (used while connecting).
 *
 * The smoke is a handful of streams. Each stream keeps releasing soft puffs from one spot low in the sphere;
 * every puff rises, sways, spreads out and fades away, like smoke from a cigarette.
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
        val tau = 2f * PI.toFloat()

        // Outer glow
        drawCircle(
            brush = Brush.radialGradient(
                0f to Nova.Lime.copy(alpha = 0.34f),
                0.55f to Nova.Lime.copy(alpha = 0.11f),
                1f to Color.Transparent,
                center = c,
                radius = r * 1.5f,
            ),
            radius = r * 1.5f,
            center = c,
        )

        // Body (dark in the middle, only a mild lift at the edge)
        drawCircle(
            brush = Brush.radialGradient(
                0f to Nova.OrbDeep,
                0.58f to Nova.OrbMid,
                0.88f to lerp(Nova.OrbMid, Nova.OrbBright, 0.55f),
                1f to lerp(Nova.OrbBright, Nova.OrbGlow, 0.25f),
                center = c,
                radius = r,
            ),
            radius = r,
            center = c,
        )

        clipPath(Path().apply { addOval(Rect(c, r)) }) {
            val streams = 6
            val puffs = 8
            for (i in 0 until streams) {
                val loops = 2 + i % 2                                   // whole number -> seamless loop
                // each stream has one source, low in the sphere and spread left to right
                val sx = c.x + r * (-0.65f + 1.3f * hash(i, 0, 1))
                val sy = c.y + r * (0.15f + 0.65f * hash(i, 0, 2))
                val sway = r * (0.16f + 0.14f * hash(i, 0, 3))          // how far it wanders sideways
                val phase = hash(i, 0, 4) * tau
                val lean = (hash(i, 0, 5) - 0.5f) * 0.5f * r            // slight drift to one side

                for (k in 0 until puffs) {
                    val x = loops * t / tau + i * 0.37f + k.toFloat() / puffs
                    val a = x - floor(x)                                 // age of this puff, 0..1
                    val env = sin(PI.toFloat() * a).let { it * it }      // fade in, fade out
                    if (env < 0.01f) continue

                    val wobble = sin(tau * 1.3f * a + phase + k * 0.9f) * sway * (0.25f + a)
                    val px = sx + wobble + lean * a
                    val py = sy - r * 1.05f * a                          // rises
                    val pr = r * (0.07f + 0.17f * a)                     // spreads as it ages
                    val tint = lerp(Nova.Lime, Color.White, 0.30f + 0.2f * hash(i, k, 6))

                    drawCircle(
                        brush = Brush.radialGradient(
                            0f to tint.copy(alpha = 0.16f * env),
                            1f to Color.Transparent,
                            center = Offset(px, py),
                            radius = pr,
                        ),
                        radius = pr,
                        center = Offset(px, py),
                    )
                }
            }

            // gentle fade so the very middle stays dark behind the status text
            drawCircle(
                brush = Brush.radialGradient(
                    0f to Nova.OrbDeep.copy(alpha = 0.85f),
                    0.5f to Nova.OrbDeep.copy(alpha = 0.85f),
                    1f to Color.Transparent,
                    center = c,
                    radius = r * 0.45f,
                ),
                radius = r * 0.45f,
                center = c,
            )
        }
    }
}

/** Repeatable pseudo-random number in 0..1 for an (a, b, slot) triple. */
private fun hash(a: Int, b: Int, slot: Int): Float {
    val v = sin(a * 127.1f + b * 311.7f + slot * 74.7f) * 43758.547f
    return v - floor(v)
}
