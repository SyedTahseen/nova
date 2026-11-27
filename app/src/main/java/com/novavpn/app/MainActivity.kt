package com.novavpn.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.novavpn.app.data.Region
import com.novavpn.app.ui.NovaApp
import com.novavpn.app.ui.theme.NovaTheme
import com.novavpn.app.vpn.VpnController

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private var pendingRegion: Region? = null

    private val vpnConsent = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            pendingRegion?.let { VpnController.connect(this, it) }
        }
        pendingRegion = null
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* VPN works either way; the status notification is just hidden if denied. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            NovaTheme {
                NovaApp(
                    vm = vm,
                    onConnect = ::connect,
                    onDisconnect = { VpnController.disconnect(this) },
                )
            }
        }
    }

    private fun connect(region: Region) {
        val consent = VpnService.prepare(this)
        if (consent != null) {
            pendingRegion = region
            vpnConsent.launch(consent)
        } else {
            VpnController.connect(this, region)
        }
    }
}
