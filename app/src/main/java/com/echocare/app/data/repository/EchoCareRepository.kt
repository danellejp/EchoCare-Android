package com.echocare.app.data.repository

import android.util.Log
import com.echocare.app.data.api.RetrofitClient
import com.echocare.app.data.model.CryEvent
import com.echocare.app.data.model.StatusResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Repository for all EchoCare data operations.
 *
 * Responsibilities:
 *   - Fetches cry events from Pi via Retrofit
 *   - Filters events by time range (client-side, since Pi endpoint doesn't support it)
 *   - Filters events by cry type (client-side)
 *   - Handles errors and returns Result<T> for clean error propagation
 *
 * All network calls run on Dispatchers.IO.
 */
class EchoCareRepository {

    private val apiService = RetrofitClient.apiService
    private val TAG = "EchoCareRepository"

    /**
     * Fetches cry events from the Pi and applies client-side filtering.
     *
     * @param hoursBack Number of hours to look back (24 for "past 24h", 168 for "past 7 days")
     * @param cryTypeFilter Optional cry type filter: "Hungry", "Pain", "Normal", or null for all
     * @return Result containing filtered list of CryEvent, or an error
     */
    suspend fun getCryEvents(
        hoursBack: Int = 168,
        cryTypeFilter: String? = null
    ): Result<List<CryEvent>> = withContext(Dispatchers.IO) {
        try {
            // Fetch max events from Pi (we filter client-side)
            val response = apiService.getRecentEvents(limit = 50)

            if (response.isSuccessful && response.body() != null) {
                val allEvents = response.body()!!.events

                // Step 1: Filter by time range
                val cutoffTime = getCutoffTimestamp(hoursBack)
                val timeFiltered = allEvents.filter { event ->
                    event.timestamp >= cutoffTime
                }

                // Step 2: Filter by cry type (if specified)
                val finalEvents = if (cryTypeFilter != null) {
                    timeFiltered.filter { it.cryType.equals(cryTypeFilter, ignoreCase = true) }
                } else {
                    timeFiltered
                }

                Log.d(TAG, "Fetched ${allEvents.size} events, " +
                        "after time filter: ${timeFiltered.size}, " +
                        "after type filter: ${finalEvents.size}")

                Result.success(finalEvents)
            } else {
                val errorMsg = "API error: ${response.code()} - ${response.message()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error fetching cry events", e)
            Result.failure(e)
        }
    }

    /**
     * Checks if the Pi is online and reachable.
     *
     * @return Result containing StatusResponse, or an error if unreachable
     */
    suspend fun getPiStatus(): Result<StatusResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getStatus()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Pi returned ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cannot reach Pi", e)
            Result.failure(e)
        }
    }

    /**
     * Syncs the Pi's clock with the phone's current time.
     * Called once when the dashboard first loads.
     */
    suspend fun syncPiTime(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val currentTime = formatter.format(Date())

            val response = apiService.setTime(mapOf("datetime" to currentTime))
            if (response.isSuccessful) {
                Log.d("EchoCareRepository", "Pi time synced to: $currentTime")
                Result.success(true)
            } else {
                Result.failure(Exception("Time sync failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("EchoCareRepository", "Time sync error", e)
            Result.failure(e)
        }
    }

    /**
     * Calculates the ISO timestamp for the cutoff point.
     * Events before this timestamp will be excluded.
     *
     * @param hoursBack How many hours to look back from now
     * @return ISO format timestamp string (e.g. "2026-01-15T14:00:00")
     */
    private fun getCutoffTimestamp(hoursBack: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, -hoursBack)
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        return format.format(calendar.time)
    }
}