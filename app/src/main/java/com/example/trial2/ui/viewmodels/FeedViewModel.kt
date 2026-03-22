package com.trail2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trail2.auth.TokenManager
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

enum class FeedTab { FEED, RECOMMENDED }

data class FeedTabState(
    val routes: List<TrailRoute> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true
)

data class FeedUiState(
    val selectedTab: FeedTab = FeedTab.RECOMMENDED,
    val feedState: FeedTabState = FeedTabState(),
    val recommendedState: FeedTabState = FeedTabState(),
    val isLoggedIn: Boolean = false
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        loadRecommended()
        viewModelScope.launch {
            tokenManager.isLoggedIn.collect { loggedIn ->
                _uiState.update { it.copy(isLoggedIn = loggedIn) }
                if (loggedIn) {
                    loadFeed()
                } else {
                    _uiState.update { it.copy(feedState = FeedTabState()) }
                }
            }
        }
    }

    fun switchTab(tab: FeedTab) {
        if (_uiState.value.selectedTab == tab) return
        _uiState.update { it.copy(selectedTab = tab) }
        when (tab) {
            FeedTab.FEED -> if (_uiState.value.feedState.routes.isEmpty() && !_uiState.value.feedState.isLoading) loadFeed()
            FeedTab.RECOMMENDED -> if (_uiState.value.recommendedState.routes.isEmpty() && !_uiState.value.recommendedState.isLoading) loadRecommended()
        }
    }

    fun loadFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(feedState = it.feedState.copy(isLoading = true, error = null, currentPage = 1)) }
            when (val result = routeRepository.getFeed(page = 1)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(feedState = it.feedState.copy(
                        routes = result.data.items,
                        isLoading = false,
                        currentPage = 1,
                        hasMorePages = result.data.page < result.data.pages
                    ))
                }
                is ApiResult.Error -> _uiState.update { it.copy(feedState = it.feedState.copy(isLoading = false, error = result.message)) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(feedState = it.feedState.copy(isLoading = false, error = "Нет подключения к интернету")) }
            }
        }
    }

    fun loadMoreFeed() {
        val tab = _uiState.value.feedState
        if (tab.isLoadingMore || !tab.hasMorePages) return
        viewModelScope.launch {
            val nextPage = tab.currentPage + 1
            _uiState.update { it.copy(feedState = it.feedState.copy(isLoadingMore = true)) }
            when (val result = routeRepository.getFeed(page = nextPage)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(feedState = it.feedState.copy(
                        routes = it.feedState.routes + result.data.items,
                        isLoadingMore = false,
                        currentPage = nextPage,
                        hasMorePages = nextPage < result.data.pages
                    ))
                }
                else -> _uiState.update { it.copy(feedState = it.feedState.copy(isLoadingMore = false)) }
            }
        }
    }

    fun loadRecommended() {
        viewModelScope.launch {
            _uiState.update { it.copy(recommendedState = it.recommendedState.copy(isLoading = true, error = null, currentPage = 1)) }
            when (val result = routeRepository.getRecommended(page = 1)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(recommendedState = it.recommendedState.copy(
                        routes = result.data.items,
                        isLoading = false,
                        currentPage = 1,
                        hasMorePages = result.data.page < result.data.pages
                    ))
                }
                is ApiResult.Error -> _uiState.update { it.copy(recommendedState = it.recommendedState.copy(isLoading = false, error = result.message)) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(recommendedState = it.recommendedState.copy(isLoading = false, error = "Нет подключения к интернету")) }
            }
        }
    }

    fun loadMoreRecommended() {
        val tab = _uiState.value.recommendedState
        if (tab.isLoadingMore || !tab.hasMorePages) return
        viewModelScope.launch {
            val nextPage = tab.currentPage + 1
            _uiState.update { it.copy(recommendedState = it.recommendedState.copy(isLoadingMore = true)) }
            when (val result = routeRepository.getRecommended(page = nextPage)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(recommendedState = it.recommendedState.copy(
                        routes = it.recommendedState.routes + result.data.items,
                        isLoadingMore = false,
                        currentPage = nextPage,
                        hasMorePages = nextPage < result.data.pages
                    ))
                }
                else -> _uiState.update { it.copy(recommendedState = it.recommendedState.copy(isLoadingMore = false)) }
            }
        }
    }

    fun toggleLike(routeId: String, currentlyLiked: Boolean) {
        viewModelScope.launch {
            val result = if (currentlyLiked) routeRepository.unlikeRoute(routeId)
                         else routeRepository.likeRoute(routeId)
            if (result is ApiResult.Success) {
                updateRouteInBothTabs(routeId) { route ->
                    route.copy(
                        isLiked = !currentlyLiked,
                        likesCount = route.likesCount + if (currentlyLiked) -1 else 1
                    )
                }
            }
        }
    }

    fun toggleSave(routeId: String, currentlySaved: Boolean) {
        viewModelScope.launch {
            val result = if (currentlySaved) routeRepository.unsaveRoute(routeId)
                         else routeRepository.saveRoute(routeId)
            if (result is ApiResult.Success) {
                updateRouteInBothTabs(routeId) { route ->
                    route.copy(
                        isSaved = !currentlySaved,
                        savesCount = route.savesCount + if (currentlySaved) -1 else 1
                    )
                }
            }
        }
    }

    private fun updateRouteInBothTabs(routeId: String, transform: (TrailRoute) -> TrailRoute) {
        _uiState.update { state ->
            state.copy(
                feedState = state.feedState.copy(routes = state.feedState.routes.map { if (it.id == routeId) transform(it) else it }),
                recommendedState = state.recommendedState.copy(routes = state.recommendedState.routes.map { if (it.id == routeId) transform(it) else it })
            )
        }
    }
}
