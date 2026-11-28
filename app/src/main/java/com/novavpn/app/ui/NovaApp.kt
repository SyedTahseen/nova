package com.novavpn.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
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

/** The screens of the app. Exactly one is shown at a time; Settings and Regions slide over the main screen. */
private enum class Screen { Home, Connected, Settings, Regions }

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
    val regions by vm.regions.collectAsStateWithLifecycle()

    // "main" = Home or Connected (depending on the VPN), otherwise the name of the open sub screen.
    var sub by rememberSaveable { mutableStateOf("main") }
    // Where Back from Regions goes: Regions can be opened from Home, Connected and Settings.
    var regionsParent by rememberSaveable { mutableStateOf("main") }
    var openAddOnShow by rememberSaveable { mutableStateOf(false) }
    // Home's region row lives here so its scroll position survives opening Settings/Regions and coming back.
    val regionRowState = rememberLazyListState()
    var centeredRegionId by rememberSaveable { mutableStateOf<String?>(null) }

    fun openRegions(from: String, add: Boolean = false) {
        regionsParent = from
        openAddOnShow = add
        sub = "regions"
    }

    fun goBack() {
        sub = if (sub == "regions") regionsParent else "main"
        openAddOnShow = false
    }
    // Regions registers its own handler for closing the search field first; that one wins while searching.
    BackHandler(enabled = sub != "main") { goBack() }

    val selected = vm.region(selectedId)
    val inSession = vpn.status != VpnStatus.Disconnected

    val screen = when (sub) {
        "settings" -> Screen.Settings
        "regions" -> Screen.Regions
        else -> if (inSession) Screen.Connected else Screen.Home
    }

    Box(Modifier.fillMaxSize().background(Nova.Black)) {
        AnimatedContent(
            targetState = screen,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                when {
                    // Closing a panel: it slides back the way it came, over the screen below.
                    initialState == Screen.Regions -> panelTransition(entering = false, fromLeft = false)
                    initialState == Screen.Settings -> panelTransition(entering = false, fromLeft = true)
                    // Opening: Regions rises from the bottom, Settings comes in from the left.
                    targetState == Screen.Regions -> panelTransition(entering = true, fromLeft = false)
                    targetState == Screen.Settings -> panelTransition(entering = true, fromLeft = true)
                    else -> screenTransition()
                }
            },
            label = "screen",
        ) { s ->
            when (s) {
                Screen.Connected -> ConnectedScreen(
                    vpn = vpn,
                    speed = speed,
                    onOpenRegions = { openRegions("main") },
                    onDisconnect = onDisconnect,
                )

                Screen.Home -> HomeScreen(
                    regions = regions,
                    selected = selected,
                    error = vpn.error,
                    onSelect = { vm.select(it.id); VpnController.clearError() },
                    onOpenRegions = { openRegions("main") },
                    onOpenSettings = { sub = "settings" },
                    onAddServer = { openRegions("main", add = true) },
                    listState = regionRowState,
                    centeredId = centeredRegionId,
                    onCentered = { centeredRegionId = it },
                    onConnect = {
                        VpnController.clearError()
                        selected?.let(onConnect)
                    },
                )

                Screen.Settings -> SettingsScreen(
                    onBack = { goBack() },
                    onOpenRegions = { openRegions("settings") },
                    onRefreshPings = {
                        vm.refreshPings()
                        Toast.makeText(context, "Re-testing servers…", Toast.LENGTH_SHORT).show()
                    },
                )

                Screen.Regions -> RegionsScreen(
                    vm = vm,
                    onSelect = { r ->
                        vm.select(r.id)
                        goBack()
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
}
