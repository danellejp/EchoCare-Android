package com.echocare.app.ui.charts

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.echocare.app.R
import com.echocare.app.util.AppConstants
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.chip.Chip

/**
 * Fragment for the Charts/Data Visualisation page
 *
 * Displays:
 *   1. Summary cards (total cries, most common type, last cry)
 *   2. Cry type distribution bar chart (Hungry / Pain / Normal)
 *   3. Cry timeline bar chart (cries per hour of day, 0–23)
 *
 * Supports:
 *   - Time range filtering (24h / 7 days) via chips
 *   - Pull to refresh functionality
 *   - Empty and error states
 *
 * Uses MPAndroidChart for chart rendering.
 */
class ChartsFragment : Fragment() {

    private lateinit var viewModel: ChartsViewModel

    // Views
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var barChartDistribution: BarChart
    private lateinit var barChartTimeline: BarChart
    private lateinit var txtTotalCries: TextView
    private lateinit var txtMostCommon: TextView
    private lateinit var txtLastCry: TextView
    private lateinit var layoutChartsEmpty: LinearLayout
    private lateinit var txtChartsEmpty: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutSummaryCards: LinearLayout
    private lateinit var chipCharts24h: Chip
    private lateinit var chipCharts7d: Chip

    // Chart colours matching the dashboard cry type colours
    private val hungryColor = Color.parseColor("#4CAF50")  // Green
    private val painColor = Color.parseColor("#F44336")    // Red
    private val normalColor = Color.parseColor("#2196F3")  // Blue
    private val timelineColor = Color.parseColor("#5C6BC0") // Indigo

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_charts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[ChartsViewModel::class.java]

        initializeViews(view)
        setupCharts()
        setupPullToRefresh()
        setupTimeFilter()
        observeViewModel()

        // Load data on first open
        viewModel.loadChartData()
    }

    // =========================================================================
    // View Initialisation
    // =========================================================================

    private fun initializeViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipeRefreshCharts)
        barChartDistribution = view.findViewById(R.id.barChartDistribution)
        barChartTimeline = view.findViewById(R.id.barChartTimeline)
        txtTotalCries = view.findViewById(R.id.txtTotalCries)
        txtMostCommon = view.findViewById(R.id.txtMostCommon)
        txtLastCry = view.findViewById(R.id.txtLastCry)
        layoutChartsEmpty = view.findViewById(R.id.layoutChartsEmpty)
        txtChartsEmpty = view.findViewById(R.id.txtChartsEmpty)
        progressBar = view.findViewById(R.id.progressBarCharts)
        layoutSummaryCards = view.findViewById(R.id.layoutSummaryCards)
        chipCharts24h = view.findViewById(R.id.chipCharts24h)
        chipCharts7d = view.findViewById(R.id.chipCharts7d)
    }

    // =========================================================================
    // Chart Setup - configure appearance before data is loaded
    // =========================================================================

    private fun setupCharts() {
        setupDistributionChart()
        setupTimelineChart()
    }

    /**
     * Configure the cry type distribution bar chart
     * X-axis: Hungry, Pain, Normal
     * Y-axis: Count
     */
    private fun setupDistributionChart() {
        barChartDistribution.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setTouchEnabled(false)
            setFitBars(true)
            animateY(800)
            setExtraBottomOffset(8f) // add extra bottom padding for labels

            // X-axis at bottom
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textSize = 13f
                textColor = Color.DKGRAY
                valueFormatter = IndexAxisValueFormatter(listOf("Hungry", "Pain", "Normal"))
            }

            // Left Y-axis
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E0E0E0")
                axisMinimum = 0f
                axisMaximum = 50f
                granularity = 5f
                textSize = 12f
                textColor = Color.GRAY
            }

            // Hide right Y-axis
            axisRight.isEnabled = false

            // No data text
            setNoDataText("No data available")
            setNoDataTextColor(Color.GRAY)
        }
    }

    /**
     * Configure the cry timeline bar chart.
     * X-axis: Hours 0–23
     * Y-axis: Cry count per hour
     */
    private fun setupTimelineChart() {
        barChartTimeline.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setTouchEnabled(true)
            setScaleEnabled(false)
            setFitBars(true)
            animateY(1000)

            // X-axis at bottom
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 3f
                labelCount = 8
                textSize = 10f
                textColor = Color.DKGRAY
                labelRotationAngle = -45f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val hour = value.toInt()
                        return when {
                            hour == 0 -> "12AM"
                            hour < 12 -> "${hour}AM"
                            hour == 12 -> "12PM"
                            else -> "${hour - 12}PM"
                        }
                    }
                }
            }

            // Left Y-axis
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E0E0E0")
                axisMinimum = 0f
                axisMaximum = 30f
                granularity = 5f
                textSize = 12f
                textColor = Color.GRAY
            }

            // Hide right Y-axis
            axisRight.isEnabled = false

            // No data text
            setNoDataText("No data available")
            setNoDataTextColor(Color.GRAY)
        }
    }

    // =========================================================================
    // Pull-to-Refresh
    // =========================================================================

    private fun setupPullToRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.primary)
        swipeRefresh.setOnRefreshListener {
            viewModel.loadChartData()
        }
    }

    // =========================================================================
    // Time Filter
    // =========================================================================

    private fun setupTimeFilter() {
        chipCharts24h.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.setTimeRange(AppConstants.DEFAULT_TIME_RANGE_HOURS)
            }
        }

        chipCharts7d.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.setTimeRange(AppConstants.WEEK_TIME_RANGE_HOURS)
            }
        }
    }

    // =========================================================================
    // Observe ViewModel LiveData
    // =========================================================================

    private fun observeViewModel() {
        // Loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            swipeRefresh.isRefreshing = isLoading
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Empty state
        viewModel.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
            layoutChartsEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            barChartDistribution.visibility = if (isEmpty) View.GONE else View.VISIBLE
            barChartTimeline.visibility = if (isEmpty) View.GONE else View.VISIBLE
            layoutSummaryCards.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        // Error state
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                layoutChartsEmpty.visibility = View.VISIBLE
                txtChartsEmpty.text = error
            }
        }

        // Summary cards
        viewModel.totalCries.observe(viewLifecycleOwner) { total ->
            txtTotalCries.text = total.toString()
        }

        viewModel.mostCommonType.observe(viewLifecycleOwner) { type ->
            txtMostCommon.text = type
        }

        viewModel.lastCryTime.observe(viewLifecycleOwner) { time ->
            txtLastCry.text = time
        }

        // Distribution chart
        viewModel.distributionData.observe(viewLifecycleOwner) { distribution ->
            if (distribution.isNotEmpty()) {
                updateDistributionChart(distribution)
            }
        }

        // Timeline chart
        viewModel.timelineData.observe(viewLifecycleOwner) { timeline ->
            if (timeline.isNotEmpty()) {
                updateTimelineChart(timeline)
            }
        }
    }

    // =========================================================================
    // Chart Data Updates
    // =========================================================================

    /**
     * Update the cry type distribution bar chart with new data.
     * Each bar represents one cry type with its own colour.
     */
    private fun updateDistributionChart(distribution: Map<String, Int>) {
        val entries = listOf(
            BarEntry(0f, (distribution["Hungry"] ?: 0).toFloat()),
            BarEntry(1f, (distribution["Pain"] ?: 0).toFloat()),
            BarEntry(2f, (distribution["Normal"] ?: 0).toFloat())
        )

        val dataSet = BarDataSet(entries, "Cry Types").apply {
            colors = listOf(hungryColor, painColor, normalColor)
            valueTextSize = 14f
            valueTextColor = Color.DKGRAY
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
        }

        barChartDistribution.data = BarData(dataSet).apply {
            barWidth = 0.6f
        }
        barChartDistribution.invalidate()
        barChartDistribution.animateY(800)
    }

    /**
     * Update the cry timeline bar chart with new data.
     * 24 bars, one per hour.
     */
    private fun updateTimelineChart(timeline: Map<Int, Int>) {
        val entries = (0..23).map { hour ->
            BarEntry(hour.toFloat(), (timeline[hour] ?: 0).toFloat())
        }

        val dataSet = BarDataSet(entries, "Cries per Hour").apply {
            color = timelineColor
            valueTextSize = 10f
            valueTextColor = Color.DKGRAY
            setDrawValues(false) // Too many bars for value labels
        }

        barChartTimeline.data = BarData(dataSet).apply {
            barWidth = 0.8f
        }
        barChartTimeline.invalidate()
        barChartTimeline.animateY(1000)
    }
}