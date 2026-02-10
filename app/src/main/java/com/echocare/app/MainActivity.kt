package com.echocare.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.echocare.app.service.UDPListenerService
import com.echocare.app.util.AppConstants
import com.echocare.app.util.IntentActions
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Main Activity - Entry point of the EchoCare app.
 *
 * Manages two states:
 * 1. LANDING PAGE (shown initially):
 *    - Welcome screen with monitoring controls (Start/Stop from Milestone 4.2)
 *    - "Get Started" button to enter the dashboard
 *
 * 2. MAIN APP (shown after "Get Started"):
 *    - NavHostFragment hosting 4 page fragments
 *    - BottomNavigationView with Dashboard, Charts, Settings, Info tabs
 *    - UDP listener service continues running in background regardless of screen
 *
 * Preserves all functionality from Milestones 4.1-4.3:
 *    - UDP service start/stop
 *    - Notification permission handling
 *    - Broadcast receiver for cry detection events
 *    - Service status updates
 */
class MainActivity : AppCompatActivity() {

    // =========================================================================
    // Landing Page UI Elements (from Milestones 4.1-4.3)
    // =========================================================================
    private lateinit var layoutLanding: View
    private lateinit var statusTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var lastCryTextView: TextView
    private lateinit var btnGetStarted: Button

    // =========================================================================
    // Main App UI Elements (NEW - Milestone 4.4)
    // =========================================================================
    private lateinit var layoutMainApp: View
    private lateinit var bottomNavigation: BottomNavigationView

    // =========================================================================
    // Broadcast Receiver (from Milestone 4.2/4.3 - UNCHANGED)
    // =========================================================================
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

    // =========================================================================
    // Permission Launcher (from Milestone 4.3 - UNCHANGED)
    // =========================================================================
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

    // =========================================================================
    // Lifecycle
    // =========================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize all UI elements
        initializeViews()

        // Set up landing page controls (service start/stop)
        setupButtonListeners()

        // Set up navigation for main app area (NEW)
        setupNavigation()

        // Set up "Get Started" button to transition landing → main app (NEW)
        setupLandingToMainTransition()

        // Register broadcast receiver for service updates
        registerReceivers()

        // Update UI based on current service state
        updateUI(UDPListenerService.isRunning)

        // Check and request notification permission if needed
        checkNotificationPermission()
    }

    // =========================================================================
    // Initialization
    // =========================================================================

    private fun initializeViews() {
        // Landing page views (existing from 4.1-4.3)
        layoutLanding = findViewById(R.id.layoutLanding)
        statusTextView = findViewById(R.id.statusTextView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        lastCryTextView = findViewById(R.id.lastCryTextView)
        btnGetStarted = findViewById(R.id.btnGetStarted)

        // Main app views (NEW - Milestone 4.4)
        layoutMainApp = findViewById(R.id.layoutMainApp)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        // Set initial text
        lastCryTextView.text = "No cries detected yet"
    }

    // =========================================================================
    // Navigation Setup (NEW - Milestone 4.4)
    // =========================================================================

    /**
     * Sets up the Navigation component with BottomNavigationView.
     * The NavHostFragment hosts the 4 page fragments.
     * setupWithNavController() handles tab ↔ fragment switching automatically.
     */
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        bottomNavigation.setupWithNavController(navController)
    }

    /**
     * Sets up the "Get Started" button to transition from the landing page
     * to the main app (dashboard with bottom navigation).
     *
     * The UDP listener service keeps running regardless of this transition.
     */
    private fun setupLandingToMainTransition() {
        btnGetStarted.setOnClickListener {
            layoutLanding.visibility = View.GONE
            layoutMainApp.visibility = View.VISIBLE
        }
    }

    // =========================================================================
    // Service Management (from Milestones 4.2/4.3 - UNCHANGED)
    // =========================================================================

    private fun setupButtonListeners() {
        startButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startMonitoringService()
                } else {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
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
            lastCryTextView.text = "No cries detected yet"

        } catch (e: Exception) {
            Toast.makeText(this, "Failed to stop monitoring: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // =========================================================================
    // UI Updates (from Milestones 4.2/4.3 - UNCHANGED)
    // =========================================================================

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
        return try {
            val parts = timestamp.split("T")
            if (parts.size > 1) {
                val timePart = parts[1].split(".")[0]
                timePart.substring(0, 5)
            } else {
                timestamp
            }
        } catch (e: Exception) {
            timestamp
        }
    }

    // =========================================================================
    // Lifecycle (from Milestones 4.2/4.3 - UNCHANGED)
    // =========================================================================

    override fun onResume() {
        super.onResume()
        updateUI(UDPListenerService.isRunning)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(serviceStatusReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }
}