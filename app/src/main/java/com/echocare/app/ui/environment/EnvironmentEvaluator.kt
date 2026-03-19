package com.echocare.app.ui.environment

import android.graphics.Color

/**
 * Evaluates temperature and humidity readings against ideal ranges
 * for a baby's room.
 *
 * Extracted from EnvironmentViewModel for testability.
 * Contains pure evaluation logic with no Android dependencies
 * except Colour constants.
 *
 * Ideal ranges:
 *   - Temperature: 16°C - 20°C
 *   - Humidity: 40% – 60%
 */
object EnvironmentEvaluator {

    // Temperature thresholds (°C)
    const val TEMP_LOW = 14.0
    const val TEMP_IDEAL_LOW = 16.0
    const val TEMP_IDEAL_HIGH = 20.0
    const val TEMP_HIGH = 22.0

    // Humidity thresholds (%)
    const val HUMIDITY_LOW = 30.0
    const val HUMIDITY_IDEAL_LOW = 40.0
    const val HUMIDITY_IDEAL_HIGH = 60.0
    const val HUMIDITY_HIGH = 70.0

    // Colours
    const val COLOR_IDEAL = 0xFF4CAF50.toInt()
    const val COLOR_WARNING = 0xFFFF9800.toInt()
    const val COLOR_DANGER = 0xFFF44336.toInt()

    /**
     * Result of evaluating a sensor reading.
     */
    data class EvalResult(val label: String, val color: Int)

    /**
     * Evaluate a temperature reading.
     *
     * @param temp Temperature in °C
     * @return EvalResult with status label and colour
     */
    fun evaluateTemperature(temp: Double): EvalResult {
        return when {
            temp < TEMP_LOW -> EvalResult("Too Cold", COLOR_DANGER)
            temp < TEMP_IDEAL_LOW -> EvalResult("Slightly Cool", COLOR_WARNING)
            temp <= TEMP_IDEAL_HIGH -> EvalResult("Ideal", COLOR_IDEAL)
            temp <= TEMP_HIGH -> EvalResult("Slightly Warm", COLOR_WARNING)
            else -> EvalResult("Too Hot", COLOR_DANGER)
        }
    }

    /**
     * Evaluate a humidity reading.
     *
     * @param humidity Humidity percentage
     * @return EvalResult with status label and colour
     */
    fun evaluateHumidity(humidity: Double): EvalResult {
        return when {
            humidity < HUMIDITY_LOW -> EvalResult("Too Dry", COLOR_DANGER)
            humidity < HUMIDITY_IDEAL_LOW -> EvalResult("Slightly Dry", COLOR_WARNING)
            humidity <= HUMIDITY_IDEAL_HIGH -> EvalResult("Ideal", COLOR_IDEAL)
            humidity <= HUMIDITY_HIGH -> EvalResult("Slightly Humid", COLOR_WARNING)
            else -> EvalResult("Too Humid", COLOR_DANGER)
        }
    }
}