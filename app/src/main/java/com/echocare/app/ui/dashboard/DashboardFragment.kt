package com.echocare.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.echocare.app.R
import com.echocare.app.util.AppConstants
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Dashboard Fragment — the main screen of EchoCare.
 *
 * Displays:
 *   - Summary header with event count
 *   - Time range filter toggle (24h / 7 days)
 *   - Cry type filter chips (All / Hungry / Pain / Normal)
 *   - Scrollable list of cry events (RecyclerView)
 *   - Pull-to-refresh to reload data
 *   - Empty state when no events match filters
 *   - Error state when Pi is unreachable
 *
 * Observes DashboardViewModel for reactive UI updates.
 */
class DashboardFragment : Fragment() {

    // ViewModel scoped to this Fragment
    private val viewModel: DashboardViewModel by viewModels()

    // UI references
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CryEventAdapter
    private lateinit var tvEventCount: TextView
    private lateinit var btnTimeFilter: Button
    private lateinit var chipGroupType: ChipGroup
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvEmptyMessage: TextView
    private lateinit var layoutError: LinearLayout
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnRetry: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupSwipeRefresh()
        setupFilters()
        observeViewModel()

        // Initial data load
        viewModel.loadDashboardData()
    }

    // =========================================================================
    // Setup Methods
    // =========================================================================

    private fun initViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipeRefreshLayout)
        recyclerView = view.findViewById(R.id.rvCryEvents)
        tvEventCount = view.findViewById(R.id.tvEventCount)
        btnTimeFilter = view.findViewById(R.id.btnTimeFilter)
        chipGroupType = view.findViewById(R.id.chipGroupCryType)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage)
        layoutError = view.findViewById(R.id.layoutError)
        tvErrorMessage = view.findViewById(R.id.tvErrorMessage)
        btnRetry = view.findViewById(R.id.btnRetry)
    }

    private fun setupRecyclerView() {
        adapter = CryEventAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Smooth scroll to top when new data arrives
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (positionStart == 0) {
                    recyclerView.scrollToPosition(0)
                }
            }
        })
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(
            R.color.primary,
            R.color.hungry_green,
            R.color.pain_red
        )
        swipeRefresh.setOnRefreshListener {
            viewModel.loadDashboardData()
        }
    }

    private fun setupFilters() {
        // Time filter toggle button
        btnTimeFilter.setOnClickListener {
            viewModel.toggleTimeFilter()
        }

        // Cry type filter chips
        chipGroupType.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            val selectedChip = view?.findViewById<Chip>(checkedIds.first())
            val filterType = when (selectedChip?.id) {
                R.id.chipAll -> AppConstants.ALL
                R.id.chipHungry -> AppConstants.CRY_TYPE_HUNGRY
                R.id.chipPain -> AppConstants.CRY_TYPE_PAIN
                R.id.chipNormal -> AppConstants.CRY_TYPE_NORMAL
                else -> AppConstants.ALL
            }
            viewModel.setCryTypeFilter(filterType)
        }

        // Retry button in error state
        btnRetry.setOnClickListener {
            viewModel.loadDashboardData()
        }
    }

    // =========================================================================
    // ViewModel Observers
    // =========================================================================

    private fun observeViewModel() {
        // Cry events list
        viewModel.cryEvents.observe(viewLifecycleOwner) { events ->
            adapter.submitList(events)
        }

        // Loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            swipeRefresh.isRefreshing = isLoading
        }

        // Event count
        viewModel.eventCount.observe(viewLifecycleOwner) { count ->
            tvEventCount.text = "$count cry event${if (count != 1) "s" else ""}"
        }

        // Empty state
        viewModel.isEmpty.observe(viewLifecycleOwner) { empty ->
            val hasError = viewModel.errorMessage.value != null
            if (empty && !hasError) {
                layoutEmpty.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                layoutError.visibility = View.GONE
                tvEmptyMessage.text = getEmptyMessage()
            } else if (!hasError) {
                layoutEmpty.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }

        // Error state
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                layoutError.visibility = View.VISIBLE
                tvErrorMessage.text = error
                recyclerView.visibility = View.GONE
                layoutEmpty.visibility = View.GONE
            } else {
                layoutError.visibility = View.GONE
            }
        }

        // Time filter label
        viewModel.timeFilterLabel.observe(viewLifecycleOwner) { label ->
            btnTimeFilter.text = label
        }
    }

    /**
     * Generates a contextual empty state message based on active filters.
     */
    private fun getEmptyMessage(): String {
        val typeFilter = viewModel.getCurrentCryTypeFilter()
        val timeLabel = viewModel.timeFilterLabel.value ?: "Past 7 Days"

        return if (typeFilter != null) {
            "No \"$typeFilter\" cries detected in the $timeLabel."
        } else {
            "No cry events detected in the $timeLabel.\nYour baby is sleeping peacefully!"
        }
    }
}