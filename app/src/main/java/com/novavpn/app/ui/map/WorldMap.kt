package com.novavpn.app.ui.map

import kotlinx.coroutines.delay
import androidx.compose.runtime.produceState
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.os.SystemClock
import com.novavpn.app.ui.theme.NovaShapes
import com.novavpn.app.ui.theme.NovaType
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.novavpn.app.data.Region
import com.novavpn.app.ui.components.SignalIcon
import com.novavpn.app.ui.components.PILL_OVERLAY_ALPHA
import com.novavpn.app.ui.components.chipSurface
import com.novavpn.app.ui.theme.Nova
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.tan

/** Slightly longer than the panel exit animation (Motion.Medium + its fade delay). */
private const val PANEL_EXIT_MS = 420L

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
    val panX: Float = 0f,
    val panY: Float = 0f,
) {
    val scale = (width / Math.toRadians(SPAN_LON)).toFloat() // px per radian

    /** Longitude currently at the middle of the screen once the user's drag is applied. */
    val viewLon: Double = centerLon - Math.toDegrees((panX / scale).toDouble())

    /** Un-wrapped x, used for the grid so its phase never jumps. */
    fun xRaw(lon: Double): Float = (width / 2f + panX + Math.toRadians(lon - centerLon) * scale).toFloat()

    /** x of the copy of [lon] nearest to the middle of the screen, so markers survive wrapping around the globe. */
    fun x(lon: Double): Float {
        val wrapped = lon - 360.0 * Math.round((lon - viewLon) / 360.0)
        return xRaw(wrapped)
    }

    fun y(lat: Double): Float = (centerY + panY - (merc(lat) - merc(centerLat)) * scale).toFloat()
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
 * Home is now swapped out when Settings/Regions open, so the map is rebuilt every time Home comes back.
 * Parsing world.bin and building the path is expensive, so do it once per process.
 */
private object MapPathsCache {
    @Volatile var cached: MapPaths? = null

    /** Loads off the main thread the first time; every later call returns the cached copy at once. */
    suspend fun load(context: android.content.Context): MapPaths {
        cached?.let { return it }
        val app = context.applicationContext
        return withContext(Dispatchers.Default) { buildPaths(WorldGeo.load(app)) }.also { cached = it }
    }
}

/**
 * Dark map backdrop with country borders, a square grid, a lime marker for the selected region and a
 * floating label pill. The camera glides to the selected region whenever it changes.
 */
@Composable
fun WorldMap(
    selected: Region?,
    others: List<Region>,
    onMarkerClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** True while another screen is on top. The map keeps its position then and glides back when this turns false. */
    overlayOpen: Boolean = false,
) {
    val context = LocalContext.current
    // null only during the very first load; the map shows its background and grid until it arrives.
    val paths by produceState<MapPaths?>(MapPathsCache.cached, context) { value = MapPathsCache.load(context) }

    // Redrawing the whole world every frame is the most expensive thing in the app. If the camera moved
    // while a panel (Regions/Settings) is sliding away, both would fight for frames and the panel
    // animation would stutter. So the camera waits until the panel has left, then glides.
    val closedAt = remember { longArrayOf(0L) }
    LaunchedEffect(overlayOpen) { if (!overlayOpen) closedAt[0] = SystemClock.uptimeMillis() }
    val targetLon = (selected?.lon ?: 10.0).toFloat()
    val targetLat = (selected?.lat ?: 25.0).toFloat()
    var camLon by remember { mutableStateOf(targetLon) }
    var camLat by remember { mutableStateOf(targetLat) }
    LaunchedEffect(targetLon, targetLat, overlayOpen) {
        if (!overlayOpen) {
            val wait = closedAt[0] + PANEL_EXIT_MS - SystemClock.uptimeMillis()
            if (wait > 0) delay(wait)
            camLon = targetLon
            camLat = targetLat
        }
    }
    val cLon by animateFloatAsState(camLon, tween(900, easing = FastOutSlowInEasing), label = "lon")
    val cLat by animateFloatAsState(camLat, tween(900, easing = FastOutSlowInEasing), label = "lat")

    // Read only inside the marker canvas so the pulse doesn't redraw the whole map.
    val pulse = rememberInfiniteTransition(label = "pulse")
    val ring = pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "ring",
    )

    val density = LocalDensity.current

    // User drag offset in px. Double tap, a new selection, or coming back from another screen eases it to zero.
    var panX by remember { mutableStateOf(0f) }
    var panY by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()
    var recenterJob by remember { mutableStateOf<Job?>(null) }
    fun recenter() {
        recenterJob?.cancel()
        recenterJob = scope.launch {
            val fx = panX
            val fy = panY
            if (fx == 0f && fy == 0f) return@launch
            animate(0f, 1f, animationSpec = tween(600, easing = FastOutSlowInEasing)) { t, _ ->
                panX = fx * (1f - t)
                panY = fy * (1f - t)
            }
        }
    }
    LaunchedEffect(selected?.id, overlayOpen) {
        if (!overlayOpen) {
            val wait = closedAt[0] + PANEL_EXIT_MS - SystemClock.uptimeMillis()
            if (wait > 0) delay(wait)
            recenter()
        }
    }

    // Leaving the app: snap back so it reopens on the selected region.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                recenterJob?.cancel()
                panX = 0f
                panY = 0f
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BoxWithConstraints(modifier.fillMaxSize().background(Nova.Ocean)) {
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val proj = Projection(wPx, cLon.toDouble(), cLat.toDouble(), hPx * 0.34f, panX, panY)

        // Map layer (drag to move, double tap to recenter)
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { recenter() })
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { recenterJob?.cancel() },
                        onDrag = { change, drag ->
                            change.consume()
                            val limY = size.height * 0.45f
                            panX += drag.x
                            panY = (panY + drag.y).coerceIn(-limY, limY)
                        },
                    )
                },
        ) {
            drawGrid(proj)
            paths?.let { drawLand(it, proj) }

            others.forEach { r ->
                if (r.id != selected?.id) {
                    drawCircle(Nova.Gray.copy(alpha = 0.7f), 3.dp.toPx(), Offset(proj.x(r.lon), proj.y(r.lat)))
                }
            }

            // Fade into the black controls at the bottom
            drawRect(
                Brush.verticalGradient(
                    0.55f to Color.Transparent,
                    1f to Nova.Black.copy(alpha = 0.85f),
                ),
            )
        }

        if (selected != null) {
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
            drawCircle(Nova.Lime, 4.5.dp.toPx(), m)
        }

        // Floating label pill above the marker
        val gapPx = with(density) { 22.dp.toPx() }
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
                .chipSurface(NovaShapes.Pill, fillAlpha = PILL_OVERLAY_ALPHA)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onMarkerClick,
                )
                .padding(start = 5.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
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
                style = NovaType.Secondary,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        } // selected != null
    }
}

private fun DrawScope.drawGrid(proj: Projection) {
    val step = 30.dp.toPx()
    val ox = proj.xRaw(0.0) % step
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
    // Draw every wrapped copy of the world that is at least partly on screen.
    val shifts = (-2..2).filter { k ->
        proj.viewLon + halfSpan > -180.0 + 360.0 * k && proj.viewLon - halfSpan < 180.0 + 360.0 * k
    }
    val ty = proj.centerY + proj.panY + (merc(proj.centerLat) * s).toFloat() // screen y = ty + (-merc(lat)) * s

    for (k in shifts) {
        val tx = (proj.width / 2f + proj.panX + (k * 2.0 * PI - Math.toRadians(proj.centerLon)) * s).toFloat()
        withTransform({
            translate(left = tx, top = ty)
            scale(scaleX = s, scaleY = s, pivot = Offset.Zero)
        }) {
            drawPath(paths.land, Nova.Land)
            drawPath(
                paths.land,
                Nova.LandBorder,
                style = Stroke(width = 1.dp.toPx() / s, join = StrokeJoin.Round),
            )
            drawPath(paths.water, Nova.Ocean)
        }
    }
}
