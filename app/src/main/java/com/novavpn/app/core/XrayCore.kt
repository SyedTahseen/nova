package com.novavpn.app.core

import android.content.Context
import android.util.Log
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray

/**
 * Thin wrapper around the gomobile-generated Xray bindings (AndroidLibXrayLite, `libv2ray.aar`).
 *
 * This is the ONLY file that touches the native library. If your libv2ray build exposes a slightly
 * different API (older builds use `Libv2ray.newV2RayPoint(...)` / `runLoop()`), adapt it here.
 */
object XrayCore {
    private const val TAG = "XrayCore"

    private var envReady = false
    private var controller: CoreController? = null

    @Synchronized
    fun start(context: Context, configJson: String, tunFd: Int) {
        if (!envReady) {
            Libv2ray.initCoreEnv(context.filesDir.absolutePath, "")
            envReady = true
        }
        stop()

        val handler = object : CoreCallbackHandler {
            override fun startup(): Long {
                Log.i(TAG, "core started")
                return 0
            }

            override fun shutdown(): Long {
                Log.i(TAG, "core stopped")
                return 0
            }

            override fun onEmitStatus(l: Long, s: String?): Long {
                Log.d(TAG, "status: $s")
                return 0
            }
        }

        val c = Libv2ray.newCoreController(handler)
        c.startLoop(configJson, tunFd)
        controller = c
    }

    @Synchronized
    fun stop() {
        runCatching { controller?.stopLoop() }
            .onFailure { Log.w(TAG, "stopLoop failed", it) }
        controller = null
    }

    val isRunning: Boolean
        @Synchronized get() = controller?.isRunning == true
}
