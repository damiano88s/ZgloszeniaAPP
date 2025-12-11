package com.example.zgloszeniaapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,          // Twój fiolet
    secondary = PurpleGrey40,
    tertiary = Pink40,

    background = Color(0xFFF2F2F2),       // jasny popiel – tło apki
    surface = Color(0xFFFAFAFA),          // bardzo jasna powierzchnia

    onBackground = Color(0xFF333333),     // główny tekst
    onSurface = Color(0xFF333333),
    onSurfaceVariant = Color(0xFF666666), // opisy / nieaktywne
    outline = Color(0xFFBBBBBB)          // delikatne obramowania / kreski
)

@Composable
fun ZgloszeniaAPPTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,   // <<< wyłączamy dynamiczne kolory
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
