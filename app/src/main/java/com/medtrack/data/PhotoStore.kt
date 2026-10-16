package com.medtrack.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.roundToInt

/** Copies chosen photos into app-private storage, resized, so the original can be deleted safely. */
class PhotoStore(private val context: Context) {
    val photosDir: File get() = File(context.filesDir, "photos").apply { mkdirs() }

    suspend fun importFrom(uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Not an image" }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, MAX_SIDE) }
        val decoded = resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Could not decode image")
        val rotation = resolver.openInputStream(uri).use { stream -> stream?.let { ExifInterface(it).rotationDegrees } ?: 0 }

        val (w, h) = scaledSize(decoded.width, decoded.height, MAX_SIDE)
        var bitmap = if (w != decoded.width || h != decoded.height) Bitmap.createScaledBitmap(decoded, w, h, true) else decoded
        if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        val file = File(photosDir, "${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        file.absolutePath
    }

    /**
     * Saves the part of the photo at [path] between the given fractions (0..1) of its width and height as a new
     * photo, and returns its path. The original is left for the caller to delete.
     */
    suspend fun crop(path: String, left: Float, top: Float, right: Float, bottom: Float): String = withContext(Dispatchers.IO) {
        val source = BitmapFactory.decodeFile(path) ?: error("Could not read the photo")
        val x = (left * source.width).toInt().coerceIn(0, source.width - 1)
        val y = (top * source.height).toInt().coerceIn(0, source.height - 1)
        val w = ((right - left) * source.width).toInt().coerceIn(1, source.width - x)
        val h = ((bottom - top) * source.height).toInt().coerceIn(1, source.height - y)
        val cropped = Bitmap.createBitmap(source, x, y, w, h)
        val file = File(photosDir, "${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { cropped.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        file.absolutePath
    }

    fun delete(path: String?) {
        val file = path?.let(::File) ?: return
        if (file.parentFile?.absolutePath == photosDir.absolutePath) file.delete()
    }

    companion object {
        const val MAX_SIDE = 1200

        /** Largest power-of-two subsampling that keeps the longest side at or above [maxSide]. */
        fun sampleSizeFor(width: Int, height: Int, maxSide: Int): Int {
            var sample = 1
            while (maxOf(width, height) / (sample * 2) >= maxSide) sample *= 2
            return sample
        }

        fun scaledSize(width: Int, height: Int, maxSide: Int): Pair<Int, Int> {
            val longest = maxOf(width, height)
            if (longest <= maxSide) return width to height
            val factor = maxSide.toDouble() / longest
            return (width * factor).roundToInt().coerceAtLeast(1) to (height * factor).roundToInt().coerceAtLeast(1)
        }

        fun decodeSampled(path: String, maxSide: Int): Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0) return null
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSide) }
            return BitmapFactory.decodeFile(path, options)
        }
    }
}
