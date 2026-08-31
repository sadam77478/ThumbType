package com.sadam.thumbtype.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
        composeRule.onNodeWithText("Continue").performClick()

        waitForText("Choose your focus")
        composeRule.onNodeWithText("Continue").performClick()

        waitForText("Build a daily routine")
        composeRule.onNodeWithText("Continue").performClick()

        waitForText("Plan ready")
        composeRule.onNodeWithText("Skip test for now").assertIsDisplayed().performClick()

        waitForTag("thumbtype-shell-home")
        composeRule.onNodeWithTag("thumbtype-shell-home").assertIsDisplayed()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching { composeRule.onNodeWithText(text).fetchSemanticsNode() }.isSuccess
        }
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching { composeRule.onNodeWithTag(tag).fetchSemanticsNode() }.isSuccess
        }
    }
}
