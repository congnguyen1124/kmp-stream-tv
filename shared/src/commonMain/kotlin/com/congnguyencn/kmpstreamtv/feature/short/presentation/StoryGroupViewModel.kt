package com.congnguyencn.kmpstreamtv.feature.short.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.congnguyencn.kmpstreamtv.feature.home.presentation.Observation
import com.congnguyencn.kmpstreamtv.feature.short.domain.repository.ShortRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class StoryGroupViewModel internal constructor(
    private val repository: ShortRepository,
    private val mapper: ShortUiMapper,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(StoryGroupUiState())
    val uiState: StateFlow<StoryGroupUiState> = mutableUiState.asStateFlow()
    val currentState: StoryGroupUiState get() = uiState.value

    private var loadJob: Job? = null

    fun load(initialShortId: String) {
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                mutableUiState.value = StoryGroupUiState(isLoading = true)
                try {
                    val group = repository.getStoryGroup(initialShortId)
                    val items = group.items.map(mapper::map)
                    mutableUiState.value =
                        StoryGroupUiState(
                            items = items,
                            activeIndex = items.indexOfFirst { it.id == initialShortId }.coerceAtLeast(0),
                        )
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Exception) {
                    mutableUiState.value =
                        StoryGroupUiState(
                            errorMessage = error.message ?: "Unable to load this story",
                        )
                }
            }
    }

    fun moveToPrevious(): Boolean {
        val index = currentState.activeIndex
        if (index <= 0) return false
        mutableUiState.value = currentState.copy(activeIndex = index - 1)
        return true
    }

    fun moveToNext(): Boolean {
        val index = currentState.activeIndex
        if (index >= currentState.items.lastIndex) return false
        mutableUiState.value = currentState.copy(activeIndex = index + 1)
        return true
    }

    fun observe(onState: (StoryGroupUiState) -> Unit): Observation =
        Observation(viewModelScope.launch { uiState.collect(onState) })

    fun dispose() {
        viewModelScope.cancel()
    }
}
