package com.echocare.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a single cry event from the database
 * Matches the structure returned by Pi's FastAPI backend
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
}

/**
 * Response from /recent-events endpoint
 */
data class RecentEventsResponse(
    @SerializedName("events")
    val events: List<CryEvent>,

    @SerializedName("count")
    val count: Int,

    @SerializedName("time_range")
    val timeRange: String
)

/**
 * Response from /statistics endpoint
 */
data class StatisticsResponse(
    @SerializedName("total_events")
    val totalEvents: Int,

    @SerializedName("time_period")
    val timePeriod: String,

    @SerializedName("cry_type_distribution")
    val cryTypeDistribution: Map<String, Int>,

    @SerializedName("average_temperature")
    val averageTemperature: Double?,

    @SerializedName("average_humidity")
    val averageHumidity: Double?,

    @SerializedName("hourly_distribution")
    val hourlyDistribution: Map<String, Int>
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
    val piInfo: PiInfo
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
    @SerializedName("type")
    val type: String,  // "cry_detected"

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
