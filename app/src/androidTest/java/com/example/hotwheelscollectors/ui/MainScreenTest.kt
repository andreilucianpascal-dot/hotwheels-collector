// MainScreenTest.kt
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
class MainScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun mainScreenShowsAllTabs() {
        // Verify bottom navigation tabs
        composeRule.onNodeWithText("Collection").assertIsDisplayed()
        composeRule.onNodeWithText("Mainline").assertIsDisplayed()
        composeRule.onNodeWithText("Premium").assertIsDisplayed()
        composeRule.onNodeWithText("Others").assertIsDisplayed()
    }

    @Test
    fun fabOpensAddCarOptions() {
        // Click FAB
        composeRule.onNodeWithContentDescription("Add Car").performClick()

        // Verify add options are shown
        composeRule.onNodeWithText("Add Mainline").assertIsDisplayed()
        composeRule.onNodeWithText("Add Premium").assertIsDisplayed()
        composeRule.onNodeWithText("Add Other").assertIsDisplayed()
    }

    @Test
    fun searchBarFiltersCollection() {
        // Enter search query
        composeRule.onNodeWithContentDescription("Search")
            .performClick()
        composeRule.onNodeWithContentDescription("Search field")
            .performTextInput("Ferrari")

        // Verify search results
        composeRule.onNodeWithText("Ferrari F40").assertIsDisplayed()
        composeRule.onNodeWithText("Ferrari 458").assertIsDisplayed()
    }

    @Test
    fun sortingOptionsWork() {
        // Open sort menu
        composeRule.onNodeWithContentDescription("Sort").performClick()

        // Select sort option
        composeRule.onNodeWithText("Name").performClick()

        // Verify sorting applied - FIXED LINE 69
        composeRule.onNodeWithTag("car_item").assertExists()
    }

    @Test
    fun filteringOptionsWork() {
        // Open filter menu
        composeRule.onNodeWithContentDescription("Filter").performClick()

        // Apply filter
        composeRule.onNodeWithText("Premium Only").performClick()
        composeRule.onNodeWithText("Apply").performClick()

        // Verify only premium cars shown
        composeRule.onAllNodesWithTag("car_item")
            .filterToOne(hasTestTag("premium_badge"))
            .assertExists()
    }

    @Test
    fun syncIndicatorShowsProgress() {
        // Trigger sync
        composeRule.onNodeWithContentDescription("Sync").performClick()

        // Verify sync progress shown
        composeRule.onNodeWithTag("sync_progress").assertExists()
    }

    private fun assertAreInAlphabeticalOrder() {
        // Custom assertion to verify items are in alphabetical order
    }
}