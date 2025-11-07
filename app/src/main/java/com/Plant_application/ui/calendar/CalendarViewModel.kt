package com.Plant_application.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.Plant_application.data.database.AppDatabase
import com.Plant_application.data.database.CalendarTask
import com.Plant_application.data.database.CalendarTaskDao
import com.Plant_application.data.database.PlantItem
import com.Plant_application.data.database.TaskType
import com.Plant_application.data.repository.PlantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val plantRepository: PlantRepository
    private val taskDao: CalendarTaskDao
    private val allPlants: LiveData<List<PlantItem>>
    private val allIncompleteTasks: LiveData<List<CalendarTask>>

    private val _selectedDate = MutableLiveData<Long>()
    val selectedDate: LiveData<Long> = _selectedDate

    val selectedDateTodos: LiveData<List<CalendarTask>>

    val taskSyncTrigger = MediatorLiveData<Unit>()

    private val _isDeleteMode = MutableLiveData(false)
    val isDeleteMode: LiveData<Boolean> = _isDeleteMode

    private val _selectedItems = MutableLiveData<Set<Long>>(emptySet())
    val selectedItems: LiveData<Set<Long>> = _selectedItems

    init {
        val db = AppDatabase.getDatabase(application)
        plantRepository = PlantRepository(db.plantDao())
        taskDao = db.calendarTaskDao()
        allPlants = plantRepository.getAllPlants()
        allIncompleteTasks = taskDao.getIncompleteTasks()

        selectedDateTodos = _selectedDate.switchMap { date ->
            taskDao.getTasksForDate(date)
        }

        taskSyncTrigger.addSource(allPlants) { syncTasks() }
        taskSyncTrigger.addSource(allIncompleteTasks) { syncTasks() }

        onDateSelected(Calendar.getInstance())
    }

    fun onDateSelected(calendar: Calendar) {
        val selectedDateKey = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        _selectedDate.value = selectedDateKey
    }

    fun onDateSelected(year: Int, month: Int, dayOfMonth: Int) {
        val calendar = Calendar.getInstance().apply {
            set(year, month, dayOfMonth)
        }
        onDateSelected(calendar)
    }

    private fun syncTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            val plants = allPlants.value ?: return@launch
            val tasks = allIncompleteTasks.value ?: return@launch
            val taskMap = tasks.associateBy { Triple(it.plantId, it.taskType, it.dueDate) }
            val today = getNormalizedDate(System.currentTimeMillis())

            for (plant in plants) {
                if (plant.wateringCycleMax > 0) {
                    val nextWateringDate = getNextDueDate(plant.lastWateredTimestamp, plant.wateringCycleMax)
                    if (nextWateringDate > 0 && taskMap[Triple(plant.id, TaskType.WATERING, nextWateringDate)] == null) {
                        taskDao.insert(CalendarTask(
                            plantId = plant.id,
                            taskType = TaskType.WATERING,
                            title = "${plant.nickname} 물 주기",
                            dueDate = nextWateringDate
                        ))
                    }
                    taskDao.completePastTasks(plant.id, TaskType.WATERING, today)
                }

                if (plant.pesticideCycleMax > 0) {
                    val nextPesticideDate = getNextDueDate(plant.lastPesticideTimestamp, plant.pesticideCycleMax)
                    if (nextPesticideDate > 0 && taskMap[Triple(plant.id, TaskType.PESTICIDE, nextPesticideDate)] == null) {
                        taskDao.insert(CalendarTask(
                            plantId = plant.id,
                            taskType = TaskType.PESTICIDE,
                            title = "${plant.nickname} 살충제",
                            dueDate = nextPesticideDate
                        ))
                    }
                    taskDao.completePastTasks(plant.id, TaskType.PESTICIDE, today)
                }
            }
        }
    }

    private fun getNextDueDate(lastTimestamp: Long, intervalDays: Int): Long {
        if (intervalDays <= 0) return 0

        val lastDate = Calendar.getInstance().apply {
            timeInMillis = lastTimestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        lastDate.add(Calendar.DAY_OF_YEAR, intervalDays)
        return lastDate.timeInMillis
    }

    private fun getNormalizedDate(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun isTaskEditable(taskDate: Long): Boolean {
        val today = getNormalizedDate(System.currentTimeMillis())
        val tomorrow = today + TimeUnit.DAYS.toMillis(1)
        return taskDate == today || taskDate == tomorrow
    }

    fun onTaskChecked(task: CalendarTask, isChecked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val plantId = task.plantId ?: return@launch
            val plant = plantRepository.getPlantByIdSnapshot(plantId) ?: return@launch

            val updatedTask = task.copy()
            val updatedPlant: PlantItem

            if (isChecked) {
                updatedTask.isCompleted = true
                updatedTask.previousTimestamp = when (task.taskType) {
                    TaskType.WATERING -> plant.lastWateredTimestamp
                    TaskType.PESTICIDE -> plant.lastPesticideTimestamp
                    else -> 0L
                }

                updatedPlant = when (task.taskType) {
                    TaskType.WATERING -> plant.copy(lastWateredTimestamp = System.currentTimeMillis())
                    TaskType.PESTICIDE -> plant.copy(lastPesticideTimestamp = System.currentTimeMillis())
                    else -> plant
                }
            } else {
                updatedTask.isCompleted = false
                updatedPlant = when (task.taskType) {
                    TaskType.WATERING -> plant.copy(lastWateredTimestamp = task.previousTimestamp)
                    TaskType.PESTICIDE -> plant.copy(lastPesticideTimestamp = task.previousTimestamp)
                    else -> plant
                }
            }

            plantRepository.updatePlant(updatedPlant)
            taskDao.update(updatedTask)
        }
    }

    fun addCustomTask(title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val date = _selectedDate.value ?: return@launch
            val task = CalendarTask(
                plantId = null,
                taskType = TaskType.CUSTOM,
                title = title,
                dueDate = date
            )
            taskDao.insert(task)
        }
    }

    fun enterDeleteMode(taskId: Long) {
        if (_isDeleteMode.value == false) {
            _isDeleteMode.value = true
            _selectedItems.value = setOf(taskId)
        }
    }

    fun exitDeleteMode() {
        if (_isDeleteMode.value == true) {
            _isDeleteMode.value = false
            _selectedItems.value = emptySet()
        }
    }

    fun toggleTaskSelection(taskId: Long) {
        val current = _selectedItems.value ?: emptySet()
        _selectedItems.value = if (current.contains(taskId)) current - taskId else current + taskId
    }

    fun deleteSelectedTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            val idsToDelete = _selectedItems.value ?: return@launch
            val tasks = selectedDateTodos.value?.filter { it.id in idsToDelete } ?: return@launch

            val customTaskIds = tasks.filter { it.taskType == TaskType.CUSTOM }.map { it.id }
            val generatedTaskIds = tasks.filter { it.taskType != TaskType.CUSTOM }.map { it.id }

            if (customTaskIds.isNotEmpty()) {
                taskDao.deleteTasksByIds(customTaskIds)
            }
            if (generatedTaskIds.isNotEmpty()) {
                taskDao.skipTasksByIds(generatedTaskIds)
            }

            withContext(Dispatchers.Main) {
                exitDeleteMode()
            }
        }
    }
}