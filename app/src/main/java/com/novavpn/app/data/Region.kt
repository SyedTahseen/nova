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
) {
    val label: String get() = "$city, $country"
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
