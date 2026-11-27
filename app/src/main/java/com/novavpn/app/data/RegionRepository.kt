package com.novavpn.app.data

import android.content.Context
import com.novavpn.app.core.Countries
import com.novavpn.app.core.ServerLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket

object RegionRepository {

    /** Reads the server list bundled in assets/regions.json. */
    fun load(context: Context): List<Region> {
        val text = runCatching {
            context.assets.open("regions.json").bufferedReader().use { it.readText() }
        }.getOrDefault("[]")
        val arr = JSONArray(text)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Region(
                id = o.getString("id"),
                city = o.getString("city"),
                country = o.getString("country"),
                code = o.getString("code"),
                lat = o.getDouble("lat"),
                lon = o.getDouble("lon"),
                premium = o.optBoolean("premium", false),
                link = o.getString("link"),
            )
        }
    }

    private const val PREFS = "nova"
    private const val KEY_CUSTOM = "custom_regions"

    /** Servers the user added from the Regions screen. */
    fun loadCustom(context: Context): List<Region> {
        val text = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(text)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                val link = o.getString("link")
                var city = o.getString("city")
                var country = o.getString("country")
                var code = o.optString("code", "")
                var lat = o.optDouble("lat", 0.0)
                var lon = o.optDouble("lon", 0.0)
                if (code.isBlank()) {
                    // Servers saved by an older version had no country: work it out from the name/host.
                    val host = runCatching { ServerLink.parse(link).host }.getOrNull()
                    val c = Countries.detect(city, host)
                    city = Countries.cleanName(city).ifBlank { city }
                    country = c?.name ?: ""
                    code = c?.code ?: ""
                    lat = c?.lat ?: lat
                    lon = c?.lon ?: lon
                }
                Region(
                    id = o.getString("id"),
                    city = city,
                    country = country,
                    code = code,
                    lat = lat,
                    lon = lon,
                    premium = false,
                    link = link,
                    custom = true,
                    group = o.optString("group", "").ifBlank { MANUAL_GROUP },
                    subId = o.optString("subId", "").takeIf { it.isNotBlank() },
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveCustom(context: Context, regions: List<Region>) {
        val arr = JSONArray()
        regions.filter { it.custom }.forEach { r ->
            arr.put(
                JSONObject()
                    .put("id", r.id).put("city", r.city).put("country", r.country)
                    .put("code", r.code).put("lat", r.lat).put("lon", r.lon)
                    .put("link", r.link).put("group", r.group).put("subId", r.subId ?: ""),
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_CUSTOM, arr.toString()).apply()
    }

    const val MANUAL_GROUP = "My servers"
    private const val KEY_SUBS = "subscriptions"

    fun loadSubscriptions(context: Context): List<Subscription> {
        val text = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SUBS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(text)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Subscription(o.getString("id"), o.getString("name"), o.getString("url"), o.optLong("updatedAt", 0L))
            }
        }.getOrDefault(emptyList())
    }

    fun saveSubscriptions(context: Context, subs: List<Subscription>) {
        val arr = JSONArray()
        subs.forEach {
            arr.put(JSONObject().put("id", it.id).put("name", it.name).put("url", it.url).put("updatedAt", it.updatedAt))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_SUBS, arr.toString()).apply()
    }

    /** UDP-only servers (WireGuard, kcp) can't be probed with a TCP connect. */
    fun canProbe(region: Region): Boolean =
        runCatching { !ServerLink.parse(region.link).isUdpOnly }.getOrDefault(true)

    /** TCP connect time to the server in ms, or null if unreachable. */
    suspend fun ping(region: Region): Int? = withContext(Dispatchers.IO) {
        runCatching {
            val server = ServerLink.parse(region.link)
            if (server.isUdpOnly) return@runCatching null // can't probe UDP with a TCP connect
            val start = System.nanoTime()
            Socket().use { it.connect(InetSocketAddress(server.host, server.port), 2500) }
            ((System.nanoTime() - start) / 1_000_000).toInt()
        }.getOrNull()
    }
}
