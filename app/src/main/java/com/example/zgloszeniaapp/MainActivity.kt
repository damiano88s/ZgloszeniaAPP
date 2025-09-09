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






class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppScreen() }
    }
}

@Composable
fun AppScreen() {
    val context = LocalContext.current

    var userName by remember { mutableStateOf(UserPrefs.getName(context) ?: "") }
    val userId = remember { UserPrefs.getOrCreateUuid(context) }

    // Pola formularza
    var typ by remember { mutableStateOf<String?>(null) } // "Gabaryty" / "Zlecenia"
    var adres by remember { mutableStateOf("") }
    var opis by remember { mutableStateOf("") }

    // 3 zdjęcia (bitmapy + base64)
    var photoBitmap1 by remember { mutableStateOf<Bitmap?>(null) }
    var photoBitmap2 by remember { mutableStateOf<Bitmap?>(null) }
    var photoBitmap3 by remember { mutableStateOf<Bitmap?>(null) }

    var photoBase641 by remember { mutableStateOf<String?>(null) }
    var photoBase642 by remember { mutableStateOf<String?>(null) }
    var photoBase643 by remember { mutableStateOf<String?>(null) }

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

    // --- 3 launchery aparatu (po jednym na slot) ---
    val launcher1 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bm: Bitmap? ->
        photoBitmap1 = bm
        photoBase641 = bm?.let { bitmapToBase64(it) }
    }
    val launcher2 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bm: Bitmap? ->
        photoBitmap2 = bm
        photoBase642 = bm?.let { bitmapToBase64(it) }
    }
    val launcher3 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bm: Bitmap? ->
        photoBitmap3 = bm
        photoBase643 = bm?.let { bitmapToBase64(it) }
    }

    Scaffold { padding ->
        val scroll = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(scroll)      // 👈 DODAJ
                .imePadding(),               // klawiatura nie zasłania pól
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

            // --- 3 sloty zdjęć ---
            Spacer(Modifier.height(8.dp))
            PhotoSlot(
                label = "Zdjęcie 1",
                bitmap = photoBitmap1,
                onTake = { launcher1.launch(null) },
                onRetake = { launcher1.launch(null) },
                onClear = { photoBitmap1 = null; photoBase641 = null }
            )

            Spacer(Modifier.height(8.dp))
            PhotoSlot(
                label = "Zdjęcie 2",
                bitmap = photoBitmap2,
                onTake = { launcher2.launch(null) },
                onRetake = { launcher2.launch(null) },
                onClear = { photoBitmap2 = null; photoBase642 = null }
            )

            Spacer(Modifier.height(8.dp))
            PhotoSlot(
                label = "Zdjęcie 3",
                bitmap = photoBitmap3,
                onTake = { launcher3.launch(null) },
                onRetake = { launcher3.launch(null) },
                onClear = { photoBitmap3 = null; photoBase643 = null }
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
                        message = msg
                        if (ok) {
                            adres = ""
                            opis = ""
                            photoBitmap1 = null; photoBase641 = null
                            photoBitmap2 = null; photoBase642 = null
                            photoBitmap3 = null; photoBase643 = null
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSending) "Wysyłanie..." else "Wyślij")
            }

            if (message != null) {
                Text(message!!, style = MaterialTheme.typography.bodyMedium)
            }
        }
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
    fun send(
        url: String,
        payload: JSONObject,
        onDone: (Boolean, String) -> Unit
    ) = viewModelScope.launch {
        val result = postJson(url, payload)
        onDone(result.first, result.second)
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

            if (showFull) {
                AlertDialog(
                    onDismissRequest = { showFull = false },
                    confirmButton = {
                        TextButton(onClick = { showFull = false }) { Text("Zamknij") }
                    },
                    text = {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 240.dp),
                            contentScale = ContentScale.Fit
                        )

                    }
                )
            }
        }
    }
}


