package com.example.zgloszeniaapp

import android.content.Context
import android.util.Log

object GrafikTest {

    private const val TAG = "GRAFIK_TEST"

    suspend fun downloadAndRead(context: Context) {
        try {
            val file = GrafikDownload.ensureLocalFile(context, GrafikConfig.GRAFIK_URL)
            Log.d(TAG, "Pobrano plik: ${file.absolutePath}, rozmiar: ${file.length()}")

            val rows = file.inputStream().use { GrafikExcelReader.read(it) }
            Log.d(TAG, "Wczytano wierszy: ${rows.size}")

            if (rows.isNotEmpty()) {
                Log.d(TAG, "Pierwszy wpis: ${rows.first()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Błąd: ${e.message}", e)
        }
    }
}
