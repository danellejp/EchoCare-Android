package com.echocare.app.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.echocare.app.R
import com.echocare.app.data.model.CryEvent

/**
 * RecyclerView adapter for displaying cry events on the Dashboard.
 *
 * Uses ListAdapter with DiffUtil for efficient updates — only items
 * that actually changed get rebound, which avoids flickering on refresh.
 *
 * Each item shows:
 *   - Cry type with color-coded indicator
 *   - Relative time (e.g. "2h ago")
 *   - Full date/time
 *   - Classification confidence percentage
 *   - Temperature and humidity readings
 */
class CryEventAdapter : ListAdapter<CryEvent, CryEventAdapter.CryEventViewHolder>(CryEventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CryEventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cry_event, parent, false)
        return CryEventViewHolder(view)
    }

    override fun onBindViewHolder(holder: CryEventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for a single cry event card.
     */
    class CryEventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvCryType: TextView = itemView.findViewById(R.id.tvCryType)
        private val tvTimeAgo: TextView = itemView.findViewById(R.id.tvTimeAgo)
        private val tvDateTime: TextView = itemView.findViewById(R.id.tvDateTime)
        private val tvConfidence: TextView = itemView.findViewById(R.id.tvConfidence)
        private val tvTemperature: TextView = itemView.findViewById(R.id.tvTemperature)
        private val tvHumidity: TextView = itemView.findViewById(R.id.tvHumidity)
        private val viewTypeIndicator: View = itemView.findViewById(R.id.viewTypeIndicator)

        fun bind(event: CryEvent) {
            // Cry type label and color
            tvCryType.text = event.cryType
            val typeColor = getCryTypeColor(event.cryType)
            tvCryType.setTextColor(ContextCompat.getColor(itemView.context, typeColor))
            viewTypeIndicator.setBackgroundColor(ContextCompat.getColor(itemView.context, typeColor))

            // Time information
            tvTimeAgo.text = event.timeAgo()
            tvDateTime.text = event.formattedDateTime()

            // Confidence score
            tvConfidence.text = "${event.classificationPercent()}%"

            // Environment data
            tvTemperature.text = event.formattedTemperature()
            tvHumidity.text = event.formattedHumidity()
        }

        /**
         * Maps cry type string to a color resource.
         */
        private fun getCryTypeColor(cryType: String): Int {
            return when (cryType.lowercase()) {
                "hungry" -> R.color.hungry_green
                "pain" -> R.color.pain_red
                "normal" -> R.color.normal_blue
                else -> R.color.normal_blue
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates.
     * Compares items by ID and checks content equality.
     */
    class CryEventDiffCallback : DiffUtil.ItemCallback<CryEvent>() {
        override fun areItemsTheSame(oldItem: CryEvent, newItem: CryEvent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CryEvent, newItem: CryEvent): Boolean {
            return oldItem == newItem
        }
    }
}