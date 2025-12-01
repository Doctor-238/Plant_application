package com.Plant_application.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.Plant_application.MainActivity
import com.Plant_application.PlantApplication
import com.Plant_application.R
import com.Plant_application.receiver.NotificationReceiver

object NotificationHelper {

    // 1단계: 초기 알림 발송 (물 주기 / 살충제 버튼 표시)
    fun sendPlantCareNotification(
        context: Context,
        plantId: Int,
        title: String,
        content: String,
        needsWater: Boolean,
        needsPesticide: Boolean
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, plantId, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, PlantApplication.CHANNEL_ID_PLANT_CARE)
            .setSmallIcon(R.drawable.plant1)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true) // 갱신 시 소리/진동 반복 방지

        // 💧 물 주기 확인 요청 버튼
        if (needsWater) {
            val waterCheckIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = "ACTION_CHECK_WATER" // 바로 실행하지 않고 확인 단계로 이동
                putExtra("plantId", plantId)
                putExtra("plantName", title.substringAfter(": ").trim()) // 식물 이름 전달
            }
            val waterPendingIntent = PendingIntent.getBroadcast(
                context, plantId * 10 + 1, waterCheckIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(0, "💧 물 주기", waterPendingIntent)
        }

        // 🧴 살충제 확인 요청 버튼
        if (needsPesticide) {
            val pestCheckIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = "ACTION_CHECK_PESTICIDE" // 바로 실행하지 않고 확인 단계로 이동
                putExtra("plantId", plantId)
                putExtra("plantName", title.substringAfter(": ").trim())
            }
            val pestPendingIntent = PendingIntent.getBroadcast(
                context, plantId * 10 + 2, pestCheckIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(0, "🧴 살충제", pestPendingIntent)
        }

        notificationManager.notify(plantId, builder.build())
    }

    // 2단계: 확인 알림 갱신 (예/아니오 버튼 표시)
    fun showConfirmationNotification(
        context: Context,
        plantId: Int,
        plantName: String,
        actionType: String // "WATER" or "PESTICIDE"
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val title = if (actionType == "WATER") "💧 물 주기 확인" else "🧴 살충제 확인"
        val content = "${plantName}에게 정말 ${if (actionType == "WATER") "물을 주시겠습니까?" else "살충제를 뿌리시겠습니까?"}"

        // 예 (실행) 인텐트
        val yesIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = if (actionType == "WATER") "ACTION_DO_WATER" else "ACTION_DO_PESTICIDE"
            putExtra("plantId", plantId)
            putExtra("plantName", plantName)
        }
        val yesPendingIntent = PendingIntent.getBroadcast(
            context, plantId * 10 + 3, yesIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 아니오 (취소) 인텐트
        val noIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = "ACTION_CANCEL"
            putExtra("plantId", plantId)
        }
        val noPendingIntent = PendingIntent.getBroadcast(
            context, plantId * 10 + 4, noIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, PlantApplication.CHANNEL_ID_PLANT_CARE)
            .setSmallIcon(R.drawable.plant1)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 확인 창은 좀 더 눈에 띄게
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            // '예' 버튼
            .addAction(0, "✅ 예", yesPendingIntent)
            // '아니오' 버튼
            .addAction(0, "🚫 아니오", noPendingIntent)

        // 기존 알림 ID(plantId)를 사용하여 내용을 덮어씌움 (새 알림이 쌓이는게 아니라 내용이 바뀜)
        notificationManager.notify(plantId, builder.build())
    }
}