package com.sadam.thumbtype.mobile.app.navigation

import com.sadam.thumbtype.mobile.AppScreen

/** Central navigation policy for the current single-activity application. */
object ThumbTypeNavigation {
    val bottomDestinations = listOf(
        AppScreen.Home,
        AppScreen.Learn,
        AppScreen.Practice,
        AppScreen.Progress,
        AppScreen.Profile
    )

    val fullScreenDestinations = setOf(
        AppScreen.Onboarding,
        AppScreen.Trainer,
        AppScreen.Results,
        AppScreen.Privacy
    )

    fun backDestination(current: AppScreen): AppScreen = when (current) {
        AppScreen.Trainer, AppScreen.Results -> AppScreen.Home
        AppScreen.Privacy -> AppScreen.Profile
        AppScreen.Home, AppScreen.Onboarding -> current
        else -> AppScreen.Home
    }
}
