package com.example.zgloszeniaapp

object Config {
    // Nazwy typów (u Ciebie używane w RadioButton)
    const val SHEET_GABARYTY = "Gabaryty"
    const val SHEET_ZLECENIA = "Zlecenia"

    // URL do Web App (koniecznie /exec)
    const val WEB_APP_URL =
        "https://script.google.com/macros/s/AKfycbwWpUChZYzIhUxlu4Q6gASKKCLZXtav_wkYX1NQoJ-ftxledjGOEW2P055kjbMGByHo/exec"
    // Token – zostaw taki, jak miałeś; jeśli nie używasz, może być pusty.
    const val SECRET_TOKEN = ""  // <- wstaw swój, jeżeli sprawdzasz go w Apps Script

    // Wersja aplikacji – możesz wpisać ręcznie,
    // albo pobrać z BuildConfig (jeśli masz włączone buildConfig = true).
    const val APP_VERSION = "1.0.0"
    // Alternatywa (jeśli chcesz z BuildConfig):
    // val APP_VERSION: String get() = BuildConfig.VERSION_NAME
}
