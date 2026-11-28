package com.novavpn.app.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novavpn.app.data.Subscription
import com.novavpn.app.ui.Press
import com.novavpn.app.ui.components.chipSurface
import com.novavpn.app.ui.pressable
import com.novavpn.app.ui.theme.Nova
import com.novavpn.app.ui.theme.NovaShapes
import com.novavpn.app.ui.theme.NovaType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Traffic, expiry and notes a subscription provider sent along with the server list. */
@Composable
fun SubscriptionCard(sub: Subscription, modifier: Modifier = Modifier) {
    val info = sub.info ?: return
    val used = info.usedBytes
    val total = info.totalBytes?.takeIf { it > 0 }
    val now = System.currentTimeMillis()
    val expireMs = info.expireAt?.takeIf { it > 0 }?.times(1000)
    val expired = expireMs != null && expireMs <= now
    val depleted = total != null && used != null && used >= total
    val fraction = if (total != null && used != null) (used.toDouble() / total).coerceIn(0.0, 1.0).toFloat() else 0f
    val barColor = when {
        depleted -> Nova.Red
        fraction >= 0.8f -> Nova.Orange
        else -> Nova.Lime
    }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier
            .fillMaxWidth()
            .clip(NovaShapes.Pill)
            .background(Nova.Card)
            .border(1.dp, Nova.CardBorder, NovaShapes.Pill)
            .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                sub.name, color = Nova.White, style = NovaType.BodyStrong,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            if (sub.updatedAt > 0) {
                Text(
                    "Updated " + DateUtils.getRelativeTimeSpanString(sub.updatedAt, now, DateUtils.MINUTE_IN_MILLIS),
                    color = Nova.TextMuted, style = NovaType.Small, modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        // Traffic
        if (used != null || info.totalBytes != null) {
            Spacer(Modifier.height(12.dp))
            if (total != null && used != null) {
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Nova.BarTrack)) {
                    Box(Modifier.fillMaxWidth(fraction).height(6.dp).clip(RoundedCornerShape(3.dp)).background(barColor))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${formatBytes(used)} of ${formatBytes(total)} used", color = Nova.White, style = NovaType.Caption)
                    Text(
                        if (depleted) "No traffic left" else "${formatBytes(total - used)} left",
                        color = if (depleted) Nova.Red else Nova.TextMuted, style = NovaType.Caption,
                    )
                }
            } else {
                val usedText = used?.let { "${formatBytes(it)} used · " }.orEmpty()
                Text(
                    if (total == null) usedText + "Unlimited traffic" else "${formatBytes(total)} traffic",
                    color = Nova.White, style = NovaType.Caption,
                )
            }
        }

        // Expiry
        if (info.expireAt != null) {
            Spacer(Modifier.height(if (used != null || info.totalBytes != null) 6.dp else 12.dp))
            val (text, color) = when {
                expireMs == null -> "No expiry" to Nova.TextMuted
                expired -> "Expired on ${formatDate(expireMs)}" to Nova.Red
                else -> {
                    val days = ((expireMs - now) / DateUtils.DAY_IN_MILLIS).toInt()
                    val left = when {
                        days >= 1 -> "$days day${if (days > 1) "s" else ""} left"
                        else -> "less than a day left"
                    }
                    "Expires ${formatDate(expireMs)} · $left" to (if (days <= 3) Nova.Orange else Nova.TextMuted)
                }
            }
            Text(text, color = color, style = NovaType.Caption)
        }

        // Provider's note
        info.announce?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = Nova.IconMuted, style = NovaType.Caption, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }

        // Links
        val links = listOfNotNull(
            info.webUrl?.let { "Website" to it },
            info.supportUrl?.let { "Support" to it },
        )
        if (links.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                links.forEach { (label, url) ->
                    val shape = NovaShapes.Control
                    Box(
                        Modifier
                            .height(36.dp)
                            .pressable(shape, Press.Pill, label = label, onClick = { runCatching { uriHandler.openUri(url) } })
                            .chipSurface(shape)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(label, color = Nova.White, style = NovaType.Secondary) }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val kb = 1024.0
    val v = bytes.toDouble()
    return when {
        v >= kb * kb * kb * kb -> String.format(Locale.US, "%.2f TB", v / (kb * kb * kb * kb))
        v >= kb * kb * kb -> String.format(Locale.US, "%.1f GB", v / (kb * kb * kb))
        v >= kb * kb -> String.format(Locale.US, "%.0f MB", v / (kb * kb))
        v >= kb -> String.format(Locale.US, "%.0f KB", v / kb)
        else -> "$bytes B"
    }
}

private fun formatDate(ms: Long): String = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(ms))
