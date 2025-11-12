package com.example.zgloszeniaapp

object Config {
    // Nazwy typów (u Ciebie używane w RadioButton)
    const val SHEET_GABARYTY = "Gabaryty"
    const val SHEET_ZLECENIA = "Zlecenia"

    // URL do Web App (koniecznie /exec)
    const val WEB_APP_URL =
        "https://script.google.com/macros/s/AKfycbxJvgYeXqRXc0vw7e5ve8OSveIrdOgzVbRsXide-eaMPV7OHNte50EFfIdLC9eg1AB-/exec"
    // Token – zostaw taki, jak miałeś; jeśli nie używasz, może być pusty.
    const val SECRET_TOKEN = ""  // <- wstaw swój, jeżeli sprawdzasz go w Apps Script

    // Wersja aplikacji – możesz wpisać ręcznie,
    // albo pobrać z BuildConfig (jeśli masz włączone buildConfig = true).
    const val APP_VERSION = "1.0.0"
    // Alternatywa (jeśli chcesz z BuildConfig):
    // val APP_VERSION: String get() = BuildConfig.VERSION_NAME
}
