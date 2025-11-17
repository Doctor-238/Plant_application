package com.Plant_application.ui.calendar

import android.os.Bundle
import android.text.InputType
import android.transition.TransitionManager
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.Plant_application.R
import com.Plant_application.data.database.PlantItem
import com.Plant_application.databinding.FragmentCalendarBinding
import java.util.Calendar

class CalendarFragment : Fragment(R.layout.fragment_calendar) {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalendarViewModel by viewModels()
    private lateinit var todoAdapter: TodoAdapter
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCalendarBinding.bind(view)

        setupRecyclerView()
        setupCalendarView()
        setupListeners()
        setupBackButtonHandler()
        observeViewModel()

        viewModel.loadAllPlants()
    }

    private fun setupRecyclerView() {
        todoAdapter = TodoAdapter(
            onTaskClick = { task ->
                if (viewModel.isDeleteMode.value == true) {
                    viewModel.toggleTaskSelection(task.id)
                } else if (viewModel.isTaskEditable(task.dueDate)) {
                    viewModel.onTaskChecked(task, !task.isCompleted)
                }
            },
            onTaskLongClick = { task ->
                viewModel.enterDeleteMode(task.id)
            },
            isDeleteMode = { viewModel.isDeleteMode.value ?: false },
            isSelected = { taskId -> viewModel.selectedItems.value?.contains(taskId) ?: false },
            isEditable = { date -> viewModel.isTaskEditable(date) }
        )
        binding.rvTodoList.apply {
            adapter = todoAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupCalendarView() {
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            viewModel.onDateSelected(year, month, dayOfMonth)
        }
    }

    private fun setupListeners() {
        binding.fabAddTodo.setOnClickListener {
            showAddTodoDialog()
        }

        binding.ivBackDeleteMode.setOnClickListener {
            viewModel.exitDeleteMode()
        }

        binding.btnDelete.setOnClickListener {
            val count = viewModel.selectedItems.value?.size ?: 0
            if (count > 0) {
                AlertDialog.Builder(requireContext())
                    .setTitle("삭제 확인")
                    .setMessage("${count}개의 일정을 정말 삭제하시겠습니까? (식물 일정은 다음 주기로 넘어갑니다)")
                    .setPositiveButton("예") { _, _ -> viewModel.deleteSelectedTasks() }
                    .setNegativeButton("아니오", null)
                    .show()
            }
        }
    }

    private fun setupBackButtonHandler() {
        onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                viewModel.exitDeleteMode()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    private fun observeViewModel() {
        viewModel.selectedDateTodos.observe(viewLifecycleOwner) { todos ->
            todoAdapter.submitList(todos)
            binding.tvEmptyTodo.isVisible = todos.isEmpty()
            binding.rvTodoList.isVisible = todos.isNotEmpty()
        }

        viewModel.isDeleteMode.observe(viewLifecycleOwner) { isDeleteMode ->
            onBackPressedCallback.isEnabled = isDeleteMode

            TransitionManager.beginDelayedTransition(binding.toolbarContainer)
            binding.toolbarNormal.isVisible = !isDeleteMode
            binding.toolbarDelete.isVisible = isDeleteMode

            binding.fabAddTodo.isVisible = !isDeleteMode

            todoAdapter.notifyItemRangeChanged(0, todoAdapter.itemCount, "DELETE_MODE_CHANGED")
        }

        viewModel.selectedItems.observe(viewLifecycleOwner) { selectedIds ->
            binding.btnDelete.isEnabled = selectedIds.isNotEmpty()
            todoAdapter.notifyItemRangeChanged(0, todoAdapter.itemCount, "SELECTION_CHANGED")
        }
    }

    private fun showAddTodoDialog() {
        val context = requireContext()
        val plants = viewModel.allPlantsList.value ?: emptyList()

        if (plants.isEmpty()) {
            showSimpleAddTodoDialog()
        } else {
            showPlantSelectAddTodoDialog(plants)
        }
    }

    private fun showSimpleAddTodoDialog() {
        val context = requireContext()
        val editText = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "할 일 입력"
        }

        val container = FrameLayout(context).apply {
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, padding / 2)
            addView(editText)
        }

        AlertDialog.Builder(context)
            .setTitle("개인 할 일 추가")
            .setMessage("등록된 식물이 없습니다. 개인 일정을 추가합니다.")
            .setView(container)
            .setPositiveButton("추가") { _, _ ->
                val title = editText.text.toString()
                if (title.isNotBlank()) {
                    viewModel.addCustomTask(title, emptyList())
                } else {
                    Toast.makeText(context, "할 일을 입력해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showPlantSelectAddTodoDialog(plants: List<PlantItem>) {
        val context = requireContext()
        val plantNames = plants.map { it.nickname }.toTypedArray()
        val selectedPlants = BooleanArray(plants.size)
        val selectedPlantIds = mutableListOf<Int>()

        val layout = layoutInflater.inflate(R.layout.dialog_add_task, null) as FrameLayout
        val editText = layout.findViewById<EditText>(R.id.et_task_title)

        AlertDialog.Builder(context)
            .setTitle("할 일 추가")
            .setView(layout)
            .setMultiChoiceItems(plantNames, selectedPlants) { _, which, isChecked ->
                selectedPlants[which] = isChecked
            }
            .setPositiveButton("추가") { _, _ ->
                val title = editText.text.toString()
                if (title.isNotBlank()) {
                    selectedPlantIds.clear()
                    for (i in selectedPlants.indices) {
                        if (selectedPlants[i]) {
                            selectedPlantIds.add(plants[i].id)
                        }
                    }
                    viewModel.addCustomTask(title, selectedPlantIds)
                } else {
                    Toast.makeText(context, "할 일을 입력해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onBackPressedCallback.remove()
        binding.rvTodoList.adapter = null
        _binding = null
    }
}