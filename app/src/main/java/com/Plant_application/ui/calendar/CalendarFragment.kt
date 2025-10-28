package com.Plant_application.ui.calendar

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.Plant_application.R
import com.Plant_application.databinding.FragmentCalendarBinding
import java.util.Calendar

class CalendarFragment : Fragment(R.layout.fragment_calendar) {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalendarViewModel by viewModels()
    private lateinit var todoAdapter: TodoAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCalendarBinding.bind(view)

        setupRecyclerView()
        setupCalendarView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        todoAdapter = TodoAdapter()
        binding.root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_todo_list).apply {
            adapter = todoAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupCalendarView() {
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            viewModel.onDateSelected(year, month, dayOfMonth)
        }
        val today = Calendar.getInstance()
        viewModel.onDateSelected(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
    }

    private fun observeViewModel() {
        viewModel.selectedDateTodos.observe(viewLifecycleOwner) { todos ->
            todoAdapter.submitList(todos)
            binding.root.findViewById<android.widget.TextView>(R.id.tv_empty_todo).isVisible = todos.isEmpty()
        }

        viewModel.allTodoItems.observe(viewLifecycleOwner) { todoMap ->
            // TODO: CalendarView Decorator
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
