package com.hfad.kronosync.ui

import ScheduleViewModel
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hfad.kronosync.R
import com.hfad.kronosync.model.XmlScheduleDataSource
import com.hfad.kronosync.repository.ScheduleRepository
import com.hfad.kronosync.viewmodel.MyViewModelFactory
import com.hfad.kronosync.viewmodel.SharedViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ScheduleFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var scheduleAdapter: ScheduleAdapter
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val repository = ScheduleRepository(XmlScheduleDataSource())
    private val viewModel: ScheduleViewModel by viewModels { MyViewModelFactory(repository) }

    private lateinit var dateTextView: TextView
    private lateinit var weekTextView: TextView
    private lateinit var previousWeekButton: ImageView
    private lateinit var nextWeekButton: ImageView
    private lateinit var dayButtons: Array<Button>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupWeekNavigation()
        setupDayButtons()
        setupObservers()

        updateDateAndSchedule()
    }

    private fun initializeViews(view: View) {
        dateTextView = view.findViewById(R.id.dateTextView)
        weekTextView = view.findViewById(R.id.weekTextView)
        previousWeekButton = view.findViewById(R.id.previousWeekButton)
        nextWeekButton = view.findViewById(R.id.nextWeekButton)

        recyclerView = view.findViewById(R.id.scheduleRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        scheduleAdapter = ScheduleAdapter(emptyList())
        recyclerView.adapter = scheduleAdapter

        dayButtons = arrayOf(
            view.findViewById(R.id.monday_btn),
            view.findViewById(R.id.tuesday_btn),
            view.findViewById(R.id.wednesday_btn),
            view.findViewById(R.id.thursday_btn),
            view.findViewById(R.id.friday_btn),
            view.findViewById(R.id.saturday_btn),
            view.findViewById(R.id.sunday_btn)
        )
    }


    private fun setupWeekNavigation() {
        previousWeekButton.setOnClickListener {
            val newWeek = viewModel.getCurrentWeek() - 1
            if (newWeek >= 1) {
                viewModel.setCurrentWeek(newWeek)
                updateDateAndSchedule()
            }
        }

        nextWeekButton.setOnClickListener {
            val thisYear = Calendar.getInstance().get(Calendar.YEAR)
            val maxWeeksInYear = Calendar.getInstance().apply {
                set(Calendar.YEAR, thisYear)
                set(Calendar.MONTH, Calendar.DECEMBER)
                set(Calendar.DAY_OF_MONTH, 31)
            }.getActualMaximum(Calendar.WEEK_OF_YEAR)

            val newWeek = viewModel.getCurrentWeek() + 1
            if (newWeek <= maxWeeksInYear) {
                viewModel.setCurrentWeek(newWeek)
                updateDateAndSchedule()
            }
        }
    }


    private fun setupDayButtons() {
        dayButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                val newDayOfWeek = if (index == 6) Calendar.SUNDAY else index + 2
                viewModel.setCurrentDayOfWeek(newDayOfWeek) // Uppdaterar dag i ViewModel

                dayButtons.forEach { it.isSelected = false }
                button.isSelected = true

                updateDateAndSchedule()
            }
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun setupObservers() {
        sharedViewModel.selectedProgramCode.observe(viewLifecycleOwner) { programCode ->
            programCode?.let { viewModel.fetchSchedule(it) }
        }

        viewModel.scheduleItems.observe(viewLifecycleOwner) { result ->
            result.onSuccess { scheduleItems ->
                scheduleAdapter = ScheduleAdapter(scheduleItems)
                recyclerView.adapter = scheduleAdapter
                scheduleAdapter.notifyDataSetChanged()
            }
            result.onFailure { exception ->
                println("Error: ${exception.message}")
                // fixar något bättre senare om de behövs
                exception.printStackTrace()
            }
        }
    }

    private fun updateDateAndSchedule() {
        val currentWeek = viewModel.getCurrentWeek()
        val currentDayOfWeek = viewModel.getCurrentDayOfWeek()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.WEEK_OF_YEAR, currentWeek)
        calendar.set(Calendar.DAY_OF_WEEK, currentDayOfWeek)

        // specil kontroll för söndag, osäker dock
        if (currentDayOfWeek == Calendar.SUNDAY && calendar.get(Calendar.DAY_OF_WEEK) > Calendar.SUNDAY) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }

        val dateString = SimpleDateFormat("EEE, d MMM", Locale("sv", "SE")).format(calendar.time)
            .replace(".", "")
        dateTextView.text = dateString
        weekTextView.text = getString(R.string.week_number, currentWeek)

        sharedViewModel.selectedProgramCode.value?.let {
            viewModel.fetchSchedule(it)
        }
        markCurrentDayButton()
    }

    private fun markCurrentDayButton() {
        val currentDayIndex = viewModel.getCurrentDayOfWeek() - 2
        dayButtons.forEachIndexed { index, button ->
            button.isSelected = index == currentDayIndex
        }
    }

}
