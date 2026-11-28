package com.novavpn.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novavpn.app.core.Countries
import com.novavpn.app.core.ServerLink
import com.novavpn.app.data.Region
import com.novavpn.app.data.RegionRepository
import com.novavpn.app.data.Subscription
import com.novavpn.app.data.SubscriptionClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.URI
import java.util.UUID

enum class SortMode { Default, Ping, Name }

/** How the Regions list is split into sections. */
enum class GroupMode { Source, Country, Protocol, None }

/** Outcome of importing pasted / scanned text. */
data class ImportResult(
    val added: Int,
    val duplicates: Int,
    val error: String?,
    /** Set when the text was a single http(s) URL, i.e. probably a subscription. */
    val subscriptionUrl: String? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("nova", Context.MODE_PRIVATE)

    private val bundled: List<Region> = RegionRepository.load(app)

    private val _subs = MutableStateFlow(RegionRepository.loadSubscriptions(app))
    val subscriptions: StateFlow<List<Subscription>> = _subs.asStateFlow()

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

    private val _sort = MutableStateFlow(
        runCatching { SortMode.valueOf(prefs.getString(KEY_SORT, "") ?: "") }.getOrDefault(SortMode.Default),
    )
    val sort: StateFlow<SortMode> = _sort.asStateFlow()

    private val _group = MutableStateFlow(
        runCatching { GroupMode.valueOf(prefs.getString(KEY_GROUP, "") ?: "") }.getOrDefault(GroupMode.Source),
    )
    val groupMode: StateFlow<GroupMode> = _group.asStateFlow()

    /** True while a subscription is downloading. */
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** Server ids the user connected to, newest first. */
    private val _recent = MutableStateFlow(
        (prefs.getString(KEY_RECENT, "") ?: "").split(',').filter { it.isNotBlank() },
    )

    /**
     * Pills shown on Home (at most [HOME_PILLS]): recently used servers first, then the fastest
     * tested ones, then anything else to fill up. The selected server is always included.
     * Selecting a pill does not reorder the row; only connecting (recents) and new ping results do.
     */
    val homePills: StateFlow<List<Region>> =
        combine(_regions, _recent, _pings, _selectedId) { regions, recent, pings, selId ->
            val byId = regions.associateBy { it.id }
            val recents = recent.mapNotNull { byId[it] }.take(HOME_RECENT)
            val fastest = regions
                .filter { pings[it.id] != null }
                .sortedBy { pings[it.id] }
            val picked = (recents + fastest + regions).distinctBy { it.id }.take(HOME_PILLS).toMutableList()
            val sel = selId?.let { byId[it] }
            if (sel != null && picked.none { it.id == sel.id }) {
                if (picked.size >= HOME_PILLS) picked[picked.lastIndex] = sel else picked += sel
            }
            picked.toList()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Remember [id] as recently used (called when the user connects). */
    fun markRecent(id: String) {
        val next = (listOf(id) + _recent.value.filter { it != id }).take(HOME_RECENT_KEEP)
        _recent.value = next
        prefs.edit().putString(KEY_RECENT, next.joinToString(",")).apply()
    }

    // Subscriptions can hold hundreds of servers; don't open hundreds of sockets at once.
    private val pingGate = Semaphore(16)

    init {
        refreshPings()
    }

    fun region(id: String?): Region? =
        _regions.value.firstOrNull { it.id == id } ?: _regions.value.firstOrNull()

    fun select(id: String?) {
        _selectedId.value = id
        prefs.edit().apply { if (id == null) remove(KEY_SELECTED) else putString(KEY_SELECTED, id) }.apply()
    }

    fun setSort(mode: SortMode) {
        _sort.value = mode
        prefs.edit().putString(KEY_SORT, mode.name).apply()
    }

    fun setGroupMode(mode: GroupMode) {
        _group.value = mode
        prefs.edit().putString(KEY_GROUP, mode.name).apply()
    }

    // --- testing ------------------------------------------------------------------------------

    fun refreshPings() {
        _pings.value = emptyMap()
        _regions.value.forEach { pingOne(it) }
    }

    private fun pingOne(r: Region) {
        if (!RegionRepository.canProbe(r)) return
        viewModelScope.launch {
            val ms = pingGate.withPermit { RegionRepository.ping(r) }
            _pings.update { it + (r.id to ms) }
        }
    }

    // --- adding servers -----------------------------------------------------------------------

    /**
     * Adds one server from a share link. [name] overrides the name in the link.
     * @return null on success, otherwise a short error message.
     */
    fun addServer(rawLink: String, name: String): String? {
        val r = addLinks(listOf(rawLink.trim()), name, RegionRepository.MANUAL_GROUP, null)
        return when {
            r.added > 0 -> null
            r.duplicates > 0 -> "This server is already in your list"
            else -> r.error ?: "Invalid link"
        }
    }

    /** Imports pasted or scanned text: one or more share links, a base64 blob, or a subscription URL. */
    fun importText(text: String): ImportResult {
        val t = text.trim()
        if (t.isEmpty()) return ImportResult(0, 0, "Nothing to import")
        if (Regex("^https?://\\S+$", RegexOption.IGNORE_CASE).matches(t)) {
            return ImportResult(0, 0, null, subscriptionUrl = t)
        }
        val raw = t.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val links = SubscriptionClient.extractLinks(t).ifEmpty { raw }
        return addLinks(links, "", RegionRepository.MANUAL_GROUP, null)
    }

    private fun addLinks(links: List<String>, name: String, group: String, subId: String?): ImportResult {
        var dup = 0
        var error: String? = null
        val fresh = mutableListOf<Region>()
        links.forEach { link ->
            if ((_regions.value + fresh).any { sameServer(it.link, link) }) {
                dup++
                return@forEach
            }
            try {
                fresh += makeRegion(link, if (links.size == 1) name else "", group, subId, null)
            } catch (e: Exception) {
                error = e.message ?: "Invalid link"
            }
        }
        if (fresh.isNotEmpty()) {
            _regions.update { it + fresh }
            persist()
            if (_selectedId.value == null) select(fresh.first().id)
            fresh.forEach(::pingOne)
        }
        return ImportResult(fresh.size, dup, if (fresh.isEmpty() && dup == 0) error else null)
    }

    /** Builds a Region from a link. Throws with a readable message if the link is invalid. */
    private fun makeRegion(link: String, name: String, group: String, subId: String?, id: String?): Region {
        val parsed = ServerLink.parse(link)
        val title = name.trim().ifBlank { parsed.name?.trim().orEmpty() }.ifBlank { parsed.host }
        // Flag + map position come from the proxy name ("Germany", "🇩🇪 Frankfurt", "DE-01") or its host.
        val country = Countries.detect(title, parsed.host)
        return Region(
            id = id ?: "custom-" + UUID.randomUUID().toString().take(8),
            city = Countries.cleanName(title).ifBlank { title },
            country = country?.name ?: "",
            code = country?.code ?: "",
            lat = country?.lat ?: 0.0,
            lon = country?.lon ?: 0.0,
            premium = false,
            link = link.trim(),
            custom = true,
            group = group,
            subId = subId,
        )
    }

    // --- subscriptions ------------------------------------------------------------------------

    /** Downloads [url] and adds (or updates) its servers. [onResult] gets null on success, else an error. */
    fun addSubscription(name: String, url: String, onResult: (String?) -> Unit) {
        val u = url.trim()
        if (!Regex("^https?://\\S+$", RegexOption.IGNORE_CASE).matches(u)) {
            onResult("Enter a valid http:// or https:// URL")
            return
        }
        viewModelScope.launch {
            _busy.value = true
            val error = try {
                val links = SubscriptionClient.fetch(u)
                val existing = _subs.value.firstOrNull { it.url == u }
                val sub = Subscription(
                    id = existing?.id ?: UUID.randomUUID().toString().take(8),
                    name = name.trim().ifBlank { existing?.name ?: hostOf(u) },
                    url = u,
                    updatedAt = System.currentTimeMillis(),
                )
                applySubscription(sub, links)
                null
            } catch (e: Exception) {
                e.message ?: "Couldn't download the subscription"
            } finally {
                _busy.value = false
            }
            onResult(error)
        }
    }

    /** Re-downloads every subscription. [onResult] gets a one-line summary. */
    fun refreshSubscriptions(onResult: (String) -> Unit) {
        val subs = _subs.value
        if (subs.isEmpty()) { onResult("No subscriptions to update"); return }
        viewModelScope.launch {
            _busy.value = true
            var ok = 0
            var failed = 0
            subs.forEach { sub ->
                try {
                    applySubscription(sub.copy(updatedAt = System.currentTimeMillis()), SubscriptionClient.fetch(sub.url))
                    ok++
                } catch (e: Exception) {
                    failed++
                }
            }
            _busy.value = false
            onResult(
                if (failed == 0) "Updated $ok subscription${if (ok > 1) "s" else ""}"
                else "Updated $ok, $failed failed",
            )
        }
    }

    /** Replaces all servers of [sub] with [links], keeping ids (and so selection) for unchanged links. */
    private fun applySubscription(sub: Subscription, links: List<String>) {
        val old = _regions.value.filter { it.subId == sub.id }
        val oldByKey = old.associateBy { key(it.link) }
        val seen = HashSet<String>()
        val fresh = links.mapNotNull { link ->
            val k = key(link)
            if (!seen.add(k)) null
            else runCatching { makeRegion(link, "", sub.name, sub.id, oldByKey[k]?.id) }.getOrNull()
        }
        require(fresh.isNotEmpty()) { "No supported servers found in this subscription" }

        val freshIds = fresh.map { it.id }.toSet()
        _regions.update { list -> list.filter { it.subId != sub.id } + fresh }
        _subs.update { list -> if (list.any { it.id == sub.id }) list.map { if (it.id == sub.id) sub else it } else list + sub }
        val removedIds = old.map { it.id }.toSet() - freshIds
        _pings.update { p -> p.filterKeys { it !in removedIds } }
        persist()

        val sel = _selectedId.value
        if (sel == null) select(fresh.first().id)
        else if (old.any { it.id == sel } && sel !in freshIds) select(_regions.value.firstOrNull()?.id)
        fresh.filter { it.id !in _pings.value }.forEach(::pingOne)
    }

    fun deleteSubscriptions() {
        _subs.value = emptyList()
        deleteWhere { it.subId != null }
    }

    // --- deleting -----------------------------------------------------------------------------

    /** Servers whose last test failed (UDP-only servers are never tested, so never listed). */
    fun unreachableIds(): Set<String> {
        val p = _pings.value
        return _regions.value.filter { it.custom && p.containsKey(it.id) && p[it.id] == null }.map { it.id }.toSet()
    }

    /** Extra copies of the same host + port + credentials (the first one is kept). */
    fun duplicateIds(): Set<String> {
        val seen = HashSet<String>()
        val dups = mutableSetOf<String>()
        _regions.value.forEach { r ->
            val k = runCatching { ServerLink.parse(r.link).let { "${it.protocol}|${it.host}|${it.port}|${it.secret}" } }
                .getOrDefault(r.link)
            if (!seen.add(k) && r.custom) dups += r.id
        }
        return dups
    }

    fun deleteIds(ids: Set<String>) = deleteWhere { it.id in ids }

    fun deleteServer(id: String) = deleteWhere { it.id == id }

    fun deleteManual() = deleteWhere { it.custom && it.subId == null }

    fun deleteAllCustom() {
        _subs.value = emptyList()
        deleteWhere { it.custom }
    }

    private fun deleteWhere(predicate: (Region) -> Boolean) {
        val ids = _regions.value.filter { it.custom && predicate(it) }.map { it.id }.toSet()
        if (ids.isNotEmpty()) {
            _regions.update { list -> list.filter { it.id !in ids } }
            _pings.update { it - ids }
            if (_selectedId.value in ids) select(_regions.value.firstOrNull()?.id)
        }
        persist()
    }

    private fun persist() {
        RegionRepository.saveCustom(getApplication(), _regions.value)
        RegionRepository.saveSubscriptions(getApplication(), _subs.value)
    }

    private fun key(link: String) = link.substringBefore('#').trim()

    private fun sameServer(a: String, b: String) = key(a) == key(b)

    private fun hostOf(url: String) = runCatching { URI(url).host }.getOrNull()?.removePrefix("www.") ?: "Subscription"

    private companion object {
        const val KEY_SELECTED = "selected_region"
        const val KEY_SORT = "sort_mode"
        const val KEY_GROUP = "group_mode"
        const val KEY_RECENT = "recent_regions"
        const val HOME_PILLS = 10
        const val HOME_RECENT = 5
        const val HOME_RECENT_KEEP = 20
    }
}
