package com.echocare.app.data.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the UDPNotification data model.
 *
 * Tests:
 *   - getCryTypeDisplay(): maps cry type for notification display
 *   - getDisplayConfidence(): selects correct confidence and converts to percentage
 *   - Edge cases with null/missing values
 */
class UDPNotificationTest {

    // Helper - create a UDPNotification with default values
    private fun createNotification(
        cryType: String = "Hungry",
        detectionConfidence: Double = 0.95,
        classificationConfidence: Double? = 0.82,
        temperature: Double? = 21.5,
        humidity: Double? = 48.0,
        timestamp: String = "2026-03-18T14:30:00"
    ) = UDPNotification(
        cryType = cryType,
        detectionConfidence = detectionConfidence,
        classificationConfidence = classificationConfidence,
        temperature = temperature,
        humidity = humidity,
        timestamp = timestamp
    )


    // getCryTypeDisplay() Tests
    @Test
    fun `getCryTypeDisplay returns Hungry for hungry type`() {
        val notification = createNotification(cryType = "Hungry")
        assertEquals("Hungry", notification.getCryTypeDisplay())
    }

    @Test
    fun `getCryTypeDisplay returns Pain for pain type`() {
        val notification = createNotification(cryType = "Pain")
        assertEquals("Pain", notification.getCryTypeDisplay())
    }

    @Test
    fun `getCryTypeDisplay returns Crying for normal type`() {
        val notification = createNotification(cryType = "Normal")
        assertEquals("Crying", notification.getCryTypeDisplay())
    }


    // getDisplayConfidence() Tests
    @Test
    fun `getDisplayConfidence uses classification confidence when available`() {
        val notification = createNotification(
            classificationConfidence = 0.75,
            detectionConfidence = 0.95
        )
        assertEquals(75, notification.getDisplayConfidence())
    }

    @Test
    fun `getDisplayConfidence falls back to detection confidence when classification is null`() {
        val notification = createNotification(
            classificationConfidence = null,
            detectionConfidence = 0.92
        )
        assertEquals(92, notification.getDisplayConfidence())
    }

    @Test
    fun `getDisplayConfidence handles full confidence`() {
        val notification = createNotification(classificationConfidence = 1.0)
        assertEquals(100, notification.getDisplayConfidence())
    }

    @Test
    fun `getDisplayConfidence handles low confidence`() {
        val notification = createNotification(classificationConfidence = 0.10)
        assertEquals(10, notification.getDisplayConfidence())
    }


    // Null Safety Tests
    @Test
    fun `notification with null temperature is valid`() {
        val notification = createNotification(temperature = null)
        assertNull(notification.temperature)
    }

    @Test
    fun `notification with null humidity is valid`() {
        val notification = createNotification(humidity = null)
        assertNull(notification.humidity)
    }

    @Test
    fun `notification preserves all fields correctly`() {
        val notification = createNotification(
            cryType = "Pain",
            detectionConfidence = 0.98,
            classificationConfidence = 0.88,
            temperature = 23.1,
            humidity = 55.0,
            timestamp = "2026-03-18T15:00:00"
        )
        assertEquals("Pain", notification.cryType)
        assertEquals(0.98, notification.detectionConfidence, 0.001)
        assertEquals(0.88, notification.classificationConfidence!!, 0.001)
        assertEquals(23.1, notification.temperature!!, 0.001)
        assertEquals(55.0, notification.humidity!!, 0.001)
        assertEquals("2026-03-18T15:00:00", notification.timestamp)
    }
}