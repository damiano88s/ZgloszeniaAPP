package com.example.zgloszeniaapp

import android.content.Context
import java.util.UUID

object UserPrefs {

    private const val PREFS = "zgloszenia_prefs"
    private const val KEY_USER = "user"
    private const val KEY_UNITY = "unity"
    private const val KEY_SETUP_DONE = "setup_done"
    private const val KEY_UUID = "user_uuid"

    fun setName(context: Context, name: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER, name)
            .apply()
    }

    fun getName(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_USER, null)

    fun setUnity(context: Context, unity: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_UNITY, unity)
            .apply()
    }

    fun getUnity(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_UNITY, null)

    fun setSetupDone(context: Context, done: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SETUP_DONE, done)
            .apply()
    }

    fun isSetupDone(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SETUP_DONE, false)

    fun getOrCreateUuid(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        var uuid = prefs.getString(KEY_UUID, null)
        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_UUID, uuid).apply()
        }
        return uuid
    }
}
