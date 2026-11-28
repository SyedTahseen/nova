package com.novavpn.app.ui.screens

import com.novavpn.app.ui.theme.NovaShapes
import com.novavpn.app.ui.theme.NovaType
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.mutableStateOf
import com.novavpn.app.ui.Motion
import com.novavpn.app.ui.Press
import com.novavpn.app.ui.pressable
import com.novavpn.app.ui.statusTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novavpn.app.ui.components.ChevronIcon
import com.novavpn.app.ui.components.OrbView
import com.novavpn.app.ui.components.SlideDirection
import com.novavpn.app.ui.components.SlideToggle
import com.novavpn.app.ui.theme.Nova
import com.novavpn.app.vpn.Speed
import com.novavpn.app.vpn.VpnState
import com.novavpn.app.vpn.VpnStatus
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun ConnectedScreen(
    vpn: VpnState,
    speed: Speed,
    onOpenRegions: () -> Unit,
    onDisconnect: () -> Unit,
) {
    var elapsedSec by remember { mutableLongStateOf(0L) }
    LaunchedEffect(vpn.status, vpn.connectedAtMs) {
        if (vpn.status == VpnStatus.Connected) {
            while (true) {
                elapsedSec = ((System.currentTimeMillis() - vpn.connectedAtMs) / 1000).coerceAtLeast(0)
                delay(500)
            }
        } else if (vpn.status == VpnStatus.Connecting) {
            elapsedSec = 0
        }
    }

    val statusText = when (vpn.status) {
        VpnStatus.Connected -> "Connected"
        VpnStatus.Connecting -> "Connecting…"
        VpnStatus.Disconnecting -> "Disconnecting…"
        VpnStatus.Disconnected -> "Disconnected"
    }
    val live = vpn.status == VpnStatus.Connected

    Box(
        Modifier
            .fillMaxSize()
            .background(Nova.Black)
            .background(
                Brush.radialGradient(
                    0f to Nova.GlowTop.copy(alpha = 0.75f),
                    0.55f to Nova.GlowMid.copy(alpha = 0.35f),
                    1f to Color.Transparent,
                    center = Offset(540f, 330f),
                    radius = 1100f,
                ),
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Orb + status + timer. Takes all spare height; the orb's bottom edge rests on the location card.
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val rimDiameter = minOf(maxWidth * 0.75f, maxHeight - 8.dp)
                val orbSize = rimDiameter / 0.76f          // OrbView draws its rim at 76% of its size
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = orbSize * 0.12f)       // rim bottom = box bottom
                        .requiredSize(orbSize),
                    contentAlignment = Alignment.Center,
                ) {
                    OrbView(Modifier.fillMaxSize(), speed = if (live) 1f else 3f)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val statusColor by animateColorAsState(
                            if (live) Nova.Lime else Nova.White.copy(alpha = 0.85f),
                            tween(Motion.Long, easing = Motion.Standard), label = "statusColor",
                        )
                        AnimatedContent(
                            targetState = statusText,
                            transitionSpec = { statusTransition() },
                            label = "statusText",
                        ) { text ->
                            Text(text, color = statusColor, style = NovaType.Status)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            formatElapsed(elapsedSec),
                            color = Nova.White,
                            style = NovaType.Timer,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // Location card – same shape, padding and button as a server row (RegionRow): 74dp tall
            val cardShape = NovaShapes.Pill
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressable(cardShape, Press.Card, onClick = onOpenRegions)
                    .background(Nova.Card)
                    .border(1.dp, Nova.CardBorder, cardShape)
                    .padding(start = 26.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    vpn.regionLabel ?: "",
                    color = Nova.White,
                    style = NovaType.Body,
                    modifier = Modifier.weight(1f),
                )
                val btnShape = NovaShapes.Control
                Box(
                    Modifier
                        .size(54.dp)
                        .clip(btnShape)
                        .background(Nova.CardRaised)
                        .border(1.dp, Nova.CardBorder, btnShape),
                    contentAlignment = Alignment.Center,
                ) {
                    ChevronIcon(Nova.White, Modifier.size(22.dp))
                }
            }

            var lastWarning by remember { mutableStateOf(vpn.warning) }
            if (vpn.warning != null) lastWarning = vpn.warning
            AnimatedVisibility(
                visible = vpn.warning != null,
                enter = fadeIn(tween(Motion.Short, easing = Motion.Enter)) + expandVertically(tween(Motion.Medium, easing = Motion.Standard)),
                exit = fadeOut(tween(Motion.Fast)) + shrinkVertically(tween(Motion.Short, easing = Motion.Standard)),
            ) {
                Text(
                    lastWarning.orEmpty(),
                    color = Nova.Orange,
                    style = NovaType.Caption,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 6.dp, end = 6.dp),
                )
            }

            Spacer(Modifier.height(4.dp))

            // Speed cards
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SpeedCard("Download", speed.downMbps, DOWN_REF, Modifier.weight(1f))
                SpeedCard("Upload", speed.upMbps, UP_REF, Modifier.weight(1f))
            }

            Spacer(Modifier.height(18.dp))

            SlideToggle(
                label = "Disconnect",
                labelColor = Nova.Red,
                direction = SlideDirection.Left,
                onComplete = onDisconnect,
                enabled = vpn.status == VpnStatus.Connected || vpn.status == VpnStatus.Connecting,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Speed (Mb/s) that fills a bar when nothing faster has been seen recently; the scale grows if traffic exceeds it. */
private const val DOWN_REF = 15f
private const val UP_REF = 8f

private const val BAR_COUNT = 5
private const val BAR_MIN_FILL = 0.35f   // an idle bar still shows a short rounded stub, like the design

@Composable
private fun SpeedCard(title: String, mbps: Float, refMbps: Float, modifier: Modifier) {
    val shape = NovaShapes.Card
    Column(
        modifier
            .clip(shape)
            .background(Nova.Card.copy(alpha = 0.92f))
            .border(1.dp, Nova.CardBorder, shape)
            .padding(start = 12.dp, end = 12.dp, top = 11.dp, bottom = 16.dp),
    ) {
        Text(title, color = Nova.TextMuted, style = NovaType.CardTitle, modifier = Modifier.padding(start = 2.dp))
        Spacer(Modifier.height(29.dp))

        SpeedBars(rememberSpeedHistory(mbps), refMbps)

        Spacer(Modifier.height(26.dp))
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(start = 2.dp)) {
            Text(
                formatSpeed(mbps),
                color = Nova.White,
                style = NovaType.Display,
            )
            Text(
                " mb/s",
                color = Nova.TextMuted,
                style = NovaType.CardTitle,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

/** The last [BAR_COUNT] one-second samples of this card's own speed, oldest on the left, newest on the right. */
@Composable
private fun rememberSpeedHistory(mbps: Float): List<Float> {
    val latest by rememberUpdatedState(mbps)
    val history = remember { mutableStateListOf<Float>().apply { repeat(BAR_COUNT) { add(0f) } } }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            history.removeAt(0)
            history.add(latest)
        }
    }
    return history
}

/**
 * Five columns. Each column is ONE meter: a hatched track (the empty part) with a rounded bar rising from the
 * bottom (the filled part). The faster the sample, the taller the bar and the less hatch stays visible.
 * The newest sample (right-most column) is lit.
 */
@Composable
private fun SpeedBars(samples: List<Float>, refMbps: Float) {
    val scale = maxOf(refMbps, samples.maxOrNull() ?: 0f)
    Row(
        Modifier.fillMaxWidth().height(58.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (i in 0 until BAR_COUNT) {
            val lit = i == BAR_COUNT - 1
            val target = BAR_MIN_FILL + (1f - BAR_MIN_FILL) * (samples.getOrElse(i) { 0f } / scale).coerceIn(0f, 1f)
            val fill by animateFloatAsState(target, tween(Motion.Medium, easing = Motion.Standard), label = "barFill$i")
            Box(Modifier.weight(1f).fillMaxSize()) {
                // hatched track over the whole column; the bar covers its lower part
                Canvas(Modifier.fillMaxSize()) {
                    val r = 9.dp.toPx()
                    val clip = Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                0f, 0f, size.width, size.height,
                                androidx.compose.ui.geometry.CornerRadius(r, r),
                            ),
                        )
                    }
                    clipPath(clip) {
                        var x = -size.height
                        while (x < size.width) {
                            drawLine(
                                Nova.BarTrack,
                                Offset(x, size.height),
                                Offset(x + size.height, 0f),
                                strokeWidth = 1.2.dp.toPx(),
                            )
                            x += 5.dp.toPx()
                        }
                    }
                }
                // filled part
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(fill)
                        .then(
                            if (lit) Modifier.shadow(
                                14.dp, NovaShapes.Small,
                                ambientColor = Nova.Lime, spotColor = Nova.Lime,
                            ) else Modifier,
                        )
                        .clip(NovaShapes.Small)
                        .background(if (lit) Nova.Lime else Nova.BarOff),
                )
            }
        }
    }
}

private fun formatSpeed(v: Float): String =
    if (v < 10f) String.format(Locale.US, "%.1f", v) else v.toInt().toString()

private fun formatElapsed(totalSec: Long): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}
