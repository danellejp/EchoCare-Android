package com.echocare.app.ui.dashboard

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echocare.app.data.model.CryEvent
import com.echocare.app.data.repository.EchoCareRepository
import com.echocare.app.util.AppConstants
import kotlinx.coroutines.launch

/**
 * ViewModel for the Dashboard screen.
 *
 * Manages:
 *   - Loading cry events from the Pi
 *   - Time range filtering (24h vs 7 days)
 *   - Cry type filtering (All / Hungry / Pain / Normal)
 *   - Loading, error, and empty states
 *   - Pull-to-refresh
 *
 * Follows MVVM: Fragment observes LiveData, ViewModel handles logic,
 * Repository handles data fetching.
 */
class DashboardViewModel : ViewModel() {

    private val repository = EchoCareRepository()
    private val TAG = "DashboardViewModel"
    private var timeSynced = false

    // =========================================================================
    // LiveData — observed by the Fragment
    // =========================================================================

    /** The filtered list of cry events displayed in the RecyclerView */
    private val _cryEvents = MutableLiveData<List<CryEvent>>(emptyList())
    val cryEvents: LiveData<List<CryEvent>> = _cryEvents

    /** Whether data is currently being loaded */
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    /** Error message to display (null = no error) */
    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    /** Whether the event list is empty after filtering */
    private val _isEmpty = MutableLiveData(false)
    val isEmpty: LiveData<Boolean> = _isEmpty

    /** Count of events matching current filters */
    private val _eventCount = MutableLiveData(0)
    val eventCount: LiveData<Int> = _eventCount

    // =========================================================================
    // Filter State
    // =========================================================================

    /** Current time range in hours: 24 or 168 (7 days) */
    private var currentHoursBack: Int = AppConstants.WEEK_TIME_RANGE_HOURS

    /** Current cry type filter: null means "All" */
    private var currentCryTypeFilter: String? = null

    /** Human-readable label for the current time filter */
    private val _timeFilterLabel = MutableLiveData("Past 7 Days")
    val timeFilterLabel: LiveData<String> = _timeFilterLabel

    /** Human-readable label for the current type filter */
    private val _typeFilterLabel = MutableLiveData("All Types")
    val typeFilterLabel: LiveData<String> = _typeFilterLabel

    // =========================================================================
    // Public Methods — called by the Fragment
    // =========================================================================

    /**
     * Load dashboard data from the Pi.
     * Called on initial load and pull-to-refresh.
     */
    fun loadDashboardData() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            // Sync Pi time once on first load
            if (!timeSynced) {
                repository.syncPiTime()
                timeSynced = true
            }

            val result = repository.getCryEvents(
                hoursBack = currentHoursBack,
                cryTypeFilter = currentCryTypeFilter
            )

            result.onSuccess { events ->
                _cryEvents.value = events
                _eventCount.value = events.size
                _isEmpty.value = events.isEmpty()
                _errorMessage.value = null
                Log.d(TAG, "Loaded ${events.size} events")
            }

            result.onFailure { error ->
                _cryEvents.value = emptyList()
                _eventCount.value = 0
                _isEmpty.value = true
                _errorMessage.value = when {
                    error.message?.contains("Unable to resolve host") == true ->
                        "Cannot connect to EchoCare Pi.\nMake sure you're on the EchoCare WiFi network."
                    error.message?.contains("timeout") == true ->
                        "Connection timed out.\nIs the Pi powered on?"
                    error.message?.contains("Connection refused") == true ->
                        "Pi is reachable but the API server isn't running."
                    else ->
                        "Failed to load data: ${error.message}"
                }
                Log.e(TAG, "Error loading data", error)
            }

            _isLoading.value = false
        }
    }

    /**
     * Toggle between "Past 24 Hours" and "Past 7 Days".
     */
    fun toggleTimeFilter() {
        if (currentHoursBack == AppConstants.DEFAULT_TIME_RANGE_HOURS) {
            currentHoursBack = AppConstants.WEEK_TIME_RANGE_HOURS
            _timeFilterLabel.value = "Past 7 Days"
        } else {
            currentHoursBack = AppConstants.DEFAULT_TIME_RANGE_HOURS
            _timeFilterLabel.value = "Past 24 Hours"
        }
        loadDashboardData()
    }

    /**
     * Set the cry type filter.
     *
     * @param type One of: "All", "Hungry", "Pain", "Normal"
     */
    fun setCryTypeFilter(type: String) {
        currentCryTypeFilter = if (type == AppConstants.ALL) null else type
        _typeFilterLabel.value = if (type == AppConstants.ALL) "All Types" else type
        loadDashboardData()
    }

    /**
     * Returns the currently selected time filter in hours.
     */
    fun getCurrentHoursBack(): Int = currentHoursBack

    /**
     * Returns the currently selected cry type filter (null = All).
     */
    fun getCurrentCryTypeFilter(): String? = currentCryTypeFilter
}