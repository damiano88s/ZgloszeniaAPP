package com.example.zgloszeniaapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpOffset

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp



@Composable
fun GrafikScreen(padding: PaddingValues) {

    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var allRows by remember { mutableStateOf<List<GrafikRow>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var selectedName by remember { mutableStateOf<String?>(null) }
    val ready by GrafikCache.ready.collectAsState()


    LaunchedEffect(Unit) {
        error = null
        try {
            val file = GrafikDownload.localFile(context)

            val rows = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                file.inputStream().use { GrafikExcelReader.read(it) }
            }

            allRows = rows
        } catch (e: Exception) {
            error = "Nie udało się wczytać lokalnego grafiku"
        }
    }



    val names = remember(allRows) {
        allRows.map { it.imie }.distinct().sorted()
    }

    val daysOrder = listOf(
        "Poniedziałek", "Wtorek", "Środa", "Czwartek",
        "Piątek", "Sobota", "Niedziela"
    )

    val weeklyForSelected = remember(selectedName, allRows) {
        val name = selectedName ?: return@remember emptyMap()
        allRows.filter { it.imie == name }.groupBy { it.dzien }
    }

    val filteredResults = remember(searchQuery, allRows) {
        val q = normalize(searchQuery.trim())
        if (q.isBlank()) emptyList()
        else allRows.filter { normalize(it.ulica).contains(q) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {

        // TŁO
        ZntLogoBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            ScreenTitleWithUnderline(title = "GRAFIK")
            Spacer(Modifier.height(12.dp))

            // ===== WYBÓR DOZORCY =====
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                DozorcaPillPicker(
                    names = names,
                    selectedName = selectedName,
                    onSelect = { selectedName = it },

                )
            }

            Spacer(Modifier.height(12.dp))

            // ===== WYSZUKIWARKA ULIC =====
            AddressField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            Spacer(Modifier.height(12.dp))

            // ===== CZĘŚĆ PRZEWIJANA =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    loading -> CircularProgressIndicator()

                    error != null -> Text(text = error!!, color = Color.Red)

                    else -> {
                        val scroll = rememberScrollState()

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scroll),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            // --- grafik tygodniowy
                            if (selectedName != null) {
                                daysOrder.forEach { day ->
                                    val rowsForDay = weeklyForSelected[day].orEmpty()
                                    if (rowsForDay.isNotEmpty()) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth(0.95f)
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(text = day, fontSize = 16.sp)
                                                Spacer(Modifier.height(6.dp))
                                                rowsForDay.forEach { r ->
                                                    Text(text = "• ${r.ulica}")
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }

                            // --- wyniki wyszukiwania ulic
                            if (searchQuery.isNotBlank()) {
                                if (filteredResults.isEmpty()) {
                                    Text("Brak wyników")
                                } else {
                                    filteredResults.forEach { row ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth(0.95f)
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "${row.imie} – ${row.ulica}",
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = row.dzien,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Normalizacja: bez polskich znaków i bez znaczenia wielkości liter
fun normalize(text: String): String {
    return text.lowercase()
        .replace("ą", "a")
        .replace("ć", "c")
        .replace("ę", "e")
        .replace("ł", "l")
        .replace("ń", "n")
        .replace("ó", "o")
        .replace("ś", "s")
        .replace("ż", "z")
        .replace("ź", "z")
}

// ===== PILL PICKER =====
@Composable
fun DozorcaPillPicker(
    names: List<String>,
    selectedName: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val pillWidth = 240.dp

    // ✅ Automatyczna szerokość menu na podstawie najdłuższego imienia
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val longestName = remember(names) {
        names.maxByOrNull { it.length } ?: ""
    }

    // ✅ Prosty styl do mierzenia (bez MaterialTheme, żeby nie było błędu composable)
    val measureStyle = remember { TextStyle(fontSize = 16.sp) }

    val menuWidth = remember(longestName) {
        with(density) {
            textMeasurer
                .measure(
                    text = AnnotatedString(longestName),
                    style = measureStyle
                )
                .size
                .width
                .toDp() + 32.dp // padding po bokach
        }
    }

    // ✅ centrowanie menu pod pigułką
    val xOffset = (pillWidth - menuWidth) / 2

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // kotwica pod menu
        Box(
            modifier = Modifier.width(pillWidth),
            contentAlignment = Alignment.TopCenter
        ) {
            // ====== PIGUŁKA ======
            Surface(
                shape = RoundedCornerShape(999.dp),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .width(pillWidth)
                    .clickable { expanded = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dozorca:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.width(10.dp))

                    Text(
                        text = selectedName ?: "Wybierz",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )

                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ====== MENU ======
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset(x = xOffset, y = 0.dp),
                modifier = Modifier
                    .width(menuWidth)
                    .heightIn(max = 320.dp)
            ) {
                names.forEach { name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            onSelect(name)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
