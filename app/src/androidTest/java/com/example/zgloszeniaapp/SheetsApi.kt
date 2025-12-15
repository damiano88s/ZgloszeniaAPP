package com.example.zgloszeniaapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

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
            put("typ", "Wodomierze")
            put("uzytkownik", userName)
            put("urz_uuid", userId)
            put("wersja_apki", wersjaApki)
            put("timestamp_client", System.currentTimeMillis().toString())

            put("adres", adres)
            put("nr_wodomierza", numerWodomierza)
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
            val responseText = response.body?.string().orEmpty()
            response.isSuccessful && responseText.contains("\"status\":\"OK\"")
        }
    }
}
