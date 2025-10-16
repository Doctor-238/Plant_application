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
        binding.rvTodoList.adapter = todoAdapter
        binding.rvTodoList.layoutManager = LinearLayoutManager(context)
    }

    private fun setupCalendarView() {
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            viewModel.onDateSelected(year, month, dayOfMonth)
        }
        // 오늘 날짜로 초기 선택
        val today = Calendar.getInstance()
        viewModel.onDateSelected(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
    }

    private fun observeViewModel() {
        viewModel.selectedDateTodos.observe(viewLifecycleOwner) { todos ->
            todoAdapter.submitList(todos)
            binding.tvEmptyTodo.isVisible = todos.isEmpty()
        }

        // 할 일이 있는 날짜를 캘린더에 표시하는 로직 (Decorator - 추후 구현)
        viewModel.allTodoItems.observe(viewLifecycleOwner) { todoMap ->
            // TODO: CalendarView Decorator를 사용하여 todoMap의 key(날짜)에 해당하는 날들에 하이라이트 표시
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}