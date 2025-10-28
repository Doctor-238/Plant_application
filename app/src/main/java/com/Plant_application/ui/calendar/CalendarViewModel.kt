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

    // 모든 할 일 목록 (날짜별로 그룹화)
    private val _allTodoItems = MediatorLiveData<Map<Long, List<TodoItem>>>()
    val allTodoItems: LiveData<Map<Long, List<TodoItem>>> = _allTodoItems

    // 선택된 날짜의 할 일 목록
    private val _selectedDateTodos = MutableLiveData<List<TodoItem>>(emptyList())
    val selectedDateTodos: LiveData<List<TodoItem>> = _selectedDateTodos

    init {
        val plantDao = AppDatabase.getDatabase(application).plantDao()
        plantRepository = PlantRepository(plantDao)
        allPlants = plantRepository.getAllPlants()

        _allTodoItems.addSource(allPlants) { plants ->
            _allTodoItems.value = generateTodoItems(plants)
        }
    }

    // 날짜 선택 시 해당 날짜의 할 일 목록 업데이트
    fun onDateSelected(year: Int, month: Int, dayOfMonth: Int) {
        val calendar = Calendar.getInstance().apply {
            set(year, month, dayOfMonth, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val selectedDateKey = calendar.timeInMillis
        _selectedDateTodos.value = _allTodoItems.value?.get(selectedDateKey) ?: emptyList()
    }

    // 식물 목록을 기반으로 전체 할 일 목록 생성
    private fun generateTodoItems(plants: List<PlantItem>?): Map<Long, List<TodoItem>> {
        val todoMap = mutableMapOf<Long, MutableList<TodoItem>>()
        if (plants == null) return todoMap

        val today = Calendar.getInstance()

        plants.forEach { plant ->
            // 물 주기 계산
            val waterInterval = parseCycle(plant.wateringCycle)
            if (waterInterval > 0) {
                // 식물 등록일로부터 1년치 할 일 생성
                addTasksToMap(todoMap, plant, TaskType.WATERING, waterInterval, today)
            }

            // 살충제 주기 계산
            val pesticideInterval = parseCycle(plant.pesticideCycle)
            if (pesticideInterval > 0) {
                addTasksToMap(todoMap, plant, TaskType.PESTICIDE, pesticideInterval, today)
            }
        }
        return todoMap
    }

    // 반복 주기에 따라 할 일을 맵에 추가하는 헬퍼 함수
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

    // "주 1-2회" -> 4, "10일에 한 번" -> 10 과 같이 주기 텍스트를 숫자로 변환
    private fun parseCycle(cycle: String): Int {
        return when {
            "주" in cycle && ("1" in cycle || "2" in cycle) -> 4 // 주 1-2회는 평균 4일로 계산
            "주" in cycle && "1" in cycle -> 7
            "일" in cycle -> cycle.filter { it.isDigit() }.toIntOrNull() ?: 0
            else -> 0
        }
    }
}