package com.sadam.thumbtype.mobile

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class V5TrainerVisualTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun trainerRendersAndWritesScreenshotForReview() {
        composeRule.setContent {
            ThumbTypeTheme(AppSettings()) {
                V5TrainerScreen(
                    lesson = Lesson(
                        id = -500,
                        stage = 0,
                        title = "Precision Sprint",
                        subtitle = "Visual QA fixture",
                        text = "build smooth two thumb rhythm with clean accurate mobile typing",
                        skill = "Accuracy + rhythm",
                        xp = 40,
                        isPractice = true
                    ),
                    settings = AppSettings(),
                    profile = UserProfile(),
                    onExit = {},
                    onComplete = {}
                )
            }
        }

        composeRule.waitForIdle()
        val node = composeRule.onNodeWithTag("v5-trainer-root").assertIsDisplayed()
        val output = File(
            InstrumentationRegistry.getInstrumentation().targetContext.filesDir,
            "thumbtype-v5-trainer.png"
        )
        output.outputStream().use { stream ->
            node.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        assertTrue(output.exists() && output.length() > 10_000L)
    }
}
