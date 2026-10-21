package dev.gloria.apkextractor.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppListState(
    val apps: List<InstalledApp> = emptyList(),
    val query: String = "",
    val showSystem: Boolean = false,
    val loading: Boolean = true,
)

class AppListViewModel(
    private val repo: AppRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val allApps = MutableStateFlow<List<InstalledApp>?>(null)
    private val query = MutableStateFlow("")
    private val showSystem = MutableStateFlow(false)
    private val _activeExtraction = MutableStateFlow<String?>(null)
    val activeExtraction: StateFlow<String?> = _activeExtraction

    val state: StateFlow<AppListState> = combine(allApps, query, showSystem) { all, q, system ->
        AppListState(
            apps = all.orEmpty()
                .filter { system || !it.isSystem }
                .filter { it.label.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true) }
                .sortedBy { it.label.lowercase() },
            query = q,
            showSystem = system,
            loading = all == null,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppListState())

    init {
        viewModelScope.launch {
            allApps.value = withContext(ioDispatcher) { repo.loadApps() }
        }
    }

    fun setQuery(q: String) {
        query.value = q
    }

    fun setActiveExtraction(workId: String?) {
        _activeExtraction.value = workId
    }

    fun setShowSystem(on: Boolean) {
        showSystem.value = on
    }
}
