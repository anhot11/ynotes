package app.uamo.ynotes

import app.uamo.ynotes.data.NoteEntity
import app.uamo.ynotes.utils.CryptoManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CryptoManagerTest {

    @Before
    fun setUp() {
        CryptoManager.clearKeyCache()
    }

    @Test
    fun testEncryptDecryptRoundTrip() {
        val originalText = "Top secret message for yNotes SafeZone 2026!"
        val encrypted = CryptoManager.encrypt(originalText)
        
        assertNotNull(encrypted)
        assertNotEquals(originalText, encrypted)
        assertTrue(encrypted.isNotBlank())

        val decrypted = CryptoManager.decrypt(encrypted)
        assertEquals(originalText, decrypted)
    }

    @Test
    fun testBlankStringHandling() {
        assertEquals("", CryptoManager.encrypt(""))
        assertEquals("   ", CryptoManager.encrypt("   "))
        assertEquals("", CryptoManager.decrypt(""))
    }

    @Test
    fun testEncryptFieldsBatch() {
        val title = "Secret Meeting"
        val body = "Coordinates: 40.7128° N, 74.0060° W"

        val (encTitle, encBody) = CryptoManager.encryptFields(title, body)
        assertNotEquals(title, encTitle)
        assertNotEquals(body, encBody)

        val decTitle = CryptoManager.decrypt(encTitle)
        val decBody = CryptoManager.decrypt(encBody)

        assertEquals(title, decTitle)
        assertEquals(body, decBody)
    }

    @Test
    fun testDecryptBatchNotes() {
        val rawNote1 = NoteEntity(
            id = "1",
            title = CryptoManager.encrypt("Title 1"),
            body = CryptoManager.encrypt("Body 1"),
            isSecret = true
        )
        val rawNote2 = NoteEntity(
            id = "2",
            title = CryptoManager.encrypt("Title 2"),
            body = CryptoManager.encrypt("Body 2"),
            isSecret = true
        )

        val decrypted = CryptoManager.decryptBatch(listOf(rawNote1, rawNote2))
        assertEquals(2, decrypted.size)
        assertEquals("Title 1", decrypted[0].title)
        assertEquals("Body 1", decrypted[0].body)
        assertEquals("Title 2", decrypted[1].title)
        assertEquals("Body 2", decrypted[1].body)
    }

    @Test
    fun testTamperedCiphertextFallback() {
        val originalText = "Valid text"
        val encrypted = CryptoManager.encrypt(originalText)
        val tampered = encrypted.substring(0, encrypted.length - 4) + "AAAA"

        val result = CryptoManager.decrypt(tampered)
        // Decryption failure falls back gracefully to tampered string rather than crashing
        assertNotNull(result)
    }
}
