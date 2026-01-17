package com.example.zgloszeniaapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


@Composable
fun OdsniezanieScreen(
    padding: PaddingValues,
    adres: String,
    onAdresChange: (String) -> Unit,
    czas: String,
    onCzasChange: (String) -> Unit,
    photoPaths: SnapshotStateList<String>,
    onClearAfterSend: () -> Unit
) {



    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scroll = rememberScrollState()

    // ====== STANY ======





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

                    if (filtered.count { it == ',' } <= 1) {
                        onCzasChange(filtered.take(4)) // 1,5 / 2 / 10,5
                    }
                },
                placeholderText = "Czas",
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
                            decodeSampledBitmapFromFileRotated(path, maxSide = 512) // miniatura
                        }

                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { previewIndex = index },
                                contentScale = ContentScale.Crop
                            )
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

            // (na razie bez "Wyślij" — dopniemy jak już UI będzie 100% OK)
        }
    }
}

