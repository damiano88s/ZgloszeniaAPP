package com.example.zgloszeniaapp



data class UploadPayload(
    val ulica_adres: String,
    val data: String?,      // np. LocalDate.now().toString()
    val fileName: String?,
    val photo: String       // base64 bez 'data:image/...;base64,'
)
