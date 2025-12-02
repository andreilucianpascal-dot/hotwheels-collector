// CarDetailsScreenTest.kt
package com.example.hotwheelscollectors.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.hotwheelscollectors.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CarDetailsScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun carDetailsDisplaysCorrectly() {
        // Navigate to car details
        composeRule.onNodeWithText("Test Car").performClick()

        // Verify details are displayed
        composeRule.onNodeWithText("Test Car").assertIsDisplayed()
        composeRule.onNodeWithText("Ferrari").assertIsDisplayed()
        composeRule.onNodeWithText("F40").assertIsDisplayed()
    }

    @Test
    fun deleteCarShowsConfirmationDialog() {
        // Click delete button
        composeRule.onNodeWithContentDescription("Delete").performClick()

        // Verify dialog is shown
        composeRule.onNodeWithText("Delete Car?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").assertIsDisplayed()
    }
}