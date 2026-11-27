package com.novavpn.app.core

/**
 * Works out which country a proxy belongs to from its name ("Germany", "🇩🇪 Frankfurt 01", "DE-2",
 * "us-east") or, failing that, from its host, so the app can show the right flag and map position.
 */
object Countries {

    data class Country(
        val code: String,
        val name: String,
        val lat: Double,
        val lon: Double,
        val aliases: List<String> = emptyList(),
    )

    private fun C(code: String, name: String, lat: Double, lon: Double, aliases: List<String>) =
        Country(code, name, lat, lon, aliases)

    val all: List<Country> = listOf(
        C("AF", "Afghanistan", 34.53, 69.17, emptyList()),
        C("AL", "Albania", 41.33, 19.82, listOf("tirana")),
        C("DZ", "Algeria", 36.75, 3.06, listOf("algiers")),
        C("AR", "Argentina", -34.60, -58.38, listOf("buenos aires")),
        C("AM", "Armenia", 40.18, 44.51, listOf("yerevan")),
        C("AU", "Australia", -33.87, 151.21, listOf("sydney", "melbourne", "perth")),
        C("AT", "Austria", 48.21, 16.37, listOf("vienna", "wien")),
        C("AZ", "Azerbaijan", 40.41, 49.87, listOf("baku")),
        C("BH", "Bahrain", 26.23, 50.59, listOf("manama")),
        C("BD", "Bangladesh", 23.81, 90.41, listOf("dhaka")),
        C("BY", "Belarus", 53.90, 27.57, listOf("minsk")),
        C("BE", "Belgium", 50.85, 4.35, listOf("brussels")),
        C("BO", "Bolivia", -16.50, -68.15, listOf("la paz")),
        C("BA", "Bosnia and Herzegovina", 43.86, 18.41, listOf("bosnia", "sarajevo")),
        C("BR", "Brazil", -23.55, -46.63, listOf("brasil", "sao paulo", "rio de janeiro")),
        C("BG", "Bulgaria", 42.70, 23.32, listOf("sofia")),
        C("KH", "Cambodia", 11.56, 104.93, listOf("phnom penh")),
        C("CA", "Canada", 43.65, -79.38, listOf("toronto", "montreal", "vancouver")),
        C("CL", "Chile", -33.45, -70.67, listOf("santiago")),
        C("CN", "China", 39.90, 116.40, listOf("beijing", "shanghai")),
        C("CO", "Colombia", 4.71, -74.07, listOf("bogota")),
        C("CR", "Costa Rica", 9.93, -84.08, emptyList()),
        C("HR", "Croatia", 45.81, 15.98, listOf("zagreb")),
        C("CY", "Cyprus", 35.17, 33.36, listOf("nicosia")),
        C("CZ", "Czechia", 50.08, 14.43, listOf("czech republic", "czech", "prague")),
        C("DK", "Denmark", 55.68, 12.57, listOf("copenhagen")),
        C("DO", "Dominican Republic", 18.49, -69.90, emptyList()),
        C("EC", "Ecuador", -0.18, -78.47, listOf("quito")),
        C("EG", "Egypt", 30.04, 31.24, listOf("cairo")),
        C("EE", "Estonia", 59.44, 24.75, listOf("tallinn")),
        C("ET", "Ethiopia", 9.03, 38.74, listOf("addis ababa")),
        C("FI", "Finland", 60.17, 24.94, listOf("helsinki")),
        C("FR", "France", 48.86, 2.35, listOf("paris", "marseille")),
        C("GE", "Georgia", 41.72, 44.79, listOf("tbilisi")),
        C("DE", "Germany", 50.11, 8.68, listOf("deutschland", "frankfurt", "berlin", "munich", "falkenstein", "nuremberg")),
        C("GH", "Ghana", 5.60, -0.19, listOf("accra")),
        C("GR", "Greece", 37.98, 23.73, listOf("athens")),
        C("GT", "Guatemala", 14.63, -90.51, emptyList()),
        C("HK", "Hong Kong", 22.32, 114.17, listOf("hongkong")),
        C("HU", "Hungary", 47.50, 19.04, listOf("budapest")),
        C("IS", "Iceland", 64.15, -21.94, listOf("reykjavik")),
        C("IN", "India", 19.08, 72.88, listOf("mumbai", "delhi", "bangalore")),
        C("ID", "Indonesia", -6.20, 106.85, listOf("jakarta")),
        C("IR", "Iran", 35.69, 51.39, listOf("tehran")),
        C("IQ", "Iraq", 33.31, 44.36, listOf("baghdad")),
        C("IE", "Ireland", 53.35, -6.26, listOf("dublin")),
        C("IL", "Israel", 32.09, 34.78, listOf("tel aviv")),
        C("IT", "Italy", 45.46, 9.19, listOf("milan", "rome")),
        C("JP", "Japan", 35.68, 139.69, listOf("tokyo", "osaka")),
        C("JO", "Jordan", 31.95, 35.93, listOf("amman")),
        C("KZ", "Kazakhstan", 51.17, 71.43, listOf("astana", "almaty")),
        C("KE", "Kenya", -1.29, 36.82, listOf("nairobi")),
        C("KW", "Kuwait", 29.38, 47.99, emptyList()),
        C("KG", "Kyrgyzstan", 42.87, 74.59, listOf("bishkek")),
        C("LA", "Laos", 17.97, 102.60, listOf("vientiane")),
        C("LV", "Latvia", 56.95, 24.11, listOf("riga")),
        C("LB", "Lebanon", 33.89, 35.50, listOf("beirut")),
        C("LT", "Lithuania", 54.69, 25.28, listOf("vilnius")),
        C("LU", "Luxembourg", 49.61, 6.13, emptyList()),
        C("MO", "Macau", 22.20, 113.54, listOf("macao")),
        C("MY", "Malaysia", 3.14, 101.69, listOf("kuala lumpur")),
        C("MT", "Malta", 35.90, 14.51, listOf("valletta")),
        C("MX", "Mexico", 19.43, -99.13, emptyList()),
        C("MD", "Moldova", 47.01, 28.86, listOf("chisinau")),
        C("MN", "Mongolia", 47.89, 106.91, listOf("ulaanbaatar")),
        C("MA", "Morocco", 33.57, -7.59, listOf("casablanca")),
        C("MM", "Myanmar", 16.87, 96.20, listOf("burma")),
        C("NP", "Nepal", 27.70, 85.32, listOf("kathmandu")),
        C("NL", "Netherlands", 52.37, 4.90, listOf("holland", "amsterdam")),
        C("NZ", "New Zealand", -36.85, 174.76, listOf("auckland")),
        C("NG", "Nigeria", 6.52, 3.38, listOf("lagos")),
        C("MK", "North Macedonia", 41.99, 21.43, listOf("macedonia", "skopje")),
        C("NO", "Norway", 59.91, 10.75, listOf("oslo")),
        C("OM", "Oman", 23.59, 58.41, listOf("muscat")),
        C("PK", "Pakistan", 24.86, 67.00, listOf("karachi")),
        C("PA", "Panama", 8.98, -79.52, emptyList()),
        C("PY", "Paraguay", -25.26, -57.58, emptyList()),
        C("PE", "Peru", -12.05, -77.04, listOf("lima")),
        C("PH", "Philippines", 14.60, 120.98, listOf("manila")),
        C("PL", "Poland", 52.23, 21.01, listOf("warsaw", "warszawa")),
        C("PT", "Portugal", 38.72, -9.14, listOf("lisbon")),
        C("PR", "Puerto Rico", 18.47, -66.10, emptyList()),
        C("QA", "Qatar", 25.29, 51.53, listOf("doha")),
        C("RO", "Romania", 44.43, 26.10, listOf("bucharest")),
        C("RU", "Russia", 55.76, 37.62, listOf("moscow", "russian federation")),
        C("SA", "Saudi Arabia", 24.71, 46.68, listOf("riyadh")),
        C("RS", "Serbia", 44.79, 20.45, listOf("belgrade")),
        C("SG", "Singapore", 1.35, 103.82, emptyList()),
        C("SK", "Slovakia", 48.15, 17.11, listOf("bratislava")),
        C("SI", "Slovenia", 46.06, 14.51, listOf("ljubljana")),
        C("ZA", "South Africa", -26.20, 28.05, listOf("johannesburg")),
        C("KR", "South Korea", 37.57, 126.98, listOf("korea", "seoul")),
        C("ES", "Spain", 40.42, -3.70, listOf("madrid", "barcelona")),
        C("LK", "Sri Lanka", 6.93, 79.86, listOf("colombo")),
        C("SE", "Sweden", 59.33, 18.07, listOf("stockholm")),
        C("CH", "Switzerland", 47.38, 8.54, listOf("zurich", "geneva")),
        C("TW", "Taiwan", 25.03, 121.57, listOf("taipei")),
        C("TH", "Thailand", 13.76, 100.50, listOf("bangkok")),
        C("TN", "Tunisia", 36.81, 10.18, listOf("tunis")),
        C("TR", "Turkey", 41.01, 28.98, listOf("turkiye", "türkiye", "istanbul", "ankara")),
        C("UA", "Ukraine", 50.45, 30.52, listOf("kyiv", "kiev")),
        C("AE", "United Arab Emirates", 25.20, 55.27, listOf("uae", "emirates", "dubai", "abu dhabi")),
        C("GB", "United Kingdom", 51.51, -0.13, listOf("uk", "britain", "great britain", "england", "scotland", "london", "manchester")),
        C("US", "United States", 40.71, -74.01, listOf("usa", "america", "united states of america", "new york", "los angeles", "san jose", "dallas", "miami", "chicago", "seattle", "ashburn", "atlanta", "san francisco")),
        C("UY", "Uruguay", -34.90, -56.16, listOf("montevideo")),
        C("UZ", "Uzbekistan", 41.30, 69.24, listOf("tashkent")),
        C("VE", "Venezuela", 10.48, -66.90, listOf("caracas")),
        C("VN", "Vietnam", 21.03, 105.85, listOf("hanoi", "viet nam")),
    )

    private val byCode = all.associateBy { it.code }

    /** Names/aliases (lower-case, letters+digits only), longest first so "south korea" beats "korea". */
    private val phrases: List<Pair<String, Country>> = all
        .flatMap { c -> (listOf(c.name) + c.aliases).map { norm(it) to c } }
        .sortedByDescending { it.first.length }

    /** Top-level domains that are not really countries when used on proxy hosts. */
    private val ignoredTlds = setOf("co", "me", "tv", "cc", "ai", "io", "ly", "to", "fm", "sh", "ws", "gg")

    fun fromCode(code: String?): Country? = code?.uppercase()?.let { byCode[it] }

    /** Best guess for the country of a proxy called [name] living on [host]. */
    fun detect(name: String?, host: String?): Country? =
        fromFlagEmoji(name) ?: fromName(name) ?: fromCodeToken(name) ?: fromHost(host)

    /** The proxy name without flag emoji and leading separators: "🇩🇪 | Germany" -> "Germany". */
    fun cleanName(name: String?): String {
        if (name == null) return ""
        val sb = StringBuilder()
        var i = 0
        while (i < name.length) {
            val cp = name.codePointAt(i)
            if (cp !in 0x1F1E6..0x1F1FF) sb.appendCodePoint(cp)
            i += Character.charCount(cp)
        }
        return sb.toString().trim().trimStart('|', '-', '–', '—', ':', '·', '•', '/', ' ').trim()
    }

    // --- detection steps ----------------------------------------------------------------------

    private fun fromFlagEmoji(text: String?): Country? {
        if (text == null) return null
        val cps = text.codePoints().toArray()
        for (i in 0 until cps.size - 1) {
            if (cps[i] in 0x1F1E6..0x1F1FF && cps[i + 1] in 0x1F1E6..0x1F1FF) {
                val code = "" + ('A' + (cps[i] - 0x1F1E6)) + ('A' + (cps[i + 1] - 0x1F1E6))
                byCode[code]?.let { return it }
            }
        }
        return null
    }

    private fun fromName(text: String?): Country? {
        if (text.isNullOrBlank()) return null
        val hay = " " + norm(text) + " "
        return phrases.firstOrNull { (p, _) -> hay.contains(" $p ") }?.second
    }

    /** Two-letter code written in capitals as its own word: "DE-01", "[US] East", "NL". */
    private fun fromCodeToken(text: String?): Country? {
        if (text.isNullOrBlank()) return null
        return Regex("(?<![A-Za-z])[A-Z]{2}(?![A-Za-z])").findAll(text)
            .mapNotNull { byCode[it.value] }.firstOrNull()
    }

    private fun fromHost(host: String?): Country? {
        if (host.isNullOrBlank() || host.all { it.isDigit() || it == '.' || it == ':' }) return null
        val h = host.lowercase()
        fromName(h.replace('.', ' '))?.let { return it }
        val labels = h.split('.')
        // "de1.example.com", "us-east.example.net": first label minus digits/suffix
        labels.firstOrNull()?.takeWhile { it.isLetter() }?.takeIf { it.length == 2 }
            ?.let { byCode[it.uppercase()] }?.let { return it }
        val tld = labels.lastOrNull()
        if (tld != null && tld.length == 2 && tld !in ignoredTlds) return byCode[tld.uppercase()]
        return null
    }

    private fun norm(s: String): String =
        s.lowercase().map { if (it.isLetterOrDigit()) it else ' ' }.joinToString("")
            .split(' ').filter { it.isNotEmpty() }.joinToString(" ")
}
