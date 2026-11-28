package com.novavpn.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novavpn.app.MainViewModel
import com.novavpn.app.data.Region
import com.novavpn.app.ui.screens.ConnectedScreen
import com.novavpn.app.ui.screens.HomeScreen
import com.novavpn.app.ui.screens.RegionsScreen
import com.novavpn.app.ui.screens.SettingsScreen
import com.novavpn.app.ui.theme.Nova
import com.novavpn.app.vpn.VpnController
import com.novavpn.app.vpn.VpnStatus

@Composable
fun NovaApp(
    vm: MainViewModel,
    onConnect: (Region) -> Unit,
    onDisconnect: () -> Unit,
) {
    val context = LocalContext.current
    val vpn by VpnController.state.collectAsStateWithLifecycle()
    val speed by VpnController.speed.collectAsStateWithLifecycle()
    val selectedId by vm.selectedId.collectAsStateWithLifecycle()
    val pings by vm.pings.collectAsStateWithLifecycle()
    val regions by vm.regions.collectAsStateWithLifecycle()

    var showRegions by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var openAddOnShow by rememberSaveable { mutableStateOf(false) }
    // Registered first so the Regions handler below wins when Regions is open on top of Settings.
    BackHandler(enabled = showSettings) { showSettings = false }
    BackHandler(enabled = showRegions) { showRegions = false; openAddOnShow = false }

    val selected = vm.region(selectedId)
    val inSession = vpn.status != VpnStatus.Disconnected

    Box(Modifier.fillMaxSize().background(Nova.Black)) {
        Crossfade(targetState = inSession, label = "root") { connected ->
            if (connected) {
                ConnectedScreen(
                    vpn = vpn,
                    speed = speed,
                    onOpenRegions = { showRegions = true },
                    onDisconnect = onDisconnect,
                )
            } else {
                HomeScreen(
                    regions = regions,
                    selected = selected,
                    error = vpn.error,
                    onSelect = { vm.select(it.id); VpnController.clearError() },
                    onOpenRegions = { showRegions = true },
                    onOpenSettings = { showSettings = true },
                    onAddServer = { openAddOnShow = true; showRegions = true },
                    mapRecenterKey = showRegions to showSettings,
                    onConnect = {
                        VpnController.clearError()
                        selected?.let(onConnect)
                    },
                )
            }
        }

        AnimatedVisibility(
            visible = showSettings,
            enter = slideInVertically { it / 6 } + fadeIn(),
            exit = slideOutVertically { it / 6 } + fadeOut(),
        ) {
            SettingsScreen(
                onBack = { showSettings = false },
                onOpenRegions = { showRegions = true },
                onRefreshPings = {
                    vm.refreshPings()
                    Toast.makeText(context, "Re-testing servers…", Toast.LENGTH_SHORT).show()
                },
            )
        }

        AnimatedVisibility(
            visible = showRegions,
            enter = slideInVertically { it / 6 } + fadeIn(),
            exit = slideOutVertically { it / 6 } + fadeOut(),
        ) {
            RegionsScreen(
                vm = vm,
                onSelect = { r ->
                    vm.select(r.id)
                    showRegions = false
                    openAddOnShow = false
                    // Switch servers on the fly if a tunnel is already up.
                    if (vpn.status == VpnStatus.Connected && vpn.regionId != r.id) onConnect(r)
                },
                onMoreRegions = {
                    Toast.makeText(context, "More regions coming soon", Toast.LENGTH_SHORT).show()
                },
                startWithAdd = openAddOnShow,
            )
        }
    }
}
