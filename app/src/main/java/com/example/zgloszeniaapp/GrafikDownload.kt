package com.example.zgloszeniaapp

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object GrafikDownload {

    private const val PREFS = "grafik_prefs"
    private const val KEY_ETAG = "grafik_etag"
    private const val FILE_NAME = "grafik.xlsx"

    fun localFile(context: Context): File =
        File(context.filesDir, FILE_NAME)

    private fun getSavedEtag(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ETAG, null)

    private fun saveEtag(context: Context, etag: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ETAG, etag)
            .apply()
    }

    suspend fun downloadToLocal(context: Context, url: String): File =
        withContext(Dispatchers.IO) {

            val target = localFile(context)

            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                requestMethod = "GET"
                instanceFollowRedirects = true
            }

            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            connection.disconnect()
            target
        }

    // ✅ TU MA BYĆ ensureLocalFile
    suspend fun ensureLocalFile(context: Context, url: String): File =
        withContext(Dispatchers.IO) {

            val target = localFile(context)
            val savedEtag = getSavedEtag(context)

            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                requestMethod = "GET"
                instanceFollowRedirects = true

                if (savedEtag != null) {
                    setRequestProperty("If-None-Match", savedEtag)
                }
            }

            when (connection.responseCode) {

                HttpURLConnection.HTTP_NOT_MODIFIED -> {
                    connection.disconnect()
                    target
                }

                HttpURLConnection.HTTP_OK -> {
                    val newEtag = connection.getHeaderField("ETag")

                    connection.inputStream.use { input ->
                        target.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    saveEtag(context, newEtag)
                    connection.disconnect()
                    target
                }

                else -> {
                    connection.disconnect()
                    if (target.exists()) target
                    else throw IllegalStateException("Nie można pobrać pliku Excel")
                }
            }
        }
}
