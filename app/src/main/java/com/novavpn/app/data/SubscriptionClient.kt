package com.novavpn.app.data

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Downloads a subscription URL and extracts the share links inside it. */
object SubscriptionClient {

    private val SCHEMES = listOf("vless", "vmess", "trojan", "ss", "socks", "socks5", "wireguard", "wg")
    private const val MAX_BYTES = 4 * 1024 * 1024

    suspend fun fetch(url: String): List<String> = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            // Many panels pick the output format from the client name.
            setRequestProperty("User-Agent", "NovaVPN/1.0 v2rayNG/1.9")
        }
        try {
            val code = conn.responseCode
            require(code in 200..299) { "Subscription server returned HTTP $code" }
            val body = conn.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(16 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    require(out.size() <= MAX_BYTES) { "Subscription is too large" }
                }
                out.toString("UTF-8")
            }
            extractLinks(body).also { require(it.isNotEmpty()) { "No supported servers found in this subscription" } }
        } finally {
            conn.disconnect()
        }
    }

    /** Plain list of links, or the same list base64-encoded (the usual subscription format). */
    fun extractLinks(body: String): List<String> {
        fun lines(t: String) = t.lines().map { it.trim() }
            .filter { line -> line.isNotEmpty() && SCHEMES.any { line.startsWith("$it://", ignoreCase = true) } }

        lines(body).takeIf { it.isNotEmpty() }?.let { return it }
        val decoded = runCatching {
            var t = body.filter { !it.isWhitespace() }.replace('-', '+').replace('_', '/')
            while (t.length % 4 != 0) t += "="
            String(Base64.decode(t, Base64.DEFAULT), Charsets.UTF_8)
        }.getOrDefault("")
        return lines(decoded)
    }
}
