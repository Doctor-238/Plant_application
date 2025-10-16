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

    companion object {
        private const val PREFS_NAME = "planty_prefs"
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
    }
}