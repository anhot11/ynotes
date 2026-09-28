package app.uamo.ynotes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_apps")
data class HiddenAppEntity(
    @PrimaryKey val packageName: String,
    val name: String,
    val orderIndex: Int = 0,
    val userId: Int = 0
)
