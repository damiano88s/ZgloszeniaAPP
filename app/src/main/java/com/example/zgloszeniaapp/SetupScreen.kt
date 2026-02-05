package com.example.zgloszeniaapp

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp


@Composable
fun SetupScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("zgłoszenia_prefs", Context.MODE_PRIVATE)

    var username by remember { mutableStateOf("") }
    var selectedUnity by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Konfiguracja aplikacji",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Nazwa użytkownika") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Text("Wybierz UNITY:")
        Spacer(Modifier.height(8.dp))

        UnityOption("ADM ZNT", "ZNT", selectedUnity) { selectedUnity = it }
        UnityOption("ADM Zabrze", "ZABRZE", selectedUnity) { selectedUnity = it }
        UnityOption("ADM Miasteczko", "MIAS", selectedUnity) { selectedUnity = it }
        UnityOption("Dział techniczny", "EKSP", selectedUnity) { selectedUnity = it }

        Spacer(Modifier.height(32.dp))

        val isNameOk = username.trim().length >= 3
        val canFinish = selectedUnity != null && isNameOk

        Button(
            onClick = {
                UserPrefs.setName(context, username.trim())
                UserPrefs.setUnity(context, selectedUnity!!)
                UserPrefs.setSetupDone(context, true)
                onFinished()
            },
            enabled = canFinish,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zapisz i przejdź dalej")
        }
    }
}

@Composable
private fun UnityOption(
    label: String,
    value: String,
    selected: String?,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(value) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected == value,
            onClick = { onSelect(value) }
        )
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

