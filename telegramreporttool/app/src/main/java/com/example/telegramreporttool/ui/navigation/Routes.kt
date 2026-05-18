package com.example.telegramreporttool.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Landing : Route

    @Serializable
    data object Login : Route

    @Serializable
    data object Dashboard : Route

    @Serializable
    data object Reporting : Route

    @Serializable
    data object Settings : Route
}
