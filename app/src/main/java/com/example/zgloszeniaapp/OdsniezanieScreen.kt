package com.example.zgloszeniaapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.delay



@Composable
fun OdsniezanieScreen(
    padding: PaddingValues,
    userName: String,
    userId: String,
    adres: String,
    onAdresChange: (String) -> Unit,
    czas: String,
    onCzasChange: (String) -> Unit,
    photoPaths: SnapshotStateList<String>,
    onClearAfterSend: () -> Unit
) {


    val photos = remember { mutableStateListOf<Bitmap>() }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scroll = rememberScrollState()

    var isSending by rememberSaveable { mutableStateOf(false) }
    var sendError by rememberSaveable { mutableStateOf<String?>(null) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var showBanner by rememberSaveable { mutableStateOf(false) }

    var photo1 by remember { mutableStateOf<Bitmap?>(null) }
    var photo2 by remember { mutableStateOf<Bitmap?>(null) }
    var photo3 by remember { mutableStateOf<Bitmap?>(null) }


    val vm = remember { SendVm() }

    val canSave =
        userName.isNotBlank() &&
                adres.trim().length >= 3 &&
                czas.trim().isNotBlank() &&
                !czas.trim().startsWith(",") &&
                photoPaths.isNotEmpty() // na razie wymagamy min. 1 zdjęcia

    LaunchedEffect(showBanner, message) {
        if (showBanner && message == "OK") {
            delay(5000)
            showBanner = false
        }
    }


    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // wiele zdjęć

    var previewIndex by rememberSaveable { mutableIntStateOf(-1) } // -1 = brak podglądu

    // ====== APARAT (1 przycisk -> dodaje kolejne zdjęcie) ======
    var pendingPhotoPath by remember { mutableStateOf<String?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val path = pendingPhotoPath
        if (success && path != null) {
            photoPaths.add(path) // zapisujemy TYLKO ścieżkę
        } else {
            path?.let { runCatching { File(it).delete() } }
        }
        pendingPhotoPath = null

    }

    lateinit var takePhoto: () -> Unit

    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) takePhoto()
        else Toast.makeText(context, "Brak zgody na aparat", Toast.LENGTH_SHORT).show()
    }

    takePhoto = {
        try {
            val file = createImageFile(context, "photo_odsniezanie_")
            pendingPhotoPath = file.absolutePath

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Błąd aparatu: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ====== UI – RAMA jak w Wodomierzach ======
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
    ) {
        ZntLogoBackground(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 0.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scroll)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScreenTitleWithUnderline(title = "ODŚNIEŻANIE")
            Spacer(Modifier.height(16.dp))

            // ====== ADRES (jak wszędzie) ======
            AddressField(
                value = adres,
                onValueChange = onAdresChange,
                modifier = Modifier.fillMaxWidth(0.8f)
            )

// ====== CZAS (jedno pole!) ======
            AddressFieldWithPlaceholder(
                value = czas,
                onValueChange = { new ->
                    val filtered = new
                        .replace('.', ',')
                        .filter { it.isDigit() || it == ',' }

                    // nie pozwól, żeby pierwszy znak był przecinkiem
                    val noLeadingComma =
                        if (filtered.startsWith(",")) filtered.drop(1) else filtered

                    // max jeden przecinek
                    if (noLeadingComma.count { it == ',' } <= 1) {
                        onCzasChange(noLeadingComma.take(4)) // 1,5 / 2 / 10,5
                    }
                },
                placeholderText = "Czas [h]",
                modifier = Modifier
                    .width(130.dp)
                    .align(Alignment.CenterHorizontally),
                keyboardType = KeyboardType.Number,
                capitalization = KeyboardCapitalization.None
            )



            Spacer(Modifier.height(8.dp))

            // ====== JEDEN PRZYCISK: DODAJ KOLEJNE ZDJĘCIE ======
            Button(
                onClick = {
                    val hasCameraPermission =
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                    if (hasCameraPermission) takePhoto()
                    else cameraPermLauncher.launch(Manifest.permission.CAMERA)
                },
                modifier = Modifier.wrapContentWidth()
            ) {
                Text(
                    text = if (photoPaths.isEmpty()) "Zrób zdjęcie" else "Dodaj kolejne zdjęcie"
                )

            }

            // ====== MINIATURKI ======
            if (photoPaths.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    itemsIndexed(photoPaths) { index, path ->
                        val bmp = remember(path) {
                            decodeSampledBitmapFromFileRotated(path, maxSide = 512)
                        }

                        if (bmp != null) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { previewIndex = index },
                                    contentScale = ContentScale.Crop
                                )

                                // 🗑️ USUŃ zdjęcie (czytelne dla usera)
                                IconButton(
                                    onClick = {
                                        if (previewIndex == index) previewIndex = -1
                                        else if (previewIndex > index) previewIndex -= 1

                                        runCatching { File(path).delete() }
                                        photoPaths.removeAt(index)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(36.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Usuń zdjęcie"
                                    )
                                }


                            }

                        }
                    }


                }

            }

            // ====== PODGLĄD FULLSCREEN (jeśli masz gotowy dialog) ======
            if (previewIndex >= 0 && previewIndex < photoPaths.size) {
                val fullBmp = remember(previewIndex, photoPaths.toList()) {
                    decodeSampledBitmapFromFileRotated(photoPaths[previewIndex], maxSide = 2048)
                }

                if (fullBmp != null) {
                    FullscreenImageDialog(
                        bitmap = fullBmp,
                        onClose = { previewIndex = -1 }
                    )
                }
            }


            Spacer(Modifier.height(12.dp))

            Button(
                enabled = canSave && !isSending,
                onClick = {
                    focusManager.clearFocus()
                    sendError = null
                    isSending = true

                    val encodedPhotos = photoPaths.mapNotNull { path ->
                        runCatching {
                            fileToBase64Original(File(path))
                        }.getOrNull()
                    }


                    val payload = buildJsonOdsniezanie(
                        adres = adres.trim(),
                        czas = czas.trim(),
                        user = userName,
                        uuid = userId,
                        appVersion = BuildConfig.VERSION_NAME,
                        photos = encodedPhotos, // 👈 dynamiczna lista
                        unit = "ZNT"
                    )



                    vm.send(
                        url = Config.WEB_APP_URL,
                        payload = payload
                    ) { ok, msg ->
                        isSending = false
                        sendError = "RESP: $msg"

                        if (ok && msg.contains("\"status\":\"OK\"")) {
                            onClearAfterSend()
                            message = "OK"
                            showBanner = true
                            sendError = null
                        } else {
                            message = "ERR"
                            showBanner = false
                        }
                    }
                }
            ) {
                Text(if (isSending) "Wysyłam..." else "Wyślij")
            }

            sendError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }


            if (showBanner && message == "OK") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .imePadding(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    SuccessBanner(
                        "Zgłoszenie zostało wysłane",
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )
                }
            }

        }
    }
}





