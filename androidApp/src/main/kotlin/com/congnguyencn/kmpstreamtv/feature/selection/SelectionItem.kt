package com.congnguyencn.kmpstreamtv.feature.selection

data class SelectionItem(
    val id: String,
    val title: String,
    val imageUrl: String? = null,
    val isSelected: Boolean = false,
)
