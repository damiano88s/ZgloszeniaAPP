package com.example.zgloszeniaapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import android.util.Base64
import android.util.Log


/*fun createTempImageUri(context: Context, fileName: String): Uri {
    val dir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(dir, fileName)
    if (file.exists()) file.delete()
    file.createNewFile()
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}
*/
fun loadBitmap(context: Context, uri: Uri): Bitmap =
    context.contentResolver.openInputStream(uri).use { input ->
        BitmapFactory.decodeStream(input!!)
    }

fun uriToBase64Jpeg(
    context: Context,
    uri: Uri,
    maxDim: Int = 4000,     // było 1600
    quality: Int = 90      // było 85
): Pair<String, Bitmap> {
    val src = loadBitmap(context, uri)

    val w = src.width
    val h = src.height
    val scale = min(1f, maxDim.toFloat() / max(w, h).toFloat())
    val bm = if (scale < 1f)
        Bitmap.createScaledBitmap(src, (w * scale).toInt(), (h * scale).toInt(), true)
    else src

    val bos = ByteArrayOutputStream()
    bm.compress(Bitmap.CompressFormat.JPEG, quality, bos)
    val bytes = bos.toByteArray()
    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

    Log.d("PHOTO", "bm=${bm.width}x${bm.height}, bytes=${bytes.size}, b64_len=${b64.length}")

    return b64 to bm
}
