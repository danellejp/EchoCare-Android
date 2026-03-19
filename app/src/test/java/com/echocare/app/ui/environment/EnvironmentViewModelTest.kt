package com.echocare.app.ui.environment

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for EnvironmentEvaluator - the temperature and humidity
 * evaluation logic behind the Environment page.
 *
 * Tests every range boundary for both temperature and humidity:
 *   - Danger low (red)
 *   - Warning low (orange)
 *   - Ideal (green)
 *   - Warning high (orange)
 *   - Danger high (red)
 */
class EnvironmentViewModelTest {

    // Temperature Evaluation Tests
    @Test
    fun `temperature below 14 is Too Cold`() {
        val result = EnvironmentEvaluator.evaluateTemperature(13.0)
        assertEquals("Too Cold", result.label)
        assertEquals(EnvironmentEvaluator.COLOR_DANGER, result.color)
    }

    @Test
    fun `temperature at 14 is Slightly Cool`() {
        val result = EnvironmentEvaluator.evaluateTemperature(14.0)
        assertEquals("Slightly Cool", result.label)
        assertEquals(EnvironmentEvaluator.COLOR_WARNING, result.color)
    }

    @Test
    fun `temperature at 15 is Slightly Cool`() {
        val result = EnvironmentEvaluator.evaluateTemperature(15.0)
        assertEquals("Slightly Cool", result.label)
        assertEquals(EnvironmentEvaluator.COLOR_WARNING, result.color)
    }

    @Test
    fun `temperature at 16 is Ideal`() {
        val result = EnvironmentEvaluator.evaluateTemperature(16.0)
        assertEquals("Ideal", result.label)
        assertEquals(EnvironmentEvaluator.COLOR_IDEAL, result.color)
    }

    @Test
    fun `temperature at 18 is Ideal`() {
        val result = EnvironmentEvaluator.evaluateTemperature(18.0)
        assertEquals("Ideal", result.label)
        assertEquals(EnvironmentEvaluator.COLOR_IDEAL, result.color)
    }

    @Test
    fun `temperature at 20 is Ideal`() {
        val result = EnvironmentEvaluator.evaluateTemperature(20.0)
        assertEquals("Ideal", result.label)
        assertEquals(EnvironmentEvaluator.COLOR_IDEAL, result.color)
    }

    @Test
    fun `temperature at 21 is Slightly Warm`() {
        val result = EnvironmentEvaluator.evaluateTemperature(21.0)
        assertEquals("Slightly Warm", result.label)
        assertEquals(EnvironmentEvaluator.COLOR_WARNING, result.color)
    }

    @Test
    fun `temperature at 22 is Slightly Warm`() {
        val result = EnvironmentEvaluator.evaluateTemperature(22.0)
        assertEquals("Slightly Warm", result.label)
        assertEquals(EnvironmentEvaluator.COLOR_WARNING, result.color)
    }

    @Test
    fun `temperature above 22 is Too Hot`() {
        val result = EnvironmentEvaluator.evaluateTemperature(23.0)
        assertEquals("Too Hot", result.label)
        assertEquals(EnvironmentEvaluator.COLOR_DANGER, result.color)
    }

    @Test
    fun `temperature at extreme cold`() {
        val result = EnvironmentEvaluator.evaluateTemperature(5.0)
        assertEquals("Too Cold", result.label)
    }

    @Test
    fun `temperature at extreme heat`() {
        val result = EnvironmentEvaluator.evaluateTemperature(35.0)
        assertEquals("Too Hot", result.label)
    }


    // Humidity Evaluation Tests
    @Test
    fun `humidity below 30 is Too Dry`() {
        val result = EnvironmentEvaluator.evaluateHumidity(25.0)
        assertEquals("Too Dry", result.label)
        assertEquals(EnvironmentEvaluator.COLOR_DANGER, result.color)
    }

    @Test
    fun `humidity at 30 is Slightly Dry`() {
        val result = EnvironmentEvaluator.evaluateHumidity(30.0)
        assertEquals("Slightly Dry", result.label)
        assertEquals(EnvironmentEvaluator.COLOR_WARNING, result.color)
    }

    @Test
    fun `humidity at 35 is Slightly Dry`() {
        val result = EnvironmentEvaluator.evaluateHumidity(35.0)
        assertEquals("Slightly Dry", result.label)
        assertEquals(EnvironmentEvaluator.COLOR_WARNING, result.color)
    }

    @Test
    fun `humidity at 40 is Ideal`() {
        val result = EnvironmentEvaluator.evaluateHumidity(40.0)
        assertEquals("Ideal", result.label)
        assertEquals(EnvironmentEvaluator.COLOR_IDEAL, result.color)
    }

    @Test
    fun `humidity at 50 is Ideal`() {
        val result = EnvironmentEvaluator.evaluateHumidity(50.0)
        assertEquals("Ideal", result.label)
        assertEquals(EnvironmentEvaluator.COLOR_IDEAL, result.color)
    }

    @Test
    fun `humidity at 60 is Ideal`() {
        val result = EnvironmentEvaluator.evaluateHumidity(60.0)
        assertEquals("Ideal", result.label)
        assertEquals(EnvironmentEvaluator.COLOR_IDEAL, result.color)
    }

    @Test
    fun `humidity at 65 is Slightly Humid`() {
        val result = EnvironmentEvaluator.evaluateHumidity(65.0)
        assertEquals("Slightly Humid", result.label)
        assertEquals(EnvironmentEvaluator.COLOR_WARNING, result.color)
    }

    @Test
    fun `humidity at 70 is Slightly Humid`() {
        val result = EnvironmentEvaluator.evaluateHumidity(70.0)
        assertEquals("Slightly Humid", result.label)
        assertEquals(EnvironmentEvaluator.COLOR_WARNING, result.color)
    }

    @Test
    fun `humidity above 70 is Too Humid`() {
        val result = EnvironmentEvaluator.evaluateHumidity(75.0)
        assertEquals("Too Humid", result.label)
        assertEquals(EnvironmentEvaluator.COLOR_DANGER, result.color)
    }

    @Test
    fun `humidity at extreme low`() {
        val result = EnvironmentEvaluator.evaluateHumidity(10.0)
        assertEquals("Too Dry", result.label)
    }

    @Test
    fun `humidity at extreme high`() {
        val result = EnvironmentEvaluator.evaluateHumidity(95.0)
        assertEquals("Too Humid", result.label)
    }
}