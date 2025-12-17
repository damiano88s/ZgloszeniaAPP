package com.example.zgloszeniaapp

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object GrafikDownload {

    private const val FILE_NAME = "grafik.xlsx"

    fun localFile(context: Context): File =
        File(context.filesDir, FILE_NAME)

    suspend fun downloadToLocal(context: Context, url: String): File = withContext(Dispatchers.IO) {
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
}
