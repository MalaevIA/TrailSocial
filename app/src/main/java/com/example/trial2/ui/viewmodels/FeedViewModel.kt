package com.trail2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trail2.auth.TokenManager
import com.trail2.data.Difficulty
import com.trail2.data.TrailRoute
import com.trail2.data.remote.ApiResult
import com.trail2.data.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val routes: List<TrailRoute> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true,
    val filterDifficulty: Difficulty? = null
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tokenManager.isLoggedIn.collect { loggedIn ->
                if (loggedIn) {
                    loadRoutes()
                } else {
                    _uiState.value = FeedUiState()
                }
            }
        }
    }

    fun loadRoutes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, currentPage = 1) }
            val result = routeRepository.getRoutes(
                page = 1,
                difficulty = _uiState.value.filterDifficulty
            )
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            routes = result.data.items,
                            isLoading = false,
                            hasMorePages = result.data.page < result.data.pages
                        )
                    }
                }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isLoading = false, error = "Нет подключения к интернету") }
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMorePages) return
        viewModelScope.launch {
            val nextPage = _uiState.value.currentPage + 1
            _uiState.update { it.copy(isLoadingMore = true) }
            val result = routeRepository.getRoutes(
                page = nextPage,
                difficulty = _uiState.value.filterDifficulty
            )
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            routes = it.routes + result.data.items,
                            isLoadingMore = false,
                            currentPage = nextPage,
                            hasMorePages = nextPage < result.data.pages
                        )
                    }
                }
                else -> _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun setFilter(difficulty: Difficulty?) {
        _uiState.update { it.copy(filterDifficulty = difficulty) }
        loadRoutes()
    }

    fun toggleLike(routeId: String, currentlyLiked: Boolean) {
        viewModelScope.launch {
            val result = if (currentlyLiked) {
                routeRepository.unlikeRoute(routeId)
            } else {
                routeRepository.likeRoute(routeId)
            }
            if (result is ApiResult.Success) {
                _uiState.update { state ->
                    state.copy(routes = state.routes.map { route ->
                        if (route.id == routeId) route.copy(
                            isLiked = !currentlyLiked,
                            likesCount = route.likesCount + if (currentlyLiked) -1 else 1
                        ) else route
                    })
                }
            }
        }
    }

    fun toggleSave(routeId: String, currentlySaved: Boolean) {
        viewModelScope.launch {
            val result = if (currentlySaved) {
                routeRepository.unsaveRoute(routeId)
            } else {
                routeRepository.saveRoute(routeId)
            }
            if (result is ApiResult.Success) {
                _uiState.update { state ->
                    state.copy(routes = state.routes.map { route ->
                        if (route.id == routeId) route.copy(
                            isSaved = !currentlySaved,
                            savesCount = route.savesCount + if (currentlySaved) -1 else 1
                        ) else route
                    })
                }
            }
        }
    }
}
