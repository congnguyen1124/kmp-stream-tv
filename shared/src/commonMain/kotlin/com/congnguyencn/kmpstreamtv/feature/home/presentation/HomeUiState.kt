package com.congnguyencn.kmpstreamtv.feature.home.presentation

import com.congnguyencn.kmpstreamtv.feature.home.presentation.model.HomeSectionUiModel

data class HomeUiState(
    val isLoading: Boolean = true,
    val sections: List<HomeSectionUiModel> = emptyList(),
    val errorMessage: String? = null,
)
