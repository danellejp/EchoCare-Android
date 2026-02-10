package com.echocare.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

/**
 * Placeholder fragment for the Settings page.
 */
class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val textView = TextView(requireContext()).apply {
            text = "Settings\n\nComing in Milestone 4.6"
            textSize = 18f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(48, 200, 48, 48)
        }
        return textView
    }
}