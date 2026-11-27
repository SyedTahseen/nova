package com.novavpn.app.core

import android.net.Uri
import android.util.Base64
import org.json.JSONObject

/** Values from the "Manual input" form; [toLink] turns them into a normal share link. */
data class ManualServer(
    val protocol: String = "VLESS",
    val name: String = "",
    val host: String = "",
    val port: String = "443",
    /** UUID (VLESS/VMess) or password (Trojan/Shadowsocks/SOCKS). */
    val secret: String = "",
    val network: String = "tcp",
    val security: String = "none",
    val sni: String = "",
    val path: String = "",
    val publicKey: String = "",
    val shortId: String = "",
    val vision: Boolean = false,
    val cipher: String = "aes-256-gcm",
    val username: String = "",
) {
    fun toLink(): String {
        val h = host.trim()
        require(h.isNotEmpty()) { "Enter the server address" }
        val p = port.trim().toIntOrNull()
        require(p != null && p in 1..65535) { "Enter a valid port (1–65535)" }
        val s = secret.trim()
        val tag = name.trim()
        val hostPart = if (':' in h && !h.startsWith("[")) "[$h]" else h
        val frag = if (tag.isNotEmpty()) "#" + Uri.encode(tag) else ""

        return when (protocol) {
            "VLESS", "Trojan" -> {
                require(s.isNotEmpty()) { if (protocol == "VLESS") "Enter the user ID (UUID)" else "Enter the password" }
                val q = linkedMapOf("type" to network, "security" to security)
                if (security != "none" && sni.isNotBlank()) q["sni"] = sni.trim()
                if (network == "ws" && sni.isNotBlank()) q["host"] = sni.trim()
                if (path.isNotBlank()) q[if (network == "grpc") "serviceName" else "path"] = path.trim()
                if (security == "tls") q["fp"] = "chrome"
                if (security == "reality") {
                    require(publicKey.isNotBlank()) { "Enter the Reality public key" }
                    q["pbk"] = publicKey.trim()
                    if (shortId.isNotBlank()) q["sid"] = shortId.trim()
                    q["fp"] = "chrome"
                }
                if (protocol == "VLESS" && vision && network == "tcp") q["flow"] = "xtls-rprx-vision"
                val query = q.entries.joinToString("&") { "${it.key}=${Uri.encode(it.value)}" }
                "${protocol.lowercase()}://${Uri.encode(s)}@$hostPart:$p?$query$frag"
            }

            "VMess" -> {
                require(s.isNotEmpty()) { "Enter the user ID (UUID)" }
                val json = JSONObject()
                    .put("v", "2").put("ps", tag).put("add", h).put("port", p.toString())
                    .put("id", s).put("aid", "0").put("scy", "auto").put("net", network).put("type", "none")
                    .put("host", if (network == "ws") sni.trim() else "")
                    .put("path", path.trim())
                    .put("tls", if (security == "tls") "tls" else "")
                    .put("sni", sni.trim())
                    .put("fp", if (security == "tls") "chrome" else "")
                "vmess://" + Base64.encodeToString(json.toString().toByteArray(), Base64.NO_WRAP)
            }

            "Shadowsocks" -> {
                require(s.isNotEmpty()) { "Enter the password" }
                val cred = Base64.encodeToString(
                    "$cipher:$s".toByteArray(),
                    Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING,
                )
                "ss://$cred@$hostPart:$p$frag"
            }

            "SOCKS" -> {
                val auth = if (username.isNotBlank()) "${Uri.encode(username.trim())}:${Uri.encode(s)}@" else ""
                "socks5://$auth$hostPart:$p$frag"
            }

            else -> error("Unsupported protocol")
        }
    }
}
