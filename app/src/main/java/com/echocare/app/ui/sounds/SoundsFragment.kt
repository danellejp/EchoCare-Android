package com.echocare.app.ui.sounds

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.echocare.app.R
import com.google.android.material.button.MaterialButton

/**
 * Fragment for the Calming Sounds page.
 *
 * Allows parents to play White Noise, Brown Noise or Pink Noise to help their baby sleep
 * Audio loops continuously and keeps playing when the screen is locked
 * Only one sound plays at a time - switching sounds stops the current one
 *
 * Playing sounds from the phone does not interfere with the Pi's cry detection,
 * as the Pi uses its own microphone independently
 */
class SoundsFragment : Fragment() {

    private val TAG = "SoundsFragment"

    // MediaPlayer for audio playback
    private var mediaPlayer: MediaPlayer? = null

    // Track which sound is currently playing (null = nothing playing)
    private var currentlyPlaying: String? = null

    // Views
    private lateinit var btnWhiteNoise: MaterialButton
    private lateinit var btnBrownNoise: MaterialButton
    private lateinit var btnPinkNoise: MaterialButton
    private lateinit var txtNowPlaying: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sounds, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupButtons()
    }

    // =========================================================================
    // View Initialisation
    // =========================================================================

    private fun initializeViews(view: View) {
        btnWhiteNoise = view.findViewById(R.id.btnWhiteNoise)
        btnBrownNoise = view.findViewById(R.id.btnBrownNoise)
        btnPinkNoise = view.findViewById(R.id.btnPinkNoise)
        txtNowPlaying = view.findViewById(R.id.txtNowPlaying)
    }

    // =========================================================================
    // Button Setup
    // =========================================================================

    private fun setupButtons() {
        btnWhiteNoise.setOnClickListener {
            toggleSound("white", R.raw.white_noise, btnWhiteNoise)
        }

        btnBrownNoise.setOnClickListener {
            toggleSound("brown", R.raw.brown_noise, btnBrownNoise)
        }

        btnPinkNoise.setOnClickListener {
            toggleSound("pink", R.raw.pink_noise, btnPinkNoise)
        }
    }

    // =========================================================================
    // Playback Logic
    // =========================================================================

    /**
     * Toggle playback for a sound type.
     *
     * - If this sound is already playing -> stop it
     * - If a different sound is playing -> stop it and play this one
     * - If nothing is playing -> play this sound
     *
     * @param soundName Identifier ("white", "brown" or "pink")
     * @param audioResId Raw resource ID of the audio file
     * @param button The button that was tapped
     */
    private fun toggleSound(soundName: String, audioResId: Int, button: MaterialButton) {
        // If this sound is already playing, stop it
        if (currentlyPlaying == soundName) {
            stopPlayback()
            return
        }

        // Stop any current playback
        stopPlayback()

        // Start new playback
        try {
            mediaPlayer = MediaPlayer.create(requireContext(), audioResId).apply {
                isLooping = true  // Loop continuously
                setOnCompletionListener {
                    // This won't fire while looping, but acts as a safety net
                    stopPlayback()
                }
                start()
            }

            currentlyPlaying = soundName

            // Update UI
            button.text = getString(R.string.sounds_stop)
            button.setIconResource(android.R.drawable.ic_media_pause)

            val displayName = when (soundName) {
                "white" -> "☁️ White Noise"
                "brown" -> "🌊 Brown Noise"
                "pink" -> "🌸 Pink Noise"
                else -> soundName
            }
            txtNowPlaying.text = "♪ Now Playing: $displayName"
            txtNowPlaying.visibility = View.VISIBLE

            Log.d(TAG, "Playing: $soundName noise")

        } catch (e: Exception) {
            Log.e(TAG, "Error playing $soundName noise: ${e.message}")
        }
    }

    /**
     * Stop current audio playback and reset all buttons.
     */
    private fun stopPlayback() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
            currentlyPlaying = null

            // Reset all buttons
            resetButton(btnWhiteNoise)
            resetButton(btnBrownNoise)
            resetButton(btnPinkNoise)

            // Hide now playing indicator
            txtNowPlaying.visibility = View.GONE

            Log.d(TAG, "Playback stopped")

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback: ${e.message}")
        }
    }

    /**
     * Reset a button to its default "Play" state.
     */
    private fun resetButton(button: MaterialButton) {
        button.text = getString(R.string.sounds_play)
        button.setIconResource(android.R.drawable.ic_media_play)
    }

    // =========================================================================
    // Lifecycle - Audio continues on screen lock, stops on app close
    // =========================================================================

    override fun onDestroyView() {
        super.onDestroyView()
        // Stop playback when the fragment is destroyed (app closed)
        stopPlayback()
    }

    // Intentionally do NOT stop in onPause() so audio
    // continues playing when the screen locks or user switches apps
}