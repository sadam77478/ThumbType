package com.sadam.thumbtype.mobile

import androidx.compose.ui.test.assertExists
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
        waitForText("Build my training plan")
        composeRule.onNodeWithText("Build my training plan").assertIsDisplayed().performClick()

        waitForText("Set your target")
        composeRule.onNodeWithText("Set your target").assertExists()
        composeRule.onNodeWithText("Continue").performClick()

        waitForText("Choose your focus")
        composeRule.onNodeWithText("Choose your focus").assertExists()
        composeRule.onNodeWithText("Continue").performClick()

        waitForText("Build a daily routine")
        composeRule.onNodeWithText("Build a daily routine").assertExists()
        composeRule.onNodeWithText("Continue").performClick()

        waitForText("Plan ready")
        composeRule.onNodeWithText("Plan ready").assertExists()
        composeRule.onNodeWithText("Skip test for now").assertIsDisplayed().performClick()

        waitForText("Build your baseline")
        composeRule.onNodeWithText("Build your baseline").assertExists()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching { composeRule.onNodeWithText(text).fetchSemanticsNode() }.isSuccess
        }
    }
}
