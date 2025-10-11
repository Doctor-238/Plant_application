package com.Plant_application.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PlantItem::class, PlantAlarm::class], version = 2, exportSchema = false) // version 변경 및 PlantAlarm 추가
abstract class AppDatabase : RoomDatabase() {

    abstract fun plantDao(): PlantDao

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
                    .fallbackToDestructiveMigration() // 버전 변경 시 기존 데이터 삭제
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}