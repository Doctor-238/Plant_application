package com.Plant_application.background

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
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
import kotlin.math.abs

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

    private suspend fun getFreshLocation() = if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        try {
            LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
                .await()
        } catch (e: Exception) {
            null
        }
    } else {
        null
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

        val plantViewIds = listOf(R.id.widget_plant_1, R.id.widget_plant_2, R.id.widget_plant_3)
        plantViewIds.forEach { views.setViewVisibility(it, View.GONE) }

        if (plants.isEmpty()) {
            views.setViewVisibility(R.id.ll_widget_plant_images, View.GONE)
            views.setViewVisibility(R.id.widget_empty_view, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.ll_widget_plant_images, View.VISIBLE)
            views.setViewVisibility(R.id.widget_empty_view, View.GONE)

            val plant1 = plants.getOrNull(0)
            val plant2 = plants.getOrNull(1)
            val plant3 = plants.getOrNull(2)

            val itemsToShow = listOf(plant1, plant2, plant3)
            val itemViews = listOf(
                R.id.widget_plant_1 to (R.id.widget_item_image_internal to R.id.widget_item_name_internal),
                R.id.widget_plant_2 to (R.id.widget_item_image_internal to R.id.widget_item_name_internal),
                R.id.widget_plant_3 to (R.id.widget_item_image_internal to R.id.widget_item_name_internal)
            )

            if (plants.size == 1) {
                itemsToShow[0]?.let { setPlantView(views, itemViews[1].first, itemViews[1].second, it) }
            } else if (plants.size == 2) {
                itemsToShow[0]?.let { setPlantView(views, itemViews[0].first, itemViews[0].second, it) }
                itemsToShow[1]?.let { setPlantView(views, itemViews[2].first, itemViews[2].second, it) }
            } else {
                itemsToShow.forEachIndexed { index, plant ->
                    plant?.let { setPlantView(views, itemViews[index].first, itemViews[index].second, it) }
                }
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private suspend fun setPlantView(views: RemoteViews, viewId: Int, viewIds: Pair<Int, Int>, item: PlantItem) {
        views.setViewVisibility(viewId, View.VISIBLE)
        views.setTextViewText(viewIds.second, item.nickname)

        try {
            val bitmap = loadBitmapForWidget(item.imageUri)
            views.setImageViewBitmap(viewIds.first, bitmap)
        } catch (e: Exception) {
            views.setImageViewResource(viewIds.first, R.drawable.plant1)
        }

        val homeIntent = getPendingHomeIntent()
        views.setOnClickPendingIntent(viewId, homeIntent)
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