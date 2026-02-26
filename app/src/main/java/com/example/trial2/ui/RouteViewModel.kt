package com.trail2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trail2.data.Difficulty
import com.trail2.data.TrailRoute
import com.trail2.data.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RouteViewModel @Inject constructor(
    private val repository: RouteRepository
) : ViewModel() {

    private val _filterDifficulty = MutableStateFlow<Difficulty?>(null)
    val filterDifficulty: StateFlow<Difficulty?> = _filterDifficulty.asStateFlow()

    val routes: StateFlow<List<TrailRoute>> = _filterDifficulty
        .flatMapLatest { filter ->
            if (filter == null) repository.getAllRoutes()
            else repository.getRoutesByDifficulty(filter)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(difficulty: Difficulty?) {
        _filterDifficulty.value = difficulty
    }

    fun toggleLike(routeId: String, currentlyLiked: Boolean) {
        viewModelScope.launch {
            repository.toggleLike(routeId, currentlyLiked)
        }
    }

    fun toggleSave(routeId: String, currentlySaved: Boolean) {
        viewModelScope.launch {
            repository.toggleSave(routeId, currentlySaved)
        }
    }
}
