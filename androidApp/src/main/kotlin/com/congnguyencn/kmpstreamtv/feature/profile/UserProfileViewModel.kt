package com.congnguyencn.kmpstreamtv.feature.profile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserProfileViewModel : ViewModel() {
    private val mutableItems = MutableStateFlow(signedOutProfileItems)
    val items = mutableItems.asStateFlow()
}
