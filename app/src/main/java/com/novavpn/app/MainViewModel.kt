package com.novavpn.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novavpn.app.data.Region
import com.novavpn.app.data.RegionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("nova", Context.MODE_PRIVATE)

    val regions: List<Region> = RegionRepository.load(app)

    private val _selectedId = MutableStateFlow(
        prefs.getString(KEY_SELECTED, null)?.takeIf { id -> regions.any { it.id == id } }
            ?: regions.first().id,
    )
    val selectedId: StateFlow<String> = _selectedId.asStateFlow()

    private val _pings = MutableStateFlow<Map<String, Int?>>(emptyMap())
    val pings: StateFlow<Map<String, Int?>> = _pings.asStateFlow()

    init {
        refreshPings()
    }

    fun region(id: String): Region = regions.firstOrNull { it.id == id } ?: regions.first()

    fun select(id: String) {
        _selectedId.value = id
        prefs.edit().putString(KEY_SELECTED, id).apply()
    }

    fun refreshPings() {
        regions.forEach { r ->
            viewModelScope.launch {
                val ms = async { RegionRepository.ping(r) }.await()
                _pings.update { it + (r.id to ms) }
            }
        }
    }

    private companion object {
        const val KEY_SELECTED = "selected_region"
    }
}
