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
                    computeDistribution(events)
                    computeTimeline(events)
                    computeSummary(events)
                } else {
                    _distributionData.value = emptyMap()
                    _timelineData.value = emptyMap()
                    _mostCommonType.value = "—"
                    _lastCryTime.value = "—"
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

    /**
     * Get current time range in hours.
     */
    fun getCurrentHoursBack(): Int = currentHoursBack

    // =========================================================================
    // Private Helpers — compute chart data from events
    // =========================================================================

    /**
     * Compute cry type distribution (Hungry / Pain / Normal counts).
     */
    private fun computeDistribution(events: List<CryEvent>) {
        val distribution = mutableMapOf(
            "Hungry" to 0,
            "Pain" to 0,
            "Normal" to 0
        )

        for (event in events) {
            val type = event.getCryTypeDisplay()
            distribution[type] = (distribution[type] ?: 0) + 1
        }

        _distributionData.value = distribution
        Log.d(TAG, "Distribution: $distribution")
    }

    /**
     * Compute cry timeline — count of cries per hour of day (0–23).
     * Parses the timestamp from each event to extract the hour.
     */
    private fun computeTimeline(events: List<CryEvent>) {
        // Initialise all 24 hours to 0
        val timeline = mutableMapOf<Int, Int>()
        for (h in 0..23) {
            timeline[h] = 0
        }

        for (event in events) {
            try {
                // Timestamp format from Pi: "2026-02-10 14:30:22" or ISO format
                val timestamp = event.timestamp
                val hour = extractHour(timestamp)
                if (hour in 0..23) {
                    timeline[hour] = (timeline[hour] ?: 0) + 1
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse timestamp: ${event.timestamp}")
            }
        }

        _timelineData.value = timeline
        Log.d(TAG, "Timeline: $timeline")
    }

    /**
     * Compute summary stats: most common type and last cry time.
     */
    private fun computeSummary(events: List<CryEvent>) {
        // Most common type
        val typeCounts = events.groupBy { it.getCryTypeDisplay() }
            .mapValues { it.value.size }
        val mostCommon = typeCounts.maxByOrNull { it.value }
        _mostCommonType.value = mostCommon?.key ?: "—"

        // Last cry time
        val lastEvent = events.firstOrNull() // Events are sorted newest first
        _lastCryTime.value = lastEvent?.timeAgo() ?: "—"
    }

    /**
     * Extract hour (0–23) from a timestamp string.
     * Supports formats: "2026-02-10 14:30:22" and "2026-02-10T14:30:22"
     */
    private fun extractHour(timestamp: String): Int {
        return try {
            // Try "yyyy-MM-dd HH:mm:ss" format
            val timePart = if (timestamp.contains("T")) {
                timestamp.substringAfter("T")
            } else {
                timestamp.substringAfter(" ")
            }
            timePart.substringBefore(":").toInt()
        } catch (e: Exception) {
            -1
        }
    }
}