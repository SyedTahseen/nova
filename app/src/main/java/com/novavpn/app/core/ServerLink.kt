package com.novavpn.app.core

import android.net.Uri

/**
 * Parsed vless:// or trojan:// share link.
 *
 * Supported: tcp / ws / grpc / httpupgrade transports, with none / tls / reality security.
 */
data class ServerLink(
    val protocol: String,
    val host: String,
    val port: Int,
    /** UUID for VLESS, password for Trojan. */
    val secret: String,
    val network: String,
    val security: String,
    val sni: String?,
    val fingerprint: String?,
    val alpn: String?,
    val flow: String?,
    val publicKey: String?,
    val shortId: String?,
    val spiderX: String?,
    val path: String?,
    val hostHeader: String?,
    val serviceName: String?,
    val allowInsecure: Boolean,
) {
    companion object {
        fun parse(link: String): ServerLink {
            val uri = Uri.parse(link.trim())
            val scheme = uri.scheme?.lowercase() ?: error("Link has no protocol")
            require(scheme == "vless" || scheme == "trojan") { "Unsupported protocol: $scheme" }

            val host = uri.host?.takeIf { it.isNotBlank() } ?: error("Link has no host")
            val port = if (uri.port > 0) uri.port else 443
            val secret = uri.userInfo?.takeIf { it.isNotBlank() } ?: error("Link has no id/password")

            fun q(key: String): String? = uri.getQueryParameter(key)?.takeIf { it.isNotBlank() }

            val network = (q("type") ?: "tcp").lowercase()
            val security = (q("security") ?: if (scheme == "trojan") "tls" else "none").lowercase()

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
            )
        }
    }
}
