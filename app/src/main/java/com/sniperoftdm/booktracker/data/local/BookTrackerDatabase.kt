package com.sniperoftdm.booktracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sniperoftdm.booktracker.data.model.Book
import com.sniperoftdm.booktracker.data.model.Note
import com.sniperoftdm.booktracker.data.model.Session

@Database(
    entities = [Book::class, Session::class, Note::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class BookTrackerDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun sessionDao(): SessionDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var instance: BookTrackerDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bookId` INTEGER NOT NULL,
                        `page` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_bookId` ON `notes` (`bookId`)")
            }
        }

        fun getInstance(context: Context): BookTrackerDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BookTrackerDatabase::class.java,
                    "booktracker.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
