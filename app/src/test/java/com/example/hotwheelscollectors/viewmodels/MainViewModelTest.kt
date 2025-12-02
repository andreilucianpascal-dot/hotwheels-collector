package com.example.hotwheelscollectors.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.hotwheelscollectors.model.HotWheelsCar
import com.example.hotwheelscollectors.model.FilterState
import com.example.hotwheelscollectors.model.SortState
import com.example.hotwheelscollectors.model.ViewType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        viewModel = MainViewModel()
    }

    @Test
    fun `loadCars success updates state with cars`() = runTest {
        // When
        viewModel.loadCars()

        // Then
        val uiState = viewModel.uiState.first()
        assertThat(uiState).isInstanceOf(MainViewModel.UiState.Success::class.java)
    }

    @Test
    fun `toggleCarSelection adds car to selection`() = runTest {
        // Given
        val car = HotWheelsCar(id = "1", name = "Test Car")

        // When
        viewModel.toggleCarSelection(car)

        // Then
        val selectedCars = viewModel.selectedCars.first()
        assertThat(selectedCars).contains(car)
    }

    @Test
    fun `toggleCarSelection removes car from selection when already selected`() = runTest {
        // Given
        val car = HotWheelsCar(id = "1", name = "Test Car")
        viewModel.toggleCarSelection(car) // Add first

        // When
        viewModel.toggleCarSelection(car) // Remove

        // Then
        val selectedCars = viewModel.selectedCars.first()
        assertThat(selectedCars).doesNotContain(car)
    }

    @Test
    fun `clearSelection removes all selected cars`() = runTest {
        // Given
        val car1 = HotWheelsCar(id = "1", name = "Test Car 1")
        val car2 = HotWheelsCar(id = "2", name = "Test Car 2")
        viewModel.toggleCarSelection(car1)
        viewModel.toggleCarSelection(car2)

        // When
        viewModel.clearSelection()

        // Then
        val selectedCars = viewModel.selectedCars.first()
        assertThat(selectedCars).isEmpty()
    }

    @Test
    fun `updateSearchQuery updates search query`() = runTest {
        // Given
        val query = "test search"

        // When
        viewModel.updateSearchQuery(query)

        // Then
        val searchQuery = viewModel.searchQuery.first()
        assertThat(searchQuery).isEqualTo(query)
    }

    @Test
    fun `clearSearchQuery resets search query`() = runTest {
        // Given
        viewModel.updateSearchQuery("test search")

        // When
        viewModel.clearSearchQuery()

        // Then
        val searchQuery = viewModel.searchQuery.first()
        assertThat(searchQuery).isEmpty()
    }

    @Test
    fun `updateFilters updates filter state`() = runTest {
        // Given
        val filters = FilterState()

        // When
        viewModel.updateFilters(filters)

        // Then
        val filterState = viewModel.filterState.first()
        assertThat(filterState).isEqualTo(filters)
    }

    @Test
    fun `clearFilters resets filter state`() = runTest {
        // Given
        val filters = FilterState()
        viewModel.updateFilters(filters)

        // When
        viewModel.clearFilters()

        // Then
        val filterState = viewModel.filterState.first()
        assertThat(filterState).isEqualTo(FilterState())
    }

    @Test
    fun `updateSort updates sort state`() = runTest {
        // Given
        val sort = SortState.NAME_DESC

        // When
        viewModel.updateSort(sort)

        // Then
        val sortState = viewModel.sortState.first()
        assertThat(sortState).isEqualTo(sort)
    }

    @Test
    fun `updateViewType updates view type`() = runTest {
        // Given
        val viewType = ViewType.LIST

        // When
        viewModel.updateViewType(viewType)

        // Then
        val currentViewType = viewModel.viewType.first()
        assertThat(currentViewType).isEqualTo(viewType)
    }
}