package app.uamo.ynotes.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

object MediaManager {

    private const val MEDIA_DIR = "media"
    private const val MEDIA_ENCRYPTED_DIR = "media_encrypted"

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // 1/8th of available JVM RAM in KB

    private val bitmapCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    /**
     * Clear all cached thumbnails from memory (e.g. when locking Safe Zone).
     */
    fun clearCache() {
        bitmapCache.evictAll()
    }

    /**
     * Clean any orphaned temporary files in cacheDir left over from unexpected OS process termination.
     */
    fun cleanOrphanedTempFiles(context: Context) {
        try {
            context.cacheDir.listFiles { file ->
                file.name.startsWith("temp_compress_") || file.name.startsWith("temp_decrypted_")
            }?.forEach { file ->
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getMediaDir(context: Context, noteId: String, isSecret: Boolean): File {
        val baseName = if (isSecret) MEDIA_ENCRYPTED_DIR else MEDIA_DIR
        val dir = File(context.filesDir, "$baseName/$noteId")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Save a media file from a content URI to the note's media directory.
     * If isSecret, encrypts the file on write using streaming AES-GCM.
     * Returns the generated filename.
     */
    fun saveMedia(context: Context, noteId: String, uri: Uri, isSecret: Boolean): String? {
        return try {
            val fileName = "img_${UUID.randomUUID().toString().take(8)}.jpg"
            val dir = getMediaDir(context, noteId, isSecret)
            val targetFile = File(dir, if (isSecret) fileName.replace(".jpg", ".enc") else fileName)

            val rawBitmap = decodeSampledBitmapFromUri(context, uri, 1920) ?: return null
            val bitmap = if (rawBitmap.config == Bitmap.Config.HARDWARE) {
                rawBitmap.copy(Bitmap.Config.ARGB_8888, false) ?: rawBitmap
            } else {
                rawBitmap
            }

            if (isSecret) {
                val tempFile = File(context.cacheDir, "temp_compress_${UUID.randomUUID()}.jpg")
                try {
                    FileOutputStream(tempFile).use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                    }
                    FileInputStream(tempFile).use { input ->
                        FileOutputStream(targetFile).use { output ->
                            CryptoManager.encryptFile(input, output)
                        }
                    }
                } finally {
                    if (tempFile.exists()) tempFile.delete()
                }
            } else {
                FileOutputStream(targetFile).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
                }
            }
            if (rawBitmap != bitmap) rawBitmap.recycle()
            bitmap.recycle()

            fileName
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Load a bitmap from a note's media file.
     * If isSecret, decrypts the file first.
     * Uses inSampleSize for memory-efficient thumbnail loading.
     */
    fun loadMediaBitmap(
        context: Context,
        noteId: String,
        fileName: String,
        isSecret: Boolean,
        maxSize: Int = 512
    ): Bitmap? {
        val cacheKey = "${noteId}_${fileName}_$maxSize"
        bitmapCache.get(cacheKey)?.let { return it }

        val bitmap = try {
            if (isSecret) {
                val encFile = File(getMediaDir(context, noteId, true), fileName.replace(".jpg", ".enc"))
                if (!encFile.exists()) return null
                
                val byteOut = java.io.ByteArrayOutputStream()
                java.io.FileInputStream(encFile).use { input ->
                    CryptoManager.decryptFile(input, byteOut)
                }
                val decryptedBytes = byteOut.toByteArray()
                decodeSampledBitmapFromByteArray(decryptedBytes, maxSize)
            } else {
                val file = File(getMediaDir(context, noteId, false), fileName)
                if (!file.exists()) return null
                decodeSampledBitmapFromFile(file.absolutePath, maxSize)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }

        if (bitmap != null) {
            bitmapCache.put(cacheKey, bitmap)
        }
        return bitmap
    }

    /**
     * Delete all media files for a note.
     * Evicts only the cache entries that belong to the deleted note.
     */
    fun deleteNoteMedia(context: Context, noteId: String, isSecret: Boolean) {
        try {
            val dir = File(context.filesDir, "${if (isSecret) MEDIA_ENCRYPTED_DIR else MEDIA_DIR}/$noteId")
            // Evict all cached bitmaps for this specific note before deleting files
            val keysToEvict = bitmapCache.snapshot().keys.filter { it.startsWith("${noteId}_") }
            keysToEvict.forEach { bitmapCache.remove(it) }
            if (dir.exists()) dir.deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Delete a single media file.
     * Evicts only the specific cache entries for the deleted file, leaving
     * all other note thumbnails intact in memory.
     */
    fun deleteMediaFile(context: Context, noteId: String, fileName: String, isSecret: Boolean) {
        try {
            val dir = getMediaDir(context, noteId, isSecret)
            val file = if (isSecret) {
                File(dir, fileName.replace(".jpg", ".enc"))
            } else {
                File(dir, fileName)
            }
            if (file.exists()) file.delete()
            // Remove only the cache entries for this specific file across all maxSize variants
            val prefix = "${noteId}_${fileName}_"
            val keysToEvict = bitmapCache.snapshot().keys.filter { it.startsWith(prefix) }
            keysToEvict.forEach { bitmapCache.remove(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun decodeSampledBitmapFromByteArray(bytes: ByteArray, maxSize: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize)
        options.inJustDecodeBounds = false
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        return if (bmp.config == Bitmap.Config.HARDWARE) {
            bmp.copy(Bitmap.Config.ARGB_8888, false) ?: bmp
        } else {
            bmp
        }
    }

    private fun decodeSampledBitmapFromFile(path: String, maxSize: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize)
        options.inJustDecodeBounds = false
        val bmp = BitmapFactory.decodeFile(path, options) ?: return null
        return if (bmp.config == Bitmap.Config.HARDWARE) {
            bmp.copy(Bitmap.Config.ARGB_8888, false) ?: bmp
        } else {
            bmp
        }
    }

    private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, maxSize: Int): Bitmap? {
        return try {
            var bmp: Bitmap? = null
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                try {
                    val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                    bmp = android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        val width = info.size.width
                        val height = info.size.height
                        var sampleSize = 1
                        if (height > maxSize || width > maxSize) {
                            val halfHeight = height / 2
                            val halfWidth = width / 2
                            while (halfHeight / sampleSize >= maxSize && halfWidth / sampleSize >= maxSize) {
                                sampleSize *= 2
                            }
                        }
                        decoder.setTargetSampleSize(sampleSize)
                        decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
            if (bmp == null) {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, options)
                }
                options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize)
                options.inJustDecodeBounds = false
                context.contentResolver.openInputStream(uri)?.use { input ->
                    bmp = BitmapFactory.decodeStream(input, null, options)
                }
            }
            bmp
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
