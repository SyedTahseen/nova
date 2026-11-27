package com.novavpn.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.TrafficStats
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.novavpn.app.MainActivity
import com.novavpn.app.R
import com.novavpn.app.core.ServerLink
import com.novavpn.app.core.XrayConfigBuilder
import com.novavpn.app.core.XrayCore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NovaVpnService : VpnService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tun: ParcelFileDescriptor? = null
    private var connectJob: Job? = null
    private var speedJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            VpnController.ACTION_START -> {
                val link = intent.getStringExtra(VpnController.EXTRA_LINK)
                val label = intent.getStringExtra(VpnController.EXTRA_LABEL) ?: "Nova VPN"
                val id = intent.getStringExtra(VpnController.EXTRA_ID)
                if (link == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                enterForeground(notification("Connecting…", label))
                val previous = connectJob
                connectJob = scope.launch {
                    previous?.cancelAndJoin()
                    connect(id, label, link)
                }
            }

            VpnController.ACTION_STOP -> disconnect()
        }
        return START_NOT_STICKY
    }

    private suspend fun connect(id: String?, label: String, link: String) {
        VpnController.update { VpnState(VpnStatus.Connecting, id, label) }
        VpnController.setSpeed(Speed())
        try {
            speedJob?.cancel()
            teardownCore()

            val server = ServerLink.parse(link)
            val config = XrayConfigBuilder.build(server)

            val builder = Builder()
                .setSession(label)
                .setMtu(1500)
                .addAddress("10.10.14.1", 30)
                .addRoute("0.0.0.0", 0)
                .addAddress("fd00:10:14::1", 126)
                .addRoute("::", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
            // Xray's own outbound sockets must not loop back into the tunnel.
            runCatching { builder.addDisallowedApplication(packageName) }

            val pfd = builder.establish() ?: error("VPN permission was revoked")
            tun = pfd

            XrayCore.start(applicationContext, config, pfd.fd)

            delay(600)
            check(XrayCore.isRunning) { "Xray core failed to start" }

            VpnController.update {
                VpnState(VpnStatus.Connected, id, label, connectedAtMs = System.currentTimeMillis())
            }
            notifyUpdate(notification("Connected", label))
            startSpeedMonitor()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            teardownCore()
            VpnController.update { VpnState(error = e.message ?: "Connection failed") }
            VpnController.setSpeed(Speed())
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun disconnect() {
        VpnController.update { it.copy(status = VpnStatus.Disconnecting) }
        connectJob?.cancel()
        speedJob?.cancel()
        scope.launch {
            teardownCore()
            VpnController.update { VpnState() }
            VpnController.setSpeed(Speed())
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun teardownCore() {
        XrayCore.stop()
        runCatching { tun?.close() }
        tun = null
    }

    /** Samples this app's own socket counters: Xray's outbound traffic is our uid's traffic. */
    private fun startSpeedMonitor() {
        speedJob?.cancel()
        speedJob = scope.launch {
            val uid = Process.myUid()
            var lastRx = TrafficStats.getUidRxBytes(uid)
            var lastTx = TrafficStats.getUidTxBytes(uid)
            var last = SystemClock.elapsedRealtime()
            var down = 0f
            var up = 0f
            while (isActive) {
                delay(1000)
                val now = SystemClock.elapsedRealtime()
                val rx = TrafficStats.getUidRxBytes(uid)
                val tx = TrafficStats.getUidTxBytes(uid)
                val dt = (now - last) / 1000f
                if (dt > 0f && rx >= 0 && tx >= 0) {
                    val d = (rx - lastRx).coerceAtLeast(0) * 8f / 1_000_000f / dt
                    val u = (tx - lastTx).coerceAtLeast(0) * 8f / 1_000_000f / dt
                    down = down * 0.4f + d * 0.6f
                    up = up * 0.4f + u * 0.6f
                    VpnController.setSpeed(Speed(down, up))
                }
                lastRx = rx
                lastTx = tx
                last = now
            }
        }
    }

    override fun onRevoke() {
        disconnect()
    }

    override fun onDestroy() {
        scope.cancel()
        teardownCore()
        VpnController.update { VpnState() }
        VpnController.setSpeed(Speed())
        super.onDestroy()
    }

    // --- notification ----------------------------------------------------------------------

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notif_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    private fun notification(title: String, text: String): Notification {
        ensureChannel()
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, NovaVpnService::class.java).setAction(VpnController.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_vpn)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, getString(R.string.notif_disconnect), stop)
            .build()
    }

    private fun enterForeground(n: Notification) {
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIF_ID, n, type)
    }

    private fun notifyUpdate(n: Notification) {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, n)
    }

    private companion object {
        const val CHANNEL_ID = "vpn_status"
        const val NOTIF_ID = 1
    }
}
