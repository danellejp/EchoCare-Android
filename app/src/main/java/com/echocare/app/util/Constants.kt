package com.echocare.app.util

/**
 * Network configuration constants for EchoCare system
 */
object NetworkConfig {
    // Raspberry Pi Configuration (EchoCare Network)
    const val PI_IP_ADDRESS = "192.168.4.1"
    const val PI_API_PORT = 8000
    const val PI_API_BASE_URL = "http://$PI_IP_ADDRESS:$PI_API_PORT/"

    // UDP Broadcasting Configuration
    const val UDP_LISTEN_PORT = 9999
    const val UDP_BROADCAST_ADDRESS = "192.168.4.255"

    // Network Timeouts (milliseconds)
    const val CONNECTION_TIMEOUT = 10000L  // 10 seconds
    const val READ_TIMEOUT = 30000L        // 30 seconds
    const val WRITE_TIMEOUT = 30000L       // 30 seconds

    // Pi Status Check Configuration
    const val PI_OFFLINE_THRESHOLD_MINUTES = 30  // Consider offline after 30 minutes
    const val PI_HEARTBEAT_INTERVAL_MINUTES = 2   // Temperature updates every 2 minutes

    // Retry Configuration
    const val MAX_RETRY_ATTEMPTS = 3
    const val RETRY_DELAY_MS = 2000L  // 2 seconds between retries
}

/**
 * App-wide constants
 */
object AppConstants {
    // Notification Configuration
    const val NOTIFICATION_CHANNEL_ID = "echocare_cry_alerts"
    const val NOTIFICATION_CHANNEL_NAME = "Cry Alerts"
    const val SERVICE_NOTIFICATION_ID = 1
    const val CRY_NOTIFICATION_ID = 2

    // Vibration Pattern (milliseconds)
    val VIBRATION_PATTERN = longArrayOf(
        0,      // Delay before starting
        500,    // Vibrate for 500ms
        200,    // Pause for 200ms
        500,    // Vibrate for 500ms
        200,    // Pause
        500     // Final vibration
    )

    // SharedPreferences Keys
    const val PREF_NAME = "echocare_preferences"
    const val PREF_NOTIFICATIONS_ENABLED = "notifications_enabled"
    const val PREF_VIBRATION_ENABLED = "vibration_enabled"
    const val PREF_THEME_MODE = "theme_mode"  // "light", "dark", "system"
    const val PREF_SERVICE_RUNNING = "service_running"

    // Dashboard Configuration
    const val DEFAULT_TIME_RANGE_HOURS = 24
    const val WEEK_TIME_RANGE_HOURS = 168  // 7 days
    const val DASHBOARD_REFRESH_INTERVAL_MS = 30000L  // 30 seconds

    // Cry Type Constants
    const val CRY_TYPE_HUNGRY = "hungry"
    const val CRY_TYPE_PAIN = "pain"
    const val CRY_TYPE_NORMAL = "normal"

    // Confidence Thresholds
    const val MIN_DETECTION_CONFIDENCE = 85  // 85%
    const val MIN_CLASSIFICATION_CONFIDENCE = 70  // 70%
}

/**
 * Environment information constants
 * Reference values for temperature and humidity
 */
object EnvironmentConstants {
    // Temperature (Celsius)
    const val TEMP_OPTIMAL_MIN = 16.0
    const val TEMP_OPTIMAL_MAX = 22.0
    const val TEMP_WARNING_LOW = 14.0
    const val TEMP_WARNING_HIGH = 24.0

    // Humidity (Percentage)
    const val HUMIDITY_OPTIMAL_MIN = 40.0
    const val HUMIDITY_OPTIMAL_MAX = 60.0
    const val HUMIDITY_WARNING_LOW = 30.0
    const val HUMIDITY_WARNING_HIGH = 70.0

    // Display Messages
    const val TEMP_TOO_LOW_MESSAGE = "Room is too cold for baby. Recommended: 16-22°C"
    const val TEMP_TOO_HIGH_MESSAGE = "Room is too warm for baby. Recommended: 16-22°C"
    const val TEMP_OPTIMAL_MESSAGE = "Room temperature is optimal for baby"

    const val HUMIDITY_TOO_LOW_MESSAGE = "Air is too dry. Recommended: 40-60%"
    const val HUMIDITY_TOO_HIGH_MESSAGE = "Air is too humid. Recommended: 40-60%"
    const val HUMIDITY_OPTIMAL_MESSAGE = "Humidity level is optimal for baby"
}

/**
 * Intent action constants for broadcasts
 */
object IntentActions {
    const val CRY_DETECTED = "com.echocare.app.CRY_DETECTED"
    const val SERVICE_STATUS_CHANGED = "com.echocare.app.SERVICE_STATUS"
    const val DATA_UPDATED = "com.echocare.app.DATA_UPDATED"
}
