package com.echocare.app.data.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the CryEvent data model.
 *
 * Tests:
 *   - getCryTypeDisplay(): maps raw cry type to user-friendly name
 *   - classificationPercent(): converts decimal confidence to percentage
 *   - detectionPercent(): converts decimal confidence to percentage
 *   - formattedTemperature(): formats temperature for display
 *   - formattedHumidity(): formats humidity for display
 */
class CryEventTest {

    // Helper - create a CryEvent with default values for convenience
    private fun createEvent(
        id: Int = 1,
        timestamp: String = "2026-03-18 14:30:00",
        cryType: String = "Hungry",
        detectionConfidence: Double = 0.95,
        classificationConfidence: Double = 0.82,
        temperature: Double? = 21.5,
        humidity: Double? = 48.0
    ) = CryEvent(
        id = id,
        timestamp = timestamp,
        cryType = cryType,
        detectionConfidence = detectionConfidence,
        classificationConfidence = classificationConfidence,
        temperature = temperature,
        humidity = humidity
    )

    // getCryTypeDisplay() Tests
    @Test
    fun `getCryTypeDisplay returns Hungry for hungry type`() {
        val event = createEvent(cryType = "Hungry")
        assertEquals("Hungry", event.getCryTypeDisplay())
    }

    @Test
    fun `getCryTypeDisplay returns Pain for pain type`() {
        val event = createEvent(cryType = "Pain")
        assertEquals("Pain", event.getCryTypeDisplay())
    }

    @Test
    fun `getCryTypeDisplay returns Normal for normal type`() {
        val event = createEvent(cryType = "Normal")
        assertEquals("Normal", event.getCryTypeDisplay())
    }

    @Test
    fun `getCryTypeDisplay handles lowercase input`() {
        val event = createEvent(cryType = "hungry")
        assertEquals("Hungry", event.getCryTypeDisplay())
    }

    @Test
    fun `getCryTypeDisplay returns raw type for unknown values`() {
        val event = createEvent(cryType = "Unknown")
        assertEquals("Unknown", event.getCryTypeDisplay())
    }


    // Confidence Percentage Tests
    @Test
    fun `classificationPercent converts decimal to percentage`() {
        val event = createEvent(classificationConfidence = 0.82)
        assertEquals(82, event.classificationPercent())
    }

    @Test
    fun `classificationPercent handles zero confidence`() {
        val event = createEvent(classificationConfidence = 0.0)
        assertEquals(0, event.classificationPercent())
    }

    @Test
    fun `classificationPercent handles full confidence`() {
        val event = createEvent(classificationConfidence = 1.0)
        assertEquals(100, event.classificationPercent())
    }

    @Test
    fun `detectionPercent converts decimal to percentage`() {
        val event = createEvent(detectionConfidence = 0.95)
        assertEquals(95, event.detectionPercent())
    }

    @Test
    fun `detectionPercent handles threshold boundary`() {
        val event = createEvent(detectionConfidence = 0.85)
        assertEquals(85, event.detectionPercent())
    }


    // Temperature and Humidity Formatting Tests
    @Test
    fun `formattedTemperature displays value with one decimal`() {
        val event = createEvent(temperature = 22.3)
        assertEquals("22.3°C", event.formattedTemperature())
    }

    @Test
    fun `formattedTemperature returns NA when null`() {
        val event = createEvent(temperature = null)
        assertEquals("N/A", event.formattedTemperature())
    }

    @Test
    fun `formattedHumidity displays value with one decimal`() {
        val event = createEvent(humidity = 48.5)
        assertEquals("48.5%", event.formattedHumidity())
    }

    @Test
    fun `formattedHumidity returns NA when null`() {
        val event = createEvent(humidity = null)
        assertEquals("N/A", event.formattedHumidity())
    }

    // Edge Cases
    @Test
    fun `event with negative temperature formats correctly`() {
        val event = createEvent(temperature = -2.5)
        assertEquals("-2.5°C", event.formattedTemperature())
    }

    @Test
    fun `event with zero values formats correctly`() {
        val event = createEvent(temperature = 0.0, humidity = 0.0)
        assertEquals("0.0°C", event.formattedTemperature())
        assertEquals("0.0%", event.formattedHumidity())
    }

    @Test
    fun `event with very high confidence rounds correctly`() {
        val event = createEvent(classificationConfidence = 0.999)
        assertEquals(99, event.classificationPercent())
    }
}