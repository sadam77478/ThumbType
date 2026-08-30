package com.sadam.thumbtype.mobile.app.navigation

import com.sadam.thumbtype.mobile.AppScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThumbTypeNavigationTest {
    @Test
    fun navigationSetsAreStableAndDisjoint() {
        assertEquals(
            listOf(AppScreen.Home, AppScreen.Learn, AppScreen.Practice, AppScreen.Progress, AppScreen.Profile),
            ThumbTypeNavigation.bottomDestinations
        )
        assertTrue(ThumbTypeNavigation.bottomDestinations.toSet().intersect(ThumbTypeNavigation.fullScreenDestinations).isEmpty())
    }

    @Test
    fun backPolicyReturnsSafeDestinations() {
        assertEquals(AppScreen.Home, ThumbTypeNavigation.backDestination(AppScreen.Trainer))
        assertEquals(AppScreen.Home, ThumbTypeNavigation.backDestination(AppScreen.Results))
        assertEquals(AppScreen.Profile, ThumbTypeNavigation.backDestination(AppScreen.Privacy))
        assertEquals(AppScreen.Home, ThumbTypeNavigation.backDestination(AppScreen.Learn))
        assertEquals(AppScreen.Home, ThumbTypeNavigation.backDestination(AppScreen.Home))
        assertEquals(AppScreen.Onboarding, ThumbTypeNavigation.backDestination(AppScreen.Onboarding))
    }
}
