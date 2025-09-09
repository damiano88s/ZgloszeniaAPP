package com.example.zgloszeniaapp



import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

fun bitmapToBase64Jpeg(bitmap: Bitmap, quality: Int = 85): String {
    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
    val bytes = baos.toByteArray()
    // NO_WRAP = bez łamań linii; bez prefiksu data:image/...
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}
