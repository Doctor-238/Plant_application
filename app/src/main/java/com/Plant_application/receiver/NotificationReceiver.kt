package com.Plant_application.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.Plant_application.data.database.AppDatabase
import com.Plant_application.data.database.DiaryEntry
import com.Plant_application.data.repository.PlantRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val plantId = intent.getIntExtra("plantId", -1)
        val action = intent.action
        val notificationId = intent.getIntExtra("notificationId", 0)

        if (plantId != -1 && action != null) {
            val db = AppDatabase.getDatabase(context)
            val plantRepository = PlantRepository(db.plantDao())
            val diaryDao = db.diaryEntryDao()

            CoroutineScope(Dispatchers.IO).launch {
                val plant = plantRepository.getPlantByIdSnapshot(plantId)
                if (plant != null) {
                    val currentTime = System.currentTimeMillis()

                    if (action == "ACTION_WATER") {
                        val updatedPlant = plant.copy(lastWateredTimestamp = currentTime)
                        plantRepository.updatePlant(updatedPlant)

                        // 일지 추가
                        diaryDao.insert(
                            DiaryEntry(
                                plantId = plantId,
                                timestamp = currentTime,
                                content = "알림을 통해 물 주기 완료",
                                linkedTaskId = null
                            )
                        )

                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(context, "${plant.nickname} 물 주기 완료!", Toast.LENGTH_SHORT).show()
                        }
                    } else if (action == "ACTION_PESTICIDE") {
                        val updatedPlant = plant.copy(lastPesticideTimestamp = currentTime)
                        plantRepository.updatePlant(updatedPlant)

                        // 일지 추가
                        diaryDao.insert(
                            DiaryEntry(
                                plantId = plantId,
                                timestamp = currentTime,
                                content = "알림을 통해 살충제 완료",
                                linkedTaskId = null
                            )
                        )

                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(context, "${plant.nickname} 살충제 완료!", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // 작업 완료 후 해당 알림 닫기
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(notificationId)
                }
            }
        }
    }
}