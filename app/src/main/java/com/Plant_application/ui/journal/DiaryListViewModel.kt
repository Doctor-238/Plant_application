package com.Plant_application.ui.journal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.Plant_application.data.database.AppDatabase
import com.Plant_application.data.database.CalendarTask
import com.Plant_application.data.database.CalendarTaskDao
import com.Plant_application.data.database.DiaryEntry
import com.Plant_application.data.database.DiaryEntryDao
import com.Plant_application.data.database.TaskType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class DiaryListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val diaryDao: DiaryEntryDao = db.diaryEntryDao()
    private val taskDao: CalendarTaskDao = db.calendarTaskDao()

    private val _plantId = MutableLiveData<Int>()

    val diaryEntries: LiveData<List<DiaryEntry>> = _plantId.switchMap { id ->
        diaryDao.getEntriesForPlant(id)
    }

    private val _isDeleteMode = MutableLiveData(false)
    val isDeleteMode: LiveData<Boolean> = _isDeleteMode

    private val _selectedItems = MutableLiveData<Set<Long>>(emptySet())
    val selectedItems: LiveData<Set<Long>> = _selectedItems

    fun loadEntries(plantId: Int) {
        if (_plantId.value == plantId) return
        _plantId.value = plantId
    }

    fun addCustomDiaryEntry(content: String) {
        val plantId = _plantId.value ?: return
        if (content.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            // 일정(Todo)에도 자동 추가 (체크 안 된 상태로)
            val task = CalendarTask(
                plantId = plantId,
                taskType = TaskType.CUSTOM,
                title = content,
                dueDate = today,
                isCompleted = false // 요청사항: 체크 안 된 상태로 추가
            )
            val taskId = taskDao.insert(task)

            diaryDao.insert(
                DiaryEntry(
                    plantId = plantId,
                    timestamp = System.currentTimeMillis(),
                    content = content,
                    linkedTaskId = taskId
                )
            )
        }
    }

    fun enterDeleteMode(entryId: Long) {
        if (_isDeleteMode.value == false) {
            _isDeleteMode.value = true
            _selectedItems.value = setOf(entryId)
        }
    }

    fun toggleItemSelection(entryId: Long) {
        val current = _selectedItems.value ?: emptySet()
        _selectedItems.value = if (current.contains(entryId)) current - entryId else current + entryId
    }

    fun exitDeleteMode() {
        _isDeleteMode.value = false
        _selectedItems.value = emptySet()
    }

    fun deleteSelectedEntries() {
        val idsToDelete = _selectedItems.value ?: return
        val currentEntries = diaryEntries.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            currentEntries.filter { it.id in idsToDelete }.forEach { entry ->
                if (entry.linkedTaskId != null) {
                    taskDao.deleteById(entry.linkedTaskId)
                }
                diaryDao.deleteById(entry.id)
            }
            withContext(Dispatchers.Main) {
                exitDeleteMode()
            }
        }
    }
}