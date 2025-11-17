package com.Plant_application.data.preference

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_IS_FIRST_LAUNCH, true)
        set(value) {
            prefs.edit().putBoolean(KEY_IS_FIRST_LAUNCH, value).apply()
        }

    var lastKnownLat: Float
        get() = prefs.getFloat(KEY_LAST_LAT, 0f)
        set(value) {
            prefs.edit().putFloat(KEY_LAST_LAT, value).apply()
        }

    var lastKnownLon: Float
        get() = prefs.getFloat(KEY_LAST_LON, 0f)
        set(value) {
            prefs.edit().putFloat(KEY_LAST_LON, value).apply()
        }

    var surveySpace: Int
        get() = prefs.getInt(KEY_SURVEY_SPACE, -1)
        set(value) {
            prefs.edit().putInt(KEY_SURVEY_SPACE, value).apply()
        }

    var surveySunlight: Int
        get() = prefs.getInt(KEY_SURVEY_SUNLIGHT, -1)
        set(value) {
            prefs.edit().putInt(KEY_SURVEY_SUNLIGHT, value).apply()
        }

    var surveyTemp: Int
        get() = prefs.getInt(KEY_SURVEY_TEMP, -1)
        set(value) {
            prefs.edit().putInt(KEY_SURVEY_TEMP, value).apply()
        }

    var surveyHumidity: Int
        get() = prefs.getInt(KEY_SURVEY_HUMIDITY, -1)
        set(value) {
            prefs.edit().putInt(KEY_SURVEY_HUMIDITY, value).apply()
        }

    var notifWaterEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_WATER, false)
        set(value) {
            prefs.edit().putBoolean(KEY_NOTIF_WATER, value).apply()
        }

    var notifPesticideEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_PESTICIDE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_NOTIF_PESTICIDE, value).apply()
        }

    var notifTempEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_TEMP, false)
        set(value) {
            prefs.edit().putBoolean(KEY_NOTIF_TEMP, value).apply()
        }

    companion object {
        private const val PREFS_NAME = "planty_prefs"
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_LAST_LAT = "last_known_lat"
        private const val KEY_LAST_LON = "last_known_lon"
        private const val KEY_SURVEY_SPACE = "survey_space"
        private const val KEY_SURVEY_SUNLIGHT = "survey_sunlight"
        private const val KEY_SURVEY_TEMP = "survey_temp"
        private const val KEY_SURVEY_HUMIDITY = "survey_humidity"

        private const val KEY_NOTIF_WATER = "notif_water_enabled"
        private const val KEY_NOTIF_PESTICIDE = "notif_pesticide_enabled"
        private const val KEY_NOTIF_TEMP = "notif_temp_enabled"
    }
}