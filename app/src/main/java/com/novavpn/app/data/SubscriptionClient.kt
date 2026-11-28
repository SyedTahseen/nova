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

    /** The servers in a subscription plus whatever the provider says about the plan. */
    data class Fetched(val links: List<String>, val info: SubscriptionInfo?)

    suspend fun fetch(url: String): Fetched = withContext(Dispatchers.IO) {
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
            val links = extractLinks(body).also { require(it.isNotEmpty()) { "No supported servers found in this subscription" } }

            // Plan details: response headers win, "#key: value" lines in the list fill the gaps.
            val meta = HashMap<String, String>()
            bodyMeta(body).forEach { (k, v) -> meta[k] = v }
            decodeBase64(body)?.let { d -> bodyMeta(d).forEach { (k, v) -> meta.putIfAbsent(k, v) } }
            listOf(
                "subscription-userinfo", "profile-title", "profile-update-interval",
                "support-url", "profile-web-page-url", "announce",
            ).forEach { key -> conn.getHeaderField(key)?.takeIf { it.isNotBlank() }?.let { meta[key] = it } }
            if ("profile-title" !in meta) {
                // Some panels only name the profile in Content-Disposition: attachment; filename="Name".
                conn.getHeaderField("content-disposition")
                    ?.let { Regex("filename\\*?=(?:UTF-8'')?\"?([^\";]+)").find(it)?.groupValues?.get(1) }
                    ?.let { meta["profile-title"] = java.net.URLDecoder.decode(it.trim(), "UTF-8") }
            }
            Fetched(links, parseInfo(meta))
        } finally {
            conn.disconnect()
        }
    }

    /** "# key: value" / "#key=value" comment lines some providers put at the top of the list. */
    private fun bodyMeta(text: String): Map<String, String> {
        val out = HashMap<String, String>()
        val re = Regex("^(?:#|//)\\s*([A-Za-z-]+)\\s*[:=]\\s*(.+)$")
        text.lineSequence().take(40).forEach { line ->
            re.find(line.trim())?.let { m -> out.putIfAbsent(m.groupValues[1].lowercase(), m.groupValues[2].trim()) }
        }
        return out
    }

    private fun parseInfo(meta: Map<String, String>): SubscriptionInfo? {
        fun text(key: String) = meta[key]?.let(::decodeValue)?.takeIf { it.isNotBlank() }
        var used: Long? = null
        var total: Long? = null
        var expire: Long? = null
        meta["subscription-userinfo"]?.split(';')?.forEach { part ->
            val k = part.substringBefore('=').trim().lowercase()
            val v = part.substringAfter('=', "").trim().toDoubleOrNull()?.toLong() ?: return@forEach
            when (k) {
                "upload" -> used = (used ?: 0L) + v
                "download" -> used = (used ?: 0L) + v
                "total" -> total = v
                "expire" -> expire = v
            }
        }
        fun url(key: String) = text(key)?.takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) }
        val info = SubscriptionInfo(
            usedBytes = used,
            totalBytes = total,
            expireAt = expire,
            title = text("profile-title"),
            announce = text("announce"),
            supportUrl = url("support-url"),
            webUrl = url("profile-web-page-url"),
            updateHours = meta["profile-update-interval"]?.trim()?.toIntOrNull()?.coerceIn(1, 24 * 30),
        )
        return info.takeIf {
            it.usedBytes != null || it.totalBytes != null || it.expireAt != null || it.title != null ||
                it.announce != null || it.supportUrl != null || it.webUrl != null || it.updateHours != null
        }
    }

    /** Values can be sent as "base64:...." so they survive non-ASCII text in headers. */
    private fun decodeValue(v: String): String {
        val t = v.trim()
        if (!t.startsWith("base64:", ignoreCase = true)) return t
        return decodeBase64(t.substring(7)) ?: t
    }

    private fun decodeBase64(body: String): String? = runCatching {
        var t = body.filter { !it.isWhitespace() }.replace('-', '+').replace('_', '/')
        while (t.length % 4 != 0) t += "="
        String(Base64.decode(t, Base64.DEFAULT), Charsets.UTF_8)
    }.getOrNull()

    /** Plain list of links, or the same list base64-encoded (the usual subscription format). */
    fun extractLinks(body: String): List<String> {
        fun lines(t: String) = t.lines().map { it.trim() }
            .filter { line -> line.isNotEmpty() && SCHEMES.any { line.startsWith("$it://", ignoreCase = true) } }

        lines(body).takeIf { it.isNotEmpty() }?.let { return it }
        return lines(decodeBase64(body).orEmpty())
    }
}
