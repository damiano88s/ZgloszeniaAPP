package com.example.zgloszeniaapp

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.io.File

class PhotoViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val path = intent.getStringExtra("photo_path")
        if (path.isNullOrEmpty()) { finish(); return }

        // Wczytujemy bitmapę w ROZMIAR EKRANU (bez kompresji pliku, tylko podgląd)
        val maxSide = screenMaxSide(this)

        val preview = decodeSampledBitmapFromFile(path, maxSide) ?: run {
            finish(); return
        }
        val rotated = rotateBitmapIfRequired(preview, File(path))

        setContent {
            // Pełny ekran, tap = zamknij (nic nie kasujemy)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            )
            {
                Image(
                    bitmap = rotated.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
