package com.sniperoftdm.booktracker.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Manages local book cover storage, downscaling, deletion, and cleanup in app-private storage.
 */
class CoverStorage(private val filesDir: File) {

    constructor(context: Context) : this(context.filesDir)

    val coversDir: File = File(filesDir, "covers").apply { mkdirs() }

    /** Save image from input stream for a book id, downscaling to at most 600px max dimension. */
    fun save(inputStream: InputStream, bookId: Long): String? {
        val bytes = try {
            inputStream.readBytes()
        } catch (e: Exception) {
            return null
        }
        if (bytes.isEmpty()) return null

        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
        val origWidth = boundsOptions.outWidth
        val origHeight = boundsOptions.outHeight
        if (origWidth <= 0 || origHeight <= 0) {
            return null
        }

        val maxSide = maxOf(origWidth, origHeight)
        var sampleSize = 1
        while (maxSide / (sampleSize * 2) >= 600) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null

        val currentMax = maxOf(decodedBitmap.width, decodedBitmap.height)
        val finalBitmap = if (currentMax > 600) {
            val scale = 600f / currentMax.toFloat()
            val targetWidth = (decodedBitmap.width * scale).toInt().coerceAtLeast(1)
            val targetHeight = (decodedBitmap.height * scale).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(decodedBitmap, targetWidth, targetHeight, true)
            if (scaled != decodedBitmap) {
                decodedBitmap.recycle()
            }
            scaled
        } else {
            decodedBitmap
        }

        val targetFile = File(coversDir, "$bookId.jpg")
        return try {
            FileOutputStream(targetFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            "covers/$bookId.jpg"
        } catch (e: Exception) {
            null
        } finally {
            finalBitmap.recycle()
        }
    }

    /** Save image from a local file for a book id, returning relative path. */
    fun saveFromFile(sourceFile: File, bookId: Long): String? {
        if (!sourceFile.exists() || !sourceFile.isFile) return null
        return try {
            FileInputStream(sourceFile).use { save(it, bookId) }
        } catch (e: Exception) {
            null
        }
    }

    /** Save image from an Android content URI for a book id. */
    fun saveFromUri(context: Context, uri: Uri, bookId: Long): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { save(it, bookId) }
        } catch (e: Exception) {
            null
        }
    }

    /** Resolve relative cover path to an existing local file or null. */
    fun getFile(coverPath: String?): File? {
        if (coverPath.isNullOrBlank()) return null
        val file = File(filesDir, coverPath)
        return if (file.exists() && file.isFile) file else null
    }

    /** Delete local cover image associated with book id. */
    fun delete(bookId: Long): Boolean {
        val file = File(coversDir, "$bookId.jpg")
        return if (file.exists()) file.delete() else true
    }

    /** Clear all stored cover images in the covers directory. */
    fun clearAll() {
        if (coversDir.exists()) {
            coversDir.listFiles()?.forEach { it.delete() }
        }
    }
}
