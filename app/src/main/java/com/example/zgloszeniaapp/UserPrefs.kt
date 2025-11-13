package com.example.zgloszeniaapp


import android.content.Context
import java.util.UUID

object UserPrefs {
    private const val PREFS = "zgloszenia_prefs"
    private const val KEY_NAME = "user_name"
    private const val KEY_UUID = "user_uuid"

    fun getName(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_NAME, null)

    fun setName(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_NAME, value.trim()).apply()
    }

    fun getOrCreateUuid(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getString(KEY_UUID, null)
        if (current != null) return current
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_UUID, newId).apply()
        return newId
    }
}
