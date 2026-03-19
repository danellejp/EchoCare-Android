package com.echocare.app.ui.dashboard

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.echocare.app.util.AppConstants
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.echocare.app.MainDispatcherRule

/**
 * Unit tests for DashboardViewModel.
 *
 * Tests:
 *   - Initial state values
 *   - Time filter toggling (24h / 7 days)
 *   - Cry type filter setting
 *   - Filter label updates
 */
class DashboardViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        viewModel = DashboardViewModel()
    }


    // Initial State Tests
    @Test
    fun `initial events list is empty`() {
        assertTrue(viewModel.cryEvents.value!!.isEmpty())
    }

    @Test
    fun `initial loading state is false`() {
        assertFalse(viewModel.isLoading.value!!)
    }

    @Test
    fun `initial error message is null`() {
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `initial empty state is false`() {
        assertFalse(viewModel.isEmpty.value!!)
    }

    @Test
    fun `initial event count is zero`() {
        assertEquals(0, viewModel.eventCount.value)
    }

    @Test
    fun `initial time filter is Past 7 Days`() {
        assertEquals("Past 7 Days", viewModel.timeFilterLabel.value)
    }

    @Test
    fun `initial type filter is All Types`() {
        assertEquals("All Types", viewModel.typeFilterLabel.value)
    }

    @Test
    fun `initial hours back is 168`() {
        assertEquals(AppConstants.WEEK_TIME_RANGE_HOURS, viewModel.getCurrentHoursBack())
    }

    @Test
    fun `initial cry type filter is null`() {
        assertNull(viewModel.getCurrentCryTypeFilter())
    }


    // Time Filter Toggle Tests
    @Test
    fun `toggleTimeFilter switches from 7 days to 24 hours`() {
        viewModel.toggleTimeFilter()
        assertEquals(AppConstants.DEFAULT_TIME_RANGE_HOURS, viewModel.getCurrentHoursBack())
        assertEquals("Past 24 Hours", viewModel.timeFilterLabel.value)
    }

    @Test
    fun `toggleTimeFilter switches back to 7 days`() {
        viewModel.toggleTimeFilter() // To 24h
        viewModel.toggleTimeFilter() // Back to 7d
        assertEquals(AppConstants.WEEK_TIME_RANGE_HOURS, viewModel.getCurrentHoursBack())
        assertEquals("Past 7 Days", viewModel.timeFilterLabel.value)
    }

    @Test
    fun `toggleTimeFilter three times ends on 24 hours`() {
        viewModel.toggleTimeFilter() // 24h
        viewModel.toggleTimeFilter() // 7d
        viewModel.toggleTimeFilter() // 24h
        assertEquals(AppConstants.DEFAULT_TIME_RANGE_HOURS, viewModel.getCurrentHoursBack())
    }


    // Cry Type Filter Tests
    @Test
    fun `setCryTypeFilter to Hungry updates filter`() {
        viewModel.setCryTypeFilter("Hungry")
        assertEquals("Hungry", viewModel.getCurrentCryTypeFilter())
        assertEquals("Hungry", viewModel.typeFilterLabel.value)
    }

    @Test
    fun `setCryTypeFilter to Pain updates filter`() {
        viewModel.setCryTypeFilter("Pain")
        assertEquals("Pain", viewModel.getCurrentCryTypeFilter())
        assertEquals("Pain", viewModel.typeFilterLabel.value)
    }

    @Test
    fun `setCryTypeFilter to Normal updates filter`() {
        viewModel.setCryTypeFilter("Normal")
        assertEquals("Normal", viewModel.getCurrentCryTypeFilter())
        assertEquals("Normal", viewModel.typeFilterLabel.value)
    }

    @Test
    fun `setCryTypeFilter to All resets to null`() {
        viewModel.setCryTypeFilter("Hungry") // Set filter first
        viewModel.setCryTypeFilter(AppConstants.ALL) // Reset to all
        assertNull(viewModel.getCurrentCryTypeFilter())
        assertEquals("All Types", viewModel.typeFilterLabel.value)
    }

    @Test
    fun `setCryTypeFilter can switch between types`() {
        viewModel.setCryTypeFilter("Hungry")
        assertEquals("Hungry", viewModel.getCurrentCryTypeFilter())

        viewModel.setCryTypeFilter("Pain")
        assertEquals("Pain", viewModel.getCurrentCryTypeFilter())

        viewModel.setCryTypeFilter("Normal")
        assertEquals("Normal", viewModel.getCurrentCryTypeFilter())
    }


    // Combined Filter Tests
    @Test
    fun `time and type filters work independently`() {
        viewModel.toggleTimeFilter() // 24h
        viewModel.setCryTypeFilter("Pain")

        assertEquals(AppConstants.DEFAULT_TIME_RANGE_HOURS, viewModel.getCurrentHoursBack())
        assertEquals("Pain", viewModel.getCurrentCryTypeFilter())
    }

    @Test
    fun `resetting type filter preserves time filter`() {
        viewModel.toggleTimeFilter() // 24h
        viewModel.setCryTypeFilter("Hungry")
        viewModel.setCryTypeFilter(AppConstants.ALL)

        assertEquals(AppConstants.DEFAULT_TIME_RANGE_HOURS, viewModel.getCurrentHoursBack())
        assertNull(viewModel.getCurrentCryTypeFilter())
    }
}