package com.novavpn.app.ui.screens

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novavpn.app.data.Subscription
import com.novavpn.app.ui.Motion
import com.novavpn.app.ui.components.ChevronIcon
import com.novavpn.app.ui.theme.Nova
import com.novavpn.app.ui.theme.NovaSpace
import com.novavpn.app.ui.theme.NovaType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Section heading for a subscription's servers. When the provider sent plan details a chevron
 * appears; tapping the heading opens a few plain lines (traffic, expiry, note, links) under it.
 */
@Composable
fun SubscriptionHeader(
    sub: Subscription,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val info = sub.info?.takeIf { it.hasPlanDetails }
    val now = System.currentTimeMillis()
    val used = info?.usedBytes
    val total = info?.totalBytes?.takeIf { it > 0 }
    val expireMs = info?.expireAt?.takeIf { it > 0 }?.times(1000)
    val expired = expireMs != null && expireMs <= now
    val depleted = total != null && used != null && used >= total
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, tween(Motion.Short), label = "subChevron")

    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (info != null) Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onToggle) else Modifier)
                .padding(start = NovaSpace.Xs, end = NovaSpace.Xs, bottom = NovaSpace.Xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(sub.name, color = Nova.White, style = NovaType.Heading, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            Text("$count", color = Nova.TextMuted, style = NovaType.Secondary, modifier = Modifier.padding(start = NovaSpace.S))
            if (info != null) {
                if (expired || depleted) {
                    Text(
                        if (expired) "Expired" else "No traffic",
                        color = Nova.Red, style = NovaType.Caption, modifier = Modifier.padding(start = NovaSpace.S),
                    )
                }
                ChevronIcon(Nova.TextMuted, Modifier.padding(start = 2.dp).size(20.dp).rotate(rotation))
            }
        }

        if (info != null) {
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(Motion.Medium, easing = Motion.Standard)) + fadeIn(tween(Motion.Short)),
                exit = shrinkVertically(tween(Motion.Short, easing = Motion.Standard)) + fadeOut(tween(Motion.Fast)),
            ) {
                Details(sub, now, used, total, expireMs, expired, depleted)
            }
        }
    }
}

@Composable
private fun Details(
    sub: Subscription,
    now: Long,
    used: Long?,
    total: Long?,
    expireMs: Long?,
    expired: Boolean,
    depleted: Boolean,
) {
    val info = sub.info ?: return
    val uriHandler = LocalUriHandler.current
    Column(Modifier.fillMaxWidth().padding(start = NovaSpace.Xs, end = NovaSpace.Xs, bottom = NovaSpace.S), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Traffic
        if (used != null || info.totalBytes != null) {
            if (total != null && used != null) {
                val fraction = (used.toDouble() / total).coerceIn(0.0, 1.0).toFloat()
                val barColor = when {
                    depleted -> Nova.Red
                    fraction >= 0.8f -> Nova.Orange
                    else -> Nova.Lime
                }
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Nova.BarTrack)) {
                    Box(Modifier.fillMaxWidth(fraction).height(4.dp).clip(RoundedCornerShape(2.dp)).background(barColor))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${formatBytes(used)} of ${formatBytes(total)}", color = Nova.IconMuted, style = NovaType.Caption)
                    Text(
                        if (depleted) "None left" else "${formatBytes(total - used)} left",
                        color = if (depleted) Nova.Red else Nova.TextMuted, style = NovaType.Caption,
                    )
                }
            } else {
                val usedText = used?.let { "${formatBytes(it)} used · " }.orEmpty()
                Text(
                    if (total == null) usedText + "Unlimited" else "${formatBytes(total)} total",
                    color = Nova.IconMuted, style = NovaType.Caption,
                )
            }
        }

        // Expiry
        if (info.expireAt != null) {
            when {
                expireMs == null -> Text("No expiry", color = Nova.TextMuted, style = NovaType.Caption)
                expired -> Text("Expired ${formatDate(expireMs)}", color = Nova.Red, style = NovaType.Caption)
                else -> {
                    val days = ((expireMs - now) / DateUtils.DAY_IN_MILLIS).toInt()
                    val left = if (days >= 1) "$days day${if (days > 1) "s" else ""} left" else "less than a day left"
                    Text(
                        "Expires ${formatDate(expireMs)} · $left",
                        color = if (days <= 3) Nova.Orange else Nova.TextMuted, style = NovaType.Caption,
                    )
                }
            }
        }

        // Provider's note
        info.announce?.let {
            Text(it, color = Nova.TextMuted, style = NovaType.Caption, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }

        // Links, plain text
        val links = listOfNotNull(info.webUrl?.let { "Website" to it }, info.supportUrl?.let { "Support" to it })
        if (links.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                links.forEach { (label, url) ->
                    Text(
                        label, color = Nova.Lime, style = NovaType.Caption,
                        modifier = Modifier.clickable { runCatching { uriHandler.openUri(url) } },
                    )
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
