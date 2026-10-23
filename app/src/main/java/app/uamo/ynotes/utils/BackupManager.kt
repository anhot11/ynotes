package app.uamo.ynotes.utils

import android.content.Context
import android.net.Uri
import app.uamo.ynotes.data.BookEntity
import app.uamo.ynotes.data.HiddenAppEntity
import app.uamo.ynotes.data.NoteDatabase
import app.uamo.ynotes.data.NoteEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupManager {

    private const val MAGIC_HEADER = "YNOTE_BACKUP_V1"
    private const val PBKDF2_ITERATIONS = 65536
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    data class BackupManifest(
        val version: Int = 1,
        val timestamp: Long = System.currentTimeMillis(),
        val noteCount: Int,
        val notes: List<NoteEntity>,
        val books: List<BookEntity>,
        val hiddenApps: List<HiddenAppEntity>
    )

    sealed class BackupResult {
        data class Success(val notesCount: Int, val booksCount: Int) : BackupResult()
        data class Error(val message: String) : BackupResult()
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Export all notes, books, hidden apps, and media files into a zero-knowledge
     * AES-256-GCM encrypted backup stream written to destinationUri.
     */
    suspend fun exportBackup(
        context: Context,
        destinationUri: Uri,
        password: String
    ): BackupResult = withContext(Dispatchers.IO) {
        if (password.length < 4) {
            return@withContext BackupResult.Error("La contraseña debe tener al menos 4 caracteres.")
        }

        val tempZipFile = File(context.cacheDir, "temp_backup_${System.currentTimeMillis()}.zip")
        try {
            val db = NoteDatabase.getDatabase(context)
            val allNotes = db.noteDao().getAllNotesDirect()
            val allBooks = db.noteDao().getAllBooksDirect()
            val allHiddenApps = db.hiddenAppDao().getAllHiddenAppsSync()

            // Prepare notes for portable backup:
            // For secret notes, decrypt title and body in RAM so they can be re-encrypted
            // with the destination device's KeyStore upon restore.
            val portableNotes = allNotes.map { note ->
                if (note.isSecret) {
                    note.copy(
                        title = CryptoManager.decrypt(note.title),
                        body = CryptoManager.decrypt(note.body)
                    )
                } else {
                    note
                }
            }

            val manifest = BackupManifest(
                version = 1,
                timestamp = System.currentTimeMillis(),
                noteCount = portableNotes.size,
                notes = portableNotes,
                books = allBooks,
                hiddenApps = allHiddenApps
            )

            // Step 1: Write all data into a temporary ZIP archive
            ZipOutputStream(BufferedOutputStream(FileOutputStream(tempZipFile))).use { zos ->
                // Write manifest.json
                zos.putNextEntry(ZipEntry("manifest.json"))
                val manifestJson = Gson().toJson(manifest)
                zos.write(manifestJson.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // Pack public media files: context.filesDir/media/
                val publicMediaDir = File(context.filesDir, "media")
                if (publicMediaDir.exists() && publicMediaDir.isDirectory) {
                    publicMediaDir.walkTopDown().filter { it.isFile }.forEach { file ->
                        val relativePath = "media/" + file.relativeTo(publicMediaDir).path
                        zos.putNextEntry(ZipEntry(relativePath))
                        file.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }

                // Pack secret media files: context.filesDir/media_encrypted/
                // Decrypt into the zip archive in RAM so they are encrypted under the backup key
                val secretMediaDir = File(context.filesDir, "media_encrypted")
                if (secretMediaDir.exists() && secretMediaDir.isDirectory) {
                    secretMediaDir.walkTopDown().filter { it.isFile }.forEach { file ->
                        val relativePath = "media_secret/" + file.relativeTo(secretMediaDir).path
                        zos.putNextEntry(ZipEntry(relativePath))
                        try {
                            val tempDecrypted = File(context.cacheDir, "temp_dec_${file.name}")
                            file.inputStream().use { input ->
                                tempDecrypted.outputStream().use { output ->
                                    CryptoManager.decryptFile(input, output)
                                }
                            }
                            tempDecrypted.inputStream().use { it.copyTo(zos) }
                            tempDecrypted.delete()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        zos.closeEntry()
                    }
                }
            }

            // Step 2: Encrypt the entire ZIP archive with PBKDF2 + AES-256-GCM
            val random = SecureRandom()
            val salt = ByteArray(SALT_LENGTH_BYTES).also { random.nextBytes(it) }
            val iv = ByteArray(IV_LENGTH_BYTES).also { random.nextBytes(it) }

            val secretKey = deriveKey(password.toCharArray(), salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

            val outputStream = context.contentResolver.openOutputStream(destinationUri, "wt")
                ?: return@withContext BackupResult.Error("No se pudo abrir el archivo de destino para escritura.")

            outputStream.use { os ->
                // Write Magic Header
                os.write(MAGIC_HEADER.toByteArray(Charsets.UTF_8))
                // Write Salt
                os.write(salt)
                // Write IV
                os.write(iv)

                // Write encrypted stream in chunks
                val buffer = ByteArray(8192)
                FileInputStream(tempZipFile).use { fis ->
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        val encryptedChunk = cipher.update(buffer, 0, bytesRead)
                        if (encryptedChunk != null && encryptedChunk.isNotEmpty()) {
                            os.write(encryptedChunk)
                        }
                    }
                    val finalBytes = cipher.doFinal()
                    if (finalBytes != null && finalBytes.isNotEmpty()) {
                        os.write(finalBytes)
                    }
                }
                os.flush()
            }

            BackupResult.Success(manifest.noteCount, manifest.books.size)
        } catch (e: Throwable) {
            e.printStackTrace()
            BackupResult.Error("Error al exportar: ${e.localizedMessage ?: e.message}")
        } finally {
            if (tempZipFile.exists()) tempZipFile.delete()
        }
    }

    /**
     * Restore notes, books, hidden apps, and media files from an encrypted backup URI.
     */
    suspend fun restoreBackup(
        context: Context,
        sourceUri: Uri,
        password: String
    ): BackupResult = withContext(Dispatchers.IO) {
        val tempZipFile = File(context.cacheDir, "temp_restore_${System.currentTimeMillis()}.zip")
        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext BackupResult.Error("No se pudo abrir el archivo de respaldo seleccionado.")

            // Read and verify header
            val headerBytes = ByteArray(MAGIC_HEADER.length)
            val bytesReadHeader = inputStream.read(headerBytes)
            if (bytesReadHeader != MAGIC_HEADER.length || String(headerBytes, Charsets.UTF_8) != MAGIC_HEADER) {
                inputStream.close()
                return@withContext BackupResult.Error("Formato no válido. El archivo no es una copia de seguridad válida de yNotes.")
            }

            // Read Salt
            val salt = ByteArray(SALT_LENGTH_BYTES)
            val saltRead = inputStream.read(salt)
            if (saltRead != SALT_LENGTH_BYTES) {
                inputStream.close()
                return@withContext BackupResult.Error("Archivo de respaldo dañado (error en cabecera de sal).")
            }

            // Read IV
            val iv = ByteArray(IV_LENGTH_BYTES)
            val ivRead = inputStream.read(iv)
            if (ivRead != IV_LENGTH_BYTES) {
                inputStream.close()
                return@withContext BackupResult.Error("Archivo de respaldo dañado (error en vector de inicialización).")
            }

            // Decrypt stream using AES-256-GCM
            val secretKey = deriveKey(password.toCharArray(), salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

            try {
                FileOutputStream(tempZipFile).use { fos ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        val decryptedChunk = cipher.update(buffer, 0, read)
                        if (decryptedChunk != null && decryptedChunk.isNotEmpty()) {
                            fos.write(decryptedChunk)
                        }
                    }
                    val finalChunk = cipher.doFinal()
                    if (finalChunk != null && finalChunk.isNotEmpty()) {
                        fos.write(finalChunk)
                    }
                }
            } catch (e: javax.crypto.AEADBadTagException) {
                return@withContext BackupResult.Error("Contraseña incorrecta o archivo de copia dañado.")
            } finally {
                inputStream.close()
            }

            // Step 3: Extract ZIP and parse manifest
            var manifest: BackupManifest? = null
            val db = NoteDatabase.getDatabase(context)

            ZipInputStream(BufferedInputStream(FileInputStream(tempZipFile))).use { zis ->
                var entry: ZipEntry?
                while (zis.nextEntry.also { entry = it } != null) {
                    val entryName = entry!!.name

                    when {
                        entryName == "manifest.json" -> {
                            val reader = InputStreamReader(zis, Charsets.UTF_8)
                            val type = object : TypeToken<BackupManifest>() {}.type
                            manifest = Gson().fromJson(reader, type)
                        }
                        entryName.startsWith("media/") -> {
                            // Restore public media file
                            val subPath = entryName.removePrefix("media/")
                            val targetFile = File(File(context.filesDir, "media"), subPath)
                            targetFile.parentFile?.mkdirs()
                            FileOutputStream(targetFile).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                        entryName.startsWith("media_secret/") -> {
                            // Restore secret media file: Re-encrypt with local device KeyStore
                            val subPath = entryName.removePrefix("media_secret/")
                            val targetFile = File(File(context.filesDir, "media_encrypted"), subPath)
                            targetFile.parentFile?.mkdirs()

                            val tempDecrypted = File(context.cacheDir, "temp_rest_secret_${System.currentTimeMillis()}")
                            try {
                                FileOutputStream(tempDecrypted).use { fos ->
                                    zis.copyTo(fos)
                                }
                                tempDecrypted.inputStream().use { input ->
                                    FileOutputStream(targetFile).use { output ->
                                        CryptoManager.encryptFile(input, output)
                                    }
                                }
                            } finally {
                                if (tempDecrypted.exists()) tempDecrypted.delete()
                            }
                        }
                    }
                    zis.closeEntry()
                }
            }

            if (manifest == null) {
                return@withContext BackupResult.Error("El archivo de copia de seguridad no contiene datos válidos.")
            }

            // Step 4: Insert books
            if (manifest!!.books.isNotEmpty()) {
                db.noteDao().insertBooks(manifest!!.books)
            }

            // Step 5: Insert hidden apps
            if (manifest!!.hiddenApps.isNotEmpty()) {
                db.hiddenAppDao().insertAll(manifest!!.hiddenApps)
            }

            // Step 6: Re-encrypt secret notes with local device KeyStore and insert all notes
            val restoredNotes = manifest!!.notes.map { note ->
                if (note.isSecret) {
                    note.copy(
                        title = CryptoManager.encrypt(note.title),
                        body = CryptoManager.encrypt(note.body)
                    )
                } else {
                    note
                }
            }

            db.noteDao().insertNotes(restoredNotes)

            // Self-destruct / invalidate backup file so it cannot be reused
            try {
                android.provider.DocumentsContract.deleteDocument(context.contentResolver, sourceUri)
            } catch (_: Throwable) {
                try {
                    context.contentResolver.openOutputStream(sourceUri, "wt")?.use { os ->
                        os.write(ByteArray(128))
                        os.flush()
                    }
                } catch (_: Throwable) {}
            }

            BackupResult.Success(restoredNotes.size, manifest!!.books.size)
        } catch (e: Throwable) {
            e.printStackTrace()
            BackupResult.Error("Error al restaurar: ${e.localizedMessage ?: e.message}")
        } finally {
            if (tempZipFile.exists()) tempZipFile.delete()
        }
    }
}
