package com.echocare.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.echocare.app.MainActivity
import com.echocare.app.R
import com.echocare.app.data.model.UDPNotification
import com.echocare.app.util.AppConstants
import com.echocare.app.util.IntentActions
import com.echocare.app.util.NetworkConfig
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Foreground service that listens for UDP broadcasts from Raspberry Pi
 * Runs continuously in the background to receive real-time cry notifications
 */
class UDPListenerService : Service() {

    companion object {
        private const val TAG = "UDPListenerService"
        private const val NOTIFICATION_ID = AppConstants.SERVICE_NOTIFICATION_ID

        // Service state
        var isRunning = false
            private set
    }

    // Coroutine scope for managing async operations
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // UDP socket for receiving broadcasts
    private var udpSocket: DatagramSocket? = null

    // Coroutine job for the listening loop
    private var listenerJob: Job? = null

    // Wake lock to keep service running
    private var wakeLock: PowerManager.WakeLock? = null

    // Helper for managing notifications
    private lateinit var notificationHelper: NotificationHelper

    // JSON parser
    private val gson = Gson()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        // Initialize notification helper
        notificationHelper = NotificationHelper(this)

        // Create notification channels
        createNotificationChannels()

        // Acquire wake lock to prevent service from being killed
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service start command received")

        // Start as foreground service with persistent notification
        startForeground(NOTIFICATION_ID, createServiceNotification())

        // Start UDP listener if not already running
        if (!isRunning) {
            startUDPListener()
            isRunning = true

            // Broadcast service status change
            sendBroadcast(Intent(IntentActions.SERVICE_STATUS_CHANGED).apply {
                putExtra("is_running", true)
            })
        }

        // If service is killed, restart it
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")

        // Stop UDP listener
        stopUDPListener()

        // Release wake lock
        releaseWakeLock()

        // Cancel all coroutines
        serviceScope.cancel()

        isRunning = false

        // Broadcast service status change
        sendBroadcast(Intent(IntentActions.SERVICE_STATUS_CHANGED).apply {
            putExtra("is_running", false)
        })
    }

    /**
     * Start listening for UDP broadcasts on port 9999
     */
    private fun startUDPListener() {
        Log.d(TAG, "Starting UDP listener on port ${NetworkConfig.UDP_LISTEN_PORT}")

        listenerJob = serviceScope.launch {
            try {
                // Create UDP socket bound to listen port
                udpSocket = DatagramSocket(NetworkConfig.UDP_LISTEN_PORT).apply {
                    broadcast = true
                    reuseAddress = true
                }

                Log.d(TAG, "UDP socket created successfully")

                // Buffer for receiving packets
                val buffer = ByteArray(2048)

                // Continuous listening loop
                while (isActive && isRunning) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)

                        // Block until packet received
                        udpSocket?.receive(packet)

                        // Extract message from packet
                        val message = String(packet.data, 0, packet.length)
                        val senderIP = packet.address.hostAddress

                        Log.d(TAG, "Received UDP packet from $senderIP: $message")

                        // Verify packet is from Pi
                        if (senderIP == NetworkConfig.PI_IP_ADDRESS ||
                            senderIP?.startsWith("192.168.4.") == true) {

                            // Parse and handle the notification
                            handleUDPMessage(message)
                        } else {
                            Log.w(TAG, "Ignored packet from unknown IP: $senderIP")
                        }

                    } catch (e: Exception) {
                        if (isActive) {
                            Log.e(TAG, "Error receiving UDP packet: ${e.message}")
                            // Small delay before retry
                            delay(1000)
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to create UDP socket: ${e.message}")
            } finally {
                // Clean up socket
                udpSocket?.close()
                udpSocket = null
                Log.d(TAG, "UDP socket closed")
            }
        }
    }

    /**
     * Stop the UDP listener
     */
    private fun stopUDPListener() {
        Log.d(TAG, "Stopping UDP listener")

        // Cancel listener job
        listenerJob?.cancel()
        listenerJob = null

        // Close socket
        udpSocket?.close()
        udpSocket = null
    }

    /**
     * Parse and handle incoming UDP message
     */
    private fun handleUDPMessage(message: String) {
        try {
            // Parse JSON to UDPNotification object
            val notification = gson.fromJson(message, UDPNotification::class.java)

            Log.d(TAG, "Parsed UDP: cry_type=${notification.cryType}, " +
                    "confidence=${notification.getDisplayConfidence()}%")

            // Verify it's a valid cry notification (has cry_type)
            if (!notification.cryType.isNullOrBlank()) {
                // Show notification to user
                notificationHelper.showCryNotification(notification)

                // Broadcast data update to MainActivity
                sendBroadcast(Intent(IntentActions.CRY_DETECTED).apply {
                    putExtra("cry_type", notification.getCryTypeDisplay())
                    putExtra("confidence", notification.getDisplayConfidence())
                    putExtra("temperature", notification.temperature ?: 0.0)
                    putExtra("humidity", notification.humidity ?: 0.0)
                    putExtra("timestamp", notification.timestamp)
                })

                Log.d(TAG, "✅ Cry notification sent: ${notification.getCryTypeDisplay()} " +
                        "(${notification.getDisplayConfidence()}%)")
            } else {
                Log.w(TAG, "Ignored notification with empty cry_type")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse UDP message: ${e.message}")
            Log.e(TAG, "Raw message: $message")
        }
    }

    /**
     * Create notification channels for Android 8.0+
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Service notification channel (low importance)
            val serviceChannel = NotificationChannel(
                AppConstants.NOTIFICATION_CHANNEL_ID + "_service",
                "Monitoring Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows that EchoCare is monitoring for baby cries"
                setShowBadge(false)
            }

            // Cry alert channel (high importance)
            val alertChannel = NotificationChannel(
                AppConstants.NOTIFICATION_CHANNEL_ID,
                AppConstants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when baby is crying"
                enableVibration(true)
                vibrationPattern = AppConstants.VIBRATION_PATTERN
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    /**
     * Create the persistent foreground service notification
     */
    private fun createServiceNotification(): Notification {
        // Intent to open app when notification is tapped
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, AppConstants.NOTIFICATION_CHANNEL_ID + "_service")
            .setContentTitle(getString(R.string.notif_service_title))
            .setContentText(getString(R.string.notif_service_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Can't be dismissed
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    /**
     * Acquire wake lock to keep service running
     */
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "EchoCare::UDPListenerWakeLock"
            ).apply {
                acquire(10 * 60 * 1000L) // 10 minutes
            }
            Log.d(TAG, "Wake lock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock: ${e.message}")
        }
    }

    /**
     * Release wake lock
     */
    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "Wake lock released")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release wake lock: ${e.message}")
        }
    }
}