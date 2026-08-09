package app.uamo.ynotes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

import androidx.room.Index

@Entity(
    tableName = "notes",
    indices = [
        Index("isSecret"),
        Index("isDeleted"),
        Index("bookId")
    ]
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val isSecret: Boolean = false,
    val color: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val bookId: String? = null,
    val isDeleted: Boolean = false,
    val isBodyHidden: Boolean = false,
    val mediaFiles: String = "",
    val expiresAt: Long? = null,
    val isWidgetSpecial: Boolean = false
)
