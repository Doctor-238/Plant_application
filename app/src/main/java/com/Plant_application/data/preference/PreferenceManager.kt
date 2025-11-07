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

    companion object {
        private const val PREFS_NAME = "planty_prefs"
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_LAST_LAT = "last_known_lat"
        private const val KEY_LAST_LON = "last_known_lon"
    }
}