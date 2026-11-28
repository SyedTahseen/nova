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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.novavpn.app.ui.components.DialInk
import com.novavpn.app.ui.components.PowerDial
import com.novavpn.app.ui.components.SlideDirection
import com.novavpn.app.ui.components.SlideToggle
import com.novavpn.app.ui.theme.Nova
import com.novavpn.app.vpn.Speed
import com.novavpn.app.vpn.VpnState
import com.novavpn.app.vpn.VpnStatus
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.ceil

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
            // Tick ring + yellow disc with status + timer inside
            Box(
                Modifier.fillMaxWidth().height(330.dp),
                contentAlignment = Alignment.Center,
            ) {
                PowerDial(Modifier.size(310.dp), live = live) {
                    val statusColor by animateColorAsState(
                        if (live) DialInk else DialInk.copy(alpha = 0.8f),
                        tween(Motion.Long, easing = Motion.Standard), label = "statusColor",
                    )
                    AnimatedContent(
                        targetState = statusText,
                        transitionSpec = { statusTransition() },
                        label = "statusText",
                    ) { text ->
                        Text(text, color = statusColor, style = NovaType.Status)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        formatElapsed(elapsedSec),
                        color = DialInk,
                        style = NovaType.Timer,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Location card
            val cardShape = NovaShapes.Pill
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressable(cardShape, Press.Card, onClick = onOpenRegions)
                    .background(Nova.Card.copy(alpha = 0.92f))
                    .border(1.dp, Nova.CardBorder, cardShape)
                    .padding(start = 20.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    vpn.regionLabel ?: "",
                    color = Nova.White,
                    style = NovaType.BodyStrong,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .size(54.dp)
                        .clip(NovaShapes.Control)
                        .background(Nova.CardRaised)
                        .border(1.dp, Nova.CardBorder, NovaShapes.Control),
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

            Spacer(Modifier.height(10.dp))

            // Speed cards
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SpeedCard("Download", speed.downMbps, DOWN_STEPS, Modifier.weight(1f))
                SpeedCard("Upload", speed.upMbps, UP_STEPS, Modifier.weight(1f))
            }

            Spacer(Modifier.weight(1f))

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

private val DOWN_STEPS = floatArrayOf(5f, 15f, 30f, 60f, 100f)
private val UP_STEPS = floatArrayOf(2f, 8f, 15f, 30f, 60f)

@Composable
private fun SpeedCard(title: String, mbps: Float, steps: FloatArray, modifier: Modifier) {
    val shape = NovaShapes.Pill
    Column(
        modifier
            .clip(shape)
            .background(Nova.Card.copy(alpha = 0.92f))
            .border(1.dp, Nova.CardBorder, shape)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(title, color = Nova.TextMuted, style = NovaType.Caption, modifier = Modifier.padding(start = 2.dp))
        Spacer(Modifier.height(10.dp))

        val level = if (mbps < 0.05f) -1 else steps.indexOfFirst { mbps <= it }.let { if (it == -1) 4 else it }
        SpeedBars(level)

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(start = 2.dp)) {
            Text(
                formatSpeed(mbps),
                color = Nova.White,
                style = NovaType.Display,
            )
            Text(
                " mb/s",
                color = Nova.TextMuted,
                style = NovaType.Body,
                modifier = Modifier.padding(bottom = 7.dp),
            )
        }
    }
}

/** Five rounded bars with a hatched strip above each; the bar for the current speed band lights up. */
@Composable
private fun SpeedBars(level: Int) {
    Row(
        Modifier.fillMaxWidth().height(62.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        for (i in 0 until 5) {
            val lit = i == level
            val lift by animateFloatAsState(if (lit) 1f else 0f, tween(Motion.Medium, easing = Motion.Standard), label = "lift$i")
            Box(Modifier.weight(1f).fillMaxSize()) {
                // hatch strip
                Canvas(Modifier.fillMaxWidth().height(22.dp).align(Alignment.TopCenter)) {
                    val r = 5.dp.toPx()
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
                // bar
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-6 * lift).dp)
                        .fillMaxWidth()
                        .height(34.dp)
                        .then(
                            if (lit) Modifier.shadow(
                                12.dp, NovaShapes.Small,
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
