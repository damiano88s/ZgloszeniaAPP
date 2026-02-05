package com.example.zgloszeniaapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import android.util.Log
import androidx.compose.ui.platform.LocalContext


class SheetsApi(
    private val endpointUrl: String,
    private val client: OkHttpClient = OkHttpClient()
) {

    suspend fun sendJson(payload: JSONObject): Boolean =
        withContext(Dispatchers.IO) {

            val body = payload.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(endpointUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->

                Log.e("HTTP", "URL = ${request.url}")
                Log.e("HTTP", "BODY = $payload")

                val responseText = response.body?.string().orEmpty()

                Log.e("HTTP", "CODE = ${response.code}")
                Log.e("HTTP", "RESPONSE = $responseText")

                response.isSuccessful &&
                        responseText.contains("\"status\":\"OK\"")
            }
        }
}
