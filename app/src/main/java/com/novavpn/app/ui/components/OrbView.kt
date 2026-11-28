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
import kotlin.math.cos
import kotlin.math.sin

/**
 * The glowing lime plasma sphere: dense dark smoky swirls flowing through intense yellow,
 * with a hot fresnel rim.
 *
 * API 33+  : a real-time AGSL fragment shader (domain-warped fractal noise, i.e. the standard
 *            "flowing smoke / plasma" technique) wrapped onto a sphere.
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

private const val ORB_AGSL = """
uniform float2 uCenter;
uniform float uRadius;
uniform float uTime;

float hash(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
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

half4 main(float2 fc) {
    float2 p = (fc - uCenter) / uRadius;
    float d = length(p);
    float aa = clamp((1.0 - d) * uRadius / 1.5, 0.0, 1.0);
    if (aa <= 0.0) { return half4(0.0); }

    // Wrap the flow onto a sphere: coordinates stretch towards the limb.
    float z = sqrt(max(1.0 - d * d, 0.0));
    float2 q = p / (0.55 + 0.45 * z);

    // Slow overall rotation of the whole body
    float ang = uTime * 0.06;
    float ca = cos(ang);
    float sa = sin(ang);
    q = float2(ca * q.x - sa * q.y, sa * q.x + ca * q.y);

    float t = uTime * 0.14;

    // Domain warping (warp the noise with noise, twice) = swirling smoke
    float2 s = q * 1.5;
    float2 w = float2(fbm(s + float2(0.0, t)),
                      fbm(s + float2(5.2, 1.3 - t)));
    float2 w2 = float2(fbm(s + 3.2 * w + float2(1.7, 9.2) + t * 0.7),
                       fbm(s + 3.2 * w + float2(8.3, 2.8) - t * 0.7));
    float f = fbm(s + 3.6 * w2);

    float3 dark   = float3(0.10, 0.13, 0.00);
    float3 yellow = float3(0.90, 1.00, 0.16);
    float3 hot    = float3(0.97, 1.00, 0.58);

    // Dense dark lanes vs intense yellow
    float dens = smoothstep(0.30, 0.72, f);
    float3 col = mix(dark, yellow, dens);

    // Soft bright smoke ribbons following the flow (ridged noise)
    float rid = 1.0 - abs(2.0 * fbm(s * 1.6 + 3.0 * w2 + float2(t, -t)) - 1.0);
    rid = pow(rid, 3.0);
    col += hot * rid * (0.15 + 0.55 * dens);

    // Keep the middle calmer/darker so the status text stays readable
    col = mix(col * 0.55, col, smoothstep(0.05, 0.95, d));

    // Hot fresnel rim
    float rim = pow(1.0 - z, 2.4);
    col = mix(col, hot, clamp(rim * 1.15, 0.0, 1.0));
    col += yellow * rim * 0.45;

    col = clamp(col, 0.0, 1.0);
    return half4(half3(col * aa), half(aa));
}
"""

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
    shader.setFloatUniform("uCenter", c.x, c.y)
    shader.setFloatUniform("uRadius", r)
    shader.setFloatUniform("uTime", t)
    drawCircle(brush = brush, radius = r + 2f, center = c)
}

// ---------------------------------------------------------------------------------------------
// API 26-32 : Canvas fallback (soft smoke blobs, no thin strokes)
// ---------------------------------------------------------------------------------------------

private fun DrawScope.drawFallbackOrb(c: Offset, r: Float, t: Float) {
    val clip = Path().apply { addOval(Rect(c, r)) }
    clipPath(clip) {
        // Bright yellow body
        drawCircle(
            brush = Brush.radialGradient(
                0f to Nova.OrbMid,
                0.7f to Nova.OrbBright,
                1f to Nova.OrbGlow,
                center = c,
                radius = r,
            ),
            radius = r,
            center = c,
        )

        // Big stretched blobs orbiting tangentially = swirling smoke
        val count = 16
        for (i in 0 until count) {
            val dir = if (i % 2 == 0) 1f else -1f
            val fr = (i * 0.618f) % 1f
            val orbit = r * (0.12f + 0.55f * fr)
            val a = t * (0.14f + 0.05f * (i % 4)) * dir + i * 2.4f
            val bc = Offset(c.x + cos(a) * orbit, c.y + sin(a) * orbit)
            val br = r * (0.38f + 0.28f * ((i * 0.37f) % 1f))
            val rot = a * (180f / PI.toFloat()) + 90f
            val color = if (i % 3 == 0) Nova.Lime.copy(alpha = 0.40f) else Nova.OrbDeep.copy(alpha = 0.60f)
            rotate(rot, pivot = bc) {
                scale(2.0f, 0.62f, pivot = bc) {
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

        // Calm dark core (text readability) fading into a hot rim
        drawCircle(
            brush = Brush.radialGradient(
                0.00f to Nova.OrbDeep.copy(alpha = 0.45f),
                0.45f to Color.Transparent,
                0.78f to Color.Transparent,
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
