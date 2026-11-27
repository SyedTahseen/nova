package com.novavpn.app.data

import android.content.Context
import com.novavpn.app.core.ServerLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.InetSocketAddress
import java.net.Socket

object RegionRepository {

    /** Reads the server list bundled in assets/regions.json. */
    fun load(context: Context): List<Region> {
        val text = context.assets.open("regions.json").bufferedReader().use { it.readText() }
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

    /** TCP connect time to the server in ms, or null if unreachable. */
    suspend fun ping(region: Region): Int? = withContext(Dispatchers.IO) {
        runCatching {
            val server = ServerLink.parse(region.link)
            val start = System.nanoTime()
            Socket().use { it.connect(InetSocketAddress(server.host, server.port), 2500) }
            ((System.nanoTime() - start) / 1_000_000).toInt()
        }.getOrNull()
    }
}
