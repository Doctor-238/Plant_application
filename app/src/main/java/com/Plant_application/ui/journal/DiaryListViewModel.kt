package com.Plant_application.ui.journal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.Plant_application.data.database.AppDatabase
import com.Plant_application.data.database.DiaryEntry
import com.Plant_application.data.database.DiaryEntryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DiaryListViewModel(application: Application) : AndroidViewModel(application) {

    private val diaryDao: DiaryEntryDao = AppDatabase.getDatabase(application).diaryEntryDao()

    private val _plantId = MutableLiveData<Int>()

    val diaryEntries: LiveData<List<DiaryEntry>> = _plantId.switchMap { id ->
        diaryDao.getEntriesForPlant(id)
    }

    fun loadEntries(plantId: Int) {
        if (_plantId.value == plantId) return
        _plantId.value = plantId
    }

    fun addCustomDiaryEntry(content: String) {
        val plantId = _plantId.value ?: return
        if (content.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            diaryDao.insert(
                DiaryEntry(
                    plantId = plantId,
                    timestamp = System.currentTimeMillis(),
                    content = content,
                    linkedTaskId = null
                )
            )
        }
    }
}