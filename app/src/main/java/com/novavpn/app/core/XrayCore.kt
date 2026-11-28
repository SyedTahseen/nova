package com.novavpn.app.core

import android.content.Context
import android.util.Log
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import java.io.File
import java.lang.reflect.InvocationTargetException

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

    /** Xray-core version bundled in libv2ray.aar, e.g. "26.11.26". */
    fun version(): String = runCatching { Libv2ray.checkVersionX() }.getOrDefault("unknown")

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

    /** Xray's error log; recreated on every connect. */
    fun logFile(context: Context): File = File(context.filesDir, "xray.log")

    /** Last few lines of Xray's error log, for showing the real reason a connection failed. */
    fun logTail(context: Context, lines: Int = 3): String? = runCatching {
        logFile(context).readLines().filter { it.isNotBlank() }.takeLast(lines)
            .joinToString("\n") { it.substringAfter("] ").take(200) }
            .takeIf { it.isNotBlank() }
    }.getOrNull()

    /**
     * Real round-trip through the running tunnel (HTTP GET of [url]) in ms.
     * Returns -1 if it failed, or null when this libv2ray build has no measureDelay (unknown).
     */
    fun measureDelay(url: String): Long? {
        val c = synchronized(this) { controller } ?: return null
        val m = runCatching { c.javaClass.getMethod("measureDelay", String::class.java) }.getOrNull()
            ?: return null
        return try {
            (m.invoke(c, url) as Number).toLong()
        } catch (e: InvocationTargetException) {
            Log.w(TAG, "measureDelay failed: ${e.targetException?.message}")
            -1L
        } catch (e: Throwable) {
            null
        }
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
