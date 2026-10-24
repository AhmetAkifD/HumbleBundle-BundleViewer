package com.ahmet.bundleviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class ListState(
    val bundles: List<BundleSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdated: LocalDateTime? = null,
)

sealed interface DetailState {
    data object Loading : DetailState
    data class Loaded(val detail: BundleDetail) : DetailState
    data class Failed(val message: String) : DetailState
}

class BundleViewModel : ViewModel() {
    private val _list = MutableStateFlow(ListState())
    val list: StateFlow<ListState> = _list.asStateFlow()

    private val _details = MutableStateFlow<Map<String, DetailState>>(emptyMap())
    val details: StateFlow<Map<String, DetailState>> = _details.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_list.value.isLoading) return
        _list.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val bundles = BundleScraper.fetchBundles()
                _list.update { ListState(bundles = bundles, lastUpdated = LocalDateTime.now()) }
                // Contents and prices may have changed, so drop cached details.
                _details.value = emptyMap()
            } catch (e: Exception) {
                _list.update { it.copy(isLoading = false, error = e.toUserMessage()) }
            }
        }
    }

    fun loadDetail(bundle: BundleSummary, force: Boolean = false) {
        val current = _details.value[bundle.url]
        if (!force && (current is DetailState.Loaded || current is DetailState.Loading)) return
        _details.update { it + (bundle.url to DetailState.Loading) }
        viewModelScope.launch {
            val state = try {
                DetailState.Loaded(BundleScraper.fetchDetail(bundle.url))
            } catch (e: Exception) {
                DetailState.Failed(e.toUserMessage())
            }
            _details.update { it + (bundle.url to state) }
        }
    }

    private fun Exception.toUserMessage() = when (this) {
        is java.net.UnknownHostException -> "İnternet bağlantısı yok gibi görünüyor."
        is java.net.SocketTimeoutException -> "Sunucu zamanında yanıt vermedi."
        else -> message ?: "Bilinmeyen bir hata oluştu."
    }
}
