package com.echocare.app.ui.science

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import androidx.fragment.app.Fragment
import com.echocare.app.R
import com.google.android.material.button.MaterialButton

/**
 * Fragment for the "Science Behind EchoCare".
 *
 * Displays educational content about how EchoCare works:
 *   1. Mel-spectrograms explanation
 *   2. Cry type spectrograms with audio playback (Hungry, Pain, Normal)
 *   3. Baby Chillanto dataset information
 *   4. Privacy and edge processing explanation
 *   5. Future work
 *
 * Uses MediaPlayer for audio playback of sample cry clips.
 * No ViewModel needed - all content is static.
 */
class ScienceFragment : Fragment() {

    private val TAG = "ScienceFragment"

    // MediaPlayer for cry audio playback
    private var mediaPlayer: MediaPlayer? = null

    // Track which button is currently playing
    private var currentlyPlayingButton: MaterialButton? = null

    // Play buttons
    private lateinit var btnPlayHungry: MaterialButton
    private lateinit var btnPlayPain: MaterialButton
    private lateinit var btnPlayNormal: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_science, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupPlayButtons()
    }

    // View Initialization
    private fun initializeViews(view: View) {
        btnPlayHungry = view.findViewById(R.id.btnPlayHungry)
        btnPlayPain = view.findViewById(R.id.btnPlayPain)
        btnPlayNormal = view.findViewById(R.id.btnPlayNormal)
    }

    // Audio Playback Setup
    /**
     * Set up play button click listeners.
     * Each button plays a sample cry audio clip of that type.
     * Only one clip plays at a time - tapping another stops the current one.
     */
    private fun setupPlayButtons() {
        btnPlayHungry.setOnClickListener {
            togglePlayback(R.raw.cry_hungry, btnPlayHungry)
        }

        btnPlayPain.setOnClickListener {
            togglePlayback(R.raw.cry_pain, btnPlayPain)
        }

        btnPlayNormal.setOnClickListener {
            togglePlayback(R.raw.cry_normal, btnPlayNormal)
        }
    }

    /**
     * Toggle audio playback for a cry type.
     * If the same button is tapped again, stop playback.
     * If a different button is tapped, stop current and play new.
     *
     * @param audioResId Raw resource ID of the audio file
     * @param button The MaterialButton that was tapped
     */
    private fun togglePlayback(audioResId: Int, button: MaterialButton) {
        // If this button is already playing, stop it
        if (currentlyPlayingButton == button && mediaPlayer?.isPlaying == true) {
            stopPlayback()
            return
        }

        // Stop any current playback
        stopPlayback()

        // Start new playback
        try {
            mediaPlayer = MediaPlayer.create(requireContext(), audioResId).apply {
                setOnCompletionListener {
                    // Reset button when audio finishes
                    resetButton(button)
                    currentlyPlayingButton = null
                }
                start()
            }

            // Update button to show "Stop"
            button.text = getString(R.string.stop_cry)
            button.setIconResource(android.R.drawable.ic_media_pause)
            currentlyPlayingButton = button

            Log.d(TAG, "Playing audio: $audioResId")

        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio: ${e.message}")
        }
    }

    /**
     * Stop current audio playback and reset the active button.
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

            // Reset the currently playing button
            currentlyPlayingButton?.let { resetButton(it) }
            currentlyPlayingButton = null

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback: ${e.message}")
        }
    }

    /**
     * Reset a play button to its default "Play" state.
     */
    private fun resetButton(button: MaterialButton) {
        button.text = getString(R.string.play_cry)
        button.setIconResource(android.R.drawable.ic_media_play)
    }

    // Lifecycle - Clean up MediaPlayer
    override fun onPause() {
        super.onPause()
        // Stop playback when user navigates away
        stopPlayback()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Release MediaPlayer resources
        stopPlayback()
    }
}