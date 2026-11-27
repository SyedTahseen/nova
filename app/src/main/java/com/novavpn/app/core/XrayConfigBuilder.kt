package com.novavpn.app.core

import org.json.JSONArray
import org.json.JSONObject

/** Builds the Xray JSON config: one TUN inbound -> proxy outbound for the selected server. */
object XrayConfigBuilder {

    fun build(server: ServerLink, logPath: String? = null): String {
        val root = JSONObject()

        val log = JSONObject().put("loglevel", "warning")
        logPath?.let { log.put("error", it) }
        root.put("log", log)

        root.put(
            "dns",
            JSONObject().put("servers", JSONArray().put("1.1.1.1").put("8.8.8.8")),
        )

        val tunInbound = JSONObject()
            .put("tag", "tun-in")
            .put("protocol", "tun")
            .put(
                "settings",
                JSONObject()
                    .put("name", "xray0")
                    .put("MTU", 1500)
                    .put("userLevel", 0),
            )
            .put(
                "sniffing",
                JSONObject()
                    .put("enabled", true)
                    .put("destOverride", JSONArray().put("http").put("tls").put("quic")),
            )
        root.put("inbounds", JSONArray().put(tunInbound))

        root.put(
            "outbounds",
            JSONArray()
                .put(proxyOutbound(server))
                .put(JSONObject().put("tag", "direct").put("protocol", "freedom"))
                .put(JSONObject().put("tag", "block").put("protocol", "blackhole")),
        )

        root.put(
            "routing",
            JSONObject().put("domainStrategy", "AsIs").put("rules", JSONArray()),
        )

        return root.toString()
    }

    private fun proxyOutbound(s: ServerLink): JSONObject {
        val out = JSONObject().put("tag", "proxy").put("protocol", s.protocol)

        when (s.protocol) {
            "vless" -> {
                val user = JSONObject()
                    .put("id", s.secret)
                    .put("encryption", s.encryption)
                    .put("level", 0)
                // Flow (XTLS Vision) is only valid on raw TCP.
                if (s.network == "tcp") s.flow?.let { user.put("flow", it) }
                out.put("settings", JSONObject().put("vnext", vnext(s, user)))
            }

            "vmess" -> {
                val user = JSONObject()
                    .put("id", s.secret)
                    .put("alterId", s.alterId)
                    .put("security", s.method ?: "auto")
                    .put("level", 0)
                out.put("settings", JSONObject().put("vnext", vnext(s, user)))
            }

            "trojan" -> {
                out.put("settings", JSONObject().put("servers", JSONArray().put(server(s).put("password", s.secret))))
            }

            "shadowsocks" -> {
                out.put(
                    "settings",
                    JSONObject().put(
                        "servers",
                        JSONArray().put(server(s).put("method", s.method ?: "aes-256-gcm").put("password", s.secret)),
                    ),
                )
            }

            "socks" -> {
                val srv = server(s)
                s.username?.let { srv.put("users", JSONArray().put(JSONObject().put("user", it).put("pass", s.secret))) }
                out.put("settings", JSONObject().put("servers", JSONArray().put(srv)))
            }

            "wireguard" -> {
                val peer = JSONObject()
                    .put("publicKey", s.publicKey ?: "")
                    .put("endpoint", if (':' in s.host) "[${s.host}]:${s.port}" else "${s.host}:${s.port}")
                    .put("allowedIPs", JSONArray().put("0.0.0.0/0").put("::/0"))
                s.presharedKey?.let { peer.put("preSharedKey", it) }
                val settings = JSONObject()
                    .put("secretKey", s.secret)
                    .put(
                        "address",
                        JSONArray().also { arr ->
                            (s.localAddress ?: "10.0.0.2/32").split(',').map { it.trim() }.filter { it.isNotEmpty() }
                                .forEach { arr.put(if ('/' in it) it else if (':' in it) "$it/128" else "$it/32") }
                        },
                    )
                    .put("peers", JSONArray().put(peer))
                    .put("mtu", s.mtu ?: 1280)
                s.reserved?.let { r ->
                    val nums = r.split(',').mapNotNull { it.trim().toIntOrNull() }
                    if (nums.size == 3) settings.put("reserved", JSONArray(nums))
                }
                out.put("settings", settings)
                return out
            }

            else -> error("Unsupported protocol: ${s.protocol}")
        }

        // Plain SOCKS / Shadowsocks over raw TCP need no stream settings.
        val plain = (s.protocol == "socks" || s.protocol == "shadowsocks") && s.network == "tcp" && s.security == "none"
        if (!plain) out.put("streamSettings", streamSettings(s))
        return out
    }

    private fun server(s: ServerLink) = JSONObject()
        .put("address", s.host)
        .put("port", s.port)
        .put("level", 0)

    private fun vnext(s: ServerLink, user: JSONObject) = JSONArray().put(
        JSONObject()
            .put("address", s.host)
            .put("port", s.port)
            .put("users", JSONArray().put(user)),
    )

    private fun streamSettings(s: ServerLink): JSONObject {
        val ss = JSONObject().put("network", s.network).put("security", s.security)

        when (s.security) {
            "tls" -> {
                val tls = JSONObject()
                    .put("serverName", s.sni ?: s.host)
                    .put("allowInsecure", s.allowInsecure)
                s.fingerprint?.let { tls.put("fingerprint", it) }
                s.alpn?.let { a ->
                    tls.put("alpn", JSONArray().also { arr -> a.split(',').forEach { arr.put(it.trim()) } })
                }
                ss.put("tlsSettings", tls)
            }

            "reality" -> {
                val reality = JSONObject()
                    .put("serverName", s.sni ?: s.host)
                    .put("fingerprint", s.fingerprint ?: "chrome")
                    .put("publicKey", s.publicKey ?: "")
                    .put("shortId", s.shortId ?: "")
                    .put("spiderX", s.spiderX ?: "")
                ss.put("realitySettings", reality)
            }
        }

        when (s.network) {
            "ws" -> {
                val ws = JSONObject().put("path", s.path ?: "/")
                (s.hostHeader ?: s.sni)?.let { ws.put("headers", JSONObject().put("Host", it)) }
                ss.put("wsSettings", ws)
            }

            "grpc" -> ss.put(
                "grpcSettings",
                JSONObject()
                    .put("serviceName", s.serviceName ?: "")
                    .put("multiMode", s.mode == "multi"),
            )

            "xhttp" -> {
                val x = JSONObject().put("path", s.path ?: "/")
                (s.hostHeader ?: s.sni)?.let { x.put("host", it) }
                s.mode?.let { x.put("mode", it) }
                ss.put("xhttpSettings", x)
            }

            "kcp" -> {
                val k = JSONObject().put("header", JSONObject().put("type", s.headerType ?: "none"))
                s.seed?.let { k.put("seed", it) }
                ss.put("kcpSettings", k)
            }

            "tcp" -> if (s.headerType == "http") {
                val req = JSONObject()
                    .put("path", JSONArray().put(s.path ?: "/"))
                s.hostHeader?.let { req.put("headers", JSONObject().put("Host", JSONArray().put(it))) }
                ss.put(
                    "tcpSettings",
                    JSONObject().put("header", JSONObject().put("type", "http").put("request", req)),
                )
            }

            "httpupgrade" -> {
                val hu = JSONObject().put("path", s.path ?: "/")
                (s.hostHeader ?: s.sni)?.let { hu.put("host", it) }
                ss.put("httpupgradeSettings", hu)
            }
        }

        return ss
    }
}
