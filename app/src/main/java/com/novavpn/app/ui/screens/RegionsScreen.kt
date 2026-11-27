package com.novavpn.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novavpn.app.data.Region
import com.novavpn.app.ui.components.CheckIcon
import com.novavpn.app.ui.components.ChevronIcon
import com.novavpn.app.ui.components.OrbView
import com.novavpn.app.ui.components.SignalIcon
import com.novavpn.app.ui.theme.Nova

@Composable
fun RegionsScreen(
    regions: List<Region>,
    selectedId: String,
    pings: Map<String, Int?>,
    onSelect: (Region) -> Unit,
    onMoreRegions: () -> Unit,
) {
    val free = regions.filter { !it.premium }
    val premium = regions.filter { it.premium }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(Nova.Black)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Banner(regions, onMoreRegions) }
        item { Spacer(Modifier.height(4.dp)) }

        items(free, key = { it.id }) { r ->
            RegionRow(r, r.id == selectedId, pings[r.id]) { onSelect(r) }
        }

        if (premium.isNotEmpty()) {
            item {
                Text(
                    "Premium regions",
                    color = Nova.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 14.dp, bottom = 4.dp, start = 4.dp),
                )
            }
            items(premium, key = { it.id }) { r ->
                RegionRow(r, r.id == selectedId, pings[r.id]) { onSelect(r) }
            }
        }

        item { Spacer(Modifier.navigationBarsPadding()) }
    }
}

@Composable
private fun Banner(regions: List<Region>, onClick: () -> Unit) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    0f to Color(0xFF0C0C0D),
                    1f to Color(0xFF1A1F00),
                ),
            )
            .clickable(onClick = onClick),
    ) {
        // Orb peeking in from the right edge
        OrbView(
            Modifier
                .size(260.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 120.dp),
        )

        Text(
            "Need more\nregions?",
            color = Nova.White,
            fontSize = 26.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 20.dp),
        )

        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
                .size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Nova.White),
            contentAlignment = Alignment.Center,
        ) {
            ChevronIcon(Nova.Black, Modifier.size(22.dp))
        }

        Row(
            Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val flags = regions.take(2).map { it.flag }
            Box(Modifier.width(66.dp).height(30.dp)) {
                flags.forEachIndexed { i, f ->
                    Box(
                        Modifier
                            .offset(x = (i * 20).dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Nova.CardRaised)
                            .border(1.5.dp, Color(0xFF0C0C0D), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text(f, fontSize = 15.sp) }
                }
                Box(
                    Modifier
                        .offset(x = (flags.size * 20).dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Nova.White)
                        .border(1.5.dp, Color(0xFF0C0C0D), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("+", color = Nova.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }
            Text(
                "Europe, USA, Canada",
                color = Nova.TextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 28.dp),
            )
        }
    }
}

@Composable
private fun RegionRow(region: Region, selected: Boolean, pingMs: Int?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val bg = if (selected) Nova.Lime else Nova.Card
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .then(if (selected) Modifier else Modifier.border(1.dp, Nova.CardBorder, shape))
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            region.label,
            color = if (selected) Nova.Black else Nova.White.copy(alpha = 0.78f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        val btnShape = RoundedCornerShape(18.dp)
        Box(
            Modifier
                .size(54.dp)
                .clip(btnShape)
                .background(if (selected) Color.Transparent else Nova.CardRaised)
                .border(1.dp, if (selected) Nova.Black.copy(alpha = 0.85f) else Nova.CardBorder, btnShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                CheckIcon(Nova.Black, Modifier.size(22.dp))
            } else {
                SignalIcon(pingColor(pingMs), Modifier.size(20.dp))
            }
        }
    }
}

private fun pingColor(ms: Int?): Color = when {
    ms == null -> Nova.Gray
    ms < 120 -> Nova.Green
    ms < 250 -> Nova.Orange
    else -> Nova.Red
}
