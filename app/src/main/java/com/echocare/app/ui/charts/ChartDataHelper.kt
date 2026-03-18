package com.echocare.app.ui.charts

import com.echocare.app.data.model.CryEvent

/**
 * Helper object for computing chart data from cry events.
 * Extracted from ChartsViewModel for testability.
 *
 * Contains pure functions with no Android dependencies,
 * making them easy to unit test.
 */
object ChartDataHelper {

    /**
     * Compute cry type distribution from a list of events.
     * Returns a map of display name --> count.
     *
     * @param events List of cry events
     * @return Map of "Hungry" --> count, "Pain" --> count, "Normal" --> count
     */
    fun computeDistribution(events: List<CryEvent>): Map<String, Int> {
        val distribution = mutableMapOf(
            "Hungry" to 0,
            "Pain" to 0,
            "Normal" to 0
        )

        for (event in events) {
            val type = event.getCryTypeDisplay()
            distribution[type] = (distribution[type] ?: 0) + 1
        }

        return distribution
    }

    /**
     * Compute cry timeline - count of cries per hour of day (0–23).
     *
     * @param events List of cry events
     * @return Map of hour (0–23) --> count
     */
    fun computeTimeline(events: List<CryEvent>): Map<Int, Int> {
        val timeline = mutableMapOf<Int, Int>()
        for (h in 0..23) {
            timeline[h] = 0
        }

        for (event in events) {
            val hour = extractHour(event.timestamp)
            if (hour in 0..23) {
                timeline[hour] = (timeline[hour] ?: 0) + 1
            }
        }

        return timeline
    }

    /**
     * Find the most common cry type from a list of events.
     *
     * @param events List of cry events
     * @return Display name of the most common type, or "-" if empty
     */
    fun findMostCommonType(events: List<CryEvent>): String {
        if (events.isEmpty()) return "-"

        val typeCounts = events.groupBy { it.getCryTypeDisplay() }
            .mapValues { it.value.size }
        return typeCounts.maxByOrNull { it.value }?.key ?: "-"
    }

    /**
     * Extract hour (0–23) from a timestamp string.
     * Supports: "2026-03-18 14:30:00" and "2026-03-18T14:30:00"
     *
     * @param timestamp Timestamp string
     * @return Hour (0-23) or -1 if parsing fails
     */
    fun extractHour(timestamp: String): Int {
        return try {
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