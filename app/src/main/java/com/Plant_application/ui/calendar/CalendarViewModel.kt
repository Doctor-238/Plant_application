package com.Plant_application.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.Plant_application.data.database.AppDatabase
import com.Plant_application.data.database.PlantItem
import com.Plant_application.data.repository.PlantRepository
import java.util.Calendar

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val plantRepository: PlantRepository
    val allPlants: LiveData<List<PlantItem>>

    private val _allTodoItems = MediatorLiveData<Map<Long, List<TodoItem>>>()
    val allTodoItems: LiveData<Map<Long, List<TodoItem>>> = _allTodoItems

    private val _selectedDate = MutableLiveData<Long>()
    val selectedDateTodos = MediatorLiveData<List<TodoItem>>()

    init {
        val plantDao = AppDatabase.getDatabase(application).plantDao()
        plantRepository = PlantRepository(plantDao)
        allPlants = plantRepository.getAllPlants()

        _allTodoItems.addSource(allPlants) { plants ->
            _allTodoItems.value = generateTodoItems(plants)
        }

        selectedDateTodos.addSource(_allTodoItems) { todoMap ->
            _selectedDate.value?.let { date ->
                selectedDateTodos.value = todoMap[date] ?: emptyList()
            }
        }
        selectedDateTodos.addSource(_selectedDate) { date ->
            selectedDateTodos.value = _allTodoItems.value?.get(date) ?: emptyList()
        }
    }

    fun onDateSelected(year: Int, month: Int, dayOfMonth: Int) {
        val calendar = Calendar.getInstance().apply {
            set(year, month, dayOfMonth, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val selectedDateKey = calendar.timeInMillis
        _selectedDate.value = selectedDateKey
    }

    private fun generateTodoItems(plants: List<PlantItem>?): Map<Long, List<TodoItem>> {
        val todoMap = mutableMapOf<Long, MutableList<TodoItem>>()
        if (plants == null) return todoMap

        val today = Calendar.getInstance()

        plants.forEach { plant ->
            val waterInterval = plant.wateringCycleMax
            if (waterInterval > 0) {
                addTasksToMap(todoMap, plant, TaskType.WATERING, waterInterval, today)
            }

            val pesticideInterval = plant.pesticideCycleMax
            if (pesticideInterval > 0) {
                addTasksToMap(todoMap, plant, TaskType.PESTICIDE, pesticideInterval, today)
            }
        }
        return todoMap
    }

    private fun addTasksToMap(
        map: MutableMap<Long, MutableList<TodoItem>>,
        plant: PlantItem,
        taskType: TaskType,
        interval: Int,
        today: Calendar
    ) {
        val plantCalendar = Calendar.getInstance().apply { timeInMillis = plant.timestamp }
        val oneYearLater = (today.clone() as Calendar).apply { add(Calendar.YEAR, 1) }

        while (plantCalendar.before(oneYearLater)) {
            val dateKey = (plantCalendar.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            if (!map.containsKey(dateKey)) {
                map[dateKey] = mutableListOf()
            }
            map[dateKey]?.add(TodoItem(plant, taskType, dateKey))
            plantCalendar.add(Calendar.DAY_OF_YEAR, interval)
        }
    }
}