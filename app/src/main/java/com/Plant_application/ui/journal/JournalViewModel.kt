package com.Plant_application.ui.journal

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.Plant_application.data.database.AppDatabase
import com.Plant_application.data.database.PlantItem
import com.Plant_application.data.repository.PlantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File

data class JournalTabState(
    val items: List<PlantItem> = emptyList(),
    val selectedItemIds: Set<Int> = emptySet(),
    val isDeleteMode: Boolean = false
)

class JournalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PlantRepository
    private val plantDao = AppDatabase.getDatabase(application).plantDao()

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery
    private val _sortType = MutableLiveData("최신순")

    private val allPlants: LiveData<List<PlantItem>>
    private val _filteredPlants = MutableLiveData<List<PlantItem>>()

    private val _isDeleteMode = MutableLiveData(false)
    val isDeleteMode: LiveData<Boolean> = _isDeleteMode

    private val _selectedItems = MutableLiveData<Set<Int>>(emptySet())
    val selectedItems: LiveData<Set<Int>> = _selectedItems

    private val _resetSearchEvent = MutableSharedFlow<Unit>()
    val resetSearchEvent = _resetSearchEvent.asSharedFlow()

    val currentTabState = MediatorLiveData<JournalTabState>()

    init {
        repository = PlantRepository(plantDao)
        allPlants = repository.getAllPlants()

        val filterTrigger = MediatorLiveData<Unit>()
        val triggerObserver = Observer<Any> { filterAndSortPlants() }

        filterTrigger.addSource(allPlants, triggerObserver)
        filterTrigger.addSource(_searchQuery, triggerObserver)
        filterTrigger.addSource(_sortType, triggerObserver)
        filterTrigger.observeForever {}

        val stateObserver = Observer<Any> {
            val newState = JournalTabState(
                _filteredPlants.value ?: emptyList(),
                _selectedItems.value ?: emptySet(),
                _isDeleteMode.value ?: false
            )
            if (currentTabState.value != newState) {
                currentTabState.value = newState
            }
        }
        currentTabState.addSource(_filteredPlants, stateObserver)
        currentTabState.addSource(_selectedItems, stateObserver)
        currentTabState.addSource(_isDeleteMode, stateObserver)
    }

    fun getPlantsForCurrentTab(): LiveData<List<PlantItem>> = _filteredPlants

    private fun filterAndSortPlants() {
        viewModelScope.launch(Dispatchers.Default) {
            val plantList = allPlants.value ?: return@launch
            val query = _searchQuery.value ?: ""
            val sort = _sortType.value ?: "최신순"

            val filtered = if (query.isEmpty()) {
                plantList
            } else {
                plantList.filter { it.nickname.contains(query, ignoreCase = true) }
            }

            val sorted = when (sort) {
                "오래된 순" -> filtered.sortedBy { it.timestamp }
                "이름 오름차순" -> filtered.sortedBy { it.nickname }
                "이름 내림차순" -> filtered.sortedByDescending { it.nickname }
                else -> filtered.sortedByDescending { it.timestamp } // "최신순"
            }
            _filteredPlants.postValue(sorted)
        }
    }

    fun setSearchQuery(query: String) {
        if (_searchQuery.value != query) _searchQuery.value = query
    }

    fun setSortType(sortType: String) {
        if (_sortType.value != sortType) _sortType.value = sortType
    }

    fun enterDeleteMode(initialItemId: Int) {
        if (_isDeleteMode.value == false) {
            viewModelScope.launch { _resetSearchEvent.emit(Unit) }
            _isDeleteMode.value = true
            _selectedItems.value = setOf(initialItemId)
        }
    }

    fun exitDeleteMode() {
        if (_isDeleteMode.value == true) {
            viewModelScope.launch { _resetSearchEvent.emit(Unit) }
            _isDeleteMode.value = false
            _selectedItems.value = emptySet()
        }
    }

    fun toggleItemSelection(itemId: Int) {
        val current = _selectedItems.value ?: emptySet()
        _selectedItems.value = if (current.contains(itemId)) current - itemId else current + itemId
    }

    fun selectAll(items: List<PlantItem>) {
        val current = _selectedItems.value ?: emptySet()
        _selectedItems.value = current + items.map { it.id }
    }

    fun deselectAll(items: List<PlantItem>) {
        val current = _selectedItems.value ?: emptySet()
        _selectedItems.value = current - items.map { it.id }.toSet()
    }

    fun deleteSelectedItems() {
        viewModelScope.launch(Dispatchers.IO) {
            val idsToDelete = _selectedItems.value ?: return@launch
            val itemsToDelete = allPlants.value?.filter { it.id in idsToDelete } ?: return@launch

            itemsToDelete.forEach { item ->
                try {
                    // 이미지 파일 삭제
                    File(item.imageUri).delete()
                    // 데이터베이스에서 식물 정보 삭제
                    plantDao.delete(item)
                } catch (e: Exception) {
                    Log.e("JournalViewModel", "Error deleting plant", e)
                }
            }
        }
        exitDeleteMode()
    }
}