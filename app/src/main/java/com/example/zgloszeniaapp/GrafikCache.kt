package com.example.zgloszeniaapp

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

object GrafikCache {

    private val _rows = MutableStateFlow<List<GrafikRow>>(emptyList())
    val rows: StateFlow<List<GrafikRow>> = _rows.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    suspend fun preload(context: Context) = withContext(Dispatchers.IO) {
        try {
            // 1) pobierz/odśwież plik lokalny
            val file = GrafikDownload.ensureLocalFile(context, GrafikConfig.GRAFIK_URL)

            // 2) przeczytaj Excel do listy (TO JEST KLUCZ – żeby nie lagowało przy kliknięciu)
            val loadedRows = file.inputStream().use { GrafikExcelReader.read(it) }

            // 3) wrzuć do cache
            _rows.value = loadedRows
            _ready.value = true
        } catch (e: Exception) {
            // jak się nie uda – zostaw co było
            _ready.value = false
        }
    }
}

