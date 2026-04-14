package com.example.socialconnect

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Base64
import android.widget.ImageView
import java.io.ByteArrayOutputStream
import androidx.core.graphics.scale

object ImageUtils {

    // Convert Uri to base64 string
    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val compressed = compressBitmap(bitmap)
            Base64.encodeToString(compressed, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    // Compress bitmap before converting
    private fun compressBitmap(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        // resize if too large
        val maxSize = 512
        val ratio = minOf(
            maxSize.toFloat() / bitmap.width,
            maxSize.toFloat() / bitmap.height
        )
        val resized = if (ratio < 1) {
            bitmap.scale((bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt())
        } else bitmap

        resized.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return outputStream.toByteArray()
    }

    // Load base64 string into any ImageView
    fun loadBase64(base64: String?, imageView: ImageView, placeholder: Drawable? = null) {
        if (base64.isNullOrEmpty()) {
            placeholder?.let { imageView.setImageDrawable(it) }
            return
        }
        try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            placeholder?.let { imageView.setImageDrawable(it) }
        }
    }
}