package app.uamo.ynotes.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import app.uamo.ynotes.data.NoteEntity
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {
    private const val ALGORITHM = "AES"
    private const val BLOCK_MODE = "GCM"
    private const val PADDING = "NoPadding"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_ALIAS = "ynotes_safe_zone_key"

    @Volatile
    private var cachedKey: SecretKey? = null

    private val isAndroidKeyStoreAvailable: Boolean by lazy {
        try {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            true
        } catch (_: Throwable) {
            false
        }
    }

    private var fallbackJvmKey: SecretKey? = null

    @Synchronized
    fun getSecretKey(): SecretKey {
        cachedKey?.let { return it }

        val key = if (isAndroidKeyStoreAvailable) {
            try {
                val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                existingKey?.secretKey ?: createAndroidKeyStoreKey()
            } catch (e: Exception) {
                getOrCreateFallbackKey()
            }
        } else {
            getOrCreateFallbackKey()
        }

        cachedKey = key
        return key
    }

    @Synchronized
    fun clearKeyCache() {
        cachedKey = null
    }

    @Synchronized
    private fun createAndroidKeyStoreKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        )
        return keyGenerator.generateKey()
    }

    @Synchronized
    private fun getOrCreateFallbackKey(): SecretKey {
        fallbackJvmKey?.let { return it }
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val generated = keyGen.generateKey()
        fallbackJvmKey = generated
        return generated
    }

    fun encrypt(text: String): String {
        if (text.isBlank()) return text
        return try {
            encryptWithKey(text, getSecretKey())
        } catch (e: Exception) {
            e.printStackTrace()
            text
        }
    }

    fun decrypt(encryptedTextBase64: String): String {
        if (encryptedTextBase64.isBlank()) return encryptedTextBase64
        return try {
            decryptWithKey(encryptedTextBase64, getSecretKey())
        } catch (e: Exception) {
            e.printStackTrace()
            encryptedTextBase64
        }
    }

    /**
     * Batch-decrypt a list of NoteEntity objects reusing a single key resolution.
     */
    fun decryptBatch(notes: List<NoteEntity>): List<NoteEntity> {
        if (notes.isEmpty()) return notes
        return try {
            val key = getSecretKey()
            notes.map { note ->
                note.copy(
                    title = decryptWithKey(note.title, key),
                    body = decryptWithKey(note.body, key)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            notes
        }
    }

    /**
     * Batch-encrypt title and body in one pass.
     */
    fun encryptFields(title: String, body: String): Pair<String, String> {
        return try {
            val key = getSecretKey()
            Pair(encryptWithKey(title, key), encryptWithKey(body, key))
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(title, body)
        }
    }

    private fun encryptWithKey(text: String, key: SecretKey): String {
        if (text.isBlank()) return text
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encryptedText = cipher.doFinal(text.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(iv.size + encryptedText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedText, 0, combined, iv.size, encryptedText.size)

        return encodeBase64(combined)
    }

    private fun decryptWithKey(encryptedTextBase64: String, key: SecretKey): String {
        if (encryptedTextBase64.isBlank()) return encryptedTextBase64
        return try {
            val combined = decodeBase64(encryptedTextBase64)
            if (combined.size < 12) return encryptedTextBase64

            val iv = combined.copyOfRange(0, 12)
            val encryptedText = combined.copyOfRange(12, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val decryptedBytes = cipher.doFinal(encryptedText)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            encryptedTextBase64
        }
    }

    private fun encodeBase64(bytes: ByteArray): String {
        return try {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Throwable) {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    private fun decodeBase64(str: String): ByteArray {
        return try {
            Base64.decode(str, Base64.DEFAULT)
        } catch (_: Throwable) {
            java.util.Base64.getDecoder().decode(str.trim())
        }
    }

    /**
     * Encrypt a file using streaming AES-GCM.
     */
    fun encryptFile(inputStream: InputStream, outputStream: OutputStream) {
        val key = getSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        outputStream.write(cipher.iv) // 12 bytes IV
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            val output = cipher.update(buffer, 0, bytesRead)
            if (output != null && output.isNotEmpty()) outputStream.write(output)
        }
        val finalOutput = cipher.doFinal()
        if (finalOutput != null && finalOutput.isNotEmpty()) outputStream.write(finalOutput)
        outputStream.flush()
    }

    /**
     * Decrypt a file that was encrypted with encryptFile().
     */
    fun decryptFile(inputStream: InputStream, outputStream: OutputStream) {
        val key = getSecretKey()
        val iv = ByteArray(12)
        var totalRead = 0
        while (totalRead < 12) {
            val read = inputStream.read(iv, totalRead, 12 - totalRead)
            if (read == -1) throw java.io.IOException("Unexpected end of encrypted file")
            totalRead += read
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            val output = cipher.update(buffer, 0, bytesRead)
            if (output != null && output.isNotEmpty()) outputStream.write(output)
        }
        val finalOutput = cipher.doFinal()
        if (finalOutput != null && finalOutput.isNotEmpty()) outputStream.write(finalOutput)
        outputStream.flush()
    }
}
