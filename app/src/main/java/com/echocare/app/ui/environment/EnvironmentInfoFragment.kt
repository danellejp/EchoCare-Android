package com.echocare.app.ui.environment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.echocare.app.R

/**
 * Fragment for the Temperature & Humidity Information page.
 *
 * Displays:
 *   1. Current temperature and humidity readings from the Pi's DHT22 sensor
 *   2. Status indicators (Ideal / Too Hot / Too Cold / etc.)
 *   3. Ideal ranges for a baby's room
 *   4. Educational cards about how temperature and humidity affect infants
 *   5. Medical disclaimer
 *
 * Supports pull-to-refresh to get the latest readings.
 */
class EnvironmentInfoFragment : Fragment() {

    private lateinit var viewModel: EnvironmentViewModel

    // Views
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var txtCurrentTemp: TextView
    private lateinit var txtCurrentHumidity: TextView
    private lateinit var txtTempStatus: TextView
    private lateinit var txtHumidityStatus: TextView
    private lateinit var txtLastUpdated: TextView
    private lateinit var txtNoData: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_environment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[EnvironmentViewModel::class.java]

        initializeViews(view)
        setupPullToRefresh()
        observeViewModel()

        // Load data on first open
        viewModel.loadEnvironmentData()
    }


    // View Initialisation
    private fun initializeViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipeRefreshEnv)
        txtCurrentTemp = view.findViewById(R.id.txtCurrentTemp)
        txtCurrentHumidity = view.findViewById(R.id.txtCurrentHumidity)
        txtTempStatus = view.findViewById(R.id.txtTempStatus)
        txtHumidityStatus = view.findViewById(R.id.txtHumidityStatus)
        txtLastUpdated = view.findViewById(R.id.txtLastUpdated)
        txtNoData = view.findViewById(R.id.txtNoData)
    }

    // Pull-to-Refresh
    private fun setupPullToRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.primary)
        swipeRefresh.setOnRefreshListener {
            viewModel.loadEnvironmentData()
        }
    }

    // Observe ViewModel LiveData
    private fun observeViewModel() {
        // Loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            swipeRefresh.isRefreshing = isLoading
        }

        // Data availability
        viewModel.hasData.observe(viewLifecycleOwner) { hasData ->
            txtNoData.visibility = if (hasData) View.GONE else View.VISIBLE
            txtCurrentTemp.visibility = if (hasData) View.VISIBLE else View.INVISIBLE
            txtCurrentHumidity.visibility = if (hasData) View.VISIBLE else View.INVISIBLE
        }

        // Temperature
        viewModel.temperature.observe(viewLifecycleOwner) { temp ->
            temp?.let {
                txtCurrentTemp.text = String.format("%.1f°C", it)
            }
        }

        // Humidity
        viewModel.humidity.observe(viewLifecycleOwner) { humidity ->
            humidity?.let {
                txtCurrentHumidity.text = String.format("%.1f%%", it)
            }
        }

        // Temperature status
        viewModel.tempStatus.observe(viewLifecycleOwner) { status ->
            txtTempStatus.text = status
        }

        viewModel.tempStatusColor.observe(viewLifecycleOwner) { color ->
            txtTempStatus.setTextColor(color)
        }

        // Humidity status
        viewModel.humidityStatus.observe(viewLifecycleOwner) { status ->
            txtHumidityStatus.text = status
        }

        viewModel.humidityStatusColor.observe(viewLifecycleOwner) { color ->
            txtHumidityStatus.setTextColor(color)
        }

        // Last updated
        viewModel.lastUpdated.observe(viewLifecycleOwner) { time ->
            txtLastUpdated.text = time
        }

        // Error
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                txtNoData.text = error
                txtNoData.visibility = View.VISIBLE
            }
        }
    }
}