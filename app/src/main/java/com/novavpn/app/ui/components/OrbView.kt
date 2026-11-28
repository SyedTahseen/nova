package com.novavpn.app.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
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
 * The glowing lime sphere with smoke inside.
 * Android 13+ draws real smoke with a noise shader (domain-warped fractal noise); older versions fall back to
 * [CanvasOrb], which fakes it with drifting translucent wisps. [speed] > 1 makes the smoke move faster.
 */
@Composable
fun OrbView(modifier: Modifier = Modifier, speed: Float = 1f) {
    if (Build.VERSION.SDK_INT >= 33) SmokeOrb(modifier, speed) else CanvasOrb(modifier, speed)
}

private fun DrawScope.drawOuterGlow(c: Offset, r: Float) {
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
}

@RequiresApi(33)
@Composable
private fun SmokeOrb(modifier: Modifier, speed: Float) {
    val shader = remember { RuntimeShader(SMOKE_AGSL) }
    val brush = remember(shader) { ShaderBrush(shader) }
    var time by remember { mutableFloatStateOf(0f) }
    val currentSpeed by rememberUpdatedState(speed)
    LaunchedEffect(Unit) {
        // Accumulate time (instead of looping a value) so a change of speed never makes the smoke jump.
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) time += (now - last) / 1_000_000_000f * currentSpeed
                last = now
            }
        }
    }
    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f * 0.76f
        drawOuterGlow(c, r)
        shader.setFloatUniform("center", c.x, c.y)
        shader.setFloatUniform("radius", r)
        shader.setFloatUniform("time", time)
        drawCircle(brush, r, c)
    }
}

/** Smoke: fractal noise warped by more noise, so it folds and curls and keeps changing shape. */
private const val SMOKE_AGSL = """
uniform float2 center;
uniform float radius;
uniform float time;

float hash(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + float2(1.0, 0.0));
    float c = hash(i + float2(0.0, 1.0));
    float d = hash(i + float2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(float2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p = p * 2.03 + float2(11.7, 7.3);
        a *= 0.5;
    }
    return v;
}

half4 main(float2 fragCoord) {
    float2 uv = (fragCoord - center) / radius;
    float d = length(uv);
    float aa = 1.5 / radius;
    float mask = 1.0 - smoothstep(1.0 - aa, 1.0, d);
    if (mask <= 0.0) return half4(0.0);

    float t = time * 0.05;
    float2 p = uv * 1.5;

    // warp the coordinates twice: this is what makes it flow like smoke instead of blobs
    float2 q = float2(fbm(p + float2(0.0, t)), fbm(p + float2(5.2, 1.3) - t));
    float2 w = float2(fbm(p + 2.5 * q + float2(1.7, 9.2) + 0.7 * t),
                      fbm(p + 2.5 * q + float2(8.3, 2.8) - 0.5 * t));
    float f = fbm(p + 2.8 * w);

    float dens = smoothstep(0.38, 0.85, f);
    float ridge = 1.0 - abs(2.0 * fbm(p * 1.3 + 3.0 * w) - 1.0);   // thin streaks inside the smoke
    dens *= 0.55 + 0.45 * ridge;

    // empty middle, smoke gathers toward the edge
    dens *= smoothstep(0.18, 0.70, d);

    float3 deep = float3(0.15, 0.18, 0.0);
    float3 mid = float3(0.43, 0.52, 0.0);
    float3 bright = float3(0.82, 0.93, 0.0);
    float3 col = mix(deep, mid, dens * 0.85);
    col = mix(col, bright, pow(dens, 2.2) * 0.6);

    // soft edge lift (kept small: less inner glow)
    col = mix(col, float3(0.78, 0.92, 0.1), smoothstep(0.80, 1.0, d) * 0.25);

    return half4(float4(col * mask, mask));
}
"""

/** Fallback for Android 12 and older: translucent wisps drawn with Canvas. */
@Composable
private fun CanvasOrb(modifier: Modifier, speed: Float) {
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

        drawOuterGlow(c, r)

        // Body
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
                    0f to Color.White.copy(alpha = 0.10f),
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
