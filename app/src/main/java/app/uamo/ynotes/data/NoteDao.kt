package app.uamo.ynotes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    // Full note queries
    @Query("SELECT * FROM notes WHERE isSecret = 0 AND isDeleted = 0 AND (expiresAt IS NULL OR expiresAt > :currentTime) ORDER BY isWidgetSpecial DESC, isPinned DESC, updatedAt DESC")
    fun getPublicNotes(currentTime: Long = System.currentTimeMillis()): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isSecret = 1 AND isDeleted = 0 AND (expiresAt IS NULL OR expiresAt > :currentTime) ORDER BY isWidgetSpecial DESC, isPinned DESC, updatedAt DESC")
    fun getSecretNotes(currentTime: Long = System.currentTimeMillis()): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun getDeletedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE bookId = :bookId AND isDeleted = 0 AND (expiresAt IS NULL OR expiresAt > :currentTime) ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByBook(bookId: String, currentTime: Long = System.currentTimeMillis()): Flow<List<NoteEntity>>

    @Query("UPDATE notes SET isDeleted = 1, updatedAt = :currentTime WHERE isDeleted = 0 AND expiresAt IS NOT NULL AND expiresAt < :currentTime")
    suspend fun deleteExpiredNotes(currentTime: Long = System.currentTimeMillis())

    // Single note by ID (for editor - loads full body on demand)
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Query("UPDATE notes SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun moveToTrash(id: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE notes SET isDeleted = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun restoreFromTrash(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: String)
    
    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun emptyTrash()

    // Books
    @Query("SELECT * FROM books")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBook(id: String)

    @Query("UPDATE notes SET bookId = NULL WHERE bookId = :bookId")
    suspend fun removeBookFromNotes(bookId: String)
}
