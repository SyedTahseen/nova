package com.novavpn.app.ui.screens

import com.novavpn.app.ui.theme.NovaShapes
import com.novavpn.app.ui.theme.NovaType
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.novavpn.app.ui.Motion
import com.novavpn.app.ui.Press
import com.novavpn.app.ui.pressable
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

/** Item count / start index of the looping pill row: far enough from both ends that nobody reaches them. */
private const val REGION_ROW_COUNT = 2_000_000
const val REGION_ROW_START = 1_000_000

@Composable
fun HomeScreen(
    regions: List<Region>,
    /** The few pills shown in the row (recent + fastest), not every server. */
    pills: List<Region>,
    selected: Region?,
    error: String?,
    onSelect: (Region) -> Unit,
    onOpenRegions: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddServer: () -> Unit,
    onConnect: () -> Unit,
    /** Scroll position of the region row. Owned by NovaApp so it survives leaving and re-entering Home. */
    listState: LazyListState,
    /** Region that was last brought to the middle of the row (null = never, i.e. the app was just opened). */
    centeredId: String?,
    onCentered: (String) -> Unit,
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
            // Keep the last message while it fades out so the text does not vanish mid-animation.
            var lastError by remember { mutableStateOf(error) }
            if (error != null) lastError = error
            AnimatedVisibility(
                visible = error != null,
                enter = fadeIn(tween(Motion.Short, easing = Motion.Enter)) + expandVertically(tween(Motion.Medium, easing = Motion.Standard)),
                exit = fadeOut(tween(Motion.Fast)) + shrinkVertically(tween(Motion.Short, easing = Motion.Standard)),
            ) {
                Text(
                    text = lastError.orEmpty(),
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
            val n = pills.size
            val loop = n >= 3
            val index = pills.indexOfFirst { it.id == selected.id }.coerceAtLeast(0)
            // Bring the selected pill to the middle when the selection changes (or the app is opened).
            // Coming back to Home with the same selection leaves the row exactly where the user scrolled it.
            LaunchedEffect(selected.id, n) {
                val id = selected.id
                if (id == centeredId || n == 0) return@LaunchedEffect
                // On first open the row has not been measured yet; wait for it.
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.isNotEmpty() }.first { it }
                fun isSelected(i: Int) = pills.getOrNull(if (loop) i.mod(n) else i)?.id == id
                // Of the visible copies of the selected pill, the one closest to the middle.
                fun distanceToCenter(): Float? {
                    val info = listState.layoutInfo
                    val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                    return info.visibleItemsInfo
                        .filter { isSelected(it.index) }
                        .map { it.offset + it.size / 2f - viewportCenter }
                        .minByOrNull { kotlin.math.abs(it) }
                }
                // Off screen: jump close first so it can be measured.
                if (distanceToCenter() == null) {
                    val first = listState.firstVisibleItemIndex
                    listState.scrollToItem(if (loop) first - first.mod(n) + index else index)
                }
                distanceToCenter()?.let {
                    if (centeredId == null) listState.scrollBy(it) // app just opened: already centered, no animation
                    else listState.animateScrollBy(it, tween(Motion.Long, easing = Motion.Standard))
                }
                onCentered(id)
            }

            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                // Endless row: the same few pills repeat in both directions.
                items(if (loop) REGION_ROW_COUNT else n, key = { it }) { i ->
                    val region = pills[if (loop) i % n else i]
                    RegionChip(
                        region = region,
                        selected = region.id == selected.id,
                        onClick = {
                            if (region.id == selected.id) onOpenRegions() else onSelect(region)
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
            .pressable(shape, Press.Pill, label = "Open settings", onClick = onClick)
            .chipSurface(shape, fillAlpha = PILL_OVERLAY_ALPHA),
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
            .pressable(shape, Press.Pill, onClick = onClick)
            .chipSurface(shape, selected)
            .padding(start = 9.dp, end = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlagBadge(region.code, size = 30)
        val labelColor by animateColorAsState(if (selected) Nova.Lime else Nova.White, tween(Motion.Short), label = "chipLabel")
        Text(
            text = region.label,
            color = labelColor,
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

