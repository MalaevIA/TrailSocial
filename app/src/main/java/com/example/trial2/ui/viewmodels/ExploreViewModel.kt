package com.trail2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExploreUiState(
    val query: String = "",
    val routes: List<TrailRoute> = emptyList(),
    val users: List<User> = emptyList(),
    val regions: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSearching: Boolean = false
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val routeRepository: RouteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
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
            when (val result = routeRepository.getRoutes()) {
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
            _uiState.update { it.copy(isLoading = true, query = region) }
            when (val result = routeRepository.getRoutes(region = region)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(routes = result.data.items, isLoading = false)
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
