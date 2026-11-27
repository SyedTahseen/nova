package com.novavpn.app.ui.map

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novavpn.app.data.Region
import com.novavpn.app.ui.components.SignalIcon
import com.novavpn.app.ui.theme.Nova
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.tan

private const val SPAN_LON = 104.0 // degrees of longitude visible across the screen width

private fun merc(lat: Double): Double {
    val l = lat.coerceIn(-82.0, 82.0)
    return ln(tan(PI / 4 + Math.toRadians(l) / 2))
}

private class Projection(
    val width: Float,
    val centerLon: Double,
    val centerLat: Double,
    val centerY: Float,
) {
    val scale = (width / Math.toRadians(SPAN_LON)).toFloat() // px per radian

    fun x(lon: Double): Float = (width / 2f + Math.toRadians(lon - centerLon) * scale).toFloat()
    fun y(lat: Double): Float = (centerY - (merc(lat) - merc(centerLat)) * scale).toFloat()
}

/** Map geometry pre-built once in "unit space" (x = lon in radians, y = -mercator), so each frame is just a transform. */
private class MapPaths(val land: Path, val water: Path)

private fun buildPaths(data: WorldGeo.MapData): MapPaths {
    fun build(rings: List<DoubleArray>): Path {
        val path = Path().apply { fillType = PathFillType.NonZero }
        for (ring in rings) {
            var i = 0
            while (i < ring.size) {
                val x = Math.toRadians(ring[i]).toFloat()
                val y = (-merc(ring[i + 1])).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                i += 2
            }
            path.close()
        }
        return path
    }
    return MapPaths(build(data.land), build(data.water))
}

/**
 * Dark map backdrop with country borders, a square grid, a lime marker for the selected region and a
 * floating label pill. The camera glides to the selected region whenever it changes.
 */
@Composable
fun WorldMap(
    selected: Region,
    others: List<Region>,
    onMarkerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val paths = remember { buildPaths(WorldGeo.load(context)) }

    val cLon by animateFloatAsState(selected.lon.toFloat(), tween(900, easing = FastOutSlowInEasing), label = "lon")
    val cLat by animateFloatAsState(selected.lat.toFloat(), tween(900, easing = FastOutSlowInEasing), label = "lat")

    // Read only inside the marker canvas so the pulse doesn't redraw the whole map.
    val pulse = rememberInfiniteTransition(label = "pulse")
    val ring = pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "ring",
    )

    val density = LocalDensity.current

    BoxWithConstraints(modifier.fillMaxSize().background(Nova.Ocean)) {
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val proj = Projection(wPx, cLon.toDouble(), cLat.toDouble(), hPx * 0.40f)

        // Map layer
        Canvas(Modifier.fillMaxSize()) {
            drawGrid(proj)
            drawLand(paths, proj)

            others.forEach { r ->
                if (r.id != selected.id) {
                    drawCircle(Nova.Gray.copy(alpha = 0.7f), 3.dp.toPx(), Offset(proj.x(r.lon), proj.y(r.lat)))
                }
            }

            // Fade into the black controls at the bottom
            drawRect(
                Brush.verticalGradient(
                    0.65f to Color.Transparent,
                    1f to Nova.Black.copy(alpha = 0.55f),
                ),
            )
        }

        // Marker layer (animates every frame)
        Canvas(Modifier.fillMaxSize()) {
            val m = Offset(proj.x(selected.lon), proj.y(selected.lat))
            val t = ring.value
            drawCircle(
                Nova.Lime.copy(alpha = (1f - t) * 0.5f),
                radius = 6.dp.toPx() + t * 22.dp.toPx(),
                center = m,
                style = Stroke(1.5.dp.toPx()),
            )
            drawCircle(Nova.Lime.copy(alpha = 0.25f), 9.dp.toPx(), m)
            drawCircle(Nova.Lime, 5.dp.toPx(), m)
        }

        // Floating label pill above the marker
        val gapPx = with(density) { 17.dp.toPx() }
        val mx = proj.x(selected.lon)
        val my = proj.y(selected.lat)
        Row(
            Modifier
                .layout { measurable, constraints ->
                    val p = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        p.place((mx - p.width / 2f).roundToInt(), (my - p.height - gapPx).roundToInt())
                    }
                }
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xE61F1F22))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onMarkerClick,
                )
                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(Nova.Lime),
                contentAlignment = Alignment.Center,
            ) {
                SignalIcon(Nova.Black, Modifier.size(16.dp))
            }
            Text(
                selected.label,
                color = Nova.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

private fun DrawScope.drawGrid(proj: Projection) {
    val step = 30.dp.toPx()
    val ox = proj.x(0.0) % step
    val oy = proj.y(0.0) % step
    var x = ox
    while (x < size.width) {
        drawLine(Nova.Grid, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
        x += step
    }
    var y = oy
    while (y < size.height) {
        drawLine(Nova.Grid, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        y += step
    }
}

private fun DrawScope.drawLand(paths: MapPaths, proj: Projection) {
    val s = proj.scale
    val halfSpan = SPAN_LON / 2
    // Also draw a wrapped copy when the view crosses the antimeridian.
    val shifts = buildList {
        add(0)
        if (proj.centerLon - halfSpan < -180) add(-1)
        if (proj.centerLon + halfSpan > 180) add(1)
    }
    val ty = proj.centerY + (merc(proj.centerLat) * s).toFloat() // screen y = ty + (-merc(lat)) * s

    for (k in shifts) {
        val tx = (proj.width / 2f + (k * 2.0 * PI - Math.toRadians(proj.centerLon)) * s).toFloat()
        withTransform({
            translate(left = tx, top = ty)
            scale(scaleX = s, scaleY = s, pivot = Offset.Zero)
        }) {
            drawPath(paths.land, Nova.Land)
            drawPath(
                paths.land,
                Nova.LandBorder,
                style = Stroke(width = 0.8.dp.toPx() / s, join = StrokeJoin.Round),
            )
            drawPath(paths.water, Nova.Ocean)
        }
    }
}
