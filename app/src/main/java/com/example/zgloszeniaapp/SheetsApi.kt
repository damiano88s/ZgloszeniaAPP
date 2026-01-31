package com.example.zgloszeniaapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import android.util.Log


class SheetsApi(
    private val endpointUrl: String,
    private val client: OkHttpClient = OkHttpClient()
) {

    suspend fun sendWodomierz(
        userName: String,
        userId: String,
        wersjaApki: String,
        adres: String,
        numerWodomierza: String,
        stan: String,
        photoBase64: String?,
        fileName: String?
    ): Boolean = withContext(Dispatchers.IO) {

        val json = JSONObject().apply {
            put("unit", "ZNT")              // ✳️ dopasuj do ADM
            put("module", "TECH")           // ✳️ TECH lub ZLEC
            put("type", "Wodomierze")       // ✳️ dokładnie taka nazwa jaką rozpoznaje Apps Script

            put("user", userName)
            put("urz_uuid", userId)
            put("appVersion", wersjaApki)
            put("timestamp_client", System.currentTimeMillis().toString())

            put("adres", adres)
            put("numerWodomierza", numerWodomierza)
            put("stan", stan)


            if (!photoBase64.isNullOrBlank() && !fileName.isNullOrBlank()) {
                put("photo1", photoBase64)
                put("fileName1", fileName)
            }
        }

        val body = json.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(endpointUrl)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->

            Log.e("HTTP", "URL = ${request.url}")
            Log.e("HTTP", "BODY = $body")

            val responseText = response.body?.string().orEmpty()

            Log.e("HTTP", "CODE = ${response.code}")
            Log.e("HTTP", "RESPONSE = $responseText")

            return@withContext response.isSuccessful &&
                    responseText.contains("\"status\":\"OK\"")
        }

    }

}