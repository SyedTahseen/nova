package com.novavpn.app.core

import org.json.JSONArray
import org.json.JSONObject

/** Builds the Xray JSON config: one TUN inbound -> proxy outbound for the selected server. */
object XrayConfigBuilder {

    fun build(server: ServerLink): String {
        val root = JSONObject()

        root.put("log", JSONObject().put("loglevel", "warning"))

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
                    .put("encryption", "none")
                    .put("level", 0)
                s.flow?.let { user.put("flow", it) }
                out.put(
                    "settings",
                    JSONObject().put(
                        "vnext",
                        JSONArray().put(
                            JSONObject()
                                .put("address", s.host)
                                .put("port", s.port)
                                .put("users", JSONArray().put(user)),
                        ),
                    ),
                )
            }

            "trojan" -> {
                out.put(
                    "settings",
                    JSONObject().put(
                        "servers",
                        JSONArray().put(
                            JSONObject()
                                .put("address", s.host)
                                .put("port", s.port)
                                .put("password", s.secret)
                                .put("level", 0),
                        ),
                    ),
                )
            }
        }

        out.put("streamSettings", streamSettings(s))
        return out
    }

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

            "grpc" -> ss.put("grpcSettings", JSONObject().put("serviceName", s.serviceName ?: ""))

            "httpupgrade" -> {
                val hu = JSONObject().put("path", s.path ?: "/")
                (s.hostHeader ?: s.sni)?.let { hu.put("host", it) }
                ss.put("httpupgradeSettings", hu)
            }
        }

        return ss
    }
}
