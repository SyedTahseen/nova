package com.novavpn.app.vpn

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.novavpn.app.data.Region
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class VpnStatus { Disconnected, Connecting, Connected, Disconnecting }

data class VpnState(
    val status: VpnStatus = VpnStatus.Disconnected,
    val regionId: String? = null,
    val regionLabel: String? = null,
    /** Wall-clock time (ms) at which the tunnel came up; 0 while not connected. */
    val connectedAtMs: Long = 0L,
    val error: String? = null,
    /** Non-fatal problem while connected, e.g. the server isn't answering. */
    val warning: String? = null,
)

data class Speed(val downMbps: Float = 0f, val upMbps: Float = 0f)

/** Process-wide bridge between the UI and [NovaVpnService]. */
object VpnController {
    const val ACTION_START = "com.novavpn.app.START"
    const val ACTION_STOP = "com.novavpn.app.STOP"
    const val EXTRA_ID = "id"
    const val EXTRA_LABEL = "label"
    const val EXTRA_LINK = "link"

    private val _state = MutableStateFlow(VpnState())
    val state: StateFlow<VpnState> = _state.asStateFlow()

    private val _speed = MutableStateFlow(Speed())
    val speed: StateFlow<Speed> = _speed.asStateFlow()

    internal fun update(block: (VpnState) -> VpnState) = _state.update(block)
    internal fun setSpeed(s: Speed) { _speed.value = s }

    fun clearError() = _state.update { it.copy(error = null) }

    /** Starts (or switches) the tunnel. VPN consent must already have been granted. */
    fun connect(context: Context, region: Region) {
        val intent = Intent(context, NovaVpnService::class.java)
            .setAction(ACTION_START)
            .putExtra(EXTRA_ID, region.id)
            .putExtra(EXTRA_LABEL, region.label)
            .putExtra(EXTRA_LINK, region.link)
        ContextCompat.startForegroundService(context, intent)
    }

    fun disconnect(context: Context) {
        context.startService(
            Intent(context, NovaVpnService::class.java).setAction(ACTION_STOP),
        )
    }
}
