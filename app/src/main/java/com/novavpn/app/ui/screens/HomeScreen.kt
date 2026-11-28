package com.novavpn.app.ui.screens

import com.novavpn.app.ui.theme.NovaShapes
import com.novavpn.app.ui.theme.NovaType
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novavpn.app.data.Region
import com.novavpn.app.ui.components.HamburgerIcon
import com.novavpn.app.ui.components.EmptyStateCard
import com.novavpn.app.ui.components.PILL_OVERLAY_ALPHA
import com.novavpn.app.ui.components.chipSurface
import com.novavpn.app.ui.components.SlideDirection
import com.novavpn.app.ui.components.SlideToggle
import com.novavpn.app.ui.map.WorldMap
import com.novavpn.app.ui.theme.Nova

@Composable
fun HomeScreen(
    regions: List<Region>,
    selected: Region?,
    error: String?,
    onSelect: (Region) -> Unit,
    onOpenRegions: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddServer: () -> Unit,
    onConnect: () -> Unit,
    mapOverlayOpen: Boolean = false,
) {
    Box(Modifier.fillMaxSize().background(Nova.Black)) {
        WorldMap(
            selected = selected,
            others = regions,
            onMarkerClick = onOpenRegions,
            overlayOpen = mapOverlayOpen,
        )

        MenuPill(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 12.dp),
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
                    style = NovaType.Caption,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }

            if (selected == null) {
                EmptyStateCard(
                    title = "No servers yet",
                    message = "Add a vless, vmess, trojan, ss, socks or wireguard link. The flag and location are taken from the proxy name, e.g. Germany.",
                    actionLabel = "Add server",
                    onAction = onAddServer,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    overlay = true,
                )
            } else {
            val listState = rememberLazyListState()
            val index = regions.indexOfFirst { it.id == selected?.id }.coerceAtLeast(0)
            LaunchedEffect(selected?.id) { listState.animateScrollToItem(index) }

            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                items(regions, key = { it.id }) { region ->
                    RegionChip(
                        region = region,
                        selected = region.id == selected?.id,
                        onClick = {
                            if (region.id == selected?.id) onOpenRegions() else onSelect(region)
                        },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

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
}

/** Hamburger pill pinned to the top-left of Home; opens Settings. */
@Composable
private fun MenuPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = NovaShapes.Pill
    Box(
        modifier
            .size(width = 64.dp, height = 48.dp)
            .chipSurface(shape, fillAlpha = PILL_OVERLAY_ALPHA)
            .clickable(onClickLabel = "Open settings", role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        HamburgerIcon(Nova.White, Modifier.size(22.dp))
    }
}

@Composable
private fun RegionChip(region: Region, selected: Boolean, onClick: () -> Unit) {
    val shape = NovaShapes.Pill
    Row(
        Modifier
            .height(48.dp)
            .chipSurface(shape, selected)
            .clickable(onClick = onClick)
            .padding(start = 9.dp, end = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlagBadge(region.code, size = 30)
        Text(
            text = region.label,
            color = if (selected) Nova.Lime else Nova.White,
            style = NovaType.Secondary,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/** Round country flag: the flag artwork cropped to fill the circle. Unknown codes show an empty circle. */
@Composable
fun FlagBadge(code: String, size: Int, modifier: Modifier = Modifier) {
    val res = FLAG_RES[code.uppercase()]
    Box(modifier.size(size.dp).clip(CircleShape).background(Nova.CardRaised)) {
        if (res != null) {
            Image(
                painterResource(res),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

