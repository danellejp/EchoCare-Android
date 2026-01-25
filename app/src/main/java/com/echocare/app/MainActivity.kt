package com.echocare.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.echocare.app.service.UDPListenerService
import com.echocare.app.util.AppConstants
import com.echocare.app.util.IntentActions

/**
 * Main Activity - Entry point of the app
 * Provides controls to start/stop the UDP monitoring service
 */
class MainActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var statusTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var lastCryTextView: TextView

    // Service status receiver
    private val serviceStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                IntentActions.SERVICE_STATUS_CHANGED -> {
                    val isRunning = intent.getBooleanExtra("is_running", false)
                    updateUI(isRunning)
                }
                IntentActions.CRY_DETECTED -> {
                    val cryType = intent.getStringExtra("cry_type") ?: "Unknown"
                    val confidence = intent.getIntExtra("confidence", 0)
                    val timestamp = intent.getStringExtra("timestamp") ?: ""

                    lastCryTextView.text = "Last cry: $cryType ($confidence%) at ${formatTimestamp(timestamp)}"
                }
            }
        }
    }

    // Permission launcher for notification permission (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            startMonitoringService()
        } else {
            Toast.makeText(this, "Notification permission denied. Alerts won't work.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        initializeViews()

        // Set up button click listeners
        setupButtonListeners()

        // Register broadcast receiver for service updates
        registerReceivers()

        // Update UI based on current service state
        updateUI(UDPListenerService.isRunning)

        // Check and request notification permission if needed
        checkNotificationPermission()
    }

    private fun initializeViews() {
        statusTextView = findViewById(R.id.statusTextView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        lastCryTextView = findViewById(R.id.lastCryTextView)

        // Set initial text
        lastCryTextView.text = "No cries detected yet"
    }

    private fun setupButtonListeners() {
        startButton.setOnClickListener {
            // Check notification permission before starting
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startMonitoringService()
                } else {
                    // Request permission
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                // Android 12 and below - no permission needed
                startMonitoringService()
            }
        }

        stopButton.setOnClickListener {
            stopMonitoringService()
        }
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(IntentActions.SERVICE_STATUS_CHANGED)
            addAction(IntentActions.CRY_DETECTED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serviceStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(serviceStatusReceiver, filter, RECEIVER_NOT_EXPORTED)
        }
    }

    private fun checkNotificationPermission() {
        // Check if notification permission is needed (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Show explanation
                Toast.makeText(
                    this,
                    "EchoCare needs notification permission to alert you when baby is crying",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun startMonitoringService() {
        try {
            val intent = Intent(this, UDPListenerService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            updateUI(true)

            Toast.makeText(this, "Monitoring started", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            updateUI(false)
            Toast.makeText(this, "Failed to start monitoring: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopMonitoringService() {
        try {
            val intent = Intent(this, UDPListenerService::class.java)
            stopService(intent)

            updateUI(false)

            Toast.makeText(this, "Monitoring stopped", Toast.LENGTH_SHORT).show()

            // Clear last cry text
            lastCryTextView.text = "No cries detected yet"

        } catch (e: Exception) {
            Toast.makeText(this, "Failed to stop monitoring: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateUI(isServiceRunning: Boolean) {
        if (isServiceRunning) {
            statusTextView.text = "Status: Monitoring Active"
            statusTextView.setTextColor(getColor(R.color.online_green))
            startButton.isEnabled = false
            stopButton.isEnabled = true
        } else {
            statusTextView.text = "Status: Not Monitoring"
            statusTextView.setTextColor(getColor(R.color.offline_red))
            startButton.isEnabled = true
            stopButton.isEnabled = false
        }
    }

    private fun formatTimestamp(timestamp: String): String {
        // Simple formatting - extract time from ISO format
        return try {
            val parts = timestamp.split("T")
            if (parts.size > 1) {
                val timePart = parts[1].split(".")[0] // Remove milliseconds
                timePart.substring(0, 5) // HH:MM
            } else {
                timestamp
            }
        } catch (e: Exception) {
            timestamp
        }
    }

    override fun onResume() {
        super.onResume()
        // Update UI when returning to app
        updateUI(UDPListenerService.isRunning)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister receiver to prevent memory leaks
        try {
            unregisterReceiver(serviceStatusReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }
}