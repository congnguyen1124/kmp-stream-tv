package com.congnguyencn.kmpstreamtv.feature.short.presentation

data class ShortUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val items: List<ShortItemUiModel> = emptyList(),
    val activeIndex: Int = 0,
    val hasNextPage: Boolean = true,
    val errorMessage: String? = null,
)

data class StoryGroupUiState(
    val isLoading: Boolean = false,
    val items: List<ShortItemUiModel> = emptyList(),
    val activeIndex: Int = 0,
    val errorMessage: String? = null,
)
