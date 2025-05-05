package com.jennycoffee.locationtracker

import android.content.Context

object AppPreferences {
    private const val PREF_NAME = "MyAppPrefs"
    private const val KEY_INPUT1 = "input1"
    private const val KEY_INPUT2 = "input2"
    private const val KEY_INPUT3 = "input3"

    fun saveInputs(context: Context, input1: String, input2: String, input3: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_INPUT1, input1)
            .putString(KEY_INPUT2, input2)
            .putString(KEY_INPUT3, input3)
            .apply()
    }

    fun getInput1(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_INPUT1, "") ?: ""
    }

    fun getInput2(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_INPUT2, "") ?: ""
    }

    fun getInput3(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_INPUT3, "") ?: ""
    }

}
