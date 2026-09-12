package com.congnguyencn.kmpstreamtv.feature.profile

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.congnguyencn.kmpstreamtv.R

sealed interface ProfileItem {
    data object SignIn : ProfileItem

    data object Gap : ProfileItem

    data class Section(
        val actions: List<ProfileAction>,
    ) : ProfileItem
}

data class ProfileAction(
    val id: String,
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
)

internal val signedOutProfileItems =
    listOf(
        ProfileItem.SignIn,
        ProfileItem.Gap,
        ProfileItem.Section(
            actions =
                listOf(
                    ProfileAction("settings", R.drawable.ic_app_setting, R.string.app_settings),
                ),
        ),
        ProfileItem.Gap,
        ProfileItem.Section(
            actions =
                listOf(
                    ProfileAction("about", R.drawable.ic_info_outline_medium, R.string.about),
                    ProfileAction("privacy", R.drawable.ic_policy, R.string.privacy_policy),
                    ProfileAction("terms", R.drawable.ic_term, R.string.terms_and_conditions),
                    ProfileAction("feedback", R.drawable.ic_feedback, R.string.feedback),
                ),
        ),
        ProfileItem.Gap,
    )
