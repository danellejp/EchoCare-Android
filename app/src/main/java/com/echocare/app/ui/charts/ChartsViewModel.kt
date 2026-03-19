package com.echocare.app.ui.charts

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
 * ViewModel for the Charts/Visualisation screen.
 *
 * Manages:
 *   - Loading cry events from the Pi via the repository
 *   - Computing chart data (distribution + timeline)
 *   - Time range filtering (24h vs 7 days)
 *   - Loading, error and empty states
 *
 * Follows MVVM: Fragment observes LiveData, ViewModel handles logic.
 */
class ChartsViewModel : ViewModel() {

    private val repository = EchoCareRepository()
    private val TAG = "ChartsViewModel"

    // =========================================================================
    // LiveData — observed by the Fragment
    // =========================================================================

    /** Raw cry events from API */
    private val _cryEvents = MutableLiveData<List<CryEvent>>(emptyList())
    val cryEvents: LiveData<List<CryEvent>> = _cryEvents

    /** Cry type distribution: map of type name → count */
    private val _distributionData = MutableLiveData<Map<String, Int>>(emptyMap())
    val distributionData: LiveData<Map<String, Int>> = _distributionData

    /** Cry timeline: map of hour (0–23) → count */
    private val _timelineData = MutableLiveData<Map<Int, Int>>(emptyMap())
    val timelineData: LiveData<Map<Int, Int>> = _timelineData

    /** Total cry count */
    private val _totalCries = MutableLiveData(0)
    val totalCries: LiveData<Int> = _totalCries

    /** Most common cry type */
    private val _mostCommonType = MutableLiveData("—")
    val mostCommonType: LiveData<String> = _mostCommonType

    /** Time of last cry */
    private val _lastCryTime = MutableLiveData("—")
    val lastCryTime: LiveData<String> = _lastCryTime

    /** Loading state */
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    /** Error message (null = no error) */
    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    /** Whether data is empty */
    private val _isEmpty = MutableLiveData(false)
    val isEmpty: LiveData<Boolean> = _isEmpty

    // =========================================================================
    // Filter State
    // =========================================================================

    /** Current time range in hours */
    private var currentHoursBack: Int = AppConstants.WEEK_TIME_RANGE_HOURS

    /** Human-readable label for current time filter */
    private val _timeFilterLabel = MutableLiveData("Past 7 Days")
    val timeFilterLabel: LiveData<String> = _timeFilterLabel

    // =========================================================================
    // Public Methods
    // =========================================================================

    /**
     * Load chart data from the Pi.
     * Called on initial load and pull-to-refresh.
     */
    fun loadChartData() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = repository.getCryEvents(
                hoursBack = currentHoursBack,
                cryTypeFilter = null // Always fetch all types for charts
            )

            result.onSuccess { events ->
                _cryEvents.value = events
                _totalCries.value = events.size
                _isEmpty.value = events.isEmpty()
                _errorMessage.value = null

                if (events.isNotEmpty()) {
                    _distributionData.value = ChartDataHelper.computeDistribution(events)
                    _timelineData.value = ChartDataHelper.computeTimeline(events)
                    _mostCommonType.value = ChartDataHelper.findMostCommonType(events)

                    // Last cry time — events are sorted newest first
                    _lastCryTime.value = events.firstOrNull()?.timeAgo() ?: "-"
                } else {
                    _distributionData.value = emptyMap()
                    _timelineData.value = emptyMap()
                    _mostCommonType.value = "-"
                    _lastCryTime.value = "-"
                }

                Log.d(TAG, "Loaded ${events.size} events for charts")
            }

            result.onFailure { error ->
                _cryEvents.value = emptyList()
                _totalCries.value = 0
                _isEmpty.value = true
                _distributionData.value = emptyMap()
                _timelineData.value = emptyMap()
                _errorMessage.value = when {
                    error.message?.contains("Unable to resolve host") == true ->
                        "Cannot connect to EchoCare Pi.\nMake sure you're on the EchoCare WiFi network."
                    error.message?.contains("timeout") == true ->
                        "Connection timed out.\nIs the Pi powered on?"
                    else ->
                        "Failed to load chart data: ${error.message}"
                }
                Log.e(TAG, "Error loading chart data", error)
            }

            _isLoading.value = false
        }
    }

    /**
     * Set time range filter.
     * @param hours 24 for past day, 168 for past week
     */
    fun setTimeRange(hours: Int) {
        currentHoursBack = hours
        _timeFilterLabel.value = if (hours == AppConstants.DEFAULT_TIME_RANGE_HOURS)
            "Past 24 Hours" else "Past 7 Days"
        loadChartData()
    }
}