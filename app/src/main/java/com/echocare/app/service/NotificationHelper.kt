package com.echocare.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.echocare.app.MainActivity
import com.echocare.app.R
import com.echocare.app.data.model.UDPNotification
import com.echocare.app.util.AppConstants

/**
 * Helper class for creating and showing notifications
 */
class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /**
     * Show notification when baby cry is detected
     */
    fun showCryNotification(notification: UDPNotification) {
        // Check if notifications are enabled in preferences
        val prefs = context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean(AppConstants.PREF_NOTIFICATIONS_ENABLED, true)
        val vibrationEnabled = prefs.getBoolean(AppConstants.PREF_VIBRATION_ENABLED, true)

        if (!notificationsEnabled) {
            return
        }

        // Create intent to open app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification message
        val title = context.getString(R.string.notif_cry_title)
        val message = if (notification.classificationConfidence != null &&
            notification.classificationConfidence >= 0.70) {
            // High confidence - show cry type
            context.getString(
                R.string.notif_cry_message,
                notification.getDisplayConfidence(),
                notification.getCryTypeDisplay()
            )
        } else {
            // Low confidence - just say crying detected
            context.getString(
                R.string.notif_cry_message_low_confidence
            )
        }

        // Create notification
        val notificationBuilder = NotificationCompat.Builder(
            context,
            AppConstants.NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Dismiss when tapped
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Add additional info as expanded content
        notification.temperature?.let { temp ->
            notification.humidity?.let { humidity ->
                val expandedText = "$message\n\n🌡️ ${String.format("%.1f°C", temp)}  💧 ${String.format("%.1f%%", humidity)}"
                notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            }
        }

        // Show notification with unique ID each time
        notificationManager.notify(
            System.currentTimeMillis().toInt(),
            notificationBuilder.build()
        )

        // Trigger cry-type-specific vibration if enabled
        if (vibrationEnabled) {
            triggerVibration(notification.cryType)
        }

        android.util.Log.d("NotificationHelper", "Notification sent: $title")
    }

    /**
     * Trigger cry-type-specific haptic feedback vibration.
     *
     * Each cry type has a distinct vibration pattern so deaf/HoH parents
     * can identify the cry type through touch alone:
     *   - Hungry: Short rapid pulses (rhythmic, like the feeding cry pattern)
     *   - Pain: Long sustained vibration (urgent, attention-grabbing)
     *   - Normal: Regular medium pulses (gentle, informational)
     *
     * @param cryType The detected cry type ("Hungry", "Pain", or "Normal")
     */
    private fun triggerVibration(cryType: String) {
        try {
            // Select vibration pattern based on cry type
            val pattern = when (cryType.lowercase()) {
                "hungry" -> AppConstants.VIBRATION_PATTERN_HUNGRY
                "pain" -> AppConstants.VIBRATION_PATTERN_PAIN
                else -> AppConstants.VIBRATION_PATTERN_NORMAL
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Modern vibration API (Android 8.0+)
                val vibrationEffect = VibrationEffect.createWaveform(
                    pattern,
                    -1 // Don't repeat
                )
                vibrator.vibrate(vibrationEffect)
            } else {
                // Legacy vibration API
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }

            android.util.Log.d("NotificationHelper", "Vibration: $cryType pattern")

        } catch (e: Exception) {
            // Vibration failed but don't crash the app
            android.util.Log.e("NotificationHelper", "Vibration failed: ${e.message}")
        }
    }

    /**
     * Cancel all notifications
     */
    fun cancelAll() {
        notificationManager.cancelAll()
    }
}