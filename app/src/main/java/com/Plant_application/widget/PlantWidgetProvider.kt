package com.Plant_application.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.Plant_application.R
import com.Plant_application.background.PlantUpdateWorker

class PlantWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            try {
                // 수정: 위젯 추가 시 바로 새로고침 UI 보여주고 작업 시작 (true 전달)
                startOneTimeWork(context, appWidgetId, true)
            } catch (t: Throwable) {
                Log.e("PlantWidgetProvider", "onUpdate failed for widget $appWidgetId", t)
                handleWidgetError(context, appWidgetId)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (intent.action == "WIDGET_REFRESH_CLICK") {
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    startOneTimeWork(context, appWidgetId, true)
                } else {
                    Log.e("PlantWidgetProvider", "Refresh click received with invalid widget ID")
                }
            } else {
                super.onReceive(context, intent)
            }
        } catch (t: Throwable) {
            Log.e("PlantWidgetProvider", "onReceive failed", t)
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                handleWidgetError(context, appWidgetId)
            }
        }
    }

    private fun startOneTimeWork(context: Context, appWidgetId: Int, isRefresh: Boolean = false) {
        if (isRefresh) {
            val views = RemoteViews(context.packageName, R.layout.plant_widget)
            views.setTextViewText(R.id.tv_widget_weather_summary, "새로고침 중...")

            // 추가: 로딩 상태에서도 새로고침 버튼이 동작하도록 설정
            setupClickIntents(context, appWidgetId, views)

            // partiallyUpdateAppWidget 대신 updateAppWidget 사용으로 전체 뷰 갱신 보장
            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
        }

        val workRequest = OneTimeWorkRequestBuilder<PlantUpdateWorker>()
            .setInputData(workDataOf(AppWidgetManager.EXTRA_APPWIDGET_ID to appWidgetId))
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }

    private fun handleWidgetError(context: Context, appWidgetId: Int) {
        try {
            val views = RemoteViews(context.packageName, R.layout.plant_widget)
            views.setTextViewText(R.id.tv_widget_weather_summary, "위젯 오류 발생")
            views.setViewVisibility(R.id.ll_widget_plant_images, View.GONE)
            views.setViewVisibility(R.id.widget_empty_view, View.VISIBLE)
            views.setTextViewText(R.id.widget_empty_view, "오류가 발생했습니다. 새로고침을 눌러주세요.")
            setupClickIntents(context, appWidgetId, views)
            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            Log.e("PlantWidgetProvider", "Failed to update widget with error state", e)
        }
    }

    companion object {
        fun setupClickIntents(context: Context, appWidgetId: Int, remoteViews: RemoteViews) {
            val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val refreshIntent = Intent(context, PlantWidgetProvider::class.java).apply {
                action = "WIDGET_REFRESH_CLICK"
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.withAppendedPath(Uri.parse("plantwidget://widget/id/"), "$appWidgetId/refresh")
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(context, appWidgetId * 10 + 1, refreshIntent, pendingIntentFlag)
            remoteViews.setOnClickPendingIntent(R.id.iv_widget_refresh, refreshPendingIntent)
        }
    }
}