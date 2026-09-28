package app.uamo.ynotes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenAppDao {

    @Query("SELECT * FROM hidden_apps ORDER BY orderIndex ASC, name ASC")
    fun getAllHiddenApps(): Flow<List<HiddenAppEntity>>

    @Query("SELECT * FROM hidden_apps ORDER BY orderIndex ASC, name ASC")
    suspend fun getAllHiddenAppsSync(): List<HiddenAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(app: HiddenAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<HiddenAppEntity>)

    @Query("DELETE FROM hidden_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM hidden_apps WHERE packageName IN (:packageNames)")
    suspend fun deleteMultiple(packageNames: List<String>)

    @Query("DELETE FROM hidden_apps")
    suspend fun deleteAll()
}
