package com.Plant_application.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Database(entities = [PlantItem::class, CalendarTask::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun plantDao(): PlantDao
    abstract fun calendarTaskDao(): CalendarTaskDao

    suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            val allPlants = plantDao().getAllPlantsList()
            allPlants.forEach {
                try {
                    File(it.imageUri).delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            clearAllTables()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "plant_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}