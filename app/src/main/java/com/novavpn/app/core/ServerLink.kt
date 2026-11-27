package com.novavpn.app.core

import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.net.URLDecoder

/**
 * A parsed share link. [protocol] is the Xray outbound protocol name:
 * vless | vmess | trojan | shadowsocks | socks | wireguard.
 *
 * Accepted link schemes: vless://, vmess:// (base64-JSON and URI style), trojan://, ss://
 * (SIP002 and legacy), socks:// / socks5://, wireguard:// / wg://.
 * Transports: tcp, ws, grpc, httpupgrade, xhttp, kcp. Security: none / tls / reality.
 */
data class ServerLink(
    val protocol: String,
    val host: String,
    val port: Int,
    /** UUID for VLESS/VMess, password for Trojan/Shadowsocks/SOCKS, private key for WireGuard. */
    val secret: String,
    val network: String = "tcp",
    val security: String = "none",
    val sni: String? = null,
    val fingerprint: String? = null,
    val alpn: String? = null,
    val flow: String? = null,
    val publicKey: String? = null,
    val shortId: String? = null,
    val spiderX: String? = null,
    val path: String? = null,
    val hostHeader: String? = null,
    val serviceName: String? = null,
    val allowInsecure: Boolean = false,
    /** Display name from the link (#fragment / vmess "ps"). */
    val name: String? = null,
    /** Shadowsocks cipher, or VMess security. */
    val method: String? = null,
    /** SOCKS username. */
    val username: String? = null,
    val alterId: Int = 0,
    val encryption: String = "none",
    val headerType: String? = null,
    val mode: String? = null,
    val seed: String? = null,
    // WireGuard
    val localAddress: String? = null,
    val presharedKey: String? = null,
    val mtu: Int? = null,
    val reserved: String? = null,
) {
    /** Human-friendly protocol name for the UI. */
    val protocolLabel: String
        get() = when (protocol) {
            "vless" -> "VLESS"
            "vmess" -> "VMess"
            "trojan" -> "Trojan"
            "shadowsocks" -> "Shadowsocks"
            "socks" -> "SOCKS"
            "wireguard" -> "WireGuard"
            else -> protocol
        }

    /** True when the server is reached over UDP only, so a TCP probe is meaningless. */
    val isUdpOnly: Boolean get() = protocol == "wireguard" || network == "kcp"

    companion object {
        const val SUPPORTED = "vless://, vmess://, trojan://, ss://, socks5://, wireguard://"

        private val TRANSPORTS = setOf("tcp", "ws", "grpc", "httpupgrade", "xhttp", "kcp")

        fun parse(link: String): ServerLink {
            val raw = link.trim()
            val scheme = raw.substringBefore("://", "").lowercase()
            require(scheme.isNotEmpty() && "://" in raw) { "Link has no protocol (expected $SUPPORTED)" }
            return when (scheme) {
                "vless", "trojan" -> parseUri(raw, scheme)
                "vmess" -> parseVmess(raw)
                "ss" -> parseShadowsocks(raw)
                "socks", "socks5", "socks5h", "socks4" -> parseSocks(raw)
                "wireguard", "wg" -> parseWireGuard(raw)
                else -> error("Unsupported protocol: $scheme (supported: $SUPPORTED)")
            }
        }

        // --- vless:// and trojan:// (and URI-style vmess) --------------------------------------

        private fun parseUri(raw: String, scheme: String): ServerLink {
            val uri = Uri.parse(raw)
            val host = cleanHost(uri.host) ?: error("Link has no host")
            val port = if (uri.port > 0) uri.port else 443
            val secret = uri.userInfo?.takeIf { it.isNotBlank() } ?: error("Link has no id/password")
            fun q(key: String): String? = uri.getQueryParameter(key)?.takeIf { it.isNotBlank() }

            val network = normalizeNetwork(q("type"))
            val security = (q("security") ?: if (scheme == "trojan") "tls" else "none").lowercase()
            if (security == "reality") {
                require(q("pbk") != null) { "Reality link is missing the public key (pbk)" }
            }

            return ServerLink(
                protocol = scheme,
                host = host,
                port = port,
                secret = secret,
                network = network,
                security = security,
                sni = q("sni") ?: q("peer"),
                fingerprint = q("fp"),
                alpn = q("alpn"),
                flow = q("flow"),
                publicKey = q("pbk"),
                shortId = q("sid"),
                spiderX = q("spx"),
                path = q("path"),
                hostHeader = q("host"),
                serviceName = q("serviceName"),
                allowInsecure = q("allowInsecure") == "1" || q("insecure") == "1",
                name = uri.fragment?.takeIf { it.isNotBlank() },
                method = q("encryption")?.takeIf { scheme == "vmess" },
                encryption = if (scheme == "vless") (q("encryption") ?: "none") else "none",
                headerType = q("headerType"),
                mode = q("mode"),
                seed = q("seed"),
            )
        }

        // --- vmess:// ---------------------------------------------------------------------------

        private fun parseVmess(raw: String): ServerLink {
            val body = raw.substringAfter("://")
            // Newer URI style: vmess://uuid@host:port?...
            if ('@' in body.substringBefore('?').substringBefore('#')) return parseUri(raw, "vmess")

            val json = runCatching {
                JSONObject(b64(body.substringBefore('#')))
            }.getOrElse { error("Invalid vmess link (cannot decode)") }

            fun s(key: String): String? = json.optString(key, "").takeIf { it.isNotBlank() }

            val host = s("add") ?: error("Link has no host")
            val port = s("port")?.toIntOrNull() ?: 443
            val id = s("id") ?: error("Link has no id")
            val network = normalizeNetwork(s("net"))
            val tls = s("tls")?.lowercase()
            val security = when (tls) { "tls" -> "tls"; "reality" -> "reality"; else -> "none" }
            val netPath = s("path")

            return ServerLink(
                protocol = "vmess",
                host = cleanHost(host)!!,
                port = port,
                secret = id,
                network = network,
                security = security,
                sni = s("sni"),
                fingerprint = s("fp"),
                alpn = s("alpn"),
                path = if (network == "grpc") null else netPath,
                serviceName = if (network == "grpc") netPath else null,
                hostHeader = s("host"),
                allowInsecure = s("allowInsecure") == "1" || json.optBoolean("allowInsecure", false),
                name = s("ps") ?: body.substringAfter('#', "").takeIf { it.isNotBlank() },
                method = s("scy") ?: "auto",
                alterId = s("aid")?.toIntOrNull() ?: 0,
                headerType = s("type"),
                mode = if (network == "grpc") s("type") else null,
            )
        }

        // --- ss:// ------------------------------------------------------------------------------

        private fun parseShadowsocks(raw: String): ServerLink {
            var body = raw.substringAfter("://")
            val name = body.substringAfter('#', "").takeIf { it.isNotBlank() }?.let(::urlDecode)
            body = body.substringBefore('#')
            val query = body.substringAfter('?', "")
            body = body.substringBefore('?')
            require(!query.contains("plugin=")) { "Shadowsocks plugins (obfs, v2ray-plugin) are not supported" }

            val creds: String
            val hostPort: String
            if ('@' in body) {
                val userInfo = body.substringBeforeLast('@')
                hostPort = body.substringAfterLast('@')
                creds = if (':' in userInfo) urlDecode(userInfo) else b64(userInfo)
            } else {
                val decoded = runCatching { b64(body) }.getOrElse { error("Invalid ss link (cannot decode)") }
                creds = decoded.substringBeforeLast('@')
                hostPort = decoded.substringAfterLast('@', "")
            }
            require(':' in creds) { "ss link is missing cipher or password" }
            val method = creds.substringBefore(':')
            val password = creds.substringAfter(':')
            val (host, port) = splitHostPort(hostPort, 8388)

            return ServerLink(
                protocol = "shadowsocks",
                host = host,
                port = port,
                secret = password,
                method = method,
                name = name,
            )
        }

        // --- socks:// ---------------------------------------------------------------------------

        private fun parseSocks(raw: String): ServerLink {
            val uri = Uri.parse(raw)
            val host = cleanHost(uri.host) ?: error("Link has no host")
            val port = if (uri.port > 0) uri.port else 1080
            var user: String? = null
            var pass = ""
            uri.userInfo?.takeIf { it.isNotBlank() }?.let { info ->
                val creds = if (':' in info) info else runCatching { b64(info) }.getOrDefault(info)
                user = creds.substringBefore(':')
                pass = creds.substringAfter(':', "")
            }
            return ServerLink(
                protocol = "socks",
                host = host,
                port = port,
                secret = pass,
                username = user,
                name = uri.fragment?.takeIf { it.isNotBlank() },
            )
        }

        // --- wireguard:// -----------------------------------------------------------------------

        private fun parseWireGuard(raw: String): ServerLink {
            val uri = Uri.parse(raw)
            val host = cleanHost(uri.host) ?: error("Link has no host")
            val port = if (uri.port > 0) uri.port else 51820
            val key = uri.userInfo?.takeIf { it.isNotBlank() } ?: error("Link has no private key")
            fun q(vararg keys: String): String? =
                keys.firstNotNullOfOrNull { uri.getQueryParameter(it)?.takeIf { v -> v.isNotBlank() } }

            val peer = q("publickey", "publicKey", "peer_public_key") ?: error("WireGuard link is missing the peer public key")
            return ServerLink(
                protocol = "wireguard",
                host = host,
                port = port,
                secret = key,
                publicKey = peer,
                presharedKey = q("presharedkey", "psk"),
                localAddress = q("address", "ip"),
                mtu = q("mtu")?.toIntOrNull(),
                reserved = q("reserved"),
                name = uri.fragment?.takeIf { it.isNotBlank() },
            )
        }

        // --- helpers ----------------------------------------------------------------------------

        private fun normalizeNetwork(t: String?): String {
            val n = when (val v = (t ?: "tcp").lowercase()) {
                "raw" -> "tcp"
                "splithttp" -> "xhttp"
                "mkcp" -> "kcp"
                else -> v
            }
            require(n in TRANSPORTS) { "Unsupported transport: $n" }
            return n
        }

        private fun cleanHost(h: String?): String? =
            h?.trim()?.removePrefix("[")?.removeSuffix("]")?.takeIf { it.isNotBlank() }

        private fun splitHostPort(hp: String, defaultPort: Int): Pair<String, Int> {
            val s = hp.trim()
            require(s.isNotEmpty()) { "Link has no host" }
            if (s.startsWith("[")) {
                val end = s.indexOf(']')
                require(end > 0) { "Bad IPv6 address" }
                val port = s.substring(end + 1).removePrefix(":").toIntOrNull() ?: defaultPort
                return s.substring(1, end) to port
            }
            val i = s.lastIndexOf(':')
            return if (i > 0) s.substring(0, i) to (s.substring(i + 1).toIntOrNull() ?: defaultPort) else s to defaultPort
        }

        private fun urlDecode(s: String) = runCatching { URLDecoder.decode(s, "UTF-8") }.getOrDefault(s)

        /** Base64 that tolerates URL-safe alphabet and missing padding. */
        private fun b64(s: String): String {
            var t = s.trim().replace('-', '+').replace('_', '/')
            while (t.length % 4 != 0) t += "="
            return String(Base64.decode(t, Base64.DEFAULT), Charsets.UTF_8)
        }
    }
}
