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
import android.view.animation.AnimationUtils
import android.widget.ImageView

/**
 * Main Activity - Entry point of the EchoCare app.
 *
 * Manages two states:
 * 1. LANDING PAGE (shown initially):
 *    - Welcome screen with monitoring controls (Start/Stop)
 *    - "Get Started" button to enter the dashboard
 *
 * 2. MAIN APP (shown after "Get Started"):
 *    - NavHostFragment hosting 4 page fragments
 *    - BottomNavigationView with Dashboard, Charts, Science, Info tabs
 *    - UDP listener service continues running in background regardless of screen
 *
 * Preserves all functionality:
 *    - UDP service start/stop
 *    - Notification permission handling
 *    - Broadcast receiver for cry detection events
 *    - Service status updates
 */
class MainActivity : AppCompatActivity() {

    // =========================================================================
    // Landing Page UI Elements
    // =========================================================================
    private lateinit var layoutLanding: View
    private lateinit var statusTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var btnGetStarted: Button
    private lateinit var iconSoundwave: ImageView
    private lateinit var taglineTextView: TextView
    private lateinit var titleTextView: TextView

    // =========================================================================
    // Main App UI Elements
    // =========================================================================
    private lateinit var layoutMainApp: View
    private lateinit var bottomNavigation: BottomNavigationView

    // =========================================================================
    // Broadcast Receiver
    // =========================================================================
    private val serviceStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                IntentActions.SERVICE_STATUS_CHANGED -> {
                    val isRunning = intent.getBooleanExtra("is_running", false)
                    updateUI(isRunning)
                }
            }
        }
    }

    // =========================================================================
    // Permission Launcher
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

        // Add landing page animations
        animateLandingPage()

        // Set up landing page controls (service start/stop)
        setupButtonListeners()

        // Set up navigation for main app area
        setupNavigation()

        // Set up "Get Started" button to transition landing → main app
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
        // Landing page views
        layoutLanding = findViewById(R.id.layoutLanding)
        statusTextView = findViewById(R.id.statusTextView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        btnGetStarted = findViewById(R.id.btnGetStarted)
        iconSoundwave = findViewById(R.id.iconSoundwave)
        taglineTextView = findViewById(R.id.taglineTextView)
        titleTextView = findViewById(R.id.titleTextView)

        // Main app views
        layoutMainApp = findViewById(R.id.layoutMainApp)
        bottomNavigation = findViewById(R.id.bottomNavigation)

    }

    private fun animateLandingPage() {
        // Icon: fade in + scale up
        val iconAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in_scale)
        iconSoundwave.startAnimation(iconAnim)

        // Title: fade in + slide up (starts 300ms after icon)
        val titleAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in_slide_up)
        titleTextView.startAnimation(titleAnim)

        // Tagline: fade in + slide up (starts 600ms after icon)
        val taglineAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in_slide_up)
        taglineAnim.startOffset = 300
        taglineTextView.startAnimation(taglineAnim)

        // Sound wave icon: subtle continuous pulse
        iconSoundwave.postDelayed({
            val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse)
            iconSoundwave.startAnimation(pulseAnim)
        }, 1200) // Start pulsing after initial animation completes
    }

    // =========================================================================
    // Navigation Setup
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
    // Service Management
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
                    "EchoCare needs notification permission for real-time alerts",
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

        } catch (e: Exception) {
            Toast.makeText(this, "Failed to stop monitoring: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // =========================================================================
    // UI Updates
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
    // Lifecycle
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