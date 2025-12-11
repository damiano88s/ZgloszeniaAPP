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
import androidx.compose.ui.zIndex

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalFocusManager

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.graphics.Brush

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp



private const val PHOTOS_ENABLED = false //import wylacza zdjecia






class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Czyść dane drafta przy każdym uruchomieniu aplikacji:
        applicationContext.clearDraft()   // <<< DODAJ TO!

        // Czyść pliki zdjęć
        clearPhotoCacheFiles()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }
        }

        setContent {
            com.example.zgloszeniaapp.ui.theme.ZgloszeniaAPPTheme {
                AppScreen()
            }
        }
    }


    // Funkcja czyści wszystkie pliki zaczynające się od "photo" z folderu cache
    private fun clearPhotoCacheFiles() {
        val cacheDir = cacheDir
        cacheDir?.listFiles()?.forEach { file ->
            if (file.name.startsWith("photo")) file.delete()
        }
    }

}

enum class GabarytCategory(val label: String) {
    ROZNE("różne"),
    BIO("BIO"),
    OPONY("opony")
}





@Composable
fun AppScreen() {

    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val draft = remember { context.loadDraft() }

    var userName by rememberSaveable { mutableStateOf(UserPrefs.getName(context) ?: "") }
    val userId = remember { UserPrefs.getOrCreateUuid(context) }

    var typ by rememberSaveable { mutableStateOf<String?>(draft.typ ?: Config.SHEET_GABARYTY) }
    var adres by rememberSaveable { mutableStateOf(draft.adres) }
    var opis by rememberSaveable { mutableStateOf(draft.opis) }

    // kategorie gabarytów
    var catRozne by rememberSaveable { mutableStateOf(false) }
    var catBio by rememberSaveable { mutableStateOf(false) }
    var catOpony by rememberSaveable { mutableStateOf(false) }

    // zdjęcia
    var photoFile1Path by rememberSaveable { mutableStateOf<String?>(null) }
    var photoBitmap1 by remember { mutableStateOf<Bitmap?>(null) }
    var photoFile2Path by rememberSaveable { mutableStateOf<String?>(null) }
    var photoBitmap2 by remember { mutableStateOf<Bitmap?>(null) }
    var photoFile3Path by rememberSaveable { mutableStateOf<String?>(null) }
    var photoBitmap3 by remember { mutableStateOf<Bitmap?>(null) }

    val takePictureLauncher1 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoFile1Path != null) {
            photoBitmap1 = decodeSampledBitmapFromFile(photoFile1Path!!, maxSide = 2048)
        }
    }
    val takePictureLauncher2 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoFile2Path != null) {
            photoBitmap2 = decodeSampledBitmapFromFile(photoFile2Path!!, maxSide = 2048)
        }
    }
    val takePictureLauncher3 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoFile3Path != null) {
            photoBitmap3 = decodeSampledBitmapFromFile(photoFile3Path!!, maxSide = 2048)
        }
    }

    var isSending by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showBanner by remember { mutableStateOf(false) }

    // auto-czyszczenie formularza po OK
    LaunchedEffect(showBanner, message) {
        if (showBanner && message == "OK") {
            adres = ""
            opis = ""
            photoBitmap1 = null
            photoFile1Path?.let { runCatching { File(it).delete() } }
            photoFile1Path = null
            photoBitmap2 = null
            photoFile2Path?.let { runCatching { File(it).delete() } }
            photoFile2Path = null
            photoBitmap3 = null
            photoFile3Path?.let { runCatching { File(it).delete() } }
            photoFile3Path = null

            context.clearDraft()

            kotlinx.coroutines.delay(3000)
            message = null
            showBanner = false
        }
    }

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTapGestures { focusManager.clearFocus() }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scroll)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // --- NAGŁÓWEK „ZGŁOSZENIA” + podkreślenie ---
                var titleWidthDp by remember { mutableStateOf(0.dp) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ZGŁOSZENIA",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 22.sp, //zmniejszamy czcionke zgłoszenia
                        fontWeight = FontWeight.Medium,
                        onTextLayout = { result ->
                            val widthPx = result.size.width
                            titleWidthDp = with(density) { widthPx.toDp() }   // <<< UŻYWAMY density
                        }
                    )


                    Spacer(Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .width(titleWidthDp)
                            .height(2.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(2.dp)
                            )
                    )
                }

                // --- Zakładki: Gabaryty / Zlecenia ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(0.8f), // dokładnie jak adres
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TypeTab(
                            label = "Gabaryty",
                            selected = typ == Config.SHEET_GABARYTY,
                            onClick = { typ = Config.SHEET_GABARYTY }
                        )
                        TypeTab(
                            label = "Zlecenia",
                            selected = typ == Config.SHEET_ZLECENIA,
                            onClick = { typ = Config.SHEET_ZLECENIA }
                        )
                    }
                }


            }


                // --- pole ADRES w stylu kreski jak w KD ---
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AddressField(
                        value = adres,
                        onValueChange = { adres = it },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                }

                // --- OPIS / TYP GABARYTÓW ---
                if (typ == Config.SHEET_GABARYTY) {
                    Text(
                        text = "Typ gabarytów",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Light,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 30.dp, bottom = 4.dp), // tu obnizamy napis typ gabarytow
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),    // <<< TU OBNIŻASZ OPCJE rozne/bio/opony
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CategoryOption(
                            text = "różne",
                            selected = catRozne,
                            onClick = { catRozne = !catRozne }
                        )
                        CategoryOption(
                            text = "BIO",
                            selected = catBio,
                            onClick = { catBio = !catBio }
                        )
                        CategoryOption(
                            text = "opony",
                            selected = catOpony,
                            onClick = { catOpony = !catOpony }
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = opis,
                        onValueChange = { opis = it },
                        label = { Text("Opis") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp)
                            .shadow(8.dp, RoundedCornerShape(18.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                                    )
                                ),
                                shape = RoundedCornerShape(18.dp)
                            ),
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                }

                // --- zdjęcia (jeśli PHOTOS_ENABLED) ---
                if (PHOTOS_ENABLED) {
                    Spacer(Modifier.height(8.dp))
                    PhotoSlot(
                        label = "Zdjęcie 1",
                        bitmap = photoBitmap1,
                        photoFilePath = photoFile1Path,
                        onTake = {
                            val file = createImageFile(context, "photo1_${System.currentTimeMillis()}")
                            val uri = FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", file
                            )
                            photoFile1Path = file.absolutePath
                            takePictureLauncher1.launch(uri)
                        },
                        onRetake = {
                            val file = createImageFile(context, "photo1_${System.currentTimeMillis()}")
                            val uri = FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", file
                            )
                            photoFile1Path = file.absolutePath
                            takePictureLauncher1.launch(uri)
                        },
                        onClear = {
                            photoBitmap1 = null
                            photoFile1Path?.let { path -> runCatching { File(path).delete() } }
                            photoFile1Path = null
                        }
                    )

                    Spacer(Modifier.height(8.dp))
                    PhotoSlot(
                        label = "Zdjęcie 2",
                        bitmap = photoBitmap2,
                        photoFilePath = photoFile2Path,
                        onTake = {
                            val file = createImageFile(context, "photo2_${System.currentTimeMillis()}")
                            val uri = FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", file
                            )
                            photoFile2Path = file.absolutePath
                            takePictureLauncher2.launch(uri)
                        },
                        onRetake = {
                            val file = createImageFile(context, "photo2_${System.currentTimeMillis()}")
                            val uri = FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", file
                            )
                            photoFile2Path = file.absolutePath
                            takePictureLauncher2.launch(uri)
                        },
                        onClear = {
                            photoBitmap2 = null
                            photoFile2Path?.let { File(it).delete() }
                            photoFile2Path = null
                        }
                    )

                    Spacer(Modifier.height(8.dp))
                    PhotoSlot(
                        label = "Zdjęcie 3",
                        bitmap = photoBitmap3,
                        photoFilePath = photoFile3Path,
                        onTake = {
                            val file = createImageFile(context, "photo3_${System.currentTimeMillis()}")
                            val uri = FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", file
                            )
                            photoFile3Path = file.absolutePath
                            takePictureLauncher3.launch(uri)
                        },
                        onRetake = {
                            val file = createImageFile(context, "photo3_${System.currentTimeMillis()}")
                            val uri = FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", file
                            )
                            photoFile3Path = file.absolutePath
                            takePictureLauncher3.launch(uri)
                        },
                        onClear = {
                            photoBitmap3 = null
                            photoFile3Path?.let { File(it).delete() }
                            photoFile3Path = null
                        }
                    )

                    Spacer(Modifier.height(12.dp))
                }

                // --- PRZYCISK WYŚLIJ ---
                val vm = remember { SendVm() }
                val hasOpis = opis.trim().isNotBlank()
                val hasPhoto = PHOTOS_ENABLED && (
                        photoFile1Path != null ||
                                photoFile2Path != null ||
                                photoFile3Path != null
                        )

                Button(
                    enabled = when (typ) {
                        Config.SHEET_GABARYTY ->
                            !isSending &&
                                    !showBanner &&
                                    userName.isNotBlank() &&
                                    adres.trim().length >= 3 &&
                                    (catRozne || catBio || catOpony)

                        Config.SHEET_ZLECENIA ->
                            !isSending &&
                                    !showBanner &&
                                    userName.isNotBlank() &&
                                    adres.trim().length >= 3 &&
                                    (hasOpis || hasPhoto)

                        else -> false
                    },
                    onClick = {
                        if (typ == null) {
                            message = "Zaznacz typ zgłoszenia"
                            return@Button
                        }
                        if (adres.trim().length < 3) {
                            message = "Podaj poprawny adres"
                            return@Button
                        }

                        when (typ) {
                            Config.SHEET_GABARYTY -> {
                                if (!catRozne && !catBio && !catOpony) {
                                    message = "Wybierz kategorię gabarytów"
                                    return@Button
                                }
                            }

                            Config.SHEET_ZLECENIA -> {
                                if (!hasOpis && !hasPhoto) {
                                    message = if (PHOTOS_ENABLED) {
                                        "Podaj opis lub dodaj zdjęcie"
                                    } else {
                                        "Podaj opis"
                                    }
                                    return@Button
                                }
                            }
                        }

                        val finalOpis = when (typ) {
                            Config.SHEET_GABARYTY -> {
                                val parts = mutableListOf<String>()
                                if (catRozne) parts.add("różne")
                                if (catBio) parts.add("BIO")
                                if (catOpony) parts.add("opony")
                                parts.joinToString(", ")
                            }

                            Config.SHEET_ZLECENIA -> opis.trim()
                            else -> opis.trim()
                        }

                        isSending = true
                        message = null
                        Log.d("API_URL", Config.WEB_APP_URL)

                        val payload = JSONObject().apply {
                            put("sekret", Config.SECRET_TOKEN)
                            put("typ", typ!!)
                            put("ulica_adres", adres.trim())
                            put("opis", finalOpis)
                            put("uzytkownik", userName.trim())
                            put("urz_uuid", userId)
                            put("wersja_apki", Config.APP_VERSION)
                            put("timestamp_client", System.currentTimeMillis())

                            photoFile1Path?.let { path ->
                                val f = File(path)
                                put("photo1", fileToBase64Original(f))
                                put(
                                    "fileName1",
                                    f.name.ifBlank { "zdjecie1_${System.currentTimeMillis()}.jpg" }
                                )
                            }
                            photoFile2Path?.let { path ->
                                val f = File(path)
                                put("photo2", fileToBase64Original(f))
                                put(
                                    "fileName2",
                                    f.name.ifBlank { "zdjecie2_${System.currentTimeMillis()}.jpg" }
                                )
                            }
                            photoFile3Path?.let { path ->
                                val f = File(path)
                                put("photo3", fileToBase64Original(f))
                                put(
                                    "fileName3",
                                    f.name.ifBlank { "zdjecie3_${System.currentTimeMillis()}.jpg" }
                                )
                            }
                        }

                        Log.d("API_DEBUG", "URL: ${Config.WEB_APP_URL}")
                        Log.d(
                            "API_DEBUG", "Payload keys: " +
                                    listOfNotNull(
                                        photoFile1Path?.let { "photo1" },
                                        photoFile2Path?.let { "photo2" },
                                        photoFile3Path?.let { "photo3" }
                                    )
                        )

                        vm.send(
                            url = Config.WEB_APP_URL,
                            payload = payload
                        ) { ok, msg ->
                            isSending = false
                            Log.d("RESPONSE_DEBUG", "Response message: $msg")
                            message = if (ok) "OK" else "Błąd: $msg"
                            showBanner = ok
                        }
                    },
                    modifier = Modifier
                        .wrapContentWidth()
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text(if (isSending) "Wysyłanie..." else "Wyślij")
                }

                Log.d("UI_DEBUG", "Stan message: $message, showBanner: $showBanner")
            }

            // baner nad wszystkim
            if (showBanner && message == "OK") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                        .zIndex(1f),
                    contentAlignment = Alignment.Center

                ) {
                    SuccessBanner(
                        "Zgłoszenie zostało wysłane ✅",
                        modifier = Modifier.padding(horizontal = 16.dp)
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
fun TypeTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    var textWidth by remember { mutableStateOf(0) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp)   // ← USUWAMY horizontal = 16.dp
    ) {
        // Tekst zakładki – mierzymy szerokość
        Box(
            modifier = Modifier.height(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 22.sp,     //zwiekszam czcionke gabaryty/zlecenia
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Light,
                color = if (selected) activeColor else inactiveColor,
                onTextLayout = { result ->
                    textWidth = result.size.width
                }
            )
        }

        // Podkreślenie dokładnie tak długie jak tekst
        Box(
            modifier = Modifier
                .height(8.dp)
                .width(with(LocalDensity.current) { textWidth.toDp() }),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .fillMaxWidth()
                        .background(activeColor, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}




    @Composable
    fun CategoryChip(
        label: String,
        selected: Boolean,
        onClick: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    if (selected)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (selected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
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
        .callTimeout(60, TimeUnit.SECONDS)
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
} // <-- Poprawnie zamknięta klasa

// ↓ Tę funkcję umieszczasz POZA klasą, na tym samym poziomie co inne funkcje
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
    photoFilePath: String?,          // <-- ścieżka do pliku, NIE File
    onTake: () -> Unit,
    onRetake: () -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
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
                    .clickable {
                        // pokaż pełny ekran jeśli mamy ścieżkę
                        photoFilePath?.let { path ->
                            val intent = Intent(context, PhotoViewActivity::class.java)
                            intent.putExtra("photo_path", path)
                            context.startActivity(intent)
                        }
                    },
                contentScale = ContentScale.Crop
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = onRetake, modifier = Modifier.weight(1f)) { Text("Zmień") }
                OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text("Usuń") }
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

// ====== DRAFT (auto-zapis formularza) ======
data class Draft(
    val typ: String?,
    val adres: String,
    val opis: String
)

object DraftKeys {
    const val FILE = "form_draft"
    const val TYP = "typ"
    const val ADRES = "adres"
    const val OPIS = "opis"

}

fun Context.saveDraft(d: Draft) {
    getSharedPreferences(DraftKeys.FILE, Context.MODE_PRIVATE).edit().apply {
        putString(DraftKeys.TYP, d.typ)
        putString(DraftKeys.ADRES, d.adres)
        putString(DraftKeys.OPIS, d.opis)
    }.apply()
}

fun Context.loadDraft(): Draft {
    val sp = getSharedPreferences(DraftKeys.FILE, Context.MODE_PRIVATE)
    return Draft(
        typ   = sp.getString(DraftKeys.TYP, null),
        adres = sp.getString(DraftKeys.ADRES, "") ?: "",
        opis  = sp.getString(DraftKeys.OPIS, "") ?: ""
    )
}

fun Context.clearDraft() {
    getSharedPreferences(DraftKeys.FILE, Context.MODE_PRIVATE).edit().clear().apply()
}

// (opcjonalne) szybkie dekodowanie Base64 -> mini podgląd bitmapy
fun base64ToBitmap(b64: String): Bitmap? = try {
    val bytes = android.util.Base64.decode(b64, android.util.Base64.NO_WRAP)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
} catch (_: Exception) { null }

// Odczyt ORYGINALNEGO pliku do Base64 (bez kompresji)
fun fileToBase64Original(file: java.io.File): String {
    val bytes = file.readBytes()
    return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
}

// Lekki podgląd do miniatury (żeby nie zjadało RAM-u). Pełny ekran otwierasz z pliku.
fun decodeSampledBitmapFromFile(path: String, maxSide: Int = 2048): android.graphics.Bitmap? =
    try {
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(path, bounds)
        val w = bounds.outWidth
        val h = bounds.outHeight
        if (w <= 0 || h <= 0) {
            null
        } else {
            var inSample = 1
            val bigger = maxOf(w, h)
            while ((bigger / inSample) > maxSide) inSample *= 2
            val opts = android.graphics.BitmapFactory.Options().apply {
                inSampleSize = inSample
                inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
            }
            android.graphics.BitmapFactory.decodeFile(path, opts)
        }
    } catch (_: Exception) {
        null
    }


fun screenMaxSide(ctx: Context): Int {
    val dm = ctx.resources.displayMetrics
    return maxOf(dm.widthPixels, dm.heightPixels)
}
@Composable
fun UnderlineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
        textStyle = textStyle,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
            disabledIndicatorColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun AddressField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lineColor = if (value.trim().isNotEmpty()) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Column(modifier = modifier) {

        TextField(
            value = value,
            onValueChange = { rawText ->
                // Czy na końcu jest spacja?
                val hasTrailingSpace = rawText.endsWith(" ")

                // Usuwamy spacje TYLKO z końca, ale zapamiętujemy, że była
                val core = rawText.trimEnd()

                // Zamiana na "Każde Słowo z Dużej"
                val normalizedCore = core
                    .split(' ')
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { word ->
                        val lower = word.lowercase()
                        lower.replaceFirstChar { ch ->
                            if (ch.isLowerCase()) ch.titlecase() else ch.toString()
                        }
                    }

                // Jeśli użytkownik wcisnął spację na końcu, to ją przywracamy
                val finalText = if (hasTrailingSpace) {
                    if (normalizedCore.isEmpty()) " " else normalizedCore + " "
                } else {
                    normalizedCore
                }

                onValueChange(finalText)
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 0.dp),
            placeholder = {
                Text(
                    text = "Wpisz nazwę ulicy",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 22.sp, //zwiekszanie napisu
                    fontWeight = FontWeight.Light
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy( //zwiekszanie czcionki wpisywanego adresu
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium
            ),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                autoCorrect = true,
                keyboardType = KeyboardType.Text
            )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(lineColor)
        )
    }
}

@Composable
fun CategoryOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {

        // Tekst – stała wysokość, żeby nic nie skakało
        Box(
            modifier = Modifier
                .height(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 22.sp, //zwiekszamy czcionke rozne/bio/opony
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) activeColor else inactiveColor
            )
        }

        // Podkreślenie – DŁUŻSZE, ale stałe (np. 60.dp)
        Box(
            modifier = Modifier
                .height(8.dp)
                .width(60.dp),              // <<< TU DŁUGOŚĆ KRESKI
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .width(60.dp)      // <<< TO SAMO CO WYŻEJ
                        .background(activeColor, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}


