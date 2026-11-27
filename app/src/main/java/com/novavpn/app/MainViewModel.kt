package com.novavpn.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novavpn.app.core.Countries
import com.novavpn.app.core.ServerLink
import com.novavpn.app.data.Region
import com.novavpn.app.data.RegionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class SortMode { Default, Ping, Name }

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("nova", Context.MODE_PRIVATE)

    private val bundled: List<Region> = RegionRepository.load(app)

    private val _regions = MutableStateFlow(bundled + RegionRepository.loadCustom(app))
    val regions: StateFlow<List<Region>> = _regions.asStateFlow()

    private val _selectedId = MutableStateFlow(
        prefs.getString(KEY_SELECTED, null)?.takeIf { id -> _regions.value.any { it.id == id } }
            ?: _regions.value.firstOrNull()?.id,
    )
    /** Null while the server list is empty. */
    val selectedId: StateFlow<String?> = _selectedId.asStateFlow()

    private val _pings = MutableStateFlow<Map<String, Int?>>(emptyMap())
    val pings: StateFlow<Map<String, Int?>> = _pings.asStateFlow()

    private val _sort = MutableStateFlow(SortMode.Default)
    val sort: StateFlow<SortMode> = _sort.asStateFlow()

    init {
        refreshPings()
    }

    fun region(id: String?): Region? =
        _regions.value.firstOrNull { it.id == id } ?: _regions.value.firstOrNull()

    fun select(id: String?) {
        _selectedId.value = id
        prefs.edit().apply { if (id == null) remove(KEY_SELECTED) else putString(KEY_SELECTED, id) }.apply()
    }

    fun setSort(mode: SortMode) { _sort.value = mode }

    fun refreshPings() {
        _regions.value.forEach { pingOne(it) }
    }

    private fun pingOne(r: Region) {
        viewModelScope.launch {
            val ms = async { RegionRepository.ping(r) }.await()
            _pings.update { it + (r.id to ms) }
        }
    }

    /**
     * Adds a server from a vless:// or trojan:// share link.
     * @return null on success, otherwise a short error message to show the user.
     */
    fun addServer(rawLink: String, name: String): String? {
        val link = rawLink.trim()
        val parsed = runCatching { ServerLink.parse(link) }
            .getOrElse { return it.message ?: "Invalid link" }

        if (_regions.value.any { sameServer(it.link, link) }) return "This server is already in your list"

        val title = name.trim().ifBlank { parsed.name?.trim().orEmpty() }.ifBlank { parsed.host }
        // Flag + map position come from the proxy name ("Germany", "🇩🇪 Frankfurt", "DE-01") or its host.
        val country = Countries.detect(title, parsed.host)

        val region = Region(
            id = "custom-" + UUID.randomUUID().toString().take(8),
            city = Countries.cleanName(title).ifBlank { title },
            country = country?.name ?: "",
            code = country?.code ?: "",
            lat = country?.lat ?: 0.0,
            lon = country?.lon ?: 0.0,
            premium = false,
            link = link,
            custom = true,
        )
        _regions.update { it + region }
        persistCustom()
        if (_selectedId.value == null) select(region.id)
        pingOne(region)
        return null
    }

    fun deleteServer(id: String) {
        val target = _regions.value.firstOrNull { it.id == id && it.custom } ?: return
        _regions.update { list -> list.filter { it.id != target.id } }
        _pings.update { it - target.id }
        if (_selectedId.value == target.id) select(_regions.value.firstOrNull()?.id)
        persistCustom()
    }

    fun deleteAllCustom() {
        val ids = _regions.value.filter { it.custom }.map { it.id }.toSet()
        if (ids.isEmpty()) return
        _regions.update { list -> list.filter { !it.custom } }
        _pings.update { it - ids }
        if (_selectedId.value in ids) select(_regions.value.firstOrNull()?.id)
        persistCustom()
    }

    private fun persistCustom() = RegionRepository.saveCustom(getApplication(), _regions.value)

    private fun sameServer(a: String, b: String) =
        a.substringBefore('#').trim() == b.substringBefore('#').trim()

    private companion object {
        const val KEY_SELECTED = "selected_region"
    }
}
