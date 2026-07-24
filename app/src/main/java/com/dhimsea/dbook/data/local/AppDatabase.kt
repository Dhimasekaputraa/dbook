package com.dhimsea.dbook.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dhimsea.dbook.data.local.dao.BookDao
import com.dhimsea.dbook.data.local.dao.ScanDirectoryDao
import com.dhimsea.dbook.data.local.entity.BookEntity
import com.dhimsea.dbook.data.local.entity.ScanDirectoryEntity

@Database(
    entities = [BookEntity::class, ScanDirectoryEntity::class],
    version = 3,
    exportSchema = false
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun scanDirectoryDao(): ScanDirectoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                CREATE TABLE IF NOT EXISTS scan_directories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    uriString TEXT NOT NULL,
                    displayName TEXT NOT NULL,
                    addedAt INTEGER NOT NULL
                    )
                """)
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN fileSize INTEGER NOT NULL DEFAULT -1")
            }
        }
    }
}