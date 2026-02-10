package com.echocare.app.data.model

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Represents a single cry event from the database
 * Matches the structure returned by Pi's FastAPI backend
 * Maps to JSON from GET /recent-events:
 *  {
 *     "id": 15,
 *     "timestamp": "2026-01-22T10:30:45",
 *     "cry_type": "Hungry",
 *     "detection_confidence": 0.9234,
 *     "classification_confidence": 0.8567,
 *     "temperature": 22.3,
 *     "humidity": 48.5
 *  }
 */
data class CryEvent(
    @SerializedName("id")
    val id: Int,

    @SerializedName("timestamp")
    val timestamp: String,  // ISO 8601 format: "2026-01-22T10:30:45.123456"

    @SerializedName("cry_type")
    val cryType: String,  // "hungry", "pain", "normal"

    @SerializedName("detection_confidence")
    val detectionConfidence: Double,  // 0.0 - 1.0

    @SerializedName("classification_confidence")
    val classificationConfidence: Double?,  // 0.0 - 1.0, null for "normal"

    @SerializedName("temperature")
    val temperature: Double?,  // Celsius

    @SerializedName("humidity")
    val humidity: Double?  // Percentage 0-100
) {
    /**
     * Format timestamp for display
     * "2026-01-22T10:30:45" -> "Jan 22, 10:30 AM"
     */
    fun getFormattedTime(): String {
        return try {
            val dateTime = timestamp.split("T")
            val date = dateTime[0].split("-")
            val time = dateTime[1].split(":")

            val month = when (date[1]) {
                "01" -> "Jan"
                "02" -> "Feb"
                "03" -> "Mar"
                "04" -> "Apr"
                "05" -> "May"
                "06" -> "Jun"
                "07" -> "Jul"
                "08" -> "Aug"
                "09" -> "Sep"
                "10" -> "Oct"
                "11" -> "Nov"
                "12" -> "Dec"
                else -> date[1]
            }

            val hour = time[0].toInt()
            val minute = time[1]
            val amPm = if (hour >= 12) "PM" else "AM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }

            "$month ${date[2]}, $displayHour:$minute $amPm"
        } catch (e: Exception) {
            timestamp  // Return raw timestamp if parsing fails
        }
    }

    /**
     * Get display confidence percentage
     * Uses classification confidence if available, otherwise detection confidence
     */
    fun getDisplayConfidence(): Int {
        val conf = classificationConfidence ?: detectionConfidence
        return (conf * 100).toInt()
    }

    /**
     * Get cry type display name
     */
    fun getCryTypeDisplay(): String {
        return when (cryType.lowercase()) {
            "hungry" -> "Hungry"
            "pain" -> "Discomfort"
            "normal" -> "Normal"
            else -> cryType
        }
    }

    /**
     * Returns the classification confidence as a percentage integer (e.g. 85).
     * Null-safe: falls back to detection confidence if classification is null.
     * Used by CryEventAdapter for the confidence badge on each card.
     */
    fun classificationPercent(): Int {
        val conf = classificationConfidence ?: detectionConfidence
        return (conf * 100).toInt()
    }

    /**
     * Returns the detection confidence as a percentage integer.
     */
    fun detectionPercent(): Int = (detectionConfidence * 100).toInt()

    /**
     * Formats the timestamp into a full date/time string.
     * Example: "22 Jan 2026, 10:30"
     * Uses SimpleDateFormat for reliable parsing (handles microseconds etc).
     * Used by CryEventAdapter for the secondary date line on each card.
     */
    fun formattedDateTime(): String {
        return try {
            val cleanTimestamp = timestamp.split(".")[0]  // Remove microseconds if present
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val date = inputFormat.parse(cleanTimestamp)
            date?.let { outputFormat.format(it) } ?: timestamp
        } catch (e: Exception) {
            timestamp
        }
    }

    /**
     * Returns a relative time string like "2h ago", "Just now", etc.
     * Falls back to formattedDateTime() for events older than 7 days.
     * Used by CryEventAdapter for the primary time display on each card.
     */
    fun timeAgo(): String {
        return try {
            val cleanTimestamp = timestamp.split(".")[0]  // Remove microseconds if present
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(cleanTimestamp) ?: return timestamp
            val now = Date()
            val diffMs = now.time - date.time

            val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
            val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
            val days = TimeUnit.MILLISECONDS.toDays(diffMs)

            when {
                minutes < 1 -> "Just now"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                days < 7 -> "${days}d ago"
                else -> formattedDateTime()
            }
        } catch (e: Exception) {
            timestamp
        }
    }

    /**
     * Returns the temperature formatted with unit, or "N/A" if null.
     * Example: "22.3°C"
     * Used by CryEventAdapter for the temperature display on each card.
     */
    fun formattedTemperature(): String {
        return temperature?.let { String.format("%.1f°C", it) } ?: "N/A"
    }

    /**
     * Returns the humidity formatted with unit, or "N/A" if null.
     * Example: "48.5%"
     * Used by CryEventAdapter for the humidity display on each card.
     */
    fun formattedHumidity(): String {
        return humidity?.let { String.format("%.1f%%", it) } ?: "N/A"
    }
}

/**
 * Response from /recent-events endpoint
 */
data class RecentEventsResponse(
    @SerializedName("events")
    val events: List<CryEvent>,

    @SerializedName("count")
    val count: Int,

    @SerializedName("limit")
    val timeRange: Int
)

/**
 * Response from /statistics endpoint
 */
data class StatisticsResponse(
    @SerializedName("time_window_hours")
    val timeWindowHours: Int,

    @SerializedName("statistics")
    val statistics: Statistics,

    @SerializedName("generated_at")
    val generatedAt: String
)

/**
 * Nested statistics object inside StatisticsResponse.
 * Matches the Pi's actual "statistics" JSON object.
 */
data class Statistics(
    @SerializedName("total_cries")
    val totalCries: Int,

    @SerializedName("by_type")
    val byType: Map<String, Int>,      // e.g. {"Hungry": 8, "Pain": 5, "Normal": 2}

    @SerializedName("average_confidence")
    val averageConfidence: Double
)

/**
 * Response from /status endpoint
 */
data class StatusResponse(
    @SerializedName("status")
    val status: String,  // "online"

    @SerializedName("timestamp")
    val timestamp: String,

    @SerializedName("database_connected")
    val databaseConnected: Boolean,

    @SerializedName("total_events")
    val totalEvents: Int,

    @SerializedName("pi_info")
    val piInfo: PiInfo?
)

data class PiInfo(
    @SerializedName("hostname")
    val hostname: String,

    @SerializedName("system")
    val system: String
)

/**
 * UDP notification message from Pi broadcast
 */
data class UDPNotification(
    @SerializedName("cry_type")
    val cryType: String,

    @SerializedName("detection_confidence")
    val detectionConfidence: Double,

    @SerializedName("classification_confidence")
    val classificationConfidence: Double?,

    @SerializedName("temperature")
    val temperature: Double?,

    @SerializedName("humidity")
    val humidity: Double?,

    @SerializedName("timestamp")
    val timestamp: String
) {
    fun getCryTypeDisplay(): String {
        return when (cryType.lowercase()) {
            "hungry" -> "Hungry"
            "pain" -> "Discomfort"
            "normal" -> "Crying"
            else -> cryType
        }
    }

    fun getDisplayConfidence(): Int {
        val conf = classificationConfidence ?: detectionConfidence
        return (conf * 100).toInt()
    }
}
