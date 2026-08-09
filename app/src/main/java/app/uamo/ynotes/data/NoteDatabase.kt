package app.uamo.ynotes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [NoteEntity::class, BookEntity::class], version = 6, exportSchema = false)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `books` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `color` INTEGER NOT NULL, `iconName` TEXT NOT NULL, `isSecret` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `color` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `isPinned` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `bookId` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `isBodyHidden` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `mediaFiles` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `expiresAt` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `isWidgetSpecial` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isSecret` ON `notes` (`isSecret`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isDeleted` ON `notes` (`isDeleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_bookId` ON `notes` (`bookId`)")
            }
        }

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "ynotes_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
