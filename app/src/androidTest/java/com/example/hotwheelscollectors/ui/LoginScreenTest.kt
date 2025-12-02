// LoginScreenTest.kt
package com.example.hotwheelscollectors.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.hotwheelscollectors.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class LoginScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun loginScreenShowsAllElements() {
        composeRule.onNodeWithText("Email").assertIsDisplayed()
        composeRule.onNodeWithText("Password").assertIsDisplayed()
        composeRule.onNodeWithText("Sign In").assertIsDisplayed()
        composeRule.onNodeWithText("Forgot Password?").assertIsDisplayed()
    }

    @Test
    fun invalidEmailShowsError() {
        // Enter invalid email
        composeRule.onNodeWithText("Email")
            .performTextInput("invalid-email")

        // Click sign in
        composeRule.onNodeWithText("Sign In").performClick()

        // Verify error is shown
        composeRule.onNodeWithText("Invalid email format")
            .assertIsDisplayed()
    }
}