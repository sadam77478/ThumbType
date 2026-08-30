package com.sadam.thumbtype.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingJourneyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun onboardingJourney_reachesHomeOnRealAndroidRuntime() {
        composeRule.onNodeWithText("Build my training plan").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Set your target").assertIsDisplayed()

        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Choose your focus").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Build a daily routine").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Plan ready").assertIsDisplayed()
        composeRule.onNodeWithText("Skip test for now").performClick()

        composeRule.waitUntil(timeoutMillis = 8_000) {
            runCatching {
                composeRule.onNodeWithText("Build your baseline").fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithText("Build your baseline").assertIsDisplayed()
    }
}
