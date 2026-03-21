package com.echocare.app.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.echocare.app.data.model.CryEvent
import java.io.File
import java.io.FileWriter
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for downloading cry event data as a plain text file/report.
 *
 * Generates a text file containing all cry events for a given time period
 * and saves it to the device's Downloads folder. Parents can later share
 * this report with a paediatrician when connected to the internet.
 *
 * Content format:
 *   ID, Timestamp, Cry Type, Detection Confidence, Classification Confidence, Temperature, Humidity
 */
object ReportExporter {

    private const val TAG = "ReportExporter"

    /**
     * Generate a report from cry events and save to Downloads.
     *
     * @param context Android context
     * @param events List of cry events to include in the report
     * @param timeRange Time range label (e.g., "Past 7 Days")
     */
    fun saveReport(context: Context, events: List<CryEvent>, timeRange: String) {
        if (events.isEmpty()) {
            Toast.makeText(context, "No cry data to export", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val fileName = generateFileName()
            val content = generateReportContent(events, timeRange)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - use MediaStore
                saveWithMediaStore(context, fileName, content)
            } else {
                // Older Android - save directly to Downloads
                saveToDownloadsLegacy(fileName, content)
            }

            Toast.makeText(
                context,
                "Report saved to Downloads as $fileName",
                Toast.LENGTH_LONG
            ).show()

            Log.d(TAG, "Report saved: $fileName (${events.size} events)")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save report: ${e.message}")
            Toast.makeText(context, "Failed to save report", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Generate a timestamped filename.
     */
    private fun generateFileName(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        return "EchoCare_Report_${timestamp}.txt"
    }

    /**
     * Generate the content as a string.
     */
    private fun generateReportContent(events: List<CryEvent>, timeRange: String): String {
        val builder = StringBuilder()

        // Header row
        builder.append("ID,Timestamp,Cry Type,Detection Confidence (%),Classification Confidence (%),Temperature (°C),Humidity (%)\n")

        // Data rows
        for (event in events) {
            builder.append("${event.id},")
            builder.append("${event.timestamp},")
            builder.append("${event.cryType},")
            builder.append("${event.detectionPercent()},")
            builder.append("${event.classificationPercent()},")
            builder.append("${event.temperature ?: "N/A"},")
            builder.append("${event.humidity ?: "N/A"}\n")
        }

        // Summary section
        builder.append("\n")
        builder.append("--- Report Summary ---\n")
        builder.append("Time Range,$timeRange\n")
        builder.append("Total Events,${events.size}\n")

        // Count by type
        val typeCounts = events.groupBy { it.cryType }
        for ((type, typeEvents) in typeCounts) {
            builder.append("$type,${typeEvents.size}\n")
        }

        // Date generated
        val fullDateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        builder.append("Generated,${fullDateFormat.format(Date())}\n")

        return builder.toString()
    }

    /**
     * Save to Downloads using MediaStore (Android 10+).
     * No storage permission needed.
     */
    private fun saveWithMediaStore(context: Context, fileName: String, content: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw Exception("Failed to create file in Downloads")

        resolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
            outputStream.write(content.toByteArray())
        }

        contentValues.clear()
        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)
    }

    /**
     * Save directly to Downloads folder (Android 9 and below).
     */
    private fun saveToDownloadsLegacy(fileName: String, content: String) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        FileWriter(file).use { writer ->
            writer.write(content)
        }
    }
}