package com.novavpn.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novavpn.app.data.Region
import com.novavpn.app.ui.components.FlagBadge
import com.novavpn.app.ui.components.SlideDirection
import com.novavpn.app.ui.components.SlideToggle
import com.novavpn.app.ui.map.WorldMap
import com.novavpn.app.ui.theme.Nova

@Composable
fun HomeScreen(
    regions: List<Region>,
    selected: Region,
    error: String?,
    onSelect: (Region) -> Unit,
    onOpenRegions: () -> Unit,
    onConnect: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Nova.Black)) {
        WorldMap(
            selected = selected,
            others = regions,
            onMarkerClick = onOpenRegions,
        )

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            if (error != null) {
                Text(
                    text = error,
                    color = Nova.Red,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }

            val listState = rememberLazyListState()
            val index = regions.indexOfFirst { it.id == selected.id }.coerceAtLeast(0)
            LaunchedEffect(selected.id) { listState.animateScrollToItem(index) }

            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                items(regions, key = { it.id }) { region ->
                    RegionChip(
                        region = region,
                        selected = region.id == selected.id,
                        onClick = {
                            if (region.id == selected.id) onOpenRegions() else onSelect(region)
                        },
                    )
                }
            }

            Spacer(Modifier.height(15.dp))

            SlideToggle(
                label = "Connect",
                labelColor = Nova.White,
                direction = SlideDirection.Right,
                onComplete = onConnect,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun RegionChip(region: Region, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .height(48.dp)
            .clip(shape)
            .background(if (selected) Nova.Lime.copy(alpha = 0.12f) else Nova.Card)
            .border(1.dp, if (selected) Nova.Lime.copy(alpha = 0.85f) else Nova.CardBorder, shape)
            .clickable(onClick = onClick)
            .padding(start = 9.dp, end = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlagBadge(region.code, 30.dp)
        Text(
            text = region.label,
            color = if (selected) Nova.Lime else Nova.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
