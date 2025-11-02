package com.Plant_application.ui.journal

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.switchMap
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

    private val _plants: LiveData<List<PlantItem>>

    private val _isDeleteMode = MutableLiveData(false)
    val isDeleteMode: LiveData<Boolean> = _isDeleteMode

    private val _selectedItems = MutableLiveData<Set<Int>>(emptySet())
    val selectedItems: LiveData<Set<Int>> = _selectedItems

    private val _resetSearchEvent = MutableSharedFlow<Unit>()
    val resetSearchEvent = _resetSearchEvent.asSharedFlow()

    val currentTabState = MediatorLiveData<JournalTabState>()

    private val filterTrigger = MediatorLiveData<Pair<String, String>>()

    init {
        repository = PlantRepository(plantDao)

        filterTrigger.addSource(_searchQuery) { query ->
            filterTrigger.value = Pair(query ?: "", _sortType.value ?: "최신순")
        }
        filterTrigger.addSource(_sortType) { sort ->
            filterTrigger.value = Pair(_searchQuery.value ?: "", sort ?: "최신순")
        }

        _plants = filterTrigger.switchMap { (query, sort) ->
            repository.getPlants(query, sort)
        }

        val stateObserver = Observer<Any> {
            val newState = JournalTabState(
                _plants.value ?: emptyList(),
                _selectedItems.value ?: emptySet(),
                _isDeleteMode.value ?: false
            )
            if (currentTabState.value != newState) {
                currentTabState.value = newState
            }
        }
        currentTabState.addSource(_plants, stateObserver)
        currentTabState.addSource(_selectedItems, stateObserver)
        currentTabState.addSource(_isDeleteMode, stateObserver)
    }

    fun getPlantsForCurrentTab(): LiveData<List<PlantItem>> = _plants

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
            val itemsToDelete = _plants.value?.filter { it.id in idsToDelete } ?: return@launch

            itemsToDelete.forEach { item ->
                try {
                    File(item.imageUri).delete()
                    plantDao.delete(item)
                } catch (e: Exception) {
                    Log.e("JournalViewModel", "Error deleting plant", e)
                }
            }
        }
        exitDeleteMode()
    }
}