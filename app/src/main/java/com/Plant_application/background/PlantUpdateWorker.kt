package com.Plant_application.background

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.Plant_application.R
import com.Plant_application.data.api.WeatherApiService
import com.Plant_application.data.database.AppDatabase
import com.Plant_application.data.database.PlantItem
import com.Plant_application.data.preference.PreferenceManager
import com.Plant_application.data.repository.PlantRepository
import com.Plant_application.data.repository.WeatherRepository
import com.Plant_application.util.NotificationHelper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.bumptech.glide.Glide
import com.Plant_application.widget.PlantWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class PlantUpdateWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val plantRepository: PlantRepository = PlantRepository(AppDatabase.getDatabase(context).plantDao())
    private val weatherRepository: WeatherRepository = WeatherRepository(WeatherApiService.create())
    private val prefs: PreferenceManager = PreferenceManager(context)
    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)

    companion object {
        const val TEMP_THRESHOLD = 8.0
        const val TEMP_DURATION_HOURS = 12
    }

    override suspend fun doWork(): Result {
        try {
            val location = getFreshLocation()
            val apiKey = context.getString(R.string.openweathermap_api_key)
            var weatherSummaryText = "날씨 정보 없음"
            var relevantForecasts: List<com.Plant_application.data.api.Forecast> = emptyList()

            if (location != null) {
                val response = weatherRepository.getFiveDayForecast(location.latitude, location.longitude, apiKey)
                if (response.isSuccessful && response.body() != null) {
                    val forecastList = response.body()!!.list
                    val today = LocalDate.now()
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    val todayForecasts = forecastList.filter {
                        LocalDate.parse(it.dt_txt, formatter) == today
                    }

                    if (todayForecasts.isNotEmpty()) {
                        val firstForecast = todayForecasts.first()
                        weatherSummaryText = "현재 %.0f°C, ${firstForecast.weather.firstOrNull()?.description ?: ""}".format(firstForecast.main.temp)
                    }

                    val nowDateTime = LocalDateTime.now()
                    val forecastEndDateTime = nowDateTime.plusHours(TEMP_DURATION_HOURS.toLong())
                    relevantForecasts = forecastList.filter {
                        val forecastTime = LocalDateTime.parse(it.dt_txt, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        forecastTime.isAfter(nowDateTime.minusHours(1)) && forecastTime.isBefore(forecastEndDateTime.plusHours(1))
                    }
                }
            }

            val allPlants = plantRepository.getAllPlantsList()
            val notificationsToSend = mutableMapOf<String, MutableSet<String>>()
            val now = System.currentTimeMillis()

            for (plant in allPlants) {
                val reasons = mutableSetOf<String>()

                val (needsWater, waterNotif) = checkWatering(plant, now)
                if (needsWater) reasons.add("WATER")
                if (waterNotif) notificationsToSend.getOrPut(plant.nickname) { mutableSetOf() }.add("물 주기")

                val (needsPesticide, pesticideNotif) = checkPesticide(plant, now)
                if (needsPesticide) reasons.add("PESTICIDE")
                if (pesticideNotif) notificationsToSend.getOrPut(plant.nickname) { mutableSetOf() }.add("살충제")

                val (tempMismatch, tempNotif) = checkTemperature(plant, relevantForecasts)
                if (tempMismatch) reasons.add("TEMP")
                if (tempNotif) notificationsToSend.getOrPut(plant.nickname) { mutableSetOf() }.add("온도 경고")

                updatePlantInDb(plant, reasons, now)
            }

            sendNotifications(notificationsToSend)
            updateAllAppWidgets(weatherSummaryText)

            return Result.success()

        } catch (t: Throwable) {
            Log.e("PlantUpdateWorker", "Work failed", t)

            val errorMessage = when (t) {
                is SecurityException -> "위치 권한 필요"
                is IOException -> "네트워크 오류"
                else -> "업데이트 오류"
            }

            try {
                updateAllAppWidgets(errorMessage)
            } catch (updateE: Exception) {
                Log.e("PlantUpdateWorker", "Failed to update widget with error", updateE)
            }

            return Result.failure()
        }
    }

    private suspend fun getFreshLocation(): android.location.Location? {
        val hasFine = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            Log.w("PlantUpdateWorker", "No location permission granted")
            return null
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        return try {
            val lastLocation = fusedLocationClient.lastLocation.await()
            if (lastLocation != null) {
                return lastLocation
            }
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token
            ).await()
        } catch (e: Exception) {
            Log.e("PlantUpdateWorker", "Failed to get location", e)
            null
        }
    }

    private fun checkWatering(plant: PlantItem, now: Long): Pair<Boolean, Boolean> {
        if (plant.wateringCycleMax <= 0) return Pair(false, false)
        val minMillis = plant.lastWateredTimestamp + TimeUnit.DAYS.toMillis(plant.wateringCycleMin.toLong())
        val maxMillis = plant.lastWateredTimestamp + TimeUnit.DAYS.toMillis(plant.wateringCycleMax.toLong())
        val needsAttention = now in minMillis..maxMillis
        val sendNotification = needsAttention && prefs.notifWaterEnabled
        return Pair(needsAttention, sendNotification)
    }

    private fun checkPesticide(plant: PlantItem, now: Long): Pair<Boolean, Boolean> {
        if (plant.pesticideCycleMax <= 0) return Pair(false, false)
        val minMillis = plant.lastPesticideTimestamp + TimeUnit.DAYS.toMillis(plant.pesticideCycleMin.toLong())
        val maxMillis = plant.lastPesticideTimestamp + TimeUnit.DAYS.toMillis(plant.pesticideCycleMax.toLong())
        val needsAttention = now in minMillis..maxMillis
        val sendNotification = needsAttention && prefs.notifPesticideEnabled
        return Pair(needsAttention, sendNotification)
    }

    private fun checkTemperature(plant: PlantItem, forecasts: List<com.Plant_application.data.api.Forecast>): Pair<Boolean, Boolean> {
        val (minTemp, maxTemp) = parseTempRange(plant.tempRange) ?: return Pair(false, false)

        if (forecasts.isEmpty()) return Pair(false, false)

        var mismatchDurationHours = 0
        for (forecast in forecasts) {
            val temp = forecast.main.temp
            if (temp < minTemp - TEMP_THRESHOLD || temp > maxTemp + TEMP_THRESHOLD) {
                mismatchDurationHours += 3
            }
        }

        val needsAttention = mismatchDurationHours >= TEMP_DURATION_HOURS
        val sendNotification = needsAttention && prefs.notifTempEnabled
        return Pair(needsAttention, sendNotification)
    }

    private fun parseTempRange(range: String): Pair<Double, Double>? {
        return try {
            val numbers = range.replace("°C", "").trim().split("-")
            if (numbers.size == 2) {
                Pair(numbers[0].trim().toDouble(), numbers[1].trim().toDouble())
            } else if (numbers.size == 1) {
                Pair(numbers[0].trim().toDouble(), numbers[0].trim().toDouble())
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun updatePlantInDb(plant: PlantItem, reasons: Set<String>, now: Long) {
        val needsAttention = reasons.isNotEmpty()
        if (needsAttention && plant.needsAttentionTimestamp == null) {
            plantRepository.updatePlant(
                plant.copy(
                    needsAttentionTimestamp = now,
                    attentionReasons = reasons.joinToString(",")
                )
            )
        } else if (!needsAttention && plant.needsAttentionTimestamp != null) {
            plantRepository.updatePlant(
                plant.copy(
                    needsAttentionTimestamp = null,
                    attentionReasons = null
                )
            )
        } else if (needsAttention && plant.attentionReasons != reasons.joinToString(",")) {
            plantRepository.updatePlant(
                plant.copy(
                    attentionReasons = reasons.joinToString(",")
                )
            )
        }
    }

    private fun sendNotifications(notificationsToSend: Map<String, Set<String>>) {
        if (notificationsToSend.isEmpty()) return

        val title = "식물 관리 알림"
        val content = notificationsToSend.map { (plantName, reasons) ->
            "$plantName: ${reasons.joinToString(", ")}"
        }.joinToString("\n")

        NotificationHelper.sendPlantCareNotification(context, title, content)
    }

    private suspend fun updateAllAppWidgets(weatherSummary: String) {
        val componentName = ComponentName(context, PlantWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (appWidgetIds.isEmpty()) return

        val plants = plantRepository.getNeedsAttentionPlantsSnapshot()

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(appWidgetId, weatherSummary, plants)
        }
    }

    private suspend fun updateAppWidget(appWidgetId: Int, weatherSummary: String, plants: List<PlantItem>) {
        val views = RemoteViews(context.packageName, R.layout.plant_widget)
        views.setTextViewText(R.id.tv_widget_weather_summary, weatherSummary)

        PlantWidgetProvider.setupClickIntents(context, appWidgetId, views)

        // 초기화: 식물 그룹과 구분선 모두 숨김
        val plantGroups = listOf(R.id.widget_plant_group_1, R.id.widget_plant_group_2, R.id.widget_plant_group_3)
        plantGroups.forEach { views.setViewVisibility(it, View.GONE) }

        views.setViewVisibility(R.id.widget_divider_1, View.GONE)
        views.setViewVisibility(R.id.widget_divider_2, View.GONE)

        if (plants.isEmpty()) {
            views.setViewVisibility(R.id.ll_widget_plant_images, View.GONE)
            views.setViewVisibility(R.id.widget_empty_view, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.ll_widget_plant_images, View.VISIBLE)
            views.setViewVisibility(R.id.widget_empty_view, View.GONE)

            val itemsToShow = plants.take(3)

            // 4개의 ID: (Container, Image, Name, Reason)
            val itemViews = listOf(
                listOf(R.id.widget_plant_group_1, R.id.iv_widget_plant_1, R.id.tv_widget_plant_1, R.id.tv_widget_reason_1),
                listOf(R.id.widget_plant_group_2, R.id.iv_widget_plant_2, R.id.tv_widget_plant_2, R.id.tv_widget_reason_2),
                listOf(R.id.widget_plant_group_3, R.id.iv_widget_plant_3, R.id.tv_widget_plant_3, R.id.tv_widget_reason_3)
            )

            // 1개일 때: 가운데(2번) 표시, 구분선 없음
            if (itemsToShow.size == 1) {
                setPlantView(views, itemViews[1], itemsToShow[0])
            }
            // 2개일 때: 양옆(1번, 3번) 표시, 가운데 구분선(1번, 2번 중 하나) 표시
            // 여기서는 위젯 레이아웃 구조상 1번과 2번 슬롯을 채우고 사이 구분선(divider_1)을 켜는 게 자연스러움.
            // 하지만 기존 로직(양끝 배치)을 유지하려면: 1번 그룹, 구분선1(숨김), 2번 그룹(비움), 구분선2(숨김), 3번 그룹 사용 -> 가운데가 빔.
            // 요청하신 대로 "구분선"이 있으려면 붙여서 배치하는 게 좋으므로 1번, 2번 슬롯을 사용하겠습니다.
            else if (itemsToShow.size == 2) {
                setPlantView(views, itemViews[0], itemsToShow[0]) // 왼쪽
                views.setViewVisibility(R.id.widget_divider_1, View.VISIBLE) // 구분선
                setPlantView(views, itemViews[1], itemsToShow[1]) // 가운데(사실상 오른쪽 역할)
                // 이렇게 하면 1, 2번 위치에 뜨고 3번은 비게 됨.
                // 만약 꽉 채우고 싶다면 weight가 있으므로 1, 2번만 켜면 반반씩 차지함.
            }
            // 3개일 때: 다 켜고 구분선 2개 켬
            else {
                setPlantView(views, itemViews[0], itemsToShow[0])
                views.setViewVisibility(R.id.widget_divider_1, View.VISIBLE)
                setPlantView(views, itemViews[1], itemsToShow[1])
                views.setViewVisibility(R.id.widget_divider_2, View.VISIBLE)
                setPlantView(views, itemViews[2], itemsToShow[2])
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private suspend fun setPlantView(views: RemoteViews, ids: List<Int>, item: PlantItem) {
        val groupId = ids[0]
        val imageId = ids[1]
        val nameId = ids[2]
        val reasonId = ids[3]

        views.setViewVisibility(groupId, View.VISIBLE)
        views.setTextViewText(nameId, item.nickname)

        val (reasonText, textColor) = getReasonInfo(item.attentionReasons)
        views.setTextViewText(reasonId, reasonText)
        views.setTextColor(reasonId, textColor)

        try {
            val bitmap = loadBitmapForWidget(item.imageUri)
            views.setImageViewBitmap(imageId, bitmap)
        } catch (e: Exception) {
            views.setImageViewResource(imageId, R.drawable.plant1)
        }

        val homeIntent = getPendingHomeIntent()
        views.setOnClickPendingIntent(groupId, homeIntent)
    }

    private fun getReasonInfo(reasons: String?): Pair<String, Int> {
        if (reasons.isNullOrEmpty()) return Pair("", Color.GRAY)
        val firstReason = reasons.split(",").firstOrNull() ?: return Pair("", Color.GRAY)

        return when (firstReason) {
            "WATER" -> Pair("💧 물 주기", Color.parseColor("#2196F3"))
            "PESTICIDE" -> Pair("🐛 살충제", Color.parseColor("#F2A74B"))
            "TEMP" -> Pair("🌡️ 온도 경고", Color.parseColor("#E36161"))
            else -> Pair("", Color.GRAY)
        }
    }

    private fun getPendingHomeIntent(): PendingIntent {
        return NavDeepLinkBuilder(context)
            .setComponentName(com.Plant_application.MainActivity::class.java)
            .setGraph(R.navigation.mobile_navigation)
            .setDestination(R.id.navigation_home)
            .createPendingIntent()
    }

    private suspend fun loadBitmapForWidget(imageUri: String): Bitmap {
        return withContext(Dispatchers.IO) {
            Glide.with(context)
                .asBitmap()
                .load(Uri.fromFile(File(imageUri)))
                .submit(100, 100)
                .get()
        }
    }
}