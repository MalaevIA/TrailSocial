package com.trail2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trail2.auth.TokenManager
import com.trail2.data.Difficulty
import com.trail2.data.RegionInfo
import com.trail2.data.TrailRoute
import com.trail2.data.User
import com.trail2.data.remote.ApiResult
import com.trail2.data.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExploreUiState(
    val query: String = "",
    val routes: List<TrailRoute> = emptyList(),
    val users: List<User> = emptyList(),
    val regions: List<RegionInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSearching: Boolean = false,
    val selectedRegion: String? = null,
    val filterDifficulty: Difficulty? = null
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadRegions()
        loadAllRoutes()
        viewModelScope.launch {
            tokenManager.isLoggedIn.drop(1).collect {
                _uiState.update { it.copy(
                    routes = emptyList(),
                    query = "",
                    selectedRegion = null,
                    isSearching = false
                )}
                loadAllRoutes()
            }
        }
    }

    fun refresh() {
        loadRegions()
        loadAllRoutes()
    }

    private fun loadRegions() {
        viewModelScope.launch {
            when (val result = routeRepository.getRegions()) {
                is ApiResult.Success -> _uiState.update { it.copy(regions = result.data) }
                else -> {}
            }
        }
    }

    private fun loadAllRoutes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = routeRepository.getRoutes(difficulty = _uiState.value.filterDifficulty)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(routes = result.data.items, isLoading = false)
                }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isLoading = false, error = "Нет подключения") }
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(isSearching = false, users = emptyList()) }
            loadAllRoutes()
            return
        }
        searchJob = viewModelScope.launch {
            delay(500)
            _uiState.update { it.copy(isSearching = true) }
            when (val result = routeRepository.searchRoutes(query)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(routes = result.data.items, isSearching = false)
                }
                else -> _uiState.update { it.copy(isSearching = false) }
            }
            when (val result = routeRepository.searchUsers(query)) {
                is ApiResult.Success -> _uiState.update { it.copy(users = result.data.items) }
                else -> {}
            }
        }
    }

    fun searchByRegion(region: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedRegion = region) }
            when (val result = routeRepository.getRoutes(region = region, difficulty = _uiState.value.filterDifficulty)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(routes = result.data.items, isLoading = false)
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearRegion() {
        _uiState.update { it.copy(selectedRegion = null) }
        loadAllRoutes()
    }

    fun setFilter(difficulty: Difficulty?) {
        _uiState.update { it.copy(filterDifficulty = difficulty) }
        val region = _uiState.value.selectedRegion
        if (region != null) searchByRegion(region) else loadAllRoutes()
    }
}
