package com.echocare.app.ui.charts

import com.echocare.app.data.model.CryEvent
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ChartDataHelper: the computation logic behind the Charts page.
 *
 * Tests:
 *   - computeDistribution(): counts events per cry type
 *   - computeTimeline(): counts events per hour of day
 *   - findMostCommonType(): identifies the dominant cry type
 *   - extractHour(): parses hour from timestamp strings
 */
class ChartsViewModelTest {

    // Helper - create test events
    private fun createEvent(
        cryType: String = "Hungry",
        timestamp: String = "2026-03-18 14:30:00"
    ) = CryEvent(
        id = 1,
        timestamp = timestamp,
        cryType = cryType,
        detectionConfidence = 0.95,
        classificationConfidence = 0.82,
        temperature = 21.0,
        humidity = 50.0
    )


    // computeDistribution() Tests
    @Test
    fun `computeDistribution counts each cry type correctly`() {
        val events = listOf(
            createEvent(cryType = "Hungry"),
            createEvent(cryType = "Hungry"),
            createEvent(cryType = "Pain"),
            createEvent(cryType = "Normal"),
            createEvent(cryType = "Normal"),
            createEvent(cryType = "Normal")
        )

        val result = ChartDataHelper.computeDistribution(events)

        assertEquals(2, result["Hungry"])
        assertEquals(1, result["Pain"])
        assertEquals(3, result["Normal"])
    }

    @Test
    fun `computeDistribution returns zeros for empty list`() {
        val result = ChartDataHelper.computeDistribution(emptyList())

        assertEquals(0, result["Hungry"])
        assertEquals(0, result["Pain"])
        assertEquals(0, result["Normal"])
    }

    @Test
    fun `computeDistribution handles single cry type`() {
        val events = listOf(
            createEvent(cryType = "Pain"),
            createEvent(cryType = "Pain"),
            createEvent(cryType = "Pain")
        )

        val result = ChartDataHelper.computeDistribution(events)

        assertEquals(0, result["Hungry"])
        assertEquals(3, result["Pain"])
        assertEquals(0, result["Normal"])
    }


    // computeTimeline() Tests
    @Test
    fun `computeTimeline counts events per hour correctly`() {
        val events = listOf(
            createEvent(timestamp = "2026-03-18 08:30:00"),
            createEvent(timestamp = "2026-03-18 08:45:00"),
            createEvent(timestamp = "2026-03-18 14:00:00"),
            createEvent(timestamp = "2026-03-18 22:15:00")
        )

        val result = ChartDataHelper.computeTimeline(events)

        assertEquals(2, result[8])   // Two events at 8 AM
        assertEquals(1, result[14])  // One event at 2 PM
        assertEquals(1, result[22])  // One event at 10 PM
        assertEquals(0, result[0])   // No events at midnight
        assertEquals(0, result[12])  // No events at noon
    }

    @Test
    fun `computeTimeline initialises all 24 hours to zero`() {
        val result = ChartDataHelper.computeTimeline(emptyList())

        assertEquals(24, result.size)
        for (hour in 0..23) {
            assertEquals(0, result[hour])
        }
    }

    @Test
    fun `computeTimeline handles midnight correctly`() {
        val events = listOf(
            createEvent(timestamp = "2026-03-18 00:05:00")
        )

        val result = ChartDataHelper.computeTimeline(events)
        assertEquals(1, result[0])
    }

    @Test
    fun `computeTimeline handles 11 PM correctly`() {
        val events = listOf(
            createEvent(timestamp = "2026-03-18 23:59:00")
        )

        val result = ChartDataHelper.computeTimeline(events)
        assertEquals(1, result[23])
    }


    // findMostCommonType() Tests
    @Test
    fun `findMostCommonType returns dominant type`() {
        val events = listOf(
            createEvent(cryType = "Hungry"),
            createEvent(cryType = "Hungry"),
            createEvent(cryType = "Hungry"),
            createEvent(cryType = "Pain"),
            createEvent(cryType = "Normal")
        )

        assertEquals("Hungry", ChartDataHelper.findMostCommonType(events))
    }

    @Test
    fun `findMostCommonType returns dash for empty list`() {
        assertEquals("-", ChartDataHelper.findMostCommonType(emptyList()))
    }

    @Test
    fun `findMostCommonType handles single event`() {
        val events = listOf(createEvent(cryType = "Pain"))
        assertEquals("Pain", ChartDataHelper.findMostCommonType(events))
    }


    // extractHour() Tests
    @Test
    fun `extractHour parses space-separated timestamp`() {
        assertEquals(14, ChartDataHelper.extractHour("2026-03-18 14:30:00"))
    }

    @Test
    fun `extractHour parses ISO format timestamp`() {
        assertEquals(8, ChartDataHelper.extractHour("2026-03-18T08:15:00"))
    }

    @Test
    fun `extractHour handles midnight`() {
        assertEquals(0, ChartDataHelper.extractHour("2026-03-18 00:00:00"))
    }

    @Test
    fun `extractHour handles end of day`() {
        assertEquals(23, ChartDataHelper.extractHour("2026-03-18 23:59:59"))
    }

    @Test
    fun `extractHour returns negative one for invalid format`() {
        assertEquals(-1, ChartDataHelper.extractHour("invalid"))
    }

    @Test
    fun `extractHour returns negative one for empty string`() {
        assertEquals(-1, ChartDataHelper.extractHour(""))
    }
}