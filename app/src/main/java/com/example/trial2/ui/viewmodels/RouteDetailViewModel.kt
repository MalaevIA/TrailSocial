package com.trail2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trail2.data.Comment
import com.trail2.data.TrailRoute
import com.trail2.data.remote.ApiResult
import com.trail2.data.repository.CommentRepository
import com.trail2.data.repository.RouteRepository
import com.trail2.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RouteDetailUiState(
    val route: TrailRoute? = null,
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val commentText: String = "",
    val isSendingComment: Boolean = false,
    val isNavigating: Boolean = false
)

@HiltViewModel
class RouteDetailViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouteDetailUiState())
    val uiState: StateFlow<RouteDetailUiState> = _uiState.asStateFlow()

    fun loadRoute(routeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = routeRepository.getRouteById(routeId)) {
                is ApiResult.Success -> _uiState.update { it.copy(route = result.data, isLoading = false) }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isLoading = false, error = "Нет подключения") }
            }
            loadComments(routeId)
        }
    }

    private fun loadComments(routeId: String) {
        viewModelScope.launch {
            when (val result = commentRepository.getComments(routeId)) {
                is ApiResult.Success -> _uiState.update { it.copy(comments = result.data.items) }
                else -> {}
            }
        }
    }

    fun onCommentTextChange(text: String) {
        _uiState.update { it.copy(commentText = text) }
    }

    fun sendComment() {
        val routeId = _uiState.value.route?.id ?: return
        val text = _uiState.value.commentText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingComment = true) }
            when (val result = commentRepository.createComment(routeId, text)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            comments = it.comments + result.data,
                            commentText = "",
                            isSendingComment = false
                        )
                    }
                }
                else -> _uiState.update { it.copy(isSendingComment = false) }
            }
        }
    }

    fun toggleLike() {
        val route = _uiState.value.route ?: return
        viewModelScope.launch {
            val result = if (route.isLiked) {
                routeRepository.unlikeRoute(route.id)
            } else {
                routeRepository.likeRoute(route.id)
            }
            if (result is ApiResult.Success) {
                _uiState.update { state ->
                    val current = state.route ?: return@update state
                    state.copy(route = current.copy(
                        isLiked = !current.isLiked,
                        likesCount = current.likesCount + if (current.isLiked) -1 else 1
                    ))
                }
            }
        }
    }

    fun toggleSave() {
        val route = _uiState.value.route ?: return
        viewModelScope.launch {
            val result = if (route.isSaved) {
                routeRepository.unsaveRoute(route.id)
            } else {
                routeRepository.saveRoute(route.id)
            }
            if (result is ApiResult.Success) {
                _uiState.update { state ->
                    val current = state.route ?: return@update state
                    state.copy(route = current.copy(
                        isSaved = !current.isSaved,
                        savesCount = current.savesCount + if (current.isSaved) -1 else 1
                    ))
                }
            }
        }
    }

    fun startNavigation() {
        _uiState.update { it.copy(isNavigating = true) }
    }

    fun stopNavigation() {
        _uiState.update { it.copy(isNavigating = false) }
    }

    fun toggleFollow() {
        val route = _uiState.value.route ?: return
        val author = route.author
        viewModelScope.launch {
            val result = if (author.isFollowing) {
                userRepository.unfollow(author.id)
            } else {
                userRepository.follow(author.id)
            }
            if (result is ApiResult.Success) {
                _uiState.update { state ->
                    val current = state.route ?: return@update state
                    state.copy(route = current.copy(
                        author = current.author.copy(isFollowing = !current.author.isFollowing)
                    ))
                }
            }
        }
    }
}
