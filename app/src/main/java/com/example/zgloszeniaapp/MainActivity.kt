package com.example.zgloszeniaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.ui.text.style.TextAlign
import android.graphics.Bitmap

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Base64
import java.io.ByteArrayOutputStream

import android.util.Log

import android.widget.Toast
import java.time.LocalDate

import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap

import androidx.compose.ui.unit.dp

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.platform.LocalContext


import android.net.Uri

import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext


import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

import java.io.IOException
import java.util.concurrent.TimeUnit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageDecoder

import android.os.Build
import android.provider.MediaStore

import java.io.File

import android.graphics.BitmapFactory
import androidx.core.content.FileProvider

import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.saveable.rememberSaveable

import android.graphics.Matrix
import android.media.ExifInterface


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }
        }


        setContent { AppScreen() }
    }
}

private var currentPhotoUri: Uri? = null
private val TAKE_PHOTO_REQUEST = 1


@Composable
fun AppScreen() {
    val context = LocalContext.current

    // Formularz
    var userName by rememberSaveable { mutableStateOf(UserPrefs.getName(context) ?: "") }
    val userId = remember { UserPrefs.getOrCreateUuid(context) } // tu wystarczy zwykłe remember
    var typ by rememberSaveable { mutableStateOf<String?>(null) }
    var adres by rememberSaveable { mutableStateOf("") }
    var opis by rememberSaveable { mutableStateOf("") }

    var photoBase641 by remember { mutableStateOf<String?>(null) }
    var photoBase642 by remember { mutableStateOf<String?>(null) }
    var photoBase643 by remember { mutableStateOf<String?>(null) }

// Bitmapy i File przechowuj przez zwykłe remember, np:
    var photoFile1 by remember { mutableStateOf<File?>(null) }
    var photoBitmap1 by remember { mutableStateOf<Bitmap?>(null) }
    var photoFile2 by remember { mutableStateOf<File?>(null) }
    var photoBitmap2 by remember { mutableStateOf<Bitmap?>(null) }
    var photoFile3 by remember { mutableStateOf<File?>(null) }
    var photoBitmap3 by remember { mutableStateOf<Bitmap?>(null) }


    // Launchery dla 3 zdjęć
    // Launchery dla 3 zdjęć – poprawione!
    val takePictureLauncher1 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        Log.d("ZDJECIE1", "TakePicture1 success=$success, file=${photoFile1?.absolutePath}")
        if (success && photoFile1 != null) {
            val bitmapRaw = BitmapFactory.decodeFile(photoFile1!!.absolutePath)
            val bitmap = rotateBitmapIfRequired(bitmapRaw, photoFile1!!)
            photoBitmap1 = bitmap
            photoBase641 = bitmapToBase64(bitmap)
        }
    }

    val takePictureLauncher2 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        Log.d("ZDJECIE2", "TakePicture2 success=$success, file=${photoFile2?.absolutePath}")
        if (success && photoFile2 != null) {
            val bitmapRaw = BitmapFactory.decodeFile(photoFile2!!.absolutePath)
            val bitmap = rotateBitmapIfRequired(bitmapRaw, photoFile2!!)
            photoBitmap2 = bitmap
            photoBase642 = bitmapToBase64(bitmap)
        }
    }

    val takePictureLauncher3 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        Log.d("ZDJECIE3", "TakePicture3 success=$success, file=${photoFile3?.absolutePath}")
        if (success && photoFile3 != null) {
            val bitmapRaw = BitmapFactory.decodeFile(photoFile3!!.absolutePath)
            val bitmap = rotateBitmapIfRequired(bitmapRaw, photoFile3!!)
            photoBitmap3 = bitmap
            photoBase643 = bitmapToBase64(bitmap)
        }
    }


    var isSending by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    // Imię i nazwisko przy pierwszym uruchomieniu
    if (userName.isBlank()) {
        NameDialog(
            onSave = {
                UserPrefs.setName(context, it)
                userName = it
            }
        )
    }

    Scaffold { padding ->
        val scroll = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(scroll)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Zgłoszenia",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.Center
            )

            // Przełącznik typu
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = typ == Config.SHEET_GABARYTY,
                    onClick = { typ = Config.SHEET_GABARYTY }
                )
                Text("Gabaryty", modifier = Modifier.padding(end = 16.dp))

                RadioButton(
                    selected = typ == Config.SHEET_ZLECENIA,
                    onClick = { typ = Config.SHEET_ZLECENIA }
                )
                Text("Zlecenie")
            }

            // Adres
            OutlinedTextField(
                value = adres,
                onValueChange = { adres = it },
                label = { Text("Adres") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Opis
            OutlinedTextField(
                value = opis,
                onValueChange = { opis = it },
                label = { Text("Opis") },
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp)
            )

            // --- Sloty zdjęć ---
            Spacer(Modifier.height(8.dp))
            PhotoSlot(
                label = "Zdjęcie 1",
                bitmap = photoBitmap1,
                onTake = {
                    val file = createImageFile(context, "photo1_${System.currentTimeMillis()}")
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    photoFile1 = file
                    takePictureLauncher1.launch(uri)
                },
                onRetake = {
                    val file = createImageFile(context, "photo1_${System.currentTimeMillis()}")
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    photoFile1 = file
                    takePictureLauncher1.launch(uri)
                },
                onClear = {
                    photoBitmap1 = null
                    photoBase641 = null
                    photoFile1?.delete()
                    photoFile1 = null
                }
            )

            Spacer(Modifier.height(8.dp))
            PhotoSlot(
                label = "Zdjęcie 2",
                bitmap = photoBitmap2,
                onTake = {
                    val file = createImageFile(context, "photo2_${System.currentTimeMillis()}")
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    photoFile2 = file
                    takePictureLauncher2.launch(uri)
                },
                onRetake = {
                    val file = createImageFile(context, "photo2_${System.currentTimeMillis()}")
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    photoFile2 = file
                    takePictureLauncher2.launch(uri)
                },
                onClear = {
                    photoBitmap2 = null
                    photoBase642 = null
                    photoFile2?.delete()
                    photoFile2 = null
                }
            )

            Spacer(Modifier.height(8.dp))
            PhotoSlot(
                label = "Zdjęcie 3",
                bitmap = photoBitmap3,
                onTake = {
                    val file = createImageFile(context, "photo3_${System.currentTimeMillis()}")
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    photoFile3 = file
                    takePictureLauncher3.launch(uri)
                },
                onRetake = {
                    val file = createImageFile(context, "photo3_${System.currentTimeMillis()}")
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    photoFile3 = file
                    takePictureLauncher3.launch(uri)
                },
                onClear = {
                    photoBitmap3 = null
                    photoBase643 = null
                    photoFile3?.delete()
                    photoFile3 = null
                }
            )

            Spacer(Modifier.height(12.dp))

            // Przycisk Wyślij
            val vm = remember { SendVm() }
            Button(
                enabled = !isSending &&
                        userName.isNotBlank() &&
                        adres.trim().length >= 3 &&
                        (photoBase641 != null || photoBase642 != null || photoBase643 != null),
                onClick = {
                    if (typ == null) {
                        message = "Zaznacz typ zgłoszenia"
                        return@Button
                    }

                    isSending = true
                    message = null
                    Log.d("API_URL", Config.WEB_APP_URL)

                    // JSON z 1–3 zdjęciami (tylko istniejące)
                    val payload = JSONObject().apply {
                        put("sekret", Config.SECRET_TOKEN)
                        put("typ", typ!!)
                        put("ulica_adres", adres.trim())
                        put("opis", opis.trim())
                        put("uzytkownik", userName.trim())
                        put("urz_uuid", userId)
                        put("wersja_apki", Config.APP_VERSION)
                        put("timestamp_client", System.currentTimeMillis())

                        photoBase641?.let {
                            put("photo1", it)
                            put("fileName1", "zdjecie1_${System.currentTimeMillis()}.jpg")
                        }
                        photoBase642?.let {
                            put("photo2", it)
                            put("fileName2", "zdjecie2_${System.currentTimeMillis()}.jpg")
                        }
                        photoBase643?.let {
                            put("photo3", it)
                            put("fileName3", "zdjecie3_${System.currentTimeMillis()}.jpg")
                        }
                    }

                    Log.d("API_DEBUG", "URL: ${Config.WEB_APP_URL}")
                    Log.d("API_DEBUG", "Payload: $payload")

                    vm.send(
                        url = Config.WEB_APP_URL,
                        payload = payload
                    ) { ok, msg ->
                        isSending = false
                        message = if (ok) "OK" else "Błąd: $msg"

                        if (ok) {
                            // wyczyść formularz i zdjęcia
                            adres = ""
                            opis = ""
                            // Usuwanie plików i czyszczenie zmiennych
                            photoBitmap1 = null; photoBase641 = null; photoFile1?.delete(); photoFile1 = null
                            photoBitmap2 = null; photoBase642 = null; photoFile2?.delete(); photoFile2 = null
                            photoBitmap3 = null; photoBase643 = null; photoFile3?.delete(); photoFile3 = null
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSending) "Wysyłanie..." else "Wyślij")
            }

            if (message == "OK") {
                SuccessBanner("Zgłoszenie zostało wysłane ✅")
                LaunchedEffect(message) {
                    kotlinx.coroutines.delay(2500)
                    message = null
                }
            } else if (message != null) {
                Text(
                    message!!,
                    color = Color(0xFFB00020),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}



@Composable
fun SuccessBanner(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE8F5E9))      // jasna zieleń tła
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF2E7D32)            // ciemna zieleń ikonki
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = Color(0xFF2E7D32),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}


@Composable
fun NameDialog(onSave: (String) -> Unit) {
    var temp by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { /* brak zamknięcia bez zapisu */ },
        confirmButton = {
            TextButton(
                enabled = temp.trim().length >= 3,
                onClick = { onSave(temp.trim()) }
            ) { Text("Zapisz") }
        },
        title = { Text("Imię i nazwisko") },
        text = {
            OutlinedTextField(
                value = temp,
                onValueChange = { temp = it },
                placeholder = { Text("np. Anna Kowalska") }
            )
        }
    )
}



class SendVm : ViewModel() {
    private val client = OkHttpClient.Builder()
        .callTimeout(60, TimeUnit.SECONDS)     // cały call max 60s
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun send(
        url: String,
        payload: JSONObject,
        onDone: (Boolean, String) -> Unit
    ) {
        val jsonString = payload.toString()
        val body = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())

        val req = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onDone(false, "Błąd sieci: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val msg = it.body?.string() ?: ""
                    onDone(it.isSuccessful, msg)
                }
            }
        })
    }
}

    private suspend fun postJson(url: String, json: JSONObject): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                conn.outputStream.use { it.write(json.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = stream?.bufferedReader()?.readText().orEmpty()
                conn.disconnect()

                android.util.Log.d("API_RESPONSE", "Kod: $code, Body: $body")

                if (code in 200..299 && body.contains("\"status\":\"OK\"")) {
                    true to "Zapisano ✅"
                } else {
                    false to "Błąd serwera: $body"
                }
            } catch (e: Exception) {
                false to "Brak internetu lub błąd: ${e.message}"
            }
        }
    }


fun buildJson(
    typ: String,
    ulicaAdres: String,
    opis: String,
    user: String,
    uuid: String,
    appVersion: String,
    photoBase64: String?,         // <-- dodaj tu
    fileName: String?             // <-- i tu
): JSONObject = JSONObject().apply {
    put("sekret", Config.SECRET_TOKEN)
    put("typ", typ)
    put("ulica_adres", ulicaAdres)
    put("opis", opis)
    put("uzytkownik", user)
    put("urz_uuid", uuid)
    put("wersja_apki", appVersion)
    put("timestamp_client", System.currentTimeMillis())
    if (photoBase64 != null) put("photo", photoBase64)
    if (fileName != null) put("fileName", fileName)
}

fun bitmapToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
    val byteArray = outputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.NO_WRAP)
}
@Composable
fun PhotoSlot(
    label: String,
    bitmap: Bitmap?,
    onTake: () -> Unit,
    onRetake: () -> Unit,
    onClear: () -> Unit
) {
    var showFull by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)

        if (bitmap == null) {
            Button(onClick = onTake, modifier = Modifier.fillMaxWidth()) {
                Text("Zrób zdjęcie")
            }
        } else {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { showFull = true },
                contentScale = ContentScale.Crop
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = onRetake, modifier = Modifier.weight(1f)) { Text("Zmień") }
                OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text("Usuń") }
            }

            // PODGLĄD NA CAŁYM EKRANIE
            if (showFull) {
                Dialog(onDismissRequest = { showFull = false }) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .clickable { showFull = false },
                        contentAlignment = Alignment.Center
                    ) {
                        // Automatycznie dobiera proporcje
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(), // dodałem
                            contentScale = ContentScale.Fit
                        )
                        IconButton(
                            onClick = { showFull = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Zamknij",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

    }
}


// Tworzy docelowy Uri w MediaStore – aparat zapisze tu PEŁNE zdjęcie (nie miniaturę).
fun uriToBase64Jpeg(context: Context, uri: Uri): Pair<String, Bitmap?> {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
    Log.d("PHOTO_SIZE", "Photo size: ${bytes.size / 1024} KB")
    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
    val preview: Bitmap? = try {
        if (Build.VERSION.SDK_INT >= 28) {
            val src = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                val ow = info.size.width
                val oh = info.size.height
                val maxSide = 1280f
                val scale = minOf(maxSide / ow, maxSide / oh, 1f)
                val tw = (ow * scale).toInt().coerceAtLeast(1)
                val th = (oh * scale).toInt().coerceAtLeast(1)
                decoder.setTargetSize(tw, th)
            }
        } else null
    } catch (e: Exception) {
        Log.w("PHOTO_PREVIEW", "Preview decode failed: ${e.message}")
        null
    }
    return base64 to preview
}


fun createImageFile(context: Context, fileName: String): File {
    val storageDir = context.cacheDir
    return File.createTempFile(fileName, ".jpg", storageDir)
}

fun rotateBitmapIfRequired(bitmap: Bitmap, file: File): Bitmap {
    val ei = ExifInterface(file.absolutePath)
    val orientation = ei.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_UNDEFINED
    )

    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> {
            val matrix = Matrix()
            matrix.postRotate(90f)
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        ExifInterface.ORIENTATION_ROTATE_180 -> {
            val matrix = Matrix()
            matrix.postRotate(180f)
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> {
            val matrix = Matrix()
            matrix.postRotate(270f)
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        else -> bitmap
    }
}

