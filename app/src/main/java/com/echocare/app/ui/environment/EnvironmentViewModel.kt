package com.echocare.app.ui.environment

import android.graphics.Color
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echocare.app.data.repository.EchoCareRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the Environment Information page.
 *
 * Manages:
 *   - Fetching latest temperature and humidity from the most recent cry event
 *   - Evaluating readings against ideal ranges
 *   - Providing status labels and colours for the UI
 *
 * Temperature and humidity data comes from the DHT22 sensor on the Pi,
 * recorded with each cry event. This ViewModel fetches the most recent
 * event to get the latest readings.
 */
class EnvironmentViewModel : ViewModel() {

    private val repository = EchoCareRepository()
    private val TAG = "EnvironmentViewModel"

    // LiveData - observed by the Fragment
    /** Current temperature reading */
    private val _temperature = MutableLiveData<Double?>(null)
    val temperature: LiveData<Double?> = _temperature

    /** Current humidity reading */
    private val _humidity = MutableLiveData<Double?>(null)
    val humidity: LiveData<Double?> = _humidity

    /** Temperature status text (e.g., "Ideal", "Too High") */
    private val _tempStatus = MutableLiveData("")
    val tempStatus: LiveData<String> = _tempStatus

    /** Humidity status text */
    private val _humidityStatus = MutableLiveData("")
    val humidityStatus: LiveData<String> = _humidityStatus

    /** Temperature status colour */
    private val _tempStatusColor = MutableLiveData(Color.GRAY)
    val tempStatusColor: LiveData<Int> = _tempStatusColor

    /** Humidity status colour */
    private val _humidityStatusColor = MutableLiveData(Color.GRAY)
    val humidityStatusColor: LiveData<Int> = _humidityStatusColor

    /** Last updated timestamp */
    private val _lastUpdated = MutableLiveData("")
    val lastUpdated: LiveData<String> = _lastUpdated

    /** Whether data is available */
    private val _hasData = MutableLiveData(false)
    val hasData: LiveData<Boolean> = _hasData

    /** Loading state */
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    /** Error message */
    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage


    // Ideal Ranges
    companion object {
        // Temperature ranges (°C)
        const val TEMP_LOW = 14.0
        const val TEMP_IDEAL_LOW = 16.0
        const val TEMP_IDEAL_HIGH = 20.0
        const val TEMP_HIGH = 22.0

        // Humidity ranges (%)
        const val HUMIDITY_LOW = 30.0
        const val HUMIDITY_IDEAL_LOW = 40.0
        const val HUMIDITY_IDEAL_HIGH = 60.0
        const val HUMIDITY_HIGH = 70.0

        // Status colours
        val COLOR_IDEAL = Color.parseColor("#4CAF50")       // Green
        val COLOR_WARNING = Color.parseColor("#FF9800")      // Orange
        val COLOR_DANGER = Color.parseColor("#F44336")       // Red
    }


    // Public Methods
    /**
     * Load the latest environment readings from the Pi.
     * Fetches the most recent cry event which includes DHT22 sensor data.
     */
    fun loadEnvironmentData() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = repository.getCryEvents(
                hoursBack = 168, // Past week
                cryTypeFilter = null
            )

            result.onSuccess { events ->
                if (events.isNotEmpty()) {
                    val latestEvent = events.first()

                    val temp = latestEvent.temperature
                    val humid = latestEvent.humidity

                    if (temp != null && humid != null) {
                        _temperature.value = temp
                        _humidity.value = humid
                        _hasData.value = true

                        evaluateTemperature(temp)
                        evaluateHumidity(humid)

                        _lastUpdated.value = "Last updated: ${latestEvent.timeAgo()}"
                    } else {
                        _hasData.value = false
                    }
                } else {
                    _hasData.value = false
                }

                _errorMessage.value = null
                Log.d(TAG, "Environment data loaded: temp=${_temperature.value}, humidity=${_humidity.value}")
            }

            result.onFailure { error ->
                _hasData.value = false
                _errorMessage.value = when {
                    error.message?.contains("Unable to resolve host") == true ->
                        "Cannot connect to EchoCare Pi.\nMake sure you're on the EchoCare WiFi network."
                    error.message?.contains("timeout") == true ->
                        "Connection timed out.\nIs the Pi powered on?"
                    else ->
                        "Failed to load environment data: ${error.message}"
                }
                Log.e(TAG, "Error loading environment data", error)
            }

            _isLoading.value = false
        }
    }


    // Private Helpers - evaluate readings against ideal ranges
    /**
     * Evaluate temperature and set status label + colour.
     */
    private fun evaluateTemperature(temp: Double) {
        when {
            temp < TEMP_LOW -> {
                _tempStatus.value = "Too Cold"
                _tempStatusColor.value = COLOR_DANGER
            }
            temp < TEMP_IDEAL_LOW -> {
                _tempStatus.value = "Slightly Cool"
                _tempStatusColor.value = COLOR_WARNING
            }
            temp <= TEMP_IDEAL_HIGH -> {
                _tempStatus.value = "Ideal"
                _tempStatusColor.value = COLOR_IDEAL
            }
            temp <= TEMP_HIGH -> {
                _tempStatus.value = "Slightly Warm"
                _tempStatusColor.value = COLOR_WARNING
            }
            else -> {
                _tempStatus.value = "Too Hot"
                _tempStatusColor.value = COLOR_DANGER
            }
        }
    }

    /**
     * Evaluate humidity and set status label + colour.
     */
    private fun evaluateHumidity(humidity: Double) {
        when {
            humidity < HUMIDITY_LOW -> {
                _humidityStatus.value = "Too Dry"
                _humidityStatusColor.value = COLOR_DANGER
            }
            humidity < HUMIDITY_IDEAL_LOW -> {
                _humidityStatus.value = "Slightly Dry"
                _humidityStatusColor.value = COLOR_WARNING
            }
            humidity <= HUMIDITY_IDEAL_HIGH -> {
                _humidityStatus.value = "Ideal"
                _humidityStatusColor.value = COLOR_IDEAL
            }
            humidity <= HUMIDITY_HIGH -> {
                _humidityStatus.value = "Slightly Humid"
                _humidityStatusColor.value = COLOR_WARNING
            }
            else -> {
                _humidityStatus.value = "Too Humid"
                _humidityStatusColor.value = COLOR_DANGER
            }
        }
    }
}