package com.novavpn.app.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import com.novavpn.app.ui.theme.Nova
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.math.cos
import kotlin.math.sin

/**
 * The glowing lime plasma sphere: dense dark smoky swirls flowing through intense yellow,
 * with a hot fresnel rim.
 *
 * API 33+  : a real-time AGSL fragment shader: warped-noise smoke plus ~40 soft, crossing
 *            strands (arcs of circles wrapped on the sphere) with a dark calm centre.
 * API 26-32: Canvas fallback built from large soft, stretched, orbiting gradient blobs
 *            (soft smoke instead of thin lines).
 *
 * [speed] > 1 makes the smoke flow faster (used while connecting).
 */
@Composable
fun OrbView(modifier: Modifier = Modifier, speed: Float = 1f) {
    // Time is accumulated (not looped) so changing [speed] never makes the flow jump.
    val smoothSpeed by animateFloatAsState(speed, tween(900), label = "orbSpeed")
    val speedState = rememberUpdatedState(smoothSpeed)
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) time.floatValue += (now - last) / 1_000_000_000f * speedState.value
                last = now
            }
        }
    }

    val shader: Any? = remember {
        if (Build.VERSION.SDK_INT >= 33) createOrbShader() else null
    }
    val brush: Any? = remember(shader) {
        if (Build.VERSION.SDK_INT >= 33 && shader != null) ShaderBrush(shader as RuntimeShader) else null
    }

    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f * 0.76f
        val t = time.floatValue

        // Outer halo, hugging the rim
        val haloR = r * 1.3f
        drawCircle(
            brush = Brush.radialGradient(
                0.00f to Nova.Lime.copy(alpha = 0.00f),
                0.62f to Nova.Lime.copy(alpha = 0.22f),
                0.77f to Nova.Lime.copy(alpha = 0.50f),
                0.86f to Nova.Lime.copy(alpha = 0.16f),
                1.00f to Color.Transparent,
                center = c,
                radius = haloR,
            ),
            radius = haloR,
            center = c,
        )

        if (Build.VERSION.SDK_INT >= 33 && shader != null && brush != null) {
            drawShaderOrb(shader as RuntimeShader, brush as ShaderBrush, c, r, t)
        } else {
            drawFallbackOrb(c, r, t)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// API 33+ : AGSL shader
// ---------------------------------------------------------------------------------------------

private const val RING_COUNT = 40

private const val ORB_AGSL = """
uniform float2 uCenter;
uniform float uRadius;
uniform float uTime;
uniform float4 uRingA[40];   // xyz = ring normal, w = height (small-circle offset)
uniform float4 uRingB[40];   // xyz = in-plane axis, w = phase
uniform float4 uRingC[40];   // x = width, y = amplitude, z = frequency

float hash(float2 p) {
    float3 p3 = fract(float3(p.x, p.y, p.x) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float vnoise(float2 p) {
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
        v += a * vnoise(p);
        p = float2(0.8 * p.x - 0.6 * p.y, 0.6 * p.x + 0.8 * p.y) * 2.0 + float2(7.1, 3.3);
        a *= 0.5;
    }
    return v / 0.9375;
}

float2 rot(float2 v, float a) {
    float c = cos(a);
    float s = sin(a);
    return float2(c * v.x - s * v.y, s * v.x + c * v.y);
}

half4 main(float2 fc) {
    float2 p = (fc - uCenter) / uRadius;
    float d = length(p);
    float aa = clamp((1.0 - d) * uRadius / 1.5, 0.0, 1.0);
    if (aa <= 0.0) { return half4(0.0); }

    float z = sqrt(max(1.0 - d * d, 0.0));
    float t = uTime * 0.10;

    // ---- Smoke: dense dark swirls (domain-warped noise) ----
    float2 q = rot(p / (0.55 + 0.45 * z), uTime * 0.03);
    float2 s = q * 1.4;
    float2 w = float2(fbm(s + float2(0.0, t)), fbm(s + float2(5.2, 1.3 - t)));
    float f = fbm(s + 2.5 * w + float2(0.0, t * 0.5));
    float smoke = smoothstep(0.32, 0.68, f);

    // ---- Strands: soft arcs of circles wrapped on the sphere, crossing each other ----
    float wx = fbm(p * 1.8 + float2(t * 0.3, 0.0)) - 0.5;
    float wy = fbm(p * 1.8 + float2(9.1, t * 0.3)) - 0.5;
    float3 P = float3(p.x + 0.12 * wx, p.y + 0.12 * wy, z + 0.06 * (wx - wy));

    float line = 0.0;
    float glow = 0.0;
    for (int i = 0; i < 40; i++) {
        float4 A = uRingA[i];
        float dist = abs(dot(P, A.xyz) - A.w);
        float wd = uRingC[i].x;
        if (dist < wd * 4.0) {
            float4 B = uRingB[i];
            float u = dot(P, B.xyz);
            float m = smoothstep(0.5, 0.95, 0.5 + 0.5 * sin(u * uRingC[i].z + B.w));
            float amp = uRingC[i].y * m;
            float k = dist / wd;
            line += exp(-k * k) * amp;
            glow += exp(-k * k / 9.0) * amp;
        }
    }

    float3 dark   = float3(0.05, 0.065, 0.0);
    float3 olive  = float3(0.50, 0.60, 0.02);
    float3 yellow = float3(0.90, 1.00, 0.16);
    float3 hot    = float3(0.97, 1.00, 0.62);

    float edge = smoothstep(0.2, 1.0, d);
    float centre = 0.35 + 0.65 * smoothstep(0.05, 0.8, d);   // quieter middle for the status text

    float3 col = mix(dark, olive, pow(edge, 1.3));
    col *= 0.25 + 0.75 * smoke;
    col += (yellow * 0.5 - col) * (pow(edge, 3.5) * (0.4 + 0.6 * smoke));

    float3 strand = float3(0.80, 1.00, 0.15);
    col += strand * min(line, 1.0) * 0.6 * centre * (0.8 + 0.5 * smoke);
    col += strand * glow * 0.10 * centre;

    float rim = pow(1.0 - z, 3.0);
    col = mix(col, hot, clamp(rim * 0.9, 0.0, 1.0));
    col += yellow * rim * 0.4;

    col = clamp(col, 0.0, 1.0);
    return half4(half3(col * aa), half(aa));
}
"""

/** Static random parameters for each strand (a ring on the sphere). Built once. */
private class Ring(
    val th: Float, val ph: Float, val sp: Float, val h: Float,
    val wd: Float, val amp: Float, val fr: Float, val pc: Float, val ps: Float,
)

private val rings: List<Ring> = Random(7).let { rnd ->
    List(RING_COUNT) { i ->
        Ring(
            th = acos(2f * rnd.nextFloat() - 1f),
            ph = rnd.nextFloat() * 6.2832f,
            sp = (0.04f + 0.07f * rnd.nextFloat()) * (if (i % 2 == 0) 1f else -1f),
            h = (rnd.nextFloat() - 0.5f) * 1.1f,
            wd = 0.016f + 0.022f * rnd.nextFloat(),
            amp = 0.4f + 0.6f * rnd.nextFloat(),
            fr = 2.5f + 2.5f * rnd.nextFloat(),
            pc = rnd.nextFloat() * 6.28f,
            ps = 0.3f + 0.4f * rnd.nextFloat(),
        )
    }
}

private val ringA = FloatArray(RING_COUNT * 4)
private val ringB = FloatArray(RING_COUNT * 4)
private val ringC = FloatArray(RING_COUNT * 4)

/** Recomputes the strands' orientation for time [t] (cheap: 40 rings, once per frame). */
private fun updateRings(t: Float) {
    for (i in 0 until RING_COUNT) {
        val r = rings[i]
        val th = r.th + 0.35f * sin(t * 0.17f + i * 1.7f)
        val ph = r.ph + t * r.sp
        val nx = sin(th) * cos(ph)
        val ny = sin(th) * sin(ph)
        val nz = cos(th)
        // e1 = normalize(n x axis), e2 = n x e1
        val axX = if (abs(nz) < 0.9f) 0f else 1f
        val axZ = if (abs(nz) < 0.9f) 1f else 0f
        var e1x = ny * axZ
        var e1y = nz * axX - nx * axZ
        var e1z = -ny * axX
        val l = sqrt(e1x * e1x + e1y * e1y + e1z * e1z).coerceAtLeast(1e-4f)
        e1x /= l; e1y /= l; e1z /= l
        val e2x = ny * e1z - nz * e1y
        val e2y = nz * e1x - nx * e1z
        val e2z = nx * e1y - ny * e1x

        ringA[i * 4] = nx; ringA[i * 4 + 1] = ny; ringA[i * 4 + 2] = nz; ringA[i * 4 + 3] = r.h
        ringB[i * 4] = e2x; ringB[i * 4 + 1] = e2y; ringB[i * 4 + 2] = e2z
        ringB[i * 4 + 3] = r.pc + t * r.ps
        ringC[i * 4] = r.wd; ringC[i * 4 + 1] = r.amp; ringC[i * 4 + 2] = r.fr
    }
}

@RequiresApi(33)
private fun createOrbShader(): Any = RuntimeShader(ORB_AGSL)

@RequiresApi(33)
private fun DrawScope.drawShaderOrb(
    shader: RuntimeShader,
    brush: ShaderBrush,
    c: Offset,
    r: Float,
    t: Float,
) {
    updateRings(t)
    shader.setFloatUniform("uCenter", c.x, c.y)
    shader.setFloatUniform("uRadius", r)
    shader.setFloatUniform("uTime", t)
    shader.setFloatUniform("uRingA", ringA)
    shader.setFloatUniform("uRingB", ringB)
    shader.setFloatUniform("uRingC", ringC)
    drawCircle(brush = brush, radius = r + 2f, center = c)
}

// ---------------------------------------------------------------------------------------------
// API 26-32 : Canvas fallback (soft smoke blobs, no thin strokes)
// ---------------------------------------------------------------------------------------------

private fun DrawScope.drawFallbackOrb(c: Offset, r: Float, t: Float) {
    val clip = Path().apply { addOval(Rect(c, r)) }
    clipPath(clip) {
        // Dark body with an empty centre, warming to yellow at the edge
        drawCircle(
            brush = Brush.radialGradient(
                0.00f to Nova.OrbDeep,
                0.60f to Nova.OrbDeep,
                0.85f to Nova.OrbMid,
                1.00f to Nova.OrbBright,
                center = c,
                radius = r,
            ),
            radius = r,
            center = c,
        )

        // Thin stretched glows sweeping tangentially around the outer ring = flowing strands
        val count = 28
        for (i in 0 until count) {
            val dir = if (i % 3 == 0) -1f else 1f
            val fr = (i * 0.618f) % 1f
            val orbit = r * (0.50f + 0.42f * fr)
            val a = t * (0.10f + 0.04f * (i % 4)) * dir + i * 2.4f
            val bc = Offset(c.x + cos(a) * orbit, c.y + sin(a) * orbit)
            val br = r * (0.22f + 0.16f * ((i * 0.37f) % 1f))
            val rot = a * (180f / PI.toFloat()) + 90f
            val color = Nova.Lime.copy(alpha = 0.16f + 0.22f * ((i * 0.29f) % 1f))
            rotate(rot, pivot = bc) {
                scale(0.28f, 2.6f, pivot = bc) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            0f to color,
                            1f to Color.Transparent,
                            center = bc,
                            radius = br,
                        ),
                        radius = br,
                        center = bc,
                    )
                }
            }
        }

        // Hot rim
        drawCircle(
            brush = Brush.radialGradient(
                0.00f to Color.Transparent,
                0.80f to Color.Transparent,
                0.93f to Nova.Lime.copy(alpha = 0.55f),
                1.00f to Nova.OrbGlow,
                center = c,
                radius = r,
            ),
            radius = r,
            center = c,
        )
    }
}
