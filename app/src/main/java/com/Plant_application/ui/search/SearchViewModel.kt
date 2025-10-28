package com.Plant_application.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.Plant_application.data.database.AppDatabase
import com.Plant_application.data.database.PlantItem
import com.Plant_application.data.repository.PlantRepository

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PlantRepository

    private val _searchQuery = MutableLiveData<String>("")

    val searchResults: LiveData<List<PlantItem>> = _searchQuery.switchMap { query ->
        if (query.isBlank()) {
            MutableLiveData(emptyList())
        } else {
            repository.searchPlantsByName(query)
        }
    }

    init {
        val plantDao = AppDatabase.getDatabase(application).plantDao()
        repository = PlantRepository(plantDao)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}