package com.novavpn.app.data

data class Region(
    val id: String,
    val city: String,
    val country: String,
    val code: String,
    val lat: Double,
    val lon: Double,
    val premium: Boolean,
    /** vless:// or trojan:// share link for this location's server. */
    val link: String,
    /** True for servers the user added themselves (stored on device, can be deleted). */
    val custom: Boolean = false,
    /** Where the server came from: a subscription name, or "My servers" for hand-added ones. */
    val group: String = "",
    /** Id of the subscription that owns this server, if any. */
    val subId: String? = null,
) {
    /** Protocol shown in grouping/sorting, derived from the share link. */
    val protocolName: String
        get() = when (link.substringBefore("://").lowercase()) {
            "vless" -> "VLESS"
            "vmess" -> "VMess"
            "trojan" -> "Trojan"
            "ss" -> "Shadowsocks"
            "socks", "socks4", "socks5", "socks5h" -> "SOCKS"
            "wireguard", "wg" -> "WireGuard"
            else -> "Other"
        }

    /** "Germany" for a proxy named Germany; "Frankfurt 01, Germany" when the name adds detail. */
    val label: String
        get() = when {
            country.isBlank() -> city
            city.contains(country, ignoreCase = true) -> city
            else -> "$city, $country"
        }
    val flag: String get() = flagEmoji(code)
}

/** ISO-3166 alpha-2 code -> flag emoji (regional indicator symbols). */
fun flagEmoji(code: String): String {
    if (code.length != 2) return "🏳"
    val base = 0x1F1E6 - 'A'.code
    val first = base + code[0].uppercaseChar().code
    val second = base + code[1].uppercaseChar().code
    return String(Character.toChars(first)) + String(Character.toChars(second))
}

/** A subscription URL the user added; its servers are tagged with [Region.subId]. */
data class Subscription(
    val id: String,
    val name: String,
    val url: String,
    val updatedAt: Long,
    /** What the provider told us about the plan (traffic, expiry, ...). Null if it sent nothing. */
    val info: SubscriptionInfo? = null,
)

/**
 * Plan details sent by the subscription server (Subscription-Userinfo, Profile-Title, ... headers
 * or "#key: value" comment lines inside the list).
 */
data class SubscriptionInfo(
    /** Bytes used so far (upload + download). */
    val usedBytes: Long? = null,
    /** Traffic limit in bytes; 0 = unlimited. */
    val totalBytes: Long? = null,
    /** Expiry as Unix seconds; 0 = never expires. */
    val expireAt: Long? = null,
    val title: String? = null,
    val announce: String? = null,
    val supportUrl: String? = null,
    val webUrl: String? = null,
    /** How often the provider wants the list refreshed, in hours. */
    val updateHours: Int? = null,
) {
    /** True when there is something worth showing on a card. */
    val hasPlanDetails: Boolean
        get() = usedBytes != null || totalBytes != null || expireAt != null || !announce.isNullOrBlank()
}
