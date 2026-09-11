package com.congnguyencn.kmpstreamtv.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.congnguyencn.kmpstreamtv.feature.home.domain.repository.HomeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Shared through the ViewModel layer; native UIs only render [uiState] and send intents back. */
class HomeViewModel internal constructor(
    private val repository: HomeRepository,
    private val mapper: HomeUiMapper,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()
    val currentState: HomeUiState get() = uiState.value

    private var loadJob: Job? = null

    init {
        loadHome()
    }

    fun loadHome() {
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
                try {
                    val sections = mapper.map(repository.getHomeSections())
                    mutableUiState.value = HomeUiState(isLoading = false, sections = sections)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Exception) {
                    mutableUiState.value =
                        HomeUiState(
                            isLoading = false,
                            errorMessage = error.message ?: "Unable to load Home content",
                        )
                }
            }
    }

    /** Swift does not consume Flow directly, so it owns and cancels this lifecycle-bound bridge. */
    fun observe(onState: (HomeUiState) -> Unit): Observation =
        Observation(
            viewModelScope.launch { uiState.collect(onState) },
        )

    /** Called by the Swift owner. Android's ViewModelStore invokes normal ViewModel clearing. */
    fun dispose() {
        viewModelScope.cancel()
    }
}
