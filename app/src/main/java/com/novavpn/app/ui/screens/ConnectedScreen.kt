package com.novavpn.app.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.ui.draw.drawBehind
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
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        0f to Color(0xFF3A4200).copy(alpha = 0.85f),
                        0.5f to Color(0xFF1A1D00).copy(alpha = 0.6f),
                        1f to Color.Transparent,
                        center = Offset(size.width / 2f, size.height * 0.27f),
                        radius = size.width * 1.25f,
                    ),
                )
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Orb + status + timer
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.fillMaxWidth().height(290.dp),
                contentAlignment = Alignment.Center,
            ) {
                OrbView(
                    Modifier.requiredSize(345.dp),
                    speed = if (live) 1f else 4f,
                    radiusFraction = 0.84f,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        statusText,
                        color = if (live) Nova.Lime else Nova.White.copy(alpha = 0.85f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        formatElapsed(elapsedSec),
                        color = Nova.White,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Location card
            val cardShape = RoundedCornerShape(28.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(Nova.Card.copy(alpha = 0.92f))
                    .border(1.dp, Nova.CardBorder, cardShape)
                    .clickable(onClick = onOpenRegions)
                    .padding(start = 32.dp, end = 7.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    vpn.regionLabel ?: "",
                    color = Nova.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.Transparent)
                        .border(1.dp, Nova.CardBorder, RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    ChevronIcon(Nova.White, Modifier.size(24.dp), strokeWidth = 1.6.dp)
                }
            }

            Spacer(Modifier.height(5.dp))

            // Speed cards
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                SpeedCard("Download", speed.downMbps, DOWN_STEPS, Modifier.weight(1f))
                SpeedCard("Upload", speed.upMbps, UP_STEPS, Modifier.weight(1f))
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(20.dp))

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

/** Slight per-bar height variation, as in the design. */
private val BAR_HEIGHTS = intArrayOf(38, 35, 38, 35, 40)

@Composable
private fun SpeedCard(title: String, mbps: Float, steps: FloatArray, modifier: Modifier) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier
            .clip(shape)
            .background(Nova.Card.copy(alpha = 0.92f))
            .border(1.dp, Nova.CardBorder, shape)
            .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
    ) {
        Text(title, color = Nova.TextMuted, fontSize = 15.sp, modifier = Modifier.padding(start = 0.dp))
        Spacer(Modifier.height(28.dp))

        val level = if (mbps < 0.05f) -1 else steps.indexOfFirst { mbps <= it }.let { if (it == -1) 4 else it }
        SpeedBars(level)

        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                formatSpeed(mbps),
                color = Nova.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                " mb/s",
                color = Nova.TextMuted,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

/** Five rounded bars with a hatched strip above each; the bar for the current speed band lights up. */
@Composable
private fun SpeedBars(level: Int) {
    Row(
        Modifier.fillMaxWidth().height(60.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (i in 0 until 5) {
            val lit = i == level
            val barH by animateDpAsState((BAR_HEIGHTS[i] + if (lit) 2 else 0).dp, tween(250), label = "barH$i")
            Box(Modifier.weight(1f).fillMaxSize()) {
                // hatch strip
                Canvas(Modifier.fillMaxWidth().height(22.dp).align(Alignment.TopCenter)) {
                    val r = 6.dp.toPx()
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
                                Color(0xFF2D2D31),
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
                        .fillMaxWidth()
                        .height(barH)
                        .then(
                            if (lit) Modifier.shadow(
                                16.dp, RoundedCornerShape(10.dp),
                                ambientColor = Nova.Lime, spotColor = Nova.Lime,
                            ) else Modifier,
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (lit) Nova.Lime else Color(0xFF414148)),
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
