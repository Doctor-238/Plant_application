package com.Plant_application.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
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
import java.util.Calendar
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.floor

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val plantRepository: PlantRepository
    private val taskDao: CalendarTaskDao
    private val allPlantsLive: LiveData<List<PlantItem>>
    private val allIncompleteTasks: LiveData<List<CalendarTask>>

    private val _selectedDate = MutableLiveData<Long>()
    val selectedDate: LiveData<Long> = _selectedDate

    val selectedDateTodos: LiveData<List<CalendarTask>>

    private val _isDeleteMode = MutableLiveData(false)
    val isDeleteMode: LiveData<Boolean> = _isDeleteMode

    private val _selectedItems = MutableLiveData<Set<Long>>(emptySet())
    val selectedItems: LiveData<Set<Long>> = _selectedItems

    private val plantsObserver = Observer<List<PlantItem>> { triggerSync() }
    private val tasksObserver = Observer<List<CalendarTask>> { triggerSync() }

    private val syncing = AtomicBoolean(false)

    init {
        val db = AppDatabase.getDatabase(application)
        plantRepository = PlantRepository(db.plantDao())
        taskDao = db.calendarTaskDao()
        allPlantsLive = plantRepository.getAllPlants()
        allIncompleteTasks = taskDao.getIncompleteTasks()

        selectedDateTodos = _selectedDate.switchMap { date -> taskDao.getTasksForDateAll(date) }

        allPlantsLive.observeForever(plantsObserver)
        allIncompleteTasks.observeForever(tasksObserver)

        onDateSelected(Calendar.getInstance())
        triggerSync()
    }

    override fun onCleared() {
        allPlantsLive.removeObserver(plantsObserver)
        allIncompleteTasks.removeObserver(tasksObserver)
        super.onCleared()
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
        val calendar = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
        onDateSelected(calendar)
    }

    private fun triggerSync() {
        if (!syncing.compareAndSet(false, true)) {
            return
        }
        syncTasks()
    }

    private fun syncTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val plants = taskDao.getAllPlantsSnapshot()
                val today = getNormalizedDate(System.currentTimeMillis())
                for (plant in plants) {
                    handleWateringTask(plant, today)
                    handlePesticideTask(plant, today)
                }
            } finally {
                syncing.set(false)
            }
        }
    }

    private suspend fun handleWateringTask(plant: PlantItem, today: Long) {
        val due = computeCycleMidpointDueDate(plant.lastWateredTimestamp, plant.wateringCycleMin, plant.wateringCycleMax)
        if (due > 0) {
            taskDao.purgeConflictingTasks(plant.id, TaskType.WATERING, due)
            val active = taskDao.getActiveTask(plant.id, TaskType.WATERING)
            if (active == null) {
                insertSafe(
                    CalendarTask(
                        plantId = plant.id,
                        taskType = TaskType.WATERING,
                        title = "${plant.nickname} 물 주기 (${plant.wateringCycleMin}-${plant.wateringCycleMax}일)",
                        dueDate = due
                    )
                )
            }
            taskDao.completePastTasks(plant.id, TaskType.WATERING, today)
        }
    }

    private suspend fun handlePesticideTask(plant: PlantItem, today: Long) {
        val due = computeCycleMidpointDueDate(plant.lastPesticideTimestamp, plant.pesticideCycleMin, plant.pesticideCycleMax)
        if (due > 0) {
            taskDao.purgeConflictingTasks(plant.id, TaskType.PESTICIDE, due)
            val active = taskDao.getActiveTask(plant.id, TaskType.PESTICIDE)
            if (active == null) {
                insertSafe(
                    CalendarTask(
                        plantId = plant.id,
                        taskType = TaskType.PESTICIDE,
                        title = "${plant.nickname} 살충제 (${plant.pesticideCycleMin}-${plant.pesticideCycleMax}일)",
                        dueDate = due
                    )
                )
            }
            taskDao.completePastTasks(plant.id, TaskType.PESTICIDE, today)
        }
    }

    private suspend fun insertSafe(task: CalendarTask) {
        val plantId = task.plantId
        if (plantId == null) {
            taskDao.insert(task)
            return
        }
        val exists = plantRepository.getPlantByIdSnapshot(plantId) != null
        if (exists) {
            taskDao.insert(task)
        }
    }

    private fun computeCycleMidpointDueDate(lastTimestamp: Long, minDays: Int, maxDays: Int): Long {
        if (maxDays <= 0) return 0
        val interval = if (minDays > 0) floor((minDays + maxDays) / 2.0).toInt() else maxDays
        return getNextDueDate(lastTimestamp, interval)
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
            val plantId = task.plantId

            if (plantId == null) {
                if (task.taskType == TaskType.CUSTOM) {
                    val updatedTask = task.copy(isCompleted = isChecked)
                    taskDao.update(updatedTask)
                }
                return@launch
            }

            val plant = plantRepository.getPlantByIdSnapshot(plantId) ?: return@launch
            val updatedTask = task.copy()
            val updatedPlant: PlantItem = if (isChecked) {
                updatedTask.isCompleted = true
                updatedTask.previousTimestamp = when (task.taskType) {
                    TaskType.WATERING -> plant.lastWateredTimestamp
                    TaskType.PESTICIDE -> plant.lastPesticideTimestamp
                    else -> 0L
                }
                when (task.taskType) {
                    TaskType.WATERING -> plant.copy(lastWateredTimestamp = System.currentTimeMillis())
                    TaskType.PESTICIDE -> plant.copy(lastPesticideTimestamp = System.currentTimeMillis())
                    else -> plant
                }
            } else {
                updatedTask.isCompleted = false
                when (task.taskType) {
                    TaskType.WATERING -> plant.copy(lastWateredTimestamp = updatedTask.previousTimestamp)
                    TaskType.PESTICIDE -> plant.copy(lastPesticideTimestamp = updatedTask.previousTimestamp)
                    else -> plant
                }
            }
            plantRepository.updatePlant(updatedPlant)
            taskDao.update(updatedTask)
            triggerSync()
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
            triggerSync()
        }
    }

    fun enterDeleteMode(taskId: Long) {
        if (_isDeleteMode.value == false) {
            _isDeleteMode.value = true
            _selectedItems.value = setOf(taskId)
        }
    }

    fun toggleTaskSelection(taskId: Long) {
        val current = _selectedItems.value ?: emptySet()
        _selectedItems.value = if (current.contains(taskId)) current - taskId else current + taskId
    }

    fun exitDeleteMode() {
        _isDeleteMode.value = false
        _selectedItems.value = emptySet()
    }

    fun deleteSelectedTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            val ids = _selectedItems.value ?: return@launch
            ids.forEach { taskDao.deleteById(it) }
            exitDeleteMode()
            triggerSync()
        }
    }
}