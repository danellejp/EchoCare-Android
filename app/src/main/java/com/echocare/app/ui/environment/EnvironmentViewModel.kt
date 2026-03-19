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

                        val tempResult = EnvironmentEvaluator.evaluateTemperature(temp)
                        _tempStatus.value = tempResult.label
                        _tempStatusColor.value = tempResult.color

                        val humidResult = EnvironmentEvaluator.evaluateHumidity(humid)
                        _humidityStatus.value = humidResult.label
                        _humidityStatusColor.value = humidResult.color

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
}