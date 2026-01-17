package com.example.zgloszeniaapp

// Android core
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.Toast
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.content.ContentValues
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.concurrent.TimeUnit

// Activity / lifecycle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

// Compose runtime
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect

// Compose UI core
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp

// Compose foundation
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions

// Compose Material 3
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text

// Material icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale

// Window / dialog
import androidx.compose.ui.window.Dialog

// Keyboard / text
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density

// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Networking (OkHttp)
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

// File provider / EXIF
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface

// App theme
import com.example.zgloszeniaapp.ui.theme.ZgloszeniaAPPTheme
import org.json.JSONObject

import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.core.content.ContextCompat
import com.example.zgloszeniaapp.GrafikScreen

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

import kotlinx.coroutines.delay
import android.Manifest

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.saveable.listSaver


private const val PHOTOS_ENABLED = false //import wylacza zdjecia






class MainActivity : ComponentActivity() {

    private var grafikReadyForSplash: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {

        // systemowy splash + trzymanie do czasu pobrania grafiku
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !grafikReadyForSplash }

        // (opcjonalnie) miękkie zejście, żeby nie było “pstryk”
        splashScreen.setOnExitAnimationListener { view ->
            android.animation.ObjectAnimator.ofFloat(view.view, "alpha", 1f, 0f).apply {
                duration = 150
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        view.remove()
                    }
                })
                start()
            }
        }

        super.onCreate(savedInstanceState)


        // Czyść dane drafta przy każdym uruchomieniu aplikacji
        applicationContext.clearDraft()

        // Czyść pliki zdjęć
        clearPhotoCacheFiles()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }
        }

        // 2️⃣ Pobieranie grafiku w tle (splash nadal widoczny)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                GrafikDownload.ensureLocalFile(
                    applicationContext,
                    GrafikConfig.GRAFIK_URL
                )
            } catch (e: Exception) {
                // na razie ignorujemy
            } finally {
                withContext(Dispatchers.Main) {
                    grafikReadyForSplash = true
                }
            }
        }

        setContent {
            ZgloszeniaAPPTheme(darkTheme = false) {
                AppScreen()
            }
        }
    }

    // ✅ TA FUNKCJA MA BYĆ TYLKO TU
    private fun clearPhotoCacheFiles() {
        val cacheDir = cacheDir
        cacheDir?.listFiles()?.forEach { file ->
            if (file.name.startsWith("photo")) file.delete()
        }
    }
}
@Composable
private fun FullImageSplash(
    durationMs: Int,
    imageRes: Int
) {
    var visible by remember { mutableStateOf(true) }
    val alpha = remember { androidx.compose.animation.core.Animatable(1f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(durationMs.toLong())
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = androidx.compose.animation.core.tween(150)
        )
        visible = false
    }

    if (!visible) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFFE6E6E6)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            modifier = Modifier.graphicsLayer(alpha = alpha.value)
        )
    }
}

@Composable
private fun SplashOverlay(onFinished: () -> Unit) {
    val logoPainter = painterResource(id = R.drawable.logo)

    val scale = remember { androidx.compose.animation.core.Animatable(1.0f) }

    LaunchedEffect(Unit) {
        scale.snapTo(1.0f) // start = jak na systemowym splashu
        scale.animateTo(
            targetValue = 1.25f,  // możesz dać 1.20f–1.35f według gustu
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 3000,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        )
        onFinished()
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFFE6E6E6)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = logoPainter,
            contentDescription = null,
            modifier = Modifier.graphicsLayer(
                scaleX = scale.value,
                scaleY = scale.value
            )
        )
    }
}


enum class GabarytCategory(val label: String) {
    ROZNE("różne"),
    BIO("BIO"),
    OPONY("opony")
}

enum class Screen {
    ZGLOSZENIA,
    WODOMIERZE,
    GRAFIK,
    ODSNIEZANIE
}



@OptIn(ExperimentalMaterial3Api::class)



@Composable
fun AppScreen() {

    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        GrafikCache.preload(context.applicationContext)
    }

    var pendingScreen by remember { mutableStateOf<Screen?>(null) }

    // ================== GRAFIK – sprawdzanie przy starcie aplikacji ==================

    var grafikAllRows by remember { mutableStateOf<List<GrafikRow>>(emptyList()) }
    //var grafikLoading by remember { mutableStateOf(false) }
    var grafikError by remember { mutableStateOf<String?>(null) }
    var grafikLoadedOnce by remember { mutableStateOf(false) }
    var grafikReady by rememberSaveable { mutableStateOf(false) }
    var firstInstallInfo by rememberSaveable { mutableStateOf(false) }


    // ====== DANE GRAFIKU W PAMIĘCI ======


    LaunchedEffect(Unit) {
        if (grafikLoadedOnce) return@LaunchedEffect

        grafikError = null

        try {
            val file = GrafikDownload.localFile(context)
            val rows = withContext(kotlinx.coroutines.Dispatchers.IO) {
                file.inputStream().use { GrafikExcelReader.read(it) }
            }
            grafikAllRows = rows
        } catch (e: Exception) {
            if (grafikLoadedOnce) {
                grafikError = "Nie udało się wczytać grafiku"
            }
        } finally {
            grafikLoadedOnce = true
        }
    }



    LaunchedEffect(Unit) {
        if (grafikLoadedOnce) return@LaunchedEffect

        grafikError = null
        firstInstallInfo = false

        val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val everLoadedKey = "grafik_ever_loaded"
        val grafikEverLoaded = prefs.getBoolean(everLoadedKey, false)

        try {
            // 1) upewnij się, że plik jest lokalnie
            val file = GrafikDownload.ensureLocalFile(context, GrafikConfig.GRAFIK_URL)

            // 2) spróbuj odczytać excel do listy
            val rows = withContext(kotlinx.coroutines.Dispatchers.IO) {
                file.inputStream().use { GrafikExcelReader.read(it) }
            }
            grafikAllRows = rows
            grafikReady = true

            // 3) zapamiętaj, że grafik chociaż raz zadziałał
            prefs.edit().putBoolean(everLoadedKey, true).apply()

        } catch (e: Exception) {
            grafikReady = false

            if (!grafikEverLoaded) {
                // ✅ świeża instalacja / pierwszy start
                firstInstallInfo = true
            } else {
                // ✅ jeśli kiedyś działało, to realny błąd
                grafikError = "Nie udało się wczytać grafiku"
            }
        } finally {
            grafikLoadedOnce = true
        }
    }


// ================================================================================



    val draft = remember { context.loadDraft() }

    var userName by rememberSaveable { mutableStateOf(UserPrefs.getName(context) ?: "") }
    val userId = remember { UserPrefs.getOrCreateUuid(context) }

    var typ by rememberSaveable { mutableStateOf(draft.typ ?: Config.SHEET_GABARYTY) }
    var typPicked by rememberSaveable { mutableStateOf(false) }

    var screen by rememberSaveable { mutableStateOf(Screen.ZGLOSZENIA) }
    BackHandler(enabled = screen == Screen.WODOMIERZE) {
        screen = Screen.ZGLOSZENIA
    }

    BackHandler(enabled = screen == Screen.GRAFIK) {
        screen = Screen.ZGLOSZENIA
    }


    var menuExpanded by remember { mutableStateOf(false) }


    var adres by rememberSaveable { mutableStateOf(draft.adres) }
    var opis by rememberSaveable { mutableStateOf(draft.opis) }

    // --- WODOMIERZE: stan formularza (trwa dopóki nie wyślesz)
    var wodAdres by rememberSaveable { mutableStateOf("") }
    var wodNumer by rememberSaveable { mutableStateOf("") }
    var wodStan by rememberSaveable { mutableStateOf("") }
    var wodPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var wodPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var odsnAdres by rememberSaveable { mutableStateOf("") }
    var odsnCzas by rememberSaveable { mutableStateOf("") }

// zdjęcia: trzymamy ścieżki (bitmaps nie zapisujemy “na stałe”)
    val odsnPhotoPaths = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf<String>() }


    // kategorie gabarytów
    var catRozne by rememberSaveable { mutableStateOf(false) }
    var catBio by rememberSaveable { mutableStateOf(false) }
    var catOpony by rememberSaveable { mutableStateOf(false) }

    // STAN GRAFIKU – ma żyć dopóki apka działa
    var grafikSelectedName by remember { mutableStateOf<String?>(null) }
    var grafikSearchQuery by remember { mutableStateOf("") }
    var grafikSelectedDozorca by remember { mutableStateOf<String?>(null) } // jeśli masz


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

    fun clearWodomierze() {
        wodAdres = ""
        wodNumer = ""
        wodStan = ""
        wodPhotoPath?.let { runCatching { File(it).delete() } }
        wodPhotoPath = null
        wodPhotoBitmap = null
    }

    LaunchedEffect(menuExpanded) {
        if (!menuExpanded && pendingScreen != null) {
            screen = pendingScreen!!
            pendingScreen = null
        }
    }


    // auto-czyszczenie formularza po OK
    LaunchedEffect(showBanner, message) {
        if (showBanner && message == "OK") {
            adres = ""
            opis = ""

            // reset wyboru gabarytów ⬅⬅⬅ TO JEST WAŻNE
            catRozne = false
            catBio = false
            catOpony = false
            typ = Config.SHEET_GABARYTY
            typPicked = false



            photoBitmap1 = null
            photoFile1Path?.let { runCatching { File(it).delete() } }
            photoFile1Path = null
            photoBitmap2 = null
            photoFile2Path?.let { runCatching { File(it).delete() } }
            photoFile2Path = null
            photoBitmap3 = null
            photoFile3Path?.let { runCatching { File(it).delete() } }
            photoFile3Path = null

            clearWodomierze()
            context.clearDraft()

            kotlinx.coroutines.delay(3000)
            message = null
            showBanner = false
            delay(3000)
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        focusManager.clearFocus()
                    }
            ) {

                // ====== TREŚĆ EKRANU ======
                when (screen) {

                    Screen.ZGLOSZENIA -> {
                        ZgloszeniaScreen(
                            padding = padding,
                            focusManager = focusManager,
                            density = density,

                            typ = typ,
                            onTypChange = { typ = it },

                            typPicked = typPicked,
                            onTypPickedChange = { typPicked = it },

                            adres = adres,
                            onAdresChange = { adres = it },

                            opis = opis,
                            onOpisChange = { opis = it },

                            catRozne = catRozne,
                            onCatRozne = { catRozne = it },

                            catBio = catBio,
                            onCatBio = { catBio = it },

                            catOpony = catOpony,
                            onCatOpony = { catOpony = it },

                            isSending = isSending,
                            onIsSending = { isSending = it },

                            message = message,
                            onMessage = { message = it },

                            showBanner = showBanner,
                            onShowBanner = { showBanner = it },

                            userName = userName,
                            userId = userId,

                            photoFile1Path = photoFile1Path,
                            photoFile2Path = photoFile2Path,
                            photoFile3Path = photoFile3Path
                        )
                    }

                    Screen.WODOMIERZE -> {
                        WodomierzeScreen(
                            padding = padding,
                            userName = userName,
                            userId = userId,

                            adres = wodAdres,
                            onAdresChange = { wodAdres = it },

                            numerWodomierza = wodNumer,
                            onNumerWodomierzaChange = { wodNumer = it },

                            stan = wodStan,
                            onStanChange = { wodStan = it },

                            photoPath = wodPhotoPath,
                            onPhotoPathChange = { wodPhotoPath = it },

                            photoBitmap = wodPhotoBitmap,
                            onPhotoBitmapChange = { wodPhotoBitmap = it },

                            onAfterSendClear = {
                                wodAdres = ""
                                wodNumer = ""
                                wodStan = ""
                                wodPhotoPath?.let { runCatching { File(it).delete() } }
                                wodPhotoPath = null
                                wodPhotoBitmap = null
                            }
                        )
                    }

                    Screen.ODSNIEZANIE -> {
                        OdsniezanieScreen(
                            padding = padding,
                            adres = odsnAdres,
                            onAdresChange = { odsnAdres = it },
                            czas = odsnCzas,
                            onCzasChange = { odsnCzas = it },
                            photoPaths = odsnPhotoPaths,
                            onClearAfterSend = {
                                odsnAdres = ""
                                odsnCzas = ""
                                // usuń pliki + wyczyść listę
                                odsnPhotoPaths.forEach { path -> runCatching { File(path).delete() } }
                                odsnPhotoPaths.clear()
                            }
                        )

                    }

                    Screen.GRAFIK -> {
                        GrafikScreen(
                            padding = padding,

                            selectedName = grafikSelectedName,
                            onSelectedNameChange = { grafikSelectedName = it },

                            searchQuery = grafikSearchQuery,
                            onSearchQueryChange = { grafikSearchQuery = it },

                            selectedDozorca = grafikSelectedDozorca,
                            onSelectedDozorcaChange = { grafikSelectedDozorca = it },

                            allRows = grafikAllRows,
                            loading = false,
                            error = grafikError,
                            firstInstallInfo = firstInstallInfo
                        )
                    }


                }

                // ====== MENU (3 KROPKI) ======
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .zIndex(10f)
                ) {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.padding(top = 30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu"
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {

                        DropdownMenuItem(
                            text = { Text("Wodomierze") },
                            onClick = {
                                menuExpanded = false
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    screen = Screen.WODOMIERZE
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Odśnieżanie") },
                            onClick = {
                                menuExpanded = false
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    screen = Screen.ODSNIEZANIE
                                }
                            }
                        )

                        



                        DropdownMenuItem(
                            text = { Text("Grafik") },
                            onClick = {
                                menuExpanded = false
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    screen = Screen.GRAFIK
                                }
                            }
                        )



                    }
                }
            }
        }
    }

    @Composable
fun ZgloszeniaScreen(
    padding: PaddingValues,
    focusManager: androidx.compose.ui.focus.FocusManager,
    density: Density,
    typ: String?,
    onTypChange: (String) -> Unit,
    typPicked: Boolean,
    onTypPickedChange: (Boolean) -> Unit,
    adres: String,
    onAdresChange: (String) -> Unit,
    opis: String,
    onOpisChange: (String) -> Unit,
    catRozne: Boolean,
    onCatRozne: (Boolean) -> Unit,
    catBio: Boolean,
    onCatBio: (Boolean) -> Unit,
    catOpony: Boolean,
    onCatOpony: (Boolean) -> Unit,
    isSending: Boolean,
    onIsSending: (Boolean) -> Unit,
    message: String?,
    onMessage: (String?) -> Unit,
    showBanner: Boolean,
    onShowBanner: (Boolean) -> Unit,
    userName: String,
    userId: String,
    photoFile1Path: String?,
    photoFile2Path: String?,
    photoFile3Path: String?
) {
    val scroll = rememberScrollState()
    val vm = remember { SendVm() }

    val hasOpis = opis.trim().isNotBlank()
    val hasPhoto = PHOTOS_ENABLED && (
            photoFile1Path != null || photoFile2Path != null || photoFile3Path != null
            )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
    ) {

        // ✅ LOGO TŁA (TU)
        ZntLogoBackground(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 0.dp)
        )

        // ✅ UI (TU)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scroll)
                .imePadding()
                .zIndex(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            ScreenTitleWithUnderline(title = "ZGŁOSZENIA")

            Spacer(Modifier.height(30.dp))

            // --- Zakładki: Gabaryty / Zlecenia ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 0.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TypeTab(
                        label = "Gabaryty",
                        selected = typPicked && typ == Config.SHEET_GABARYTY,
                        onClick = {
                            onTypChange(Config.SHEET_GABARYTY)
                            onTypPickedChange(true)
                        }
                    )

                    TypeTab(
                        label = "Zlecenia",
                        selected = typPicked && typ == Config.SHEET_ZLECENIA,
                        onClick = {
                            onTypChange(Config.SHEET_ZLECENIA)
                            onTypPickedChange(true)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- pole ADRES ---
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AddressField(
                    value = adres,
                    onValueChange = { onAdresChange(it) },
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            }

            // --- OPIS / TYP GABARYTÓW ---
            if (typ == Config.SHEET_GABARYTY) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    var textWidthDp by remember { mutableStateOf(0.dp) }
                    val density = LocalDensity.current

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 0.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Typ gabarytów:",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            onTextLayout = { result ->
                                textWidthDp = with(density) { result.size.width.toDp() }
                            }
                        )

                        Spacer(Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .width(textWidthDp)
                                .height(2.dp) // ⬅️ grubość kreski
                                .background(
                                    MaterialTheme.colorScheme.outline, // ⬅️ kolor jak NIEAKTYWNY adres
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }


                    Spacer(modifier = Modifier.height(4.dp))


                }


                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CategoryOption(
                        text = "różne",
                        selected = catRozne,
                        onClick = {
                            onCatRozne(!catRozne)
                            focusManager.clearFocus()
                        }
                    )

                    CategoryOption(
                        text = "BIO",
                        selected = catBio,
                        onClick = {
                            onCatBio(!catBio)
                            focusManager.clearFocus()
                        }
                    )

                    CategoryOption(
                        text = "opony",
                        selected = catOpony,
                        onClick = {
                            onCatOpony(!catOpony)
                            focusManager.clearFocus()
                        }
                    )
                }

            } else {
                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    OpisFieldUnderline(
                        value = opis,
                        onValueChange = { onOpisChange(it) },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                }
            }

            // --- zdjęcia (jeśli PHOTOS_ENABLED) ---
            if (PHOTOS_ENABLED) {
                Spacer(Modifier.height(8.dp))
                Spacer(Modifier.height(12.dp))
            }

            // --- PRZYCISK WYŚLIJ + BANER POD NIM ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
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
                        focusManager.clearFocus()
                        if (typ == null) {
                            onMessage("Zaznacz typ zgłoszenia")
                            return@Button
                        }
                        if (adres.trim().length < 3) {
                            onMessage("Podaj poprawny adres")
                            return@Button
                        }

                        when (typ) {
                            Config.SHEET_GABARYTY -> {
                                if (!catRozne && !catBio && !catOpony) {
                                    onMessage("Wybierz kategorię gabarytów")
                                    return@Button
                                }
                            }
                            Config.SHEET_ZLECENIA -> {
                                if (!hasOpis && !hasPhoto) {
                                    onMessage(if (PHOTOS_ENABLED) "Podaj opis lub dodaj zdjęcie" else "Podaj opis")
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

                        onIsSending(true)
                        onMessage(null)

                        val payload = JSONObject().apply {
                            put("sekret", Config.SECRET_TOKEN)
                            put("typ", typ!!)
                            put("ulica_adres", adres.trim())
                            put("opis", finalOpis)
                            put("uzytkownik", userName.trim())
                            put("urz_uuid", userId)
                            put("wersja_apki", BuildConfig.VERSION_NAME)
                            put("timestamp_client", System.currentTimeMillis())

                            photoFile1Path?.let { path ->
                                val f = File(path)
                                put("photo1", fileToBase64Original(f))
                                put("fileName1", f.name.ifBlank { "zdjecie1_${System.currentTimeMillis()}.jpg" })
                            }
                            photoFile2Path?.let { path ->
                                val f = File(path)
                                put("photo2", fileToBase64Original(f))
                                put("fileName2", f.name.ifBlank { "zdjecie2_${System.currentTimeMillis()}.jpg" })
                            }
                            photoFile3Path?.let { path ->
                                val f = File(path)
                                put("photo3", fileToBase64Original(f))
                                put("fileName3", f.name.ifBlank { "zdjecie3_${System.currentTimeMillis()}.jpg" })
                            }
                        }

                        vm.send(
                            url = Config.WEB_APP_URL,
                            payload = payload
                        ) { ok, msg ->
                            onIsSending(false)
                            onMessage(if (ok) "OK" else "Błąd: $msg")
                            onShowBanner(ok)
                        }
                    },
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(if (isSending) "Wysyłanie..." else "Wyślij")
                }
            }

            Spacer(Modifier.height(24.dp))

            Spacer(Modifier.height(24.dp))

            if (showBanner && message == "OK") {
                SuccessBanner(
                    "Zgłoszenie zostało wysłane",
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(top = 12.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }



        }
    }
}


@Composable
fun DebossLogo(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 300.dp
) {
    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        // 1) cień (w dół/prawo)
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .offset(x = 1.dp, y = 1.dp),
            colorFilter = ColorFilter.tint(Color.Black),
            alpha = 0.05f
        )

        // 2) światło (w górę/lewo)
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .offset(x = (-1).dp, y = (-1).dp),
            colorFilter = ColorFilter.tint(Color.White),
            alpha = 0.06f
        )

        // 3) baza (prawie niewidoczna)
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            colorFilter = ColorFilter.tint(Color(0xFF9E9E9E)),
            alpha = 0.03f
        )
    }
}


@Composable
fun ZntLogoBackground(
    modifier: Modifier = Modifier
) {
    DebossLogo(
        modifier = modifier
            .fillMaxWidth(0.7f)
            .aspectRatio(1f)
            .zIndex(0f)
    )
}







@Composable
fun SuccessBanner(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = Color(0xFF2E7D32), // spokojna zieleń
                shape = RoundedCornerShape(14.dp)
            )
            .padding(vertical = 14.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
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

    var textWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    // zamiana px -> dp bez toDp()
    val underlineWidth = remember(textWidthPx, density.density) {
        (textWidthPx / density.density).dp
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp)
    ) {
        // Tekst zakładki – mierzymy szerokość
        Box(
            modifier = Modifier.height(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 22.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Light,
                color = if (selected) activeColor else inactiveColor,
                onTextLayout = { result ->
                    textWidthPx = result.size.width
                }
            )
        }

        // Podkreślenie dokładnie jak tekst
        Box(
            modifier = Modifier
                .height(8.dp)
                .width(underlineWidth),
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
            ) { Text("Wyślij") }
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
    private val mainHandler = Handler(Looper.getMainLooper())

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
                mainHandler.post {
                    onDone(false, "Błąd sieci: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val msg = it.body?.string() ?: ""
                    mainHandler.post {
                        onDone(it.isSuccessful, msg)
                    }
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

            val msg = "HTTP $code | $body"
            val ok = (code in 200..299 && body.contains("\"status\":\"OK\""))
            ok to msg

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

fun buildJsonWodomierze(
    adres: String,
    numerWodomierza: String,
    stan: String,
    user: String,
    uuid: String,
    appVersion: String,
    photoBase64: String?,
    fileName: String?
): JSONObject = JSONObject().apply {
    put("sekret", Config.SECRET_TOKEN) // jeśli nie używasz w Apps Script, może zostać
    put("typ", "Wodomierze")

    // Apps Script czyta: data.adres || data.ulica_adres
    put("adres", adres)

    // Apps Script czyta: data.nr_wodomierza
    put("nr_wodomierza", numerWodomierza)

    put("stan", stan)

    put("uzytkownik", user)
    put("urz_uuid", uuid)
    put("wersja_apki", appVersion)
    put("timestamp_client", System.currentTimeMillis().toString())

    // Apps Script wymaga: photo1 + fileName1
    if (!photoBase64.isNullOrBlank()) put("photo1", photoBase64)
    if (!fileName.isNullOrBlank()) put("fileName1", fileName)
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

fun decodeSampledBitmapFromFileRotated(path: String, maxSide: Int = 2048): Bitmap? {
    return try {
        // 1) bounds
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)

        val w0 = bounds.outWidth
        val h0 = bounds.outHeight
        if (w0 <= 0 || h0 <= 0) return null

        // 2) sample
        var inSample = 1
        val bigger = maxOf(w0, h0)
        while ((bigger / inSample) > maxSide) inSample *= 2

        val opts = BitmapFactory.Options().apply {
            inSampleSize = inSample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val bmp = BitmapFactory.decodeFile(path, opts) ?: return null

        // 3) EXIF degrees
        val exif = ExifInterface(path)
        val exifDeg = exif.rotationDegrees

        // fallback: jeśli EXIF nic nie mówi, a bitmapa jest pozioma -> obróć w pion
        val degrees = if (exifDeg == 0 && bmp.width > bmp.height) 90 else exifDeg

        Log.d("WODOMIERZ_EXIF", "exifDeg=$exifDeg usedDeg=$degrees w=${bmp.width} h=${bmp.height}")

        if (degrees == 0) bmp
        else {
            val m = Matrix().apply { postRotate(degrees.toFloat()) }
            Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
    } catch (e: Exception) {
        Log.e("WODOMIERZ_EXIF", "rotate failed: ${e.message}", e)
        null
    }
}



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
fun AddressFieldWithPlaceholder(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderText: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Words
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
                // Jeśli to jest pole tekstowe (adres) → Twoje formatowanie Każde Słowo z Dużej
                // Jeśli to jest pole liczbowe (czas) → nie ruszamy tekstu
                val finalText = if (keyboardType == KeyboardType.Number) {
                    rawText
                } else {
                    val hasTrailingSpace = rawText.endsWith(" ")
                    val core = rawText.trimEnd()

                    val normalizedCore = core
                        .split(' ')
                        .filter { it.isNotBlank() }
                        .joinToString(" ") { word ->
                            val lower = word.lowercase()
                            lower.replaceFirstChar { ch ->
                                if (ch.isLowerCase()) ch.titlecase() else ch.toString()
                            }
                        }

                    if (hasTrailingSpace) {
                        if (normalizedCore.isEmpty()) " " else normalizedCore + " "
                    } else {
                        normalizedCore
                    }
                }

                onValueChange(finalText)
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 0.dp),
            placeholder = {
                Text(
                    text = placeholderText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
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
                capitalization = capitalization,
                autoCorrect = (keyboardType != KeyboardType.Number),
                keyboardType = keyboardType
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
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Light,
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

@Composable
fun OpisFieldUnderline(
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
            onValueChange = onValueChange,
            singleLine = false,
            minLines = 1,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Opis",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 20.sp,
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
fun WodomierzeScreen(
    padding: PaddingValues,
    userName: String,
    userId: String,

    adres: String,
    onAdresChange: (String) -> Unit,

    numerWodomierza: String,
    onNumerWodomierzaChange: (String) -> Unit,

    stan: String,
    onStanChange: (String) -> Unit,

    photoPath: String?,
    onPhotoPathChange: (String?) -> Unit,

    photoBitmap: Bitmap?,
    onPhotoBitmapChange: (Bitmap?) -> Unit,

    onAfterSendClear: () -> Unit
) {

    val focusManager = LocalFocusManager.current
    val scroll = rememberScrollState()

    var showPhotoPreview by remember { mutableStateOf(false) }


    var isSending by rememberSaveable { mutableStateOf(false) }
    var sendError by rememberSaveable { mutableStateOf<String?>(null) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var showBanner by rememberSaveable { mutableStateOf(false) }


    val vm = remember { SendVm() }


    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoPath != null) {
            onPhotoBitmapChange(decodeSampledBitmapFromFileRotated(photoPath, maxSide = 2048))
        }
    }


    // Aparat: prosimy o uprawnienie i dopiero wtedy odpalamy TakePicture
    val context = LocalContext.current
    lateinit var takePhoto: () -> Unit


    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            takePhoto()
        } else {
            Toast.makeText(context, "Brak zgody na aparat", Toast.LENGTH_SHORT).show()
        }
    }


    // Robi plik w cache + odpala aparat (bez zapisu do galerii)
    takePhoto = {
        try {
            val file = createImageFile(context, "photo_wodomierz_")
            onPhotoPathChange(file.absolutePath)


            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Błąd aparatu: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    val canSave =
        userName.isNotBlank() &&
                adres.trim().length >= 3 &&
                numerWodomierza.trim().isNotBlank() &&
                stan.trim().isNotBlank() &&
                photoBitmap != null

    LaunchedEffect(showBanner, message) {
        if (showBanner && message == "OK") {
            delay(5000)
            showBanner = false
        }
    }



    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
    )

    {

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
            ScreenTitleWithUnderline(title = "WODOMIERZE")
            Spacer(Modifier.height(16.dp))

            AddressField(
                value = adres,
                onValueChange = onAdresChange,
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            // ... i cała reszta Twojego UI dalej (bez zmian)



            AddressFieldWithPlaceholder(
                value = numerWodomierza,
                onValueChange = onNumerWodomierzaChange,
                placeholderText = "Numer wodomierza",
                modifier = Modifier.fillMaxWidth(0.8f),
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.None
            )


            AddressFieldWithPlaceholder(
                value = stan,
                onValueChange = { new ->
                    val filtered = new
                        .replace('.', ',')
                        .filter { it.isDigit() || it == ',' }

                    // tylko jeden przecinek
                    if (filtered.count { it == ',' } <= 1) {
                        onStanChange(filtered)
                    }
                },
                placeholderText = "Stan",
                modifier = Modifier.fillMaxWidth(0.8f),
                keyboardType = KeyboardType.Number,
                capitalization = KeyboardCapitalization.None
            )



            // ====== ZDJĘCIE (1 SLOT) ======
            Spacer(Modifier.height(8.dp))

            if (photoBitmap == null) {
                Button(
                    onClick = {
                        val hasCameraPermission =
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                        if (hasCameraPermission) {
                            takePhoto()
                        } else {
                            cameraPermLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        "Zrób zdjęcie wodomierza",
                        textAlign = TextAlign.Center
                    )
                }



        } else {
                Image(
                    bitmap = photoBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(180.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { showPhotoPreview = true },
                    contentScale = ContentScale.Crop
                )


                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { takePhoto() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Zmień") }

                    OutlinedButton(
                        onClick = {
                            onPhotoBitmapChange(null)
                            photoPath?.let { runCatching { File(it).delete() } }
                            onPhotoPathChange(null)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Usuń") }
                }
            }

            if (showPhotoPreview && photoBitmap != null) {
                FullscreenImageDialog(
                    bitmap = photoBitmap!!,
                    onClose = { showPhotoPreview = false }
                )
            }


            Button(
                enabled = canSave && !isSending,
                onClick = {
                    focusManager.clearFocus()   // ⬅️ TO DODAJESZ
                    sendError = null
                    isSending = true

                    sendError = null
                    isSending = true

                    val photoB64 = photoPath?.let { path ->
                        fileToBase64Original(File(path))
                    }

                    val fileName = "wodomierz_${System.currentTimeMillis()}.jpg"

                    val payload = buildJsonWodomierze(
                        adres = adres.trim(),
                        numerWodomierza = numerWodomierza.trim(),
                        stan = stan.trim(),
                        user = userName,
                        uuid = userId,
                        appVersion = Config.APP_VERSION,
                        photoBase64 = photoB64,
                        fileName = fileName
                    )

                    vm.send(
                        url = Config.WEB_APP_URL,
                        payload = payload
                    ) { ok, msg ->
                        isSending = false


                        // msg to odpowiedź z Apps Script
                        sendError = "RESP: $msg"

                        if (ok && msg.contains("\"status\":\"OK\"")) {
                            onAfterSendClear()
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





        }
        if (showBanner && message == "OK") {
            SuccessBanner(
                "Wodomierz został wysłany",
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .padding(bottom = 24.dp)
            )
        }


    }
}

@Composable
fun UnderlineInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    hidePlaceholderWhenFocused: Boolean = false
) {
    val lineColor = if (value.trim().isNotEmpty()) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(modifier = modifier) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            modifier = Modifier.fillMaxWidth(),
            interactionSource = interactionSource,
            placeholder = {
                val hide = hidePlaceholderWhenFocused && isFocused
                if (!hide && value.isBlank()) {
                    Text(text = placeholder)
                }
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
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
fun ScreenTitleWithUnderline(
    title: String,
    modifier: Modifier = Modifier,
    topPadding: androidx.compose.ui.unit.Dp = 24.dp,
    bottomPadding: androidx.compose.ui.unit.Dp = 12.dp
) {
    var titleWidthDp by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            onTextLayout = { result ->
                titleWidthDp = with(density) { result.size.width.toDp() }
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
}

@Composable
fun FullscreenImageDialog(
    bitmap: Bitmap,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // obraz na cały ekran
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // X w prawym górnym rogu
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Zamknij",
                    tint = Color.White
                )
            }
        }
    }
}



