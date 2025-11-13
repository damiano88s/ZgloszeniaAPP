package com.example.zgloszeniaapp



data class UploadPayload(
    val ulica_adres: String,
    val data: String?,       // np. LocalDate.now().toString()
    val photo1: String?,     // Base64
    val photo2: String?,     // Base64
    val photo3: String?      // Base64
)

