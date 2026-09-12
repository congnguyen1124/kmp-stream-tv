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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShortViewModel internal constructor(
    private val repository: ShortRepository,
    private val mapper: ShortUiMapper,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ShortUiState())
    val uiState: StateFlow<ShortUiState> = mutableUiState.asStateFlow()
    val currentState: ShortUiState get() = uiState.value

    private var nextPage = 0
    private var loadJob: Job? = null
    private var pendingInitialId: String? = null
    private val likedIds = mutableSetOf<String>()
    private val followedProviderIds = mutableSetOf<String>()
    private val localComments = mutableMapOf<String, MutableList<String>>()

    init {
        reload()
    }

    fun reload() {
        loadJob?.cancel()
        nextPage = 0
        mutableUiState.value = ShortUiState()
        loadPage(reset = true)
    }

    fun loadMore() {
        val state = currentState
        if (state.isLoading || state.isLoadingMore || !state.hasNextPage) return
        loadPage(reset = false)
    }

    fun select(index: Int) {
        if (index !in currentState.items.indices || index == currentState.activeIndex) return
        mutableUiState.update { it.copy(activeIndex = index) }
        if (index >= currentState.items.lastIndex - PREFETCH_DISTANCE) loadMore()
    }

    fun selectById(id: String) {
        currentState.items.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let {
            pendingInitialId = null
            select(it)
            return
        }
        pendingInitialId = id
        if (!currentState.isLoading && !currentState.isLoadingMore && currentState.hasNextPage) {
            loadMore()
        }
    }

    fun toggleLike(id: String) {
        val wasLiked = id in likedIds
        if (wasLiked) likedIds.remove(id) else likedIds.add(id)
        mutableUiState.update { state ->
            state.copy(
                items =
                    state.items.map { item ->
                        if (item.id == id) {
                            val count = (item.likeCount + if (wasLiked) -1 else 1).coerceAtLeast(0)
                            item.copy(
                                isLiked = !wasLiked,
                                likeCount = count,
                                likeCountLabel = count.compactCount(),
                            )
                        } else {
                            item
                        }
                    },
            )
        }
    }

    fun toggleFollow(providerId: String) {
        val follow =
            currentState.items
                .firstOrNull { it.providerId == providerId }
                ?.isFollowingProvider
                ?.not()
                ?: return
        if (follow) followedProviderIds.add(providerId) else followedProviderIds.remove(providerId)
        mutableUiState.update { state ->
            state.copy(
                items =
                    state.items.map { item ->
                        if (item.providerId == providerId) item.copy(isFollowingProvider = follow) else item
                    },
            )
        }
    }

    fun addComment(
        id: String,
        comment: String,
    ) {
        val normalized = comment.trim()
        if (normalized.isEmpty()) return
        localComments.getOrPut(id, ::mutableListOf).add(normalized)
        mutableUiState.update { state ->
            state.copy(
                items =
                    state.items.map { item ->
                        if (item.id == id) {
                            val count = item.commentCount + 1
                            item.copy(
                                commentCount = count,
                                commentCountLabel = count.compactCount(),
                            )
                        } else {
                            item
                        }
                    },
            )
        }
    }

    fun commentsFor(id: String): List<String> = localComments[id].orEmpty()

    fun observe(onState: (ShortUiState) -> Unit): Observation =
        Observation(viewModelScope.launch { uiState.collect(onState) })

    fun dispose() {
        viewModelScope.cancel()
    }

    private fun loadPage(reset: Boolean) {
        loadJob =
            viewModelScope.launch {
                mutableUiState.update {
                    if (reset) {
                        it.copy(isLoading = true, errorMessage = null)
                    } else {
                        it.copy(isLoadingMore = true, errorMessage = null)
                    }
                }
                try {
                    val page = repository.getShorts(page = nextPage, pageSize = PAGE_SIZE)
                    val mapped =
                        page.items.map(mapper::map).map { item ->
                            val commentCount = item.commentCount + localComments[item.id].orEmpty().size
                            item.copy(
                                isLiked = item.id in likedIds,
                                isFollowingProvider = item.providerId in followedProviderIds,
                                commentCount = commentCount,
                                commentCountLabel = commentCount.compactCount(),
                            )
                        }
                    val existing = if (reset) emptyList() else currentState.items
                    val items = existing + mapped.filter { candidate -> existing.none { it.id == candidate.id } }
                    nextPage += 1
                    val targetIndex = pendingInitialId?.let { id -> items.indexOfFirst { it.id == id } } ?: -1
                    if (targetIndex >= 0) pendingInitialId = null
                    mutableUiState.value =
                        currentState.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            items = items,
                            activeIndex =
                                when {
                                    targetIndex >= 0 -> targetIndex
                                    items.isEmpty() -> 0
                                    else -> currentState.activeIndex.coerceIn(items.indices)
                                },
                            hasNextPage = page.hasNextPage,
                        )
                    if (pendingInitialId != null && page.hasNextPage) loadMore()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Exception) {
                    mutableUiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            errorMessage = error.message ?: "Unable to load short videos",
                        )
                    }
                }
            }
    }

    private companion object {
        const val PAGE_SIZE = 4
        const val PREFETCH_DISTANCE = 2
    }
}
