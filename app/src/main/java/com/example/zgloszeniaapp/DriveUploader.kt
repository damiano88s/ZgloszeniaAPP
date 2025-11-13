package com.example.zgloszeniaapp



import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import com.google.gson.Gson

object DriveUploader {
    private val client = OkHttpClient()
    private val gson = Gson()

    fun upload(
        execUrl: String,
        payload: UploadPayload,
        onResult: (ok: Boolean, code: Int, body: String) -> Unit
    ) {
        val json = gson.toJson(payload)
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder().url(execUrl).post(body).build()

        Log.d("UPLOAD", "URL=${req.url}")
        Log.d("UPLOAD", "LEN=${json.length}")

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("UPLOAD", "IO: ${e.message}", e)
                onResult(false, -1, e.message ?: "")
            }

            override fun onResponse(call: Call, resp: Response) {
                val code = resp.code
                val text = resp.body?.string().orEmpty()
                Log.d("UPLOAD", "HTTP $code body=$text")
                onResult(resp.isSuccessful, code, text)
            }
        })
    }
}
